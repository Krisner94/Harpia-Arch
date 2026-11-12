# Harpia Arch

Base Maven para aplicações Quarkus com **OpenAPI contract-first** e o núcleo de workflows
[`harpia-core`](harpia-core/README.md).

O repositório é um agregador com seis módulos:

| Módulo | Papel |
|---|---|
| `harpia-arch` | Pai de build: Quarkus BOM, versões, `openapi-generator-maven-plugin` e o perfil de codegen. |
| `harpia-quarkus` | Pai das aplicações Quarkus: dependências comuns, plugins e perfil `native`. |
| `harpia-core` | Núcleo: `FlowItem`, `Workflow`, `FlowBuilder`, exceptions e annotations CDI. |
| `harpia-core-deployment` | Validação da arquitetura em build time (Jandex + `@BuildStep`). |
| `harpia-integration-tests` | Aplicação Quarkus de teste: Hibernate ORM, H2, REST e Native Image. |
| `harpia-benchmarks` | Benchmarks JMH do motor de workflows. |

## Uso

A aplicação usa **`harpia-quarkus`** como pai. O POM fica só com a identidade do serviço:

```xml
<parent>
    <groupId>io.github.krisner94</groupId>
    <artifactId>harpia-quarkus</artifactId>
    <version>1.0.0</version>
</parent>

<groupId>com.acme</groupId>
<artifactId>meu-servico</artifactId>
<version>1.0.0</version>
<packaging>quarkus</packaging>
```

Nada de BOM, versões de plugin, dependências de REST, JUnit ou Rest Assured no POM da aplicação.

O `harpia-quarkus` já fornece:

- Quarkus BOM e a versão do Quarkus;
- `quarkus-arc`, `quarkus-rest` e `quarkus-rest-jackson`;
- `microprofile-openapi-api` e `jakarta.validation-api`, usados pelo código gerado;
- `harpia-core` (engine de workflows + annotations);
- `quarkus-junit` e Rest Assured em escopo de teste;
- `quarkus-maven-plugin` com `extensions=true`;
- `maven-compiler-plugin` (Java 25, `-parameters`) e `maven-surefire-plugin`;
- perfil `native` com `maven-failsafe-plugin`, `quarkus.native.enabled` e `skipITs=false`;
- o perfil `harpia-openapi-codegen`, ativado quando existe `src/main/resources/openapi`.

A aplicação só declara o que é específico dela: dependências extras, propriedades do contrato e
configuração de runtime.

> `packaging=quarkus` é obrigatório: é o lifecycle do Quarkus que executa `generate-code` e
> `build`. Não declare o `quarkus-maven-plugin` nem os plugins de compilação/teste no POM da
> aplicação.

## Annotations

Todas as annotations ficam em `io.github.krisner94.harpia.annotation` e já vêm pelo
`harpia-quarkus`. Detalhes em [`harpia-core/README.md`](harpia-core/README.md).

| Annotation | Papel | CDI |
|---|---|---|
| `@HarpiaApplication` | Classe principal da aplicação. | Marker |
| `@Controller` | Adapter de entrada HTTP. | `@ApplicationScoped` |
| `@Service` | Regras de negócio. | `@ApplicationScoped` |
| `@Repository` | Persistência. | `@ApplicationScoped` |
| `@Activity` | Etapa de um workflow. | `@ApplicationScoped` |
| `@WorkflowDefinition` | Workflow que declara as Activities. | `@ApplicationScoped` |
| `@Component` | Componente genérico. | `@ApplicationScoped` |

```java
import io.github.krisner94.harpia.annotation.Controller;
import io.github.krisner94.harpia.annotation.Service;
import io.github.krisner94.harpia.annotation.Activity;
import io.github.krisner94.harpia.annotation.WorkflowDefinition;
```

A annotation de workflow é `@WorkflowDefinition` porque Java não permite uma annotation e uma
classe com o mesmo nome simples (`Workflow<I, C, O>`) no mesmo escopo. Ela representa o
`@Workflow` previsto nas diretrizes.

