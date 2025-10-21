package com.sparta.payment_system.controller;

import com.sparta.payment_system.entity.Product;
import com.sparta.payment_system.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "*")
public class ProductController {
    
    private final ProductRepository productRepository;
    
    @Autowired
    public ProductController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }
    
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Product API is working!");
    }
    
    @PostMapping
    public ResponseEntity<?> createProduct(@RequestBody Product product) {
        try {
            System.out.println("상품 생성 요청 받음: " + product);
            Product savedProduct = productRepository.save(product);
            System.out.println("상품 저장 완료: " + savedProduct);
            return ResponseEntity.ok(savedProduct);
        } catch (Exception e) {
            System.err.println("상품 생성 에러: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body("상품 생성 실패: " + e.getMessage());
        }
    }
    
    @GetMapping
    public ResponseEntity<List<Product>> getAllProducts() {
        try {
            List<Product> products = productRepository.findAll();
            return ResponseEntity.ok(products);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Product> getProduct(@PathVariable Long id) {
        try {
            Optional<Product> product = productRepository.findById(id);
            return product.map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Product> updateProduct(@PathVariable Long id, @RequestBody Product productDetails) {
        try {
            Optional<Product> productOptional = productRepository.findById(id);
            if (productOptional.isPresent()) {
                Product product = productOptional.get();
                product.setName(productDetails.getName());
                product.setPrice(productDetails.getPrice());
                product.setStock(productDetails.getStock());
                product.setDescription(productDetails.getDescription());
                
                Product updatedProduct = productRepository.save(product);
                return ResponseEntity.ok(updatedProduct);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        try {
            if (productRepository.existsById(id)) {
                productRepository.deleteById(id);
                return ResponseEntity.ok().build();
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/search")
    public ResponseEntity<List<Product>> searchProducts(@RequestParam String name) {
        try {
            List<Product> products = productRepository.findByNameContainingIgnoreCase(name);
            return ResponseEntity.ok(products);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/price-range")
    public ResponseEntity<List<Product>> getProductsByPriceRange(
            @RequestParam Double minPrice, 
            @RequestParam Double maxPrice) {
        try {
            List<Product> products = productRepository.findByPriceBetween(minPrice, maxPrice);
            return ResponseEntity.ok(products);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/in-stock")
    public ResponseEntity<List<Product>> getInStockProducts(@RequestParam Integer minStock) {
        try {
            List<Product> products = productRepository.findByStockGreaterThan(minStock);
            return ResponseEntity.ok(products);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    // 테스트용 상품 생성 API
    @PostMapping("/test-data")
    public ResponseEntity<String> createTestProducts() {
        try {
            // 기존 상품이 있는지 확인
            if (productRepository.existsById(1L)) {
                return ResponseEntity.ok("테스트 상품이 이미 존재합니다.");
            }
            
            // 테스트 상품 생성
            Product testProduct = new Product();
            testProduct.setName("스파르타 티셔츠 (화이트, M)");
            testProduct.setPrice(java.math.BigDecimal.valueOf(1000));
            testProduct.setStock(100);
            testProduct.setDescription("부드러운 코튼 100% 티셔츠. 데일리로 착용하기 좋은 베이직 핏.");
            
            productRepository.save(testProduct);
            
            return ResponseEntity.ok("테스트 상품이 생성되었습니다. Product ID: 1");
        } catch (Exception e) {
            System.err.println("테스트 상품 생성 오류: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("테스트 상품 생성 실패: " + e.getMessage());
        }
    }
}
