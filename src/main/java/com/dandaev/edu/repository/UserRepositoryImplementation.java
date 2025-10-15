package com.dandaev.edu.repository;

import java.util.HashMap;
import java.util.Map;

import com.dandaev.edu.annotations.dependency.injection.Service;
import com.dandaev.edu.entities.User;

@Service
public class UserRepositoryImplementation implements UserRepository {
	private Map<Long, User> users = new HashMap<>();

	@Override
	public User findById(Long id) {
		return users.get(id);
	}

	@Override
	public void save(User user) {
		users.put(1L, user); // Простая реализация
		System.out.println("User saved: " + user.getName());
	}
}
