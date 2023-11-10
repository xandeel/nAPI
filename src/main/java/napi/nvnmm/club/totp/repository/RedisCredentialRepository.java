package napi.nvnmm.club.totp.repository;

import napi.nvnmm.club.nAPI;
import napi.nvnmm.club.totp.TotpService;
import com.warrenstrange.googleauth.ICredentialRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

@RequiredArgsConstructor
public class RedisCredentialRepository implements ICredentialRepository {

    private final nAPI api;

    @Override
    public String getSecretKey(String userName) {
        return api.getRedisService().executeCommand(redis ->
                redis.get(String.format(TotpService.SECRET_KEY_FORMAT, userName)));
    }

    @Override
    public void saveUserCredentials(String userName, String secretKey, int validationCode, List<Integer> scratchCodes) {
        api.getRedisService().executeCommand(redis -> {
            redis.set(String.format(TotpService.SECRET_KEY_FORMAT, userName), secretKey);
            redis.set(String.format(TotpService.VALIDATION_CODE_FORMAT, userName), String.valueOf(validationCode));
            redis.set(String.format(TotpService.SCRATCH_CODES_FORMAT, userName), StringUtils.join(scratchCodes, ","));
            return null;
        });
    }
}
