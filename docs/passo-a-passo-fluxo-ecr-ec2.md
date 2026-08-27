# Passo a passo - Fluxo Docker, ECR e EC2

Este documento registra o que esta sendo feito para transformar o projeto local em um fluxo funcional de publicacao em nuvem.

A ideia principal e sair deste ciclo:

```text
codigo rodando somente no computador local
```

para este ciclo:

```text
alterar codigo
gerar jar
criar imagem Docker local
enviar imagem para Amazon ECR
entrar na EC2
baixar imagem atualizada
substituir container antigo pelo novo
```

## 1. Estado atual

O projeto possui quatro aplicacoes Spring Boot:

```text
cloud-commerce -> front web, porta 8081
produto        -> API de produtos, porta 8082
estoque        -> API e consumidor RabbitMQ de estoque, porta 8083
pedido         -> API e mensageria de pedidos, porta 8084
```

Tambem existe um RabbitMQ local via Docker Compose:

```text
rabbitmq -> portas 5672 e 15672
```

O banco de dados deixou de ficar fixo no `application.properties` e passou a ser lido por variaveis de ambiente:

```text
DB_URL
DB_USERNAME
DB_PASSWORD
```

Nos servicos que usam RabbitMQ tambem existem:

```text
RABBITMQ_HOST
RABBITMQ_PORT
RABBITMQ_USERNAME
RABBITMQ_PASSWORD
```

## 2. O que significa o docker build

Quando rodamos:

```powershell
docker build -t microservicoscommerce-pedido .
```

o Docker cria uma imagem local no computador.

Essa imagem ainda nao esta na AWS. Ela fica apenas no Docker da maquina onde o comando foi executado.

Para conferir imagens locais:

```powershell
docker images
```

ou especificamente:

```powershell
docker images microservicoscommerce-pedido
```

Resumo:

```text
docker build -> cria imagem local
docker tag   -> cria um nome/endereco para enviar ao ECR
docker push  -> envia a imagem para o ECR
docker pull  -> baixa a imagem em outra maquina, como a EC2
docker run   -> cria/roda um container a partir da imagem
```

## 3. Preparacao feita no Windows

Para o Docker Desktop funcionar no Windows, foi necessario habilitar recursos de virtualizacao.

Comandos usados no PowerShell como administrador:

```powershell
dism.exe /online /enable-feature /featurename:Microsoft-Windows-Subsystem-Linux /all /norestart
dism.exe /online /enable-feature /featurename:VirtualMachinePlatform /all /norestart
dism.exe /online /enable-feature /featurename:HypervisorPlatform /all /norestart
bcdedit /set hypervisorlaunchtype auto
```

Depois disso, o computador precisou ser reiniciado e o Docker Desktop aberto novamente.

## 4. Preparacao da AWS CLI

Primeiro foi validado se a AWS CLI estava instalada:

```powershell
aws --version
```

Depois a CLI foi configurada:

```powershell
aws configure
```

Valores importantes:

```text
AWS Access Key ID     -> chave publica IAM
AWS Secret Access Key -> chave secreta IAM, nao deve ser versionada
Default region name   -> sa-east-1
Default output format -> json
```

Para confirmar qual identidade a CLI esta usando:

```powershell
aws sts get-caller-identity
```

Esse comando e importante porque a permissao de ECR precisa estar na mesma identidade usada pela CLI.

## 5. Permissoes IAM usadas

Foi necessario liberar permissoes para o usuario IAM usado pela AWS CLI.

Primeira permissao:

```text
AmazonEC2ContainerRegistryPowerUser
```

Ela permite autenticar no ECR, listar repositorios existentes e enviar imagens para repositorios ja criados.

Depois apareceu falta da acao:

```text
ecr:CreateRepository
```

