package Pages;

import Base.sharedData;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.Assert;
import java.net.URI;
import java.util.*;
import java.util.function.Supplier;

import static io.restassured.RestAssured.given;

public class Hotel extends WebSocketClient {

    // 🔹 Shared Data Fields
    private final List<String> receivedMessages = new ArrayList<>();
    private String latestHotelId;
    private String checkIn;
    private String checkOut;
    private final String nationality = "70";
    private String passengers = "1-0";
    private String connectionId;
    private String bookingCode;
    private String preBookingCode;

    // 🔹 Constants
    private static final int RETRY_INTERVAL_MS = 1000;

    // 🔹 Last REST response
    private Response showHotelRoomsResponse;

    public Hotel(URI serverUri) {
        super(serverUri);
    }

    // ------------------ WEBSOCKET Starting ------------------

    @Override
    public void onOpen(ServerHandshake handshake) {
        System.out.println("Connected to SignalR WebSocket");
        send("{\"protocol\":\"json\",\"version\":1}\u001e");
        System.out.println("Sent SignalR handshake to server");
    }

    @Override
    public void onMessage(String message) {
        System.out.println("Received: " + message);
        receivedMessages.add(message);

        if (message.equals("{}\u001e")) {
            System.out.println("Handshake acknowledged — sending registration and search...");
            sendRegisterAndSearch();
            return;
        }

        if (message.contains("\"target\":\"ReceiveHotelSearchFinished\"")) {
            System.out.println("Hotel search finished!");
        }

        if (message.contains("\"target\":\"ReceiveFirtPageHotelResult\"")) {
            parseHotelResult(message);
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("Connection closed: " + reason);
    }

    @Override
    public void onError(Exception ex) {
        System.err.println("WebSocket error:");
        ex.printStackTrace();
    }

    // ------------------ Request sent to socket ------------------

    public void sendRegisterAndSearch() {
        try {
            connectionId = UUID.randomUUID().toString();

            String payload = String.format(
                    "{\"arguments\":[\"%s\"],\"invocationId\":\"0\",\"target\":\"RegisterConnection\",\"type\":1}\u001e" +
                            "{\"arguments\":[\"%s\",{" +
                            "\"checkIn\":\"2025-10-15\",\"checkOut\":\"2025-10-21\",\"code\":\"417\",\"type\":2," +
                            "\"guestNationality\":\"70\",\"guestNationalityName\":\"Egypt\"," +
                            "\"hotelPassenger\":[{\"adults\":1,\"children\":0,\"childrenAges\":[]}]," +
                            "\"city\":\"Cairo\",\"country\":\"Egypt\",\"rate\":\"\",\"source\":\"\",\"env\":\"dev\"}]," +
                            "\"invocationId\":\"1\",\"target\":\"SearchHotels\",\"type\":1}\u001e",
                    connectionId, connectionId
            );

            Thread.sleep(1000);
            send(payload);

            System.out.println("Sent RegisterConnection + SearchHotels");
            System.out.println("Connection ID: " + connectionId);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void parseHotelResult(String message) {
        try {
            JSONObject outer = new JSONObject(message);
            String innerJson = outer.getJSONArray("arguments").getString(0);
            JSONObject inner = new JSONObject(innerJson);

            JSONArray hotelResults = inner.optJSONArray("hotelResults");
            if (hotelResults == null || hotelResults.isEmpty()) {
                System.out.println("No hotel results found yet.");
                return;
            }

            JSONObject firstHotel = hotelResults.getJSONObject(0);
            latestHotelId = firstHotel.getString("id");

            JSONArray rooms = firstHotel.optJSONArray("rooms");
            if (rooms == null || rooms.isEmpty()) {
                System.out.println("No rooms found yet.");
                return;
            }

            for (int i = 0; i < rooms.length(); i++) {
                JSONObject room = rooms.getJSONObject(i);
                if (room.has("bookingCode") && !room.isNull("bookingCode")) {
                    bookingCode = room.getString("bookingCode");

                    JSONObject hotelInfo = firstHotel.getJSONObject("hotel");
                    checkIn = hotelInfo.optString("checkInTime", "");
                    checkOut = hotelInfo.optString("checkOutTime", "");

                    int adults = room.optInt("adults", 1);
                    int children = room.optInt("children", 0);
                    passengers = adults + "-" + children;

                    System.out.printf("Hotel ID: %s | Booking Code: %s%n", latestHotelId, bookingCode);
                    break;
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to parse hotel result message");
            e.printStackTrace();
        }
    }

    // ------------------ REST ASSURED Booking REQUESTS ------------------

    public Response showHotelRooms() {
        RestAssured.baseURI = sharedData.getBaseUri();
        Assert.assertNotNull(latestHotelId, "latestHotelId is null — ensure hotel search completed.");

        Response response = given()
                .relaxedHTTPSValidation()
                .basePath("/api/Hotel/HotelResultDetails/" + connectionId + "/" + latestHotelId + "/")
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .body(new JSONObject()
                        .put("bookingCode", "")
                        .put("bookingCodes", new JSONArray())
                        .put("cacheKey", connectionId)
                        .put("checkIn", checkIn)
                        .put("checkOut", checkOut)
                        .put("hotelId", latestHotelId)
                        .put("nationality", nationality)
                        .put("passengers", passengers)
                        .put("rooms", new JSONArray())
                        .toString())
                .post()
                .then()
                .extract().response();

        System.out.println("Status Code: " + response.getStatusCode());
        Assert.assertEquals(response.statusCode(), 200, "Expected status 200 for showHotelRooms");

        showHotelRoomsResponse = response;
        return response;
    }

    public void waitForHotelRoomsReady(Supplier<Response> supplier, int timeoutSeconds) {
        long startTime = System.currentTimeMillis();

        while ((System.currentTimeMillis() - startTime) < timeoutSeconds * 1000) {
            Response response = supplier.get();
            try {
                List<String> bookingCodes = response.jsonPath().getList("rooms.bookingCode");
                if (bookingCodes != null && !bookingCodes.isEmpty()) {
                    System.out.println("Booking codes available: " + bookingCodes);
                    return;
                }
            } catch (Exception ignored) {}

            System.out.println("Waiting for room data...");
            try { Thread.sleep(RETRY_INTERVAL_MS); } catch (InterruptedException ignored) {}
        }

        throw new AssertionError("Timeout waiting for hotel rooms data!");
    }

    public Response prebookHotel() {
        RestAssured.baseURI = sharedData.getBaseUri();
        List<String> bookingCodes = showHotelRoomsResponse.jsonPath().getList("rooms.bookingCode");
        Assert.assertFalse(bookingCodes.isEmpty(), "No booking codes found in hotel rooms response");

        System.out.println("🔍 Found " + bookingCodes.size() + " booking codes to try...");

        Response response = null;
        String successfulCode = null;

        for (String code : bookingCodes) {
            System.out.println("Trying to prebook using code: " + code);

            // Build request body
            JSONObject body = new JSONObject()
                    .put("bookingCode", code)
                    .put("bookingCodes", new JSONArray().put(code))
                    .put("cacheKey", connectionId)
                    .put("checkIn", checkIn)
                    .put("checkOut", checkOut)
                    .put("hotelId", latestHotelId)
                    .put("nationality", nationality)
                    .put("passengers", passengers)
                    .put("rooms", new JSONArray()
                            .put(new JSONObject()
                                    .put("adults", 1)
                                    .put("children", 0)
                                    .put("childrenAges", new JSONArray())))
                    .put("payLater", false);

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

            System.out.println("🔎 Prebook response for code " + code + ":");
            response.prettyPrint();

            int statusCode = response.getStatusCode();
            boolean success = false;
            String message = null;
            try {
                success = response.jsonPath().getBoolean("success");
                message = response.jsonPath().getString("message");
            } catch (Exception e) {
                System.out.println("⚠️ Could not parse response JSON safely.");
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

        preBookingCode = response.jsonPath().getString("data.bookingCode");
        Assert.assertNotNull(preBookingCode, "Prebooking code is null after all retries");
        return response;
    }
    public void initiateBooking() {
        RestAssured.baseURI = sharedData.getBaseUri();

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
                .put("cacheKey", connectionId)
                .put("bookingCode", preBookingCode)
                .put("checkIn", checkIn)
                .put("checkOut", checkOut)
                .put("currencyId", 43497)
                .put("currencyRate", 1)
                .put("deviceType", 0)
                .put("hotelId", latestHotelId)
                .put("languageId", 43499)
                .put("priceChanged", false)
                .put("isPayLater", false)
                .put("isCanPay", true)
                .put("paymentMethodId", 1)
                .put("nationality", nationality)
                .put("nationalityName", "Egypt")
                .put("termsAndCondtions", true)
                .put("roomPassengers", "1-0")
                .put("passengers", new JSONArray().put(passenger))
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

        System.out.println("Booking response:");
        response.prettyPrint();

        Assert.assertEquals(response.getStatusCode(), 200, "Expected status code 200 for final booking");
        Assert.assertNotNull(response.jsonPath().getString("data.bookingId"), "Booking ID is null");

    }
}
