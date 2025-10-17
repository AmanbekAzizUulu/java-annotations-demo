package com.dandaev.edu.entities;

import java.util.List;

import com.dandaev.edu.annotations.builder.pattern.generator.DefaultValue;
import com.dandaev.edu.annotations.builder.pattern.generator.GenerateBuilder;
import com.dandaev.edu.annotations.builder.pattern.generator.GenerateToString;

@GenerateBuilder(builderName = "PersonBuilder", fluent = true)
@GenerateToString(includeFields = true)
public class Person {
	private String name;
	private int age;

	@DefaultValue("unknown")
	private String address;

	private List<String> hobbies;

	// Нужны геттеры и сеттеры для работы с рефлексией
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getAge() {
		return age;
	}

	public void setAge(int age) {
		this.age = age;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public List<String> getHobbies() {
		return hobbies;
	}

	public void setHobbies(List<String> hobbies) {
		this.hobbies = hobbies;
	}
}
