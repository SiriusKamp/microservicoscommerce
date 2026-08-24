# Ensinar RabbitMQ no Microserviços Commerce

Este documento explica a configuração inicial do RabbitMQ no projeto. A intenção é servir como material de estudo: não apenas dizer "o que foi criado", mas explicar o papel de cada peça.

## Objetivo desta etapa

Nesta primeira etapa, o RabbitMQ foi configurado como infraestrutura de mensageria entre os serviços `pedido` e `estoque`.

Ainda não foi implementada a regra de negócio completa de publicar e consumir mensagens. O foco aqui é preparar a base:

1. Subir o RabbitMQ localmente.
2. Ensinar os serviços Spring Boot a se conectarem ao RabbitMQ.
3. Criar exchange, filas e bindings.
4. Definir o formato inicial das mensagens.

## O fluxo que queremos construir

O fluxo final será:

1. O front envia uma compra para o serviço `pedido`.
2. O serviço `pedido` cria um pedido com status `PENDENTE`.
3. O serviço `pedido` publica uma mensagem `PedidoSolicitado`.
4. O serviço `estoque` consome essa mensagem.
5. O serviço `estoque` verifica se há estoque suficiente.
6. O serviço `estoque` publica uma resposta.
7. O serviço `pedido` recebe a resposta e atualiza o pedido para `CONFIRMADO` ou `RECUSADO`.

Nesta etapa, fizemos a preparação das peças 3, 4, 6 e 7.

## Arquivos criados ou alterados

### Infraestrutura local

- `docker-compose.yml`

Define um container RabbitMQ local com painel de administração.

### Serviço pedido

- `pedido/pom.xml`
- `pedido/src/main/resources/application.properties`
- `pedido/src/main/java/com/cloudcommerce/pedido/config/RabbitMQConfig.java`
- `pedido/src/main/java/com/cloudcommerce/pedido/messaging/PedidoSolicitadoMessage.java`
- `pedido/src/main/java/com/cloudcommerce/pedido/messaging/EstoqueRespostaMessage.java`

### Serviço estoque

- `estoque/pom.xml`
- `estoque/src/main/resources/application.properties`
- `estoque/src/main/java/com/cloudcommerce/estoque/config/RabbitMQConfig.java`
- `estoque/src/main/java/com/cloudcommerce/estoque/messaging/PedidoSolicitadoMessage.java`
- `estoque/src/main/java/com/cloudcommerce/estoque/messaging/EstoqueRespostaMessage.java`

## docker-compose.yml

Arquivo:

```text
docker-compose.yml
```

Ele sobe o RabbitMQ com a imagem:

```text
rabbitmq:3.13-management
```

Essa imagem tem duas coisas:

- RabbitMQ em si, usado pelos microserviços.
- Painel Management, usado por nós no navegador.

Portas:

```text
5672  -> porta usada pelas aplicações Spring Boot
15672 -> porta usada pelo painel web
```

Usuário e senha locais:

```text
guest / guest
```

Para subir:

```bash
docker compose up -d rabbitmq
```

Para abrir o painel:

```text
http://localhost:15672
```

## Dependência AMQP

Arquivos:

```text
pedido/pom.xml
estoque/pom.xml
```

