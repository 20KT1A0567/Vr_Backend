package com.vrtechnologies.vrtech.service;

import com.vrtechnologies.vrtech.dto.request.ProductSectionItemRequest;
import com.vrtechnologies.vrtech.dto.request.ProductSectionRequest;
import com.vrtechnologies.vrtech.dto.response.ProductResponse;
import com.vrtechnologies.vrtech.dto.response.ProductSectionResponse;
import com.vrtechnologies.vrtech.entity.Product;
import com.vrtechnologies.vrtech.entity.ProductSection;
import com.vrtechnologies.vrtech.entity.enums.ProductSectionSelectionMode;
import com.vrtechnologies.vrtech.entity.enums.ProductSectionType;
import com.vrtechnologies.vrtech.repository.OrderItemRepository;
import com.vrtechnologies.vrtech.repository.ProductRepository;
import com.vrtechnologies.vrtech.repository.ProductSectionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductSectionServiceTest {

    @Mock
    private ProductSectionRepository productSectionRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private ProductService productService;

    @InjectMocks
    private ProductSectionService productSectionService;

    @Test
    void saveSectionAcceptsSimpleProductIdsContract() {
        Product firstProduct = createProduct(101L, "Refurb Pro 14");
        Product secondProduct = createProduct(102L, "Refurb Air 13");
        ProductSectionRequest request = new ProductSectionRequest();
        request.setTitle("Featured Products");
        request.setSectionType(ProductSectionType.FEATURED_PRODUCTS);
        request.setSelectionMode(ProductSectionSelectionMode.MANUAL);
        request.setProductIds(List.of(101L, 102L));

        when(productRepository.findAllById(List.of(101L, 102L))).thenReturn(List.of(firstProduct, secondProduct));
        when(productService.getPublicProductsByIdsPreservingOrder(List.of(101L, 102L))).thenReturn(List.of(firstProduct, secondProduct));
        when(productService.toProductResponse(firstProduct)).thenReturn(ProductResponse.builder().id(101L).title("Refurb Pro 14").build());
        when(productService.toProductResponse(secondProduct)).thenReturn(ProductResponse.builder().id(102L).title("Refurb Air 13").build());
        when(productSectionRepository.save(any(ProductSection.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductSectionResponse response = productSectionService.saveSection(request, null);

        assertThat(response.getProducts()).extracting(item -> item.getProduct().getId()).containsExactly(101L, 102L);
        assertThat(response.getResolvedProducts()).extracting(ProductResponse::getId).containsExactly(101L, 102L);
    }

    @Test
    void saveSectionPrefersExplicitProductObjectsWhenBothContractsAreProvided() {
        Product firstProduct = createProduct(101L, "Refurb Pro 14");
        ProductSectionItemRequest itemRequest = new ProductSectionItemRequest();
        itemRequest.setProductId(101L);
        itemRequest.setDisplayOrder(0);

        ProductSectionRequest request = new ProductSectionRequest();
        request.setTitle("Featured Products");
        request.setSectionType(ProductSectionType.FEATURED_PRODUCTS);
        request.setSelectionMode(ProductSectionSelectionMode.MANUAL);
        request.setProducts(List.of(itemRequest));
        request.setProductIds(List.of(101L, 102L));

        when(productRepository.findAllById(List.of(101L))).thenReturn(List.of(firstProduct));
        when(productService.getPublicProductsByIdsPreservingOrder(List.of(101L))).thenReturn(List.of(firstProduct));
        when(productService.toProductResponse(firstProduct)).thenReturn(ProductResponse.builder().id(101L).title("Refurb Pro 14").build());
        when(productSectionRepository.save(any(ProductSection.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductSectionResponse response = productSectionService.saveSection(request, null);

        assertThat(response.getProducts()).hasSize(1);
        assertThat(response.getProducts().get(0).getProduct().getId()).isEqualTo(101L);
        assertThat(response.getResolvedProducts()).extracting(ProductResponse::getId).containsExactly(101L);
    }

    private Product createProduct(Long id, String title) {
        Product product = new Product();
        product.setId(id);
        product.setTitle(title);
        product.setPrice(BigDecimal.valueOf(64999));
        product.setAvailable(true);
        return product;
    }
}
