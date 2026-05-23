package br.eti.logos.commons.exceptions;

import br.eti.logos.commons.utils.Utils;
import lombok.Builder;
import lombok.Getter;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

@Getter
@Builder
public class ErrorValidationException extends RuntimeException {

    private final String errorCode;

    public ErrorValidationException(String message) {
        super(message);
        this.errorCode = null;
    }

    public ErrorValidationException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorValidationException(ErrorResponse errorResponse) {
        super(Utils.getJsonFromObject(errorResponse));
        this.errorCode = errorResponse.getErrorCode();
    }

    public ErrorValidationException(String errorCode, MessageSource messageSource, Object... args) {
        super(resolveMessage(errorCode, messageSource, args));
        this.errorCode = errorCode;
    }

    private static String resolveMessage(String errorCode, MessageSource messageSource, Object... args) {
        return messageSource.getMessage(errorCode, args, LocaleContextHolder.getLocale());
    }

    @Override
    public String getMessage() {
        return super.getMessage();
    }
}
