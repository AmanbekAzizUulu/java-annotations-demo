package com.dandaev.edu;

import java.util.HashMap;
import java.util.Map;

import com.dandaev.edu.controllers.UserController;
import com.dandaev.edu.web.framework.implementation.WebFramework;

public class WebFrameworkExample {
	public static void main(String[] args) {
		WebFramework framework = new WebFramework();
		framework.registerController(new UserController());

		// Симуляция HTTP запросов
		System.out.println("GET /api/users:");
		System.out.println(framework.handleRequest("GET", "/api/users", new HashMap<>(), ""));

		System.out.println("\nGET /api/users/1:");
		System.out.println(framework.handleRequest("GET", "/api/users/1", new HashMap<>(), ""));

		System.out.println("\nPOST /api/users:");
		Map<String, String> params = new HashMap<>();
		params.put("name", "Bob");
		params.put("email", "bob@example.com");
		params.put("age", "35");
		System.out.println(framework.handleRequest("POST", "/api/users", params, ""));

		System.out.println("\nGET /api/users/search?q=java:");
		Map<String, String> searchParams = new HashMap<>();
		searchParams.put("q", "java");
		System.out.println(framework.handleRequest("GET", "/api/users/search", searchParams, ""));
	}
}
