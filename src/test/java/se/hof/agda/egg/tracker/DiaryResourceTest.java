package se.hof.agda.egg.tracker;

import io.agroal.api.AgroalDataSource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.time.Instant;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Testcontainers
@QuarkusTest
@Transactional
public class DiaryResourceTest {

    @Inject
    AgroalDataSource dataSource;

    @Test
    public void testPOSTDiaryEntry() throws IOException, SQLException {

        String validDiaryEntry = new String(Files.readAllBytes(
                Paths.get("src/test/resources/POST-valid-diary.json")));
        given().body(validDiaryEntry)
               .contentType(ContentType.JSON)
               .when().post("diary/entry")
               .then().assertThat().statusCode(200);

        String selectDiaryEntry = "SELECT * FROM diary.entries WHERE eggs=? AND datetime=?";
        try (Connection dbConn = dataSource.getConnection();
             PreparedStatement ps = dbConn.prepareStatement(selectDiaryEntry);
        ) {
            ps.setInt(1, 6);
            Timestamp expectedTimestamp = Timestamp.from(Instant.ofEpochMilli(1564000592095L));
            ps.setTimestamp(2, expectedTimestamp);
            ResultSet rs = ps.executeQuery();
            int count = 0;
            while (rs.next()) {
                int actualEggs = rs.getInt("eggs");
                Timestamp actualTimestamp = rs.getTimestamp("datetime");
                assertEquals(6, actualEggs);
                assertEquals(expectedTimestamp, actualTimestamp);
                count++;
            }
            assertEquals(1, count);
        }
    }

    @Test
    public void testPostCsv() throws IOException {

        String fakeBatchFile = new String(Files.readAllBytes(
                Paths.get("src/test/resources/batch-example.csv")
        ));
        given().body(fakeBatchFile)
               .contentType(MediaType.TEXT_PLAIN)
               .when()
               .post("diary/entries")
               .then().assertThat().statusCode(200);
    }

//    @Test
//    public void testGETDiaryEntryByDate() {
//        try {
//            setupDb();
//
//            Matcher matcher = null;
//            given().accept(ContentType.JSON)
//                   .when().get("diary/entries?date=2019-11-11")
//                   .then().assertThat().statusCode(200);
//                   //.and().assertThat().body(matcher);
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
//
//    }

    private void setupDb() throws SQLException {
        String insertData = "INSERT INTO diary.entries VALUES(?,?)";
        try (Connection dbConn = dataSource.getConnection();
             PreparedStatement ps = dbConn.prepareStatement(insertData)) {
            ps.setInt(1, 3);
            ps.setTimestamp(2,
                            Timestamp.from(Instant.ofEpochMilli(1573470192000L)));
            ps.executeUpdate();
        }
    }
}