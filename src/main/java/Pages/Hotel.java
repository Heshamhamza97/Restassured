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
                // Step 1: Parse outer message
                JSONObject outer = new JSONObject(message);
                // Step 2: Extract arguments[0] (stringified JSON)
                String innerJson = outer.getJSONArray("arguments").getString(0);
                // Step 3: Parse the inner JSON string
                JSONObject inner = new JSONObject(innerJson);

                // Step 4: Get first hotel result ID
                JSONArray hotelResults = inner.getJSONArray("hotelResults");
                String firstHotelId = hotelResults.getJSONObject(0).getString("id");
                JSONObject hotel = hotelResults.getJSONObject(0);
                JSONObject hotelInfo = hotel.getJSONObject("hotel");
                String CheckInDate = hotelInfo.getString("checkInTime");
                String CheckOutDate = hotelInfo.getString("checkOutTime");

                JSONArray roomResults = hotel.getJSONArray("rooms");
                int adults = roomResults.getJSONObject(0).getInt("adults");
                int children = roomResults.getJSONObject(0).getInt("children");
               bookingCode = inner.getJSONArray("hotelResults")
                        .getJSONObject(0)
                        .getJSONArray("rooms")
                        .getJSONObject(0)
                        .getString("bookingCode");
               latestHotelId = firstHotelId;
                checkIn = CheckInDate;
                checkOut = CheckOutDate;
                passengers = adults + "-" + children;

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
    }
    public void prebookHotel() throws IOException, InterruptedException {
        RestAssured.baseURI = sharedData.getBaseUri();
        nationality="70";
        Response response = given()
                .relaxedHTTPSValidation()
                .basePath("/api/Hotel/PreBook")
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .body("{"
                        + "\"bookingCode\":\"" + bookingCode + "\","
                        + "\"bookingCodes\":[\"" + bookingCode + "\"],"
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
    }
    public void intiateBooking() throws IOException, InterruptedException {
        RestAssured.baseURI = sharedData.getBaseUri();
        nationality="70";
        String nationalityName = "Egypt";

        // --- Build passenger object ---
        JSONObject passenger = new JSONObject();
        passenger.put("passengerTypeId", 43482);
        passenger.put("passengerTitleId", 43479);
        passenger.put("firstName", "Kimberly");
        passenger.put("lastName", "test");
        passenger.put("dateOfBirth", "2008-07-02");
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
        body.put("bookingCode", bookingCode);
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
        body.put("source", "mobile");
        // --- Send POST request ---
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

