package Pages;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class Hotel extends WebSocketClient {
    private final List<String> receivedMessages = new ArrayList<>();

    public Hotel(URI serverUri) {
        super(serverUri);
    }

    @Override
    public void onOpen(ServerHandshake handshake) {
        System.out.println("✅ Connected to SignalR WebSocket");

        // Step 1 — Send handshake
        String handshakeMessage = "{\"protocol\":\"json\",\"version\":1}\u001e";
        this.send(handshakeMessage);
        System.out.println("🤝 Sent SignalR handshake to server");

        // Step 2 — Wait a bit and send hotel search messages
        new Thread(() -> {
            try {
                Thread.sleep(1000); // Give server time to process handshake
                sendRegisterAndSearch(); // Automatically send messages
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    @Override
    public void onMessage(String message) {
        System.out.println("📩 Received message: " + message);
        getReceivedMessages().add(message);

        if (message.contains("\"target\":\"ReceiveHotelSearchFinished\"")) {
            System.out.println("\n🎯 Found hotel search results message!");
            System.out.println(message);
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("❌ Connection closed: " + reason);
    }

    @Override
    public void onError(Exception ex) {
        System.out.println("⚠️ WebSocket error:");
        ex.printStackTrace();
    }
    public void sendRegisterAndSearch() {
        try {
            String registerMsg = """
                {"arguments":["6cef850a-9b9b-46f6-b6be-1f81757ec2c1"],"invocationId":"0","target":"RegisterConnection","type":1}\u001e
            """;

            String searchMsg = """
                {"arguments":["6cef850a-9b9b-46f6-b6be-1f81757ec2c1",{
                    "checkIn":"2025-10-14",
                    "checkOut":"2025-10-20",
                    "code":"417",
                    "type":2,
                    "guestNationality":"70",
                    "guestNationalityName":"Egypt",
                    "hotelPassenger":[{"adults":1,"children":0,"childrenAges":[]}],
                    "city":"Cairo",
                    "country":"Egypt",
                    "rate":"",
                    "key":"",
                    "source":"",
                    "env":"dev"
                }],"invocationId":"1","target":"SearchHotels","type":1}\u001e
            """;

            // Send both
            this.send(registerMsg);
            Thread.sleep(1000);
            this.send(searchMsg);

            System.out.println("📤 Sent RegisterConnection and SearchHotels messages.");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            e.printStackTrace();
        }
    }
    public List<String> getReceivedMessages() {
        return receivedMessages;
    }
}
