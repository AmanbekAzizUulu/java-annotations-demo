package com.dandaev.edu.annotations.jsonparser;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)

public @interface JsonDate {
	String pattern() default "yyyy-MM-dd HH:mm:ss";
}
