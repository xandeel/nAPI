package napi.nvnmm.club.banphrase;

import napi.nvnmm.club.nAPI;
import com.google.gson.JsonObject;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.Document;

import java.util.UUID;

@Data
@NoArgsConstructor
public class Banphrase {

    private UUID id;
    private String name;
    private String phrase;
    private String operator;
    private String muteMode;
    private long duration;
    private boolean enabled;
    private boolean caseSensitive;

    public Banphrase(Document document) {
        this.id = UUID.fromString(document.getString("id"));
        this.name = document.getString("name");
        this.phrase = document.getString("phrase");
        this.operator = document.getString("operator");
        this.muteMode = document.getString("muteMode");
        this.duration = document.get("duration", Number.class).longValue();
        this.enabled = document.getBoolean("enabled");
        this.caseSensitive = document.getBoolean("caseSensitive");

        if (!caseSensitive && !operator.equals("REGEX"))
            this.phrase = phrase.toLowerCase();
    }

    public Document toBson() {
        Document document = new Document();
        document.append("id", id.toString());
        document.append("name", name);
        document.append("phrase", phrase);
        document.append("operator", operator);
        document.append("muteMode", muteMode);
        document.append("duration", duration);
        document.append("enabled", enabled);
        document.append("caseSensitive", caseSensitive);
        return document;
    }

    public JsonObject toJson() {
        return nAPI.GSON.toJsonTree(this).getAsJsonObject();
    }


}
