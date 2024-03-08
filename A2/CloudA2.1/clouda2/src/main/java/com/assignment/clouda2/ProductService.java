package com.assignment.clouda2;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductService {
    @Autowired
    private ProductRepository productRepository;

    @Transactional
    public void storeProducts(Product products) {
        productRepository.save(products);
    }

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public void deleteAllProducts() {
        productRepository.deleteAll();
    }
}
