package com.sparta.payment_system.service;

import com.sparta.payment_system.entity.Product;
import com.sparta.payment_system.entity.StockAlert;
import com.sparta.payment_system.repository.ProductRepository;
import com.sparta.payment_system.repository.StockAlertRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class StockService {
    
    private final ProductRepository productRepository;
    private final StockAlertRepository stockAlertRepository;
    
    @Autowired
    public StockService(ProductRepository productRepository, StockAlertRepository stockAlertRepository) {
        this.productRepository = productRepository;
        this.stockAlertRepository = stockAlertRepository;
    }
    
    /**
     * 상품 재고 차감 및 알림 처리
     * @param productId 상품 ID
     * @param quantity 차감할 수량
     * @return 성공 여부
     */
    public boolean decreaseStock(Long productId, int quantity) {
        try {
            System.out.println("재고 차감 요청 - Product ID: " + productId + ", Quantity: " + quantity);
            
            // 1. 상품 조회
            Optional<Product> productOptional = productRepository.findById(productId);
            if (productOptional.isEmpty()) {
                System.err.println("상품을 찾을 수 없습니다. Product ID: " + productId);
                throw new RuntimeException("상품을 찾을 수 없습니다. Product ID: " + productId);
            }
            
            Product product = productOptional.get();
            
            // 2. 상품 상태 확인
            if (product.getStatus() != Product.ProductStatus.ACTIVE) {
                System.err.println("판매 중이 아닌 상품입니다. Product ID: " + productId + ", Status: " + product.getStatus());
                throw new RuntimeException("판매 중이 아닌 상품입니다. Status: " + product.getStatus().getDescription());
            }
            
            // 3. 재고 확인 및 차감
            if (product.getStock() < quantity) {
                System.err.println("재고가 부족합니다. Product ID: " + productId + 
                                 ", Current Stock: " + product.getStock() + ", Requested: " + quantity);
                throw new RuntimeException("재고가 부족합니다. 현재 재고: " + product.getStock() + "개, 요청 수량: " + quantity + "개");
            }
            
            // 4. 재고 차감
            int oldStock = product.getStock();
            System.out.println("재고 차감 전 - Product ID: " + productId + 
                             ", Current Stock: " + oldStock + ", Requested Quantity: " + quantity);
            
            boolean decreaseResult = product.decreaseStock(quantity);
            if (!decreaseResult) {
                System.err.println("재고 차감 실패 - Product ID: " + productId + 
                                 ", Current Stock: " + oldStock + ", Requested Quantity: " + quantity);
                throw new RuntimeException("재고 차감에 실패했습니다.");
            }
            
            Product savedProduct = productRepository.save(product);
            System.out.println("재고 차감 완료 - Product ID: " + productId + 
                             ", Old Stock: " + oldStock + ", New Stock: " + savedProduct.getStock() + 
                             ", Saved Product ID: " + savedProduct.getProductId());
            
            // 5. 재고 상태에 따른 알림 처리
            checkAndCreateStockAlert(product);
            
            return true;
            
        } catch (Exception e) {
            System.err.println("재고 차감 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
    
    /**
     * 상품 재고 증가 및 알림 처리
     * @param productId 상품 ID
     * @param quantity 증가할 수량
     * @return 성공 여부
     */
    public boolean increaseStock(Long productId, int quantity) {
        try {
            System.out.println("재고 증가 요청 - Product ID: " + productId + ", Quantity: " + quantity);
            
            // 1. 상품 조회
            Optional<Product> productOptional = productRepository.findById(productId);
            if (productOptional.isEmpty()) {
                System.err.println("상품을 찾을 수 없습니다. Product ID: " + productId);
                throw new RuntimeException("상품을 찾을 수 없습니다. Product ID: " + productId);
            }
            
            Product product = productOptional.get();
            int oldStock = product.getStock();
            
            // 2. 재고 증가
            product.increaseStock(quantity);
            productRepository.save(product);
            
            System.out.println("재고 증가 완료 - Product ID: " + productId + 
                             ", Old Stock: " + oldStock + ", New Stock: " + product.getStock());
            
            // 3. 재고 복구 알림 처리 (품절에서 재고가 생긴 경우)
            if (oldStock == 0 && product.getStock() > 0) {
                createStockAlert(product, StockAlert.AlertType.STOCK_RESTORED, 
                               "상품 재고가 복구되었습니다. 현재 재고: " + product.getStock() + "개");
            }
            
            return true;
            
        } catch (Exception e) {
            System.err.println("재고 증가 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
    
    /**
     * 상품 상태 변경
     * @param productId 상품 ID
     * @param newStatus 새로운 상태
     * @return 성공 여부
     */
    public boolean updateProductStatus(Long productId, Product.ProductStatus newStatus) {
        try {
            System.out.println("상품 상태 변경 요청 - Product ID: " + productId + ", New Status: " + newStatus);
            
            Optional<Product> productOptional = productRepository.findById(productId);
            if (productOptional.isEmpty()) {
                System.err.println("상품을 찾을 수 없습니다. Product ID: " + productId);
                throw new RuntimeException("상품을 찾을 수 없습니다. Product ID: " + productId);
            }
            
            Product product = productOptional.get();
            Product.ProductStatus oldStatus = product.getStatus();
            product.setStatus(newStatus);
            productRepository.save(product);
            
            System.out.println("상품 상태 변경 완료 - Product ID: " + productId + 
                             ", Old Status: " + oldStatus + ", New Status: " + newStatus);
            
            return true;
            
        } catch (Exception e) {
            System.err.println("상품 상태 변경 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
    
    /**
     * 재고 부족 알림 생성 및 중복 방지
     * @param product 상품 정보
     */
    private void checkAndCreateStockAlert(Product product) {
        try {
            // 1. 재고 상태 확인
            if (product.getStock() == 0) {
                // 품절 알림
                createStockAlertIfNotExists(product, StockAlert.AlertType.OUT_OF_STOCK, 
                                          "상품이 품절되었습니다. Product ID: " + product.getProductId());
                
                // 상품 상태를 품절로 변경
                if (product.getStatus() == Product.ProductStatus.ACTIVE) {
                    product.setStatus(Product.ProductStatus.OUT_OF_STOCK);
                    productRepository.save(product);
                }
                
            } else if (product.isLowStock()) {
                // 재고 부족 알림
                createStockAlertIfNotExists(product, StockAlert.AlertType.LOW_STOCK, 
                                          "재고가 부족합니다. 현재 재고: " + product.getStock() + "개, 최소 재고: " + product.getMinStockAlert() + "개");
            }
            
        } catch (Exception e) {
            System.err.println("재고 알림 생성 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 중복 알림 방지를 위한 알림 생성
     * @param product 상품 정보
     * @param alertType 알림 타입
     * @param message 알림 메시지
     */
    private void createStockAlertIfNotExists(Product product, StockAlert.AlertType alertType, String message) {
        try {
            // 최근 1시간 내 동일한 알림이 있는지 확인
            List<StockAlert> recentAlerts = stockAlertRepository.findRecentPendingAlertsByProductAndType(
                product.getProductId(), alertType);
            
            if (recentAlerts.isEmpty()) {
                createStockAlert(product, alertType, message);
            } else {
                System.out.println("중복 알림 방지 - Product ID: " + product.getProductId() + 
                                 ", Alert Type: " + alertType + ", Recent alerts: " + recentAlerts.size());
            }
            
        } catch (Exception e) {
            System.err.println("중복 알림 확인 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 재고 알림 생성
     * @param product 상품 정보
     * @param alertType 알림 타입
     * @param message 알림 메시지
     */
    private void createStockAlert(Product product, StockAlert.AlertType alertType, String message) {
        try {
            StockAlert alert = new StockAlert(
                product.getProductId(),
                product.getStock(),
                product.getMinStockAlert(),
                alertType,
                message
            );
            
            stockAlertRepository.save(alert);
            System.out.println("재고 알림 생성 완료 - Product ID: " + product.getProductId() + 
                             ", Alert Type: " + alertType + ", Message: " + message);
            
        } catch (Exception e) {
            System.err.println("재고 알림 생성 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 대기중인 재고 알림 조회
     * @return 대기중인 알림 목록
     */
    @Transactional(readOnly = true)
    public List<StockAlert> getPendingStockAlerts() {
        try {
            return stockAlertRepository.findByStatusOrderByCreatedAtAsc(StockAlert.AlertStatus.PENDING);
        } catch (Exception e) {
            System.err.println("대기중인 재고 알림 조회 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
    
    /**
     * 특정 상품의 재고 알림 조회
     * @param productId 상품 ID
     * @return 해당 상품의 알림 목록
     */
    @Transactional(readOnly = true)
    public List<StockAlert> getStockAlertsByProduct(Long productId) {
        try {
            return stockAlertRepository.findByProductIdOrderByCreatedAtDesc(productId);
        } catch (Exception e) {
            System.err.println("상품별 재고 알림 조회 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
    
    /**
     * 알림 해결 처리
     * @param alertId 알림 ID
     * @return 성공 여부
     */
    public boolean resolveStockAlert(Long alertId) {
        try {
            Optional<StockAlert> alertOptional = stockAlertRepository.findById(alertId);
            if (alertOptional.isEmpty()) {
                System.err.println("알림을 찾을 수 없습니다. Alert ID: " + alertId);
                throw new RuntimeException("알림을 찾을 수 없습니다. Alert ID: " + alertId);
            }
            
            StockAlert alert = alertOptional.get();
            alert.resolve();
            stockAlertRepository.save(alert);
            
            System.out.println("알림 해결 처리 완료 - Alert ID: " + alertId);
            return true;
            
        } catch (Exception e) {
            System.err.println("알림 해결 처리 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
}
