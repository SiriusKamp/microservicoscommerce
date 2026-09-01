# Cloud Commerce

Guia rapido para executar localmente o projeto Cloud Commerce.

## Servicos

| Servico | Porta |
| --- | ---: |
| `cloud-commerce` | 8081 |
| `produto` | 8082 |
| `estoque` | 8083 |
| `pedido` | 8084 |
| `rabbitmq` | 5672 / 15672 |

## Como rodar

### 1. Pre-requisitos

```text
Java 21
Docker Desktop
Git
```

Validar:

```powershell
java -version
javac -version
docker info
```

### 2. Baixar o projeto

```powershell
git clone URL_DO_REPOSITORIO
Set-Location "C:\CAMINHO\PARA\microservicoscommerce"
```

### 3. Configurar arquivos .env

Os arquivos `.env` serao enviados junto com a atividade.

Eles devem ser colocados nas pastas correspondentes com o nome `.env`:

```text
produto/.env
estoque/.env
pedido/.env
```

Os arquivos `.env.example` permanecem no repositorio apenas como modelo.

### 4. Subir RabbitMQ com Docker

Antes de rodar, abrir o Docker Desktop.

Opcao A: usando Docker Compose na raiz do projeto:

```powershell
Set-Location "C:\CAMINHO\PARA\microservicoscommerce"
docker compose up -d rabbitmq
```

Opcao B: usando comando direto:

```powershell
docker run -d `
  --name commerce-rabbitmq `
  -p 5672:5672 `
  -p 15672:15672 `
  -e RABBITMQ_DEFAULT_USER=guest `
  -e RABBITMQ_DEFAULT_PASS=guest `
  rabbitmq:3.13-management
```

Painel:

```text
http://localhost:15672
usuario: guest
senha: guest
```

### 5. Rodar servicos

Abrir um PowerShell para cada servico.

Produto:

```powershell
Set-Location "C:\CAMINHO\PARA\microservicoscommerce\produto"
.\mvnw.cmd spring-boot:run
```

Estoque:

```powershell
Set-Location "C:\CAMINHO\PARA\microservicoscommerce\estoque"
.\mvnw.cmd spring-boot:run
```

Pedido:

```powershell
Set-Location "C:\CAMINHO\PARA\microservicoscommerce\pedido"
.\mvnw.cmd spring-boot:run
```

Front:

```powershell
Set-Location "C:\CAMINHO\PARA\microservicoscommerce\cloud-commerce"
.\mvnw.cmd spring-boot:run
```

Acessar:

```text
http://localhost:8081
```

## Ordem recomendada

```text
1. Abrir Docker Desktop
2. Subir RabbitMQ
3. Rodar produto
4. Rodar estoque
5. Rodar pedido
6. Rodar cloud-commerce
```

## Documentacao

Guias principais:

- [README.md](README.md): execucao local e microservicos.
- [README-AWS-KUBERNETES.md](README-AWS-KUBERNETES.md): Docker, ECR, EKS e Kubernetes.

Documento principal:

```text
docs/01-documentacao-arquitetural.docx
```

Documento complementar AWS:

```text
docs/02-aws-ecr-ec2-deploy.md
```
