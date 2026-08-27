# AWS, ECR e EC2 - Documentacao do fluxo de deploy

Este documento registra o caminho usado para levar o projeto Cloud Commerce do ambiente local para uma infraestrutura inicial na AWS usando EC2, Docker e Amazon ECR.

O objetivo atual nao e ter uma arquitetura final de producao, mas sim criar uma base pratica para estudo:

```text
codigo local
commit no Git
build Maven
imagem Docker local
push para Amazon ECR
pull na EC2
docker run na EC2
```

## 1. Conceitos principais

| Conceito | Papel no fluxo |
| --- | --- |
| Git | Versiona codigo, Dockerfile, documentacao e arquivos `.env.example` |
| Maven | Gera o `.jar` da aplicacao Java |
| Docker build | Cria imagem local a partir do `.jar` |
| Docker tag | Da a imagem local um nome de repositorio remoto |
| Amazon ECR | Guarda imagens Docker na AWS |
| Docker push | Envia imagem local para o ECR |
| EC2 | Servidor Linux onde os containers rodam |
| Docker pull | Baixa a imagem do ECR na EC2 |
| Docker run | Cria um container na EC2 |
| IAM User | Permite que a maquina local publique imagens |
| IAM Role da EC2 | Permite que a instancia baixe imagens sem guardar chave fixa |

## 2. Criacao da conta AWS

Primeiro passo:

```text
criar conta AWS
configurar acesso ao Console AWS
escolher a regiao de trabalho
```

Regiao usada no estudo:

```text
sa-east-1
```

Essa regiao representa Sao Paulo.

## 3. Criacao do IAM User para uso local

Para usar a AWS CLI no computador local, foi necessario configurar uma identidade IAM.

Fluxo:

```text
IAM
Users
Create user
criar ou selecionar usuario
Security credentials
Create access key
uso: Command Line Interface
```

Depois, no PowerShell:

```powershell
aws configure
```

Valores informados:

```text
AWS Access Key ID
AWS Secret Access Key
Default region name: sa-east-1
Default output format: json
```

Validacao:

```powershell
aws sts get-caller-identity
```

Importante:

```text
Access key e secret key nao devem ir para Git, documento publico, imagem Docker ou e-mail.
```

## 4. Permissoes do IAM User para publicar no ECR

O usuario IAM local precisa publicar imagens no ECR.

Policy adicionada:

```text
AmazonEC2ContainerRegistryPowerUser
```

Tambem foi necessario permitir a criacao de repositorios:

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

Nome sugerido para a policy:

```text
AllowEcrCreateRepositoryCommerceStudy
```

Sem essas permissoes, erros comuns sao:

```text
ecr:DescribeRepositories nao autorizado
ecr:CreateRepository nao autorizado
ecr:InitiateLayerUpload nao autorizado
```

## 5. Criacao da instancia EC2

Configuracao usada no estudo:

```text
Servico: Amazon EC2
Sistema: Amazon Linux 2023
Armazenamento: 20 GB
Acesso: SSH com chave .pem
Docker instalado na instancia
```

Ao criar a instancia, foi necessario criar ou selecionar um Key Pair.

O arquivo `.pem` deve ser guardado localmente, por exemplo:

```text
C:\Users\USUARIO\Downloads\cloud-commerce-key.pem
```

Atencao:

```text
A AWS so permite baixar a chave privada .pem no momento de criacao do Key Pair.
Se a chave for perdida, ela nao pode ser baixada novamente.
```

## 6. Regras de Security Group

O Security Group controla quais portas podem receber acesso externo.

Para SSH:

```text
Type: SSH
Protocol: TCP
Port: 22
Source: SEU_IP_PUBLICO/32
```

O IP publico local pode ser obtido com:

```powershell
(Invoke-RestMethod https://checkip.amazonaws.com).Trim()
```

Para teste das aplicacoes, liberar as portas finais:

```text
8081 -> front
8082 -> produto
8083 -> estoque
8084 -> pedido
```

Em ambiente de estudo, pode ser usado:

```text
0.0.0.0/0
```

Observacao:

```text
0.0.0.0/0 significa acesso vindo de qualquer IP.
Para estudo e aceitavel com cuidado, mas em producao o ideal e restringir acesso e usar load balancer/gateway.
```

RabbitMQ:

```text
5672  -> porta de mensageria usada pelas aplicacoes
15672 -> painel web administrativo
```

O painel `15672` nao deve ficar aberto ao mundo em ambiente real.

## 7. Acesso SSH na EC2

Formato correto:

