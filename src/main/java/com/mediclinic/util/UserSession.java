package com.mediclinic.util;

import com.mediclinic.model.Role;
import com.mediclinic.model.User;

public class UserSession {

    private static UserSession instance;
    private User user;

    private UserSession(User user) {
        this.user = user;
    }

    public static void setInstance(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
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

    /**
     * Check if a user is currently authenticated
     */
    public static boolean isAuthenticated() {
        return instance != null && instance.user != null;
    }

    /**
     * Check if the current user has a specific role
     */
    public boolean hasRole(Role role) {
        return user != null && user.getRole() == role;
    }

    /**
     * Check if the current user has any of the specified roles
     */
    public boolean hasAnyRole(Role... roles) {
        if (user == null) {
            return false;
        }
        Role userRole = user.getRole();
        for (Role role : roles) {
            if (userRole == role) {
                return true;
            }
        }
        return false;
    }

    public static void clean() {
        instance = null;
    }
}