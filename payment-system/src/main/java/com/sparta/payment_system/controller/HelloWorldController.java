package com.sparta.payment_system.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloWorldController {

    @GetMapping("/")
    public String helloWorld() {
        return "Hello World from Sparta Payment System!";
    }

    @GetMapping("/hello")
    public String hello() {
        return "Hello! Payment System is running successfully.";
    }
}
