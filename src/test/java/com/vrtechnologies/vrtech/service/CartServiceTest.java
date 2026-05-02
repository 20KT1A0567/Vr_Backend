package com.vrtechnologies.vrtech.service;

import com.vrtechnologies.vrtech.dto.request.CartItemRequest;
import com.vrtechnologies.vrtech.dto.response.CartItemResponse;
import com.vrtechnologies.vrtech.dto.response.ProductResponse;
import com.vrtechnologies.vrtech.entity.CartItem;
import com.vrtechnologies.vrtech.entity.Product;
import com.vrtechnologies.vrtech.entity.User;
import com.vrtechnologies.vrtech.repository.CartItemRepository;
import com.vrtechnologies.vrtech.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserContextService userContextService;

    @Mock
    private ProductService productService;

    @InjectMocks
    private CartService cartService;

    @Test
    void getCartAggregatesDuplicateRowsByProduct() {
        User user = createUser(7L);
        Product laptop = createProduct(11L, 10);
        Product monitor = createProduct(12L, 5);
        CartItem firstLaptopRow = createCartItem(1L, user, laptop, 1);
        CartItem secondLaptopRow = createCartItem(2L, user, laptop, 2);
        CartItem monitorRow = createCartItem(3L, user, monitor, 1);

        when(userContextService.getCurrentUser()).thenReturn(user);
        when(cartItemRepository.findByUserIdOrderByIdAsc(user.getId())).thenReturn(List.of(firstLaptopRow, secondLaptopRow, monitorRow));
        when(productService.toProductResponse(laptop)).thenReturn(ProductResponse.builder().id(laptop.getId()).build());
        when(productService.toProductResponse(monitor)).thenReturn(ProductResponse.builder().id(monitor.getId()).build());

        List<CartItemResponse> result = cartService.getCart();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(firstLaptopRow.getId());
        assertThat(result.get(0).getQuantity()).isEqualTo(3);
        assertThat(result.get(0).getProduct().getId()).isEqualTo(laptop.getId());
        assertThat(result.get(1).getId()).isEqualTo(monitorRow.getId());
        assertThat(result.get(1).getQuantity()).isEqualTo(1);
        assertThat(result.get(1).getProduct().getId()).isEqualTo(monitor.getId());
    }

    @Test
    void addToCartMergesDuplicateRowsBeforeReturningCart() {
        User user = createUser(7L);
        Product laptop = createProduct(11L, 10);
        CartItem firstLaptopRow = createCartItem(1L, user, laptop, 1);
        CartItem secondLaptopRow = createCartItem(2L, user, laptop, 2);
        CartItemRequest request = new CartItemRequest();
        request.setProductId(laptop.getId());
        request.setQuantity(1);

        when(userContextService.getCurrentUser()).thenReturn(user);
        when(productRepository.findById(laptop.getId())).thenReturn(Optional.of(laptop));
        when(cartItemRepository.findAllByUserIdAndProductIdOrderByIdAsc(user.getId(), laptop.getId()))
                .thenReturn(List.of(firstLaptopRow, secondLaptopRow));
        when(cartItemRepository.findByUserIdOrderByIdAsc(user.getId())).thenAnswer(invocation -> List.of(firstLaptopRow));
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(productService.toProductResponse(laptop)).thenReturn(ProductResponse.builder().id(laptop.getId()).build());

        List<CartItemResponse> result = cartService.addToCart(request);

        assertThat(firstLaptopRow.getQuantity()).isEqualTo(4);
        assertThat(result).singleElement().satisfies(item -> {
            assertThat(item.getId()).isEqualTo(firstLaptopRow.getId());
            assertThat(item.getQuantity()).isEqualTo(4);
            assertThat(item.getProduct().getId()).isEqualTo(laptop.getId());
        });
        verify(cartItemRepository).save(eq(firstLaptopRow));
        verify(cartItemRepository).deleteAll(eq(List.of(secondLaptopRow)));
    }

    private User createUser(Long id) {
        User user = new User();
        user.setId(id);
        user.setName("Test User");
        user.setEmail("test@example.com");
        return user;
    }

    private Product createProduct(Long id, int stockQuantity) {
        Product product = new Product();
        product.setId(id);
        product.setTitle("Product " + id);
        product.setPrice(BigDecimal.valueOf(1000));
        product.setAvailable(true);
        product.setStockQuantity(stockQuantity);
        return product;
    }

    private CartItem createCartItem(Long id, User user, Product product, int quantity) {
        CartItem item = new CartItem();
        item.setId(id);
        item.setUser(user);
        item.setProduct(product);
        item.setQuantity(quantity);
        return item;
    }
}
