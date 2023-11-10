package napi.nvnmm.club.config;

import napi.nvnmm.club.util.configuration.StaticConfiguration;
import napi.nvnmm.club.util.configuration.defaults.MongoConfig;
import napi.nvnmm.club.util.configuration.defaults.RedisConfig;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
public class MainConfig implements StaticConfiguration {

    private final MongoConfig mongoConfig = new MongoConfig();
    private final RedisConfig redisConfig = new RedisConfig();
    private final List<UUID> oplist = new ArrayList<>();

}
