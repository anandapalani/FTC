package com.bells.service;

import com.bells.exception.BusinessRuleViolationException;
import com.bells.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository repository;

    public ProductService(ProductRepository repository) {
        this.repository = repository;
    }

    public void registerProduct(String name) {
        log.info("Attempting to register product: '{}'", name);

        if (name == null || name.isBlank()) {
            log.warn("Registration rejected: product name is blank");
            throw new BusinessRuleViolationException("Product name must not be blank.");
        }

        String trimmed = name.trim();

        if (repository.exists(trimmed)) {
            log.warn("Registration rejected: duplicate product '{}'", trimmed);
            throw new BusinessRuleViolationException("Product '" + trimmed + "' is already registered.");
        }

        repository.save(trimmed);
        log.info("Product '{}' registered successfully", trimmed);
    }

    public java.util.List<String> getAllProducts() {
        return repository.findAll();
    }
}
