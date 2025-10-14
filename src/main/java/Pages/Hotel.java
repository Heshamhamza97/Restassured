package Pages;

import Base.sharedData;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;

public class Hotel extends WebSocketClient {
    private final List<String> receivedMessages = new ArrayList<>();
    private String latestHotelId;
    private String checkIn;
    private String checkOut;
    private String nationality;
    private String passengers = null;
    private String connectionId;
    private String bookingCode;
    private String PrebookingCode;
    public Response showHotelRoomsResponse;
    public Hotel(URI serverUri) {
        super(serverUri);
    }


    @Override
    public void onOpen(ServerHandshake handshake) {
        System.out.println("✅ Connected to SignalR WebSocket");

        // Step 1: Send the SignalR handshake message
        String handshakeMessage = "{\"protocol\":\"json\",\"version\":1}\u001e";
        this.send(handshakeMessage);
        System.out.println("🤝 Sent SignalR handshake to server");
    }

    @Override
    public void onMessage(String message) {
        System.out.println("📩 Received message: " + message);
        receivedMessages.add(message);

        // Step 2: Wait for handshake ACK (server sends {})
        if (message.equals("{}\u001e")) {
            System.out.println("Handshake acknowledged — sending registration and search...");
            sendRegisterAndSearch();
        }
        // Step 3: Detect search results (optional)
        if (message.contains("\"target\":\"ReceiveHotelSearchFinished\"")) {
            System.out.println("Found hotel search results!");
            System.out.println(message);
        }
        if (message.contains("\"target\":\"ReceiveFirtPageHotelResult\"")) {
            try {
                JSONObject outer = new JSONObject(message);
                String innerJson = outer.getJSONArray("arguments").getString(0);
                JSONObject inner = new JSONObject(innerJson);

                JSONArray hotelResults = inner.getJSONArray("hotelResults");
                JSONObject firstHotel = hotelResults.getJSONObject(0);

                // Only proceed if "rooms" exist and contain "bookingCode"
                if (firstHotel.has("rooms")) {
                    JSONArray rooms = firstHotel.getJSONArray("rooms");

                    for (int i = 0; i < rooms.length(); i++) {
                        JSONObject room = rooms.getJSONObject(i);

                        // Proceed only if bookingCode is available
                        if (room.has("bookingCode") && !room.isNull("bookingCode")) {
                            bookingCode = room.getString("bookingCode");

                            // Extract other info only when bookingCode exists
                            String firstHotelId = firstHotel.getString("id");
                            latestHotelId = firstHotelId;

                            JSONObject hotelInfo = firstHotel.getJSONObject("hotel");
                            checkIn = hotelInfo.optString("checkInTime", "");
                            checkOut = hotelInfo.optString("checkOutTime", "");

                            int adults = room.optInt("adults", 1);
                            int children = room.optInt("children", 0);
                            passengers = adults + "-" + children;

                            System.out.println("✅ Found valid booking code: " + bookingCode);
                            break; // Stop once we find a valid one
                        }
                    }
                } else {
                    System.out.println("⚠️ No rooms found in message yet.");
                }

            } catch (Exception e) {
                System.out.println("⚠️ Failed to parse hotel result message:");
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("Connection closed: " + reason);
    }

    @Override
    public void onError(Exception ex) {
        System.out.println("WebSocket error:");
        ex.printStackTrace();
    }

    // 🔹 Step 4: Send RegisterConnection + SearchHotels (after handshake)
    public void sendRegisterAndSearch() {
        try {
           connectionId = UUID.randomUUID().toString();

            String combinedMessage = String.format(
                    "{\"arguments\":[\"%s\"],\"invocationId\":\"0\",\"target\":\"RegisterConnection\",\"type\":1}\u001e" +
                            "{\"arguments\":[\"%s\",{" +
                            "\"checkIn\":\"2025-10-15\"," +
                            "\"checkOut\":\"2025-10-21\"," +
                            "\"code\":\"417\"," +
                            "\"type\":2," +
                            "\"guestNationality\":\"70\"," +
                            "\"guestNationalityName\":\"Egypt\"," +
                            "\"hotelPassenger\":[{\"adults\":1,\"children\":0,\"childrenAges\":[]}]," +
                            "\"city\":\"Cairo\"," +
                            "\"country\":\"Egypt\"," +
                            "\"rate\":\"\"," +
                            "\"key\":\"\"," +
                            "\"source\":\"\"," +
                            "\"env\":\"dev\"}]," +
                            "\"invocationId\":\"1\",\"target\":\"SearchHotels\",\"type\":1}\u001e",
                    connectionId, connectionId
            );

            // optional small delay to ensure handshake is complete
            Thread.sleep(1000);

            this.send(combinedMessage);
            System.out.println("📤 Sent RegisterConnection + SearchHotels in a single message");
            System.out.println("🆔 Connection ID: " + connectionId);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            e.printStackTrace();
        }
    }
    public void showHotelRooms() {
        RestAssured.baseURI = sharedData.getBaseUri();
        nationality="70";
        String hotelId = latestHotelId;
        System.out.println("**********************" + hotelId);
        Response response = given()
                .relaxedHTTPSValidation()
                .basePath("/api/Hotel/HotelResultDetails/"+connectionId+"/"+hotelId+"/")
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .when()
                .body("{"
                        + "\"bookingCode\":\"\","
                        + "\"bookingCodes\":[],"
                        + "\"cacheKey\":\"" + connectionId + "\","
                        + "\"checkIn\":\"" + checkIn + "\","
                        + "\"checkOut\":\"" + checkOut + "\","
                        + "\"hotelId\":\"" + hotelId + "\","
                        + "\"nationality\":\"" + nationality + "\","
                        + "\"passengers\":\"" + passengers + "\","
                        + "\"rooms\":[]"
                        + "}")
                .post()
                .then()
                .extract().response();
        System.out.println("Status Code: " + response.getStatusCode());
        System.out.println("cache" + connectionId);
        response.prettyPrint();
        showHotelRoomsResponse = response;
    }
    public void prebookHotel() throws IOException, InterruptedException {
        RestAssured.baseURI = sharedData.getBaseUri();
        nationality="70";
        List<String> bookingCodes = showHotelRoomsResponse.getBody().jsonPath().getList("rooms.bookingCode");
        String firstBookingCode = bookingCodes.get(0);
        System.out.println(firstBookingCode);
        Response response = given()
                .relaxedHTTPSValidation()
                .basePath("/api/Hotel/PreBook")
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .body("{"
                        + "\"bookingCode\":\"" + firstBookingCode + "\","
                        + "\"bookingCodes\":[\"" + firstBookingCode + "\"],"
                        + "\"cacheKey\":\"" + connectionId + "\","
                        + "\"checkIn\":\"" + checkIn + "\","
                        + "\"checkOut\":\"" + checkOut + "\","
                        + "\"hotelId\":\"" + latestHotelId + "\","
                        + "\"nationality\":\"" + nationality + "\","
                        + "\"passengers\":\"1-0\","
                        + "\"rooms\":[{"
                        + "\"adults\":1,"
                        + "\"children\":0,"
                        + "\"childrenAges\":[]"
                        + "}],"
                        + "\"payLater\":false"
                        + "}")
                .when()
                .post()
                .then()
                .extract()
                .response();
        System.out.println("Status Code: " + response.getStatusCode());
        response.prettyPrint();
        Response PrebookResponse = response;
        PrebookingCode = PrebookResponse.getBody().jsonPath().getString("data.bookingCode");
    }
    public void intiateBooking() throws IOException, InterruptedException {
        RestAssured.baseURI = sharedData.getBaseUri();
        nationality="70";
        String nationalityName = "Egypt";

        // --- Build passenger object ---
        JSONObject passenger = new JSONObject();
        passenger.put("passengerTypeId", 43482);
        passenger.put("passengerTitleId", 43479);
        passenger.put("firstName", "Hesham");
        passenger.put("lastName", "test");
        passenger.put("dateOfBirth", "2010-07-02");
        passenger.put("roomNumber", 1);

        // --- Wrap passengers into rooms array ---
        JSONArray passengersArray = new JSONArray().put(passenger);
        JSONArray roomsArray = new JSONArray()
                .put(new JSONObject().put("passengers", passengersArray));

        // --- Build full body ---
        JSONObject body = new JSONObject();
        body.put("rooms", roomsArray);
        body.put("email", "heshamhamza1997@gmail.com");
        body.put("phone", "1030716233");
        body.put("phoneCodeId", 70);
        body.put("cacheKey", connectionId);
        body.put("bookingCode", PrebookingCode);
        body.put("checkIn", checkIn);
        body.put("checkOut", checkOut);
        body.put("currencyId", 43497);
        body.put("currencyRate", 1);
        body.put("deviceType", 0);
        body.put("hotelId", latestHotelId);
        body.put("languageId", 43499);
        body.put("priceChanged", false);
        body.put("couponCodeAmount", JSONObject.NULL);
        body.put("couponCodeId", JSONObject.NULL);
        body.put("isFreeBooking", false);
        body.put("isPayLater", false);
        body.put("isCanPay", true);
        body.put("isSendLink", false);
        body.put("isCreateLink", false);
        body.put("paymentMethodId", 1);
        body.put("smsFees", JSONObject.NULL);
        body.put("whatsappFees", JSONObject.NULL);
        body.put("paymentMethodFees", 1);
        body.put("nationality", nationality);
        body.put("nationalityName", nationalityName);
        body.put("termsAndCondtions", true);
        body.put("roomPassengers", "1-0");
        body.put("passengers", passengersArray);
        body.put("rateConditoins", new JSONArray());
        // --- Send POST request ---
        System.out.println("Booking code"+PrebookingCode);
        System.out.println("cache" + connectionId);
        System.out.println("######HotelId"+latestHotelId);
        System.out.println("request body"+body.toString());
        Response response = given()
                .relaxedHTTPSValidation()
                .basePath("/api/MobileHotels/BookingRequest") // Replace with your correct endpoint
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .body(body.toString()) // convert JSON object to string
                .when()
                .post()
                .then()
                .extract()
                .response();

        // --- Output ---
        System.out.println("Status Code: " + response.getStatusCode());
        System.out.println("Response Body:");
        System.out.println(response.asPrettyString());
    }
}

