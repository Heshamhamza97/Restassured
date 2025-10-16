package Requests.HotelRequests;

import Requests.Config.HotelSharedData;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.Assert;

import static io.restassured.RestAssured.given;

public class HotelRoomDetails {
    private final HotelSharedData sharedData;

    public HotelRoomDetails(HotelSharedData sharedData) {
        this.sharedData = sharedData;
    }

    public Response execute() {
        RestAssured.baseURI = HotelSharedData.getBaseUri();

        Assert.assertNotNull(sharedData.getLatestHotelId(), "Hotel ID is null before ShowHotelRooms request");
        Assert.assertNotNull(sharedData.getConnectionId(), "Connection ID is null before ShowHotelRooms request");

        JSONObject body = new JSONObject()
                .put("bookingCode", "")
                .put("bookingCodes", new JSONArray())
                .put("cacheKey", sharedData.getConnectionId())
                .put("checkIn", sharedData.getCheckIn())
                .put("checkOut", sharedData.getCheckOut())
                .put("hotelId", sharedData.getLatestHotelId())
                .put("nationality", sharedData.getNationality())
                .put("passengers", sharedData.getPassengers())
                .put("rooms", new JSONArray());

        Response response = given()
                .relaxedHTTPSValidation()
                .basePath("/api/Hotel/HotelResultDetails/" + sharedData.getConnectionId() + "/" + sharedData.getLatestHotelId() + "/")
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .body(body.toString())
                .post()
                .then()
                .extract()
                .response();
        System.out.println("RoomDetails body:" + body);
        response.prettyPrint();
        Assert.assertEquals(response.getStatusCode(), 200, "Expected status 200 for showHotelRooms");
        sharedData.setShowHotelRoomsResponse(response);

        System.out.println("✅ ShowHotelRooms executed successfully");
        return response;
    }
}
