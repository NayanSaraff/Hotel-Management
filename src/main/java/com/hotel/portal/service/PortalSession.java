package com.hotel.portal.service;

import com.hotel.portal.model.CustomerAccount;

/**
 * Singleton session store for the Customer Portal.
 * Holds the currently logged-in customer account.
 */
public class PortalSession {

    private static final PortalSession INSTANCE = new PortalSession();

    private CustomerAccount currentAccount;

    private PortalSession() {}

    public static PortalSession getInstance() { return INSTANCE; }

    public void setCurrentAccount(CustomerAccount account) { this.currentAccount = account; }

    public CustomerAccount getCurrentAccount() { return currentAccount; }

    public boolean isLoggedIn() { return currentAccount != null; }

    public void logout() { currentAccount = null; }

    public int getCustomerId() {
        return currentAccount != null ? currentAccount.getCustomerId() : -1;
    }
}
