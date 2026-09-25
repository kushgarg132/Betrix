package com.example.backend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** Google sign-in, CORS and rate limiting through the real security chain and GraphQL endpoint. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "app.rate-limit.auth-per-minute=2")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AuthApiTest {

    private static final String FIREBASE = "https://betrix-b3c24.web.app";

    @Autowired
    private MockMvc mvc;

    private MvcResult preflight(String origin) throws Exception {
        return mvc.perform(options("/graphql")
                .header("Origin", origin)
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "authorization,content-type")).andReturn();
    }

    private String graphql(String query) throws Exception {
        return mvc.perform(post("/graphql").contentType(MediaType.APPLICATION_JSON)
                .content("{\"query\": " + com.fasterxml.jackson.databind.node.TextNode.valueOf(query) + "}"))
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    void corsAllowsTheFrontendOrigins() throws Exception {
        for (String origin : new String[] {FIREBASE, "https://betrix-b3c24.firebaseapp.com", "http://localhost:3000"}) {
            var response = preflight(origin).getResponse();
            assertEquals(200, response.getStatus(), origin);
            assertEquals(origin, response.getHeader("Access-Control-Allow-Origin"));
            assertEquals("true", response.getHeader("Access-Control-Allow-Credentials"));
        }
    }

    @Test
    void corsRejectsEveryOtherOrigin() throws Exception {
        for (String origin : new String[] {"https://evil.example", "http://betrix-b3c24.web.app",
                "https://betrix-b3c24.web.app.evil.example"}) {
            var response = preflight(origin).getResponse();
            assertEquals(403, response.getStatus(), origin);
            assertNull(response.getHeader("Access-Control-Allow-Origin"), origin);
        }
    }

    @Test
    void authAttemptsFromOneIpAreRateLimited() throws Exception {
        String google = "mutation { googleLogin(idToken: \"x\") { token } }";

        graphql(google);
        graphql(google);
        String third = graphql(google);

        assertTrue(third.contains("\"classification\":\"TOO_MANY_REQUESTS\""), third);
        assertTrue(third.contains("Too many attempts"), third);
    }

    @Test
    void aMalformedGoogleTokenIsUnauthorized() throws Exception {
        String body = graphql("mutation { googleLogin(idToken: \"not-a-jwt\") { token } }");

        assertTrue(body.contains("\"classification\":\"UNAUTHORIZED\""), body);
        assertTrue(body.contains("Google sign-in failed"), body);
    }

    @Test
    void devToolingIsOffByDefault() throws Exception {
        String introspection = graphql("{ __schema { types { name } } }");
        assertFalse(introspection.contains("\"types\""), introspection);

        for (String path : new String[] {"/graphiql", "/swagger-ui/index.html", "/v3/api-docs"}) {
            int status = mvc.perform(get(path)).andReturn().getResponse().getStatus();
            assertTrue(status == 401 || status == 403 || status == 404, path + " -> " + status);
        }
    }

    /** Chips are whole numbers; a fractional amount must be refused by the schema before any resolver runs. */
    @Test
    void fractionalChipAmountsAreRejectedBySchemaValidation() throws Exception {
        String body = graphql("mutation { playerAction(gameId: \"g\", input: {actionType: BET, amount: 10.5}) }");

        assertTrue(body.contains("ValidationError") || body.contains("WrongType") || body.contains("10.5"), body);
        assertFalse(body.contains("FORBIDDEN"), body); // never got as far as authorization
    }
}
