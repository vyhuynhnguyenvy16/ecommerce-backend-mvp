package com.example.demo.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class ProductResponse {

    
    private Long id;
    private String name;
    private String slug;
    private String description;
    private BigDecimal basePrice;
    private String status;
    private Long categoryId;
    private String categoryName; 
    private String imageUrl;
    private List<String> images;
    private LocalDateTime createdAt;

}