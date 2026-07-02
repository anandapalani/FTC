package com.bells.service;

import com.bells.model.Product;
import com.bells.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository repository;

    public ProductService(ProductRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<Product> findAll() {
        return repository.findAll();
    }

    @Transactional
    public Product register(Product product) {
        log.info("Registering product: '{}'", product.getName());

        if (repository.existsByNameIgnoreCase(product.getName())) {
            log.warn("Duplicate rejected: '{}'", product.getName());
            throw new IllegalArgumentException(
                    "Product '" + product.getName() + "' is already registered.");
        }

        Product saved = repository.save(product);
        log.info("Saved product with id={}", saved.getId());
        return saved;
    }

    @Transactional
    public void delete(Long id) {
        log.info("Deleting product id={}", id);
        repository.deleteById(id);
    }
}
