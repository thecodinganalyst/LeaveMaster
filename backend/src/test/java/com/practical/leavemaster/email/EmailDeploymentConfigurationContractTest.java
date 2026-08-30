package com.practical.leavemaster.email;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class EmailDeploymentConfigurationContractTest {

    @Test
    void shouldMapGithubVariablesThroughTerraformIntoSpringEmailProperties() throws IOException {
        Path backendDir = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        Path repoRoot = Files.exists(backendDir.resolve("src/main/resources/application.yaml"))
                ? backendDir.getParent()
                : backendDir;

        String application = Files.readString(repoRoot.resolve("backend/src/main/resources/application.yaml"));
        String workflow = Files.readString(repoRoot.resolve(".github/workflows/deploy-cloud-run.yml"));
        String terraform = Files.readString(repoRoot.resolve("infra/terraform/main.tf"));

        assertThat(application)
                .contains("provider: ${EMAIL_PROVIDER:disabled}")
                .contains("from-address: ${EMAIL_FROM_ADDRESS:onboarding@resend.dev}")
                .contains("from-name: ${EMAIL_FROM_NAME:LeaveMaster}")
                .contains("api-key: ${RESEND_API_KEY:}");

        assertThat(workflow)
                .contains("TF_VAR_email_provider: ${{ vars.EMAIL_PROVIDER || 'disabled' }}")
                .contains("TF_VAR_resend_api_key_secret_id: ${{ vars.RESEND_API_KEY_SECRET_ID || 'leavemaster-resend-api-key' }}")
                .contains("TF_VAR_email_from_address: ${{ vars.EMAIL_FROM_ADDRESS || 'onboarding@resend.dev' }}")
                .contains("TF_VAR_email_from_name: ${{ vars.EMAIL_FROM_NAME || 'LeaveMaster' }}");

        assertThat(terraform)
                .contains("name  = \"EMAIL_PROVIDER\"")
                .contains("value = local.email_provider")
                .contains("name  = \"EMAIL_FROM_ADDRESS\"")
                .contains("name  = \"EMAIL_FROM_NAME\"")
                .contains("name = \"RESEND_API_KEY\"");
    }
}
