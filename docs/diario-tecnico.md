# Diário técnico do projeto Microserviços Commerce

Este documento registra as mudanças feitas no projeto e deve ser incrementado a cada nova alteração relevante. A ideia é servir como memória técnica para acompanhamento, revisão arquitetural e orientação de próximos passos durante o estudo de Spring Boot, microserviços, Docker, RabbitMQ, gateway, Kubernetes/EC2 e AWS.

## Como atualizar este documento

Sempre que o projeto mudar, adicionar uma nova entrada no topo da seção "Histórico de mudanças" com:

- Data da alteração.
- Objetivo da mudança.
- Problema ou contexto.
- Arquivos alterados.
- O que foi feito.
- Como foi validado.
- Pendências e próximos passos.

## Visão atual do projeto

O projeto está organizado em quatro aplicações:

- `cloud-commerce`: front MVC/Thymeleaf, porta `8081`.
- `produto`: microserviço de produtos, porta `8082`.
- `estoque`: microserviço de estoque, porta `8083`.
- `pedido`: microserviço de pedidos, porta `8084`.

Fluxo desejado nesta etapa:

- O front lista produtos buscando dados no serviço `produto`.
- O front consulta quantidades no serviço `estoque`.
- O carrinho fica no `localStorage` para permitir simular duas sessões comprando o mesmo item.
- Se o serviço de estoque estiver indisponível, os produtos ainda devem aparecer com estoque `0`.
- A finalização de pedido ainda será evoluída para integrar pedidos, estoque e mensageria.

## Histórico de mudanças

### 2026-08-26 - Conexões de banco por variáveis de ambiente

#### Objetivo

Remover credenciais de banco dos arquivos versionáveis e preparar os serviços para gerar imagens Docker destinadas ao Amazon ECR e execução posterior na EC2.

#### Arquivos alterados

- `.gitignore`
- `produto/src/main/resources/application.properties`
- `estoque/src/main/resources/application.properties`
- `pedido/src/main/resources/application.properties`
- `produto/.env.example`
- `estoque/.env.example`
- `pedido/.env.example`

#### Arquivos locais criados e ignorados pelo Git

- `produto/.env`
- `estoque/.env`
- `pedido/.env`

#### O que foi alterado

1. Os serviços `produto`, `estoque` e `pedido` passaram a usar:
   - `DB_URL`;
   - `DB_USERNAME`;
   - `DB_PASSWORD`.
2. Foi adicionado `spring.config.import=optional:file:.env[.properties]` nos três serviços para permitir leitura de `.env` em desenvolvimento local.
3. Foram criados arquivos `.env.example` sem credenciais reais, servindo como modelo seguro para versionamento.
4. O `.gitignore` passou a ignorar arquivos `.env`, builds Maven (`target/`) e arquivos locais de IDE.

#### Observação importante

O arquivo `.env` ajuda no desenvolvimento local, mas em Docker/EC2 o mesmo conjunto de variáveis deve ser passado com `--env-file` ou por outro mecanismo de configuração do ambiente. O arquivo `.env` real não deve ser enviado ao Git.

---

### 2026-08-26 - Documento ABNT do trabalho AWS

#### Objetivo

Gerar um documento Word em formato acadêmico ABNT para servir como base editável do trabalho sobre AWS, microserviços, RabbitMQ, Docker, Amazon ECR e Amazon EC2.

#### Arquivos criados

- `docs/trabalho-aws-abnt.docx`
- `docs/assets/arquitetura-atual.png`
- `docs/assets/fluxo-pedido-rabbitmq.png`
- `docs/assets/fluxo-ecr-ec2.png`

#### O que foi incluído

1. Capa e folha de rosto com campos editáveis para instituição, curso, autor, professor e cidade.
2. Resumo, palavras-chave e sumário em estilo acadêmico.
3. Introdução, objetivos, fundamentação teórica, metodologia, desenvolvimento, etapa AWS, resultados parciais, próximas etapas e considerações finais.
4. Figuras explicando a arquitetura atual, o fluxo assíncrono do pedido via RabbitMQ e o fluxo de publicação com ECR e EC2.
5. Referências formatadas em estilo acadêmico, usando documentação oficial de AWS, Docker, RabbitMQ e Spring AMQP.
6. Apêndices com comandos de virtualização no Windows, build Maven, Docker build/tag/push, login no ECR, execução na EC2 e atualização de imagem.

