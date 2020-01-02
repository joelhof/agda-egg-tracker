package se.hof.agda.egg.tracker;

import io.agroal.api.AgroalDataSource;
import org.jboss.resteasy.annotations.cache.NoCache;
import se.hof.agda.egg.tracker.dto.DiaryEntryDTO;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.sql.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Path("diary")
public class DiaryResource {
    
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
        String sql = "INSERT INTO diary.entries (eggs, datetime)" +
                "values (?,?)";
        try (
                Connection dbConnection = eggDataSource.getConnection();
                PreparedStatement ps = dbConnection.prepareStatement(sql)) {
            ps.setInt(1, body.getEggs());
            Timestamp timestamp = Timestamp.from(Instant.ofEpochMilli(body.getTimestamp()));
            ps.setTimestamp(2, timestamp);
            int res = ps.executeUpdate();
            ps.close();
            return "nr of eggs posted " + body.getEggs();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Egg diary was not updated. ", e);
        }
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

    @POST
    @Path("/entries")
    @Consumes(MediaType.TEXT_PLAIN)
    public String addEntriesFromFile(String csvBody) {
        String[] lines = csvBody.split("\\r?\\n");
        String[] metaData = lines[0].split(",");
        List<Integer> eggCounts = Arrays
                .stream(lines)
                .skip(1)
                .map(s -> s.trim().replace(",", ""))
                .map(s -> s.isEmpty() ? null : Integer.valueOf(s))
                .collect(Collectors.toList());

        LocalDate startDate = LocalDate.parse(metaData[0], DateTimeFormatter.ISO_DATE);
        LocalDate endData = LocalDate.parse(metaData[1], DateTimeFormatter.ISO_DATE);


        return Response.accepted().build().toString();
    }


    private void assertParams(@QueryParam("date") String dateString) {
        if (dateString == null)
            throw new WebApplicationException("Query parameter 'date' must be set",
                                              Response.Status.BAD_REQUEST);
    }

}