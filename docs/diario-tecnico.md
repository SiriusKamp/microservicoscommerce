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
