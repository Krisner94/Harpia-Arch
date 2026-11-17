package io.github.krisner94.harpia.vault;

import io.smallrye.config.ConfigSourceContext;

/**
 * Contrato de um provedor de segredos (OCI Vault, OpenBao/HashiCorp Vault, AWS Secrets Manager, ...).
 *
 * <p>Um provedor é descoberto via {@link java.util.ServiceLoader} e selecionado pela
 * configuração {@code harpia.vault.provider}. Para adicionar um novo provedor, basta criar
 * uma implementação desta interface (em qualquer módulo do classpath) e registrá-la em
 * {@code META-INF/services/io.github.krisner94.harpia.vault.SecretResolver}.</p>
 */
public interface SecretResolver {

    /** Identificador do provedor (ex.: {@code oci}, {@code openbao}, {@code aws}). */
    String provider();

    /** Lê a configuração específica do provedor a partir do contexto de config (ex.: bootstrap.yaml). */
    void configure(ConfigSourceContext context);

    /** Busca o valor do secret pelo nome (ou {@code null} se não existir/falhar). */
    String resolve(String secretName);
}
