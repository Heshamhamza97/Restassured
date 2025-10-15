package Pages;

import io.restassured.response.Response;

public class HotelSharedData {
    private String connectionId;
    private String latestHotelId;
    private String bookingCode;
    private String preBookingCode;
    private String baseUri = "https://travelcore.techeffic.com";
    private String checkIn = "2025-10-15";
    private String checkOut = "2025-10-21";
    private String nationality = "70";
    private int adults = 1;
    private int children = 0;
    private String passengers = adults + "-" + children;
    private Response showHotelRoomsResponse;
    private Response preBookingResponse;
    // --- Getters & Setters ---
    public String getConnectionId() { return connectionId; }
    public void setConnectionId(String connectionId) { this.connectionId = connectionId; }

    public String getLatestHotelId() { return latestHotelId; }
    public void setLatestHotelId(String latestHotelId) { this.latestHotelId = latestHotelId; }

    public String getBookingCode() { return bookingCode; }
    public void setBookingCode(String bookingCode) { this.bookingCode = bookingCode; }

    public String getPreBookingCode() { return preBookingCode; }
    public void setPreBookingCode(String preBookingCode) { this.preBookingCode = preBookingCode; }

    public String getBaseUri() { return baseUri; }
    public void setBaseUri(String baseUri) { this.baseUri = baseUri; }

    public String getCheckIn() { return checkIn; }
    public void setCheckIn(String checkIn) { this.checkIn = checkIn; }

    public String getCheckOut() { return checkOut; }
    public void setCheckOut(String checkOut) { this.checkOut = checkOut; }

    public String getNationality() { return nationality; }
    public void setNationality(String nationality) { this.nationality = nationality; }

    public int getAdults() { return adults; }
    public void setAdults(int adults) { this.adults = adults; }

    public int getChildren() { return children; }
    public void setChildren(int children) { this.children = children; }

    public String getPassengers() { return passengers; }

    public Response getShowHotelRoomsResponse() { return showHotelRoomsResponse; }
    public void setShowHotelRoomsResponse(Response showHotelRoomsResponse) { this.showHotelRoomsResponse = showHotelRoomsResponse; }

    public Response getPreBookingResponse() { return preBookingResponse; }
    public void setPreBookingResponse(Response preBookingResponse) { this.preBookingResponse = preBookingResponse; }
}
