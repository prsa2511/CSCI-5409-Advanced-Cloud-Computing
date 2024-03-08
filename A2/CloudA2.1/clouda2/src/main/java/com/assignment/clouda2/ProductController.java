package com.assignment.clouda2;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ProductController {

    @Autowired
    private ProductService productService;

    @PostMapping("/store-products")
    public ResponseEntity<PostProductResponse> storeProducts(@RequestBody ProductRequest productRequest) {
        PostProductResponse postProductResponse = new PostProductResponse();
        try {
            productService.deleteAllProducts();

            List<Product> products = productRequest.getProducts();
            for (Product product : products) {
                // Process each product here, e.g., save to the database
                productService.storeProducts(product);
                System.out.println("Product: " + product.getName() + ", Price: " + product.getPrice() + ", Availability: " + product.getAvailability());
                postProductResponse.setMessage("Success.");
            }
            return ResponseEntity.ok().body(postProductResponse);
        } catch (Exception e) {
            postProductResponse.setMessage("Error");
            return ResponseEntity.ok().body(postProductResponse);
        }
    }

    @GetMapping("/list-products")
    public ResponseEntity<GetProductResponse> listProducts() {
        List<Product> productList = productService.getAllProducts();
        GetProductResponse getProductResponse = new GetProductResponse();
        getProductResponse.setProducts(productList);
        return ResponseEntity.ok().body(getProductResponse);
    }
}

