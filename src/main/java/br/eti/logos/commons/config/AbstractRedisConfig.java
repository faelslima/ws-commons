package br.eti.logos.commons.config;

import br.eti.logos.commons.utils.Utils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.StdSubtypeResolver;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Configuracao base de Redis para todos os microsservicos i12.
 * <p>
 * Replica a configuracao original que funcionava em todos os services:
 * - RedisTemplate: GenericJackson2JsonRedisSerializer (JSON com NON_FINAL)
 * - CacheManager: serializer padrao do Spring (JDK Serialization)
 * <p>
 * Subclasses podem sobrescrever:
 * <ul>
 *   <li>{@link #buildKeyGenerator()} — para customizar a geracao de chaves</li>
 *   <li>{@link #getCacheTtl()} — para alterar o TTL de cache</li>
 * </ul>
 */
public abstract class AbstractRedisConfig {

    @Value("${spring.profiles.active:dev}")
    private String environment;

    @Value("${REDIS_HOST:localhost}")
    private String redisHost;

    @Value("${REDIS_PORT:6379}")
    private int redisPort;

    @Value("${REDIS_USERNAME:default}")
    private String redisUsername;

    @Value("${REDIS_PASSWORD:}")
    private String redisPassword;

    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        var config = new RedisStandaloneConfiguration();
        config.setHostName(redisHost);
        config.setPort(redisPort);
        if (StringUtils.isNotBlank(redisUsername)) {
            config.setUsername(redisUsername);
        }
        if (StringUtils.isNotBlank(redisPassword)) {
            config.setPassword(RedisPassword.of(redisPassword));
        }
        return new LettuceConnectionFactory(config);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setSubtypeResolver(new StdSubtypeResolver());
        objectMapper.activateDefaultTyping(
                objectMapper.getPolymorphicTypeValidator(),
                ObjectMapper.DefaultTyping.NON_FINAL
        );

        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(serializer);
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    @Primary
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration cacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(getCacheTtl())
                .disableCachingNullValues();

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(cacheConfig)
                .build();
    }

    @Bean
    public KeyGenerator redisKeyGenerator() {
        return buildKeyGenerator();
    }

    // --- Hooks para subclasses ---

    /**
     * TTL do cache. Default: 1 dia em dev, 15 dias em prd.
     */
    protected Duration getCacheTtl() {
        return !"prd".equalsIgnoreCase(environment) ? Duration.ofDays(1) : Duration.ofDays(15);
    }

    /**
     * Constroi o KeyGenerator. Default: classe + metodo + params.
     * Subclasses podem sobrescrever para chaves customizadas.
     */
    protected KeyGenerator buildKeyGenerator() {
        return (target, method, params) -> {
            StringBuilder key = new StringBuilder();
            key.append(target.getClass().getSimpleName())
                    .append(".")
                    .append(method.getName())
                    .append(":");

            if (Objects.nonNull(params)) {
                Arrays.stream(params).filter(Objects::nonNull).forEach(param -> {
                    if (param instanceof Pageable pageable) {
                        key.append("page-")
                                .append(pageable.getPageNumber())
                                .append("-size-")
                                .append(pageable.getPageSize());

                        if (pageable.getSort().isSorted()) {
                            String sortKey = pageable.getSort().stream()
                                    .map(order -> order.getProperty() + "-" + order.getDirection())
                                    .collect(Collectors.joining(","));
                            key.append("-sort-").append(sortKey);
                        }
                    } else {
                        key.append(Utils.removeAcentuacao(param.toString()));
                    }
                    key.append(":");
                });
            }

            var result = key.toString();
            if (result.endsWith(":")) {
                result = result.substring(0, result.length() - 1);
            }
            return String.format("%s:%s", environment, result);
        };
    }

    protected String getEnvironment() {
        return environment;
    }
}
