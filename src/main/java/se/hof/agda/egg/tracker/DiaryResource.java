package se.hof.agda.egg.tracker;

import io.agroal.api.AgroalDataSource;
import org.flywaydb.core.Flyway;
import se.hof.agda.egg.tracker.dto.DiaryEntryDTO;

import javax.enterprise.inject.Default;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;

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

}