#### Validação realizada

O arquivo foi exportado para PDF pelo Microsoft Word e convertido em PNG para revisão visual das 16 páginas. Foram conferidos capa, folha de rosto, resumo, sumário, tabelas, figuras, referências e blocos de comandos.

O renderizador padrão por LibreOffice não estava disponível no ambiente porque o executável `soffice` não foi encontrado; por isso a revisão visual foi feita por Word + PDF + Poppler.

---

### 2026-08-24 - Listener e producer RabbitMQ no serviço estoque

#### Objetivo

Implementar apenas o lado do microserviço `estoque` no fluxo RabbitMQ, deixando o listener e producer do microserviço `pedido` para implementação manual posterior.

#### Arquivos alterados

- `estoque/src/main/java/com/cloudcommerce/estoque/repository/EstoqueRepository.java`
- `estoque/src/main/java/com/cloudcommerce/estoque/service/EstoquePedidoService.java`
- `estoque/src/main/java/com/cloudcommerce/estoque/messaging/PedidoSolicitadoListener.java`
- `estoque/src/main/java/com/cloudcommerce/estoque/messaging/EstoqueRespostaProducer.java`
- `ENSINAR_RABBITMQ.md`

#### O que foi alterado

1. Foi criado `PedidoSolicitadoListener` para consumir mensagens da fila `commerce.pedido.solicitado.queue`.
2. Foi criado `EstoquePedidoService` para validar o pedido, verificar estoque e reduzir quantidade.
3. Foi criado `EstoqueRespostaProducer` para publicar `EstoqueRespostaMessage` com routing key `estoque.resposta`.
4. `EstoqueRepository` recebeu busca com `@Lock(LockModeType.PESSIMISTIC_WRITE)` para bloquear a linha do produto durante a baixa.
5. O processamento agrupa itens repetidos por `produtoId` antes de verificar estoque.
6. O listener foi anotado com `@Transactional` para evitar confirmar baixa de estoque se a publicação da resposta falhar.
7. A resposta do estoque retorna status:
   - `PROCESSADO`;
   - `SEM_ESTOQUE`.

#### Observações

O producer e o listener do serviço `pedido` ainda não foram implementados, conforme combinado.

O fluxo atual preparado no estoque é:

```text
commerce.pedido.solicitado.queue
-> PedidoSolicitadoListener
-> EstoquePedidoService
-> EstoqueRespostaProducer
-> commerce.pedidos.exchange com routing key estoque.resposta
```

#### Validação realizada

- `git diff --check` executado sem erros de whitespace.
- Busca estática confirmou:
  - `@RabbitListener`;
  - `RabbitTemplate`;
  - `convertAndSend`;
  - `@Transactional`;
  - `PESSIMISTIC_WRITE`;
  - status `PROCESSADO` e `SEM_ESTOQUE`.

Não foi possível executar build Maven neste ambiente porque `java` e `mvn` não estão disponíveis no PATH.

### 2026-08-24 - Comentários didáticos na configuração RabbitMQ

#### Objetivo

Adicionar comentários nos arquivos de configuração do RabbitMQ para facilitar o estudo linha a linha.

#### Arquivos alterados

- `pedido/src/main/resources/application.properties`
- `estoque/src/main/resources/application.properties`
- `pedido/src/main/java/com/cloudcommerce/pedido/config/RabbitMQConfig.java`
- `estoque/src/main/java/com/cloudcommerce/estoque/config/RabbitMQConfig.java`

#### O que foi alterado

1. Comentários nos `application.properties` explicando conexão, exchange, filas e routing keys.
2. Comentários nos `RabbitMQConfig.java` explicando `@Value`, `DirectExchange`, `Queue`, `Binding` e `MessageConverter`.
3. Comentários curtos nos parâmetros `durable` e `autoDelete` da exchange.

#### Validação realizada

- `git diff --check` executado sem erros de whitespace.

### 2026-08-24 - Configuração inicial do RabbitMQ

#### Objetivo

Iniciar a etapa de mensageria distribuída configurando RabbitMQ nos serviços `pedido` e `estoque`, sem ainda acoplar a regra de negócio de finalizar pedido.

