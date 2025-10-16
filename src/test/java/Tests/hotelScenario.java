package Tests;

import Requests.Config.HotelSharedData;
import Requests.SocketConnection.HotelSocket;
import Requests.SocketConnection.openSocket;
import Requests.HotelRequests.HotelBookingRequest;
import Requests.HotelRequests.HotelPreBook;
import Requests.HotelRequests.HotelRoomDetails;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import java.net.URI;
import java.net.URISyntaxException;

import static Requests.HotelRequests.HotelWaiter.waitForHotelRoomsReady;

public class hotelScenario {
    HotelSharedData data = new HotelSharedData();
    HotelSocket socket;

    @BeforeClass
    public void setup() throws URISyntaxException, InterruptedException {
        var response = openSocket.negotiate(1);
        var token = response.jsonPath().getString("connectionToken");

        // 1. Connect WebSocket and search
        socket = new HotelSocket(new URI(
                "wss://travelcore.techeffic.com/hubs/mobilehotelsearch?id=" +
                        token + "&access_token=" +
                        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9" +
                        ".eyJlbWFpbCI6Imhlc2hhbWhhbXphMTk5N0BnbWFpbC5jb20iLCJuYW1laWQiOiIxODQwIiwiUm9sZUlkIjoiNDM1MDQiLCJuYmYiOjE3NjAzNDMyOTMsImV4cCI6MTc2MDQyOTY5MywiaXNzIjoiRGVwa2V5IiwiYXVkIjoiUFBsIn0.xOECYVGcTWB9EA3dN80xStWXFhnZchKvwCenV9IW5h0;"
        ), data);
        socket.connectBlocking();
        Thread.sleep(20000);
    }
    @Test
    public void hotelBookingFlow() throws Exception {

        // 2. Show hotel rooms
        new HotelRoomDetails(data).execute();
        waitForHotelRoomsReady(() -> new HotelRoomDetails(data).execute(), 30);

        // 3. Prebook
        new HotelPreBook(data).execute();

        // 4. Final booking
        new HotelBookingRequest(data).execute();
    }

    @AfterClass
    public void tearDown(){
        socket.close();
    }
}
