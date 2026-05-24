package br.eti.logos.commons.cache;

import org.springframework.lang.Nullable;

/**
 * Interface funcional para resolver o ID da igreja (tenant) da sessao atual.
 * Cada servico fornece sua implementacao via bean (tipicamente delegando para SessionUtils).
 */
@FunctionalInterface
public interface TenantIdResolver {

    /**
     * Retorna o ID da igreja do usuario logado, ou null se nao houver sessao.
     */
    @Nullable
    String resolve();
}
