package com.vrtechnologies.vrtech.service;

import com.vrtechnologies.vrtech.dto.request.AttributeRequest;
import com.vrtechnologies.vrtech.dto.request.AttributeValueRequest;
import com.vrtechnologies.vrtech.dto.request.BulkVariantsRequest;
import com.vrtechnologies.vrtech.dto.request.ProductVariantRequest;
import com.vrtechnologies.vrtech.dto.request.CreateProductVariantRequest;
import com.vrtechnologies.vrtech.dto.request.UpdateProductVariantRequest;
import com.vrtechnologies.vrtech.dto.response.AttributeResponse;
import com.vrtechnologies.vrtech.dto.response.ProductVariantResponse;
import com.vrtechnologies.vrtech.entity.Attribute;
import com.vrtechnologies.vrtech.entity.AttributeValue;
import com.vrtechnologies.vrtech.entity.Product;
import com.vrtechnologies.vrtech.entity.ProductVariant;
import com.vrtechnologies.vrtech.exception.BadRequestException;
import com.vrtechnologies.vrtech.exception.ResourceNotFoundException;
import com.vrtechnologies.vrtech.repository.AttributeRepository;
import com.vrtechnologies.vrtech.repository.AttributeValueRepository;
import com.vrtechnologies.vrtech.repository.ProductRepository;
import com.vrtechnologies.vrtech.repository.ProductVariantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProductVariantService {

    private final ProductRepository productRepository;
    private final AttributeRepository attributeRepository;
    private final AttributeValueRepository attributeValueRepository;
    private final ProductVariantRepository productVariantRepository;

    public ProductVariantService(
            ProductRepository productRepository,
            AttributeRepository attributeRepository,
            AttributeValueRepository attributeValueRepository,
            ProductVariantRepository productVariantRepository
    ) {
        this.productRepository = productRepository;
        this.attributeRepository = attributeRepository;
        this.attributeValueRepository = attributeValueRepository;
        this.productVariantRepository = productVariantRepository;
    }

    @Transactional(readOnly = true)
    public List<AttributeResponse> getAllAttributes() {
        return attributeRepository.findAll().stream()
                .map(this::toAttributeResponse)
                .toList();
    }

    @Transactional
    public AttributeResponse createAttribute(AttributeRequest request) {
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new BadRequestException("Attribute name is required");
        }

        String nameNormalized = request.getName().trim();
        Attribute attribute = attributeRepository.findByNameIgnoreCase(nameNormalized)
                .orElseGet(() -> {
                    Attribute newAttr = new Attribute();
                    newAttr.setName(nameNormalized);
                    return attributeRepository.save(newAttr);
                });

        if (request.getValues() != null) {
            for (String val : request.getValues()) {
                String valNormalized = val.trim();
                if (!valNormalized.isEmpty() && attributeValueRepository.findByAttributeIdAndValueIgnoreCase(attribute.getId(), valNormalized).isEmpty()) {
                    AttributeValue attrVal = new AttributeValue();
                    attrVal.setAttribute(attribute);
                    attrVal.setValue(valNormalized);
                    attribute.getValues().add(attrVal);
                }
            }
            attributeRepository.save(attribute);
        }

        return toAttributeResponse(attribute);
    }

    @Transactional
    public AttributeResponse.AttributeValueResponse createAttributeValue(Long attributeId, AttributeValueRequest request) {
        Attribute attribute = attributeRepository.findById(attributeId)
                .orElseThrow(() -> new ResourceNotFoundException("Attribute not found"));

        if (request.getValue() == null || request.getValue().trim().isEmpty()) {
            throw new BadRequestException("Attribute value is required");
        }

        String valNormalized = request.getValue().trim();
        AttributeValue attrValue = attributeValueRepository.findByAttributeIdAndValueIgnoreCase(attributeId, valNormalized)
                .orElseGet(() -> {
                    AttributeValue newVal = new AttributeValue();
                    newVal.setAttribute(attribute);
                    newVal.setValue(valNormalized);
                    return attributeValueRepository.save(newVal);
                });

        return AttributeResponse.AttributeValueResponse.builder()
                .id(attrValue.getId())
                .value(attrValue.getValue())
                .build();
    }

    @Transactional(readOnly = true)
    public List<ProductVariantResponse> getVariantsForProduct(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product not found");
        }
        return productVariantRepository.findByProductId(productId).stream()
                .map(this::toVariantResponse)
                .toList();
    }

    @Transactional
    public List<ProductVariantResponse> saveProductVariants(Long productId, BulkVariantsRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        // Delete existing variants
        List<ProductVariant> existing = productVariantRepository.findByProductId(productId);
        productVariantRepository.deleteAll(existing);

        List<ProductVariant> saved = new ArrayList<>();
        if (request.getVariants() != null) {
            for (ProductVariantRequest varReq : request.getVariants()) {
                if (varReq.getSku() == null || varReq.getSku().trim().isEmpty()) {
                    throw new BadRequestException("SKU is required for all variants");
                }
                if (varReq.getPrice() == null) {
                    throw new BadRequestException("Price is required for all variants");
                }

                ProductVariant variant = new ProductVariant();
                variant.setProduct(product);
                variant.setSku(varReq.getSku().trim());
                variant.setPrice(varReq.getPrice());
                variant.setOriginalPrice(varReq.getOriginalPrice());
                variant.setStockQuantity(varReq.getStockQuantity() == null ? 0 : varReq.getStockQuantity());
                variant.setLowStockThreshold(varReq.getLowStockThreshold() == null ? 5 : varReq.getLowStockThreshold());
                variant.setAvailable(varReq.isAvailable());

                Set<AttributeValue> resolvedValues = new LinkedHashSet<>();
                if (varReq.getAttributeSelections() != null) {
                    for (Map.Entry<String, String> selection : varReq.getAttributeSelections().entrySet()) {
                        String attrName = selection.getKey().trim();
                        String attrVal = selection.getValue().trim();

                        if (attrName.isEmpty() || attrVal.isEmpty()) continue;

                        Attribute attribute = attributeRepository.findByNameIgnoreCase(attrName)
                                .orElseGet(() -> {
                                    Attribute newAttr = new Attribute();
                                    newAttr.setName(attrName);
                                    return attributeRepository.save(newAttr);
                                });

                        AttributeValue attributeValue = attributeValueRepository.findByAttributeIdAndValueIgnoreCase(attribute.getId(), attrVal)
                                .orElseGet(() -> {
                                    AttributeValue newVal = new AttributeValue();
                                    newVal.setAttribute(attribute);
                                    newVal.setValue(attrVal);
                                    return attributeValueRepository.save(newVal);
                                });

                        resolvedValues.add(attributeValue);
                    }
                }
                variant.setAttributeValues(resolvedValues);
                saved.add(productVariantRepository.save(variant));
            }
        }

        return saved.stream().map(this::toVariantResponse).toList();
    }

    @Transactional(readOnly = true)
    public ProductVariantResponse queryVariant(Long productId, Map<String, String> attributeSelections) {
        List<ProductVariant> variants = productVariantRepository.findByProductId(productId);
        if (variants.isEmpty()) {
            throw new ResourceNotFoundException("No variants configured for this product");
        }

        for (ProductVariant variant : variants) {
            boolean matchesAll = true;
            for (Map.Entry<String, String> selection : attributeSelections.entrySet()) {
                boolean matchesThis = variant.getAttributeValues().stream()
                        .anyMatch(av -> av.getAttribute().getName().equalsIgnoreCase(selection.getKey())
                                && av.getValue().equalsIgnoreCase(selection.getValue()));
                if (!matchesThis) {
                    matchesAll = false;
                    break;
                }
            }

            // Also check that the counts match to avoid partial selection matching
            if (matchesAll && variant.getAttributeValues().size() == attributeSelections.size()) {
                return toVariantResponse(variant);
            }
        }

        throw new ResourceNotFoundException("No variant found matching the selected combination");
    }

    public AttributeResponse toAttributeResponse(Attribute attribute) {
        return AttributeResponse.builder()
                .id(attribute.getId())
                .name(attribute.getName())
                .values(attribute.getValues().stream()
                        .map(v -> AttributeResponse.AttributeValueResponse.builder()
                                .id(v.getId())
                                .value(v.getValue())
                                .build())
                        .toList())
                .build();
    }

    public ProductVariantResponse toVariantResponse(ProductVariant variant) {
        Map<String, String> attrs = new LinkedHashMap<>();
        for (AttributeValue av : variant.getAttributeValues()) {
            attrs.put(av.getAttribute().getName(), av.getValue());
        }

        return ProductVariantResponse.builder()
                .id(variant.getId())
                .sku(variant.getSku())
                .price(variant.getPrice())
                .originalPrice(variant.getOriginalPrice())
                .stockQuantity(variant.getStockQuantity())
                .lowStockThreshold(variant.getLowStockThreshold())
                .available(variant.getAvailable() == null || variant.getAvailable())
                .attributes(attrs)
                .build();
    }

    @Transactional
    public ProductVariantResponse createProductVariant(Long productId, CreateProductVariantRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        if (request.getPrice() == null) {
            throw new BadRequestException("Price is required");
        }

        String sku = request.getSku();
        if (sku == null || sku.trim().isEmpty()) {
            sku = "VAR-" + productId + "-" + System.currentTimeMillis();
        } else {
            sku = sku.trim();
        }

        if (productVariantRepository.existsBySku(sku)) {
            throw new BadRequestException("A variant with SKU " + sku + " already exists");
        }

        ProductVariant variant = new ProductVariant();
        variant.setProduct(product);
        variant.setSku(sku);
        variant.setPrice(request.getPrice());
        variant.setOriginalPrice(request.getOriginalPrice());
        variant.setStockQuantity(request.getStockQuantity() != null ? request.getStockQuantity() : 0);
        variant.setLowStockThreshold(request.getLowStockThreshold() != null ? request.getLowStockThreshold() : 5);
        variant.setAvailable(request.getAvailable() == null || request.getAvailable());

        if (request.getAttributeValueIds() != null) {
            Set<AttributeValue> attributeValues = new LinkedHashSet<>();
            for (Long id : request.getAttributeValueIds()) {
                AttributeValue val = attributeValueRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Attribute value " + id + " not found"));
                attributeValues.add(val);
            }
            variant.setAttributeValues(attributeValues);
        }

        ProductVariant saved = productVariantRepository.save(variant);
        return toVariantResponse(saved);
    }

    @Transactional
    public ProductVariantResponse updateProductVariant(Long variantId, UpdateProductVariantRequest request) {
        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Product variant not found"));

        if (request.getSku() != null && !request.getSku().trim().isEmpty()) {
            String newSku = request.getSku().trim();
            if (!newSku.equalsIgnoreCase(variant.getSku()) && productVariantRepository.existsBySku(newSku)) {
                throw new BadRequestException("A variant with SKU " + newSku + " already exists");
            }
            variant.setSku(newSku);
        }

        if (request.getPrice() != null) {
            variant.setPrice(request.getPrice());
        }
        if (request.getOriginalPrice() != null) {
            variant.setOriginalPrice(request.getOriginalPrice());
        }
        if (request.getStockQuantity() != null) {
            variant.setStockQuantity(request.getStockQuantity());
        }
        if (request.getLowStockThreshold() != null) {
            variant.setLowStockThreshold(request.getLowStockThreshold());
        }
        if (request.getAvailable() != null) {
            variant.setAvailable(request.getAvailable());
        }

        if (request.getAttributeValueIds() != null) {
            Set<AttributeValue> attributeValues = new LinkedHashSet<>();
            for (Long id : request.getAttributeValueIds()) {
                AttributeValue val = attributeValueRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Attribute value " + id + " not found"));
                attributeValues.add(val);
            }
            variant.setAttributeValues(attributeValues);
        }

        ProductVariant saved = productVariantRepository.save(variant);
        return toVariantResponse(saved);
    }

    @Transactional
    public void deleteProductVariant(Long variantId) {
        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Product variant not found"));
        productVariantRepository.delete(variant);
    }

    @Transactional
    public void deleteAttribute(Long attributeId) {
        Attribute attribute = attributeRepository.findById(attributeId)
                .orElseThrow(() -> new ResourceNotFoundException("Attribute not found"));
        attributeRepository.delete(attribute);
    }

    @Transactional
    public void deleteAttributeValue(Long valueId) {
        AttributeValue val = attributeValueRepository.findById(valueId)
                .orElseThrow(() -> new ResourceNotFoundException("Attribute value not found"));
        attributeValueRepository.delete(val);
    }
}
