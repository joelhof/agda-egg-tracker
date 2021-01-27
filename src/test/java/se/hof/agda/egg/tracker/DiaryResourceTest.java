package se.hof.agda.egg.tracker;

import io.agroal.api.AgroalDataSource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.mapper.ObjectMapper;
import io.restassured.mapper.ObjectMapperDeserializationContext;
import io.restassured.mapper.ObjectMapperSerializationContext;
import org.junit.jupiter.api.*;
import org.testcontainers.junit.jupiter.Testcontainers;
import se.hof.agda.egg.tracker.dto.BatchResponseDTO;

import javax.inject.Inject;
import javax.json.bind.JsonbBuilder;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.time.Instant;
import java.util.Collections;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Testcontainers
@QuarkusTest
@Tag("testcontainer")
public class DiaryResourceTest {

    public static final long MOST_RECENT_ENTRY = 1573471193000L;

    @Inject
    AgroalDataSource dataSource;

    private static ObjectMapper jsonbObjectMapper;

    @BeforeAll
    public static void setup() {
        jsonbObjectMapper = new ObjectMapper() {
            @Override
            public Object deserialize(ObjectMapperDeserializationContext context) {
                return JsonbBuilder.create().fromJson(context.getDataToDeserialize().asInputStream(),
                                                      context.getType());
            }

            @Override
            public Object serialize(ObjectMapperSerializationContext objectMapperSerializationContext) {
                return null;
            }
        };
    }

    @AfterEach
    void cleanUpDb() throws SQLException {
        try (Connection dbConn = dataSource.getConnection();
             PreparedStatement ps = dbConn.prepareStatement(
                     "DELETE FROM diary.entries")) {
            ps.executeUpdate();
        }
    }

    @Test
    @DisplayName("POST a diary entry")
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
    @DisplayName("POST a batch of diary entries")
    public void testPostCsv() throws IOException {

        String fakeBatchFile = new String(Files.readAllBytes(
                Paths.get("src/test/resources/batch-example.csv")
        ));
        BatchResponseDTO expectedResponse = new BatchResponseDTO();
        expectedResponse.setFailed(Collections.emptyList());
        expectedResponse.setEggsReported(28);

        BatchResponseDTO actualResponse = given().body(fakeBatchFile)
                                                 .contentType(MediaType.TEXT_PLAIN)
                                                 .when()
                                                 .post("diary/entries")
                                                 .then().assertThat().statusCode(200)
                                                 .and().extract().as(BatchResponseDTO.class, jsonbObjectMapper);
        assertEquals(expectedResponse.getEggsReported(),
                     actualResponse.getEggsReported(),
                     "Number of eggs reported should match");
        assertEquals(expectedResponse.getFailed().size(),
                     actualResponse.getFailed().size());

    }

    @Test
    @DisplayName("GET diary entries for a single day")
    public void testGETDiaryEntryByDate() {
        try {
            long expectedTimestamp = 1573470192000L;
            int expectedEggCount = 3;
            String expectedEvent = "This is a test, Ingen skötsel?";
            insertEntryIntoDb(expectedEggCount,
                              Instant.ofEpochMilli(expectedTimestamp),
                              expectedEvent);

            given().accept(ContentType.JSON)
                   .when().get("diary/entries?date=2019-11-11")
                   .then().assertThat().statusCode(200)
                   .and().body("[0].eggs", equalTo(expectedEggCount))
                   .body("[0].timestamp", equalTo(expectedTimestamp))
                   .body("[0].event", equalTo(expectedEvent));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Test
    @DisplayName("GET diary entries in a date interval")
    public void testGetEntriesByInterval() {
        try {
            // GIVEN 2 entries the same day, only the latest is returned
            insertEntryIntoDb(3, Instant.ofEpochMilli(1573470193000L));
            String expectedEvent = "this is a test";
            insertEntryIntoDb(6, Instant.ofEpochMilli(MOST_RECENT_ENTRY), expectedEvent);
            long expectedTimestamp = MOST_RECENT_ENTRY;
            int expectedEggCount = 6;
            int expectedNrOfEntries = 1;

            given().accept(ContentType.JSON)
                   .when().get("diary/entries?from=2019-11-10&to=2020-11-10")
                   .then().assertThat().statusCode(200)
                   .and().body("$.size()", is(expectedNrOfEntries))
                   .body("[0].eggs", is(expectedEggCount))
                   .body("[0].timestamp", equalTo(expectedTimestamp))
                   .body("[0].event", equalTo(expectedEvent));;
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void insertEntryIntoDb(int eggs, Instant timestamp) throws SQLException {
        insertEntryIntoDb(eggs, timestamp, null);
    }

    private void insertEntryIntoDb(int eggs, Instant timestamp, String event) throws SQLException {
        String insertData = "INSERT INTO diary.entries (eggs, datetime, event) VALUES(?,?,?)";
        try (Connection dbConn = dataSource.getConnection();
             PreparedStatement ps = dbConn.prepareStatement(insertData)) {
            ps.setInt(1, eggs);
            ps.setTimestamp(2,
                            Timestamp.from(timestamp));
            ps.setString(3, event);
            ps.executeUpdate();
        }
    }

    @Test
    @DisplayName("POST a batch of diary entries using v2 format")
    public void testPostCsvV2() throws IOException {

        String fakeBatchFile = new String(Files.readAllBytes(
                Paths.get("src/test/resources/batch-v2-example.csv")
        ));
        BatchResponseDTO expectedResponse = new BatchResponseDTO();
        expectedResponse.setFailed(Collections.emptyList());
        expectedResponse.setEggsReported(23);

        BatchResponseDTO actualResponse = given().body(fakeBatchFile)
                                                 .contentType(MediaType.TEXT_PLAIN)
                                                 .when()
                                                 .post("diary/entries")
                                                 .then().assertThat().statusCode(200)
                                                 .and().extract().as(BatchResponseDTO.class, jsonbObjectMapper);
        assertEquals(expectedResponse.getEggsReported(),
                     actualResponse.getEggsReported(),
                     "Number of eggs reported should match");
        assertEquals(expectedResponse.getFailed().size(),
                     actualResponse.getFailed().size());
    }
}