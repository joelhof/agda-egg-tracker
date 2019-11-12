package se.hof.agda.egg.tracker;

import io.agroal.api.AgroalDataSource;
import se.hof.agda.egg.tracker.dto.DiaryEntryDTO;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.sql.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

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
       // System.out.println(flyway.info().current().getVersion().toString());

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
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/entries")
    public String getEntries(
            @QueryParam("date")String dateString) {
        LocalDate date = LocalDate.parse(dateString, DateTimeFormatter.ISO_DATE);
        System.out.println(date);
        String sql = "SELECT * FROM diary.entries WHERE date(datetime) =?";
        String response = "";
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
                response = String.valueOf(eggs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Could not fetch eggs for date= "
                                               + date.format(DateTimeFormatter.ISO_DATE));
        }

        return response;
    }

}