## Validação em build

`harpia-core` é uma extensão Quarkus: o `harpia-core-deployment` roda durante o build e falha a
aplicação antes de gerar o artefato quando uma regra é violada.

| Regra | Erro |
|---|---|
| `@Activity` implementa `FlowItem` | `@Activity ... must implement FlowItem` |
| `@WorkflowDefinition` estende `Workflow` | `@WorkflowDefinition ... must extend Workflow` |
| `@Controller` não implementa `FlowItem` | `@Controller ... must not implement FlowItem` |
| `@Controller` não declara `@Path`/verbos | `@Controller ... must not declare @Path` |
| `@Activity` não declara `@Path`/verbos | `@Activity ... must not declare GET` |
| `@Repository` não depende de `@Controller` | `@Repository ... must not depend on @Controller` |
| Uma única `@HarpiaApplication` | `Only one @HarpiaApplication is allowed` |

Exemplo de falha:

```text
[error]: Build step io.github.krisner94.harpia.deployment.HarpiaArchProcessor#validateArchitecture
threw an exception: java.lang.IllegalStateException: @Activity apurair.workflow.InvalidActivity
must implement io.github.krisner94.harpia.core.FlowItem
```

Nenhuma configuração é necessária: o Quarkus resolve o artefato de deployment a partir do
`harpia-core`.

## Contrato

Por padrão, o contrato deve estar em:

```text
src/main/resources/openapi/openapi.yaml
```

```yaml
openapi: 3.0.3
info:
  title: Pets API
  version: "1.0"
paths:
  /pets/{petId}:
    get:
      operationId: getPetById
      parameters:
        - name: petId
          in: path
          required: true
          schema:
            type: string
      responses:
        "200":
          description: Pet encontrado
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/Pet'
components:
  schemas:
    Pet:
      type: object
      properties:
        id:
          type: string
        name:
          type: string
```

Com esse contrato, o gerador cria:

```text
target/generated-sources/openapi/src/main/java/com/acme/api/PetsApi.java
target/generated-sources/openapi/src/main/java/com/acme/api/beans/*.java
```

O nome da interface é o primeiro segmento do path + o sufixo configurado. O `Resource` do
gerador anterior não é mais usado.

### Propriedades

| Propriedade | Default | Descrição |
|---|---|---|
| `harpia.openapi.spec` | `${project.basedir}/src/main/resources/openapi/openapi.yaml` | Arquivo OpenAPI de entrada |
| `harpia.openapi.api-package` | `${project.groupId}.api` | Pacote das interfaces geradas |
| `harpia.openapi.model-package` | `${harpia.openapi.api-package}.beans` | Pacote dos DTOs gerados |
| `harpia.openapi.api-suffix` | `Api` | Sufixo do nome das interfaces geradas |

Exemplo para trocar o sufixo e o arquivo:

```xml
<properties>
    <harpia.openapi.spec>${project.basedir}/src/main/resources/openapi/pets.yaml</harpia.openapi.spec>
    <harpia.openapi.api-suffix>Contract</harpia.openapi.api-suffix>
</properties>
```

O perfil `harpia-openapi-codegen` só é ativado quando existe o diretório
`src/main/resources/openapi`, para que o POM pai e projetos sem contrato não executem o gerador.

## Implementação do contrato

O gerador produz **interfaces**, não controllers. Basta implementar a interface como um bean CDI:

```java
@ApplicationScoped
public class PetsController implements PetsApi {

    @Override
    public Pet getPetById(String petId) {
        Pet pet = new Pet();
        pet.setId(petId);
        pet.setName("Rex");
        return pet;
    }
}
```

A rota, os verbos e os parâmetros vêm exclusivamente da interface gerada. Não é necessário
repetir `@Path` no controller.

## Build

```bash
mvn install
```

## IDE

O código gerado fica em:

```text
target/generated-sources/openapi/src/main/java
```

