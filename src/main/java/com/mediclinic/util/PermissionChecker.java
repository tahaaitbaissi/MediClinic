package com.mediclinic.util;

import com.mediclinic.model.Role;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class PermissionChecker {

    /**
     * Check if a role can access a specific page
     */
    public static boolean canAccessPage(Role role, String page) {
        if (role == null || page == null) {
            return false;
        }

        Set<String> allowedPages = getAllowedPages(role);
        return allowedPages.contains(page.toLowerCase());
    }

    /**
     * Require that the current user has one of the specified roles
     * Throws IllegalStateException if not authenticated or doesn't have the role
     */
    public static void requireRole(Role... roles) {
        if (!UserSession.isAuthenticated()) {
            throw new IllegalStateException("User not authenticated");
        }

        UserSession session = UserSession.getInstance();
        Role userRole = session.getUser().getRole();

        for (Role requiredRole : roles) {
            if (userRole == requiredRole) {
                return; // User has one of the required roles
            }
        }

        throw new IllegalStateException("Access denied. Required role: " + 
            Arrays.toString(roles) + ", but user has: " + userRole);
    }

    /**
     * Check if current user has any of the specified roles
     */
    public static boolean hasAnyRole(Role... roles) {
        if (!UserSession.isAuthenticated()) {
            return false;
        }

        UserSession session = UserSession.getInstance();
        Role userRole = session.getUser().getRole();

        for (Role role : roles) {
            if (userRole == role) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get allowed pages for a role
     */
    private static Set<String> getAllowedPages(Role role) {
        Set<String> pages = new HashSet<>();

        switch (role) {
            case ADMIN:
                pages.addAll(Arrays.asList("dashboard", "patients", "agenda", "doctors", "billing", "users"));
                break;
            case MEDECIN:
                pages.addAll(Arrays.asList("dashboard", "agenda", "patients")); // read-only for patients
                break;
            case SEC:
                pages.addAll(Arrays.asList("dashboard", "patients", "agenda", "billing"));
                break;
        }

        return pages;
    }

    /**
     * Check if current user can perform action on a resource
     */
    public static boolean canPerformAction(String action, String resource) {
        if (!UserSession.isAuthenticated()) {
            return false;
        }

        UserSession session = UserSession.getInstance();
        Role role = session.getUser().getRole();

        // Admin can do everything
        if (role == Role.ADMIN) {
            return true;
        }

        // Doctor can only read/view their own resources
        if (role == Role.MEDECIN) {
            return action.equals("read") || action.equals("view");
        }

        // Secretary can create/edit patients, appointments, invoices
        if (role == Role.SEC) {
            if (resource.equals("patient") || resource.equals("appointment") || resource.equals("invoice")) {
                return action.equals("create") || action.equals("edit") || action.equals("read");
            }
        }

        return false;
    }
}

