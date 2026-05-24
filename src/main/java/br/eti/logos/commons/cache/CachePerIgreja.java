package br.eti.logos.commons.cache;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Inherited
public @interface CachePerIgreja {

    String schema();
}
