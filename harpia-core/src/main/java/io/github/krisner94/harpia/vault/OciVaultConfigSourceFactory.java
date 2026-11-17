package io.github.krisner94.harpia.vault;

import io.smallrye.config.ConfigSourceContext;
import io.smallrye.config.ConfigSourceFactory;
import io.smallrye.config.ConfigValue;
import org.eclipse.microprofile.config.spi.ConfigSource;

import java.util.List;

/**
 * Registra o {@link OciVaultConfigSource} quando o Vault está habilitado.
 *
 * <p>Ativação (qualquer uma):</p>
 * <ul>
 *   <li>{@code harpia.vault.enabled=true};</li>
 *   <li>perfil {@code local} ativo (conveniência para desenvolvimento).</li>
 * </ul>
 *
 * <p>Configuração (não-secreta):</p>
 * <ul>
 *   <li>{@code harpia.vault.compartment-ocid} — compartimento do Vault (obrigatório);</li>
 *   <li>{@code harpia.vault.secret-prefix} — prefixo do nome dos secrets (padrão: {@code quarkus.application.name});</li>
 *   <li>{@code harpia.vault.profile} — perfil do {@code ~/.oci/config} (padrão: {@code DEFAULT});</li>
 *   <li>{@code harpia.vault.config-file} — caminho do arquivo de config OCI (padrão: local padrão do SDK).</li>
 * </ul>
 */
public class OciVaultConfigSourceFactory implements ConfigSourceFactory {

    @Override
    public Iterable<ConfigSource> getConfigSources(ConfigSourceContext context) {
        boolean enabled = "true".equalsIgnoreCase(value(context, "harpia.vault.enabled"));
        if (!enabled) {
            enabled = context.getProfiles().contains("local");
        }
        if (!enabled) {
            return List.of();
        }

        String compartment = value(context, "harpia.vault.compartment-ocid");
        if (compartment == null || compartment.isBlank()) {
            System.err.println("[harpia-vault] desativado: 'harpia.vault.compartment-ocid' nao configurado");
            return List.of();
        }

        String prefix = value(context, "harpia.vault.secret-prefix");
        if (prefix == null || prefix.isBlank()) {
            prefix = value(context, "quarkus.application.name");
        }
        if (prefix == null) {
            prefix = "";
        }

        String profile = value(context, "harpia.vault.profile");
        if (profile == null || profile.isBlank()) {
            profile = "DEFAULT";
        }

        String configFile = value(context, "harpia.vault.config-file");

        return List.of(new OciVaultConfigSource(prefix, compartment, profile, configFile));
    }

    private static String value(ConfigSourceContext context, String name) {
        ConfigValue configValue = context.getValue(name);
        return configValue == null ? null : configValue.getValue();
    }
}
