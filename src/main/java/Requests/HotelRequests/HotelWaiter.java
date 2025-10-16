package Requests.HotelRequests;
import io.restassured.response.Response;
import java.util.List;
import java.util.function.Supplier;

public class HotelWaiter {
    private static final int RETRY_INTERVAL_MS = 1000;

    public static void waitForHotelRoomsReady(Supplier<Response> supplier, int timeoutSeconds) {
        long start = System.currentTimeMillis();

        while ((System.currentTimeMillis() - start) < timeoutSeconds * 1000) {
            Response response = supplier.get();
            List<String> bookingCodes = null;
            try {
                bookingCodes = response.jsonPath().getList("rooms.bookingCode");
            } catch (Exception ignored) {}

            if (bookingCodes != null && !bookingCodes.isEmpty()) {
                System.out.println("Booking codes available: " + bookingCodes);
                return;
            }

            System.out.println("⏳ Waiting for room data...");
            try { Thread.sleep(RETRY_INTERVAL_MS); } catch (InterruptedException ignored) {}
        }

        throw new AssertionError("Timeout waiting for hotel rooms data!");
    }
}