#### Arquivos alterados

- `docker-compose.yml`
- `ENSINAR_RABBITMQ.md`
- `pedido/pom.xml`
- `pedido/src/main/resources/application.properties`
- `pedido/src/main/java/com/cloudcommerce/pedido/config/RabbitMQConfig.java`
- `pedido/src/main/java/com/cloudcommerce/pedido/messaging/PedidoSolicitadoMessage.java`
- `pedido/src/main/java/com/cloudcommerce/pedido/messaging/EstoqueRespostaMessage.java`
- `estoque/pom.xml`
- `estoque/src/main/resources/application.properties`
- `estoque/src/main/java/com/cloudcommerce/estoque/config/RabbitMQConfig.java`
- `estoque/src/main/java/com/cloudcommerce/estoque/messaging/PedidoSolicitadoMessage.java`
- `estoque/src/main/java/com/cloudcommerce/estoque/messaging/EstoqueRespostaMessage.java`

#### O que foi alterado

1. Foi criado `docker-compose.yml` com RabbitMQ e painel Management.
2. Os serviços `pedido` e `estoque` receberam a dependência `spring-boot-starter-amqp`.
3. Os dois serviços receberam propriedades `spring.rabbitmq.*` com valores padrão locais e suporte a variáveis de ambiente.
4. Foi criada a exchange `commerce.pedidos.exchange`.
5. Foram criadas duas filas:
   - `commerce.pedido.solicitado.queue`;
   - `commerce.estoque.resposta.queue`.
6. Foram criadas duas routing keys:
   - `pedido.solicitado`;
   - `estoque.resposta`.
7. Foram criados bindings ligando exchange, filas e routing keys.
8. Foi configurado `JacksonJsonMessageConverter` para serializar mensagens como JSON.
9. Foram criados os records de contrato:
   - `PedidoSolicitadoMessage`;
   - `EstoqueRespostaMessage`.
10. Foi criado o documento raiz `ENSINAR_RABBITMQ.md`, explicando cada peça da configuração.

#### Estado atual

O RabbitMQ está configurado, mas o fluxo de negócio ainda não publica nem consome mensagens.

A próxima etapa deve implementar:

1. `pedido` publicando `PedidoSolicitadoMessage`.
2. `estoque` consumindo a mensagem.
3. `estoque` respondendo com `EstoqueRespostaMessage`.
4. `pedido` consumindo a resposta e atualizando o status.

#### Validação realizada

- POMs de `pedido` e `estoque` parseados como XML com sucesso.
- Busca estática confirmou dependência AMQP, propriedades RabbitMQ, configs e records de mensagem.
- `git diff --check` executado sem erros de whitespace.

Não foi possível validar neste ambiente:

- `docker compose config`, porque `docker` não está disponível no PATH.
- build Maven, porque `java` e `mvn` não estão disponíveis no PATH.

### 2026-08-21 - Toast Bootstrap ao adicionar produto no carrinho

#### Objetivo

Melhorar a experiência ao adicionar produtos no carrinho, substituindo o `alert()` bloqueante do navegador por uma notificação visual integrada ao Bootstrap.

#### Arquivos alterados

- `cloud-commerce/src/main/resources/static/js/app.js`

#### O que foi alterado

1. A função `adicionarAoCarrinho` deixou de chamar `alert()` diretamente.
2. Foi criada a função `mostrarToastCarrinho`.
3. Foi criada a função `obterContainerToast`, que adiciona dinamicamente o container de toasts na página.
4. O toast mostra:
   - título da ação;
   - mensagem com o nome do produto;
   - botão para abrir o carrinho.
5. Foi mantido fallback para `alert()` caso o Bootstrap não esteja disponível.

#### Validação realizada

- `node --check` executado com sucesso em `app.js`.
- `git diff --check` executado sem erros de whitespace no arquivo alterado.

### 2026-08-21 - Padronização visual com Bootstrap e limpeza do front

#### Objetivo

Melhorar a experiência visual do front e deixar HTML, CSS e JavaScript mais legíveis para estudo.

#### Contexto

As telas estavam funcionando de forma simples, mas cada uma tinha marcação própria, classes CSS antigas e scripts com comentários muito grandes. Para estudar o projeto com mais clareza, a camada de front precisava ficar mais organizada e previsível.