Foi adicionada a dependência:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
```

Essa dependência adiciona suporte do Spring Boot para RabbitMQ.

Com ela, o projeto ganha acesso a classes como:

- `RabbitTemplate`: usado para publicar mensagens.
- `@RabbitListener`: usado para consumir mensagens.
- `Queue`: representa uma fila.
- `DirectExchange`: representa uma exchange direta.
- `Binding`: conecta uma fila a uma exchange por uma routing key.

## Configuração de conexão

Arquivos:

```text
pedido/src/main/resources/application.properties
estoque/src/main/resources/application.properties
```

Foram adicionadas estas propriedades:

```properties
spring.rabbitmq.host=${RABBITMQ_HOST:localhost}
spring.rabbitmq.port=${RABBITMQ_PORT:5672}
spring.rabbitmq.username=${RABBITMQ_USERNAME:guest}
spring.rabbitmq.password=${RABBITMQ_PASSWORD:guest}
```

O padrão local é:

```text
localhost:5672
guest / guest
```

Mas os valores também podem vir por variável de ambiente. Isso será importante quando o projeto for para Docker, Kubernetes ou AWS.

Exemplo:

```bash
RABBITMQ_HOST=rabbitmq
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=guest
RABBITMQ_PASSWORD=guest
```

## Exchange

Propriedade:

```properties
commerce.rabbitmq.exchange=commerce.pedidos.exchange
```

A exchange é o ponto de entrada das mensagens.

Pense nela como uma "central de distribuição". O serviço `pedido` não precisa enviar diretamente para a fila do `estoque`. Ele envia para a exchange com uma routing key. A exchange decide para qual fila a mensagem vai.

Tipo usado:

```java
DirectExchange
```

Uma `DirectExchange` entrega a mensagem para filas cuja binding usa exatamente a mesma routing key.

## Filas

Foram criadas duas filas:

```properties
commerce.rabbitmq.queue.pedido-solicitado=commerce.pedido.solicitado.queue
commerce.rabbitmq.queue.estoque-resposta=commerce.estoque.resposta.queue
```

### commerce.pedido.solicitado.queue

Fila que receberá mensagens de pedido solicitado.

Uso futuro:

```text
pedido publica -> estoque consome
```

### commerce.estoque.resposta.queue

Fila que receberá respostas do estoque.

Uso futuro:

```text
estoque publica -> pedido consome
```

## Routing keys

Foram criadas duas routing keys:

```properties
commerce.rabbitmq.routing-key.pedido-solicitado=pedido.solicitado
commerce.rabbitmq.routing-key.estoque-resposta=estoque.resposta
```

Routing key é a etiqueta usada para direcionar a mensagem.

Exemplo:

```text
Mensagem publicada com routing key pedido.solicitado
-> cai na fila commerce.pedido.solicitado.queue
```

Outro exemplo:

```text
Mensagem publicada com routing key estoque.resposta
-> cai na fila commerce.estoque.resposta.queue
```

## Binding

Binding é a ligação entre:

- exchange;
- fila;
- routing key.

No projeto temos:

```text
commerce.pedidos.exchange + pedido.solicitado -> commerce.pedido.solicitado.queue
commerce.pedidos.exchange + estoque.resposta -> commerce.estoque.resposta.queue
```

Sem binding, a exchange recebe a mensagem, mas não sabe em qual fila entregar.

## RabbitMQConfig.java

Arquivos:

```text
pedido/src/main/java/com/cloudcommerce/pedido/config/RabbitMQConfig.java
estoque/src/main/java/com/cloudcommerce/estoque/config/RabbitMQConfig.java
```

Essas classes criam os beans do RabbitMQ quando a aplicação Spring sobe.

Principais beans:

```java
DirectExchange
Queue
Binding
MessageConverter
```

### DirectExchange

Cria a exchange:

```text
commerce.pedidos.exchange
```

### Queue

Cria as filas:

```text
commerce.pedido.solicitado.queue
commerce.estoque.resposta.queue
```

### Binding

Liga as filas na exchange usando as routing keys:

```text
pedido.solicitado
estoque.resposta
```

### MessageConverter

Foi configurado:

```java
JacksonJsonMessageConverter
```

Ele permite que mensagens Java sejam convertidas para JSON antes de ir para o RabbitMQ.

Isso é importante porque mensagens entre microserviços devem ser dados simples, não objetos complexos de memória.

Observação: em versões novas do Spring AMQP, o `Jackson2JsonMessageConverter` ficou depreciado para remoção, e o recomendado é usar `JacksonJsonMessageConverter`, baseado no Jackson 3.

## Mensagem PedidoSolicitadoMessage

Arquivos:

```text
pedido/src/main/java/com/cloudcommerce/pedido/messaging/PedidoSolicitadoMessage.java
estoque/src/main/java/com/cloudcommerce/estoque/messaging/PedidoSolicitadoMessage.java
```

Formato:

```java
public record PedidoSolicitadoMessage(
        Long pedidoId,
        BigDecimal valorTotal,
        LocalDateTime solicitadoEm,
        List<Item> itens
) {
    public record Item(
            Long produtoId,
            Integer quantidade
    ) {
    }
}
```

Essa mensagem representa:

```text
Um pedido foi solicitado. Estoque, por favor, verifique estes itens.
```

Campos:

- `pedidoId`: identifica o pedido criado.
- `valorTotal`: valor total do pedido.
- `solicitadoEm`: data/hora em que o pedido foi solicitado.
- `itens`: produtos e quantidades que precisam ser verificados.

## Mensagem EstoqueRespostaMessage

Arquivos:

```text
pedido/src/main/java/com/cloudcommerce/pedido/messaging/EstoqueRespostaMessage.java
estoque/src/main/java/com/cloudcommerce/estoque/messaging/EstoqueRespostaMessage.java
```

Formato:

```java
public record EstoqueRespostaMessage(
        Long pedidoId,
        Boolean estoqueDisponivel,
        String status,
        String motivo,
        LocalDateTime respondidoEm
) {
}
```

Essa mensagem representa:

```text
Pedido, já verifiquei o estoque. Aqui está a resposta.
```

Campos:

- `pedidoId`: identifica qual pedido deve ser atualizado.
- `estoqueDisponivel`: `true` ou `false`.
- `status`: texto curto para orientar o próximo status do pedido.
- `motivo`: explicação opcional, por exemplo "Produto 3 sem estoque suficiente".
- `respondidoEm`: data/hora da resposta.

## Por que a mensagem existe nos dois serviços?

Por enquanto, os records foram duplicados em `pedido` e `estoque`.

Isso deixa cada microserviço independente e fácil de estudar. Em projetos maiores, existem outras opções:

- criar uma biblioteca compartilhada de contratos;
- usar schema registry;
- versionar mensagens com JSON Schema, Avro ou Protobuf.

Para este projeto de estudo, duplicar o contrato é aceitável desde que os dois lados mantenham o mesmo formato.

## Estado atual

Neste momento, o RabbitMQ está configurado e o lado do serviço `estoque` já possui listener, regra de verificação/baixa e producer de resposta.

Já foi implementado no `estoque`:

1. Listener para consumir `PedidoSolicitadoMessage`.
2. Service transacional para validar pedido e baixar estoque.
3. Producer para publicar `EstoqueRespostaMessage`.
4. Busca com bloqueio pessimista para reduzir risco de duas compras baixarem o mesmo último item ao mesmo tempo.

Ainda falta implementar no `pedido`:

1. Publicador para enviar `PedidoSolicitadoMessage`.
2. Listener para receber `EstoqueRespostaMessage`.
3. Atualização do status do pedido para `PROCESSADO` ou `SEM_ESTOQUE`.

## Listener do estoque

Arquivo:

```text
estoque/src/main/java/com/cloudcommerce/estoque/messaging/PedidoSolicitadoListener.java
```

Responsabilidade:

```text
Ler mensagens da fila commerce.pedido.solicitado.queue.
```

Ponto principal:

```java
@RabbitListener(queues = "${commerce.rabbitmq.queue.pedido-solicitado}")
public void receberPedidoSolicitado(PedidoSolicitadoMessage mensagem)
```

O `@RabbitListener` conecta o método a uma fila.

Quando uma mensagem aparece na fila configurada, o Spring AMQP chama esse método automaticamente.

O método também está anotado com:

```java
@Transactional
```

Nesta fase de estudo, isso faz a verificação/baixa do estoque e a tentativa de publicar a resposta ficarem na mesma transação de banco. Se a publicação da resposta falhar com exceção, a baixa de estoque não deve ser confirmada.

Observação importante: banco de dados e RabbitMQ continuam sendo recursos diferentes. Em sistemas de produção, o padrão mais robusto para garantir consistência entre banco e mensageria costuma ser o Outbox Pattern.

Fluxo do listener:

```text
recebe PedidoSolicitadoMessage
-> chama EstoquePedidoService
-> recebe EstoqueRespostaMessage
-> chama EstoqueRespostaProducer
```

## Service de estoque

Arquivo:

```text
estoque/src/main/java/com/cloudcommerce/estoque/service/EstoquePedidoService.java
```

Responsabilidade:

```text
Validar o pedido, verificar estoque e baixar a quantidade quando possível.
```

Ponto principal:

```java
@Transactional
public EstoqueRespostaMessage processarPedidoSolicitado(...)
```

O `@Transactional` faz a verificação e a baixa de estoque acontecerem dentro de uma transação.

Fluxo:

```text
valida mensagem
-> agrupa itens por produtoId
-> busca estoque com bloqueio
-> se faltar estoque, responde SEM_ESTOQUE
-> se houver estoque, reduz quantidade
-> responde PROCESSADO
```

## Bloqueio pessimista no estoque

Arquivo:

```text
estoque/src/main/java/com/cloudcommerce/estoque/repository/EstoqueRepository.java
```

Ponto principal:

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
```

