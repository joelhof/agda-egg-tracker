package se.hof.agda.egg.tracker;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
public class DiaryResourceTest {

    @Test
    public void testHelloEndpoint() {
        given()
          .when().get("/hello")
          .then()
             .statusCode(200)
             .body(is("hello"));
    }

    @Test
    public void testPOSTDiaryEntry() throws IOException {
        String validDiaryEntry = Files.readString(Paths.get("src/test/resources/POST-valid-diary.json"));
        given().body(validDiaryEntry)
               .contentType(ContentType.JSON)
        .when().post("diary/entry")
        .then().assertThat().statusCode(200);
    }

}