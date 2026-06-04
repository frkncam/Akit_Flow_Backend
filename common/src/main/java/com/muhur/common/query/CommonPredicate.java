package com.muhur.common.query;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Controller parametresine konur; query param'lardan dinamik QueryDSL Predicate üretilmesini sağlar.
 * {@code root} filtrelenecek entity sınıfıdır (ör. Contract.class).
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CommonPredicate {
    Class<?> root();
}
