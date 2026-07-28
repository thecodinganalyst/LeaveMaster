package com.practical.leavemaster;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class DockerProfilePropertiesTest {

    @Test
    void shouldConfigurePostgresForDockerProfile() {
        YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
        factory.setResources(new ClassPathResource("application-docker.yaml"));

        assertThat(factory.getObject()).isNotNull();
        assertThat(factory.getObject().getProperty("spring.datasource.url"))
                .isEqualTo("jdbc:postgresql://${POSTGRES_HOST:postgres}:${POSTGRES_PORT:5432}/${POSTGRES_DB:leavemaster}");
        assertThat(factory.getObject().getProperty("spring.datasource.driver-class-name"))
                .isEqualTo("org.postgresql.Driver");
        assertThat(factory.getObject().getProperty("spring.datasource.password")).isEqualTo("${POSTGRES_PASSWORD}");
        assertThat(factory.getObject().getProperty("spring.h2.console.enabled")).isEqualTo("false");
    }

    @Test
    void shouldUseVendorSpecificFlywayMigrations() throws Exception {
        YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
        factory.setResources(new ClassPathResource("application.yaml"));

        assertThat(factory.getObject()).isNotNull();
        assertThat(factory.getObject().getProperty("spring.flyway.locations"))
                .isEqualTo("classpath:db/migration/{vendor}");
        assertThat(new ClassPathResource("db/migration/h2/V1__initial_schema.sql").exists()).isTrue();
        assertThat(new ClassPathResource("db/migration/postgresql/V1__initial_schema.sql").exists()).isTrue();
        assertThat(new ClassPathResource("db/migration/postgresql/V1__initial_schema.sql")
                .getContentAsString(StandardCharsets.UTF_8)).contains("attachment BYTEA");
    }
}
