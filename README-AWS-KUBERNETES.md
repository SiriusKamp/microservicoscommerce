# Guia AWS, Docker, ECR e Kubernetes

Este guia documenta o passo a passo usado para levar o front `cloud-commerce` para a AWS usando Docker, Amazon ECR e Amazon EKS.

O foco do trabalho e demonstrar o fluxo:

```text
Aplicacao -> Docker -> Registro de imagens -> Amazon EKS -> Deployment -> Service -> Acesso externo
```

## 1. Visao geral do fluxo

```text
1. Criar usuario IAM na AWS
2. Configurar permissoes para ECR, EKS, EC2, CloudFormation e IAM
3. Instalar AWS CLI, kubectl e eksctl no Windows
4. Autenticar a AWS CLI com o usuario IAM
5. Criar Dockerfile na aplicacao
6. Gerar o .jar com Maven
7. Criar imagem Docker local
8. Publicar imagem no Amazon ECR
9. Criar cluster Amazon EKS
10. Aplicar manifestos Kubernetes
11. Acessar a aplicacao pelo LoadBalancer
12. Testar recuperacao automatica de Pod
13. Remover recursos para evitar custos
```

## 2. IAM na AWS

Entre no Console AWS e acesse:

```text
IAM -> Users -> Create user
```

Crie ou selecione o usuario usado pela AWS CLI. Neste projeto, o usuario usado foi:

```text
Sirius
```

Permissoes usadas no laboratorio:

```text
AmazonEC2ContainerRegistryPowerUser
AmazonEC2FullAccess
AWSCloudFormationFullAccess
IAMFullAccess
Permissao EKS, como eks:*
```

Observacao:

```text
Essas permissoes sao amplas e foram usadas para estudo.
```

## 3. Instalar AWS CLI no Windows

Baixar o instalador MSI oficial:

```powershell
Set-Location $HOME\Downloads
Invoke-WebRequest -Uri "https://awscli.amazonaws.com/AWSCLIV2.msi" -OutFile "AWSCLIV2.msi"
```

Instalar:

```powershell
Start-Process msiexec.exe -Wait -ArgumentList "/i AWSCLIV2.msi"
```

Feche e abra o PowerShell novamente.

Validar:

```powershell
aws --version
```

Se `aws` nao for reconhecido, adicionar o caminho ao PATH do usuario:

```powershell
$AwsCliPath = "C:\Program Files\Amazon\AWSCLIV2"
$UserPath = [Environment]::GetEnvironmentVariable("Path", "User")
[Environment]::SetEnvironmentVariable("Path", "$UserPath;$AwsCliPath", "User")
$env:Path = "$env:Path;$AwsCliPath"
```

Validar novamente:

```powershell
aws --version
```

## 4. Configurar login da AWS CLI

No IAM, crie uma access key para o usuario:

```text
IAM -> Users -> Sirius -> Security credentials -> Create access key
```

Uso escolhido:

```text
Command Line Interface
```

No PowerShell:

```powershell
aws configure
```

Preencha:

```text
AWS Access Key ID: <sua-access-key>
AWS Secret Access Key: <sua-secret-key>
Default region name: sa-east-1
Default output format: json
```

Validar quem esta logado:

```powershell
aws sts get-caller-identity
```

Resultado esperado:

```json
{
  "Account": "382597877252",
  "Arn": "arn:aws:iam::382597877252:user/Sirius"
}
```

Validar regiao:

```powershell
aws configure get region
```

## 5. Instalar kubectl

O `kubectl` e a ferramenta que conversa com o Kubernetes.

Validar:

```powershell
kubectl version --client
```

Neste projeto, a versao local validada foi:

```text
Client Version: v1.36.1
```

## 6. Instalar eksctl no Windows

O `eksctl` facilita a criacao e administracao de clusters EKS.

Baixar:

```powershell
Set-Location $HOME\Downloads
curl.exe -L -o eksctl_Windows_amd64.zip https://github.com/eksctl-io/eksctl/releases/latest/download/eksctl_Windows_amd64.zip
```

Criar pasta:

```powershell
New-Item -ItemType Directory -Force -Path "$HOME\eksctl"
```

Extrair:

```powershell
Expand-Archive .\eksctl_Windows_amd64.zip -DestinationPath "$HOME\eksctl" -Force
```