#### Arquivos alterados

- `cloud-commerce/src/main/resources/templates/home.html`
- `cloud-commerce/src/main/resources/templates/estoque.html`
- `cloud-commerce/src/main/resources/templates/pedidos.html`
- `cloud-commerce/src/main/resources/templates/carrinho.html`
- `cloud-commerce/src/main/resources/static/css/style.css`
- `cloud-commerce/src/main/resources/static/js/app.js`
- `cloud-commerce/src/main/resources/static/js/estoque.js`
- `cloud-commerce/src/main/resources/static/js/pedidos.js`

#### O que foi alterado

1. As quatro páginas passaram a carregar Bootstrap via CDN.
2. A navegação foi padronizada com `navbar` responsiva do Bootstrap.
3. O contador do carrinho passou a aparecer no header das páginas.
4. A home virou um painel de acompanhamento do projeto, mostrando os serviços já conectados.
5. A tela de produtos passou a renderizar cards Bootstrap com status de estoque.
6. A tela de pedidos passou a renderizar cards Bootstrap com status, data, total e quantidade de itens.
7. A tela de carrinho passou a usar layout em duas colunas, com lista de itens e resumo lateral.
8. O CSS local foi reduzido para personalizações pequenas sobre o Bootstrap.
9. `app.js` foi reorganizado para concentrar carrinho, totalização e formatação de moeda.
10. `estoque.js` foi separado em funções de busca, mapeamento de estoque e renderização.
11. `pedidos.js` foi separado em funções de busca, renderização, status e formatação de data.

#### Validação realizada

- `git diff --check` executado sem erros de whitespace.
- `node --check` executado com sucesso para:
  - `app.js`;
  - `estoque.js`;
  - `pedidos.js`.
- Busca por classes antigas do layout anterior não encontrou referências restantes relevantes.

#### Observações

O Bootstrap está sendo carregado por CDN:

- CSS: `https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css`
- JS: `https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js`

Isso facilita a etapa de estudo sem alterar o build Maven. Em uma etapa futura com Docker/AWS, pode fazer sentido empacotar assets localmente ou usar um processo de build do front.

#### Próximos passos recomendados

1. Subir o front e navegar pelas quatro telas.
2. Testar o carrinho em duas abas para manter a simulação de concorrência.
3. Validar visualmente a tela de produtos com `produto` e `estoque` ativos.
4. Validar visualmente a tela de pedidos com o serviço `pedido` ativo.
5. Depois disso, iniciar a implementação do fluxo RabbitMQ.

### 2026-08-19 - Tela de pedidos usando GET real do serviço pedido

#### Objetivo

Remover os pedidos mockados do front e fechar o terceiro endpoint de leitura da etapa atual:

- GET de produtos.
- GET de estoque.
- GET de pedidos.

Com isso, o projeto fica mais preparado para iniciar a próxima etapa de mensageria com RabbitMQ.

#### Diagnóstico

A página `/pedidos` do front ainda renderizava dados mockados via Thymeleaf. O controller MVC `cloud-commerce` criava uma lista fixa de pedidos e enviava para o template.

Além disso, o endpoint GET do microserviço `pedido` retornava entidades JPA diretamente. Como `Pedido` se relaciona com `PedidoItem`, e `PedidoItem` referencia `Pedido`, isso poderia causar problemas de serialização JSON, recursão ou exposição excessiva do modelo interno.

#### Arquivos alterados

- `cloud-commerce/src/main/java/com/sirius/cloud_commerce/Controlers/PedidoController.java`
- `cloud-commerce/src/main/resources/templates/pedidos.html`
- `cloud-commerce/src/main/resources/static/js/pedidos.js`
- `pedido/src/main/java/com/cloudcommerce/pedido/controller/PedidoController.java`
- `pedido/src/main/java/com/cloudcommerce/pedido/repository/PedidoRepository.java`
- `pedido/src/main/java/com/cloudcommerce/pedido/dto/PedidoResponse.java`
- `pedido/src/main/java/com/cloudcommerce/pedido/dto/PedidoItemResponse.java`

#### O que foi alterado

