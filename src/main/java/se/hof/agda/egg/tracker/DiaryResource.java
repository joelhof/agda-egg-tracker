package se.hof.agda.egg.tracker;

import io.agroal.api.AgroalDataSource;
import org.jboss.logging.Logger;
import se.hof.agda.egg.tracker.dto.BatchResponseDTO;
import se.hof.agda.egg.tracker.dto.DiaryEntryDTO;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.sql.*;
import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.Arrays.stream;

@Path("diary")
public class DiaryResource {

    private static final Logger LOG = Logger.getLogger(DiaryResource.class);

    public static final String INSERT_DIARY_ENTRY = "INSERT INTO diary.entries (eggs, datetime)" +
            "values (?,?)";

    @Inject
    AgroalDataSource eggDataSource;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/entry")
    public String createDiaryEntry(DiaryEntryDTO body) {
        System.out.println("Payload: " + body.toString());
        LOG.info("Payload: " + body.toString());
        try (
                Connection dbConnection = eggDataSource.getConnection();
                PreparedStatement ps = dbConnection.prepareStatement(INSERT_DIARY_ENTRY)) {
            addDiaryEntry(body, ps);
            int res = ps.executeUpdate();
            ps.close();
            return "nr of eggs posted " + body.getEggs();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Egg diary was not updated. ", e);
        }
    }

    private void addDiaryEntry(DiaryEntryDTO body, PreparedStatement ps) throws SQLException {
        ps.setInt(1, body.getEggs());
        Timestamp timestamp = Timestamp.from(Instant.ofEpochMilli(body.getTimestamp()));
        ps.setTimestamp(2, timestamp);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/entries")
    public List<DiaryEntryDTO> getEntries(
            @QueryParam("date")String dateString,
            @QueryParam("from")String fromString,
            @QueryParam("to")String toString) {
        assertParams(fromString, toString, dateString);

        List<DiaryEntryDTO> response;
        if (Objects.nonNull(dateString)) {
            response = fetchEntriesByDate(dateString);
        } else {
            response = fetchEntriesByInterval(fromString, toString);
        }

        return response;
    }

    private List<DiaryEntryDTO> fetchEntriesByInterval(@QueryParam("from") String fromString,
                                                       @QueryParam("to") String toString) {
        LocalDate from = LocalDate.parse(fromString, DateTimeFormatter.ISO_DATE);
        LocalDate to = LocalDate.parse(toString, DateTimeFormatter.ISO_DATE);
        String selectLatestEntry =
                " SELECT DISTINCT ON (date(datetime)) datetime, eggs" +
                " FROM diary.entries" +
                " ORDER BY date(datetime) ASC, datetime DESC";
        String selectEntriesInInterval = "" +
                "SELECT datetime, eggs " +
                "FROM (" + selectLatestEntry + ") AS latestEntry" +
                " WHERE date(datetime) >=? AND date(datetime) <=?";
        try (
                Connection dbConn = eggDataSource.getConnection();
                PreparedStatement ps = dbConn.prepareStatement(selectEntriesInInterval);
        ) {
            ps.setDate(1, Date.valueOf(from));
            ps.setDate(2, Date.valueOf(to));
            return mapToDTO(ps);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(String.format("Could not fetch eggs for from=%s and to=%s ",
                                               from.format(DateTimeFormatter.ISO_DATE),
                                                     to.format(DateTimeFormatter.ISO_DATE)));
        }
    }

    private List<DiaryEntryDTO> fetchEntriesByDate(@QueryParam("date") String dateString) {
        LocalDate date = LocalDate.parse(dateString, DateTimeFormatter.ISO_DATE);
        System.out.println(date);
        String sql = "SELECT * FROM diary.entries" +
                " WHERE date(datetime)=?" +
                " ORDER BY datetime DESC";
        try (
                Connection dbConn = eggDataSource.getConnection();
                PreparedStatement ps = dbConn.prepareStatement(sql);
        ) {
            ps.setDate(1, Date.valueOf(date));
            return mapToDTO(ps);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Could not fetch eggs for date= "
                                               + date.format(DateTimeFormatter.ISO_DATE));
        }
    }

