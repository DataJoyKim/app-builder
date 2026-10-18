package com.prometis.appbuilder.console.app.deploy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * 저장소 토큰 암복호화 (AES-GCM). 토큰은 GitHub 에 다시 보내야 하므로 해시가 아니라 되돌릴 수 있게 암호화한다.
 * 키는 platform.deploy.token-secret-key 를 SHA-256 한 값이고, 저장값은 base64(iv + 암호문)이다.
 */
@Slf4j
@Component
public class DeployTokenCipher {
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_BITS = 128;
    private static final String DEV_SECRET_KEY = "app-builder-deploy-dev-secret";

    private final SecretKeySpec key;
    private final SecureRandom random = new SecureRandom();

    public DeployTokenCipher(DeployProperties properties) {
        String secret = properties.tokenSecretKey();
        if(secret == null || secret.isBlank()) {
            log.warn("platform.deploy.token-secret-key 가 없어 개발용 키로 저장소 토큰을 암호화합니다. 운영에서는 반드시 설정하세요.");
            secret = DEV_SECRET_KEY;
        }

        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8));
            this.key = new SecretKeySpec(hash, "AES");
        }
        catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    public String encrypt(String plainText) {
        try {
            byte[] iv = new byte[IV_LENGTH];
            random.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            byte[] result = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, result, 0, iv.length);
            System.arraycopy(encrypted, 0, result, iv.length, encrypted.length);
            return Base64.getEncoder().encodeToString(result);
        }
        catch (GeneralSecurityException e) {
            throw new IllegalStateException("토큰을 암호화하지 못했습니다.", e);
        }
    }

    /**
     * @throws DeployException 키가 바뀌어 풀 수 없을 때
     */
    public String decrypt(String encryptedText) {
        try {
            byte[] data = Base64.getDecoder().decode(encryptedText);
            byte[] iv = Arrays.copyOfRange(data, 0, IV_LENGTH);
            byte[] encrypted = Arrays.copyOfRange(data, IV_LENGTH, data.length);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        }
        catch (GeneralSecurityException | IllegalArgumentException e) {
            log.warn("저장소 토큰을 복호화하지 못했습니다. platform.deploy.token-secret-key 가 바뀌었는지 확인하세요.");
            throw new DeployException(400, "저장된 토큰을 읽을 수 없습니다. 연동 설정에서 토큰을 다시 입력해주세요.");
        }
    }
}
