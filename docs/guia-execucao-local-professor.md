# Guia de execucao local - Cloud Commerce

Este documento explica como baixar e executar localmente o projeto Cloud Commerce para fins de avaliacao e estudo.

O projeto e um e-commerce didatico construido com Java Spring Boot, front em Thymeleaf/JavaScript, banco PostgreSQL e mensageria com RabbitMQ.

## 1. Arquitetura local

Servicos:

```text
cloud-commerce -> aplicacao web/front, porta 8081
produto        -> API de produtos, porta 8082
estoque        -> API de estoque e consumidor RabbitMQ, porta 8083
pedido         -> API de pedidos e produtor/consumidor RabbitMQ, porta 8084
rabbitmq       -> broker de mensagens, portas 5672 e 15672
```

Fluxo funcional:

```text
usuario acessa o front
front consulta produto, estoque e pedido via HTTP
usuario adiciona itens ao carrinho
carrinho fica no localStorage do navegador
front envia POST para o servico pedido
pedido salva o pedido como PROCESSANDO
pedido publica mensagem de pedido solicitado no RabbitMQ
estoque consome a mensagem
estoque verifica disponibilidade
estoque reduz quantidade quando houver estoque
estoque publica resposta
pedido consome a resposta
pedido atualiza status para PROCESSADO ou SEM_ESTOQUE
front lista pedidos atualizados
```

## 2. Pre-requisitos

Instalar:

```text
Git
Java JDK 21
Docker Desktop
```

Validar no terminal:

```powershell
git --version
java -version
javac -version
docker --version
docker info
```

O `java -version` e o `javac -version` devem apontar para Java 21.

Caso o terminal esteja usando outra versao, no Windows e possivel apontar temporariamente para o JDK 21:

```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
```

## 3. Baixar o projeto

Clonar o repositorio:

```powershell
git clone URL_DO_REPOSITORIO
Set-Location ".\microservicoscommerce"
```

Substituir `URL_DO_REPOSITORIO` pela URL real do Git.

## 4. Configurar variaveis de ambiente

Os servicos que acessam banco possuem arquivos de exemplo:

```text
produto/.env.example
estoque/.env.example
pedido/.env.example
```

Para rodar localmente, criar uma copia chamada `.env` dentro de cada pasta:

```powershell
Copy-Item ".\produto\.env.example" ".\produto\.env"
Copy-Item ".\estoque\.env.example" ".\estoque\.env"
Copy-Item ".\pedido\.env.example" ".\pedido\.env"
```

Editar cada `.env` com as credenciais reais do banco:

```text
DB_URL=jdbc:postgresql://HOST:5432/NOME_DO_BANCO?sslmode=require
DB_USERNAME=USUARIO_DO_BANCO
DB_PASSWORD=SENHA_DO_BANCO
```

Nos servicos `estoque` e `pedido`, manter RabbitMQ local:

```text
RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=guest
RABBITMQ_PASSWORD=guest
```

Observacao: os arquivos `.env` nao devem ser enviados ao Git.

## 5. Subir RabbitMQ local

Na raiz do projeto:

```powershell
Set-Location "C:\CAMINHO\PARA\microservicoscommerce"
docker compose up -d rabbitmq
```

Validar containers:

```powershell
docker ps
```

Painel web do RabbitMQ:

```text
http://localhost:15672
```

Credenciais locais padrao:

```text
usuario: guest
senha: guest
```

A porta `5672` e usada pela aplicacao Spring Boot para trocar mensagens com o RabbitMQ.

A porta `15672` e usada pelo navegador para acessar a interface administrativa.

## 6. Rodar os servicos com Maven

Abrir quatro terminais separados.

### Terminal 1 - produto

```powershell
Set-Location "C:\CAMINHO\PARA\microservicoscommerce\produto"
.\mvnw.cmd spring-boot:run
```

Teste:

```text
http://localhost:8082/produtos
```

### Terminal 2 - estoque

```powershell
Set-Location "C:\CAMINHO\PARA\microservicoscommerce\estoque"
.\mvnw.cmd spring-boot:run
```

Teste:

```text
http://localhost:8083/estoque
```

### Terminal 3 - pedido

```powershell
Set-Location "C:\CAMINHO\PARA\microservicoscommerce\pedido"
.\mvnw.cmd spring-boot:run
```

