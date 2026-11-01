package io.github.khaytul_illia.inventory_manager_api.product;

import io.github.khaytul_illia.inventory_manager_api.auth.request.LoginRequest;
import io.github.khaytul_illia.inventory_manager_api.product.request.CreateProductRequest;
import io.github.khaytul_illia.inventory_manager_api.support.context.E2ETestContext;
import io.github.khaytul_illia.inventory_manager_api.auth.response.AccessTokenResponse;
import io.github.khaytul_illia.inventory_manager_api.common.response.pagination.PaginatedResponse;
import io.github.khaytul_illia.inventory_manager_api.product.request.ModifyStockRequest;
import io.github.khaytul_illia.inventory_manager_api.product.request.UpdateProductRequest;
import io.github.khaytul_illia.inventory_manager_api.product.response.ProductResponse;
import io.github.khaytul_illia.inventory_manager_api.product.response.ProductShortResponse;
import io.github.khaytul_illia.inventory_manager_api.support.data.ProductTestDataFactory;
import io.github.khaytul_illia.inventory_manager_api.support.data.TestDataConstants;
import io.github.khaytul_illia.inventory_manager_api.support.data.UserTestDataFactory;
import io.github.khaytul_illia.inventory_manager_api.support.utils.e2e.ProductRequestUtils;
import io.github.khaytul_illia.inventory_manager_api.user.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

import static io.github.khaytul_illia.inventory_manager_api.support.utils.TestAuthenticationUtils.persistAuditedEntities;
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
            () -> product = productRepository.save(ProductTestDataFactory.newDefaultProduct())
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
        LoginRequest loginRequest = UserTestDataFactory.DEFAULT_LOGIN_REQUEST;

        AccessTokenResponse loginResponse = authRequestUtils.performSuccessfulLogin(loginRequest);
        String accessToken = loginResponse.accessToken();

        //Create product
        CreateProductRequest createProductRequest = ProductTestDataFactory.uniqueCreateProductRequest();

        ProductResponse createResponse = requestUtils.performSuccessfulCreateProduct(createProductRequest, port, accessToken);
        long productId = createResponse.id();

        assertThat(productRepository.existsById(productId)).isTrue();

        //Change product stock
        ModifyStockRequest stockRequest = ProductTestDataFactory.DEFAULT_MODIFY_STOCK_REQUEST;

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
        LoginRequest loginRequest = UserTestDataFactory.DEFAULT_LOGIN_REQUEST;

        AccessTokenResponse loginResponse = authRequestUtils.performSuccessfulLogin(loginRequest);
        String accessToken = loginResponse.accessToken();

        //Update product
        long productId = product.getId();
        UpdateProductRequest updateRequest = ProductTestDataFactory.DEFAULT_UPDATE_REQUEST;

        requestUtils.performSuccessfulUpdateProduct(productId, updateRequest, accessToken);

        Product stockProduct = productRepository.findById(productId).orElseThrow();
        assertThat(stockProduct.getName()).isEqualTo(updateRequest.name());
        assertThat(stockProduct.getDescription()).isEqualTo(product.getDescription());
        assertThat(stockProduct.getPrice()).isEqualTo(updateRequest.price());

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
        LoginRequest loginRequest = UserTestDataFactory.DEFAULT_LOGIN_REQUEST;

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
        User customer = userRepository.save(UserTestDataFactory.defaults().username().password(passwordEncoder.encode(TestDataConstants.USER_PASSWORD)).build());

        persistAuditedEntities(() -> {
            productRepository.save(ProductTestDataFactory.newUniqueProduct(0, "10"));
            productRepository.save(ProductTestDataFactory.newUniqueProduct(10, "5.9"));
            productRepository.save(ProductTestDataFactory.newUniqueProduct(25, "61.6"));
            productRepository.save(ProductTestDataFactory.newUniqueProduct(17, "20.5"));
            productRepository.save(ProductTestDataFactory.newUniqueProduct(21, "33.99"));
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
        LoginRequest loginRequest = new LoginRequest(customer.getUsername(), TestDataConstants.USER_PASSWORD);

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
