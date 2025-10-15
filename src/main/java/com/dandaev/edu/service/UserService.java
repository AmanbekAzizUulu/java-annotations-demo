package com.dandaev.edu.service;

import com.dandaev.edu.entities.User;

public interface UserService {
	User createUser(String name, String email, Integer age, String password);
	User getUser(Long id);
}
