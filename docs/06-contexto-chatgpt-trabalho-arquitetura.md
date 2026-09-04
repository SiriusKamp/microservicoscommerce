# Contexto do projeto Cloud Commerce para continuidade em outro chat

Data de atualização: 04/09/2026

Este arquivo reúne o estado real do projeto, as decisões já tomadas, os trabalhos acadêmicos relacionados e as próximas entregas. Ele deve ser enviado ao outro chat antes de solicitar novas alterações, para evitar que etapas concluídas sejam refeitas ou que requisitos ainda pendentes sejam descritos como implementados.

Nenhuma credencial real está registrada neste documento.

## 1. Identificação do projeto

| Item | Informação |
| --- | --- |
| Nome do código | Cloud Commerce |
| Nome usado no trabalho atual | Vix Commerce |
| Diretório local | `C:\Users\sirius.alves\Projetos\microservicoscommerce` |
| Objetivo didático | Estudar microsserviços, EDA, RabbitMQ, Docker, AWS, ECR, EC2, Kubernetes e gerenciamento de APIs no mesmo projeto |
| Linguagem | Java 21 |
| Framework | Spring Boot 4.1.0 |
| Build | Maven Wrapper |
| Persistência | PostgreSQL no Supabase |

O código continua com o nome Cloud Commerce. A documentação do trabalho atual usa Vix Commerce como cenário acadêmico. Não é necessário renomear pacotes, imagens ou serviços apenas por causa dessa adaptação.

## 2. Trabalhos acadêmicos envolvidos

Existem três frentes diferentes no mesmo repositório.

| Trabalho | Estado | Resultado principal |
| --- | --- | --- |
| Sistemas Baseados em Eventos e Mensageria Distribuída | Concluído | Fluxo pedido, estoque e auditoria com RabbitMQ, DirectExchange e TopicExchange |
| AWS e Kubernetes | Executado e documentado | Imagem publicada no ECR, aplicação testada em EKS com duas réplicas e LoadBalancer; recursos removidos depois das evidências para evitar custo |
| Arquitetura de Microsserviços e Gerenciamento de APIs | Em andamento | Bounded contexts, context map e dois ADRs concluídos; contratos e políticas de resiliência ainda precisam ser finalizados |

Não tratar o trabalho atual como TCC ou artigo científico. O texto deve ser direto, com tabelas, diagramas simples, decisões técnicas e evidências.

## 3. Estrutura do sistema

| Serviço | Porta | Responsabilidade |
| --- | ---: | --- |
| `cloud-commerce` | 8081 | Front Spring Boot, Thymeleaf, Bootstrap e JavaScript |
| `produto` | 8082 | Cadastro e consulta do catálogo de produtos |
| `estoque` | 8083 | Consulta de saldo, validação e baixa de estoque |
| `pedido` | 8084 | Criação, consulta e atualização do status dos pedidos |
| `rabbitmq` | 5672 e 15672 | Broker de mensagens e painel administrativo |

Principais pastas:

```text
microservicoscommerce/
  cloud-commerce/
  produto/
  estoque/
  pedido/
  k8s/
  docs/
  docker-compose.yml
  README.md
  README-AWS-KUBERNETES.md
```

## 4. Tecnologias já aplicadas

| Tecnologia | Uso no projeto |
| --- | --- |
| Spring Web | APIs REST e controllers do front |
| Spring Data JPA | Persistência das entidades |
| PostgreSQL e Supabase | Banco já existente antes da publicação dos trabalhos |
| Spring AMQP | Integração Java com RabbitMQ |
| RabbitMQ | Comunicação assíncrona entre pedido e estoque |
| Thymeleaf | Templates HTML do front |
| Bootstrap | Componentes e estilização das telas |
| JavaScript | Fetch das APIs, carrinho em localStorage e feedback visual |
| Docker | Imagens dos quatro serviços e container local do RabbitMQ |
| Docker Compose | Inicialização reproduzível do RabbitMQ |
| Amazon ECR | Registro das imagens Docker durante o trabalho AWS |
| Amazon EKS | Execução do front em Kubernetes durante o trabalho AWS |
| Kubernetes | Deployment, Pods, Service, ConfigMap, Secret e probes |

## 5. Fluxo funcional implementado

O fluxo principal começa no front e termina com o status do pedido atualizado.

