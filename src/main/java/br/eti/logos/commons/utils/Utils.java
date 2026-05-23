package br.eti.logos.commons.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Utilitários compartilhados pelos 8 microsserviços do i12.
 *
 * <p><b>Padrão de datas (Fase 1):</b> backend i12 recebe e devolve em ISO 8601
 * — alinhado com o frontend (i12mobile + i12web), que aplica máscara visual via
 * helpers em {@code @/utils/format}. Portanto:
 * <ul>
 *   <li>{@link #convertLocalDateToString(LocalDate)} e companhia retornam ISO
 *       ({@code yyyy-MM-dd}, {@code yyyy-MM-dd'T'HH:mm:ss}, {@code yyyy-MM-dd'T'HH:mm:ssXXX}).</li>
 *   <li>{@link #convertStringToLocalDate(String)} aceita primeiro ISO; cai para
 *       {@code dd/MM/yyyy} apenas como fallback de compatibilidade com integrações
 *       e clientes legados ainda não migrados.</li>
 *   <li>Para <b>displays</b> em PT-BR (PDFs, e-mails, telas internas que renderizam direto),
 *       use os métodos {@code *ToBrString} com sufixo explícito.</li>
 *   <li>Para padrão customizado, use a sobrecarga {@code (date, pattern)}.</li>
 * </ul>
 */
public final class Utils {

    /** Patterns BR (display em pt-BR). Use apenas em PDFs, e-mails, relatórios. */
    public static final String BR_DATE_PATTERN = "dd/MM/yyyy";
    public static final String BR_DATE_TIME_PATTERN = "dd/MM/yyyy HH:mm";

    /** Patterns ISO 8601 (API contract — frontend recebe estes e aplica máscara visual). */
    public static final String ISO_DATE_PATTERN = "yyyy-MM-dd";
    public static final String ISO_TIME_PATTERN = "HH:mm:ss";
    public static final String ISO_TIME_SHORT_PATTERN = "HH:mm";
    public static final String ISO_DATE_TIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss";
    public static final String ISO_DATE_TIME_OFFSET_PATTERN = "yyyy-MM-dd'T'HH:mm:ssXXX";

    /**
     * ObjectMapper compartilhado, configurado com JavaTimeModule (jsr310) para
     * serializar {@link LocalDate}, {@link LocalDateTime}, {@link OffsetDateTime}
     * em ISO 8601 sem timestamps numéricos.
     */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private Utils() {
    }

    // ----------------------------------------------------------------------
    // Null/empty checks
    // ----------------------------------------------------------------------

    /**
     * Retorna true se o valor é null, string em branco/literal "null"/"undefined", ou
     * coleção vazia.
     */
    public static boolean isEmpty(Object value) {
        if (Objects.isNull(value)) {
            return true;
        }
        if (value instanceof String) {
            String s = (String) value;
            if (StringUtils.isBlank(s)) return true;
            return "null".equalsIgnoreCase(s) || "undefined".equalsIgnoreCase(s);
        }
        if (value instanceof Collection<?>) {
            return CollectionUtils.isEmpty((Collection<?>) value);
        }
        return false;
    }

    public static boolean isNotEmpty(Object value) {
        return !isEmpty(value);
    }

    /** @deprecated use {@link #isEmpty(Object)}. Mantido para compat. */
    @Deprecated
    public static boolean isNullOrEmpty(Object value) {
        return isEmpty(value);
    }

    // ----------------------------------------------------------------------
    // String formatting
    // ----------------------------------------------------------------------

    public static String removeAcentuacao(String txt) {
        if (isNotEmpty(txt)) {
            txt = Normalizer.normalize(txt, Normalizer.Form.NFD);
            txt = txt.replaceAll("[^\\p{ASCII}]", "");
        }
        return txt;
    }

    public static String capitalize(String txt) {
        if (StringUtils.isNotBlank(txt)) {
            txt = Arrays.stream(txt.toLowerCase().split(" "))
                    .map(StringUtils::capitalize)
                    .collect(Collectors.joining(" "));
            txt = txt.replace(" Da ", " da ")
                    .replace(" De ", " de ")
                    .replace(" Do ", " do ")
                    .replace(" E ", " e ")
                    .replace(" Dos ", " dos ")
                    .replace(" Das ", " das ");
        }
        return StringUtils.trim(txt);
    }

    public static String removeCaracteresEspeciais(String text) {
        if (isNotEmpty(text)) {
            return text.replaceAll("[^a-zA-Z0-9]", "");
        }
        return null;
    }

    public static String mascaraNumeroTelefone(String numero) {
        if (StringUtils.isBlank(numero)) {
            return numero;
        }
        if (numero.length() != 10 && numero.length() != 11) {
            removeCaracteresEspeciais(numero);
        }
        if (numero.trim().length() >= 10) {
            return "(" + numero.substring(0, 2) + ") "
                    + numero.substring(2, 7) + "-"
                    + numero.substring(7);
        }
        return "";
    }

    public static String mascaraCep(String cep) {
        if (cep == null || !cep.matches("\\d{8}")) {
            return null;
        }
        return cep.substring(0, 5) + "-" + cep.substring(5);
    }

    // ----------------------------------------------------------------------
    // Date / time — OUTPUT
    //
    // Default = ISO 8601 (alinhado com a Fase 1: frontend recebe ISO e aplica
    // máscara visual conforme locale).
    // Para displays em PT-BR (PDFs, e-mails), use as variantes *ToBrString.
    // ----------------------------------------------------------------------

    public static Long getIdade(LocalDate dataNascimento) {
        if (Objects.isNull(dataNascimento)) {
            return null;
        }
        return (long) Period.between(dataNascimento, LocalDate.now()).getYears();
    }

    /** Formata em ISO 8601 ({@code yyyy-MM-dd}). É o padrão para JSON/API. */
    public static String convertLocalDateToString(LocalDate date) {
        return Objects.nonNull(date)
                ? date.format(DateTimeFormatter.ofPattern(ISO_DATE_PATTERN))
                : null;
    }

    /** Alias explícito de {@link #convertLocalDateToString(LocalDate)}. */
    public static String convertLocalDateToIsoString(LocalDate date) {
        return convertLocalDateToString(date);
    }

    /** Formata em "dd/MM/yyyy" para displays em pt-BR (PDFs, e-mails, relatórios). */
    public static String convertLocalDateToBrString(LocalDate date) {
        return Objects.nonNull(date)
                ? date.format(DateTimeFormatter.ofPattern(BR_DATE_PATTERN))
                : null;
    }

    /** Formata em ISO 8601 ({@code yyyy-MM-dd'T'HH:mm:ss}). É o padrão para JSON/API. */
    public static String convertLocalDateTimeToString(LocalDateTime date) {
        return Objects.nonNull(date)
                ? date.format(DateTimeFormatter.ofPattern(ISO_DATE_TIME_PATTERN))
                : null;
    }

    /** Alias explícito de {@link #convertLocalDateTimeToString(LocalDateTime)}. */
    public static String convertLocalDateTimeToIsoString(LocalDateTime date) {
        return convertLocalDateTimeToString(date);
    }

    /**
     * @deprecated Use {@link #convertLocalDateTimeToString(LocalDateTime)} (ISO) ou
     * {@link #convertLocalDateTimeToBrString(LocalDateTime, boolean)} para displays BR.
     */
    @Deprecated
    public static String convertLocalDateTimeToString(LocalDateTime date, boolean withTime) {
        return convertLocalDateTimeToBrString(date, withTime);
    }

    /** Formata um LocalDateTime com pattern customizado. */
    public static String convertLocalDateTimeToString(LocalDateTime date, String pattern) {
        return Objects.nonNull(date)
                ? date.format(DateTimeFormatter.ofPattern(pattern))
                : null;
    }

    /** Formata um OffsetDateTime com pattern customizado. */
    public static String convertLocalDateTimeToString(OffsetDateTime date, String pattern) {
        return Objects.nonNull(date)
                ? date.format(DateTimeFormatter.ofPattern(pattern))
                : null;
    }

    /** Formata LocalDateTime em "dd/MM/yyyy HH:mm" (display BR). */
    public static String convertLocalDateTimeToBrString(LocalDateTime date) {
        return convertLocalDateTimeToBrString(date, true);
    }

    /** Formata LocalDateTime em BR ({@code dd/MM/yyyy} ou {@code dd/MM/yyyy HH:mm}). */
    public static String convertLocalDateTimeToBrString(LocalDateTime date, boolean withTime) {
        return Objects.nonNull(date)
                ? date.format(DateTimeFormatter.ofPattern(withTime ? BR_DATE_TIME_PATTERN : BR_DATE_PATTERN))
                : null;
    }

    /** Formata em ISO 8601 com offset ({@code yyyy-MM-dd'T'HH:mm:ssXXX}). É o padrão para JSON/API. */
    public static String convertOffsetDateTimeToString(OffsetDateTime dateTime) {
        return Objects.nonNull(dateTime)
                ? dateTime.format(DateTimeFormatter.ofPattern(ISO_DATE_TIME_OFFSET_PATTERN))
                : null;
    }

    /** Alias explícito de {@link #convertOffsetDateTimeToString(OffsetDateTime)}. */
    public static String convertOffsetDateTimeToIsoString(OffsetDateTime dateTime) {
        return convertOffsetDateTimeToString(dateTime);
    }

    /**
     * @deprecated Use {@link #convertOffsetDateTimeToString(OffsetDateTime)} (ISO) ou
     * {@link #convertOffsetDateTimeToBrString(OffsetDateTime, boolean)} para displays BR.
     */
    @Deprecated
    public static String convertOffsetDateTimeToString(OffsetDateTime dateTime, boolean withTime) {
        return convertOffsetDateTimeToBrString(dateTime, withTime);
    }

    /** Formata OffsetDateTime em "dd/MM/yyyy HH:mm" (display BR). */
    public static String convertOffsetDateTimeToBrString(OffsetDateTime dateTime) {
        return convertOffsetDateTimeToBrString(dateTime, true);
    }

    /** Formata OffsetDateTime em BR ({@code dd/MM/yyyy} ou {@code dd/MM/yyyy HH:mm}). */
    public static String convertOffsetDateTimeToBrString(OffsetDateTime dateTime, boolean withTime) {
        return Objects.nonNull(dateTime)
                ? dateTime.format(DateTimeFormatter.ofPattern(withTime ? BR_DATE_TIME_PATTERN : BR_DATE_PATTERN))
                : null;
    }

    // ----------------------------------------------------------------------
    // Date / time — INPUT (parse)
    //
    // Defaults aceitam primeiro ISO; caem em "dd/MM/yyyy" como fallback de
    // compatibilidade. Frontends novos (i12mobile/i12web) enviam ISO; integrações
    // e callers internos legados podem ainda enviar BR.
    // ----------------------------------------------------------------------

    /**
     * Aceita primeiro ISO 8601 ({@code yyyy-MM-dd}); cai em {@code dd/MM/yyyy} como
     * fallback de compatibilidade. Retorna null se a entrada for vazia.
     */
    public static LocalDate convertStringToLocalDate(String dateString) {
        if (isEmpty(dateString)) return null;
        try {
            return LocalDate.parse(dateString, DateTimeFormatter.ofPattern(ISO_DATE_PATTERN));
        } catch (Exception ignored) {
            return LocalDate.parse(dateString, DateTimeFormatter.ofPattern(BR_DATE_PATTERN));
        }
    }

    /** Parse com pattern customizado. */
    public static LocalDate convertStringToLocalDate(String dateString, String pattern) {
        return isNotEmpty(dateString)
                ? LocalDate.parse(dateString, DateTimeFormatter.ofPattern(pattern))
                : null;
    }

    /**
     * @deprecated Hoje {@link #convertStringToLocalDate(String)} já é flexível
     * (ISO + fallback BR). Mantido apenas para clareza explícita em código que
     * quer enfatizar que aceita os dois formatos.
     */
    @Deprecated
    public static LocalDate convertStringToLocalDateFlexible(String dateString) {
        return convertStringToLocalDate(dateString);
    }

    /**
     * Aceita ISO 8601 ({@code yyyy-MM-dd[T HH:mm[:ss][XXX]]}) ou BR ({@code dd/MM/yyyy HH:mm}).
     * Se for apenas data ({@code yyyy-MM-dd}), assume meia-noite. Retorna null se vazio.
     */
    public static LocalDateTime convertStringToLocalDateTime(String dateTimeString) {
        if (isEmpty(dateTimeString)) {
            return null;
        }
        String s = dateTimeString.trim();

        // 1) ISO com offset: yyyy-MM-dd'T'HH:mm:ssXXX  (descarta offset)
        try {
            return OffsetDateTime.parse(s).toLocalDateTime();
        } catch (Exception ignored) {
            // segue
        }

        // 2) ISO sem offset: yyyy-MM-dd'T'HH:mm:ss[.SSS]
        if (s.contains("T")) {
            try {
                return LocalDateTime.parse(s);
            } catch (Exception ignored) {
                // segue
            }
        }

        // 3) Apenas data ISO: yyyy-MM-dd
        if (s.length() == 10 && s.charAt(4) == '-') {
            try {
                return LocalDate.parse(s, DateTimeFormatter.ofPattern(ISO_DATE_PATTERN)).atStartOfDay();
            } catch (Exception ignored) {
                // segue
            }
        }

        // 4) BR data: dd/MM/yyyy
        if (s.length() == 10 && s.charAt(2) == '/') {
            LocalDate localDate = convertStringToLocalDate(s, BR_DATE_PATTERN);
            return Objects.nonNull(localDate) ? localDate.atStartOfDay() : null;
        }

        // 5) BR data+hora: dd/MM/yyyy HH:mm
        return LocalDateTime.parse(s, DateTimeFormatter.ofPattern(BR_DATE_TIME_PATTERN));
    }

    /**
     * @deprecated Hoje {@link #convertStringToLocalDateTime(String)} já aceita ISO.
     */
    @Deprecated
    public static LocalDateTime convertIsoStringToLocalDateTime(String dateTime) {
        return convertStringToLocalDateTime(dateTime);
    }

    public static OffsetDateTime convertStringToOffsetDateTime(String dateTimeString) {
        if (isEmpty(dateTimeString)) return null;
        return OffsetDateTime.parse(dateTimeString);
    }

    // ----------------------------------------------------------------------
    // Money (BRL — centavos ↔ reais)
    // ----------------------------------------------------------------------

    /** Converte valor em centavos (Long) para BigDecimal em reais (2 casas). */
    public static BigDecimal convertCentsInReal(Long cents) {
        if (Objects.isNull(cents)) return BigDecimal.ZERO;
        return BigDecimal.valueOf(cents).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    /** Converte valor em reais (BigDecimal) para centavos (Long). */
    public static Long convertRealInCents(BigDecimal money) {
        if (Objects.isNull(money)) return 0L;
        return money.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).longValueExact();
    }

    /** Aumento percentual: money * (1 + percentual/100). Retorna BigDecimal com 2 casas. */
    public static BigDecimal aumentoPercentual(BigDecimal money, BigDecimal percentual) {
        if (Objects.isNull(money) || Objects.isNull(percentual)) return money;
        var fator = BigDecimal.ONE.add(percentual.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP));
        return money.multiply(fator).setScale(2, RoundingMode.HALF_UP);
    }

    // ----------------------------------------------------------------------
    // JSON / Map / Files
    // ----------------------------------------------------------------------

    public static Object getObjectFromJson(String message, Class<?> clazz) {
        try {
            return OBJECT_MAPPER.readValue(message, clazz);
        } catch (Exception e) {
            return null;
        }
    }

    public static String getJsonFromObject(Object object) {
        try {
            return OBJECT_MAPPER.writeValueAsString(object);
        } catch (Exception e) {
            return null;
        }
    }

    public static String getValueFromMapKey(Map<String, String> map, String key) {
        if (Objects.isNull(map) || isEmpty(key)) return null;
        // Headers HTTP são case-insensitive — buscar por equalsIgnoreCase.
        return map.entrySet().stream()
                .filter(e -> Objects.nonNull(e.getKey()) && e.getKey().equalsIgnoreCase(key))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    public static String calculaTamanhoArquivo(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        char prefix = "KMGTPE".charAt(exp - 1);
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), prefix);
    }

    public static String getResourceRealPath(String resource) {
        if (StringUtils.isNotBlank(resource)) {
            var resourceURL = Utils.class.getClassLoader().getResource(resource);
            if (Objects.nonNull(resourceURL)) {
                try {
                    return Paths.get(resourceURL.toURI()).toString();
                } catch (URISyntaxException e) {
                    return null;
                }
            }
        }
        return null;
    }
}
