package br.eti.logos.commons.constants;

/**
 * Constantes compartilhadas pelo sistema i12.
 *
 * <p>Foi removida a constante {@code SECRET} (HMAC) — agora vem de env var
 * {@code SECURITY_HMAC_SECRET} (vide ws-security/application.properties).
 */
public final class Constants {

    public static final String ALGORITM_ENCRYPT = "HmacSHA256";
    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String REDIS_KEY_GENERATOR = "redisKeyGenerator";

    private Constants() {
    }
}
