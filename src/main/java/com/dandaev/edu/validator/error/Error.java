package com.dandaev.edu.validator.error;

public class Error {
	private final String fieldName; 									// имя поля, в котором произошла ошибка
	private final String message; 										// текст сообщения об ошибке
	private final Object invalidValue; 									// значение, вызвавшее ошибку

	public Error(String fieldName, String message, Object invalidValue) {
		this.fieldName = fieldName;
		this.message = message;
		this.invalidValue = invalidValue;
	}

	public String getFieldName() {
		return fieldName;
	}

	public String getMessage() {
		return message;
	}

	public Object getInvalidValue() {
		return invalidValue;
	}

	@Override
	public String toString() {
		return "Validation error in field '" + fieldName + "': " + message +
				" (invalid value: " + invalidValue + ")";
	}
}
