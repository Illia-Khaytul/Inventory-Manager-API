package io.github.khaytul_illia.inventory_manager_api.support.utils.e2e;

import io.github.khaytul_illia.inventory_manager_api.common.response.pagination.PaginatedResponse;
import io.github.khaytul_illia.inventory_manager_api.product.request.CreateProductRequest;
import io.github.khaytul_illia.inventory_manager_api.product.request.ModifyStockRequest;
import io.github.khaytul_illia.inventory_manager_api.product.request.UpdateProductRequest;
import io.github.khaytul_illia.inventory_manager_api.product.response.ProductResponse;
import io.github.khaytul_illia.inventory_manager_api.product.response.ProductShortResponse;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class ProductRequestUtils {

    private final RestTestClient restClient;
    private final ObjectMapper objectMapper;

    public ProductRequestUtils(RestTestClient restClient, ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
    }

    public ProductResponse performSuccessfulCreateProduct(CreateProductRequest request, int port, String accessToken) {
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

    public ProductResponse performSuccessfulUpdateProduct(long productId, UpdateProductRequest request, String accessToken) {
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

        return response;
    }

    public ProductResponse performSuccessfulChangeProductStock(long productId, int originalStock, ModifyStockRequest request, String accessToken) {
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

        return response;
    }

    public ProductResponse performSuccessfulGetProductWithAuth(long productId, String accessToken) {
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

    public ProductResponse performSuccessfulGetProductWithNoAuth(long productId) {
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

    public PaginatedResponse<ProductShortResponse> performSuccessfulGetProducts(Map<String, String> paginationAndFiltering) {
        PaginatedResponse<ProductShortResponse> paginatedResponse = restClient
            .get()
            .uri(UriComponentsBuilder
                .fromUriString("/products")
                .queryParams(MultiValueMap.fromSingleValue(paginationAndFiltering))
                .toUriString()
            )
            .exchange()
            .expectStatus().isOk()
            .returnResult(new ParameterizedTypeReference<PaginatedResponse<ProductShortResponse>>() {
            }).getResponseBody();

        assertThat(paginatedResponse).isNotNull();
        assertThat(paginatedResponse.content()).isNotNull();

        return paginatedResponse;
    }

    public void performSuccessfulDeleteProduct(long productId, String accessToken) {
        restClient
            .delete()
            .uri("/products/{productId}", productId)
            .header("Authorization", "Bearer " + accessToken)
            .exchange()
            .expectStatus().isNoContent()
            .expectBody(Void.class);
    }

}
