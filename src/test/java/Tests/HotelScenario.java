package Tests;

import Requests.Config.HotelSharedData;
import Requests.SocketConnection.HotelSocket;
import Requests.SocketConnection.HotelSocketStatic;
import Requests.SocketConnection.openSocket;
import Requests.HotelRequests.*;
import org.testng.annotations.Test;
import org.java_websocket.client.WebSocketClient;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import static Requests.HotelRequests.HotelWaiter.waitForHotelRoomsReady;

public class HotelScenario {
    private final HotelSharedData data = new HotelSharedData();
    private static final int MAX_ATTEMPTS = 3;
    private static final int RETRY_DELAY_MS = 5000;
    private static final int CONNECT_TIMEOUT_SECONDS = 100;
    private static final int RESPONSE_TIMEOUT_SECONDS = 60;
    private static final int ROOM_READY_TIMEOUT_SECONDS = 30;

    @Test
    public void hotelBookingFlow() throws Exception {
        executeHotelBookingFlow(() -> {
            try {
                String token = openSocket.negotiate(1).jsonPath().getString("connectionToken");
                return new HotelSocket(
                        new URI("wss://travelcore.techeffic.com/hubs/mobilehotelsearch?id=" + token),
                        data,48434);
            } catch (URISyntaxException e) {
                throw new RuntimeException("Failed to create socket", e);
            }
        });
    }

    @Test
    public void testStatic() throws Exception {
        executeHotelBookingFlow(() -> {
            try {
                String token = openSocket.negotiate(1).jsonPath().getString("connectionToken");
                return new HotelSocketStatic(
                        new URI("wss://travelcore.techeffic.com/hubs/mobilehotelsearch?id=" + token),
                        data,
                        49259,
                        "2025-10-26",
                        "2025-11-01",
                        "Kuwait"
                );
            } catch (URISyntaxException e) {
                throw new RuntimeException("Failed to create socket", e);
            }
        });
    }

    private void executeHotelBookingFlow(Supplier<? extends WebSocketClient> socketSupplier) throws Exception {
        connectWithRetry(socketSupplier);
        executeBookingSteps();
    }

    private void connectWithRetry(Supplier<? extends WebSocketClient> socketSupplier) throws InterruptedException {
        boolean found = false;

        for (int attempt = 1; attempt <= MAX_ATTEMPTS && !found; attempt++) {
            System.out.println("\n=== Attempt " + attempt + "/" + MAX_ATTEMPTS + " ===");

            WebSocketClient socket = socketSupplier.get();
            socket.connectBlocking(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            // Cast to appropriate type to call waitForResponse
            if (socket instanceof HotelSocket) {
                found = ((HotelSocket) socket).waitForResponse(RESPONSE_TIMEOUT_SECONDS);
            } else if (socket instanceof HotelSocketStatic) {
                found = ((HotelSocketStatic) socket).waitForResponse(RESPONSE_TIMEOUT_SECONDS);
            }

            socket.close();

            if (!found && attempt < MAX_ATTEMPTS) {
                System.out.println("Retrying in " + (RETRY_DELAY_MS / 1000) + " seconds...");
                Thread.sleep(RETRY_DELAY_MS);
            }
        }

        if (!found) {
            throw new RuntimeException("Provider not found after " + MAX_ATTEMPTS + " attempts");
        }
    }

    private void executeBookingSteps() throws Exception {
        HotelRoomDetails roomDetails = new HotelRoomDetails(data);
        roomDetails.execute();
        waitForHotelRoomsReady(roomDetails::execute, ROOM_READY_TIMEOUT_SECONDS);
        new HotelPreBook(data).execute();
        new HotelBookingRequest(data).execute();
    }
}