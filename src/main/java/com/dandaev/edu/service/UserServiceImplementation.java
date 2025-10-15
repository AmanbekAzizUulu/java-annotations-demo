package com.dandaev.edu.service;

import com.dandaev.edu.annotations.dependency.injection.Autowired;
import com.dandaev.edu.entities.User;
import com.dandaev.edu.repository.UserRepository;
import com.dandaev.edu.validator.Validator;

public class UserServiceImplementation implements UserService {
	@Autowired
	private UserRepository userRepository;

	@Autowired(required = false)
	private Validator validator; // Опциональная зависимость

	@Override
	public User createUser(String name, String email, Integer age, String password) {
		User user = new User(name, email, age, password);

		// Валидация если доступна
		if (validator != null) {
			try {
				var errors = Validator.validate(user);
				if (!errors.isEmpty()) {
					throw new IllegalArgumentException("Validation failed: " + errors);
				}
			} catch (Exception e) {
				throw new RuntimeException("Validation error", e);
			}
		}

		userRepository.save(user);
		return user;
	}

	@Override
	public User getUser(Long id) {
		return userRepository.findById(id);
	}
}
