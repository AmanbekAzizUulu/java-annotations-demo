package com.dandaev.edu.controllers;

import java.util.ArrayList;
import java.util.List;

import com.dandaev.edu.annotations.dependency.injection.Autowired;
import com.dandaev.edu.annotations.web.framework.PathVariable;
import com.dandaev.edu.annotations.web.framework.RequestMapping;
import com.dandaev.edu.annotations.web.framework.RequestParam;
import com.dandaev.edu.annotations.web.framework.RestController;
import com.dandaev.edu.entities.User;
import com.dandaev.edu.service.UserService;

// REST контроллеры
@RestController(path = "/api/users")
public class UserController {
	@Autowired
	private UserService userService;

	private List<User> users = new ArrayList<>();

	public UserController() {
		users.add(new User("John", "john@example.com", 25, "password"));
		users.add(new User("Alice", "alice@example.com", 30, "password"));
	}

	@RequestMapping(path = "/api/users", method = "GET")
	public String getAllUsers() {
		return "All users: " + users.size();
	}

	@RequestMapping(path = "/api/users/{id}", method = "GET")
	public String getUserById(@PathVariable("id") Long id) {
		if (id >= 0 && id < users.size()) {
			return "User: " + users.get(id.intValue()).getName();
		}
		return "User not found";
	}

	@RequestMapping(path = "/api/users", method = "POST")
	public String createUser(@RequestParam("name") String name,
			@RequestParam("email") String email,
			@RequestParam(value = "age", required = false) Integer age) {
		users.add(new User(name, email, age != null ? age : 0, "default"));
		return "User created: " + name;
	}

	@RequestMapping(path = "/api/users/search", method = "GET")
	public String searchUsers(@RequestParam("q") String query) {
		return "Search results for: " + query;
	}

	public void createNewUser() {
		try {
			User user = userService.createUser("Alice", "alice@example.com", 30, "password123");
			System.out.println("User created: " + user.getName());
		} catch (Exception e) {
			System.out.println("Error creating user: " + e.getMessage());
		}
	}
}
