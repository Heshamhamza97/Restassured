package Requests.SocketConnection;

import Requests.Config.HotelSharedData;
import io.restassured.response.Response;

import static io.restassured.RestAssured.given;

public class openSocket {
    public static Response negotiate(int negotiateVersion) {
        return given()
                .baseUri(HotelSharedData.getBaseUri())
                .basePath("/hubs/mobilehotelsearch/negotiate")
                .relaxedHTTPSValidation()
                .queryParam("negotiateVersion", negotiateVersion)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .post()
                .then()
                .extract()
                .response();
    }

}
