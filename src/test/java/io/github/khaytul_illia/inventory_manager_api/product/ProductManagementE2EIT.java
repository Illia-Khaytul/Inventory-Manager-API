package io.github.khaytul_illia.inventory_manager_api.product;

import io.github.khaytul_illia.inventory_manager_api.support.context.E2ETestContext;
import io.github.khaytul_illia.inventory_manager_api.auth.request.LoginRequest;
import io.github.khaytul_illia.inventory_manager_api.auth.response.AccessTokenResponse;
import io.github.khaytul_illia.inventory_manager_api.common.response.pagination.PaginatedResponse;
import io.github.khaytul_illia.inventory_manager_api.product.request.CreateProductRequest;
import io.github.khaytul_illia.inventory_manager_api.product.request.ModifyStockRequest;
import io.github.khaytul_illia.inventory_manager_api.product.request.UpdateProductRequest;
import io.github.khaytul_illia.inventory_manager_api.product.response.ProductResponse;
import io.github.khaytul_illia.inventory_manager_api.product.response.ProductShortResponse;
import io.github.khaytul_illia.inventory_manager_api.support.utils.e2e.ProductRequestUtils;
import io.github.khaytul_illia.inventory_manager_api.user.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static io.github.khaytul_illia.inventory_manager_api.support.utils.TestDataUtils.persistAuditedEntities;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Product management API end-to-end tests")
public class ProductManagementE2EIT extends E2ETestContext {

    @Autowired
    private ProductRepository productRepository;
    private ProductRequestUtils requestUtils;

    private Product product;

    @BeforeEach
    void beforeEach(){
        requestUtils = new ProductRequestUtils(restClient, objectMapper);

        persistAuditedEntities(
            operator,
            () -> product = productRepository.save(new Product(null, "Original Product", null, 0, new BigDecimal("99.99"), null, null, null, null, null))
        );
    }

    @AfterEach
    void afterEach(){
        productRepository.deleteAll();
    }

    @Test
    @DisplayName("Operator logs in, creates a new product, changes its stock, then deletes it, product gets deleted")
    void operatorLogin_createProduct_changeProductStock_deleteProduct_productGetsDeleted(){
        //Login
        LoginRequest loginRequest = new LoginRequest(username, password);

        AccessTokenResponse loginResponse = authRequestUtils.performSuccessfulLogin(loginRequest);
        String accessToken = loginResponse.accessToken();

        //Create product
        CreateProductRequest createRequest = new CreateProductRequest("Cool Product", null, new BigDecimal("99.99"));

        ProductResponse createResponse = requestUtils.performSuccessfulCreateProduct(createRequest, port, accessToken);
        long productId = createResponse.id();

        assertThat(productRepository.existsById(productId)).isTrue();

        //Change product stock
        ModifyStockRequest stockRequest = new ModifyStockRequest(10);

        requestUtils.performSuccessfulChangeProductStock(productId, createResponse.stock(), stockRequest, accessToken);

        Product stockProduct = productRepository.findById(productId).orElseThrow();
        assertThat(stockProduct.getStock()).isEqualTo(stockRequest.stockChange());

        //Delete product
        requestUtils.performSuccessfulDeleteProduct(productId, accessToken);

        assertThat(productRepository.existsById(productId)).isFalse();
    }

    @Test
    @DisplayName("Operator logs in, updates product, then gets product, operator sees audit fields")
    void operatorLogin_updateProduct_getProduct_operatorSeesAuditFields(){
        //Login
        LoginRequest loginRequest = new LoginRequest(username, password);

        AccessTokenResponse loginResponse = authRequestUtils.performSuccessfulLogin(loginRequest);
        String accessToken = loginResponse.accessToken();

        //Update product
        long productId = product.getId();
        String newName = "New Product";
        String newPrice = "110.50";
        UpdateProductRequest updateRequest = new UpdateProductRequest(newName, null, new BigDecimal(newPrice));

        requestUtils.performSuccessfulUpdateProduct(productId, updateRequest, accessToken);

        Product stockProduct = productRepository.findById(productId).orElseThrow();
        assertThat(stockProduct.getName()).isEqualTo(newName);
        assertThat(stockProduct.getDescription()).isEqualTo(product.getDescription());
        assertThat(stockProduct.getPrice()).isEqualTo(newPrice);

        //Get product
        ProductResponse getResponse = requestUtils.performSuccessfulGetProductWithAuth(product.getId(), accessToken);

        assertThat(getResponse.createdAt()).isNotNull();
        assertThat(getResponse.createdBy()).isNotNull();
        assertThat(getResponse.modifiedAt()).isNotNull();
        assertThat(getResponse.modifiedBy()).isNotNull();
    }