1. O controller MVC do front deixou de criar pedidos mockados.
2. A página `pedidos.html` deixou de depender de `th:each` com `${pedidos}`.
3. Foi criado `pedidos.js`, que busca pedidos em `http://localhost:8084/pedidos`.
4. A tela agora monta os cards de pedidos no navegador, mostrando:
   - número do pedido;
   - data de criação;
   - quantidade de itens;
   - status;
   - valor total.
5. O microserviço `pedido` passou a retornar `PedidoResponse` no GET.
6. Foi criado `PedidoItemResponse` para expor os itens sem devolver a referência circular para `Pedido`.
7. O repository de pedidos passou a carregar os itens com `@EntityGraph(attributePaths = "itens")` nos métodos de busca.

#### Validação realizada

Foi feita revisão estática do código e diff das alterações.

Não foi possível executar build Maven neste ambiente porque:

- `java` não está disponível no PATH.
- `mvn` não está disponível no PATH.
- `JAVA_HOME` não está configurado.

#### Próximos passos recomendados

1. Subir `pedido` na porta `8084`.
2. Acessar `http://localhost:8084/pedidos` e confirmar que a resposta é uma lista JSON com `id`, `status`, `valorTotal`, `criadoEm` e `itens`.
3. Subir `cloud-commerce` na porta `8081`.
4. Acessar `http://localhost:8081/pedidos` e confirmar se os cards são renderizados a partir do serviço.
5. Depois disso, iniciar a etapa RabbitMQ com os eventos:
   - `PedidoSolicitado`;
   - `VerificarEstoque`;
   - `EstoqueDisponivel`;
   - `EstoqueIndisponivel`.

### 2026-08-19 - Correção da exibição de estoque na tela de produtos

#### Objetivo

Corrigir o problema em que a tela de produtos exibia os dados de produtos, mas não mostrava corretamente as quantidades vindas do microserviço de estoque.

#### Diagnóstico

O backend do serviço `estoque` possui o modelo `Estoque` com a propriedade Java `produtoId`. Pelo padrão do Jackson/Spring Boot, o JSON retornado pelo endpoint `/estoque` usa o campo:

```json
{
  "produtoId": 1,
  "quantidade": 10
}
```

Porém, a implementação inline dentro de `estoque.html` estava tentando cruzar os dados usando `item.idProduto`. Como `idProduto` não existe na resposta, o front não encontrava correspondência entre produto e estoque. O resultado era cada produto cair no fallback de quantidade `0`.

Também havia duplicação de lógica:

- Uma versão inline dentro de `estoque.html`.
- Uma versão separada em `static/js/estoque.js`.

Essa duplicação aumentava o risco de uma tela funcionar em uma versão e quebrar em outra.

#### Arquivos alterados

- `cloud-commerce/src/main/resources/templates/estoque.html`
- `cloud-commerce/src/main/resources/static/js/estoque.js`

#### O que foi alterado

1. `estoque.html` deixou de ter a lógica JavaScript inline da tela.
2. `estoque.html` passou a carregar `@{/js/estoque.js}`.
3. `estoque.js` ficou como fonte única da lógica da tela de produtos/estoque.
4. A associação produto x estoque usa `estoque.produtoId`, alinhado ao backend.
5. A busca de estoque agora é tolerante a falha:
   - Se `produto` estiver disponível, os produtos aparecem.
   - Se `estoque` estiver indisponível, as quantidades aparecem como `0`.
6. O botão de pedido fica desabilitado quando a quantidade em estoque é menor ou igual a `0`.

#### Validação realizada

Foram feitas validações estáticas no código e inspeção dos endpoints/configurações.

Não foi possível validar visualmente no navegador neste momento porque as portas abaixo não estavam ativas:

- `8081`: front `cloud-commerce`.
- `8082`: serviço `produto`.
- `8083`: serviço `estoque`.
- `8084`: serviço `pedido`.

Também não foi possível executar o build Maven neste ambiente porque:

- `java` não está disponível no PATH.
- `mvn` não está disponível no PATH.
- `JAVA_HOME` não está configurado.

#### Observações arquiteturais

Para a etapa atual de aprendizado, o front consultar `produto` e `estoque` via GET é aceitável para listar informações. Isso ajuda a visualizar a separação entre dados de catálogo e dados de disponibilidade.