Adicionar ao PATH:

```powershell
$UserPath = [Environment]::GetEnvironmentVariable("Path", "User")
[Environment]::SetEnvironmentVariable("Path", "$UserPath;$HOME\eksctl", "User")
$env:Path = "$env:Path;$HOME\eksctl"
```

Validar:

```powershell
eksctl version
```

Neste projeto, a versao validada foi:

```text
0.230.0
```

## 7. Criar Dockerfile no front

Arquivo:

```text
cloud-commerce/Dockerfile
```

Conteudo:

```dockerfile
FROM eclipse-temurin:21-jre

WORKDIR /app

COPY target/cloud-commerce-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
```

Explicacao:

| Linha | Funcao |
| --- | --- |
| `FROM eclipse-temurin:21-jre` | Usa uma imagem base com Java 21 |
| `WORKDIR /app` | Define a pasta de trabalho dentro do container |
| `COPY target/... app.jar` | Copia o `.jar` gerado pelo Maven para dentro da imagem |
| `EXPOSE 8081` | Documenta que a aplicacao usa a porta 8081 |
| `ENTRYPOINT ...` | Define o comando que inicia a aplicacao |

## 8. Gerar JAR e imagem Docker local

Entrar na pasta do front:

```powershell
Set-Location "C:\Users\sirius.alves\Projetos\microservicoscommerce\cloud-commerce"
```

Gerar o `.jar`:

```powershell
.\mvnw.cmd clean package -DskipTests
```

Criar imagem Docker local:

```powershell
docker build -t microservicoscommerce-cloud-commerce:v1 .
```

Validar:

```powershell
docker images microservicoscommerce-cloud-commerce:v1
```

Rodar localmente:

```powershell
docker run -d `
  --name cloud-commerce-local `
  -p 8081:8081 `
  -e AMBIENTE=demonstracao `
  -e API_KEY=valor-ficticio-nao-utilizar-em-producao `
  microservicoscommerce-cloud-commerce:v1
```

Testar:

```powershell
curl http://localhost:8081/health
curl http://localhost:8081/config
```

Remover container local:

```powershell
docker rm -f cloud-commerce-local
```

## 9. Criar repositorio no Amazon ECR

Criar repositorio:

```powershell
aws ecr create-repository `
  --repository-name microservicoscommerce-cloud-commerce `
  --region sa-east-1
```

Validar:

```powershell
aws ecr describe-repositories --region sa-east-1
```

## 10. Login, tag e push para o ECR

Bloco usado no projeto:

```powershell
$REGISTRY = "382597877252.dkr.ecr.sa-east-1.amazonaws.com"

aws ecr get-login-password --region sa-east-1 | docker login --username AWS --password-stdin $REGISTRY

docker tag microservicoscommerce-cloud-commerce:v1 $REGISTRY/microservicoscommerce-cloud-commerce:v1

