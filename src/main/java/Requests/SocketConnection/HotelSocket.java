package Requests.SocketConnection;

import Requests.Config.HotelSharedData;
import io.restassured.response.Response;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;

public class HotelSocket extends WebSocketClient {
    private static final String HANDSHAKE_MSG = "{\"protocol\":\"json\",\"version\":1}\u001e";
    private static final String KEEP_ALIVE_MSG = "{\"type\":6}\u001e";
    private static final String REGISTER_MSG_TEMPLATE =
            "{\"arguments\":[\"%s\"],\"invocationId\":\"0\",\"target\":\"RegisterConnection\",\"type\":1}\u001e";
    private static final String REGISTER_SEARCH_TEMPLATE =
                    "{\"arguments\":[\"%s\",{\"checkIn\":\"%s\",\"checkOut\":\"%s\",\"code\":\"417\",\"type\":2," +
                    "\"guestNationality\":\"%s\",\"guestNationalityName\":\"Egypt\"," +
                    "\"hotelPassenger\":[{\"adults\":%d,\"children\":%d,\"childrenAges\":%s}]," +
                    "\"city\":\"Cairo\",\"country\":\"Egypt\",\"rate\":\"\",\"key\":\"\",\"source\":\"\",\"env\":\"dev\"}]," +
                    "\"invocationId\":\"1\",\"target\":\"SearchHotels\",\"type\":1}\u001e";

    private static final int SILENCE_THRESHOLD_MS = 5000;
    private static final String BASE_URI = "https://travelcore.techeffic.com";

    private final HotelSharedData sharedData;
    private final List<String> firstPageMessages = new ArrayList<>();
    private final CountDownLatch processingLatch = new CountDownLatch(1);

    private volatile long lastMessageTime = 0;
    private volatile boolean searchFinished = false;
    private volatile int pageCount = 0;
    private volatile float minPrice = 0;
    private volatile float maxPrice = 0;

    private Thread messageMonitor;

    public HotelSocket(URI serverUri, HotelSharedData sharedData, int provider) {
        super(serverUri);
        this.sharedData = sharedData;
        sharedData.setProvider(provider);
        sharedData.setConnectionId(UUID.randomUUID().toString());
    }

    @Override
    public void onOpen(ServerHandshake handshake) {
        lastMessageTime = System.currentTimeMillis();
        send(HANDSHAKE_MSG);
        send(KEEP_ALIVE_MSG);
        send(KEEP_ALIVE_MSG);
        send(REGISTER_MSG_TEMPLATE.formatted(sharedData.getConnectionId()));
        sendRegisterAndSearch();
        startMessageMonitor();
        return;
    }

    @Override
    public void onMessage(String message) {
        System.out.println("Received message: " + message);
        lastMessageTime = System.currentTimeMillis();

        try {

            JSONObject outer = new JSONObject(message);
            String target = outer.optString("target", "");

            if ("ReceiveFirtPageHotelResult".equals(target)) {
                synchronized (firstPageMessages) {
                    firstPageMessages.add(message);
                }
            }

            if ("ReceiveHotelSearchFinished".equals(target)) {
                processSearchStatus(outer);
            }

        } catch (Exception e) {
            // Silently ignore malformed messages
        }
    }

    private void processSearchStatus(JSONObject outer) {
        try {
            JSONArray args = outer.optJSONArray("arguments");
            if (args == null || args.isEmpty()) return;

            JSONObject inner = new JSONObject(args.getString(0));
            int count = inner.optInt("pageCount", 0);

            if (count > 0) {
                pageCount = count;
            }

            JSONObject filters = inner.optJSONObject("bookingFiltrationSummary");
            if (filters != null) {
                minPrice = filters.optFloat("minPrice", 0);
                maxPrice = filters.optFloat("maxPrice", 0);
                searchFinished = true;
            }
        } catch (Exception ignored) {}
    }