```text
1. O front consulta produto por HTTP.
2. O front consulta estoque por HTTP.
3. O carrinho é mantido no localStorage de cada navegador ou sessão.
4. O front envia POST /pedidos com a lista de itens.
5. Pedido salva o registro com status PROCESSANDO.
6. Pedido publica pedido.solicitado no RabbitMQ.
7. Estoque consome a mensagem e valida todos os itens.
8. Se houver saldo para todos, estoque reduz as quantidades em uma transação.
9. Estoque publica estoque.resposta.
10. Pedido consome a resposta e atualiza o status para PROCESSADO ou SEM_ESTOQUE.
11. Se o pedido permanecer PROCESSANDO por mais de 30 segundos, pedido publica um evento de auditoria.
```

O carrinho foi mantido no `localStorage` para permitir testar duas sessões tentando comprar o mesmo item. A decisão final de disponibilidade pertence ao serviço de estoque, no momento em que ele processa o evento.

## 6. Comunicação síncrona

| Origem | Destino | Contrato |
| --- | --- | --- |
| `cloud-commerce` | `produto` | `GET /produtos` e `GET /produtos/{id}` |
| `cloud-commerce` | `estoque` | `GET /estoque` e `GET /estoque/{idProduto}` |
| `cloud-commerce` | `pedido` | `POST /pedidos`, `GET /pedidos` e `GET /pedidos/{id}` |

Comportamentos do front:

- Se o catálogo falhar, a tela informa que os produtos não puderam ser carregados.
- Se apenas o estoque falhar, os produtos continuam visíveis com quantidade zero.
- Se a criação do pedido falhar, um toast informa que o pedido não foi enviado.
- A tela de pedidos consulta o serviço real; não usa mais mock.

## 7. Comunicação assíncrona

### 7.1 Topologia RabbitMQ

| Elemento | Nome | Uso |
| --- | --- | --- |
| DirectExchange | `commerce.pedidos.exchange` | Fluxo principal entre pedido e estoque |
| Queue | `commerce.pedido.solicitado.queue` | Armazena pedidos aguardando o estoque |
| Queue | `commerce.estoque.resposta.queue` | Armazena respostas aguardando o serviço pedido |
| Routing key | `pedido.solicitado` | Roteia solicitação para a fila consumida pelo estoque |
| Routing key | `estoque.resposta` | Roteia resposta para a fila consumida pelo pedido |
| TopicExchange | `commerce.auditoria.topic.exchange` | Eventos de auditoria com correspondência por padrão |
| Queue | `commerce.auditoria.pedido.queue` | Recebe eventos de auditoria de pedido |
| Routing key | `pedido.sem-resposta-estoque` | Identifica pedido sem resposta após o limite |
| Binding topic | `pedido.#` | Aceita eventos de auditoria cujo nome começa com `pedido.` |

Os objetos `Exchange`, `Queue`, `Binding` e `MessageConverter` são declarados como beans nas classes `RabbitMQConfig`. O Spring AMQP usa esses beans para declarar a topologia no broker quando a aplicação se conecta.

### 7.2 Eventos

`pedido.solicitado`

```json
{
  "pedidoId": 10,
  "valorTotal": 99.90,
  "criadoEm": "data e hora",
  "itens": [
    {
      "produtoId": 1,
      "quantidade": 1
    }
  ]
}
```

`estoque.resposta`

```json
{
  "pedidoId": 10,
  "sucesso": true,
  "status": "PROCESSADO",
  "mensagem": "Estoque baixado com sucesso.",
  "respondidoEm": "data e hora"
}
```

`pedido.sem-resposta-estoque`

```json
{
  "pedidoId": 10,
  "statusAtual": "PROCESSANDO",
  "motivo": "Pedido criado, mas ainda sem resposta do estoque.",
  "criadoEm": "data e hora",
  "auditadoEm": "data e hora"
}
```

## 8. Bounded contexts e context map

Foram identificados quatro contextos de negócio.

| Bounded context | Autoridade de dados e regras |
| --- | --- |
| Catálogo de Produtos | Nome, descrição, preço e dados de apresentação do produto |
| Estoque | Quantidade disponível e regra que impede saldo negativo |
| Pedidos | Pedido, itens, valor total e status do processamento |
| Experiência Web | Navegação, carrinho local e composição das informações para o usuário |

Context map atual:

```text
Experiência Web -> Catálogo de Produtos por HTTP
Experiência Web -> Estoque por HTTP
Experiência Web -> Pedidos por HTTP
Pedidos -> Estoque por evento pedido.solicitado
Estoque -> Pedidos por evento estoque.resposta
Pedidos -> Auditoria por evento pedido.sem-resposta-estoque
```

RabbitMQ, Supabase, Docker, AWS e Kubernetes são infraestrutura. Eles não são bounded contexts de negócio.

## 9. Decisões arquiteturais registradas

### ADR 001 Separação dos contextos e serviços

Produto, estoque, pedido e experiência web foram separados porque possuem regras, dados e motivos de mudança diferentes.

