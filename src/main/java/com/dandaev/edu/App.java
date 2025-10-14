package com.dandaev.edu;

import com.dandaev.edu.entities.User;
import com.dandaev.edu.validator.Validator;

public class App {
	public static void main(String[] args) throws IllegalAccessException {
		User validUser = new User("John Doe", "john@example.com", 25, "secure123");
		var errors = Validator.validate(validUser);

		if (errors.isEmpty()) {
			System.out.println("User is valid");
			errors.clear();
		}

		User invalidUser = new User("J", "invalid-email", 15, "123");
		errors = Validator.validate(invalidUser);

		if (errors.isEmpty()) {
			System.out.println("User is valid");
			errors.clear();
		} else {
			for (var error : errors) {
				System.out.println(error);
			}
		}
	}
}
