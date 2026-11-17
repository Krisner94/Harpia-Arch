package io.github.krisner94.harpia.vault;

import io.smallrye.config.ConfigSourceContext;
import io.smallrye.config.ConfigSourceFactory;
import io.smallrye.config.ConfigValue;
import org.eclipse.microprofile.config.spi.ConfigSource;

import java.util.List;
import java.util.ServiceLoader;

/**
 * Registra o {@link VaultConfigSource} quando o Vault está habilitado, escolhendo o
 * {@link SecretResolver} pelo provedor configurado em {@code harpia.vault.provider}.
 *
 * <p>Ativação (qualquer uma):</p>
 * <ul>
 *   <li>{@code harpia.vault.enabled=true};</li>
 *   <li>perfil {@code local} ativo (conveniência para desenvolvimento).</li>
 * </ul>
 *
 * <p>Configuração genérica (pode viver no {@code bootstrap.yaml}):</p>
 * <ul>
 *   <li>{@code harpia.vault.provider} — provedor (padrão: {@code oci});</li>
 *   <li>{@code harpia.vault.secret-prefix} — prefixo do nome dos secrets (padrão: {@code quarkus.application.name});</li>
 * </ul>
 *
 * <p>A configuração específica de cada provedor (ex.: {@code harpia.vault.oci.*}) é lida pela
 * própria implementação do {@link SecretResolver}.</p>
 */
public class VaultConfigSourceFactory implements ConfigSourceFactory {

    @Override
    public Iterable<ConfigSource> getConfigSources(ConfigSourceContext context) {
        boolean enabled = "true".equalsIgnoreCase(value(context, "harpia.vault.enabled"));
        if (!enabled) {
            enabled = context.getProfiles().contains("local");
        }
        if (!enabled) {
            return List.of();
        }

        String provider = value(context, "harpia.vault.provider");
        if (provider == null || provider.isBlank()) {
            provider = "oci";
        }

        SecretResolver resolver = loadResolver(provider);
        if (resolver == null) {
            System.err.println("[harpia-vault] provedor '" + provider + "' nao encontrado no classpath");
            return List.of();
        }
        resolver.configure(context);

        String prefix = value(context, "harpia.vault.secret-prefix");
        if (prefix == null || prefix.isBlank()) {
            prefix = value(context, "quarkus.application.name");
        }
        if (prefix == null) {
            prefix = "";
        }

        return List.of(new VaultConfigSource(prefix, resolver));
    }

    private static SecretResolver loadResolver(String provider) {
        for (SecretResolver resolver : ServiceLoader.load(SecretResolver.class)) {
            if (resolver.provider().equalsIgnoreCase(provider)) {
                return resolver;
            }
        }
        return null;
    }

    private static String value(ConfigSourceContext context, String name) {
        ConfigValue configValue = context.getValue(name);
        return configValue == null ? null : configValue.getValue();
    }
}