```powershell
ssh -i "C:\CAMINHO\DA\CHAVE.pem" ec2-user@IP_PUBLICO_DA_EC2
```

Exemplo:

```powershell
ssh -i "C:\Users\sirius.alves\Downloads\cloud-commerce-key.pem" ec2-user@13.220.41.245
```

Na primeira conexao, o SSH pergunta se deve confiar no host:

```text
Are you sure you want to continue connecting (yes/no/[fingerprint])?
```

Responder:

```text
yes
```

Erro comum:

```text
Could not resolve hostname ec2-13.220.41.245
```

Causa:

```text
foi usado ec2-13.220.41.245 como hostname
```

Correto:

```text
ec2-user@13.220.41.245
```

## 8. IAM Role da EC2

A EC2 deve acessar o ECR sem usar access key fixa.

Para isso, criar ou associar uma IAM Role a instancia.

Role usada no estudo:

```text
Cloud-Commerce-Container
```

Permissao necessaria para baixar imagens:

```text
AmazonEC2ContainerRegistryReadOnly
```

Validacao dentro da EC2:

```bash
aws sts get-caller-identity
```

Resultado esperado:

```text
arn:aws:sts::<ACCOUNT_ID>:assumed-role/NOME_DA_ROLE/i-...
```

Isso mostra que a EC2 esta usando role, nao chave manual.

## 9. Criacao dos repositorios ECR

Repositorios criados:

```text
microservicoscommerce-cloud-commerce
microservicoscommerce-produto
microservicoscommerce-estoque
microservicoscommerce-pedido
```

Comandos:

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

## 10. Login do Docker no ECR

No computador local:

```powershell
aws ecr get-login-password --region sa-east-1 | docker login --username AWS --password-stdin <ACCOUNT_ID>.dkr.ecr.sa-east-1.amazonaws.com
```

Na EC2:

```bash
aws ecr get-login-password --region sa-east-1 | docker login --username AWS --password-stdin <ACCOUNT_ID>.dkr.ecr.sa-east-1.amazonaws.com
```

Resultado esperado:

```text
Login Succeeded
```

## 11. Fluxo codigo, commit, build e imagem

Sequencia conceitual:

```text
alterar codigo
validar localmente
commitar no Git
gerar jar
criar imagem Docker local
taguear imagem com versao
enviar imagem ao ECR
baixar imagem na EC2
recriar container
```

O commit sozinho nao envia imagem ao ECR.

Para envio automatico seria necessario configurar CI/CD:

```text
GitHub Actions
GitLab CI
Jenkins
```

No estado atual, o fluxo e manual/semi-manual para fins de aprendizado.

## 12. Build Maven

Exemplo com `pedido`:

```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"

Set-Location "C:\Users\sirius.alves\Projetos\microservicoscommerce\pedido"
.\mvnw.cmd clean package -DskipTests
```

O Maven gera:

```text
target/pedido-0.0.1-SNAPSHOT.jar
```

## 13. Docker build local

Ainda no diretorio do servico:

```powershell
docker build -t microservicoscommerce-pedido .
```

Esse comando cria uma imagem local.

Validacao:

```powershell
docker images microservicoscommerce-pedido
```

## 14. Docker tag com versao

Como ja havia imagem `1.0` na EC2, a nova versao foi publicada como `2.0`.

Comando:

```powershell
docker tag microservicoscommerce-pedido:latest <ACCOUNT_ID>.dkr.ecr.sa-east-1.amazonaws.com/microservicoscommerce-pedido:2.0
```

O nome completo informa ao Docker:

```text
registry ECR
repositorio
tag da imagem
```

Formato:

```text
<ACCOUNT_ID>.dkr.ecr.<REGIAO>.amazonaws.com/<REPOSITORIO>:<TAG>
```

## 15. Docker push para ECR

```powershell
docker push <ACCOUNT_ID>.dkr.ecr.sa-east-1.amazonaws.com/microservicoscommerce-pedido:2.0
```

Validacao:

```powershell
aws ecr describe-images `
  --repository-name microservicoscommerce-pedido `
  --region sa-east-1 `
  --query "imageDetails[*].imageTags" `
  --output table
```

Resultado esperado:

```text
2.0
```

## 16. Docker pull na EC2

Dentro da EC2:

```bash
docker pull <ACCOUNT_ID>.dkr.ecr.sa-east-1.amazonaws.com/microservicoscommerce-pedido:2.0
```

Validar:

```bash
docker images
```

## 17. Criar env na EC2

A imagem Docker nao contem as credenciais do banco.

Na EC2, criar um arquivo de ambiente:

