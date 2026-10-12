package io.github.khaytul_illia.inventory_manager_api.product;

import io.github.khaytul_illia.inventory_manager_api.TestcontainersConfiguration;
import io.github.khaytul_illia.inventory_manager_api.auth.request.LoginRequest;
import io.github.khaytul_illia.inventory_manager_api.auth.response.AccessTokenResponse;
import io.github.khaytul_illia.inventory_manager_api.auth.session.UserSessionRepository;
import io.github.khaytul_illia.inventory_manager_api.auth.token.RefreshTokenRepository;
import io.github.khaytul_illia.inventory_manager_api.common.response.pagination.PaginatedResponse;
import io.github.khaytul_illia.inventory_manager_api.product.request.CreateProductRequest;
import io.github.khaytul_illia.inventory_manager_api.product.request.ModifyStockRequest;
import io.github.khaytul_illia.inventory_manager_api.product.request.UpdateProductRequest;
import io.github.khaytul_illia.inventory_manager_api.product.response.ProductResponse;
import io.github.khaytul_illia.inventory_manager_api.product.response.ProductShortResponse;
import io.github.khaytul_illia.inventory_manager_api.user.User;
import io.github.khaytul_illia.inventory_manager_api.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@AutoConfigureRestTestClient
@ActiveProfiles("test")
@DisplayName("Product management API end-to-end tests")
public class ProductManagementE2EIT {

    @Autowired
    private RestTestClient restClient;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserSessionRepository sessionRepository;
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @LocalServerPort
    private int port;

    private final String username = "username";
    private final String password = "valid_pa55word";
    private Product product;

    @BeforeEach
    void beforeEach(){
        userRepository.save(new User(null, username, passwordEncoder.encode(password), User.UserRole.OPERATOR));

        createNewAuditedEntities(
            () -> product = productRepository.save(new Product(null, "Original Product", null, 0, new BigDecimal("99.99"), null, null, null, null, null))
        );
    }

