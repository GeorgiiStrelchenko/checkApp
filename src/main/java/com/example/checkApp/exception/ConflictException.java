package com.example.checkApp.exception;

public class ConflictException extends RuntimeException {

    public ConflictException(String resource, String name) {
        super(resource + " '" + name + "' already exists");
    }
}
