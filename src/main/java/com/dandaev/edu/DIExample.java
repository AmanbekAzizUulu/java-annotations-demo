package com.dandaev.edu;

import com.dandaev.edu.controllers.UserController;
import com.dandaev.edu.dependency.injection.container.DependencyInjectionContainer;
import com.dandaev.edu.entities.User;
import com.dandaev.edu.service.UserService;

// Пример использования DI
public class DIExample {
	public static void main(String[] args) throws Exception {
		DependencyInjectionContainer container = new DependencyInjectionContainer();
		container.scanAndRegister("com.dandaev.edu");

		UserController controller = container.getBean(UserController.class);
		controller.createNewUser();

		// Получение пользователя
		UserService userService = container.getBean(UserService.class);
		User user = userService.getUser(1L);
		if (user != null) {
			System.out.println("Found user: " + user.getName());
		}
	}
}
