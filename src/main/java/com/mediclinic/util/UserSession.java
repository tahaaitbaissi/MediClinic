package com.mediclinic.util;

import com.mediclinic.model.User;

public class UserSession {

    private static UserSession instance;
    private User user;

    private UserSession(User user) {
        this.user = user;
    }

    public static void setInstance(User user) {
        instance = new UserSession(user);
    }

    public static UserSession getInstance() {
        if (instance == null) {
            throw new IllegalStateException("Aucun utilisateur connecté.");
        }
        return instance;
    }

    public User getUser() {
        return user;
    }

    public static void clean() {
        instance = null;
    }
}