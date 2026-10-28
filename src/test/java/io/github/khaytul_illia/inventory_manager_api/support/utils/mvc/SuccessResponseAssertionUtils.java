package io.github.khaytul_illia.inventory_manager_api.support.utils.mvc;

import io.github.khaytul_illia.inventory_manager_api.auth.response.AccessTokenResponse;
import io.github.khaytul_illia.inventory_manager_api.common.response.pagination.PaginatedResponse;
import io.github.khaytul_illia.inventory_manager_api.product.response.ProductResponse;
import io.github.khaytul_illia.inventory_manager_api.user.response.UserResponse;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class SuccessResponseAssertionUtils {

    public static void assertAccessTokenResponse(ResultActions result, AccessTokenResponse expected) throws Exception {
        result
            .andExpect(jsonPath("$.issuer").value(expected.issuer()))
            .andExpect(jsonPath("$.issued_at").value(expected.issuedAt().toString()))
            .andExpect(jsonPath("$.expires_at").value(expected.expiresAt().toString()))
            .andExpect(jsonPath("$.subject").value(expected.subject()))
            .andExpect(jsonPath("$.role").value(expected.role()))
            .andExpect(jsonPath("$.access_token").value(expected.accessToken()))
            .andExpect(jsonPath("$.refresh_token").value(expected.refreshToken()));
    }

    public static void assertProductResponse(ResultActions result, ProductResponse expected) throws Exception{
        result
            .andExpect(jsonPath("$.id").value(expected.id()))
            .andExpect(jsonPath("$.name").value(expected.name()))
            .andExpect(jsonPath("$.price").value(expected.price().toPlainString()))
            .andExpect(jsonPath("$.created_at").value(expected.createdAt().toString()))
            .andExpect(jsonPath("$.created_by").value(expected.createdBy()))
            .andExpect(jsonPath("$.modified_at").value(expected.modifiedAt().toString()))
            .andExpect(jsonPath("$.modified_by").value(expected.modifiedBy()));
    }

    public static void assertCustomerProductResponse(ResultActions result, ProductResponse expected) throws Exception{
        result
            .andExpect(jsonPath("$.id").value(expected.id()))
            .andExpect(jsonPath("$.name").value(expected.name()))
            .andExpect(jsonPath("$.price").value(expected.price().toPlainString()))
            .andExpect(jsonPath("$.created_at").doesNotExist())
            .andExpect(jsonPath("$.created_by").doesNotExist())
            .andExpect(jsonPath("$.modified_at").doesNotExist())
            .andExpect(jsonPath("$.modified_by").doesNotExist());
    }

    public static void assertUserResponse(ResultActions result, UserResponse expected) throws Exception{
        result
            .andExpect(jsonPath("$.id").value(expected.id()))
            .andExpect(jsonPath("$.username").value(expected.username()))
            .andExpect(jsonPath("$.role").value(expected.role().name()));
    }

    public static void assertPaginatedResponse(ResultActions result, PaginatedResponse<?> expected) throws Exception{
        result
            .andExpect(jsonPath("$.page").value(expected.page()))
            .andExpect(jsonPath("$.page_size").value(expected.pageSize()))
            .andExpect(jsonPath("$.total_pages").value(expected.totalPages()))
            .andExpect(jsonPath("$.total_elements").value(expected.totalElements()))
            .andExpect(jsonPath("$.content").isArray());
    }

    public static void assertNoContentResponse(ResultActions result) throws Exception{
        result
            .andExpect(status().isNoContent())
            .andExpect(jsonPath("$").doesNotExist());
    }

}
