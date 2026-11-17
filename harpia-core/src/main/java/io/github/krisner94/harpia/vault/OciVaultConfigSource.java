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
import org.eclipse.microprofile.config.spi.ConfigSource;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * {@link ConfigSource} que resolve chaves no formato {@code <AMBIENTE>_DB_<CAMPO>}
 * (ex.: {@code HML_DB_URL}, {@code HML_DB_USERNAME}, {@code HML_DB_PASSWORD})
 * buscando o valor no OCI Vault pelo secret {@code <prefixo>-<ambiente>-db-<campo>}.
 *
 * <p>Assim os arquivos {@code application*.yaml} podem continuar usando
 * {@code ${HML_DB_URL}} etc., sem nunca expor a URL, o usuário ou a senha: o
 * valor é lido do Vault durante o boot da aplicação.</p>
 */
public class OciVaultConfigSource implements ConfigSource {

    private static final String NAME = "harpia-vault";
    private static final int ORDINAL = 110; // abaixo de sysprops(400), env(300) e application.yaml(~250)

    private static final Pattern KEY_PATTERN = Pattern.compile("^([A-Z0-9]+)_DB_([A-Z0-9]+)$");

    private final String prefix;
    private final String compartment;
    private final String profile;
    private final String configFile;
    private final ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();

    private volatile BasicAuthenticationDetailsProvider provider;

    public OciVaultConfigSource(String prefix, String compartment, String profile, String configFile) {
        this.prefix = prefix;
        this.compartment = compartment;
        this.profile = profile;
        this.configFile = configFile;
    }

    @Override
    public Set<String> getPropertyNames() {
        return Set.of();
    }

    @Override
    public String getValue(String propertyName) {
        Matcher matcher = KEY_PATTERN.matcher(propertyName);
        if (!matcher.matches()) {
            return null;
        }
        String secretName = prefix + "-" + matcher.group(1).toLowerCase()
                + "-db-" + matcher.group(2).toLowerCase();
        return cache.computeIfAbsent(propertyName, key -> fetchSecret(secretName));
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public int getOrdinal() {
        return ORDINAL;
    }

    private String fetchSecret(String secretName) {
        try {
            String secretId = findSecretId(secretName);
            if (secretId == null) {
                System.err.println("[harpia-vault] secret '" + secretName + "' nao encontrado");
                return null;
            }
            return readSecret(secretId);
        } catch (Exception e) {
            System.err.println("[harpia-vault] falha ao ler '" + secretName + "': " + e.getMessage());
            return null;
        }
    }

    private synchronized BasicAuthenticationDetailsProvider provider() throws Exception {
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
        try (VaultsClient client = VaultsClient.builder().build(provider())) {
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
        try (SecretsClient client = SecretsClient.builder().build(provider())) {
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
}
