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
