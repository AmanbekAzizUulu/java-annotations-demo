package com.dandaev.edu.entities;

import com.dandaev.edu.annotations.Email;
import com.dandaev.edu.annotations.NotNull;
import com.dandaev.edu.annotations.Range;
import com.dandaev.edu.annotations.Size;

public class User {
	@NotNull(message = "Name is required")
	@Size(min = 1, max = 50, message = "Name must be 2-50 characters")
	private String name;

	@NotNull(message = "Email is required")
	@Email(message = "Invalid email address")
	private String email;

	@Size(min = 6, message = "Password must be at least 6 characters")
	private String password;

	@Range(min = 18, max = 120, message = "Age must be between 18 and 120")
	private Integer age;

	public User(String name, String email, Integer age, String password) {
		this.name = name;
		this.email = email;
		this.password = password;
		this.age = age;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public Integer getAge() {
		return age;
	}

	public void setAge(Integer age) {
		this.age = age;
	}

}
