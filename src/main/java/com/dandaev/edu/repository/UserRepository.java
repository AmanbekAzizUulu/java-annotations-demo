package com.dandaev.edu.repository;

import com.dandaev.edu.entities.User;

public interface UserRepository {
	User findById(Long id);
	void save(User user);
}
