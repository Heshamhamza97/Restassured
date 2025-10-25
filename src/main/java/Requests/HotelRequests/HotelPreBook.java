package Requests.HotelRequests;

import Requests.Config.HotelSharedData;
import io.restassured.response.Response;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.Assert;

import java.util.List;

import static io.restassured.RestAssured.given;

public class HotelPreBook {
    private static final String BASE_URI = "https://travelcore.techeffic.com";
    private static final String PREBOOK_PATH = "/api/Hotel/PreBook";
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final int RETRY_DELAY_MS = 2000;
    private static final String NOT_AVAILABLE_MSG = "Not Available! Please select another option!";

    private final HotelSharedData sharedData;

    private String CheckInDate;
    private String CheckOutDate ;
    public HotelPreBook(HotelSharedData sharedData) {
        this.sharedData = sharedData;
        CheckOutDate= sharedData.getShowHotelRoomsResponse()
                .jsonPath()
                .getString("hotel.checkOutTime") ;
        CheckInDate= sharedData.getShowHotelRoomsResponse()
                .jsonPath()
                .getString("hotel.checkInTime") ;
    }
    public Response execute() throws InterruptedException {
        List<String> bookingCodes = sharedData.getShowHotelRoomsResponse()
                .jsonPath()
                .getList("rooms.bookingCode");

        Assert.assertFalse(bookingCodes.isEmpty(), "No booking codes found");

        Response response = null;

        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
            System.out.println("\n=== Prebook Attempt " + attempt + "/" + MAX_RETRY_ATTEMPTS + " ===");

            response = tryAllBookingCodes(bookingCodes);

            if (response != null) {
                sharedData.setPreBookingResponse(response);
                return response;
            }

            if (attempt < MAX_RETRY_ATTEMPTS) {
                System.out.println("All codes failed. Retrying in " + RETRY_DELAY_MS + "ms...");
                Thread.sleep(RETRY_DELAY_MS);
            }
        }

        Assert.fail("All booking codes failed after " + MAX_RETRY_ATTEMPTS + " attempts");
        return null;
    }

    private Response tryAllBookingCodes(List<String> bookingCodes) throws InterruptedException {
        for (String code : bookingCodes) {
            System.out.println("Trying code: " + code);

            Response response = sendPreBookRequest(code);

            if (isSuccessfulPrebook(response)) {
                System.out.println("Success with code: " + code);
                return response;
            }

            Thread.sleep(1000);
        }
        return null;
    }

    private Response sendPreBookRequest(String bookingCode) {
        JSONObject body = buildPreBookBody(bookingCode);
        System.out.println("PreBook body: " + body);
        return given()
                .baseUri(BASE_URI)
                .basePath(PREBOOK_PATH)
                .relaxedHTTPSValidation()
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .body(body.toString())
                .post()
                .then()
                .extract()
                .response();
    }

    private JSONObject buildPreBookBody(String bookingCode) {
        return new JSONObject()
                .put("bookingCode", bookingCode)
                .put("bookingCodes", new JSONArray().put(bookingCode))
                .put("cacheKey", sharedData.getConnectionId())
                .put("checkIn",CheckInDate)
                .put("checkOut", CheckOutDate)
                .put("hotelId", sharedData.getLatestHotelId())
                .put("nationality", sharedData.getNationality())
                .put("passengers", sharedData.getPassengers())
                .put("rooms", new JSONArray()
                        .put(new JSONObject()
                                .put("adults", sharedData.getAdults())
                                .put("children", sharedData.getChildren())
                                .put("childrenAges", sharedData.getChildrenAges())))
                .put("payLater", false);
    }

    private boolean isSuccessfulPrebook(Response response) {
        try {
            if (response.getStatusCode() != 200) return false;

            String contentType = response.getHeader("Content-Type");
            if (contentType == null || !contentType.contains("application/json")) {
                System.out.println("Non-JSON response received");
                return false;
            }

            boolean success = response.jsonPath().getBoolean("success");
            String message = response.jsonPath().getString("message");

            return success && !NOT_AVAILABLE_MSG.equalsIgnoreCase(message);

        } catch (Exception e) {
            System.err.println("Error parsing response: " + e.getMessage());
            return false;
        }
    }
}