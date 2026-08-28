# Status atual do projeto para etapa AWS/Kubernetes

Data: 2026-08-28  
Projeto: Cloud Commerce  
Contexto: etapa RabbitMQ finalizada e preparacao para trabalho de Computacao em Nuvem e Kubernetes.

## 1. Resumo executivo

O projeto Cloud Commerce esta funcional como estudo de microservicos, mensageria e containerizacao.

O fluxo RabbitMQ foi finalizado com:

```text
DirectExchange para o fluxo principal pedido -> estoque -> pedido
TopicExchange para auditoria de pedido sem resposta do estoque
Queues, bindings e routing keys configurados via Spring AMQP
Eventos trafegando em JSON
RabbitMQ local via Docker Compose
```

Para a etapa AWS, o projeto ja iniciou o fluxo de imagens Docker com Amazon ECR e EC2:

```text
AWS CLI configurada
Repositorios ECR criados
Imagem Docker do servico pedido publicada no ECR com tag 2.0
EC2 acessada via SSH
EC2 autenticada no ECR usando IAM Role
Imagem pedido:2.0 baixada e executada na EC2
```

Ainda nao foi iniciada a etapa Kubernetes/EKS.

## 2. Servicos existentes

| Servico | Porta | Estado atual |
| --- | ---: | --- |
| `cloud-commerce` | 8081 | Front web Spring Boot/Thymeleaf |
| `produto` | 8082 | API de produtos com banco Supabase/PostgreSQL |
| `estoque` | 8083 | API de estoque e consumidor RabbitMQ |
| `pedido` | 8084 | API de pedidos, producer/listener RabbitMQ e auditoria |
| `rabbitmq` | 5672 / 15672 | Broker local via Docker |

## 3. Tecnologias ja aplicadas

| Tecnologia | Uso atual |
| --- | --- |
| Java 21 | Todos os servicos Spring Boot |
| Spring Boot 4.1.0 | APIs e front |
| Spring Web | Endpoints REST |
| Spring Data JPA | Persistencia |
| PostgreSQL/Supabase | Banco atual do projeto |
| Spring AMQP | Integracao RabbitMQ |
| RabbitMQ | Mensageria distribuida local |
| Docker | Imagens dos servicos |
| Docker Compose | RabbitMQ local |
| Amazon ECR | Registro de imagens Docker |
| Amazon EC2 | Execucao manual inicial de container |

## 4. Fluxo RabbitMQ finalizado

Fluxo principal:

```text
POST /pedidos
pedido salva pedido como PROCESSANDO
pedido publica evento pedido.solicitado
estoque consome o evento
estoque valida disponibilidade
estoque baixa quantidade quando existe saldo
estoque publica estoque.resposta
pedido consome a resposta
pedido atualiza status para PROCESSADO ou SEM_ESTOQUE
```

Fluxo de auditoria:

```text
pedido agenda uma verificacao periodica
busca pedidos PROCESSANDO ha mais de 30 segundos
publica pedido.sem-resposta-estoque em uma TopicExchange
listener de auditoria consome a fila e registra log
```

## 5. Exchanges, queues, bindings e routing keys

### DirectExchange

| Elemento | Nome |
| --- | --- |
| Exchange | `commerce.pedidos.exchange` |
| Queue | `commerce.pedido.solicitado.queue` |
| Queue | `commerce.estoque.resposta.queue` |
| Routing key | `pedido.solicitado` |
| Routing key | `estoque.resposta` |

Uso:

```text
pedido.solicitado -> fila consumida pelo estoque
estoque.resposta -> fila consumida pelo pedido
```

### TopicExchange

| Elemento | Nome |
| --- | --- |
| Exchange | `commerce.auditoria.topic.exchange` |
| Queue | `commerce.auditoria.pedido.queue` |
| Routing key | `pedido.sem-resposta-estoque` |
| Binding | `pedido.#` |

Uso:

```text
qualquer evento iniciado por pedido. pode cair na fila de auditoria
```

## 6. Endpoints atuais

### Produto

| Metodo | Endpoint |
| --- | --- |
| GET | `/produtos` |
| POST | `/produtos` |
| GET | `/produtos/{id}` |

