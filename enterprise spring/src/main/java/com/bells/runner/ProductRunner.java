package com.bells.runner;

import com.bells.exception.BusinessRuleViolationException;
import com.bells.service.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Scanner;

@Component
public class ProductRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ProductRunner.class);

    private final ProductService service;

    public ProductRunner(ProductService service) {
        this.service = service;
    }

    @Override
    public void run(String... args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("=== Spring Boot Product Registry ===");
        System.out.println("Commands: add <name> | list | exit");

        while (true) {
            System.out.print("> ");
            String input = scanner.nextLine();

            if (input == null || input.isBlank()) {
                System.out.println("[ERROR] Input cannot be empty.");
                continue;
            }

            String[] parts = input.trim().split("\\s+", 2);
            String command = parts[0].toLowerCase();

            switch (command) {
                case "add" -> {
                    String name = parts.length > 1 ? parts[1] : "";
                    try {
                        service.registerProduct(name);
                        System.out.println("[OK] Registered: " + name.trim());
                    } catch (BusinessRuleViolationException e) {
                        System.out.println("[RULE VIOLATION] " + e.getMessage());
                    }
                }
                case "list" -> {
                    List<String> products = service.getAllProducts();
                    if (products.isEmpty()) {
                        System.out.println("[INFO] No products registered.");
                    } else {
                        System.out.println("[INFO] Products (" + products.size() + "):");
                        for (int i = 0; i < products.size(); i++) {
                            System.out.println("  " + (i + 1) + ". " + products.get(i));
                        }
                    }
                }
                case "exit" -> {
                    log.info("Application shutting down via user command");
                    System.out.println("Shutting down. Goodbye.");
                    scanner.close();
                    System.exit(0);
                }
                default -> System.out.println("[ERROR] Unknown command: " + command);
            }
        }
    }
}
