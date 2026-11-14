# Security

Harpia Arch does not implement authentication, authorization or cryptography. It provides a
structure that keeps the code predictable and validates the architecture early. Effective security
remains the responsibility of the application and Quarkus.

## Framework stance

- **no dynamic discovery** of Activities;
- **no reflection on the critical path** of the engine;
- **no implicit conversion** of context;
- **layer validation** at build time;
- **OpenAPI contract** as the source of the routes;
- **dependencies with versions managed** by the Quarkus BOM.

ArC may use CDI proxies for `@ApplicationScoped` beans. This is not used by the engine to discover or
execute steps.

## Authentication and authorization

- authenticate at the edge (Quarkus OIDC, mTLS or another mechanism);
- authorize in the Controller or in a policy service;
- do not rely on body fields to decide permissions;
- propagate identity/tenant through the typed context only when necessary;
- do not put secrets in the shared context.

## Input validation

- validate the DTO generated from OpenAPI with Bean Validation;
- validate business rules in the Activity;
- reject invalid input before persisting;
- do not use validation exceptions as normal flow control.

## Sensitive data

Fields such as external service passwords, tokens and credentials require specific handling.

**Do not return secrets in the common DTO.** Even if the contract allows it, the default endpoint
must remain write-only.

```yaml
govPassword:
  type: string
  format: password
  writeOnly: true
```

In the response DTO, expose only the existence:

```json
{
  "id": 1,
  "name": "Ana",
  "govPasswordConfigured": true
}
```

### What to avoid

- returning the secret in `GET /customers`;
- `?includePassword=true` on the same endpoint;
- writing the secret to logs, traces, caches or browser history;
- storing it in plain text.

### If revealing is required

Create a separate endpoint with specific authorization:

```http
GET /customers/{id}/senha-gov
Authorization: Bearer <token with customers:secret:read permission>
Cache-Control: no-store
```

Requirements:

- a dedicated permission;
- audit of who, when, which customer and IP;
- re-authentication/MFA;
- response with no cache and no body logging;
- rate limiting;
- never include it in listings.

### Storage

If revealing is required, use reversible encryption:

- AES-GCM;
- key outside the database (env, Vault or secret manager);
- random IV per record;
- `ciphertext` and `iv` columns.

If revealing is not required, use a strong hash (BCrypt or Argon2) and provide:

- `PUT /customers/{id}/gov-password` to update it;
- `POST /customers/{id}/gov-password/verify` to validate a provided value.

## Infrastructure secrets

- Maven Central tokens live in `~/.m2/settings.xml` with `600` permissions;
- database passwords and keys live in environment variables or a secret manager;
- never commit `.env`, `settings.xml` or private keys;
- use the `production` environment with reviewers in GitHub Actions;
- sign artifacts with GPG on release.

## Logs and errors

- do not log request bodies with sensitive data;
- do not log tokens or authorization headers;
- propagate the original exception without including secrets in the message;
- use a correlation ID, not personal data, to trace executions.

## Dependencies

- keep the Quarkus BOM and dependencies up to date;
- review transitive dependencies;
- validate the native build when adding libraries that use reflection;
- do not publish snapshots to production.

## Checklist

- [ ] authentication and authorization at the edge;
- [ ] response DTOs do not expose secrets;
- [ ] input validation in the contract and in the Activity;
- [ ] logs without sensitive data;
- [ ] secrets outside the repository;
- [ ] release signed and published to Maven Central;
- [ ] Native Image tested when applicable.
