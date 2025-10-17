package com.dandaev.edu;

import java.util.Arrays;

import com.dandaev.edu.entities.Person;
import com.dandaev.edu.entities.PersonBuilder;
import com.dandaev.edu.entities.PersonToStringHelper;

public class CodeGenerationExample {
	public static void main(String[] args) {
		// Использование сгенерированного builder
		PersonBuilder builder = new PersonBuilder()
				.name("John")
				.age(25)
				.address("123 Main St")
				.hobbies(Arrays.asList("Reading", "Sports"));

		Person person = builder.build();

		// Использование сгенерированного toString
		String str = PersonToStringHelper.toString(person);
		System.out.println("Generated toString: " + str);
		System.out.println("Builder pattern ready to use");
	}
}
