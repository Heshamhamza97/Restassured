package BaseTest;

import java.util.Base64;
import java.util.Random;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;

public class Base {

    private static final Random random = new Random();

    public static String generateRandomToken() {
        // 1️⃣ Create header
        JSONObject header = new JSONObject();
        header.put("alg", "HS256");
        header.put("typ", "JWT");
        String encodedHeader = base64UrlEncode(header.toString());

        // 2️⃣ Create payload
        JSONObject payload = new JSONObject();
        payload.put("email", "user" + random.nextInt(10000) + "@example.com");
        payload.put("nameid", String.valueOf(1000 + random.nextInt(9000)));
        payload.put("RoleId", String.valueOf(4000 + random.nextInt(1000)));
        payload.put("nbf", System.currentTimeMillis() / 1000);
        payload.put("exp", (System.currentTimeMillis() / 1000) + 86400); // +1 day
        payload.put("iss", "Depkey");
        payload.put("aud", "PPL");
        String encodedPayload = base64UrlEncode(payload.toString());

        // 3️⃣ Fake signature (random string)
        String signature = generateRandomBase64(43);

        // 4️⃣ Combine all parts
        return encodedHeader + "." + encodedPayload + "." + signature;
    }

    private static String base64UrlEncode(String data) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(data.getBytes(StandardCharsets.UTF_8));
    }

    private static String generateRandomBase64(int length) {
        byte[] bytes = new byte[length];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes).substring(0, length);
    }

    public static void main(String[] args) {
        System.out.println(generateRandomToken());
    }
}
