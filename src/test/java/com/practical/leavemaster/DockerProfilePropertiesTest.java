package com.practical.leavemaster;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

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
        assertThat(factory.getObject().getProperty("spring.h2.console.enabled")).isEqualTo("false");
    }
}
