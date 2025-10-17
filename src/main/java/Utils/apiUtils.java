package Utils;
import Requests.Config.HotelSharedData;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import java.util.function.Supplier;

import static io.restassured.RestAssured.given;

public class apiUtils {
    public static Response post(String baseUri, String endpoint, String token, Object body) {
        return given()
                .baseUri(baseUri)
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .body(body)
                .relaxedHTTPSValidation()
                .post(endpoint)
                .then()
                .extract()
                .response();
    }
    public static RequestSpecification getRequestSpec(String token) {
        return new RequestSpecBuilder()
                .setBaseUri(HotelSharedData.getBaseUri())
                .addHeader("Authorization", "Bearer " + token)
                .addHeader("Content-Type", "application/json")
                .build();
    }
    public static Response retryUntilSuccess(Supplier<Response> request, int maxAttempts, int waitSeconds) {
        Response response = null;
        for (int i = 0; i < maxAttempts; i++) {
            response = request.get();
            if (response.getStatusCode() == 200) return response;
            try {
                Thread.sleep(waitSeconds * 1000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        return response;
    }

}
