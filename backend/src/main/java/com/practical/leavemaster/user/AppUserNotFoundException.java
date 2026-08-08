package com.practical.leavemaster.user;

public class AppUserNotFoundException extends RuntimeException {
    public AppUserNotFoundException(String id) {
        super("User not found: " + id);
    }
}