    @Test
    @DisplayName("Operator logs in, deletes product, then deletes it again, nothing happens, product already deleted")
    void operatorLogin_deleteProduct_deleteProductAgain_nothingHappens(){
        //Login
        LoginRequest loginRequest = new LoginRequest(username, password);

        AccessTokenResponse loginResponse = authRequestUtils.performSuccessfulLogin(loginRequest);
        String accessToken = loginResponse.accessToken();

        //Delete product
        requestUtils.performSuccessfulDeleteProduct(product.getId(), accessToken);

        assertThat(productRepository.existsById(product.getId())).isFalse();

        //Delete product again
        requestUtils.performSuccessfulDeleteProduct(product.getId(), accessToken);

        assertThat(productRepository.existsById(product.getId())).isFalse();
    }

    @Test
    @DisplayName("User gets a page of products without logging in, gets one product, logs in as customer, then gets another product, user does not see audit fields")
    void getProducts_getProduct_customerLogin_getAnotherProduct_userSeesNoAuditFields(){
        //Arrange
        String customerUsername = username + "_customer";
        userRepository.save(new User(null, customerUsername, passwordEncoder.encode(password), User.UserRole.CUSTOMER));

        persistAuditedEntities(operator, () -> {
            productRepository.save(new Product(null, "Product-1", null, 0, new BigDecimal("10"), null, null, null, null, null));
            productRepository.save(new Product(null, "Product-2", null, 10, new BigDecimal("5.9"), null, null, null, null, null));
            productRepository.save(new Product(null, "Product-3", null, 25, new BigDecimal("61.6"), null, null, null, null, null));
            productRepository.save(new Product(null, "Product-4", null, 17, new BigDecimal("20.5"), null, null, null, null, null));
            productRepository.save(new Product(null, "Product-5", null, 21, new BigDecimal("33.99"), null, null, null, null, null));
        });

        //Get products
        Map<String, String> paginationAndFiltering = Map.of(
            "minStock", "10",
            "maxPrice", "50"
        );

        PaginatedResponse<ProductShortResponse> pageResponse = requestUtils.performSuccessfulGetProducts(paginationAndFiltering);

        List<ProductShortResponse> pageContent = pageResponse.content();
        assertThat(pageContent).isNotEmpty();

        ProductShortResponse product1 = pageContent.get(0);
        ProductShortResponse product2 = pageContent.get(1);

        //Get product
        ProductResponse getResponse1 = requestUtils.performSuccessfulGetProductWithNoAuth(product1.id());

        assertThat(getResponse1.name()).isEqualTo(product1.name());
        assertThat(getResponse1.createdAt()).isNull();
        assertThat(getResponse1.createdBy()).isNull();
        assertThat(getResponse1.modifiedAt()).isNull();
        assertThat(getResponse1.modifiedBy()).isNull();

        //Login customer
        LoginRequest loginRequest = new LoginRequest(customerUsername, password);

        AccessTokenResponse loginResponse = authRequestUtils.performSuccessfulLogin(loginRequest);
        String accessToken = loginResponse.accessToken();

        //Get product
        ProductResponse getResponse2 = requestUtils.performSuccessfulGetProductWithAuth(product2.id(), accessToken);

        assertThat(getResponse2.name()).isEqualTo(product2.name());
        assertThat(getResponse2.createdAt()).isNull();
        assertThat(getResponse2.createdBy()).isNull();
        assertThat(getResponse2.modifiedAt()).isNull();
        assertThat(getResponse2.modifiedBy()).isNull();
    }

}
