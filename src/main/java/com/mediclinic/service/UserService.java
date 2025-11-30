package com.mediclinic.service;

import com.mediclinic.dao.UserDAO;
import com.mediclinic.model.Role;
import com.mediclinic.model.User;
import org.mindrot.jbcrypt.BCrypt;

import java.util.List;

public class UserService {

    private final UserDAO userDAO;

    public UserService() {
        this.userDAO = new UserDAO();
    }

    /**
     * Update user password with validation
     */
    public void updatePassword(Long userId, String oldPassword, String newPassword) {
        if (newPassword == null || newPassword.length() < 4) {
            throw new IllegalArgumentException("Le mot de passe doit contenir au moins 4 caractères.");
        }

        User user = userDAO.findById(userId);
        if (user == null) {
            throw new IllegalArgumentException("Utilisateur non trouvé.");
        }

        // Verify old password
        if (oldPassword != null && !BCrypt.checkpw(oldPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Ancien mot de passe incorrect.");
        }

        // Hash and update new password
        String hashedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt());
        user.setPasswordHash(hashedPassword);
        userDAO.save(user);
    }

    /**
     * Find all users
     */
    public List<User> findAll() {
        return userDAO.findAll();
    }

    /**
     * Update user information (excluding password)
     */
    public User updateUser(User updatedUser) {
        if (updatedUser == null || updatedUser.getId() == null) {
            throw new IllegalArgumentException("Utilisateur invalide.");
        }

        User existingUser = userDAO.findById(updatedUser.getId());
        if (existingUser == null) {
            throw new IllegalArgumentException("Utilisateur non trouvé.");
        }

        // Update fields (don't update password here, use updatePassword for that)
        existingUser.setUsername(updatedUser.getUsername());
        existingUser.setRole(updatedUser.getRole());
        existingUser.setMedecin(updatedUser.getMedecin());

        return userDAO.save(existingUser);
    }

    /**
     * Delete a user
     */
    public void deleteUser(Long userId) {
        User user = userDAO.findById(userId);
        if (user == null) {
            throw new IllegalArgumentException("Utilisateur non trouvé.");
        }
        userDAO.delete(user);
    }

    /**
     * Create a new user
     */
    public User createUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("Utilisateur ne peut pas être null.");
        }

        // Check if username already exists
        if (userDAO.findByUsername(user.getUsername()) != null) {
            throw new IllegalArgumentException("Ce nom d'utilisateur est déjà pris.");
        }

        // Hash password if it's not already hashed
        if (user.getPasswordHash() != null && !user.getPasswordHash().startsWith("$2a$")) {
            String hashed = BCrypt.hashpw(user.getPasswordHash(), BCrypt.gensalt());
            user.setPasswordHash(hashed);
        }

        return userDAO.save(user);
    }

    /**
     * Find user by username
     */
    public User findByUsername(String username) {
        return userDAO.findByUsername(username);
    }

    /**
     * Find user by ID
     */
    public User findById(Long id) {
        return userDAO.findById(id);
    }

    /**
     * Create a user with a plain password (will be hashed)
     */
    public User createUserWithPassword(String username, String plainPassword, Role role) {
        if (userDAO.findByUsername(username) != null) {
            throw new IllegalArgumentException("Ce nom d'utilisateur est déjà pris.");
        }

        String hashed = BCrypt.hashpw(plainPassword, BCrypt.gensalt());
        User newUser = new User(username, hashed, role);
        return userDAO.save(newUser);
    }
}