docker push $REGISTRY/microservicoscommerce-cloud-commerce:v1
```

### 10.1 O que e `$REGISTRY`

```powershell
$REGISTRY = "382597877252.dkr.ecr.sa-east-1.amazonaws.com"
```

Isso cria uma variavel no PowerShell.

Ela guarda o endereco do registry ECR:

```text
<account-id>.dkr.ecr.<region>.amazonaws.com
```

No projeto:

```text
account-id: 382597877252
region: sa-east-1
registry: 382597877252.dkr.ecr.sa-east-1.amazonaws.com
```

Essa variavel evita repetir o endereco completo em todos os comandos.

### 10.2 O que faz o login no ECR

```powershell
aws ecr get-login-password --region sa-east-1 | docker login --username AWS --password-stdin $REGISTRY
```

Esse comando tem duas partes.

Primeira parte:

```powershell
aws ecr get-login-password --region sa-east-1
```

Ela pede para a AWS um token temporario de autenticacao no ECR.

Segunda parte:

```powershell
docker login --username AWS --password-stdin $REGISTRY
```

Ela faz login do Docker no registry ECR.

O simbolo `|` e um pipe:

```text
saida do comando da esquerda -> entrada do comando da direita
```

Entao o token gerado pela AWS vira a senha usada pelo Docker.

O usuario `AWS` e fixo nesse tipo de login do ECR. A permissao real vem da identidade configurada na AWS CLI.

### 10.3 O que faz o docker tag

```powershell
docker tag microservicoscommerce-cloud-commerce:v1 $REGISTRY/microservicoscommerce-cloud-commerce:v1
```

Esse comando nao cria uma nova imagem do zero.

Ele cria outro nome para a mesma imagem local.

Antes:

```text
microservicoscommerce-cloud-commerce:v1
```

Depois:

```text
382597877252.dkr.ecr.sa-east-1.amazonaws.com/microservicoscommerce-cloud-commerce:v1
```

Por que isso e necessario?

Porque o Docker so sabe para qual registry enviar a imagem se o nome dela tiver o endereco do registry.

Formato geral:

```text
<registry>/<repositorio>:<tag>
```

### 10.4 O que faz o docker push

```powershell
docker push $REGISTRY/microservicoscommerce-cloud-commerce:v1
```

Esse comando envia a imagem local para o Amazon ECR.

Ele envia:

```text
camadas da imagem
manifesto da imagem
tag v1
```

Depois disso, outras maquinas autorizadas podem baixar a imagem com:

```powershell
docker pull 382597877252.dkr.ecr.sa-east-1.amazonaws.com/microservicoscommerce-cloud-commerce:v1
```

Resumo:

```text
docker push = envia imagem para o registro
docker pull = baixa imagem do registro
```

## 11. Criar cluster EKS

Antes de criar, validar se nao existe cluster:

```powershell
eksctl get cluster --region sa-east-1
```

Criar cluster:

```powershell
eksctl create cluster `
  --name cloud-commerce-eks `
  --region sa-east-1 `
  --version 1.36 `
  --nodes 2 `
  --node-type t3.small
```

Esse comando cria:

```text
cluster EKS
VPC
subnets
security groups
node group
duas instancias EC2 usadas como nodes
configuracao do kubectl
```

Validar nodes:

```powershell
kubectl get nodes
```

Resultado obtido no projeto:

```text
NAME                                          STATUS   ROLES    AGE   VERSION
ip-192-168-49-28.sa-east-1.compute.internal   Ready    <none>   ...   v1.36.3-eks-cb19647
ip-192-168-6-125.sa-east-1.compute.internal   Ready    <none>   ...   v1.36.3-eks-cb19647
```

## 12. EC2 manual e necessaria?

Para o fluxo com EKS:

```text
Nao e necessario criar uma EC2 manual separada.
```

Motivo:

```text
No EKS, os nodes do cluster ja sao instancias EC2 criadas dentro do node group.
```

A EC2 manual usada anteriormente serviu como teste didatico:

```text
validar docker login no ECR
validar docker pull
validar docker run
validar variaveis por --env-file
```

No Kubernetes, isso muda:

```text
docker run manual -> Deployment Kubernetes
--env-file -> ConfigMap e Secret
-p 8081:8081 -> Service LoadBalancer
```

## 13. Arquivos YAML do Kubernetes

Os manifestos ficam em:

```text
k8s/
```

### 13.1 configmap.yaml

Arquivo:

```text
k8s/configmap.yaml
```

Funcao:

```text
guardar configuracoes nao sensiveis
```

No projeto:

```yaml
AMBIENTE: demonstracao
```

### 13.2 secret.yaml

Arquivo:

```text
k8s/secret.yaml
```

Funcao:

```text
guardar valores sensiveis ou simulados
```

No projeto:

```yaml
API_KEY: valor-ficticio-nao-utilizar-em-producao
```

O endpoint `/config` nao mostra a secret. Ele mostra apenas:

```json
{"apiKeyConfigured":true}
```

### 13.3 deployment.yaml

Arquivo:

```text
k8s/deployment.yaml
```

Funcao:

```text
dizer ao Kubernetes como rodar a aplicacao
```

Define:

```text
nome da aplicacao
imagem Docker no ECR
porta do container
quantidade de replicas
variaveis vindas do ConfigMap e Secret
health checks
```

Campo mais importante para resiliencia:

```yaml
replicas: 2
```

Isso significa:

```text
Kubernetes, mantenha sempre 2 Pods rodando.
```

### 13.4 service.yaml

Arquivo:

```text
k8s/service.yaml
```

Funcao:

