package com.bidforge.api;

import com.bidforge.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@AutoConfigureMockMvc
class AuctionApiIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String sellerToken;
    private String bidderToken;
    private String sellerName;
    private String bidderName;

    @BeforeEach
    void registerAndLogIn() throws Exception {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        sellerName = "apiseller" + unique;
        bidderName = "apibidder" + unique;

        register(sellerName);
        register(bidderName);

        sellerToken = logIn(sellerName);
        bidderToken = logIn(bidderName);
    }


    @Test
    void protectedEndpointRejectsRequestsWithoutAToken() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    @Test
    void protectedEndpointAcceptsAValidToken() throws Exception {
        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + sellerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(sellerName))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void adminEndpointIsForbiddenForOrdinaryUsers() throws Exception {
        mockMvc.perform(get("/api/admin/users").header("Authorization", "Bearer " + bidderToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    @Test
    void browsingAuctionsWorksWithoutSigningIn() throws Exception {
        mockMvc.perform(get("/api/auctions").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void pageSizeIsCappedEvenIfTheClientAsksForMore() throws Exception {

        mockMvc.perform(get("/api/auctions").param("size", "5000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(50));
    }


    @Test
    void englishAuctionFlow_fromCreationToWinner() throws Exception {
        long auctionId = createAuction(sellerToken, """
                {
                  "title": "Integration test camera",
                  "description": "Created by AuctionApiIT",
                  "category": "ELECTRONICS",
                  "auctionType": "ENGLISH",
                  "startingPrice": 100.00,
                  "minIncrement": 10.00,
                  "startTime": "%s",
                  "endTime": "%s"
                }""".formatted(future(1), future(48)));

        mockMvc.perform(post("/api/auctions/{id}/bids", auctionId)
                        .header("Authorization", "Bearer " + bidderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": 100.00}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("INVALID_AUCTION_STATE"));

        mockMvc.perform(post("/api/auctions/{id}/open", auctionId)
                        .header("Authorization", "Bearer " + sellerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"));

        mockMvc.perform(post("/api/auctions/{id}/bids", auctionId)
                        .header("Authorization", "Bearer " + sellerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": 500.00}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("SELLER_CANNOT_BID"));

        mockMvc.perform(post("/api/auctions/{id}/bids", auctionId)
                        .header("Authorization", "Bearer " + bidderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": 50.00}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("BID_TOO_LOW"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("100")));

        mockMvc.perform(post("/api/auctions/{id}/bids", auctionId)
                        .header("Authorization", "Bearer " + bidderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": 100.00}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(100.00));

        mockMvc.perform(post("/api/auctions/{id}/bids", auctionId)
                        .header("Authorization", "Bearer " + bidderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": 105.00}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("110")));

        mockMvc.perform(get("/api/auctions/{id}/bids", auctionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].bidderUsername").value(bidderName));

        mockMvc.perform(post("/api/auctions/{id}/close", auctionId)
                        .header("Authorization", "Bearer " + sellerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"))
                .andExpect(jsonPath("$.result.winnerUsername").value(bidderName))
                .andExpect(jsonPath("$.result.finalPrice").value(100.00));

        mockMvc.perform(post("/api/auctions/{id}/close", auctionId)
                        .header("Authorization", "Bearer " + sellerToken))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/api/auctions/won").header("Authorization", "Bearer " + bidderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }


    @Test
    void sealedAuction_keepsBidsSecretUntilItCloses() throws Exception {
        long auctionId = createAuction(sellerToken, """
                {
                  "title": "Integration test sealed lot",
                  "description": "Bids must stay hidden",
                  "category": "ART",
                  "auctionType": "SEALED_BID",
                  "startingPrice": 200.00,
                  "startTime": "%s",
                  "endTime": "%s"
                }""".formatted(future(1), future(48)));

        mockMvc.perform(post("/api/auctions/{id}/open", auctionId)
                        .header("Authorization", "Bearer " + sellerToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auctions/{id}/bids", auctionId)
                        .header("Authorization", "Bearer " + bidderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": 250.00}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auctions/{id}/bids", auctionId)
                        .header("Authorization", "Bearer " + bidderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": 300.00}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("SEALED_BID_ALREADY_PLACED"));

        mockMvc.perform(get("/api/auctions/{id}/bids", auctionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        mockMvc.perform(get("/api/auctions/{id}", auctionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentHighestBid").doesNotExist());

        mockMvc.perform(get("/api/auctions/{id}/bids", auctionId)
                        .header("Authorization", "Bearer " + bidderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].amount").value(250.00));

        mockMvc.perform(post("/api/auctions/{id}/close", auctionId)
                        .header("Authorization", "Bearer " + sellerToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/auctions/{id}/bids", auctionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }


    @Test
    void creatingAnAuctionWithBrokenRulesIsRejected() throws Exception {
        mockMvc.perform(post("/api/auctions")
                        .header("Authorization", "Bearer " + sellerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Missing increment",
                                  "category": "OTHER",
                                  "auctionType": "ENGLISH",
                                  "startingPrice": 10.00,
                                  "startTime": "%s",
                                  "endTime": "%s"
                                }""".formatted(future(1), future(24))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BUSINESS_RULE_VIOLATION"));

        mockMvc.perform(post("/api/auctions")
                        .header("Authorization", "Bearer " + sellerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Backwards times",
                                  "category": "OTHER",
                                  "auctionType": "ENGLISH",
                                  "startingPrice": 10.00,
                                  "minIncrement": 1.00,
                                  "startTime": "%s",
                                  "endTime": "%s"
                                }""".formatted(future(48), future(24))))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"x","email":"not-an-email","password":"short",
                                 "firstName":"","lastName":""}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }


    private void register(String username) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "email": "%s@example.test",
                                  "password": "Password@123",
                                  "firstName": "Test",
                                  "lastName": "User"
                                }""".formatted(username, username)))
                .andExpect(status().isCreated());
    }

    private String logIn(String username) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username": "%s", "password": "Password@123"}"""
                                .formatted(username)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        String token = body.get("token").asText();
        assertThat(token).isNotBlank();
        return token;
    }

    private long createAuction(String token, String json) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auctions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private static String future(int hours) {
        return Instant.now().plus(hours, ChronoUnit.HOURS).toString();
    }
}
