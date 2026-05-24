package br.eti.logos.commons.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.io.IOException;
import java.util.List;

/**
 * Modulo Jackson que ensina a deserializar {@link PageImpl} do Spring Data.
 * Necessario quando o Redis armazena Page serializado com type info polimorfica.
 */
public class PageJacksonModule {

    private PageJacksonModule() {
    }

    public static Module create() {
        SimpleModule module = new SimpleModule("PageJacksonModule");
        module.addDeserializer(PageImpl.class, new PageImplDeserializer());
        return module;
    }

    private static class PageImplDeserializer extends JsonDeserializer<PageImpl<?>> {

        @Override
        public PageImpl<?> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            JsonNode node = p.getCodec().readTree(p);

            List<Object> content = p.getCodec().readValue(
                    node.get("content").traverse(p.getCodec()),
                    new TypeReference<List<Object>>() {
                    });

            int pageNumber = node.has("number") ? node.get("number").asInt() : 0;
            int pageSize = node.has("size") ? node.get("size").asInt() : content.size();
            long totalElements = node.has("totalElements") ? node.get("totalElements").asLong() : content.size();

            return new PageImpl<>(content, PageRequest.of(pageNumber, Math.max(pageSize, 1)), totalElements);
        }
    }
}