Esse diretório é adicionado como source root pelo `build-helper-maven-plugin`. Depois de rodar
`mvn generate-sources`, recarregue o projeto Maven na IDE. Se a IDE ainda não indexar a pasta,
marque-a como **Generated Sources Root** (ou **Sources Root**).

Este POM é apenas configuração. Para um projeto que já usa outro pai Maven, copie as propriedades,
o plugin `openapi-generator-maven-plugin` e o perfil `harpia-openapi-codegen` para o POM da
aplicação.

## Núcleo de workflows

O engine fica em `harpia-core` e já vem como dependência do `harpia-quarkus`.

```java
@WorkflowDefinition
public class CreateCustomerWorkflow extends Workflow<CustomerInput, CustomerContext, Customer> {

    private final ValidationWorkflow validation;
    private final PersistenceWorkflow persistence;

    public CreateCustomerWorkflow(ValidationWorkflow validation, PersistenceWorkflow persistence) {
        this.validation = validation;
        this.persistence = persistence;
    }

    @Override
    protected void configure() {
        flowBuilder = FlowBuilder.builder(CustomerInput.class, CustomerContext.class)
                .step(validation)
                .step(persistence)
                .build();
    }
}
```

- `Workflow` implementa `FlowItem`, então um workflow pode ser etapa de outro;
- a declaração usa exclusivamente `.step()` e termina com `.build()`;
- a composição é construída uma única vez, no `@PostConstruct`;
- tipos incompatíveis entre etapas não compilam;
- contexto diferente em um subworkflow exige `adaptContext(...)` explícito;
- não há reflexão, `Object` ou cast no encadeamento.

## Build e testes

```bash
mvn install
```

O comando instala todos os módulos e executa:

- testes unitários e de composição do `harpia-core`;
- validação arquitetural e teste CDI do `harpia-core-deployment`;
- teste de integração JVM com Hibernate ORM + H2 do `harpia-integration-tests`.

Comandos isolados:

```bash
mvn -pl harpia-core test
mvn -pl harpia-core-deployment test
mvn -pl harpia-integration-tests test
```

### Teste de integração `@QuarkusIntegrationTest`

```bash
mvn -pl harpia-integration-tests verify -Dit
```

Empacota a aplicação e executa o teste HTTP contra o artefato.

### Native Image

Requer GraalVM com `native-image`:

```bash
export GRAALVM_HOME=/caminho/para/graalvm
export JAVA_HOME=$GRAALVM_HOME
export PATH=$GRAALVM_HOME/bin:$PATH

mvn -pl harpia-integration-tests verify -Dnative
```

O build nativo compila a aplicação com Hibernate ORM + H2 e executa o
`CustomerWorkflowIT` contra o executável nativo.

### Benchmarks

```bash
mvn -pl harpia-benchmarks -am package
java -jar harpia-benchmarks/target/harpia-benchmarks.jar
```

Para inspecionar alocações:

```bash
java -jar harpia-benchmarks/target/harpia-benchmarks.jar -prof gc
```

Resultado de referência nesta máquina (3 warmups, 5 medições de 500 ms):

| Benchmark | Custo |
|---|---|
| Chamada direta (`IntUnaryOperator`) | 0.44 ns/op |
| `FlowItem` simples | 0.44 ns/op |
| Activity única composta | 1.79 ns/op |
| Workflow com 3 etapas | 5.00 ns/op |
| Workflow com 16 etapas | 58.08 ns/op |
| 3 subworkflows (9 etapas) | 56.65 ns/op |
| Construção de uma composição de 3 etapas | 9.69 ns/op, 80 B/op |

Os números são específicos do ambiente; use `-prof gc` e mais iterações para análises
comparativas.


## Maven Central

A biblioteca está preparada para distribuição no Maven Central com:

```text
io.github.krisner94:harpia-core:1.0.0
io.github.krisner94:harpia-core-deployment:1.0.0
io.github.krisner94:harpia-quarkus:1.0.0
io.github.krisner94:harpia-arch:1.0.0
```

O processo completo (namespace, GPG, credenciais e comando de deploy) está em
[`RELEASING.md`](RELEASING.md).
