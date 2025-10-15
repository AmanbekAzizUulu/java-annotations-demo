package com.dandaev.edu.controllers;

import com.dandaev.edu.annotations.dependency.injection.Autowired;
import com.dandaev.edu.annotations.dependency.injection.Component;
import com.dandaev.edu.entities.User;
import com.dandaev.edu.service.UserService;

@Component
public class UserController {
	@Autowired
	private UserService userService;

	public void createNewUser() {
		try {
			User user = userService.createUser("Alice", "alice@example.com", 30, "password123");
			System.out.println("User created: " + user.getName());
		} catch (Exception e) {
			System.out.println("Error creating user: " + e.getMessage());
		}
	}
}
