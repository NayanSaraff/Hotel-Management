package com.hotel.util;

import com.hotel.model.User;

/**
 * Singleton SessionManager to hold the currently logged-in user.
 */
public class SessionManager {

    private static SessionManager instance;
    private User currentUser;

    private SessionManager() {}

    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    public boolean isAdmin() {
        return currentUser != null && "ADMIN".equalsIgnoreCase(currentUser.getRole());
    }

    public boolean isManager() {
        return currentUser != null && "MANAGER".equalsIgnoreCase(currentUser.getRole());
    }

    public boolean isReceptionist() {
        return currentUser != null && "RECEPTIONIST".equalsIgnoreCase(currentUser.getRole());
    }

    public void clearSession() {
        currentUser = null;
    }
}
