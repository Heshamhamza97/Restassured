package Requests.Config;
import com.github.javafaker.Faker;
import io.restassured.response.Response;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class HotelSharedData {
    Faker faker = new Faker();
    private String connectionId;
    private String latestHotelId;
    private static String baseUri = "https://travelcore.techeffic.com";
    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
    private String checkInDate;
    private String checkOutDate;
    private String nationality = "70";
    private int adults = faker.number().numberBetween(1,3);
    private int children = faker.number().numberBetween(0,adults);
    private JSONObject passengersData=new JSONObject();
    private Response showHotelRoomsResponse;
    private Response preBookingResponse;
    private int provider;

    public int getProvider() {
        setProvider(provider);
        return provider; }

    public void setProvider(int provider) { this.provider = provider;
     int [] arr = {49259,47568,47569,48434};
    for (int i = 0; i < arr.length; i++) {
        if (arr[i] == provider) {
            this.provider = arr[i];
            break;
        }
    }
    }
    public List<Integer> getChildrenAges() {
        return (children > 0)
                ? new ArrayList<>(Collections.nCopies(children, 0))
                : new ArrayList<>();
    }
    // --- Getters & Setters ---

    public String getConnectionId() { return connectionId; }
    public void setConnectionId(String connectionId) { this.connectionId = connectionId; }

    public String getLatestHotelId() { return latestHotelId; }
    public void setLatestHotelId(String latestHotelId) { this.latestHotelId = latestHotelId; }

    public static String getBaseUri() { return baseUri; }

    public void setCheckInDate(String checkInDate) { this.checkInDate = checkInDate; }

    public void setCheckOutDate(String checkOutDate) { this.checkOutDate = checkOutDate; }


    public String getCheckIn() {
        if (checkInDate == null) {
            Date date = faker.date().future(5, TimeUnit.DAYS);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            checkInDate = sdf.format(date);
        }
        return checkInDate;
    }

    public String getCheckOut() {
        if (checkOutDate == null) {
            if (checkInDate == null) {
                getCheckIn(); // ensure check-in is generated first
            }

            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                Date checkIn = sdf.parse(checkInDate); // parse string to Date

                Date minCheckoutDate = Date.from(checkIn.toInstant().plus(7, ChronoUnit.DAYS));

                // Generate a random checkout date after minCheckoutDate
                Date randomCheckout = faker.date().future(10, TimeUnit.DAYS, minCheckoutDate);

                // ✅ Format that Date into a String
                checkOutDate = sdf.format(randomCheckout);

            } catch (Exception e) {
                throw new RuntimeException("Error generating checkout date", e);
            }
        }
        return checkOutDate;
    }

    public void setPassengersData() {
        passengersData.put("passengerTypeId", 43482);
        passengersData.put("passengerTitleId", 43479);
        passengersData.put("firstName", faker.name().firstName());
        passengersData.put("lastName", faker.name().lastName());
        passengersData.put("dateOfBirth", formatter.format(faker.date().birthday()));
        passengersData.put("roomNumber", 1);
    }

    public JSONObject getPassengersData() {
        setPassengersData();
        return passengersData; }

    public String getNationality() { return nationality; }

    public int getAdults() { return adults; }

    public int getChildren() { return children; }
    public void setChildren(int children) { this.children = children; }

    public String getPassengers() {
        StringBuilder passengersBuilder = new StringBuilder();

        passengersBuilder.append(adults).append("-").append(children);

        List<Integer> childrenAges = getChildrenAges();
        if (children > 0 && childrenAges != null && !childrenAges.isEmpty()) {
            for (Integer age : childrenAges) {
                passengersBuilder.append("-").append(age);
            }
        }

        return passengersBuilder.toString();
    }


    public Response getShowHotelRoomsResponse() { return showHotelRoomsResponse; }
    public void setShowHotelRoomsResponse(Response showHotelRoomsResponse) { this.showHotelRoomsResponse = showHotelRoomsResponse; }

    public Response getPreBookingResponse() { return preBookingResponse; }
    public void setPreBookingResponse(Response preBookingResponse) { this.preBookingResponse = preBookingResponse; }
}
