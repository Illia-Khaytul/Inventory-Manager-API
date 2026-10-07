package io.github.khaytul_illia.inventory_manager_api.product;

import io.github.khaytul_illia.inventory_manager_api.error.exception.DuplicateEntryException;
import io.github.khaytul_illia.inventory_manager_api.error.exception.EntityNotFoundException;
import io.github.khaytul_illia.inventory_manager_api.product.request.CreateProductRequest;
import io.github.khaytul_illia.inventory_manager_api.product.request.UpdateProductRequest;
import io.github.khaytul_illia.inventory_manager_api.product.response.ProductResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    public ProductService(
        ProductRepository productRepository,
        ProductMapper productMapper
    ) {
        this.productRepository = productRepository;
        this.productMapper = productMapper;
    }

    public ProductResponse createProduct(CreateProductRequest request){
        log.info("Creating new product of name '{}'", request.name());

        log.debug("Checking if the product name is unique");

        if(productRepository.existsByName(request.name())){
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
    public ProductResponse updateProduct(long productId, UpdateProductRequest request){
        log.info("Updating product with id {}", productId);

        log.debug("Loading product by id");

        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new EntityNotFoundException("Product with id %s does not exist", productId));

        if(request.isEmpty()){
            log.info("Request is empty, nothing to update");

            return productMapper.toResponse(product);
        }

        log.debug("Checking if the new product name is unique");

        String newName = request.name();
        if(newName != null && !product.getName().equals(newName) && productRepository.existsByName(newName)){
            throw new DuplicateEntryException("Product with name '%s' already exists", newName);
        }

        log.debug("Updating product with provided data");

        productMapper.updateProduct(product, request);

        log.info("Successfully updated product");

        return productMapper.toResponse(product);
    }

}
