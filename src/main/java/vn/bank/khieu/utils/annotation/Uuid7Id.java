package vn.bank.khieu.utils.annotation;

import org.hibernate.annotations.IdGeneratorType;

import vn.bank.khieu.utils.Uuid7Generator;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@IdGeneratorType(Uuid7Generator.class)
@Retention(RUNTIME)
@Target({ FIELD, METHOD })
public @interface Uuid7Id {
}
