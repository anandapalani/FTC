package com.bells.service;

import com.bells.exception.BusinessRuleViolationException;
import com.bells.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository repository;

    private ProductService service;

    @BeforeEach
    void setUp() {
        service = new ProductService(repository);
    }

    @Test
    void registerProduct_successfulSave() {
        when(repository.exists("Widget")).thenReturn(false);

        service.registerProduct("Widget");

        verify(repository).exists("Widget");
        verify(repository).save("Widget");
    }

    @Test
    void registerProduct_blankName_throwsBusinessRuleViolationException() {
        BusinessRuleViolationException ex = assertThrows(
                BusinessRuleViolationException.class,
                () -> service.registerProduct("   ")
        );

        assertEquals("Product name must not be blank.", ex.getMessage());
        verifyNoInteractions(repository);
    }

    @Test
    void registerProduct_nullName_throwsBusinessRuleViolationException() {
        BusinessRuleViolationException ex = assertThrows(
                BusinessRuleViolationException.class,
                () -> service.registerProduct(null)
        );

        assertEquals("Product name must not be blank.", ex.getMessage());
        verifyNoInteractions(repository);
    }

    @Test
    void registerProduct_duplicateName_throwsBusinessRuleViolationException() {
        when(repository.exists("Widget")).thenReturn(true);

        BusinessRuleViolationException ex = assertThrows(
                BusinessRuleViolationException.class,
                () -> service.registerProduct("Widget")
        );

        assertTrue(ex.getMessage().contains("Widget"));
        assertTrue(ex.getMessage().contains("already registered"));
        verify(repository).exists("Widget");
        verify(repository, never()).save(any());
    }

    @Test
    void registerProduct_trimsWhitespaceBeforeSave() {
        when(repository.exists("Widget")).thenReturn(false);

        service.registerProduct("  Widget  ");

        verify(repository).exists("Widget");
        verify(repository).save("Widget");
    }
}