```text
expor a aplicacao para acesso externo
```

Tipo usado:

```yaml
type: LoadBalancer
```

Fluxo:

```text
Usuario -> Load Balancer AWS -> Service Kubernetes -> Pods -> Container cloud-commerce
```

## 14. Aplicar YAMLs no cluster

Entrar na raiz do projeto:

```powershell
Set-Location "C:\Users\sirius.alves\Projetos\microservicoscommerce"
```

Aplicar:

```powershell
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/secret.yaml
kubectl apply -f k8s/deployment.yaml
kubectl apply -f k8s/service.yaml
```

Validar:

```powershell
kubectl get deployments,pods,services
```

Resultado obtido no projeto:

```text
deployment.apps/cloud-commerce   2/2
pod/cloud-commerce-...           1/1 Running
pod/cloud-commerce-...           1/1 Running
service/cloud-commerce           LoadBalancer
```

## 15. Acessar aplicacao pelo LoadBalancer

Pegar o DNS externo:

```powershell
kubectl get service cloud-commerce
```

DNS gerado no estudo:

```text
a661234b8dea945e899b78502140bd51-629245550.sa-east-1.elb.amazonaws.com
```

Testar health:

```powershell
curl http://a661234b8dea945e899b78502140bd51-629245550.sa-east-1.elb.amazonaws.com/health
```

Resultado:

```json
{"application":"cloud-commerce","version":"v1","status":"ok"}
```

Testar config:

```powershell
curl http://a661234b8dea945e899b78502140bd51-629245550.sa-east-1.elb.amazonaws.com/config
```

Resultado:

```json
{"ambiente":"demonstracao","apiKeyConfigured":true}
```

Abrir front:

```text
http://a661234b8dea945e899b78502140bd51-629245550.sa-east-1.elb.amazonaws.com/
```

## 16. Testar recuperacao automatica

Listar Pods:

```powershell
kubectl get pods
```

Deletar um Pod:

```powershell
kubectl delete pod cloud-commerce-7554bcdcd9-4zdbv
```

Validar recriacao:

```powershell
kubectl get deployments,pods
```

Resultado esperado:

```text
deployment.apps/cloud-commerce   2/2
pod antigo removido
pod novo criado
```

Explicacao:

```text
O Deployment declarou replicas: 2.
Quando um Pod foi apagado, o Kubernetes percebeu que havia apenas 1 replica disponivel.
Entao criou um novo Pod automaticamente para voltar ao estado desejado.
```

## 17. Limpeza para evitar custo

Apagar recursos Kubernetes:

```powershell
kubectl delete -f k8s/service.yaml
kubectl delete -f k8s/deployment.yaml
kubectl delete -f k8s/configmap.yaml
kubectl delete -f k8s/secret.yaml
```

Apagar cluster:

```powershell
eksctl delete cluster --name cloud-commerce-eks --region sa-east-1
```

Validar:

```powershell
eksctl get cluster --region sa-east-1
```

Resultado esperado:

```text
No clusters found
```

## 18. Resumo final para apresentacao

```text
O projeto Cloud Commerce foi conteinerizado com Docker e publicado no Amazon ECR.
Depois, a imagem do front foi usada em um cluster Amazon EKS.
O Kubernetes criou dois Pods da aplicacao por meio de um Deployment.
As variaveis foram injetadas com ConfigMap e Secret.
O acesso externo foi feito por um Service do tipo LoadBalancer.
Ao deletar manualmente um Pod, o Kubernetes criou outro automaticamente, demonstrando recuperacao e manutencao do estado desejado.
```

## 19. Referencias

- AWS CLI User Guide: https://docs.aws.amazon.com/cli/latest/userguide/getting-started-install.html
- AWS CLI Configuration: https://docs.aws.amazon.com/cli/latest/userguide/cli-configure-files.html
- Amazon ECR push: https://docs.aws.amazon.com/AmazonECR/latest/userguide/docker-push-ecr-image.html
- Amazon EKS com eksctl: https://docs.aws.amazon.com/eks/latest/userguide/getting-started-eksctl.html
- Kubernetes Deployments: https://kubernetes.io/docs/concepts/workloads/controllers/deployment/
- Kubernetes Services: https://kubernetes.io/docs/concepts/services-networking/service/
