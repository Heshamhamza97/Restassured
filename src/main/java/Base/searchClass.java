package Base;


import io.restassured.RestAssured;
import io.restassured.response.Response;

import static io.restassured.RestAssured.given;

public class searchClass {
    public static Response negotiate(int negotiateVersion) {
        RestAssured.baseURI = sharedData.getBaseUri();

        Response response = given()
                .relaxedHTTPSValidation()
                .basePath("/hubs/mobilehotelsearch/negotiate")
                .queryParam("negotiateVersion", negotiateVersion)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .when()
                .post()
                .then()
                .extract().response();

        System.out.println("Status Code: " + response.getStatusCode());
        response.prettyPrint();
        return response;
    }
}
