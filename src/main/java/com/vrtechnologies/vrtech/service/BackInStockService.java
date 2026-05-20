package com.vrtechnologies.vrtech.service;

import com.vrtechnologies.vrtech.dto.request.BackInStockRequestDto;
import com.vrtechnologies.vrtech.dto.response.BackInStockRequestResponse;
import com.vrtechnologies.vrtech.entity.BackInStockRequest;
import com.vrtechnologies.vrtech.entity.Product;
import com.vrtechnologies.vrtech.exception.BadRequestException;
import com.vrtechnologies.vrtech.exception.ResourceNotFoundException;
import com.vrtechnologies.vrtech.repository.BackInStockRequestRepository;
import com.vrtechnologies.vrtech.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BackInStockService {
    private final BackInStockRequestRepository repository;
    private final ProductRepository productRepository;

    public BackInStockService(BackInStockRequestRepository repository, ProductRepository productRepository) {
        this.repository = repository;
        this.productRepository = productRepository;
    }

    public BackInStockRequestResponse create(BackInStockRequestDto request) {
        Product product = productRepository.findById(request.getProductId()).orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        String email = request.getEmail().trim().toLowerCase();
        if (repository.existsByProductIdAndEmailIgnoreCaseAndStatus(product.getId(), email, "WAITING")) {
            throw new BadRequestException("You are already on the back-in-stock list for this product");
        }
        BackInStockRequest entry = new BackInStockRequest();
        entry.setProduct(product);
        entry.setEmail(email);
        entry.setPhone(request.getPhone());
        return toResponse(repository.save(entry));
    }

    public List<BackInStockRequestResponse> latest() {
        return repository.findTop200ByOrderByCreatedAtDescIdDesc().stream().map(this::toResponse).toList();
    }

    public void markProductAvailable(Long productId) {
        List<BackInStockRequest> waiting = repository.findByProductIdAndStatus(productId, "WAITING");
        waiting.forEach(request -> request.setStatus("READY_TO_NOTIFY"));
        repository.saveAll(waiting);
    }

    public BackInStockRequestResponse updateStatus(Long id, String status) {
        String normalizedStatus = status == null ? "" : status.trim().toUpperCase();
        if (!List.of("WAITING", "READY_TO_NOTIFY", "NOTIFIED").contains(normalizedStatus)) {
            throw new BadRequestException("Invalid back-in-stock status");
        }
        BackInStockRequest request = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Back-in-stock request not found"));
        request.setStatus(normalizedStatus);
        return toResponse(repository.save(request));
    }

    public void delete(Long id) {
        BackInStockRequest request = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Back-in-stock request not found"));
        repository.delete(request);
    }

    private BackInStockRequestResponse toResponse(BackInStockRequest request) {
        return BackInStockRequestResponse.builder()
                .id(request.getId())
                .productId(request.getProduct().getId())
                .productTitle(request.getProduct().getTitle())
                .email(request.getEmail())
                .phone(request.getPhone())
                .status(request.getStatus())
                .createdAt(request.getCreatedAt())
                .updatedAt(request.getUpdatedAt())
                .build();
    }
}