Para a etapa de compra, o ideal é não deixar o front "decidir" a confirmação do pedido. O fluxo futuro deve caminhar para algo como:

1. Front envia pedido ao serviço `pedido`.
2. Serviço `pedido` cria pedido com status `PENDENTE`.
3. Serviço `pedido` publica uma mensagem, por exemplo `PedidoCriado`.
4. Serviço `estoque` consome a mensagem e tenta reservar/baixar estoque.
5. Serviço `estoque` publica resposta, por exemplo `EstoqueReservado` ou `EstoqueInsuficiente`.
6. Serviço `pedido` atualiza status para `CONFIRMADO` ou `RECUSADO`.

Esse fluxo demonstra melhor o conceito de mensageria distribuída e reduz acoplamento direto entre serviços no caminho crítico de confirmação.

#### Pendências identificadas

- O carrinho ainda apenas usa `console.log` e `alert` na finalização; ainda não envia POST real ao serviço `pedido`.
- O front ainda chama `localhost:8082` e `localhost:8083` diretamente. Para Docker/AWS, isso deve evoluir para configuração por ambiente ou API Gateway.
- Há credenciais de banco expostas nos arquivos `application.properties`; antes de versionar/publicar ou subir em cloud, mover para variáveis de ambiente.
- O controller MVC `EstoqueController` do front ainda cria produtos mockados, mas a tela atual busca produtos via JavaScript nos microserviços. Esses mocks podem ser removidos ou mantidos apenas para fallback intencional.

#### Próximos passos recomendados

1. Subir `produto`, `estoque` e `cloud-commerce` localmente e testar `http://localhost:8081/estoque`.
2. Confirmar no navegador se `/produtos` retorna produtos e `/estoque` retorna objetos com `produtoId`.
3. Implementar a finalização real do carrinho com POST para o serviço `pedido`.
4. Definir DTO de criação de pedido para evitar expor diretamente as entidades JPA no contrato da API.
5. Depois, introduzir RabbitMQ no fluxo de confirmação/reserva de estoque.

---

## 2026-08-25 - POST de pedidos com service e mensagem RabbitMQ

### Objetivo

Criar a camada de service do microservico `pedido` para que o POST de pedidos deixe de salvar a entidade diretamente no controller.

### Arquivos alterados

- `pedido/src/main/java/com/cloudcommerce/pedido/service/PedidoService.java`
- `pedido/src/main/java/com/cloudcommerce/pedido/controller/PedidoController.java`
- `pedido/src/main/java/com/cloudcommerce/pedido/dto/CriarPedidoRequest.java`
- `pedido/src/main/java/com/cloudcommerce/pedido/messaging/PedidoSolicitadoProducer.java`
- `docs/diario-tecnico.md`
- `ENSINAR_RABBITMQ.md`

### O que mudou

1. `PedidoService` passou a ser um `@Service` real do Spring.
2. O POST `/pedidos` agora recebe `CriarPedidoRequest`.
3. O service cria o `Pedido`, cria os `PedidoItem`, calcula `valorTotal` e salva no banco.
4. O pedido novo nasce com status `PROCESSANDO`.
5. Depois de salvar, o service monta uma `PedidoSolicitadoMessage`.
6. `PedidoSolicitadoProducer` publica a mensagem na exchange usando a routing key `pedido.solicitado`.
7. O controller retorna `201 Created` com `PedidoResponse`.
8. Erros simples de entrada retornam `400 Bad Request`.

### Fluxo atual

```text
POST /pedidos
-> PedidoController recebe CriarPedidoRequest
-> PedidoService valida os itens
-> PedidoService cria Pedido + PedidoItem
-> PedidoRepository salva no banco
-> PedidoService cria PedidoSolicitadoMessage
-> PedidoSolicitadoProducer envia para RabbitMQ
-> Estoque consome commerce.pedido.solicitado.queue
```

### Observacao importante

O envio da mensagem ainda acontece logo apos o `save` do pedido. Para um projeto de estudo isso e bom porque deixa o fluxo facil de enxergar. Em sistemas reais, o proximo refinamento seria estudar o Outbox Pattern para evitar inconsistencias entre banco de dados e RabbitMQ.

---

## 2026-08-25 - Preparacao para teste pela tela

### Objetivo

