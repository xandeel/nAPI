package napi.nvnmm.club.disguise.config;

import napi.nvnmm.club.util.configuration.StaticConfiguration;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class DisguiseConfig implements StaticConfiguration {

    private List<String> names = new ArrayList<>();

    private List<DisguiseSkinPreset> skins = new ArrayList<>();

}