```bash
nano pedido.env
```

Exemplo sem valores reais:

```text
DB_URL=jdbc:postgresql://HOST:5432/NOME_DO_BANCO?sslmode=require
DB_USERNAME=USUARIO_DO_BANCO
DB_PASSWORD=SENHA_DO_BANCO
RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=guest
RABBITMQ_PASSWORD=guest
```

Arquivos `.env` ou `.env` equivalentes devem ficar na EC2, nao no ECR e nao no Git.

## 18. Docker run na EC2

Rodar container `pedido`:

```bash
docker run -d \
  --name pedido-service \
  -p 8084:8084 \
  --env-file /home/ec2-user/pedido.env \
  <ACCOUNT_ID>.dkr.ecr.sa-east-1.amazonaws.com/microservicoscommerce-pedido:2.0
```

Validar:

```bash
docker ps
docker logs pedido-service
```

Se o container cair, verificar logs:

```bash
docker logs pedido-service
```

## 19. Parar, iniciar e remover containers

Parar:

```bash
docker stop pedido-service
```

Iniciar container existente:

```bash
docker start pedido-service
```

Reiniciar:

```bash
docker restart pedido-service
```

Remover container parado:

```bash
docker rm pedido-service
```

Importante:

```text
docker stop usa o nome do container
docker rmi usa o nome da imagem
```

## 20. Atualizar container para uma nova versao

Exemplo usando tag `3.0`:

No local:

```powershell
Set-Location "C:\Users\sirius.alves\Projetos\microservicoscommerce\pedido"
.\mvnw.cmd clean package -DskipTests
docker build -t microservicoscommerce-pedido .
docker tag microservicoscommerce-pedido:latest <ACCOUNT_ID>.dkr.ecr.sa-east-1.amazonaws.com/microservicoscommerce-pedido:3.0
docker push <ACCOUNT_ID>.dkr.ecr.sa-east-1.amazonaws.com/microservicoscommerce-pedido:3.0
```

Na EC2:

```bash
docker pull <ACCOUNT_ID>.dkr.ecr.sa-east-1.amazonaws.com/microservicoscommerce-pedido:3.0
docker stop pedido-service
docker rm pedido-service
docker run -d --name pedido-service -p 8084:8084 --env-file /home/ec2-user/pedido.env <ACCOUNT_ID>.dkr.ecr.sa-east-1.amazonaws.com/microservicoscommerce-pedido:3.0
```

## 21. Higienizacao de containers e imagens

Ver containers:

```bash
docker ps -a
```

Ver imagens:

```bash
docker images
```

Remover containers parados antigos:

```bash
docker rm NOME_DO_CONTAINER
```

Remover imagens antigas locais:

```bash
docker rmi NOME_DA_IMAGEM:TAG
```

Exemplo:

```bash
docker rmi cloudcommerce-pedido:1.0
```

Isso remove a imagem local da EC2, nao remove a imagem do ECR.

## 22. O que foi feito ate agora

Estado atingido:

```text
AWS CLI funcionando localmente
usuario IAM com permissao ECR ajustada
repositorios ECR criados
Docker local autenticado no ECR
imagem pedido:2.0 criada localmente
imagem pedido:2.0 enviada ao ECR
EC2 acessada via SSH
EC2 autenticada no ECR usando IAM Role
imagem pedido:2.0 baixada na EC2
container pedido-service executado na porta 8084
```

Ponto de atencao atual:

```text
containers precisam receber --env-file na hora do docker run
```

## 23. O que ainda falta para enriquecer o trabalho

Proximas melhorias:

```text
criar envs finais na EC2 para pedido, produto e estoque
publicar imagens 2.0 dos demais servicos
subir todos os containers na EC2
criar script de deploy por servico
criar Docker Compose da EC2 com os quatro servicos
automatizar build/push com GitHub Actions ou GitLab CI
automatizar deploy com script, webhook, ECS ou Kubernetes
adicionar gateway de entrada
adicionar logs e observabilidade
```

## 24. Resumo para apresentacao

Resumo falado:

```text
O projeto saiu do ambiente local e comecou a ser preparado para deploy em nuvem.
Foi criada uma estrutura com ECR para armazenar imagens Docker e uma EC2 para executar containers.
O primeiro deploy manual validado foi do servico pedido na versao 2.0.
A imagem foi gerada localmente, enviada ao ECR, baixada na EC2 e executada como container.
As credenciais do banco nao foram colocadas na imagem; elas devem ser fornecidas por variaveis de ambiente na hora do docker run.
```
