package com.bells.monolith;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class ProductApp {

    private static final List<String> products = new ArrayList<>();

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("=== Product Registration System ===");
        System.out.println("Commands: add <name> | list | remove <name> | exit");

        while (true) {
            System.out.print("> ");
            String input = scanner.nextLine();

            if (input == null || input.isBlank()) {
                System.out.println("Error: input cannot be empty.");
                continue;
            }

            String[] parts = input.trim().split("\\s+", 2);
            String command = parts[0].toLowerCase();

            switch (command) {
                case "add" -> {
                    if (parts.length < 2 || parts[1].isBlank()) {
                        System.out.println("Error: product name cannot be empty.");
                        break;
                    }
                    String name = parts[1].trim();
                    if (products.contains(name)) {
                        System.out.println("Error: '" + name + "' is already registered.");
                    } else {
                        products.add(name);
                        System.out.println("Added: " + name);
                    }
                }
                case "list" -> {
                    if (products.isEmpty()) {
                        System.out.println("No products registered.");
                    } else {
                        System.out.println("Registered products (" + products.size() + "):");
                        for (int i = 0; i < products.size(); i++) {
                            System.out.println("  " + (i + 1) + ". " + products.get(i));
                        }
                    }
                }
                case "remove" -> {
                    if (parts.length < 2 || parts[1].isBlank()) {
                        System.out.println("Error: product name cannot be empty.");
                        break;
                    }
                    String name = parts[1].trim();
                    if (products.remove(name)) {
                        System.out.println("Removed: " + name);
                    } else {
                        System.out.println("Error: '" + name + "' not found.");
                    }
                }
                case "exit" -> {
                    System.out.println("Goodbye.");
                    scanner.close();
                    return;
                }
                default -> System.out.println("Unknown command: " + command);
            }
        }
    }
}