    private List<DiaryEntryDTO> mapToDTO(PreparedStatement ps) throws SQLException {
        List<DiaryEntryDTO> response = new ArrayList<>();
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            int eggs = rs.getInt("eggs");
            Timestamp ts = rs.getTimestamp("datetime");
            System.out.println("eggs: " + eggs + " datetime: " + ts.toString());
            DiaryEntryDTO entry = new DiaryEntryDTO();
            entry.setEggs(eggs);
            entry.setTimestamp(ts.getTime());
            response.add(entry);
        }
        return response;
    }

    private enum BatchValidationResult {
        INVALID,
        V1,
        V2
    }

    /**
     * POST a CSV-file with egg counts. First line must be start date and
     * end date, in ISO-format.
     * The rest of the file is a list of the reported egg counts.
     * Since there is no timestamp information available batch reporting
     * will use 00:00 as the timestamp.
     * This means that batch reporting will NOT overwrite already reported counts.
     * Subsequent batch reports WILL replace already existing batch reported
     * egg counts.
     * Missing egg counts are ignored.
     * @param csvBody
     * @return
     */
    @POST
    @Path("/entries")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public BatchResponseDTO addEntriesFromFile(String csvBody) {
        LOG.info("Batch file recieved:" + csvBody);
        String[] lines = csvBody.split("\\r?\\n");
        String[] metaData = lines[0].split(",");

        BatchValidationResult validation = validateMetadata(metaData);

        switch (validation) {
            case V1:
                return batchInsertEntries(toDiaryEntriesV1(lines, metaData[0]));
            case V2:
                return batchInsertEntries(toDiaryEntriesV2(lines, metaData));
            case INVALID:
                throw new WebApplicationException(
                        "Invalid CSV schema.",
                        Response.Status.BAD_REQUEST);
        }

        return batchInsertEntries(Collections.EMPTY_LIST);
    }

    private List<DiaryEntryDTO> toDiaryEntriesV2(String[] lines, String[] metaData) {
        return stream(lines)
                .skip(1)
                .map(line -> line.split(",", -1))
                .map(columns -> {
                    LOG.info(Arrays.toString(columns));
                    DiaryEntryDTO result = new DiaryEntryDTO();
                    if (!columns[1].isEmpty()) {
                        result.setEggs(Integer.parseInt(columns[1]));
                    }
                    result.setTimestamp(LocalDate.parse(columns[0], DateTimeFormatter.ISO_DATE)
                                                 .atStartOfDay()
                                                 .toInstant(ZoneOffset.UTC)
                                                 .toEpochMilli());
                    return result;
                }).collect(Collectors.toList());
    }

    private List<DiaryEntryDTO> toDiaryEntriesV1(String[] lines, String metaDatum) {
        List<Integer> eggCounts = stream(lines)
                .skip(1)
                .map(s -> s.trim().replace(",", ""))
                .map(s -> s.isEmpty() ? null : Integer.valueOf(s))
                .collect(Collectors.toList());

        LocalDate startDate = LocalDate.parse(metaDatum, DateTimeFormatter.ISO_DATE);
        //LocalDate endDate = LocalDate.parse(metaData[1], DateTimeFormatter.ISO_DATE);

        AtomicInteger dateCounter = new AtomicInteger(0);
        return eggCounts.stream()
                 .map(count -> {
                     LocalDate date = startDate.plusDays(
                             dateCounter.getAndIncrement());
                     if (count != null) {
                         DiaryEntryDTO result = new DiaryEntryDTO();
                         result.setEggs(count);
                         result.setTimestamp(date.atStartOfDay()
                                                 .toInstant(ZoneOffset.UTC)
                                                 .toEpochMilli());
                         return result;
                     }
                     return null;
                 }).filter(Objects::nonNull)
                 .collect(Collectors.toList());
    }

    private BatchValidationResult validateMetadata(String[] metaData) {
        try {
            if (metaData.length == 1 || metaData.length == 2) {
                LocalDate.parse(metaData[0], DateTimeFormatter.ISO_DATE);
                return BatchValidationResult.V1;
            }

            String[] v2Schema = new String[]{"Timestamp", "Eggs", "Event"};
            if (metaData.length == 3 && IntStream.range(0, v2Schema.length)
                                                 .allMatch(pos -> v2Schema[pos].equalsIgnoreCase(metaData[pos]))) {
                return BatchValidationResult.V2;
            }
        } catch (Exception e) {
            LOG.error("Invalid batch file headers: " + Arrays.toString(metaData));
        }
        return BatchValidationResult.INVALID;
    }

    private BatchResponseDTO batchInsertEntries(List<DiaryEntryDTO> batch) {
        String batchInsertDiaryEntry = INSERT_DIARY_ENTRY
                + "ON CONFLICT ON CONSTRAINT no_duplicates"
                + " DO UPDATE SET eggs = EXCLUDED.eggs;";
        try (
                Connection dbConnection = eggDataSource.getConnection();
                PreparedStatement ps = dbConnection.prepareStatement(batchInsertDiaryEntry)) {
            for (DiaryEntryDTO diaryEntryDTO : batch) {
                addDiaryEntry(diaryEntryDTO, ps);
                ps.addBatch();
            }

            int[] res = ps.executeBatch();

            return BatchResponseDTO.from(batch, res);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Batch update failed.", e);
        }
    }

    private void assertParams(String from, String to, String dateString) {
        if (dateString == null && from == null && to == null)
            throw new WebApplicationException("Query parameter 'date' or 'from' and 'to' must be set",
                                              Response.Status.BAD_REQUEST);
        if (from != null && to == null)
            throw new WebApplicationException("Query parameter 'to' must be set",
                                              Response.Status.BAD_REQUEST);
        if (to != null && from == null)
            throw new WebApplicationException("Query parameter 'from' must be set",
                                              Response.Status.BAD_REQUEST);
        if (from != null && to != null && dateString != null)
            throw new WebApplicationException("Provide query parameter 'date' or" +
                                                      " parameters 'from' and 'to'" +
                                                      "not all three",
                                              Response.Status.BAD_REQUEST);
    }
}