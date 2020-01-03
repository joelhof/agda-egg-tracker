package se.hof.agda.egg.tracker;

import io.agroal.api.AgroalDataSource;
import se.hof.agda.egg.tracker.dto.BatchResponseDTO;
import se.hof.agda.egg.tracker.dto.DiaryEntryDTO;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.sql.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static java.util.Arrays.stream;

@Path("diary")
public class DiaryResource {

    public static final String INSERT_DIARY_ENTRY = "INSERT INTO diary.entries (eggs, datetime)" +
            "values (?,?)";
    @Inject
    AgroalDataSource eggDataSource;

//    @Inject
//    Flyway flyway;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/entry")
    public String createDiaryEntry(DiaryEntryDTO body) {
        System.out.println("Payload: " + body.toString());
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
            @QueryParam("date")String dateString) {
        assertParams(dateString);

        LocalDate date = LocalDate.parse(dateString, DateTimeFormatter.ISO_DATE);
        System.out.println(date);
        String sql = "SELECT * FROM diary.entries" +
                " WHERE date(datetime) =?" +
                " ORDER BY datetime DESC";
        List<DiaryEntryDTO> response = new ArrayList<>();
        try (
                Connection dbConn = eggDataSource.getConnection();
                PreparedStatement ps = dbConn.prepareStatement(sql);
                ) {
            ps.setDate(1, Date.valueOf(date));
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
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Could not fetch eggs for date= "
                                               + date.format(DateTimeFormatter.ISO_DATE));
        }

        return response;
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
        System.out.println("Batch file recieved:" + csvBody);
        String[] lines = csvBody.split("\\r?\\n");
        String[] metaData = lines[0].split(",");
        List<Integer> eggCounts = stream(lines)
                .skip(1)
                .map(s -> s.trim().replace(",", ""))
                .map(s -> s.isEmpty() ? null : Integer.valueOf(s))
                .collect(Collectors.toList());

        LocalDate startDate = LocalDate.parse(metaData[0], DateTimeFormatter.ISO_DATE);
        LocalDate endDate = LocalDate.parse(metaData[1], DateTimeFormatter.ISO_DATE);

        AtomicInteger dateCounter = new AtomicInteger(0);
        List<DiaryEntryDTO> batch = eggCounts.stream()
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

        return batchInsertEntries(batch);
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

    private void assertParams(@QueryParam("date") String dateString) {
        if (dateString == null)
            throw new WebApplicationException("Query parameter 'date' must be set",
                                              Response.Status.BAD_REQUEST);
    }
}