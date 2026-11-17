package io.github.krisner94.harpia.vault;

import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider;
import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider;
import com.oracle.bmc.secrets.SecretsClient;
import com.oracle.bmc.secrets.model.Base64SecretBundleContentDetails;
import com.oracle.bmc.secrets.requests.GetSecretBundleRequest;
import com.oracle.bmc.secrets.responses.GetSecretBundleResponse;
import com.oracle.bmc.vault.VaultsClient;
import com.oracle.bmc.vault.requests.ListSecretsRequest;
import com.oracle.bmc.vault.responses.ListSecretsResponse;
import io.smallrye.config.ConfigSourceContext;
import io.smallrye.config.ConfigValue;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementação {@link SecretResolver} para o OCI Vault.
 *
 * <p>Configuração (em {@code bootstrap.yaml} ou equivalente):</p>
 * <ul>
 *   <li>{@code harpia.vault.oci.compartment-ocid} — compartimento do Vault (obrigatório);</li>
 *   <li>{@code harpia.vault.oci.profile} — perfil do {@code ~/.oci/config} (padrão: {@code DEFAULT});</li>
 *   <li>{@code harpia.vault.oci.config-file} — caminho do arquivo de config OCI (padrão: local padrão do SDK).</li>
 * </ul>
 */
public class OciSecretResolver implements SecretResolver {

    private final ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();

    private String compartment;
    private String profile = "DEFAULT";
    private String configFile;
    private volatile BasicAuthenticationDetailsProvider provider;

    @Override
    public String provider() {
        return "oci";
    }

    @Override
    public void configure(ConfigSourceContext context) {
        this.compartment = value(context, "harpia.vault.oci.compartment-ocid");
        String p = value(context, "harpia.vault.oci.profile");
        if (p != null && !p.isBlank()) {
            this.profile = p;
        }
        this.configFile = value(context, "harpia.vault.oci.config-file");
    }

    @Override
    public String resolve(String secretName) {
        if (compartment == null || compartment.isBlank()) {
            System.err.println("[harpia-vault:oci] 'harpia.vault.oci.compartment-ocid' nao configurado");
            return null;
        }
        return cache.computeIfAbsent(secretName, this::fetch);
    }

    private String fetch(String secretName) {
        try {
            String secretId = findSecretId(secretName);
            if (secretId == null) {
                System.err.println("[harpia-vault:oci] secret '" + secretName + "' nao encontrado");
                return null;
            }
            return readSecret(secretId);
        } catch (Exception e) {
            System.err.println("[harpia-vault:oci] falha ao ler '" + secretName + "': " + e.getMessage());
            return null;
        }
    }

    private synchronized BasicAuthenticationDetailsProvider authProvider() throws Exception {
        if (provider == null) {
            if (configFile != null && !configFile.isBlank()) {
                provider = new ConfigFileAuthenticationDetailsProvider(configFile, profile);
            } else {
                provider = new ConfigFileAuthenticationDetailsProvider(profile);
            }
        }
        return provider;
    }

    private String findSecretId(String secretName) throws Exception {
        try (VaultsClient client = VaultsClient.builder().build(authProvider())) {
            ListSecretsRequest request = ListSecretsRequest.builder()
                    .compartmentId(compartment)
                    .name(secretName)
                    .build();
            ListSecretsResponse response = client.listSecrets(request);
            if (response.getItems() == null || response.getItems().isEmpty()) {
                return null;
            }
            return response.getItems().get(0).getId();
        }
    }

    private String readSecret(String secretId) throws Exception {
        try (SecretsClient client = SecretsClient.builder().build(authProvider())) {
            GetSecretBundleRequest request = GetSecretBundleRequest.builder()
                    .secretId(secretId)
                    .stage(GetSecretBundleRequest.Stage.Current)
                    .build();
            GetSecretBundleResponse response = client.getSecretBundle(request);
            Base64SecretBundleContentDetails content =
                    (Base64SecretBundleContentDetails) response.getSecretBundle().getSecretBundleContent();
            return new String(Base64.getDecoder().decode(content.getContent()), StandardCharsets.UTF_8);
        }
    }

    private static String value(ConfigSourceContext context, String name) {
        ConfigValue configValue = context.getValue(name);
        return configValue == null ? null : configValue.getValue();
    }
}
