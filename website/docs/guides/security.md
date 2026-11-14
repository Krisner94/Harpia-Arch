# Segurança

O Harpia Arch não implementa autenticação, autorização ou criptografia. Ele fornece uma estrutura
para manter o código previsível e validar a arquitetura cedo. A segurança efetiva continua sendo
responsabilidade da aplicação e do Quarkus.

## Postura do framework

- **sem descoberta dinâmica** de Activities;
- **sem reflexão no caminho crítico** do motor;
- **sem conversão implícita** de contexto;
- **validação de camadas** em build time;
- **contrato OpenAPI** como fonte das rotas;
- **dependências com versões gerenciadas** pelo Quarkus BOM.

O ArC pode usar proxies CDI para beans `@ApplicationScoped`. Isso não é usado pelo motor para
descobrir ou executar etapas.

## Autenticação e autorização

- autentique na borda (Quarkus OIDC, mTLS ou outro mecanismo);
- autorize no Controller ou em um serviço de política;
- não confie em campos do corpo para decidir permissão;
- propague identidade/tenant pelo contexto tipado apenas quando necessário;
- não coloque segredos no contexto compartilhado.

## Validação de entrada

- valide o DTO gerado pelo OpenAPI com Bean Validation;
- valide regras de negócio na Activity;
- rejeite entrada inválida antes de persistir;
- não use exceções de validação como controle de fluxo normal.

## Dados sensíveis

Campos como senha de serviço externo, tokens e credenciais exigem tratamento específico.

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

### O que evitar

- retornar o segredo em `GET /clientes`;
- `?incluirSenha=true` no mesmo endpoint;
- colocar o segredo em logs, traces, cache ou histórico de navegação;
- armazenar em texto puro.

### Se for necessário revelar

Crie um endpoint separado, com autorização específica:

```http
GET /clientes/{id}/senha-gov
Authorization: Bearer <token com permissão clientes:segredo:ler>
Cache-Control: no-store
```

Requisitos:

- permissão dedicada;
- auditoria de quem, quando, qual cliente e IP;
- reautenticação/MFA;
- resposta sem cache e sem log do corpo;
- rate limiting;
- nunca incluir em listagens.

### Armazenamento

Se a revelação for necessária, use criptografia reversível:

- AES-GCM;
- chave fora do banco (env, Vault ou secret manager);
- IV aleatório por registro;
- colunas `ciphertext` e `iv`.

Se a revelação não for necessária, use hash forte (BCrypt ou Argon2) e ofereça:

- `PUT /clientes/{id}/senha-gov` para atualizar;
- `POST /clientes/{id}/senha-gov/verificar` para validar um valor informado.

## Segredos de infraestrutura

- tokens do Maven Central ficam em `~/.m2/settings.xml` com permissão `600`;
- senhas de banco e chaves ficam em variáveis de ambiente ou secret manager;
- nunca commite `.env`, `settings.xml` ou chaves privadas;
- use o environment `production` com revisores no GitHub Actions;
- assine artefatos com GPG no release.

## Logs e erros

- não registre corpo de requisição com dados sensíveis;
- não registre tokens ou cabeçalhos de autorização;
- propague a exceção original sem incluir segredos na mensagem;
- use correlation ID, não dados pessoais, para rastrear execuções.

## Dependências

- mantenha o Quarkus BOM e as dependências atualizados;
- revise dependências transitivas;
- valide o build nativo quando adicionar bibliotecas que usam reflexão;
- não publique snapshots em produção.

## Checklist

- [ ] autenticação e autorização na borda;
- [ ] DTOs de resposta não expõem segredos;
- [ ] validação de entrada no contrato e na Activity;
- [ ] logs sem dados sensíveis;
- [ ] segredos fora do repositório;
- [ ] release assinado e publicado no Maven Central;
- [ ] Native Image testado quando aplicável.
