package napi.nvnmm.club.rank;

import napi.nvnmm.club.nAPI;
import com.google.gson.JsonObject;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
public class Rank {

    private UUID uuid;
    private String name;
    private String prefix = "§f";
    private String suffix = "§f";
    private String color = "§f";
    private String chatColor = "§f";
    private int weight = 0;
    private int queuePriority = 0;
    private boolean defaultRank = false;
    private boolean disguisable = false;
    private List<String> permissions = new ArrayList<>();
    private String discordId = null;
    private String staffDiscordId = null;
    private List<UUID> inherits = new ArrayList<>();

    public Rank(Document document) {
        this.uuid = UUID.fromString(document.getString("uuid"));
        this.name = document.getString("name");
        this.prefix = document.getString("prefix");
        this.suffix = document.getString("suffix");
        this.color = document.getString("color");
        this.chatColor = document.getString("chatColor");
        this.weight = document.getInteger("weight");
        if (document.containsKey("queuePriority"))
            this.queuePriority = document.getInteger("queuePriority");
        else queuePriority = weight;
        this.defaultRank = document.getBoolean("defaultRank");
        this.disguisable = document.getBoolean("disguisable");
        this.permissions = document.getList("permissions", String.class);
        this.discordId = document.getString("discordId");
        this.staffDiscordId = document.getString("staffDiscordId");
        this.inherits = document.getList("inherits", String.class).stream()
                .map(UUID::fromString)
                .collect(Collectors.toList());
        ;
    }

    public Document toBson() {
        Document document = new Document();
        document.append("uuid", uuid.toString());
        document.append("name", name);
        document.append("prefix", prefix);
        document.append("suffix", suffix);
        document.append("color", color);
        document.append("chatColor", chatColor);
        document.append("weight", weight);
        document.append("queuePriority", queuePriority);
        document.append("defaultRank", defaultRank);
        document.append("disguisable", disguisable);
        document.append("permissions", permissions);
        document.append("discordId", discordId);
        document.append("staffDiscordId", staffDiscordId);
        document.append("inherits", inherits.stream()
                .map(UUID::toString)
                .collect(Collectors.toList())
        );
        return document;
    }

    public JsonObject toJson() {
        return nAPI.GSON.toJsonTree(this).getAsJsonObject();
    }

    public List<String> getInheritedPermissions() {
        List<String> inheritedPermissions = new ArrayList<>();
        for (UUID inherit : inherits) {
            Optional<Rank> rankOpt = nAPI.getInstance().getRankService().getRank(inherit);
            if (rankOpt.isPresent())
                inheritedPermissions.addAll(rankOpt.get().getAllPermissions());
        }
        return inheritedPermissions;
    }

    public List<String> getAllPermissions() {
        List<String> allPermissions = new ArrayList<>();
        allPermissions.addAll(getInheritedPermissions());
        allPermissions.addAll(permissions);
        return allPermissions;
    }

    public String getWebColor() {
        String input = this.color.replaceAll("§", "")
                .replaceAll("l", "")
                .replaceAll("o", "")
                .replaceAll("n", "")
                .replaceAll("m", "")
                .replaceAll("k", "");

        switch (input) {
            case "0":
                return "#000000";
            case "1":
                return "#0000AA";
            case "2":
                return "#00AA00";
            case "3":
                return "#00AAAA";
            case "4":
                return "#AA0000";
            case "5":
                return "#AA00AA";
            case "6":
                return "#FFAA00";
            /*case "7":
                return "#AAAAAA";*/
            case "8":
                return "#555555";
            case "9":
                return "#5555FF";
            case "a":
                return "#55FF55";
            case "b":
                return "#55FFFF";
            case "c":
                return "#FF5555";
            case "d":
                return "#FF55FF";
            case "e":
                return "#FFFF55";
            case "7":
            case "f":
            default:
                //return "#FFFFFF";
                return "#AAAAAA";
        }
    }

}
