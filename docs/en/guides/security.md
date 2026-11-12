# Sensitive data security

Fields such as external service passwords (`senhaGov`, tokens and credentials) need specific
handling.

## Main rule

**Do not return secrets in the common DTO.** Even if the contract allows it, the default endpoint
should remain write-only.

```yaml
senhaGov:
  type: string
  format: password
  writeOnly: true
```

In the response DTO, expose only the existence:

```json
{
  "id": 1,
  "nome": "Ana",
  "senhaGovConfigurada": true
}
```

## What to avoid

- returning the secret in `GET /customers`;
- `?includePassword=true` on the same endpoint;
- writing the secret to logs, traces, caches or browser history;
- storing it in plain text.

Query parameters and response bodies can be recorded by proxies, APMs and debugging tools.

## If revealing is required

Create a separate endpoint with specific authorization:

```http
GET /customers/{id}/senha-gov
Authorization: Bearer <token with customers:secret:read permission>
Cache-Control: no-store
```

Requirements:

- a dedicated permission (not just “authenticated user”);
- audit of who, when, which customer and IP;
- re-authentication/MFA for the action;
- response with no cache and no body logging;
- rate limiting;
- never include it in listings.

## Storage

### If revealing is required

Use reversible encryption:

- AES-GCM;
- key outside the database (env, Vault or secret manager);
- random IV per record;
- `ciphertext` and `iv` columns.

### If revealing is not required

Use a strong hash (BCrypt or Argon2) and provide only:

- `PUT /customers/{id}/senha-gov` to update;
- `POST /customers/{id}/senha-gov/verify` to validate a provided value.

## Infrastructure credentials

- Maven Central tokens live in `~/.m2/settings.xml` with `600` permissions;
- database passwords and keys live in environment variables or a secret manager;
- never commit `.env`, `settings.xml` or private keys.