Esse lock pede ao banco para bloquear a linha do produto enquanto a transação está verificando e baixando o estoque.

Isso ajuda no seu cenário de estudo:

```text
estoque = 1
duas sessões tentam comprar ao mesmo tempo
uma transação bloqueia a linha primeiro
a outra espera
quando a segunda chegar, verá o estoque atualizado
```

Sem esse cuidado, duas execuções concorrentes poderiam ler `1` ao mesmo tempo e ambas aprovar o pedido.

## Producer do estoque

Arquivo:

```text
estoque/src/main/java/com/cloudcommerce/estoque/messaging/EstoqueRespostaProducer.java
```

Responsabilidade:

```text
Publicar a resposta do estoque na exchange.
```

Ponto principal:

```java
rabbitTemplate.convertAndSend(exchange, routingKey, mensagem);
```

Tradução:

```text
envie esta mensagem para esta exchange usando esta routing key
```

No projeto:

```text
exchange: commerce.pedidos.exchange
routing key: estoque.resposta
```

O RabbitMQ usa o binding para entregar essa mensagem na fila:

```text
commerce.estoque.resposta.queue
```

## Próximo passo recomendado

O próximo passo deve ser implementar o lado do serviço `pedido`:

```text
PedidoSolicitado
```

Fluxo da próxima etapa:

```text
POST /pedidos
-> salva pedido PENDENTE
-> publica PedidoSolicitadoMessage
-> mensagem aparece na fila commerce.pedido.solicitado.queue
```

Depois, o serviço `pedido` também deve consumir:

```text
commerce.estoque.resposta.queue
```

E atualizar o status:

```text
PROCESSADO
SEM_ESTOQUE
```
