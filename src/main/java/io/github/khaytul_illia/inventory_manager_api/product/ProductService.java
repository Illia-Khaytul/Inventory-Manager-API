package io.github.khaytul_illia.inventory_manager_api.product;

import io.github.khaytul_illia.inventory_manager_api.common.response.pagination.PaginatedResponse;
import io.github.khaytul_illia.inventory_manager_api.error.exception.DuplicateEntryException;
import io.github.khaytul_illia.inventory_manager_api.error.exception.EntityNotFoundException;
import io.github.khaytul_illia.inventory_manager_api.error.exception.InvalidStockModificationException;
import io.github.khaytul_illia.inventory_manager_api.product.request.CreateProductRequest;
import io.github.khaytul_illia.inventory_manager_api.product.request.ModifyStockRequest;
import io.github.khaytul_illia.inventory_manager_api.product.request.ProductFiltering;
import io.github.khaytul_illia.inventory_manager_api.product.request.UpdateProductRequest;
import io.github.khaytul_illia.inventory_manager_api.product.response.ProductResponse;
import io.github.khaytul_illia.inventory_manager_api.product.response.ProductShortResponse;
import io.github.khaytul_illia.inventory_manager_api.security.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductSpecificationBuilder specificationBuilder;
    private final ProductMapper productMapper;
    private final SecurityUtils securityUtils;

    public ProductService(
        ProductRepository productRepository,
        ProductSpecificationBuilder specificationBuilder,
        ProductMapper productMapper,
        SecurityUtils securityUtils
    ) {
        this.productRepository = productRepository;
        this.specificationBuilder = specificationBuilder;
        this.productMapper = productMapper;
        this.securityUtils = securityUtils;
    }

    public ProductResponse createProduct(CreateProductRequest request) {
        log.info("Creating new product of name '{}'", request.name());

        log.debug("Checking if the product name is unique");

        if (productRepository.existsByName(request.name())) {
            throw new DuplicateEntryException("Product with name '%s' already exists", request.name());
        }

        log.debug("Creating new product");

        Product product = productMapper.buildProduct(request);

        product = productRepository.save(product);

        log.info("Successfully created new product with id {}", product.getId());

        return productMapper.toResponse(product);
    }

    @Retryable(includes = OptimisticLockingFailureException.class, maxRetries = 3)
    @Transactional
    public ProductResponse updateProduct(long productId, UpdateProductRequest request) {
        log.info("Updating product with id {}", productId);

        log.debug("Loading product by id");

        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new EntityNotFoundException("Product with id %s does not exist", productId));

        if (request.isEmpty()) {
            log.info("Request is empty, nothing to update");

            return productMapper.toResponse(product);
        }

        log.debug("Checking if the new product name is unique");

        String newName = request.name();
        if (newName != null && !product.getName().equals(newName) && productRepository.existsByName(newName)) {
            throw new DuplicateEntryException("Product with name '%s' already exists", newName);
        }

        log.debug("Updating product with provided data");

        productMapper.updateProduct(product, request);

        product = productRepository.saveAndFlush(product);

        log.info("Successfully updated product");

        return productMapper.toResponse(product);
    }

    @Transactional
    public ProductResponse changeProductStock(long productId, ModifyStockRequest request) {
        log.info("Modifying stock for product with id {}", productId);

        log.debug("Applying stock modification");

        Instant modifiedAt = Instant.now();
        String modifiedBy = securityUtils.getAuthenticatedUserAccessToken().getSubject();
        int updatedRows = productRepository.changeProductStock(productId, request.stockChange(), modifiedAt, modifiedBy);

        log.debug("Loading modified product");

        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new EntityNotFoundException("Product with id %s does not exist", productId));

        log.debug("Checking if the product's stock has been modified");

        if (updatedRows == 0) {
            throw new InvalidStockModificationException(
                "Invalid stock modification",
                String.format("Tried modifying stock by %s for %s existing", request.stockChange(), product.getStock())
            );
        }

        log.info("Successfully changed product stock from {} to {} ({})", product.getStock() - request.stockChange(), product.getStock(), request.stockChange());

        return productMapper.toResponse(product);
    }

    public ProductResponse getProduct(long productId) {
        log.info("Getting product with id {}", productId);

        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new EntityNotFoundException("Product with id %s does not exist", productId));

        log.info("Successfully loaded product");

        return productMapper.toResponse(product);
    }

    public PaginatedResponse<ProductShortResponse> getProducts(Pageable pagination, ProductFiltering filtering){
        log.info("Getting page N-{} of {} products", pagination.getPageNumber(), pagination.getPageSize());

        log.debug("Building product specification from filtering");

        Specification<Product> spec = specificationBuilder.buildSpecificationFromFiltering(filtering);
        Page<Product> products = productRepository.findAll(spec, pagination);

        log.info("Successfully loaded a page of products from a found total of {}", products.getTotalElements());

        return productMapper.toPaginatedResponse(products);
    }

    @Transactional
    public void deleteProduct(long productId){
        log.info("Deleting product with id {}", productId);

        productRepository.deleteProductById(productId);

        log.info("Successfully performed deletion for product");
    }

}