### Estoque

| Metodo | Endpoint |
| --- | --- |
| GET | `/estoque` |
| GET | `/estoque/{idProduto}` |

### Pedido

| Metodo | Endpoint |
| --- | --- |
| GET | `/pedidos` |
| GET | `/pedidos/{id}` |
| POST | `/pedidos` |
| GET | `/pedido-itens/pedido/{pedidoId}` |
| GET | `/pedido-itens/{id}` |

### Front

| Metodo | Endpoint |
| --- | --- |
| GET | `/` |
| GET | `/estoque` |
| GET | `/carrinho` |
| GET | `/pedidos` |

## 7. Estado Docker/ECR/EC2

Repositorio ECR criado para cada servico:

```text
microservicoscommerce-cloud-commerce
microservicoscommerce-produto
microservicoscommerce-estoque
microservicoscommerce-pedido
```

Imagem ja publicada e validada:

```text
382597877252.dkr.ecr.sa-east-1.amazonaws.com/microservicoscommerce-pedido:2.0
```

Fluxo ja executado:

```text
mvn package
docker build
docker tag
docker push para ECR
ssh na EC2
docker pull na EC2
docker run na EC2
```

## 8. Variaveis de ambiente

Os servicos usam `.env` para separar codigo e configuracao.

Variaveis principais:

```text
DB_URL
DB_USERNAME
DB_PASSWORD
RABBITMQ_HOST
RABBITMQ_PORT
RABBITMQ_USERNAME
RABBITMQ_PASSWORD
```

Arquivos reais `.env` nao devem ir para o Git.

Arquivos de exemplo:

```text
produto/.env.example
estoque/.env.example
pedido/.env.example
```

Observacao para AWS/Kubernetes:

```text
No Kubernetes, essas configuracoes devem virar ConfigMap e Secret.
Para a entrega do trabalho, o enunciado pede Secret ficticio, sem credenciais reais.
```

## 9. O que ainda nao existe para Kubernetes

Ainda precisam ser criados:

```text
manifesto Deployment
manifesto Service LoadBalancer
manifesto ConfigMap
manifesto Secret ficticio
cluster Amazon EKS
configuracao kubectl para acessar o cluster
teste com duas replicas
teste de exclusao e recriacao automatica de Pod
evidencias com kubectl get nodes, pods, deployments e services
documento final com prints/saidas reais
limpeza dos recursos para evitar custos
```

## 10. Recomendacao para a etapa EKS

Para cumprir o trabalho de Kubernetes com menos risco, recomenda-se usar apenas uma aplicacao simples como alvo inicial.

Melhor candidato:

```text
cloud-commerce
```

Motivo:

```text
responde HTTP na porta 8081
nao depende diretamente de banco para iniciar
nao exige RabbitMQ para abrir a tela inicial
facilita demonstrar Docker, ECR, EKS, Deployment, Service, replicas, ConfigMap e Secret
```

Alternativa:

```text
criar uma API simples separada apenas para o desafio Kubernetes
```

Essa alternativa segue o enunciado, pois a complexidade funcional da aplicacao nao sera avaliada.

## 11. Pontos de atencao

1. O trabalho Kubernetes nao exige subir todos os microservicos.
2. O foco da avaliacao e o fluxo Aplicacao -> Docker -> Registro -> EKS -> Deployment -> Service -> Acesso externo.
3. Se usar `pedido`, `produto` ou `estoque`, sera necessario lidar com banco, RabbitMQ e variaveis reais.
4. Para atender ConfigMap/Secret com valores ficticios, a aplicacao precisa ter algum endpoint que comprove o recebimento, por exemplo `/config`.
5. O Service deve ser `LoadBalancer`.
6. O Deployment deve ter `replicas: 2`.
7. Depois das evidencias, recursos AWS devem ser removidos para evitar custo.

## 12. Status final

Status atual resumido:

```text
RabbitMQ: concluido
Docker local: iniciado e validado
ECR: iniciado e validado com pedido:2.0
EC2: acessada e validada
EKS/Kubernetes: ainda pendente
Documentacao AWS/Kubernetes: em preparacao
```
