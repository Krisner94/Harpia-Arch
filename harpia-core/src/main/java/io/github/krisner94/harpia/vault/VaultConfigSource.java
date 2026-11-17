package io.github.krisner94.harpia.vault;

import org.eclipse.microprofile.config.spi.ConfigSource;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * {@link ConfigSource} genérico que resolve chaves no formato {@code <AMBIENTE>_DB_<CAMPO>}
 * (ex.: {@code HML_DB_URL}) delegando a busca do valor a um {@link SecretResolver}.
 *
 * <p>O nome do secret é derivado da chave: {@code <prefixo>-<ambiente>-db-<campo>}. Assim os
 * arquivos {@code application*.yaml} podem usar {@code ${HML_DB_URL}} sem expor credenciais —
 * o valor vem do provedor configurado (OCI, OpenBao, AWS, ...).</p>
 */
public class VaultConfigSource implements ConfigSource {

    private static final String NAME = "harpia-vault";
    private static final int ORDINAL = 110; // abaixo de sysprops(400), env(300) e application.yaml(~250)

    private static final Pattern KEY_PATTERN = Pattern.compile("^([A-Z0-9]+)_DB_([A-Z0-9]+)$");

    private final String prefix;
    private final SecretResolver resolver;
    private final ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();

    public VaultConfigSource(String prefix, SecretResolver resolver) {
        this.prefix = prefix;
        this.resolver = resolver;
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
        return cache.computeIfAbsent(propertyName, key -> resolver.resolve(secretName));
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public int getOrdinal() {
        return ORDINAL;
    }
}
