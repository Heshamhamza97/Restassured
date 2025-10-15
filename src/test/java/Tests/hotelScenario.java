package Tests;

import Base.searchClass;
import Pages.Hotel;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.net.URI;

public class hotelScenario {

    private Hotel client;

    @BeforeClass
    public void setup() throws Exception {

        // Negotiate connection token
        var response = searchClass.negotiate(1);
        var token = response.jsonPath().getString("connectionToken");

        // Build WebSocket URL
        String accessToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJlbWFpbCI6Imhlc2hhbWhhbXphMTk5N0BnbWFpbC5jb20iLCJuYW1laWQiOiIxODQwIiwiUm9sZUlkIjoiNDM1MDQiLCJuYmYiOjE3NjAzNDMyOTMsImV4cCI6MTc2MDQyOTY5MywiaXNzIjoiRGVwa2V5IiwiYXVkIjoiUFBsIn0.xOECYVGcTWB9EA3dN80xStWXFhnZchKvwCenV9IW5h0";
        String wsUrl = String.format(
                "wss://travelcore.techeffic.com/hubs/mobilehotelsearch?id=%s&access_token=%s",
                token,
                accessToken
        );

        client = new Hotel(new URI(wsUrl));

        client.connectBlocking();
        System.out.println("Connected to WebSocket successfully!");

        client.sendRegisterAndSearch();
        System.out.println("Search request sent via WebSocket...");
    }

    @Test
    public void hotelBooking() throws Exception {
        Thread.sleep(20000);
        System.out.println("Waiting for 'ReceiveHotelSearchFinished' message...");

        // Wait for hotel rooms to finish retrieve data
        client.waitForHotelRoomsReady(client::showHotelRooms, 30);
        System.out.println("Hotel rooms fetched successfully!");
        // Select any available room
        client.prebookHotel();
        System.out.println("Room pre-booked successfully!");
        // Initiate booking
        client.initiateBooking();
        System.out.println("Booking initiated successfully!");
    }

    @AfterClass(alwaysRun = true)
    public void tearDown() {
        if (client != null && client.isOpen()) {
            client.close();
            System.out.println("🔌 WebSocket connection closed.");
        }
    }
}
