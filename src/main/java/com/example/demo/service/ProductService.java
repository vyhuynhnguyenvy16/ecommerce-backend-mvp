package com.example.demo.service;

import com.example.demo.dto.request.ProductRequest;
import com.example.demo.dto.response.ProductResponse;
import com.example.demo.dto.response.ProductVariantResponse;
import com.example.demo.entity.Category;
import com.example.demo.entity.Product;
import com.example.demo.entity.ProductImage;
import com.example.demo.entity.ProductVariant;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.exception.SlugAlreadyExistsException;
import com.example.demo.mapper.ProductMapper;
import com.example.demo.mapper.ProductVariantMapper;
import com.example.demo.repository.CategoryRepository;
import com.example.demo.repository.ProductRepository;
import com.example.demo.repository.ProductVariantRepository;
import com.example.demo.specification.ProductSpecification;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProductService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("name", "basePrice", "createdAt");

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductMapper productMapper;
    private final ProductVariantMapper productVariantMapper;

    @Transactional
    public ProductResponse create(ProductRequest request) {
        // TODO 13: Kiểm tra slug trùng lặp
        if (productRepository.existsBySlug(request.getSlug())) {
            throw new SlugAlreadyExistsException("Product slug already exists: " + request.getSlug());
        }

        // TODO 14: Tìm Category theo ID, đảm bảo danh mục phải tồn tại trước khi gán cho sản phẩm
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + request.getCategoryId()));

        // TODO 15: Build Product mới. Không cần set "status" vì @Builder.Default đã lo việc đó.
        Product product = Product.builder()
                .name(request.getName())
                .slug(request.getSlug())
                .description(request.getDescription())
                .basePrice(request.getBasePrice())
                .category(category)
                .build();

        Product savedProduct = productRepository.save(product);
        if (request.getImageUrls() != null && !request.getImageUrls().isEmpty()) {
            attachImages(savedProduct, request.getImageUrls());
            savedProduct = productRepository.save(savedProduct);
        }

        return productMapper.toResponse(savedProduct);
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        // TODO 17: Tìm Product theo id
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        // TODO 18: Kiểm tra và cập nhật Category nếu có sự thay đổi
        if (!product.getCategory().getId().equals(request.getCategoryId())) {
            Category newCategory = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("New Category not found with id: " + request.getCategoryId()));
            product.setCategory(newCategory);
        }

        // TODO 19: Xử lý cập nhật thông tin và kiểm tra trùng Slug (loại trừ chính nó)
        if (!product.getSlug().equals(request.getSlug())) {
            if (productRepository.existsBySlug(request.getSlug())) {
                throw new SlugAlreadyExistsException("Product slug already exists: " + request.getSlug());
            }
            product.setSlug(request.getSlug());
        }

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setBasePrice(request.getBasePrice());

        if (request.getImageUrls() != null) {
            product.getImages().clear();
            attachImages(product, request.getImageUrls());
        }

        Product updatedProduct = productRepository.save(product);
        return productMapper.toResponse(updatedProduct);
    }

    private void attachImages(Product product, List<String> imageUrls) {
        for (int i = 0; i < imageUrls.size(); i++) {
            ProductImage image = ProductImage.builder()
                    .product(product)
                    .url(imageUrls.get(i))
                    .sortOrder(i)
                    .isPrimary(i == 0)
                    .build();
            product.getImages().add(image);
        }
    }

    public void softDelete(Long id) {
        // TODO 21: Tìm product theo id
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        // TODO 22: Soft-delete bằng cách chuyển trạng thái
        product.setStatus("DELETED");
        productRepository.save(product);
    }

    @Transactional(readOnly = true)
    public ProductResponse getById(Long id) {
        // TODO 23: Lấy chi tiết sản phẩm
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        return productMapper.toResponse(product);
    }

    @Transactional(readOnly = true)
    public List<ProductVariantResponse> getVariantsByProductId(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product not found with id: " + productId);
        }
        return productVariantRepository.findByProductId(productId).stream()
                .map(productVariantMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> search(
            Long categoryId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            String name,
            int page,
            int size,
            String sortBy,
            String sortDirection
    ) {
        if (!ALLOWED_SORT_FIELDS.contains(sortBy)) {
            throw new IllegalArgumentException("Invalid sortBy field: " + sortBy);
        }

        Sort.Direction direction = sortDirection.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Sort sort = Sort.by(direction, sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Product> productPage = productRepository.findAll(
                ProductSpecification.filterBy(categoryId, minPrice, maxPrice, name, "ACTIVE"),
                pageable
        );

        return productPage.map(productMapper::toResponse);
    }

}