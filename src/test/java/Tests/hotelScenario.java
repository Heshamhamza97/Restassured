package Tests;

import Base.searchClass;
import Pages.*;
import Pages.hotelRequests.HotelBookingRequest;
import Pages.hotelRequests.HotelPreBook;
import Pages.hotelRequests.HotelRoomDetails;
import Pages.hotelRequests.HotelWaiter;
import org.testng.annotations.Test;

import java.net.URI;

import static Pages.hotelRequests.HotelWaiter.waitForHotelRoomsReady;

public class hotelScenario {
    @Test
    public void hotelBookingFlow() throws Exception {
        HotelSharedData data = new HotelSharedData();
        var response = searchClass.negotiate(1);
        var token = response.jsonPath().getString("connectionToken");

        // 1. Connect WebSocket and search
        HotelSocket socket = new HotelSocket(new URI(
                "wss://travelcore.techeffic.com/hubs/mobilehotelsearch?id=" +
                        token + "&access_token=" +
                        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9" +
                        ".eyJlbWFpbCI6Imhlc2hhbWhhbXphMTk5N0BnbWFpbC5jb20iLCJuYW1laWQiOiIxODQwIiwiUm9sZUlkIjoiNDM1MDQiLCJuYmYiOjE3NjAzNDMyOTMsImV4cCI6MTc2MDQyOTY5MywiaXNzIjoiRGVwa2V5IiwiYXVkIjoiUFBsIn0.xOECYVGcTWB9EA3dN80xStWXFhnZchKvwCenV9IW5h0;"
        ), data);
        socket.connectBlocking();
        socket.sendRegisterAndSearch();
        Thread.sleep(20000); // wait for results
        socket.close();

        // 2. Show hotel rooms
        new HotelRoomDetails(data).execute();
        waitForHotelRoomsReady(() -> new HotelRoomDetails(data).execute(), 30);

        // 3. Prebook
        new HotelPreBook(data).execute();

        // 4. Final booking
        new HotelBookingRequest(data).execute();
    }
}
