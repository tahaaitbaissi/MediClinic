package com.mediclinic.service;

import com.mediclinic.dao.UserDAO;
import com.mediclinic.model.Medecin;
import com.mediclinic.model.Role;
import com.mediclinic.model.User;
import org.mindrot.jbcrypt.BCrypt;

public class AuthService {

    private final UserDAO userDAO;

    public AuthService() {
        this.userDAO = new UserDAO();
    }

    /**
     * Tente de connecter un utilisateur.
     * @return L'objet User si succès.
     * @throws IllegalArgumentException si échec.
     */
    public User authenticate(String username, String password) {
        User user = userDAO.findByUsername(username);

        if (user == null) {
            throw new IllegalArgumentException("Nom d'utilisateur inconnu.");
        }

        // Vérification du mot de passe haché
        if (BCrypt.checkpw(password, user.getPasswordHash())) {
            return user;
        } else {
            throw new IllegalArgumentException("Mot de passe incorrect.");
        }
    }

    /**
     * Crée un compte utilisateur pour un Médecin.
     */
    public void registerMedecin(String username, String password, Medecin medecin) {
        if (userDAO.findByUsername(username) != null) {
            throw new IllegalArgumentException("Ce nom d'utilisateur est déjà pris.");
        }

        String hashed = BCrypt.hashpw(password, BCrypt.gensalt());
        User newUser = new User(username, hashed, Role.MEDECIN);
        newUser.setMedecin(medecin); // Lien avec le profil médecin

        userDAO.save(newUser);
    }

    /**
     * Crée un compte Admin (Fonction utilitaire pour l'initialisation).
     */
    public void registerAdmin(String username, String password) {
        if (userDAO.findByUsername(username) != null) {
            return; // Déjà existant
        }
        String hashed = BCrypt.hashpw(password, BCrypt.gensalt());
        User newUser = new User(username, hashed, Role.ADMIN);
        userDAO.save(newUser);
    }

    /**
     * Crée un compte Secretary.
     */
    public void registerSecretary(String username, String password) {
        if (userDAO.findByUsername(username) != null) {
            throw new IllegalArgumentException("Ce nom d'utilisateur est déjà pris.");
        }

        String hashed = BCrypt.hashpw(password, BCrypt.gensalt());
        User newUser = new User(username, hashed, Role.SEC);
        userDAO.save(newUser);
    }
}