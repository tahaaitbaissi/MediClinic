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

    /**
     * Get the medecin ID for the current user (if user is a doctor)
     */
    public static Long getMedecinId() {
        if (instance == null || instance.user == null) {
            return null;
        }
        if (instance.user.getMedecin() == null) {
            return null;
        }
        return instance.user.getMedecin().getId();
    }

    /**
     * Check if a user is logged in (alias for isAuthenticated)
     */
    public static boolean isLoggedIn() {
        return isAuthenticated();
    }

    public static void clean() {
        instance = null;
    }
}
