package com.example.demo.mapper;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;

import com.example.demo.dto.response.ProductResponse;
import com.example.demo.entity.Product;
import com.example.demo.entity.ProductImage;

@Component
public class ProductMapper {

    public ProductResponse toResponse(Product product) {
        List<ProductImage> productImages = product.getImages().stream()
                .sorted(Comparator.comparing(ProductImage::getSortOrder))
                .toList();
        List<String> imageUrls = productImages.stream()
                .map(ProductImage::getUrl)
                .toList();
        String primaryImageUrl = productImages.stream()
                .filter(image -> Boolean.TRUE.equals(image.getIsPrimary()))
                .map(ProductImage::getUrl)
                .findFirst()
                .orElse(imageUrls.isEmpty() ? null : imageUrls.get(0));

        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .slug(product.getSlug())
                .description(product.getDescription())
                .basePrice(product.getBasePrice())
                .status(product.getStatus())
                .categoryId(product.getCategory().getId())
                .categoryName(product.getCategory().getName())
                .imageUrl(primaryImageUrl)
                .images(imageUrls)
                .createdAt(product.getCreatedAt())
                .build();
    }
}