    private void startMessageMonitor() {
        messageMonitor = new Thread(() -> {
            try {
                while (!Thread.interrupted()) {
                    Thread.sleep(500);
                    if (searchFinished && (System.currentTimeMillis() - lastMessageTime) >= SILENCE_THRESHOLD_MS) {
                        processingLatch.countDown();
                        break;
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        messageMonitor.setDaemon(true);
        messageMonitor.start();
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        if (messageMonitor != null) messageMonitor.interrupt();
    }

    @Override
    public void onError(Exception ex) {
        System.err.println("WebSocket error: " + ex.getMessage());
    }

    public boolean waitForResponse(long timeoutSeconds) throws InterruptedException {
        if (!processingLatch.await(timeoutSeconds, TimeUnit.SECONDS)) {
            System.err.println("Timeout waiting for search completion");
            return false;
        }
        return processCollectedMessages();
    }

    private boolean processCollectedMessages() {
        System.out.println("Processing " + firstPageMessages.size() + " collected messages");

        synchronized (firstPageMessages) {
            for (String message : firstPageMessages) {
                if (searchInSocketMessage(message)) {
                    System.out.println("Found provider in socket message");
                    return true;
                }
            }
        }

        System.out.println("Provider not found in socket, checking pagination...");
        try {
            if (paginationFilter()) {
                return true;
            }
        } catch (Exception e) {
            System.err.println("Pagination failed: " + e.getMessage());
        }

        return false;
    }

    private boolean searchInSocketMessage(String message) {
        try {
            JSONObject outer = new JSONObject(message);
            JSONArray args = outer.optJSONArray("arguments");
            if (args == null || args.isEmpty()) return false;

            String innerJson = args.getString(0);
            JSONObject inner = new JSONObject(innerJson);
            JSONArray hotelResults = inner.optJSONArray("hotelResults");
            if (hotelResults == null || hotelResults.isEmpty()) return false;

            int targetProvider = sharedData.getProvider();

            for (int i = 0; i < hotelResults.length(); i++) {
                JSONObject hotelResult = hotelResults.optJSONObject(i);
                if (hotelResult == null) continue;

                JSONObject hotel = hotelResult.optJSONObject("hotel");
                if (hotel != null && hotel.optInt("provider", -1) == targetProvider) {
                    String hotelId = hotelResult.optString("id");
                    if (hotelId != null && !hotelId.isEmpty()) {
                        System.out.println("Found hotelId in socket: " + hotelId);
                        sharedData.setLatestHotelId(hotelId);
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to parse socket message: " + e.getMessage());
        }
        return false;
    }

    private void sendRegisterAndSearch() {
        String payload = String.format(REGISTER_SEARCH_TEMPLATE,
                sharedData.getConnectionId(),
                sharedData.getCheckIn(),
                sharedData.getCheckOut(),
                sharedData.getNationality(),
                sharedData.getAdults(),
                sharedData.getChildren(),
                sharedData.getChildrenAges()
        );
        System.out.println("Sending payload: " + payload);
        send(payload);
    }

    private boolean paginationFilter() throws InterruptedException {
        int targetProvider = sharedData.getProvider();
        String connectionId = sharedData.getConnectionId();
        String basePath = "/api/Hotel/filter/" + connectionId + "/2/417";

        for (int page = 2; page <= pageCount; page++) {
            System.out.println("Fetching page: " + page + "/" + pageCount);

            JSONObject body = new JSONObject()
                    .put("address", "")
                    .put("checkIn", sharedData.getCheckIn())
                    .put("checkOut", sharedData.getCheckOut())
                    .put("meal", new JSONArray())
                    .put("pageNumber", page)
                    .put("priceFrom", minPrice)
                    .put("priceTo", maxPrice)
                    .put("rating", new JSONArray());
            try {
                Response response = given()
                        .baseUri(BASE_URI)
                        .basePath(basePath)
                        .relaxedHTTPSValidation()
                        .header("Content-Type", "application/json")
                        .body(body.toString())
                        .post()
                        .then()
                        .extract()
                        .response();
                   response.prettyPrint();
                JSONArray hotelResults = new JSONObject(response.getBody().asString())
                        .optJSONArray("hotelResults");

        Thread.sleep(3000);
                if (hotelResults != null && searchInPaginationResults(hotelResults, targetProvider)) {
                    System.out.println("Found provider in pagination");
                    return true;
                }
            } catch (Exception e) {
                System.err.println("Error fetching page " + page + ": " + e.getMessage());
            }
        }
        System.out.println("Provider not found in any page");
        return false;
    }

    private boolean searchInPaginationResults(JSONArray hotelResults, int targetProvider) {
        for (int i = 0; i < hotelResults.length(); i++) {
            JSONObject hotelResult = hotelResults.optJSONObject(i);
            if (hotelResult == null) continue;

            JSONObject hotel = hotelResult.optJSONObject("hotel");
            if (hotel != null && hotel.optInt("provider", -1) == targetProvider) {
                String hotelId = hotelResult.optString("id");
                if (hotelId != null && !hotelId.isEmpty()) {
                    System.out.println("Found hotelId in pagination: " + hotelId);
                    sharedData.setLatestHotelId(hotelId);
                    return true;
                }
            }
        }
        return false;
    }
}