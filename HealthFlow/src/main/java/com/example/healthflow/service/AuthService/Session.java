package com.example.healthflow.service.AuthService;

public final class Session {
    private static com.example.healthflow.model.User currentUser;

    private Session() {}

    public static void set(com.example.healthflow.model.User user) {
        currentUser = user;
    }

    public static com.example.healthflow.model.User get() {
        return currentUser;
    }

    public static void clear() {
        currentUser = null;
    }
}