package com.practical.leavemaster.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class OAuthDeploymentConfigurationContractTest {

    @Test
    void shouldMapGithubAndGoogleOauthConfigurationIntoCloudRun() throws IOException {
        Path backendDir = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        Path repoRoot = Files.exists(backendDir.resolve("src/main/resources/application.yaml"))
                ? backendDir.getParent()
                : backendDir;

        String application = Files.readString(repoRoot.resolve("backend/src/main/resources/application.yaml"));
        String workflow = Files.readString(repoRoot.resolve(".github/workflows/deploy-cloud-run.yml"));
        String terraformVariables = Files.readString(repoRoot.resolve("infra/terraform/variables.tf"));
        String terraform = Files.readString(repoRoot.resolve("infra/terraform/main.tf"));

        assertThat(application)
                .contains("client-id: ${GH_CLIENT_ID:replace-me}")
                .contains("client-secret: ${GH_CLIENT_SECRET:replace-me}")
                .contains("client-id: ${GOOGLE_CLIENT_ID:replace-me}")
                .contains("client-secret: ${GOOGLE_CLIENT_SECRET:replace-me}");

        assertThat(workflow)
                .contains("TF_VAR_github_oauth_client_id: ${{ vars.GH_CLIENT_ID }}")
                .contains("TF_VAR_github_oauth_client_secret_id: ${{ vars.GH_CLIENT_SECRET_ID || 'leavemaster-github-oauth-client-secret' }}")
                .contains("TF_VAR_google_oauth_client_id: ${{ vars.GOOGLE_CLIENT_ID }}")
                .contains("TF_VAR_google_oauth_client_secret_id: ${{ vars.GOOGLE_CLIENT_SECRET_ID || 'leavemaster-google-oauth-client-secret' }}");

        assertThat(terraformVariables)
                .contains("variable \"github_oauth_client_id\"")
                .contains("variable \"github_oauth_client_secret_id\"")
                .contains("variable \"google_oauth_client_id\"")
                .contains("variable \"google_oauth_client_secret_id\"")
                .contains("lower(trimspace(var.github_oauth_client_id)) != \"replace-me\"")
                .contains("lower(trimspace(var.google_oauth_client_id)) != \"replace-me\"");

        assertThat(terraform)
                .contains("name  = \"GH_CLIENT_ID\"")
                .contains("value = var.github_oauth_client_id")
                .contains("name = \"GH_CLIENT_SECRET\"")
                .contains("data.google_secret_manager_secret.github_oauth_client_secret.secret_id")
                .contains("name  = \"GOOGLE_CLIENT_ID\"")
                .contains("value = var.google_oauth_client_id")
                .contains("name = \"GOOGLE_CLIENT_SECRET\"")
                .contains("data.google_secret_manager_secret.google_oauth_client_secret.secret_id");
    }
}
