package Utils;
import Requests.Config.HotelSharedData;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.json.JSONObject;
import org.testng.Assert;

import java.util.function.Supplier;

import static io.restassured.RestAssured.given;

public class apiUtils {

    public static JSONObject buildJsonBody(Object... keyValuePairs) {
        JSONObject body = new JSONObject();
        for (int i = 0; i < keyValuePairs.length; i += 2) {
            String key = (String) keyValuePairs[i];
            Object value = keyValuePairs[i + 1];
            body.put(key, value != null ? value : "");
        }
        return body;
    }

    public static Response post(String basePath, JSONObject body) {
        return given()
                .relaxedHTTPSValidation()
                .basePath(basePath)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .body(body.toString())
                .post()
                .then()
                .extract()
                .response();
    }

    public static Response postAndValidate(String basePath, JSONObject body, int expectedStatus) {
        Response response = post(basePath, body);
        Assert.assertEquals(response.getStatusCode(), expectedStatus,
                "Expected status " + expectedStatus + " but got " + response.getStatusCode());
        return response;
    }

    public static Response postWithLogging(String basePath, JSONObject body, String operationName) {
        System.out.println(operationName + " body: " + body);
        Response response = post(basePath, body);
        response.prettyPrint();
        System.out.println(operationName + " executed successfully");
        return response;
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
