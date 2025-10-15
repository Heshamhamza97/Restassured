package Pages;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class HotelSocket extends WebSocketClient {
    private final List<String> receivedMessages = new ArrayList<>();
    private final HotelSharedData sharedData;

    public HotelSocket(URI serverUri, HotelSharedData sharedData) {
        super(serverUri);
        this.sharedData = sharedData;
    }

    @Override
    public void onOpen(ServerHandshake handshake) {
        System.out.println("Connected to SignalR WebSocket");
        send("{\"protocol\":\"json\",\"version\":1}\u001e");
    }

    @Override
    public void onMessage(String message) {
        System.out.println("Received: " + message);
        receivedMessages.add(message);

        if (message.equals("{}\u001e")) {
            sendRegisterAndSearch();
            return;
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

    public void sendRegisterAndSearch() {
        try {
            sharedData.setConnectionId(UUID.randomUUID().toString());

            String payload = String.format(
                    "{\"arguments\":[\"%s\"],\"invocationId\":\"0\",\"target\":\"RegisterConnection\",\"type\":1}\u001e" +
                            "{\"arguments\":[\"%s\",{" +
                            "\"checkIn\":\"%s\",\"checkOut\":\"%s\",\"code\":\"417\",\"type\":2," +
                            "\"guestNationality\":\"%s\",\"guestNationalityName\":\"Egypt\"," +
                            "\"hotelPassenger\":[{\"adults\":1,\"children\":0,\"childrenAges\":[]}]," +
                            "\"city\":\"Cairo\",\"country\":\"Egypt\",\"rate\":\"\",\"source\":\"\",\"env\":\"dev\"}]," +
                            "\"invocationId\":\"1\",\"target\":\"SearchHotels\",\"type\":1}\u001e",
                    sharedData.getConnectionId(), sharedData.getConnectionId(),
                    sharedData.getCheckIn(), sharedData.getCheckOut(), sharedData.getNationality()
            );

            Thread.sleep(1000);
            send(payload);
            System.out.println("Sent RegisterConnection + SearchHotels | Connection ID: " + sharedData.getConnectionId());

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
            if (hotelResults == null || hotelResults.isEmpty()) return;

            JSONObject firstHotel = hotelResults.getJSONObject(0);
            sharedData.setLatestHotelId(firstHotel.getString("id"));

            JSONArray rooms = firstHotel.optJSONArray("rooms");
            if (rooms == null || rooms.isEmpty()) return;

            JSONObject room = rooms.getJSONObject(0);
            sharedData.setBookingCode(room.optString("bookingCode", ""));

            System.out.printf("Parsed Hotel ID: %s | Booking Code: %s%n",
                    sharedData.getLatestHotelId(), sharedData.getBookingCode());

        } catch (Exception e) {
            System.err.println("Failed to parse hotel result: " + e.getMessage());
        }
    }
}
