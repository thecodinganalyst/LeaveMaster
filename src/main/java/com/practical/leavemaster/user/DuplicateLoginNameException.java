package com.practical.leavemaster.user;

public class DuplicateLoginNameException extends RuntimeException {
    public DuplicateLoginNameException(String loginName) {
        super("Login name already exists: " + loginName);
    }
}
