# Segurança de dados sensíveis

Campos como senha de serviço externo (`senhaGov`, tokens e credenciais) exigem tratamento
específico.

## Regra principal

**Não retorne segredos no DTO comum.** Mesmo que o contrato permita, o endpoint padrão deve
continuar write-only.

```yaml
senhaGov:
  type: string
  format: password
  writeOnly: true
```

No DTO de resposta, exponha apenas a existência:

```json
{
  "id": 1,
  "nome": "Ana",
  "senhaGovConfigurada": true
}
```

## O que evitar

- retornar o segredo em `GET /clientes`;
- `?incluirSenha=true` no mesmo endpoint;
- colocar o segredo em logs, traces, cache ou histórico de navegação;
- armazenar em texto puro.

Parâmetros de consulta e corpos de resposta podem ser registrados por proxies, APMs e ferramentas
de debugging.

## Se for necessário revelar

Crie um endpoint separado, com autorização específica:

```http
GET /clientes/{id}/senha-gov
Authorization: Bearer <token com permissão clientes:segredo:ler>
Cache-Control: no-store
```

Requisitos:

- permissão dedicada (não apenas “usuário autenticado”);
- auditoria de quem, quando, qual cliente e IP;
- reautenticação/MFA para a ação;
- resposta sem cache e sem log do corpo;
- rate limiting;
- nunca incluir em listagens.

## Armazenamento

### Se a revelação for necessária

Use criptografia reversível:

- AES-GCM;
- chave fora do banco (env, Vault ou secret manager);
- IV aleatório por registro;
- colunas `ciphertext` e `iv`.

### Se a revelação não for necessária

Use hash forte (BCrypt ou Argon2) e ofereça apenas:

- `PUT /clientes/{id}/senha-gov` para atualizar;
- `POST /clientes/{id}/senha-gov/verificar` para validar um valor informado.

## Credenciais de infraestrutura

- tokens do Maven Central ficam em `~/.m2/settings.xml` com permissão `600`;
- senhas de banco e chaves ficam em variáveis de ambiente ou secret manager;
- nunca commite `.env`, `settings.xml` ou chaves privadas.
