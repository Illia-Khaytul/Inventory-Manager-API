package io.github.khaytul_illia.inventory_manager_api.support.data;

import io.github.khaytul_illia.inventory_manager_api.common.response.pagination.PaginatedResponse;
import io.github.khaytul_illia.inventory_manager_api.product.Product;
import io.github.khaytul_illia.inventory_manager_api.product.request.CreateProductRequest;
import io.github.khaytul_illia.inventory_manager_api.product.request.ModifyStockRequest;
import io.github.khaytul_illia.inventory_manager_api.product.request.UpdateProductRequest;
import io.github.khaytul_illia.inventory_manager_api.product.response.ProductResponse;
import io.github.khaytul_illia.inventory_manager_api.product.response.ProductShortResponse;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class ProductTestDataFactory {

    public static final CreateProductRequest DEFAULT_CREATE_REQUEST = defaults().toCreateRequest();
    public static final UpdateProductRequest DEFAULT_UPDATE_REQUEST = defaults()
        .name(TestDataConstants.PRODUCT_NAME + "_new")
        .price("1" + TestDataConstants.PRODUCT_PRICE)
        .toUpdateRequest();
    public static final ModifyStockRequest DEFAULT_MODIFY_STOCK_REQUEST = builder()
        .stock(10)
        .toModifyStockRequest();
    public static final ProductResponse DEFAULT_RESPONSE = defaults()
        .id(1L)
        .modification(Instant.now(), TestDataConstants.USER_USERNAME)
        .toResponse();
    public static final PaginatedResponse<ProductShortResponse> DEFAULT_PAGINATED_RESPONSE = new PaginatedResponse<>(0, 10, 1, 3, List.of(
        defaults().id().name().toShortResponse(),
        defaults().id().name().toShortResponse(),
        defaults().id().name().toShortResponse()
    ));

    public static Product newUniqueProduct(){
        return defaults().name().build();
    }

    public static Product newUniqueProduct(int stock, String price){
        return defaults().name().stock(stock).price(price).build();
    }

    public static Product newDefaultProduct(){
        return defaults().build();
    }

    public static Product newProduct(String name, int stock, String price){
        return defaults().name(name).stock(stock).price(price).build();
    }

    public static Product existingUniqueProduct(){
        return defaults().id().name().build();
    }

    public static Product existingDefaultProduct(){
        return defaults().id(1L).build();
    }

    public static CreateProductRequest uniqueCreateProductRequest(){
        return defaults().name().toCreateRequest();
    }

    public static ProductTestDataBuilder defaults(){
        return builder()
            .name(TestDataConstants.PRODUCT_NAME)
            .description(TestDataConstants.PRODUCT_DESCRIPTION)
            .price(TestDataConstants.PRODUCT_PRICE)
            .creation(Instant.now(), TestDataConstants.USER_USERNAME);
    }

    public static ProductTestDataBuilder builder(){
        return new ProductTestDataBuilder();
    }

    public static class ProductTestDataBuilder{

        private Long id;
        private String name;
        private String description;
        private int stock;
        private BigDecimal price;
        private Instant createdAt;
        private String createdBy;
        private Instant modifiedAt;
        private String modifiedBy;

        public ProductTestDataBuilder id(Long id) {
            this.id = id;

            return this;
        }

        public ProductTestDataBuilder id() {
            this.id = TestDataConstants.uniqueValue.getAndIncrement();

            return this;
        }

        public ProductTestDataBuilder name(){
            this.name = TestDataConstants.PRODUCT_NAME + " " + TestDataConstants.uniqueValue.getAndIncrement();

            return this;
        }

        public ProductTestDataBuilder name(String name) {
            this.name = name;

            return this;
        }

        public ProductTestDataBuilder description(String description) {
            this.description = description;

            return this;
        }

        public ProductTestDataBuilder stock(int stock) {
            this.stock = stock;

            return this;
        }

        public ProductTestDataBuilder price(String price) {
            this.price = price == null ? null : new BigDecimal(price);

            return this;
        }

        public ProductTestDataBuilder creation(Instant createdAt, String createdBy) {
            this.createdAt = createdAt;
            this.createdBy = createdBy;

            return this;
        }

        public ProductTestDataBuilder modification(Instant modifiedAt, String modifiedBy) {
            this.modifiedAt = modifiedAt;
            this.modifiedBy = modifiedBy;

            return this;
        }

        public ProductTestDataBuilder audited() {
            this.createdAt = Instant.now();
            this.createdBy = TestDataConstants.USER_USERNAME;
            this.modifiedAt = Instant.now();
            this.modifiedBy = TestDataConstants.USER_USERNAME;

            return this;
        }

        public Product build(){
            return new Product(id, name, description, stock, price, createdAt, createdBy, modifiedAt, modifiedBy, 1);
        }

        public ProductResponse toResponse(){
            return new ProductResponse(id, name, description, stock, price, createdAt, createdBy, modifiedAt, modifiedBy);
        }

        public ProductShortResponse toShortResponse(){
            return new ProductShortResponse(id, name, stock, price);
        }

        public CreateProductRequest toCreateRequest(){
            return new CreateProductRequest(name, description, price);
        }

        public UpdateProductRequest toUpdateRequest(){
            return new UpdateProductRequest(name, description, price);
        }

        public ModifyStockRequest toModifyStockRequest(){
            return new ModifyStockRequest(stock);
        }

    }

}
