package se.hof.agda.egg.tracker;

import io.agroal.api.AgroalDataSource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.h2.H2DatabaseTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import javax.transaction.Transactional;
import java.io.IOException;
import java.sql.SQLException;

@QuarkusTest
@Transactional
@QuarkusTestResource(H2DatabaseTestResource.class)
public class DiaryResourceTest {

    @Inject
    AgroalDataSource dataSource;

    @Disabled
    @Test
    public void testPOSTDiaryEntry() throws IOException, SQLException {
/*        String validDiaryEntry = Files.readString(Paths.get("src/test/resources/POST-valid-diary.json"));
        given().body(validDiaryEntry)
               .contentType(ContentType.JSON)
        .when().post("diary/entry")
        .then().assertThat().statusCode(200);

        String selectDiaryEntry = "SELECT * FROM entries WHERE eggs=? AND timestamp=?";
        try (Connection dbConn = dataSource.getConnection();
             PreparedStatement ps = dbConn.prepareStatement(selectDiaryEntry);
        ) {
            ps.setInt(1,6);
            ps.setLong(2, 1234L);
            ResultSet rs = ps.executeQuery();
            int count = 0;
            while (rs.next()) {
               int actualEggs = rs.getInt("eggs");
               long actualTimestamp = rs.getLong("datetime");
               assertEquals(6, actualEggs);
               assertEquals(1234L, actualTimestamp);
               count++;
            }
            assertEquals(1, count);
        }*/
    }

}