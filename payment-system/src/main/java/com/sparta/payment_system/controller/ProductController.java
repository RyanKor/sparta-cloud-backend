package com.sparta.payment_system.controller;

import com.sparta.payment_system.entity.Product;
import com.sparta.payment_system.entity.StockAlert;
import com.sparta.payment_system.repository.ProductRepository;
import com.sparta.payment_system.service.StockService;
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
    private final StockService stockService;
    
    @Autowired
    public ProductController(ProductRepository productRepository, StockService stockService) {
        this.productRepository = productRepository;
        this.stockService = stockService;
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
                product.setStatus(productDetails.getStatus());
                product.setMinStockAlert(productDetails.getMinStockAlert());
                product.setCategory(productDetails.getCategory());
                
                Product updatedProduct = productRepository.save(product);
                return ResponseEntity.ok(updatedProduct);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            System.err.println("상품 업데이트 오류: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(null);
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
            testProduct.setStatus(Product.ProductStatus.ACTIVE);
            testProduct.setMinStockAlert(5);
            testProduct.setCategory("의류");
            
            productRepository.save(testProduct);
            
            return ResponseEntity.ok("테스트 상품이 생성되었습니다. Product ID: 1");
        } catch (Exception e) {
            System.err.println("테스트 상품 생성 오류: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("테스트 상품 생성 실패: " + e.getMessage());
        }
    }
    
    // 디버깅용 상품 정보 조회 API
    @GetMapping("/{id}/debug")
    public ResponseEntity<String> debugProduct(@PathVariable Long id) {
        try {
            Optional<Product> productOptional = productRepository.findById(id);
            if (productOptional.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Product product = productOptional.get();
            String debugInfo = String.format(
                "Product Debug Info:\n" +
                "ID: %d\n" +
                "Name: %s\n" +
                "Stock: %d\n" +
                "MinStockAlert: %d\n" +
                "Status: %s\n" +
                "IsLowStock: %s\n" +
                "CreatedAt: %s\n" +
                "UpdatedAt: %s",
                product.getProductId(),
                product.getName(),
                product.getStock(),
                product.getMinStockAlert(),
                product.getStatus(),
                product.isLowStock(),
                product.getCreatedAt(),
                product.getUpdatedAt()
            );
            
            return ResponseEntity.ok(debugInfo);
        } catch (Exception e) {
            System.err.println("상품 디버그 정보 조회 오류: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("디버그 정보 조회 실패: " + e.getMessage());
        }
    }
    
    // ========== 재고 관리 API ==========
    
    /**
     * 상품 재고 차감
     */
    @PostMapping("/{id}/stock/decrease")
    public ResponseEntity<String> decreaseStock(@PathVariable Long id, @RequestParam int quantity) {
        try {
            System.out.println("재고 차감 API 호출 - Product ID: " + id + ", Quantity: " + quantity);
            
            if (quantity <= 0) {
                return ResponseEntity.badRequest().body("차감할 수량은 0보다 커야 합니다.");
            }
            
            boolean success = stockService.decreaseStock(id, quantity);
            if (success) {
                return ResponseEntity.ok("재고 차감이 완료되었습니다. 차감 수량: " + quantity);
            } else {
                return ResponseEntity.badRequest().body("재고 차감에 실패했습니다.");
            }
            
        } catch (RuntimeException e) {
            System.err.println("재고 차감 API 오류: " + e.getMessage());
            return ResponseEntity.badRequest().body("재고 차감 실패: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("재고 차감 API 예외: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("서버 오류가 발생했습니다: " + e.getMessage());
        }
    }
    
    /**
     * 상품 재고 증가
     */
    @PostMapping("/{id}/stock/increase")
    public ResponseEntity<String> increaseStock(@PathVariable Long id, @RequestParam int quantity) {
        try {
            System.out.println("재고 증가 API 호출 - Product ID: " + id + ", Quantity: " + quantity);
            
            if (quantity <= 0) {
                return ResponseEntity.badRequest().body("증가할 수량은 0보다 커야 합니다.");
            }
            
            boolean success = stockService.increaseStock(id, quantity);
            if (success) {
                return ResponseEntity.ok("재고 증가가 완료되었습니다. 증가 수량: " + quantity);
            } else {
                return ResponseEntity.badRequest().body("재고 증가에 실패했습니다.");
            }
            
        } catch (RuntimeException e) {
            System.err.println("재고 증가 API 오류: " + e.getMessage());
            return ResponseEntity.badRequest().body("재고 증가 실패: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("재고 증가 API 예외: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("서버 오류가 발생했습니다: " + e.getMessage());
        }
    }
    
    /**
     * 상품 상태 변경
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<String> updateProductStatus(@PathVariable Long id, @RequestParam String status) {
        try {
            System.out.println("상품 상태 변경 API 호출 - Product ID: " + id + ", Status: " + status);
            
            Product.ProductStatus newStatus;
            try {
                newStatus = Product.ProductStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body("유효하지 않은 상태입니다. 가능한 상태: ACTIVE, OUT_OF_STOCK, DISCONTINUED, INACTIVE");
            }
            
            boolean success = stockService.updateProductStatus(id, newStatus);
            if (success) {
                return ResponseEntity.ok("상품 상태가 변경되었습니다. 새로운 상태: " + newStatus.getDescription());
            } else {
                return ResponseEntity.badRequest().body("상품 상태 변경에 실패했습니다.");
            }
            
        } catch (RuntimeException e) {
            System.err.println("상품 상태 변경 API 오류: " + e.getMessage());
            return ResponseEntity.badRequest().body("상품 상태 변경 실패: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("상품 상태 변경 API 예외: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("서버 오류가 발생했습니다: " + e.getMessage());
        }
    }
    
    /**
     * 재고 부족 상품 조회
     */
    @GetMapping("/low-stock")
    public ResponseEntity<List<Product>> getLowStockProducts() {
        try {
            System.out.println("재고 부족 상품 조회 API 호출");
            
            List<Product> products = productRepository.findAll();
            List<Product> lowStockProducts = products.stream()
                    .filter(Product::isLowStock)
                    .toList();
            
            return ResponseEntity.ok(lowStockProducts);
            
        } catch (Exception e) {
            System.err.println("재고 부족 상품 조회 API 오류: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 특정 상태의 상품 조회
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<Product>> getProductsByStatus(@PathVariable String status) {
        try {
            System.out.println("상태별 상품 조회 API 호출 - Status: " + status);
            
            Product.ProductStatus productStatus;
            try {
                productStatus = Product.ProductStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(null);
            }
            
            List<Product> products = productRepository.findByStatus(productStatus);
            return ResponseEntity.ok(products);
            
        } catch (Exception e) {
            System.err.println("상태별 상품 조회 API 오류: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
    
    // ========== 재고 알림 API ==========
    
    /**
     * 대기중인 재고 알림 조회
     */
    @GetMapping("/stock-alerts/pending")
    public ResponseEntity<List<StockAlert>> getPendingStockAlerts() {
        try {
            System.out.println("대기중인 재고 알림 조회 API 호출");
            
            List<StockAlert> alerts = stockService.getPendingStockAlerts();
            return ResponseEntity.ok(alerts);
            
        } catch (Exception e) {
            System.err.println("대기중인 재고 알림 조회 API 오류: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 특정 상품의 재고 알림 조회
     */
    @GetMapping("/{id}/stock-alerts")
    public ResponseEntity<List<StockAlert>> getStockAlertsByProduct(@PathVariable Long id) {
        try {
            System.out.println("상품별 재고 알림 조회 API 호출 - Product ID: " + id);
            
            List<StockAlert> alerts = stockService.getStockAlertsByProduct(id);
            return ResponseEntity.ok(alerts);
            
        } catch (Exception e) {
            System.err.println("상품별 재고 알림 조회 API 오류: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 재고 알림 해결 처리
     */
    @PutMapping("/stock-alerts/{alertId}/resolve")
    public ResponseEntity<String> resolveStockAlert(@PathVariable Long alertId) {
        try {
            System.out.println("재고 알림 해결 API 호출 - Alert ID: " + alertId);
            
            boolean success = stockService.resolveStockAlert(alertId);
            if (success) {
                return ResponseEntity.ok("재고 알림이 해결 처리되었습니다.");
            } else {
                return ResponseEntity.badRequest().body("재고 알림 해결에 실패했습니다.");
            }
            
        } catch (RuntimeException e) {
            System.err.println("재고 알림 해결 API 오류: " + e.getMessage());
            return ResponseEntity.badRequest().body("재고 알림 해결 실패: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("재고 알림 해결 API 예외: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("서버 오류가 발생했습니다: " + e.getMessage());
        }
    }
}
