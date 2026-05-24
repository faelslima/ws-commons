package br.eti.logos.commons.cache;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.CacheOperationInvocationContext;
import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.*;

/**
 * Cache resolver multi-tenant por igreja.
 * <p>
 * Gera nomes de cache no formato {@code env:schema:igrejaId}, isolando dados entre igrejas.
 * O igrejaId e obtido via {@link TenantIdResolver} (sessao) ou inferido dos argumentos do metodo.
 */
@Component("tenantByIgrejaCacheResolver")
public class TenantByIgrejaCacheResolver implements CacheResolver {

    private final CacheManager cacheManager;
    private final TenantIdResolver tenantIdResolver;
    private final DefaultParameterNameDiscoverer nameDiscoverer = new DefaultParameterNameDiscoverer();

    @Value("${spring.profiles.active:default}")
    private String environment;

    public TenantByIgrejaCacheResolver(CacheManager cacheManager, TenantIdResolver tenantIdResolver) {
        this.cacheManager = cacheManager;
        this.tenantIdResolver = tenantIdResolver;
    }

    @NonNull
    @Override
    public Collection<? extends Cache> resolveCaches(@NonNull CacheOperationInvocationContext<?> context) {
        final String base = resolveBaseCacheName(context);
        if (StringUtils.isBlank(base)) {
            return Collections.emptyList();
        }

        final String igrejaIdFromSession = StringUtils.trimToNull(tenantIdResolver.resolve());

        final String igrejaIdFromArgs = StringUtils.isBlank(igrejaIdFromSession)
                ? resolveIgrejaIdFromArgs(context.getMethod(), context.getArgs())
                : null;

        final String igrejaId = StringUtils.isNotBlank(igrejaIdFromSession) ? igrejaIdFromSession : igrejaIdFromArgs;
        final String envPrefix = StringUtils.isBlank(environment) ? "default" : environment.toLowerCase();

        final String cacheName = StringUtils.isNotBlank(igrejaId)
                ? envPrefix + ":" + base + ":" + igrejaId
                : envPrefix + ":" + base;

        var cache = cacheManager.getCache(cacheName);
        if (cache == null) {
            return Collections.emptyList();
        }
        return List.of(cache);
    }

    private String resolveBaseCacheName(CacheOperationInvocationContext<?> context) {
        Method m = context.getMethod();

        var ann = m.getAnnotation(CachePerIgreja.class);
        if (Objects.nonNull(ann) && StringUtils.isNotBlank(ann.schema())) return ann.schema();

        ann = context.getTarget().getClass().getAnnotation(CachePerIgreja.class);
        if (Objects.nonNull(ann) && StringUtils.isNotBlank(ann.schema())) return ann.schema();

        return context.getOperation().getCacheNames().stream().findFirst().orElse(null);
    }

    @Nullable
    private String resolveIgrejaIdFromArgs(Method method, Object[] args) {
        if (args == null || args.length == 0) return null;

        final String[] paramNames = nameDiscoverer.getParameterNames(method);

        for (int i = 0; i < args.length; i++) {
            Object a = args[i];

            // (1) nome do parametro "igrejaId"
            if (paramNames != null && i < paramNames.length && "igrejaId".equalsIgnoreCase(paramNames[i])) {
                String candidate = trimToNonBlank(a);
                if (candidate != null) return candidate;
            }

            // (2) String direta
            if (a instanceof String s) {
                String candidate = StringUtils.trimToNull(s);
                if (candidate != null) return candidate;
            }

            // (3) Map com "igrejaId"
            if (a instanceof Map<?, ?> map) {
                String candidate = trimToNonBlank(map.get("igrejaId"));
                if (candidate != null) return candidate;
            }

            // (4) DTO com getIgrejaId()
            {
                String candidate = trimToNonBlank(invokeGetter(a, "getIgrejaId"));
                if (candidate != null) return candidate;
            }

            // (5) DTO com getIgreja().getId()
            {
                Object igreja = invokeGetter(a, "getIgreja");
                String candidate = trimToNonBlank(invokeGetter(igreja, "getId"));
                if (candidate != null) return candidate;
            }

            // (6) Record: componente "igrejaId" ou "igreja".id
            if (a != null && a.getClass().isRecord()) {
                RecordComponent[] rc = a.getClass().getRecordComponents();
                if (rc != null && rc.length > 0) {
                    for (RecordComponent c : rc) {
                        if ("igrejaId".equalsIgnoreCase(c.getName())) {
                            String candidate = trimToNonBlank(invokeMethod(a, c.getAccessor()));
                            if (candidate != null) return candidate;
                        }
                    }
                    for (RecordComponent c : rc) {
                        if ("igreja".equalsIgnoreCase(c.getName())) {
                            Object igr = invokeMethod(a, c.getAccessor());
                            String candidate = trimToNonBlank(invokeGetter(igr, "getId"));
                            if (candidate != null) return candidate;
                        }
                    }
                }
            }
        }
        return null;
    }

    @Nullable
    private String trimToNonBlank(@Nullable Object v) {
        if (v == null) return null;
        String s = String.valueOf(v).trim();
        return s.isEmpty() ? null : s;
    }

    @Nullable
    private Object invokeGetter(@Nullable Object target, String getter) {
        if (target == null) return null;
        try {
            var m = target.getClass().getMethod(getter);
            return m.invoke(target);
        } catch (Exception ignored) {
            return null;
        }
    }

    @Nullable
    private Object invokeMethod(Object target, Method m) {
        try {
            return m.invoke(target);
        } catch (Exception ignored) {
            return null;
        }
    }
}
