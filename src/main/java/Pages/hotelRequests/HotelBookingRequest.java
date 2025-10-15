package Pages.hotelRequests;

import Pages.HotelSharedData;
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
        RestAssured.baseURI = Base.sharedData.getBaseUri();

        String bookingCodes = sharedData.getPreBookingResponse().jsonPath().get("data.bookingCode");

        JSONObject passenger = new JSONObject()
                .put("passengerTypeId", 43482)
                .put("passengerTitleId", 43479)
                .put("firstName", "Hesham")
                .put("lastName", "Hamza")
                .put("dateOfBirth", "2010-07-02")
                .put("roomNumber", 1);

        JSONArray roomsArray = new JSONArray()
                .put(new JSONObject().put("passengers", new JSONArray().put(passenger)));

        JSONObject body = new JSONObject()
                .put("rooms", roomsArray)
                .put("email", "heshamhamza1997@gmail.com")
                .put("phone", "1030716233")
                .put("phoneCodeId", 70)
                .put("cacheKey", sharedData.getConnectionId())
                .put("bookingCode", bookingCodes)
                .put("checkIn", sharedData.getCheckIn())
                .put("checkOut", sharedData.getCheckOut())
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
                .put("roomPassengers", "1-0")
                .put("passengers", new JSONArray().put(passenger))
                .put("rateConditoins", new JSONArray());
        System.out.println("final book body:" + body);
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

        System.out.println("✅ Booking completed successfully!");
        return response;
    }
}
