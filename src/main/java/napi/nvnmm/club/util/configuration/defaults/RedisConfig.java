package napi.nvnmm.club.util.configuration.defaults;

import napi.nvnmm.club.util.configuration.StaticConfiguration;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class RedisConfig implements StaticConfiguration {

    private String host = "localhost";
    private int port = 6379;
    private boolean authEnabled = false;
    private String authPassword = "password";
    private int dbId = 0;

}