Deixar o fluxo navegavel pelo front para criar pedido real a partir do carrinho.

### Arquivos alterados

- `cloud-commerce/src/main/resources/static/js/app.js`
- `produto/src/main/java/com/cloudservice/produto/controller/ProdutoController.java`
- `estoque/src/main/java/com/cloudcommerce/estoque/controller/EstoqueController.java`
- `docs/diario-tecnico.md`

### O que mudou

1. O carrinho deixou de apenas exibir `alert` e `console.log` ao finalizar.
2. `finalizarPedido()` agora envia `POST http://localhost:8084/pedidos`.
3. O front converte os itens do carrinho para o contrato esperado pelo backend:

```json
{
  "itens": [
    {
      "produtoId": 1,
      "quantidade": 1,
      "precoUnitario": 99.9
    }
  ]
}
```

4. Ao criar o pedido com sucesso, o carrinho e limpo e a tela mostra o status retornado.
5. `produto` e `estoque` passaram a aceitar chamadas vindas do front em `http://localhost:8081`.

### Estado para teste

Agora ja e possivel testar pela tela ate este ponto:

```text
front carrinho
-> POST /pedidos
-> pedido salvo como PROCESSANDO
-> mensagem pedido.solicitado enviada ao RabbitMQ
-> estoque consome a mensagem
-> estoque baixa quantidade quando houver saldo
-> estoque publica resposta estoque.resposta
```

Ainda falta o listener do servico `pedido` para consumir `estoque.resposta` e atualizar o status final para `PROCESSADO` ou `SEM_ESTOQUE`.

### Atualizacao apos implementar o listener do pedido

O arquivo `pedido/src/main/java/com/cloudcommerce/pedido/messaging/EstoqueRespostaListener.java` agora esta conectado ao RabbitMQ com `@RabbitListener`.

Fluxo fechado:

```text
estoque publica estoque.resposta
-> RabbitMQ entrega na fila commerce.estoque.resposta.queue
-> EstoqueRespostaListener consome a mensagem
-> ListnerService valida pedidoId/status
-> ListnerService busca o pedido no banco
-> pedido recebe status PROCESSADO ou SEM_ESTOQUE
```

Tambem foi criada uma camada separada chamada `ListnerService` para manter o listener pequeno. O listener ficou responsavel por receber a mensagem; o service ficou responsavel pela regra de atualizar o pedido.

### Ajuste antes do teste integrado

O `@Transactional` foi removido do metodo `PedidoService.criar`.

Motivo:

```text
se o pedido publica mensagem antes do commit do banco,
o estoque pode responder muito rapido,
e o listener do pedido pode tentar atualizar um pedido ainda nao visivel no banco
```

Para esta fase do projeto, o fluxo ficou mais simples:

```text
PedidoRepository.save(...)
-> banco confirma o pedido
-> PedidoSolicitadoProducer publica a mensagem
```

Em um sistema real, o refinamento recomendado continua sendo estudar Outbox Pattern.

---

## 2026-08-25 - Diagnostico do erro ao atualizar status do pedido

### Sintoma

Durante o teste integrado, o pedido foi criado com status `PROCESSANDO`, o estoque consumiu a mensagem `pedido.solicitado` e publicou a resposta `estoque.resposta`.

Porem, ao consumir a resposta, o servico `pedido` tentou atualizar o pedido para `PROCESSADO` e o banco recusou.

### Erro encontrado

```text
ERROR: new row for relation "pedidos" violates check constraint "chk_pedido_status"
Failing row contains (1, PROCESSADO, 99.90, 2026-08-25 16:01:33.322652)
```

### Causa

A constraint atual do banco permite apenas estes status:

```text
PENDENTE
PROCESSANDO
PAGO
CONCLUIDO
ERRO
```

Mas o fluxo RabbitMQ atual usa:

```text
PROCESSANDO
PROCESSADO
SEM_ESTOQUE
```

### Consequencia no RabbitMQ

A mensagem `estoque.resposta` nao foi perdida. Ela voltou para a fila `commerce.estoque.resposta.queue` como mensagem pronta para reprocessamento.

### Correcao recomendada

Alinhar a constraint `chk_pedido_status` com os status usados pela aplicacao.

