package com.hotel.service;

import com.hotel.dao.UserDAO;
import com.hotel.model.User;
import com.hotel.util.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/**
 * Service layer for User authentication and management.
 */
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    private final UserDAO userDAO = new UserDAO();

    /**
     * Authenticate with username and plain-text password.
     * On success, stores user in SessionManager.
     */
    public boolean login(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return false;
        }
        Optional<User> userOpt = userDAO.authenticate(username.trim(), password);
        if (userOpt.isPresent()) {
            SessionManager.getInstance().setCurrentUser(userOpt.get());
            logger.info("User '{}' logged in successfully.", username);
            return true;
        }
        logger.warn("Failed login attempt for username: {}", username);
        return false;
    }

    public void logout() {
        String name = SessionManager.getInstance().isLoggedIn()
                ? SessionManager.getInstance().getCurrentUser().getUsername() : "unknown";
        SessionManager.getInstance().clearSession();
        logger.info("User '{}' logged out.", name);
    }

    public List<User> getAllUsers()              { return userDAO.findAll(); }
    public Optional<User> getUserById(int id)   { return userDAO.findById(id); }

    public int createUser(User user) {
        validateUser(user);
        return userDAO.save(user);
    }

    public boolean updateUser(User user) {
        return userDAO.update(user);
    }

    public boolean deactivateUser(int userId) {
        return userDAO.delete(userId);
    }

    private void validateUser(User u) {
        if (u.getUsername() == null || u.getUsername().isBlank())
            throw new IllegalArgumentException("Username is required.");
        if (u.getPasswordHash() == null || u.getPasswordHash().length() < 6)
            throw new IllegalArgumentException("Password must be at least 6 characters.");
        if (u.getRole() == null)
            throw new IllegalArgumentException("Role is required.");
    }
}
