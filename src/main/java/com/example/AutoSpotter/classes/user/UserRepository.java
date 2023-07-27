package com.example.AutoSpotter.classes.user;

public interface UserRepository {

    User getUserById(int id);

    void save(User user);
    
    boolean existsByUsername(String username);

    User findByUsername(String username);

    User findByEmail(String email);

    User findByUsernameAndPassword(String username, String password);
}