Foi criada uma inline policy com:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "ecr:CreateRepository"
      ],
      "Resource": "*"
    }
  ]
}
```

Nome sugerido:

```text
AllowEcrCreateRepositoryCommerceStudy
```

## 6. Repositorios ECR

Foram criados repositorios para cada imagem do projeto:

```text
microservicoscommerce-cloud-commerce
microservicoscommerce-produto
microservicoscommerce-estoque
microservicoscommerce-pedido
```

Comandos usados:

```powershell
aws ecr create-repository --repository-name microservicoscommerce-cloud-commerce --region sa-east-1
aws ecr create-repository --repository-name microservicoscommerce-produto --region sa-east-1
aws ecr create-repository --repository-name microservicoscommerce-estoque --region sa-east-1
aws ecr create-repository --repository-name microservicoscommerce-pedido --region sa-east-1
```

Validacao:

```powershell
aws ecr describe-repositories --region sa-east-1 --query "repositories[*].repositoryName" --output table
```

## 7. Login do Docker no ECR

Antes de enviar imagens, o Docker local precisa autenticar no registry ECR.

Comando usado:

```powershell
aws ecr get-login-password --region sa-east-1 | docker login --username AWS --password-stdin 382597877252.dkr.ecr.sa-east-1.amazonaws.com
```

Resultado esperado:

```text
Login Succeeded
```

Essa autenticacao nao envia nenhuma imagem ainda. Ela apenas permite que o Docker local tenha permissao temporaria para fazer `push` no ECR.

## 8. Build Maven antes do Docker

Os `Dockerfile` atuais esperam que o `.jar` ja exista dentro da pasta `target`.

Exemplo do servico `pedido`:

```dockerfile
COPY target/pedido-0.0.1-SNAPSHOT.jar app.jar
```

Por isso, antes do `docker build`, e necessario gerar o `.jar` com Maven.

No Windows, caso o Java 21 ainda nao esteja como padrao no terminal:

```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
```

Depois:

```powershell
Set-Location "C:\Users\sirius.alves\Projetos\microservicoscommerce\pedido"
.\mvnw.cmd clean package -DskipTests
```

Esse comando gera:

```text
pedido/target/pedido-0.0.1-SNAPSHOT.jar
```

## 9. Build da imagem local

Depois do `.jar` existir:

```powershell
Set-Location "C:\Users\sirius.alves\Projetos\microservicoscommerce\pedido"
docker build -t microservicoscommerce-pedido .
```

Esse passo cria a imagem local:

```text
microservicoscommerce-pedido
```

Ela ainda nao esta no ECR.

## 10. Proximo passo: tag da imagem

Para enviar ao ECR, a imagem local precisa ganhar o nome completo do repositorio remoto.

Comando para o servico `pedido`:

```powershell
docker tag microservicoscommerce-pedido:latest 382597877252.dkr.ecr.sa-east-1.amazonaws.com/microservicoscommerce-pedido:latest
```

O que esse comando faz:

```text
microservicoscommerce-pedido:latest
```

passa a ter tambem o nome:

```text
382597877252.dkr.ecr.sa-east-1.amazonaws.com/microservicoscommerce-pedido:latest
```

A imagem nao foi duplicada de verdade no disco. O Docker criou uma referencia nova para a mesma imagem.

## 11. Proximo passo: push para ECR

Depois da tag:

```powershell
docker push 382597877252.dkr.ecr.sa-east-1.amazonaws.com/microservicoscommerce-pedido:latest
```

Agora a imagem sai do computador local e vai para o Amazon ECR.

Validacao:

```powershell
aws ecr describe-images --repository-name microservicoscommerce-pedido --region sa-east-1
```

## 12. Role da EC2 para baixar imagem

O usuario IAM local faz `push`.

A EC2 precisa fazer `pull`.

Para isso, a instancia EC2 deve ter uma IAM Role com permissao:

```text
AmazonEC2ContainerRegistryReadOnly
```

Conceito:

```text
PC local com AWS CLI -> envia imagem para ECR
EC2 com IAM Role     -> baixa imagem do ECR
```

Isso evita colocar access key e secret key dentro da EC2.

## 13. Fluxo esperado dentro da EC2

Na EC2, autenticar Docker no ECR:

```bash
aws ecr get-login-password --region sa-east-1 | docker login --username AWS --password-stdin 382597877252.dkr.ecr.sa-east-1.amazonaws.com
```

Baixar imagem:

```bash
docker pull 382597877252.dkr.ecr.sa-east-1.amazonaws.com/microservicoscommerce-pedido:latest
```

Parar container antigo, se existir:

```bash
docker stop pedido
docker rm pedido
```

Rodar novo container:

```bash
docker run -d \
  --name pedido \
  -p 8084:8084 \
  --env-file ./pedido.env \
  382597877252.dkr.ecr.sa-east-1.amazonaws.com/microservicoscommerce-pedido:latest
```

O arquivo `pedido.env` na EC2 deve conter as variaveis de ambiente reais do servico, sem ser enviado ao Git.

## 14. Ciclo completo de atualizacao

Quando o codigo mudar:

```powershell
Set-Location "C:\Users\sirius.alves\Projetos\microservicoscommerce\pedido"
.\mvnw.cmd clean package -DskipTests
docker build -t microservicoscommerce-pedido .
docker tag microservicoscommerce-pedido:latest 382597877252.dkr.ecr.sa-east-1.amazonaws.com/microservicoscommerce-pedido:latest
docker push 382597877252.dkr.ecr.sa-east-1.amazonaws.com/microservicoscommerce-pedido:latest
```

Depois, na EC2:

```bash
docker pull 382597877252.dkr.ecr.sa-east-1.amazonaws.com/microservicoscommerce-pedido:latest
docker stop pedido
docker rm pedido
docker run -d --name pedido -p 8084:8084 --env-file ./pedido.env 382597877252.dkr.ecr.sa-east-1.amazonaws.com/microservicoscommerce-pedido:latest
```

## 15. Aprendizado principal

O ponto mais importante desta etapa e separar as responsabilidades:

```text
Maven -> transforma codigo Java em jar
Docker build -> transforma jar em imagem local
ECR -> armazena imagens Docker na AWS
EC2 -> executa containers a partir dessas imagens
IAM User -> permite o push feito pelo computador local
IAM Role da EC2 -> permite o pull feito pela instancia
```

Assim, o projeto passa a ter um caminho claro de entrega:

```text
codigo -> jar -> imagem local -> imagem no ECR -> container na EC2
```
