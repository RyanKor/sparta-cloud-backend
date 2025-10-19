package com.sparta.payment_system.controller;

import com.sparta.payment_system.entity.User;
import com.sparta.payment_system.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/test")
public class DatabaseTestController {

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/user")
    public User createUser(@RequestParam String username, @RequestParam String email) {
        User user = new User(username, email);
        return userRepository.save(user);
    }

    @GetMapping("/users")
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
}