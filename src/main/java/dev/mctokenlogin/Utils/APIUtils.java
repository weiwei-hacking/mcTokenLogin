package dev.mctokenlogin.Utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class APIUtils {
    private static final CloseableHttpClient HTTP_CLIENT = HttpClients.createDefault();
    private static final String PROFILE_API = "https://api.minecraftservices.com/minecraft/profile";
    private static final String NAME_CHANGE_API = "https://api.minecraftservices.com/minecraft/profile/name/";
    private static final String SKIN_CHANGE_API = "https://api.minecraftservices.com/minecraft/profile/skins";
    private static final String SLOTHPIXEL_API = "https://api.slothpixel.me/api/players/";

    public static String[] getProfileInfo(String token) throws IOException {
        try (CloseableHttpResponse response = HTTP_CLIENT.execute(createProfileRequest(token))) {
            if (response.getStatusLine().getStatusCode() != 200) {
                throw new IOException("API request failed with status: " + response.getStatusLine().getStatusCode());
            }
            String jsonString = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            JsonObject jsonObject = new JsonParser().parse(jsonString).getAsJsonObject();
            if (!jsonObject.has("name") || !jsonObject.has("id")) {
                throw new IOException("Invalid API response: missing name or id");
            }
            return new String[]{jsonObject.get("name").getAsString(), jsonObject.get("id").getAsString()};
        }
    }

    public static boolean validateSession(String token) {
        try {
            String[] profileInfo = getProfileInfo(token);
            String inGameName = profileInfo[0];
            String uuid = profileInfo[1];
            return inGameName.equals(Minecraft.getMinecraft().getSession().getUsername()) &&
                   uuid.equals(Minecraft.getMinecraft().getSession().getPlayerID());
        } catch (Exception e) {
            System.err.println("Failed to validate session: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public static boolean checkOnline(String username) {
        try (CloseableHttpResponse response = HTTP_CLIENT.execute(new HttpGet(SLOTHPIXEL_API + username))) {
            if (response.getStatusLine().getStatusCode() != 200) {
                throw new IOException("API request failed with status: " + response.getStatusLine().getStatusCode());
            }
            String jsonString = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            JsonObject jsonObject = new JsonParser().parse(jsonString).getAsJsonObject();
            return jsonObject.has("online") && jsonObject.get("online").getAsBoolean();
        } catch (Exception e) {
            System.err.println("Failed to check online status: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public static int changeName(String newName, String token) throws IOException {
        try (CloseableHttpResponse response = HTTP_CLIENT.execute(createNameChangeRequest(newName, token))) {
            return response.getStatusLine().getStatusCode();
        }
    }

    public static int changeSkin(String url, String token) throws IOException {
        try (CloseableHttpResponse response = HTTP_CLIENT.execute(createSkinChangeRequest(url, token))) {
            return response.getStatusLine().getStatusCode();
        }
    }

    private static HttpGet createProfileRequest(String token) {
        HttpGet request = new HttpGet(PROFILE_API);
        request.setHeader("Authorization", "Bearer " + token);
        return request;
    }

    private static HttpPut createNameChangeRequest(String newName, String token) {
        HttpPut request = new HttpPut(NAME_CHANGE_API + newName);
        request.setHeader("Authorization", "Bearer " + token);
        return request;
    }

    private static HttpPost createSkinChangeRequest(String url, String token) {
        HttpPost request = new HttpPost(SKIN_CHANGE_API);
        request.setHeader("Authorization", "Bearer " + token);
        request.setHeader("Content-Type", "application/json");
        String jsonString = String.format("{ \"variant\": \"classic\", \"url\": \"%s\"}", url);
        try {
            request.setEntity(new StringEntity(jsonString));
        } catch (Exception e) {
            System.err.println("Failed to create skin change request: " + e.getMessage());
            e.printStackTrace();
        }
        return request;
    }
}
