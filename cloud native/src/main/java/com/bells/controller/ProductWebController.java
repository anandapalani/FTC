package com.bells.controller;

import com.bells.model.Product;
import com.bells.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/web/products")
public class ProductWebController {

    private final ProductService service;

    public ProductWebController(ProductService service) {
        this.service = service;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("products", service.findAll());
        model.addAttribute("newProduct", new Product());
        return "products";
    }

    @PostMapping
    public String add(@Valid @ModelAttribute("newProduct") Product product,
                      BindingResult binding,
                      Model model,
                      RedirectAttributes redirectAttributes) {

        if (binding.hasErrors()) {
            model.addAttribute("products", service.findAll());
            return "products";
        }

        try {
            service.register(product);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Product '" + product.getName() + "' registered successfully.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/web/products";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        service.delete(id);
        redirectAttributes.addFlashAttribute("successMessage", "Product deleted.");
        return "redirect:/web/products";
    }
}
