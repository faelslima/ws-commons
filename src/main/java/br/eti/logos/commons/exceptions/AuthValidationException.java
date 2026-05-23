package br.eti.logos.commons.exceptions;

import lombok.Builder;
import lombok.Getter;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

@Getter
@Builder
public class AuthValidationException extends RuntimeException {

    private final String errorCode;

    public AuthValidationException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public AuthValidationException(String errorCode, MessageSource messageSource, Object... args) {
        super(resolveMessage(errorCode, messageSource, args));
        this.errorCode = errorCode;
    }

    public AuthValidationException(String message) {
        super(message);
        this.errorCode = null;
    }

    private static String resolveMessage(String errorCode, MessageSource messageSource, Object... args) {
        return messageSource.getMessage(errorCode, args, LocaleContextHolder.getLocale());
    }

    @Override
    public String getMessage() {
        return super.getMessage();
    }
}