Para manter a linguagem escolhida no projeto, a opcao recomendada e permitir:

```text
PENDENTE
PROCESSANDO
PROCESSADO
SEM_ESTOQUE
CANCELADO
```

---

## 2026-08-25 - Documentacao de apresentacao e AWS

### Objetivo

Criar materiais de estudo e apresentacao para explicar o projeto e preparar a etapa de AWS com ECR e EC2.

### Arquivos criados

- `docs/roteiro-apresentacao-microservicos.md`
- `docs/trabalho-aws-ecr-ec2.md`

### Conteudo adicionado

1. Roteiro didatico para apresentar o projeto em video.
2. Explicacao das responsabilidades de front, produto, estoque, pedido e RabbitMQ.
3. Fluxogramas em Mermaid para o fluxo de pedido e arquitetura.
4. Documento formal em estilo de trabalho academico.
5. Registro do estado atual da AWS:
   - conta criada;
   - EC2 com 20 GB;
   - Docker instalado na EC2;
   - ECR criado;
   - policy de ECR associada na EC2.
6. Comandos de virtualizacao no Windows usados para habilitar Docker Desktop.
7. Comandos de build Maven, Docker build, Docker tag e Docker push para ECR.
8. Comandos de pull e atualizacao de containers na EC2.
9. Observacao de que o ECR e um registry, nao um servico de build.

---

## 2026-08-25 - Verificacao de prontidao para teste

### Objetivo

Revisar se os servicos estao coerentes para testar o fluxo atual.

### Resultado

O projeto esta pronto para testar ate o ponto em que o estoque recebe o pedido e publica a resposta.

O ciclo completo de status ainda nao esta fechado porque falta implementar o listener do servico `pedido` que consome `estoque.resposta`.

### Ajustes feitos durante a verificacao

1. `produto` liberou CORS para o front em `http://localhost:8081`.
2. `estoque` liberou CORS para o front em `http://localhost:8081`.
3. O carrinho passou a enviar `POST /pedidos` no servico `pedido`.
4. O JavaScript do carrinho converte `id` e `preco` do localStorage para `produtoId` e `precoUnitario`.
5. A constante do carrinho foi renomeada para evitar conflito com `pedidos.js`.
6. `EstoqueRespostaListener.java` deixou de ser um arquivo vazio e virou um placeholder explicito para a proxima etapa.

### Validacoes realizadas

```text
node --check app.js
node --check pedidos.js
node --check estoque.js
git diff --check
```

Todas passaram.

### Validacoes bloqueadas no ambiente atual

Nao foi possivel compilar com Maven porque o terminal atual nao tem `JAVA_HOME` configurado.

Tambem nao foi possivel validar Docker/RabbitMQ em execucao porque `docker` nao esta disponivel no PATH deste terminal.

### Ordem recomendada para teste manual

```text
1. Subir RabbitMQ
2. Subir produto na porta 8082
3. Subir estoque na porta 8083
4. Subir pedido na porta 8084
5. Subir cloud-commerce na porta 8081
6. Abrir /estoque e adicionar produto ao carrinho
7. Abrir /carrinho e finalizar pedido
8. Conferir /pedidos
9. Conferir no RabbitMQ se existe mensagem na fila commerce.estoque.resposta.queue
```
## 2026-08-26 - Documentacao do fluxo Docker, ECR e execucao local

### Objetivo

Criar documentos para acompanhar a etapa de publicacao em ECR/EC2 e facilitar a avaliacao externa do projeto.

### Arquivos criados

- `docs/passo-a-passo-fluxo-ecr-ec2.md`
- `docs/guia-execucao-local-professor.md`

### Conteudo adicionado

1. Explicacao de que `docker build` cria uma imagem local.
2. Registro do fluxo `codigo -> jar -> imagem local -> ECR -> container na EC2`.
3. Comandos de AWS CLI, ECR, Docker login, Maven package, Docker build, Docker tag, Docker push e Docker run.
4. Diferenca entre IAM User local para `push` e IAM Role da EC2 para `pull`.
5. Guia para baixar o projeto, criar arquivos `.env`, subir RabbitMQ e executar os quatro servicos localmente.
6. Lista de problemas comuns: Docker daemon desligado, Java 21 ausente, jar nao encontrado e APIs fora do ar.

---
