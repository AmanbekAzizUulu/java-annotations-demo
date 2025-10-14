package com.dandaev.edu.validator;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import com.dandaev.edu.annotations.Email;
import com.dandaev.edu.annotations.NotNull;
import com.dandaev.edu.annotations.Range;
import com.dandaev.edu.annotations.Size;
import com.dandaev.edu.validator.error.Error;

public class Validator {
	private Validator() {
	}

	public static List<Error> validate(Object object) throws IllegalAccessException {
		List<Error> errors = new ArrayList<Error>();
		Field[] fields = object.getClass().getDeclaredFields();

		for (Field field : fields) {
			field.setAccessible(true);

			Object value = field.get(object);

			// @NotNull validation
			if (field.isAnnotationPresent(NotNull.class)) {
				NotNull annotation = field.getAnnotation(NotNull.class);
				if (value == null) {
					errors.add(new Error(field.getName(), annotation.message(), value));
				}
			}

			// @Size validation
			if (field.isAnnotationPresent(Size.class)) {
				Size annotation = field.getAnnotation(Size.class);
				if (value instanceof String) {
					String str = (String) value;
					if (str.length() < annotation.min() || str.length() > annotation.max()) {
						errors.add(new Error(field.getName(), annotation.message(), value));
					}
				}
			}

			// @Email validation
			if (field.isAnnotationPresent(Email.class) && value != null) {
				Email annotation = field.getAnnotation(Email.class);
				String email = value.toString();
				if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
					errors.add(new Error(field.getName(), annotation.message(), value));
				}
			}

			// @Range validation
			if (field.isAnnotationPresent(Range.class) && value != null) {
				Range annotation = field.getAnnotation(Range.class);
				if (value instanceof Number) {
					double num = ((Number) value).doubleValue();
					if (num < annotation.min() || num > annotation.max()) {
						errors.add(new Error(field.getName(), annotation.message(), value));
					}
				}
			}
		}
		return errors;
	}
}