    @AfterEach
    void afterEach(){
        productRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        sessionRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Operator logs in, creates a new product, changes its stock, then deletes it, product gets deleted")
    void operatorLogin_createProduct_changeProductStock_deleteProduct_productGetsDeleted(){
        //Login
        LoginRequest loginRequest = new LoginRequest(username, password);

        AccessTokenResponse loginResponse = performSuccessfulLogin(loginRequest);
        String accessToken = loginResponse.accessToken();

        //Create product
        CreateProductRequest createRequest = new CreateProductRequest("Cool Product", null, new BigDecimal("99.99"));

        ProductResponse createResponse = performSuccessfulCreateProduct(createRequest, accessToken);
        long productId = createResponse.id();

        assertThat(productRepository.existsById(productId)).isTrue();

        //Change product stock
        ModifyStockRequest stockRequest = new ModifyStockRequest(10);

        performSuccessfulChangeProductStock(productId, createResponse.stock(), stockRequest, accessToken);

        Product stockProduct = productRepository.findById(productId).orElseThrow();
        assertThat(stockProduct.getStock()).isEqualTo(stockRequest.stockChange());

        //Delete product
        performSuccessfulDeleteProduct(productId, accessToken);

        assertThat(productRepository.existsById(productId)).isFalse();
    }

    @Test
    @DisplayName("Operator logs in, updates product, then gets product, operator sees audit fields")
    void operatorLogin_updateProduct_getProduct_operatorSeesAuditFields(){
        //Login
        LoginRequest loginRequest = new LoginRequest(username, password);

        AccessTokenResponse loginResponse = performSuccessfulLogin(loginRequest);
        String accessToken = loginResponse.accessToken();

        //Update product
        long productId = product.getId();
        String newName = "New Product";
        String newPrice = "110.50";
        UpdateProductRequest updateRequest = new UpdateProductRequest(newName, null, new BigDecimal(newPrice));

        performSuccessfulUpdateProduct(productId, updateRequest, accessToken);

        Product stockProduct = productRepository.findById(productId).orElseThrow();
        assertThat(stockProduct.getName()).isEqualTo(newName);
        assertThat(stockProduct.getDescription()).isEqualTo(product.getDescription());
        assertThat(stockProduct.getPrice()).isEqualTo(newPrice);

        //Get product
        ProductResponse getResponse = performSuccessfulGetProductWithAuth(product.getId(), accessToken);

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

        AccessTokenResponse loginResponse = performSuccessfulLogin(loginRequest);
        String accessToken = loginResponse.accessToken();

        //Delete product
        performSuccessfulDeleteProduct(product.getId(), accessToken);

        assertThat(productRepository.existsById(product.getId())).isFalse();

        //Delete product again
        performSuccessfulDeleteProduct(product.getId(), accessToken);

        assertThat(productRepository.existsById(product.getId())).isFalse();
    }

    @Test
    @DisplayName("User gets a page of products without logging in, gets one product, logs in as customer, then gets another product, user does not see audit fields")
    void getProducts_getProduct_customerLogin_getAnotherProduct_userSeesNoAuditFields(){
        //Arrange
        String customerUsername = username + "_customer";
        userRepository.save(new User(null, customerUsername, passwordEncoder.encode(password), User.UserRole.CUSTOMER));

        createNewAuditedEntities(() -> {
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

        PaginatedResponse<ProductShortResponse> pageResponse = performSuccessfulGetProducts(paginationAndFiltering);

        List<ProductShortResponse> pageContent = pageResponse.content();
        assertThat(pageContent).isNotEmpty();

        ProductShortResponse product1 = pageContent.get(0);
        ProductShortResponse product2 = pageContent.get(1);

        //Get product
        ProductResponse getResponse1 = performSuccessfulGetProductWithNoAuth(product1.id());

        assertThat(getResponse1.name()).isEqualTo(product1.name());
        assertThat(getResponse1.createdAt()).isNull();
        assertThat(getResponse1.createdBy()).isNull();
        assertThat(getResponse1.modifiedAt()).isNull();
        assertThat(getResponse1.modifiedBy()).isNull();

        //Login customer
        LoginRequest loginRequest = new LoginRequest(customerUsername, password);

        AccessTokenResponse loginResponse = performSuccessfulLogin(loginRequest);
        String accessToken = loginResponse.accessToken();

        //Get product
        ProductResponse getResponse2 = performSuccessfulGetProductWithAuth(product2.id(), accessToken);

        assertThat(getResponse2.name()).isEqualTo(product2.name());
        assertThat(getResponse2.createdAt()).isNull();
        assertThat(getResponse2.createdBy()).isNull();
        assertThat(getResponse2.modifiedAt()).isNull();
        assertThat(getResponse2.modifiedBy()).isNull();
    }

    /*
            Helper methods
     */

    private ProductResponse performSuccessfulCreateProduct(CreateProductRequest request, String accessToken){
        RestTestClient.ResponseSpec responseSpec = restClient
            .post()
            .uri("/products")
            .header("Authorization", "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .body(objectMapper.writeValueAsString(request))
            .exchange();

        ProductResponse response = responseSpec.returnResult(ProductResponse.class).getResponseBody();

        assertThat(response).isNotNull();
        assertThat(response.id()).isNotNull();

        responseSpec
            .expectStatus().isCreated()
            .expectHeader().location(String.format("http://localhost:%s/products/%s", port, response.id()));

        return response;
    }

    private void performSuccessfulUpdateProduct(long productId, UpdateProductRequest request, String accessToken){
        ProductResponse response = restClient
            .patch()
            .uri("/products/{productId}", productId)
            .header("Authorization", "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .body(objectMapper.writeValueAsString(request))
            .exchange()
            .expectStatus().isOk()
            .returnResult(ProductResponse.class).getResponseBody();

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(productId);
        assertThat(response.stock()).isEqualTo(0);
    }

    private void performSuccessfulChangeProductStock(long productId, int originalStock, ModifyStockRequest request, String accessToken){
        ProductResponse response = restClient
            .patch()
            .uri("/products/{productId}/stock", productId)
            .header("Authorization", "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .body(objectMapper.writeValueAsString(request))
            .exchange()
            .expectStatus().isOk()
            .returnResult(ProductResponse.class).getResponseBody();

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(productId);
        assertThat(response.stock()).isEqualTo(originalStock + request.stockChange());
    }

    private ProductResponse performSuccessfulGetProductWithAuth(long productId, String accessToken){
        ProductResponse response = restClient
            .get()
            .uri("/products/{productId}", productId)
            .header("Authorization", "Bearer " + accessToken)
            .exchange()
            .expectStatus().isOk()
            .returnResult(ProductResponse.class).getResponseBody();

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(productId);

        return response;
    }

    private ProductResponse performSuccessfulGetProductWithNoAuth(long productId){
        ProductResponse response = restClient
            .get()
            .uri("/products/{productId}", productId)
            .exchange()
            .expectStatus().isOk()
            .returnResult(ProductResponse.class).getResponseBody();

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(productId);

        return response;
    }

    private PaginatedResponse<ProductShortResponse> performSuccessfulGetProducts(Map<String, String> paginationAndFiltering){
        PaginatedResponse<ProductShortResponse> paginatedResponse = restClient
            .get()
            .uri(UriComponentsBuilder
                .fromUriString("/products")
                .queryParams(MultiValueMap.fromSingleValue(paginationAndFiltering))
                .toUriString()
            )
            .exchange()
            .expectStatus().isOk()
            .returnResult(new ParameterizedTypeReference<PaginatedResponse<ProductShortResponse>>() {}).getResponseBody();

        assertThat(paginatedResponse).isNotNull();
        assertThat(paginatedResponse.content()).isNotNull();

        return paginatedResponse;
    }

    private void performSuccessfulDeleteProduct(long productId, String accessToken){
        restClient
            .delete()
            .uri("/products/{productId}", productId)
            .header("Authorization", "Bearer " + accessToken)
            .exchange()
            .expectStatus().isNoContent()
            .expectBody(Void.class);
    }

    private AccessTokenResponse performSuccessfulLogin(LoginRequest request){
        AccessTokenResponse response = restClient
            .post()
            .uri("/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .body(objectMapper.writeValueAsString(request))
            .exchange()
            .expectStatus().isOk()
            .returnResult(AccessTokenResponse.class).getResponseBody();

        assertThat(response).isNotNull();
        assertThat(response.subject()).isEqualTo(request.username());
        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.refreshToken()).isNotBlank();

        return response;
    }

    private void createNewAuditedEntities(Runnable operation){
        Jwt mockJwt = mock(Jwt.class);
        when(mockJwt.getSubject())
            .thenReturn(username);

        SecurityContextHolder.getContext().setAuthentication(
            new TestingAuthenticationToken(mockJwt, password, "ROLE_" + User.UserRole.OPERATOR.name())
        );

        operation.run();

        SecurityContextHolder.getContext().setAuthentication(null);
    }

}
