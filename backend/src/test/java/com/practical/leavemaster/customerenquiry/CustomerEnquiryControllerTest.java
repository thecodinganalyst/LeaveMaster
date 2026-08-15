package com.practical.leavemaster.customerenquiry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CustomerEnquiryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CustomerEnquiryRepository repository;

    @BeforeEach
    void cleanUp() {
        repository.deleteAll();
    }

    @Test
    void shouldAcceptUnauthenticatedContactSubmissionAndPersistIt() throws Exception {
        mockMvc.perform(post("/api/public/contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": " Jane Doe ",
                                  "company": " Example Pte Ltd ",
                                  "email": " JANE@EXAMPLE.COM ",
                                  "phone": "+65 6123 4567",
                                  "companySize": "21-100",
                                  "country": "Singapore",
                                  "enquiryType": "PRODUCT_DEMO",
                                  "message": " Please arrange a demo. ",
                                  "website": ""
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Thanks. Your enquiry has been received."));

        assertThat(repository.findAll()).singleElement().satisfies(enquiry -> {
            assertThat(enquiry.getName()).isEqualTo("Jane Doe");
            assertThat(enquiry.getCompany()).isEqualTo("Example Pte Ltd");
            assertThat(enquiry.getEmail()).isEqualTo("jane@example.com");
            assertThat(enquiry.getEnquiryType()).isEqualTo(CustomerEnquiryType.PRODUCT_DEMO);
            assertThat(enquiry.getStatus()).isEqualTo(CustomerEnquiryStatus.NEW);
            assertThat(enquiry.getId()).isNotBlank();
            assertThat(enquiry.getCreatedAt()).isNotNull();
        });
    }

    @Test
    void shouldRejectInvalidSubmissionWithoutPersistingIt() throws Exception {
        mockMvc.perform(post("/api/public/contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Jane Doe",
                                  "company": "",
                                  "email": "not-an-email",
                                  "enquiryType": "OTHER",
                                  "message": "Hello"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("company is required"));

        assertThat(repository.count()).isZero();
    }

    @Test
    void shouldRejectOversizedPayload() throws Exception {
        String largeMessage = "x".repeat(17_000);
        String payload = """
                {"name":"Jane","company":"Example","email":"jane@example.com","enquiryType":"OTHER","message":"%s"}
                """.formatted(largeMessage);

        mockMvc.perform(post("/api/public/contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isPayloadTooLarge());

        assertThat(repository.count()).isZero();
    }

    @Test
    void shouldNotExposeReadEndpointPublicly() throws Exception {
        mockMvc.perform(get("/api/public/contact"))
                .andExpect(status().isUnauthorized());
    }
}
