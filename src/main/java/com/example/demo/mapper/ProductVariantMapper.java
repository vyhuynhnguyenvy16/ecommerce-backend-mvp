package com.example.demo.mapper;

import com.example.demo.dto.response.ProductVariantResponse;
import com.example.demo.entity.ProductVariant;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class ProductVariantMapper {

    public ProductVariantResponse toResponse(ProductVariant variant) {
        BigDecimal price = variant.getPriceOverride() != null
                ? variant.getPriceOverride()
                : variant.getProduct().getBasePrice();

        return ProductVariantResponse.builder()
                .id(variant.getId())
                .sku(variant.getSku())
                .size(variant.getSize())
                .color(variant.getColor())
                .price(price)
                .stockQuantity(variant.getStockQuantity())
                .build();
    }
}
