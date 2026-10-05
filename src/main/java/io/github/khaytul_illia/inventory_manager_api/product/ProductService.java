package io.github.khaytul_illia.inventory_manager_api.product;

import io.github.khaytul_illia.inventory_manager_api.error.exception.DuplicateEntryException;
import io.github.khaytul_illia.inventory_manager_api.product.request.CreateProductRequest;
import io.github.khaytul_illia.inventory_manager_api.product.response.ProductResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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

}
