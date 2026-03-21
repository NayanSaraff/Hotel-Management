package com.hotel.service;

import com.hotel.dao.CustomerDAO;
import com.hotel.model.Customer;

import java.util.List;
import java.util.Optional;

/**
 * Service layer for Customer management.
 */
public class CustomerService {

    private final CustomerDAO customerDAO = new CustomerDAO();

    public List<Customer> getAllCustomers()              { return customerDAO.findAll(); }
    public Optional<Customer> getCustomerById(int id)   { return customerDAO.findById(id); }
    public List<Customer> searchCustomers(String kw)    { return customerDAO.search(kw); }

    public int addCustomer(Customer customer) {
        validateCustomer(customer);
        return customerDAO.save(customer);
    }

    public boolean updateCustomer(Customer customer) {
        validateCustomer(customer);
        return customerDAO.update(customer);
    }

    private void validateCustomer(Customer c) {
        if (c.getFirstName() == null || c.getFirstName().isBlank())
            throw new IllegalArgumentException("First name is required.");
        if (c.getPhone() == null || c.getPhone().isBlank())
            throw new IllegalArgumentException("Phone number is required.");
        if (c.getPhone().length() < 10)
            throw new IllegalArgumentException("Phone number must be at least 10 digits.");
    }
}
