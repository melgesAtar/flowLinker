package br.com.flowlinkerAPI.config.crypto;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

public final class EncryptionUtils {

    private static volatile SecretKey secretKey;
    private static final int IV_LENGTH_BYTES = 12; // recomendado para GCM
    private static final int TAG_LENGTH_BITS = 128;

    private EncryptionUtils() {}

    public static void init(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("encryption.secret não configurado");
        }
        // Deriva/ajusta chave para 256 bits a partir da string fornecida
        byte[] keyBytes = new byte[32];
        byte[] src = secret.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        System.arraycopy(src, 0, keyBytes, 0, Math.min(src.length, keyBytes.length));
        secretKey = new SecretKeySpec(keyBytes, "AES");
    }

    public static String encrypt(String plainText) {
        if (plainText == null) return null;
        ensureKey();
        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];
            new SecureRandom().nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec spec = new GCMParameterSpec(TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec);
            byte[] cipherText = cipher.doFinal(plainText.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            byte[] out = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(cipherText, 0, out, iv.length, cipherText.length);
            return Base64.getEncoder().encodeToString(out);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao criptografar", e);
        }
    }

    public static String decrypt(String encoded) {
        if (encoded == null) return null;
        ensureKey();
        try {
            byte[] all = Base64.getDecoder().decode(encoded);
            if (all.length <= IV_LENGTH_BYTES) {
                // Provavelmente não está criptografado (ex.: valor legada em texto puro)
                return encoded;
            }
            byte[] iv = new byte[IV_LENGTH_BYTES];
            byte[] cipherText = new byte[all.length - IV_LENGTH_BYTES];
            System.arraycopy(all, 0, iv, 0, iv.length);
            System.arraycopy(all, iv.length, cipherText, 0, cipherText.length);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec spec = new GCMParameterSpec(TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);
            byte[] plain = cipher.doFinal(cipherText);
            return new String(plain, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            // Valor possivelmente em formato legado (texto puro). Retorna original.
            return encoded;
        }
    }

    private static void ensureKey() {
        if (secretKey == null) {
            throw new IllegalStateException("Chave de criptografia não inicializada");
        }
    }
}


