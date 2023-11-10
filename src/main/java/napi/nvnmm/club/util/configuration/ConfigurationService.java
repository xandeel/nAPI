package napi.nvnmm.club.util.configuration;

import java.io.File;
import java.io.IOException;

public interface ConfigurationService {

    void saveConfiguration(StaticConfiguration configuration, File file) throws IOException;

    <T extends StaticConfiguration> T loadConfiguration(Class<? extends T> clazz, File file);

}
