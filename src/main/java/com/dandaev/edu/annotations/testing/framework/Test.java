package com.dandaev.edu.annotations.testing.framework;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.ElementType;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Test {
	Class<? extends Throwable> expected() default None.class;

	long timeout() default 0L;

	class None extends Throwable {
		private None() {
		}
	}
}
