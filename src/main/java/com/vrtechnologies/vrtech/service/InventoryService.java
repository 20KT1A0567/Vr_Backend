package com.vrtechnologies.vrtech.service;

import com.vrtechnologies.vrtech.dto.request.StockAdjustmentRequest;
import com.vrtechnologies.vrtech.dto.request.StockTransferRequest;
import com.vrtechnologies.vrtech.dto.response.StockMovementResponse;
import com.vrtechnologies.vrtech.dto.response.StockTransferResponse;
import com.vrtechnologies.vrtech.entity.Product;
import com.vrtechnologies.vrtech.entity.ProductStoreStock;
import com.vrtechnologies.vrtech.entity.StockMovement;
import com.vrtechnologies.vrtech.entity.StockTransfer;
import com.vrtechnologies.vrtech.entity.Store;
import com.vrtechnologies.vrtech.entity.User;
import com.vrtechnologies.vrtech.entity.enums.StockMovementType;
import com.vrtechnologies.vrtech.exception.BadRequestException;
import com.vrtechnologies.vrtech.exception.ResourceNotFoundException;
import com.vrtechnologies.vrtech.repository.ProductRepository;
import com.vrtechnologies.vrtech.repository.ProductStoreStockRepository;
import com.vrtechnologies.vrtech.repository.StockMovementRepository;
import com.vrtechnologies.vrtech.repository.StockTransferRepository;
import com.vrtechnologies.vrtech.repository.StoreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class InventoryService {

    private final ProductRepository productRepository;
    private final ProductStoreStockRepository productStoreStockRepository;
    private final StoreRepository storeRepository;
    private final StockMovementRepository stockMovementRepository;
    private final StockTransferRepository stockTransferRepository;
    private final BackInStockService backInStockService;

    public InventoryService(
            ProductRepository productRepository,
            ProductStoreStockRepository productStoreStockRepository,
            StoreRepository storeRepository,
            StockMovementRepository stockMovementRepository,
            StockTransferRepository stockTransferRepository,
            BackInStockService backInStockService
    ) {
        this.productRepository = productRepository;
        this.productStoreStockRepository = productStoreStockRepository;
        this.storeRepository = storeRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.stockTransferRepository = stockTransferRepository;
        this.backInStockService = backInStockService;
    }

    @Transactional
    public StockMovementResponse adjust(User admin, StockAdjustmentRequest request) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        Store store = request.getStoreId() == null ? null : storeRepository.findById(request.getStoreId())
                .orElseThrow(() -> new ResourceNotFoundException("Store not found"));
        int quantity = request.getQuantity() == null ? 0 : request.getQuantity();
        if (quantity <= 0) {
            throw new BadRequestException("Quantity must be greater than zero");
        }
        StockMovementType type = request.getMovementType() == null ? StockMovementType.ADJUSTMENT : request.getMovementType();
        ProductStoreStock storeStock = store == null ? null : stockRow(product, store);
        int previous = storeStock != null ? safeStock(storeStock.getStockQuantity()) : safeStock(product.getStockQuantity());
        int next = switch (type) {
            case RESTOCK, RETURN_RELEASE, ORDER_CANCEL_RELEASE -> previous + quantity;
            case ADJUSTMENT -> quantity;
            case SALE_RESERVATION -> previous - quantity;
            case TRANSFER_IN, TRANSFER_OUT -> previous;
        };
        if (next < 0) {
            throw new BadRequestException("Stock cannot go below zero");
        }
        if (storeStock != null) {
            storeStock.setStockQuantity(next);
            productStoreStockRepository.save(storeStock);
            recalculateProductStock(product);
        } else {
            product.setStockQuantity(next);
            product.setAvailable(next > 0);
        }
        productRepository.save(product);
        if (previous <= 0 && next > 0) {
            backInStockService.markProductAvailable(product.getId());
        }

        StockMovement movement = new StockMovement();
        movement.setProduct(product);
        movement.setStore(store);
        movement.setMovementType(type);
        movement.setQuantity(quantity);
        movement.setPreviousStock(previous);
        movement.setNewStock(next);
        movement.setReason(request.getReason());
        movement.setActorId(admin.getId());
        movement.setActorEmail(admin.getEmail());
        return toResponse(stockMovementRepository.save(movement));
    }

    public List<StockMovementResponse> latest() {
        return stockMovementRepository.findTop100ByOrderByCreatedAtDescIdDesc().stream().map(this::toResponse).toList();
    }

    public List<StockTransferResponse> latestTransfers() {
        return stockTransferRepository.findTop100ByOrderByCreatedAtDescIdDesc().stream().map(this::toTransferResponse).toList();
    }

    @Transactional
    public StockTransferResponse transfer(User admin, StockTransferRequest request) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        Store fromStore = storeRepository.findById(request.getFromStoreId())
                .orElseThrow(() -> new ResourceNotFoundException("Source store not found"));
        Store toStore = storeRepository.findById(request.getToStoreId())
                .orElseThrow(() -> new ResourceNotFoundException("Destination store not found"));
        if (fromStore.getId().equals(toStore.getId())) {
            throw new BadRequestException("Source and destination stores must be different");
        }
        int quantity = request.getQuantity() == null ? 0 : request.getQuantity();
        if (quantity <= 0) {
            throw new BadRequestException("Quantity must be greater than zero");
        }
        ProductStoreStock sourceStock = stockRow(product, fromStore);
        ProductStoreStock destinationStock = stockRow(product, toStore);
        int sourcePrevious = safeStock(sourceStock.getStockQuantity());
        int destinationPrevious = safeStock(destinationStock.getStockQuantity());
        if (sourcePrevious < quantity) {
            throw new BadRequestException("Transfer quantity cannot be greater than source store stock");
        }
        boolean hasStoreMapping = product.getStores() != null && !product.getStores().isEmpty();
        if (hasStoreMapping && product.getStores().stream().noneMatch((store) -> store.getId().equals(fromStore.getId()))) {
            throw new BadRequestException("Product is not mapped to the source store");
        }
        if (product.getStores() != null) {
            product.getStores().add(fromStore);
            product.getStores().add(toStore);
            productRepository.save(product);
        }

        StockTransfer transfer = new StockTransfer();
        transfer.setProduct(product);
        transfer.setFromStore(fromStore);
        transfer.setToStore(toStore);
        transfer.setQuantity(quantity);
        transfer.setReason(request.getReason());
        transfer.setInitiatedById(admin.getId());
        transfer.setInitiatedByEmail(admin.getEmail());
        transfer = stockTransferRepository.save(transfer);

        String transferReason = buildTransferReason(transfer, request.getReason());
        sourceStock.setStockQuantity(sourcePrevious - quantity);
        destinationStock.setStockQuantity(destinationPrevious + quantity);
        productStoreStockRepository.save(sourceStock);
        productStoreStockRepository.save(destinationStock);
        recalculateProductStock(product);
        productRepository.save(product);

        StockMovement outMovement = createTransferMovement(product, fromStore, StockMovementType.TRANSFER_OUT, quantity, sourcePrevious, sourcePrevious - quantity, transferReason, admin);
        StockMovement inMovement = createTransferMovement(product, toStore, StockMovementType.TRANSFER_IN, quantity, destinationPrevious, destinationPrevious + quantity, transferReason, admin);
        transfer.setOutMovementId(outMovement.getId());
        transfer.setInMovementId(inMovement.getId());
        return toTransferResponse(stockTransferRepository.save(transfer));
    }

    private StockMovement createTransferMovement(Product product, Store store, StockMovementType type, int quantity, int previousStock, int newStock, String reason, User admin) {
        StockMovement movement = new StockMovement();
        movement.setProduct(product);
        movement.setStore(store);
        movement.setMovementType(type);
        movement.setQuantity(quantity);
        movement.setPreviousStock(previousStock);
        movement.setNewStock(newStock);
        movement.setReason(reason);
        movement.setActorId(admin.getId());
        movement.setActorEmail(admin.getEmail());
        return stockMovementRepository.save(movement);
    }

    private String buildTransferReason(StockTransfer transfer, String reason) {
        String suffix = reason == null || reason.isBlank() ? "" : " - " + reason.trim();
        return "Transfer #" + transfer.getId() + ": " + transfer.getFromStore().getName() + " to " + transfer.getToStore().getName() + suffix;
    }

    public ProductStoreStock stockRow(Product product, Store store) {
        return productStoreStockRepository.findByProductIdAndStoreId(product.getId(), store.getId())
                .orElseGet(() -> createInitialStockRow(product, store));
    }

    public void recalculateProductStock(Product product) {
        int total = productStoreStockRepository.findByProductId(product.getId()).stream()
                .mapToInt(row -> safeStock(row.getStockQuantity()))
                .sum();
        product.setStockQuantity(total);
        product.setAvailable(total > 0);
    }

    private ProductStoreStock createInitialStockRow(Product product, Store store) {
        ProductStoreStock row = new ProductStoreStock();
        row.setProduct(product);
        row.setStore(store);
        int initialStock = productStoreStockRepository.findByProductId(product.getId()).isEmpty()
                ? safeStock(product.getStockQuantity())
                : 0;
        row.setStockQuantity(initialStock);
        return productStoreStockRepository.save(row);
    }

    private int safeStock(Integer value) {
        return value == null ? 0 : value;
    }

    private StockMovementResponse toResponse(StockMovement movement) {
        return StockMovementResponse.builder()
                .id(movement.getId())
                .productId(movement.getProduct().getId())
                .productTitle(movement.getProduct().getTitle())
                .storeId(movement.getStore() != null ? movement.getStore().getId() : null)
                .storeName(movement.getStore() != null ? movement.getStore().getName() : null)
                .movementType(movement.getMovementType())
                .quantity(movement.getQuantity())
                .previousStock(movement.getPreviousStock())
                .newStock(movement.getNewStock())
                .reason(movement.getReason())
                .actorEmail(movement.getActorEmail())
                .createdAt(movement.getCreatedAt())
                .build();
    }

    private StockTransferResponse toTransferResponse(StockTransfer transfer) {
        return StockTransferResponse.builder()
                .id(transfer.getId())
                .productId(transfer.getProduct().getId())
                .productTitle(transfer.getProduct().getTitle())
                .fromStoreId(transfer.getFromStore().getId())
                .fromStoreName(transfer.getFromStore().getName())
                .toStoreId(transfer.getToStore().getId())
                .toStoreName(transfer.getToStore().getName())
                .quantity(transfer.getQuantity())
                .reason(transfer.getReason())
                .initiatedById(transfer.getInitiatedById())
                .initiatedByEmail(transfer.getInitiatedByEmail())
                .outMovementId(transfer.getOutMovementId())
                .inMovementId(transfer.getInMovementId())
                .createdAt(transfer.getCreatedAt())
                .build();
    }
}
