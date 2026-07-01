package com.bells;

import com.bells.controller.ProductController;
import com.bells.repository.ProductRepository;
import com.bells.service.ProductService;

public class Main {

    public static void main(String[] args) {
        ProductRepository repository = new ProductRepository();
        ProductService service = new ProductService(repository);
        ProductController controller = new ProductController(service);
        controller.menu();
    }
}
