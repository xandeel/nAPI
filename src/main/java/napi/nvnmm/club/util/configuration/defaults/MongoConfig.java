package napi.nvnmm.club.util.configuration.defaults;

import napi.nvnmm.club.util.configuration.StaticConfiguration;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MongoConfig implements StaticConfiguration {

    private String host = "localhost";
    private int port = 27017;
    private boolean authEnabled = false;
    private String authUsername = "username";
    private String authPassword = "password";
    private String authDatabase = "admin";

}