O desenho correto para microsserviços seria um banco independente por serviço. No MVP, as tabelas estão no mesmo Supabase/PostgreSQL porque o ambiente utilizado não permite manter várias databases ativas ao mesmo tempo. A separação lógica continua válida: cada serviço deve acessar somente as tabelas sob sua responsabilidade.

### ADR 002 Comunicação assíncrona entre pedido e estoque

Pedido não chama diretamente a regra interna de estoque. Ele salva o pedido como PROCESSANDO, publica um evento e aguarda uma resposta. Essa decisão desacopla os serviços e permite observar atraso, indisponibilidade e resposta perdida.

## 10. Persistência e variáveis de ambiente

Os serviços `produto`, `estoque` e `pedido` usam:

```text
DB_URL
DB_USERNAME
DB_PASSWORD
RABBITMQ_HOST
RABBITMQ_PORT
RABBITMQ_USERNAME
RABBITMQ_PASSWORD
```

Cada serviço importa opcionalmente seu arquivo `.env` por:

```properties
spring.config.import=optional:file:.env[.properties]
```

Os arquivos `.env` reais não devem entrar no Git. Existem modelos `.env.example`, e as credenciais devem ser fornecidas separadamente para execução local.

Não substituir Supabase por H2 ou SQLite na documentação. O projeto já havia sido desenvolvido com Supabase antes da publicação do trabalho, e essa escolha deve aparecer como decisão e limitação do MVP.

## 11. Execução local

Pré-requisitos:

```text
Java 21
Docker Desktop
Git
```

Na raiz do projeto, o RabbitMQ pode ser iniciado com:

```powershell
Set-Location "C:\CAMINHO\PARA\microservicoscommerce"
docker compose up -d rabbitmq
```

Depois devem ser iniciados, em terminais separados, `produto`, `estoque`, `pedido` e `cloud-commerce` com:

```powershell
.\mvnw.cmd spring-boot:run
```

O front fica disponível em `http://localhost:8081` e o painel do RabbitMQ em `http://localhost:15672`.

## 12. Histórico AWS e Kubernetes

O trabalho AWS já passou pelas seguintes etapas:

```text
AWS CLI e eksctl instalados
usuário IAM e permissões configurados
repositórios ECR criados
imagens Docker construídas, marcadas e publicadas
imagem do front validada em EC2
cluster EKS cloud-commerce-eks criado na região sa-east-1
dois nodes Kubernetes validados
ConfigMap, Secret, Deployment e Service aplicados
Deployment com duas réplicas validado
LoadBalancer externo validado com /health, /config e tela inicial
um Pod foi excluído e recriado automaticamente
evidências foram coletadas
cluster, nodes, LoadBalancer, EC2, ECR e demais recursos foram removidos para evitar custos
```

Os manifestos continuam no repositório:

| Arquivo | Função |
| --- | --- |
| `k8s/configmap.yaml` | Define `AMBIENTE=demonstracao` |
| `k8s/secret.yaml` | Define uma `API_KEY` fictícia |
| `k8s/deployment.yaml` | Usa a imagem do ECR, duas réplicas e probes em `/health` |
| `k8s/service.yaml` | Expõe o front por um Service `LoadBalancer`, porta 80 para 8081 |

Estado atual da AWS: os recursos que geravam custo foram excluídos. As imagens e telas existentes na documentação são evidências históricas do teste, não recursos atualmente ativos.

Os arquivos `docs/02-aws-ecr-ec2-deploy.md`, `docs/03-status-atual-projeto-aws-kubernetes.md` e `docs/04-briefing-chat-trabalho-aws-kubernetes.md` foram escritos durante etapas anteriores e ainda podem conter itens marcados como pendentes. Para o resultado final do trabalho AWS, usar `docs/trabalho-aws-abnt.docx` e as evidências registradas nele.

## 13. Requisitos do trabalho atual

### Laboratório 1

- Identificar bounded contexts e limites.
- Criar context map.
- Responder às perguntas obrigatórias sobre termos, autoridade de dados e mudanças.
- Registrar decisões arquiteturais em ADRs.

Estado: concluído no documento `docs/05-documentacao-contexts.docx`.

### Laboratório 2

- Criar matriz de comunicação síncrona e assíncrona.
- Documentar APIs REST com OpenAPI.
- Documentar eventos com AsyncAPI.

Estado: a matriz e os recortes de contrato foram adicionados ao documento. Ainda faltam os arquivos completos e validados `openapi.yaml` e `asyncapi.yaml`.

### Laboratório 3

- Definir política de resiliência.
- Executar cenário de falha, degradação ou indisponibilidade.
- Registrar evidências e explicar o comportamento observado.

