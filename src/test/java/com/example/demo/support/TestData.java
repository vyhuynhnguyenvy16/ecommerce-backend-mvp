package com.example.demo.support;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.entity.Cart;
import com.example.demo.entity.CartItem;
import com.example.demo.entity.Category;
import com.example.demo.entity.Product;
import com.example.demo.entity.ProductVariant;
import com.example.demo.entity.User;
import com.example.demo.repository.CartRepository;
import com.example.demo.repository.CategoryRepository;
import com.example.demo.repository.ProductRepository;
import com.example.demo.repository.ProductVariantRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.JwtService;

import lombok.RequiredArgsConstructor;

/** Seeds the minimum data a checkout needs. Every call uses unique emails/slugs/SKUs, so tests never clash. */
@Component
@RequiredArgsConstructor
public class TestData {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final CartRepository cartRepository;
    private final JwtService jwtService;

    public record Customer(Long userId, String bearer) {
    }

    public Customer customer() {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        User user = userRepository.save(User.builder()
                .email("buyer-" + unique + "@example.com")
                .passwordHash("not-used")
                .fullName("Buyer " + unique)
                .build());
        return new Customer(user.getId(), "Bearer " + jwtService.generateAccessToken(user.getId(), "CUSTOMER"));
    }

    /** Creates a variant priced at {@code unitPrice} with {@code stock} units. */
    public ProductVariant variant(BigDecimal unitPrice, int stock) {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        Category category = categoryRepository.save(Category.builder().name("Áo " + unique).slug("ao-" + unique).build());
        Product product = productRepository.save(Product.builder()
                .category(category)
                .name("Áo thun " + unique)
                .slug("ao-thun-" + unique)
                .basePrice(unitPrice)
                .build());
        return variantRepository.save(ProductVariant.builder()
                .product(product)
                .sku("SKU-" + unique)
                .size("M")
                .color("Den")
                .stockQuantity(stock)
                .build());
    }

    @Transactional
    public void putInCart(Customer customer, ProductVariant variant, int quantity) {
        Cart cart = cartRepository.findByUserId(customer.userId())
                .orElseGet(() -> cartRepository.save(Cart.builder()
                        .user(userRepository.getReferenceById(customer.userId()))
                        .build()));
        cart.getItems().add(CartItem.builder()
                .cart(cart)
                .productVariant(variantRepository.getReferenceById(variant.getId()))
                .quantity(quantity)
                .build());
        cartRepository.save(cart);
    }

    public int stockOf(ProductVariant variant) {
        return variantRepository.findById(variant.getId()).orElseThrow().getStockQuantity();
    }
}
