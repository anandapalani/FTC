package com.bells.repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ProductRepository {

    private final List<String> storage = new ArrayList<>();

    public void save(String name) {
        storage.add(name);
    }

    public boolean exists(String name) {
        return storage.contains(name);
    }

    public List<String> findAll() {
        return Collections.unmodifiableList(storage);
    }
}