Estado: pendente.

### Entrega integrada

A entrega final deve reunir:

- Arquitetura e um recorte vertical funcional.
- Pelo menos duas ou três capacidades com autoridade de dados definida.
- Comunicação síncrona e assíncrona.
- OpenAPI e AsyncAPI válidos ou trechos completos exigidos pelo professor.
- Uma operação idempotente e tratamento de resposta perdida.
- Um cenário reproduzível de degradação ou indisponibilidade.
- Posicionamento de API Gateway, BFF ou service mesh, mesmo que algum deles não seja implementado.
- Pelo menos dois ADRs.
- README reproduzível para executar e testar o projeto.

## 14. Estado real dos requisitos mais delicados

| Requisito | Estado real | Observação |
| --- | --- | --- |
| Correlação de mensagens | Implementado | `pedidoId` liga a solicitação à resposta do estoque |
| Detecção de resposta perdida | Parcial | Após 30 segundos há evento de auditoria e log |
| Recuperação de resposta perdida | Pendente | A auditoria não republica o evento nem conclui o pedido |
| Idempotência do `POST /pedidos` | Pendente | Repetir a requisição cria outro pedido |
| Consumo idempotente | Pendente | Ainda não existe registro persistente de `eventId` processado |
| Retry e DLQ | Pendente | Ainda não foram definidos |
| OpenAPI completo | Pendente | Existe apenas documentação em tabelas e recorte no DOCX |
| AsyncAPI completo | Pendente | Existe apenas documentação dos eventos e recorte no DOCX |
| Cenário de falha documentado | Pendente | Pode usar estoque parado, RabbitMQ indisponível ou resposta perdida |
| Gateway, BFF e service mesh | Pendente na decisão | O front já exerce parte do papel de composição, mas a posição arquitetural precisa ser explicada |

## 15. Documentos do repositório

| Arquivo | Finalidade |
| --- | --- |
| `README.md` | Guia curto de execução local |
| `ENSINAR_RABBITMQ.md` | Explicação didática detalhada da mensageria |
| `docs/01-documentacao-arquitetural.docx` | Documento do trabalho de RabbitMQ e EDA |
| `README-AWS-KUBERNETES.md` | Guia de comandos e conceitos de AWS e Kubernetes |
| `docs/trabalho-aws-abnt.docx` | Relatório final do trabalho AWS com evidências |
| `docs/05-documentacao-contexts.docx` | Documento principal do trabalho atual de arquitetura |
| `docs/06-contexto-chatgpt-trabalho-arquitetura.md` | Contexto de continuidade para outro chat |

## 16. Próximos passos recomendados

Executar nesta ordem:

1. Validar a nova seção de contratos no `05-documentacao-contexts.docx`.
2. Criar `docs/contracts/openapi.yaml` com os endpoints dos serviços.
3. Criar `docs/contracts/asyncapi.yaml` com os três eventos RabbitMQ.
4. Validar os dois contratos com ferramentas adequadas.
5. Escolher e implementar a estratégia mínima de idempotência.
6. Definir retry, reconciliação e fila de mensagens não processadas.
7. Executar um cenário de falha e coletar logs, estados e tempos observados.
8. Documentar o posicionamento de API Gateway, BFF e service mesh.
9. Atualizar o README com o roteiro reproduzível da entrega integrada.
10. Fazer a avaliação final requisito por requisito.

## 17. Orientações para o outro chat

- Ensinar os conceitos enquanto orienta a implementação; o aluno entende melhor o fluxo do que a sintaxe.
- Trabalhar passo a passo, entregando uma etapa por vez para validação.
- Ler o código antes de propor mudanças.
- Não apagar alterações já existentes do aluno.
- Não afirmar que idempotência, retry, DLQ, OpenAPI completo ou AsyncAPI completo já estão implementados.
- Manter Supabase/PostgreSQL e explicar a limitação de um banco físico compartilhado no MVP.
- Usar português com acentos e cedilha em toda documentação.
- Preferir textos sucintos, tabelas e exemplos concretos.
- Não transformar o relatório em TCC ou artigo científico.
- Separar claramente o que foi implementado, o que foi apenas documentado e o que continua pendente.
- Não incluir chaves, senhas, tokens ou valores reais de `.env` em documentos ou commits.

## 18. Resultado esperado da continuidade

Ao final, o repositório deve permitir que o professor identifique os limites de negócio, siga o fluxo HTTP e RabbitMQ, consulte contratos OpenAPI e AsyncAPI, reproduza pelo menos um cenário de falha e entenda as decisões sobre dados, idempotência, resiliência e entrada das APIs.
