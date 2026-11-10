package io.github.khaytul_illia.inventory_manager_api.support.context;

import io.github.khaytul_illia.inventory_manager_api.support.DummyController;
import io.github.khaytul_illia.inventory_manager_api.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(DummyController.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = "logging.level.io.github.khaytul_illia.inventory_manager_api=DEBUG")
@ActiveProfiles("test")
public abstract class ErrorHandlerTestContext {

    @MockitoSpyBean
    protected DummyController dummyController;
    @MockitoBean
    protected SecurityUtils securityUtils;

    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    protected ObjectMapper objectMapper;

}
