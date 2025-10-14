package Tests;

import Base.searchClass;
import Pages.Hotel;
import org.testng.annotations.Test;

import java.net.URI;

public class hotelScenario {

    @Test
    public void hotelBooking() throws Exception {
        var response = searchClass.negotiate(1
        );

        var token = response.jsonPath().getString("connectionToken");
        String accessToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJlbWFpbCI6Imhlc2hhbWhhbXphMTk5N0BnbWFpbC5jb20iLCJuYW1laWQiOiIxODQwIiwiUm9sZUlkIjoiNDM1MDQiLCJuYmYiOjE3NjAzNDMyOTMsImV4cCI6MTc2MDQyOTY5MywiaXNzIjoiRGVwa2V5IiwiYXVkIjoiUFBsIn0.xOECYVGcTWB9EA3dN80xStWXFhnZchKvwCenV9IW5h0";
        String wsUrl = String.format(
                "wss://travelcore.techeffic.com/hubs/mobilehotelsearch?id=%s&access_token=%s",
                token,
                accessToken
        );
        Hotel client = new Hotel(new URI(wsUrl));

        client.connectBlocking();

        client.sendRegisterAndSearch();

        Thread.sleep(30000);
        client.close();

        client.showHotelRooms();
        Thread.sleep(1000);
        client.prebookHotel();
        Thread.sleep(1000);
        client.intiateBooking();
    }
}