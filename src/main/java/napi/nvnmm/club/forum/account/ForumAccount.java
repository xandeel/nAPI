package napi.nvnmm.club.forum.account;

import napi.nvnmm.club.nAPI;
import com.google.gson.JsonObject;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.Document;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
public class ForumAccount {

    private UUID uuid;
    private String email;
    private String password;
    private String token;

    private Map<String, String> settings = new HashMap<>();

    public ForumAccount(Document document) {
        this.uuid = UUID.fromString(document.getString("uuid"));
        this.email = document.getString("email");
        this.password = document.getString("password");
        this.token = document.getString("token");

        Document settingsDocument = document.containsKey("settings")
                ? document.get("settings", Document.class)
                : new Document();

        settingsDocument.keySet().forEach(key -> settings.put(key, settingsDocument.getString(key)));
    }

    public Document toBson() {
        Document document = new Document();
        document.append("uuid", uuid.toString());
        document.append("email", email);
        document.append("password", password);
        document.append("token", token);

        Document settingsDocument = new Document();
        settings.forEach(settingsDocument::append);
        document.append("settings", settingsDocument);

        return document;
    }

    public JsonObject toJson() {
        return nAPI.GSON.toJsonTree(this).getAsJsonObject();
    }

}
