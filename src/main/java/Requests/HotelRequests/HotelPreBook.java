package Requests.HotelRequests;

import Requests.Config.HotelSharedData;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.Assert;

import java.util.List;

import static io.restassured.RestAssured.given;

public class HotelPreBook {
    private final HotelSharedData sharedData;

    public HotelPreBook(HotelSharedData sharedData) {
        this.sharedData = sharedData;
    }

    public Response execute() throws InterruptedException {
        RestAssured.baseURI = HotelSharedData.getBaseUri();


        List<String> bookingCodes = sharedData.getShowHotelRoomsResponse().jsonPath().getList("rooms.bookingCode");
        Assert.assertFalse(bookingCodes.isEmpty(), "No booking codes found in hotel rooms response");

        System.out.println("Found " + bookingCodes.size() + " booking codes to try...");

        Response response = null;
        String successfulCode = null;

        for (String code : bookingCodes) {
            System.out.println("Trying to prebook using code: " + code);

            // Build request body
            JSONObject body = new JSONObject()
                    .put("bookingCode", code)
                    .put("bookingCodes", new JSONArray().put(code))
                    .put("cacheKey", sharedData.getConnectionId())
                    .put("checkIn", sharedData.getCheckIn())
                    .put("checkOut", sharedData.getCheckOut())
                    .put("hotelId", sharedData.getLatestHotelId())
                    .put("nationality", sharedData.getNationality())
                    .put("passengers", sharedData.getPassengers())
                    .put("rooms", new JSONArray()
                            .put(new JSONObject()
                                    .put("adults", sharedData.getAdults())
                                    .put("children", sharedData.getChildren())
                                    .put("childrenAges", new JSONArray())))
                    .put("payLater", false);
            System.out.println("PreBook body:" + body);
            // Send request
            response = given()
                    .relaxedHTTPSValidation()
                    .basePath("/api/Hotel/PreBook")
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .body(body.toString())
                    .post()
                    .then()
                    .extract()
                    .response();
            Thread.sleep(1000);
            System.out.println("🔎 Prebook response for code " + code + ":");
            response.prettyPrint();

            int statusCode = response.getStatusCode();
            boolean success = false;
            String message = null;

            try {
                String contentType = response.getHeader("Content-Type");
                String rawBody = response.getBody().asString();

                // Ensure it's JSON
                if (contentType != null && contentType.contains("application/json")) {
                    success = response.jsonPath().getBoolean("success");
                    message = response.jsonPath().getString("message");
                } else {
                    System.out.println("⚠️ Response is not JSON. Content-Type: " + contentType);
                    System.out.println("Raw Body: " + rawBody);
                }
            } catch (Exception e) {
                System.out.println("⚠️ Could not parse response JSON safely. Raw body below:");
                System.out.println(response.getBody().asString());
            }

            if (statusCode == 200 && success && !"Not Available! Please select another option!".equalsIgnoreCase(message)) {
                successfulCode = code;
                System.out.println("Successfully prebooked with code: " + code);
                break;
            } else {
                System.out.println("Code " + code + " not available or failed. Message: " + message);
            }
        }
        // If no code succeeded
        Assert.assertNotNull(successfulCode, "All booking codes failed to prebook. None available!");
        sharedData.setPreBookingResponse(response);

        return response;
    }
}