Teste:

```text
http://localhost:8084/pedidos
```

### Terminal 4 - cloud-commerce

```powershell
Set-Location "C:\CAMINHO\PARA\microservicoscommerce\cloud-commerce"
.\mvnw.cmd spring-boot:run
```

Abrir no navegador:

```text
http://localhost:8081
```

## 7. Ordem recomendada de execucao

Executar nesta ordem:

```text
1. RabbitMQ
2. produto
3. estoque
4. pedido
5. cloud-commerce
```

Motivo:

```text
produto e estoque fornecem dados para o front
pedido precisa conectar no RabbitMQ
estoque precisa conectar no RabbitMQ
front depende das APIs para exibir dados corretamente
```

## 8. Teste funcional esperado

1. Abrir:

```text
http://localhost:8081
```

2. Entrar na tela de estoque/produtos.

3. Adicionar item ao carrinho.

4. Abrir o carrinho.

5. Finalizar pedido.

6. Abrir tela de pedidos.

Resultado esperado:

```text
pedido aparece inicialmente como PROCESSANDO
apos resposta do estoque, pedido muda para PROCESSADO ou SEM_ESTOQUE
```

Para simular concorrencia:

```text
abrir duas sessoes do navegador
adicionar o mesmo item nas duas
manter apenas 1 unidade em estoque
finalizar os dois pedidos
validar se apenas um pedido processa e o outro fica sem estoque
```

## 9. Build local das aplicacoes

Cada servico pode ser empacotado com Maven:

```powershell
Set-Location "C:\CAMINHO\PARA\microservicoscommerce\pedido"
.\mvnw.cmd clean package -DskipTests
```

O resultado fica em:

```text
target/NOME_DO_SERVICO-0.0.1-SNAPSHOT.jar
```

Exemplos:

```text
produto/target/produto-0.0.1-SNAPSHOT.jar
estoque/target/estoque-0.0.1-SNAPSHOT.jar
pedido/target/pedido-0.0.1-SNAPSHOT.jar
cloud-commerce/target/cloud-commerce-0.0.1-SNAPSHOT.jar
```

## 10. Build Docker local

Depois que o `.jar` existir, e possivel criar uma imagem Docker local.

Exemplo com pedido:

```powershell
Set-Location "C:\CAMINHO\PARA\microservicoscommerce\pedido"
docker build -t microservicoscommerce-pedido .
```

Validar:

```powershell
docker images microservicoscommerce-pedido
```

Importante:

```text
docker build cria imagem local
ele nao publica automaticamente no Docker Hub nem no Amazon ECR
```

## 11. Problemas comuns

### Docker daemon nao esta rodando

Erro comum:

```text
failed to connect to the docker API
```

Solucao:

```text
abrir Docker Desktop
esperar Docker Desktop is running
rodar docker info
```

### Java 21 nao encontrado

Erro comum:

```text
release version 21 not supported
```

Solucao:

```text
instalar JDK 21
configurar JAVA_HOME
abrir um terminal novo
validar java -version e javac -version
```

### Jar nao encontrado no Docker build

Erro comum:

```text
target/pedido-0.0.1-SNAPSHOT.jar not found
```

Solucao:

```text
rodar .\mvnw.cmd clean package -DskipTests antes do docker build
```

### Front carrega, mas dados nao aparecem

Validar se as APIs estao no ar:

```text
http://localhost:8082/produtos
http://localhost:8083/estoque
http://localhost:8084/pedidos
```

Validar tambem se o navegador nao bloqueou chamadas por CORS e se os servicos estao nas portas esperadas.

## 12. Observacoes para avaliacao

Este projeto ainda e uma implementacao de estudo. Ele demonstra conceitos importantes:

```text
separacao de responsabilidades por microservico
comunicacao HTTP para consulta
mensageria assincrona para processamento de pedido
controle de status de pedido
uso de variaveis de ambiente
containerizacao com Docker
preparacao para deploy em AWS EC2/ECR
```

Melhorias futuras planejadas:

```text
usar Docker Compose para subir todos os servicos juntos
publicar imagens no Amazon ECR
executar containers na EC2
usar gateway/API gateway
estudar Kubernetes
adicionar observabilidade e logs centralizados
```
