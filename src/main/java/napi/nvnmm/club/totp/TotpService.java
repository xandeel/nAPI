package napi.nvnmm.club.totp;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import com.warrenstrange.googleauth.ICredentialRepository;
import napi.nvnmm.club.nAPI;
import napi.nvnmm.club.totp.repository.RedisCredentialRepository;
import napi.nvnmm.club.util.json.JsonBuilder;
import com.google.gson.JsonObject;
import lombok.Getter;

import java.util.UUID;

@Getter
public class TotpService {

    public static final String SECRET_KEY_FORMAT = "totp:%s:secretKey";
    public static final String VALIDATION_CODE_FORMAT = "totp:%s:validationCode";
    public static final String SCRATCH_CODES_FORMAT = "totp:%s:scratchCodes";
    public static final String CONFIRMED_ENABLED_FORMAT = "totp:%s:confirmedEnabled";

    private final nAPI api;
    private final GoogleAuthenticator googleAuthenticator;
    private final ICredentialRepository credentialRepository;

    public TotpService(nAPI api) {
        this.api = api;
        this.googleAuthenticator = new GoogleAuthenticator();
        this.credentialRepository = new RedisCredentialRepository(api);
        googleAuthenticator.setCredentialRepository(credentialRepository);
    }

    public JsonObject getData(UUID uuid) {
        return api.getRedisService().executeCommand(redis -> {
            JsonBuilder builder = new JsonBuilder();

            String format = String.format(SECRET_KEY_FORMAT, uuid.toString());
            if (redis.exists(format))
                builder.add("secretKey", redis.get(format));
            else {
                GoogleAuthenticatorKey credentials = googleAuthenticator.createCredentials(uuid.toString());
                builder.add("secretKey", credentials.getKey());
            }

            format = String.format(VALIDATION_CODE_FORMAT, uuid.toString());
            if (redis.exists(format))
                builder.add("validationCode", redis.get(format));

            format = String.format(SCRATCH_CODES_FORMAT, uuid.toString());
            if (redis.exists(format))
                builder.add("scratchCodes", redis.get(format));

            format = String.format(CONFIRMED_ENABLED_FORMAT, uuid.toString());
            if (redis.exists(format))
                builder.add("confirmedEnabled", Boolean.valueOf(redis.get(format)));

            return builder.build();
        });
    }

    public void confirmEnabled(UUID uuid) {
        api.getRedisService().executeCommand(redis ->
                redis.set(String.format(CONFIRMED_ENABLED_FORMAT, uuid.toString()), String.valueOf(true)));
    }

    public boolean hasCredentials(UUID uuid) {
        return api.getRedisService().executeCommand(redis ->
                redis.exists(String.format(SECRET_KEY_FORMAT, uuid.toString())));
    }

    public long deleteData(UUID uuid) {
        return api.getRedisService().executeCommand(redis -> {
            return redis.del(
                    String.format(SECRET_KEY_FORMAT, uuid.toString()),
                    String.format(VALIDATION_CODE_FORMAT, uuid.toString()),
                    String.format(SCRATCH_CODES_FORMAT, uuid.toString()),
                    String.format(CONFIRMED_ENABLED_FORMAT, uuid.toString())
            );
        });
    }

}
