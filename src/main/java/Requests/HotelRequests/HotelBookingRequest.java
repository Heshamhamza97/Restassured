package Requests.HotelRequests;

import Requests.Config.HotelSharedData;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.Assert;

import static io.restassured.RestAssured.given;

public class HotelBookingRequest {
    private final HotelSharedData sharedData;

    public HotelBookingRequest(HotelSharedData sharedData) {
        this.sharedData = sharedData;
    }

    public Response execute() {
        RestAssured.baseURI = HotelSharedData.getBaseUri();

        String bookingCodes = sharedData.getPreBookingResponse().jsonPath().get("data.bookingCode");
        String checkInDate = sharedData.getShowHotelRoomsResponse()
                .jsonPath()
                .getString("hotel.checkInTime");
        String checkOutDate = sharedData.getShowHotelRoomsResponse()
                .jsonPath()
                .getString("hotel.checkOutTime");

        JSONArray roomsArray = new JSONArray()
                .put(new JSONObject().put("passengers", new JSONArray().put(sharedData.getPassengersData())));

        JSONObject body = new JSONObject()
                .put("rooms", roomsArray)
                .put("email", "heshamhamza1997@gmail.com")
                .put("phone", "1030716233")
                .put("phoneCodeId", 70)
                .put("cacheKey", sharedData.getConnectionId())
                .put("bookingCode", bookingCodes)
                .put("checkIn", checkInDate)
                .put("checkOut", checkOutDate)
                .put("currencyId", 43497)
                .put("currencyRate", 1)
                .put("deviceType", 0)
                .put("hotelId", sharedData.getLatestHotelId())
                .put("languageId", 43499)
                .put("priceChanged", false)
                .put("isPayLater", false)
                .put("isCanPay", true)
                .put("paymentMethodId", 1)
                .put("nationality", sharedData.getNationality())
                .put("nationalityName", "Egypt")
                .put("termsAndCondtions", true)
                .put("roomPassengers", sharedData.getAdults() + "-" + sharedData.getChildren())
                .put("passengers", new JSONArray().put(sharedData.getPassengersData()))
                .put("rateConditoins", new JSONArray());

        Response response = given()
                .relaxedHTTPSValidation()
                .basePath("/api/MobileHotels/BookingRequest")
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .body(body.toString())
                .post()
                .then()
                .extract()
                .response();
        response.prettyPrint();
        Assert.assertEquals(response.getStatusCode(), 200, "Expected 200 status for booking request");
        Assert.assertNotNull(response.jsonPath().getString("data.bookingId"), "Booking ID is null");

        System.out.println("Booking completed successfully!");
        return response;
    }
}
