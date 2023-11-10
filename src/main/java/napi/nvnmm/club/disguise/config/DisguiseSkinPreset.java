package napi.nvnmm.club.disguise.config;

import napi.nvnmm.club.nAPI;
import com.google.gson.JsonObject;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DisguiseSkinPreset {

    private String name = "N/A";
    private boolean hidden = false;
    private String texture;
    private String signature;

    public JsonObject toJson() {
        return nAPI.GSON.toJsonTree(this).getAsJsonObject();
    }

}
