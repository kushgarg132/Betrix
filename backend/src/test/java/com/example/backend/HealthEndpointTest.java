package com.example.backend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Deploys and the container HEALTHCHECK poll /actuator/health without a token. The test profile
 * points Mongo at a dead port, so the status may be 503 (DOWN) but must never be 401/403 or 404.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HealthEndpointTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void healthIsReachableWithoutLogin() throws Exception {
        int status = mvc.perform(get("/actuator/health")).andReturn().getResponse().getStatus();
        assertTrue(status == 200 || status == 503, "unexpected status " + status);
    }

    @Test
    void healthDoesNotLeakComponentDetails() throws Exception {
        String body = mvc.perform(get("/actuator/health")).andReturn().getResponse().getContentAsString();
        assertFalse(body.contains("mongo"), body);
    }

    @Test
    void otherActuatorEndpointsAreNotPublic() throws Exception {
        for (String path : new String[] {"/actuator/env", "/actuator/beans", "/actuator/heapdump"}) {
            int status = mvc.perform(get(path)).andReturn().getResponse().getStatus();
            assertTrue(status == 401 || status == 403 || status == 404, path + " -> " + status);
        }
    }
}
