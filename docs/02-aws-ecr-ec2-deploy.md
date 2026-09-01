# AWS, ECR, EC2 e Kubernetes - Documentacao do trabalho

Este documento registra a etapa de nuvem do projeto Cloud Commerce, com foco no trabalho de AWS e Kubernetes.

O trabalho recebido pede demonstrar o fluxo:

```text
Aplicacao -> Docker -> Registro de imagens -> Amazon EKS -> Deployment -> Service -> Acesso externo
```

Neste projeto, a aplicacao escolhida para o Kubernetes sera o front `cloud-commerce`.

Motivo da escolha:

```text
responde HTTP na porta 8081
possui interface web
ja possui Dockerfile
nao precisa subir banco, RabbitMQ e todos os microservicos para cumprir o desafio
permite demonstrar ConfigMap, Secret, Deployment, Service, replicas e acesso externo
```

## 1. Estado atual

O projeto ja possui:

| Item | Estado |
| --- | --- |
| Front `cloud-commerce` | Implementado |
| Dockerfile do front | Implementado |
| Endpoint `/health` | Implementado e testado |
| Endpoint `/config` | Implementado e testado |
| Imagem Docker local `microservicoscommerce-cloud-commerce:v1` | Criada |
| Repositorio ECR `microservicoscommerce-cloud-commerce` | Criado |
| Imagem `v1` enviada ao ECR | Validada pelo pull na EC2 |
| EC2 com Docker | Criada e testada |
| Container do front na EC2 | Rodando na porta 8081 |
| Manifestos Kubernetes em `k8s/` | Criados |
| Cluster EKS | Pendente |
| Aplicacao no EKS | Pendente |
| Evidencias finais do Kubernetes | Pendentes |

## 2. Tecnologias usadas

| Tecnologia | Uso no projeto |
| --- | --- |
| Java 21 | Runtime da aplicacao Spring Boot |
| Spring Boot 4.1.0 | Front e APIs do projeto |
| Thymeleaf | Renderizacao das telas do front |
| Maven | Build do `.jar` |
| Docker | Criacao e execucao da imagem |
| Amazon ECR | Registro remoto de imagens Docker |
| Amazon EC2 | Validacao inicial da imagem em maquina Linux |
| Amazon EKS | Ambiente Kubernetes a ser usado na proxima etapa |
| Kubernetes | Orquestracao de Pods, Deployment, Service, ConfigMap e Secret |
| kubectl | CLI para controlar o cluster Kubernetes |
| eksctl | CLI sugerida para criar o cluster EKS |

## 3. Endpoints criados para a etapa Kubernetes

No front `cloud-commerce`, foram criados endpoints simples para facilitar a demonstracao do trabalho.

| Metodo | Endpoint | Objetivo |
| --- | --- | --- |
| GET | `/health` | Mostrar que a aplicacao esta no ar |
| GET | `/config` | Mostrar que ConfigMap e Secret foram injetados |

Resposta esperada de `/health`:

```json
{
  "status": "ok",
  "version": "v1",
  "application": "cloud-commerce"
}
```

Resposta esperada de `/config`:

```json
{
  "apiKeyConfigured": true,
  "ambiente": "demonstracao"
}
```

Observacao importante:

```text
O endpoint /config nao retorna o valor da API_KEY.
Ele retorna apenas true ou false, provando que a variavel foi recebida sem expor o segredo.
```

## 4. Arquivos Kubernetes criados

Os manifestos Kubernetes ficam na pasta:

```text
k8s/
```

Arquivos:

| Arquivo | Funcao |
| --- | --- |
| `k8s/configmap.yaml` | Define variavel nao sensivel `AMBIENTE` |
| `k8s/secret.yaml` | Define secret ficticia `API_KEY` |
| `k8s/deployment.yaml` | Cria dois Pods do front |
| `k8s/service.yaml` | Expoe o front via LoadBalancer |

## 5. ConfigMap

Arquivo:

```text
k8s/configmap.yaml
```

Funcao:

```text
guardar configuracoes nao sensiveis da aplicacao
```

Valor usado:

```text
AMBIENTE=demonstracao
```

No Kubernetes, esse valor sera injetado no container e lido pelo endpoint `/config`.

## 6. Secret

Arquivo:

```text
k8s/secret.yaml
```

Funcao:

```text
guardar informacoes sensiveis ou simuladas
```

Valor usado:

```text
API_KEY=valor-ficticio-nao-utilizar-em-producao
```

Para o trabalho, a secret e ficticia. Nao deve ser usada credencial real.

## 7. Deployment

Arquivo:

```text
k8s/deployment.yaml
```

Funcao:

```text
dizer ao Kubernetes como executar a aplicacao
```

Configuracoes principais:

| Campo | Valor |
| --- | --- |
| Nome | `cloud-commerce` |
| Replicas | `2` |
| Imagem | `382597877252.dkr.ecr.sa-east-1.amazonaws.com/microservicoscommerce-cloud-commerce:v1` |
| Porta do container | `8081` |
| Health check | `/health` |
| Configuracao | `ConfigMap` + `Secret` |

O `Deployment` garante que existam sempre dois Pods rodando. Se um Pod for removido manualmente, o Kubernetes cria outro para voltar ao estado desejado.

## 8. Service

Arquivo:

```text
k8s/service.yaml
```

Funcao:

```text
expor a aplicacao para acesso externo
```

Configuracoes principais:

| Campo | Valor |
| --- | --- |
| Tipo | `LoadBalancer` |
| Porta externa | `80` |
| Porta interna | `8081` |

Fluxo esperado:

```text
Usuario -> Load Balancer AWS -> Service Kubernetes -> Pods do cloud-commerce
```

## 9. Build local da aplicacao

No computador local:

```powershell
Set-Location "C:\Users\sirius.alves\Projetos\microservicoscommerce\cloud-commerce"
.\mvnw.cmd clean package -DskipTests
docker build -t microservicoscommerce-cloud-commerce:v1 .
```

Validar imagem:

```powershell
docker images microservicoscommerce-cloud-commerce:v1
```

## 10. Teste local com Docker

Rodar o container local:

```powershell
docker run -d `
  --name cloud-commerce-local `
  -p 8081:8081 `
  -e AMBIENTE=demonstracao `
  -e API_KEY=valor-ficticio-nao-utilizar-em-producao `
  microservicoscommerce-cloud-commerce:v1
```

Validar:

```powershell
curl http://localhost:8081/health
curl http://localhost:8081/config
docker ps
```

Remover container local quando necessario:

```powershell
docker rm -f cloud-commerce-local
```

## 11. Publicacao no ECR

Login no ECR:

```powershell
$REGISTRY = "382597877252.dkr.ecr.sa-east-1.amazonaws.com"

aws ecr get-login-password --region sa-east-1 | docker login --username AWS --password-stdin $REGISTRY
```

Tag da imagem:

```powershell
docker tag microservicoscommerce-cloud-commerce:v1 $REGISTRY/microservicoscommerce-cloud-commerce:v1
```

Push:

```powershell
docker push $REGISTRY/microservicoscommerce-cloud-commerce:v1
```

Validacao:

```powershell
aws ecr describe-images `
  --repository-name microservicoscommerce-cloud-commerce `
  --region sa-east-1 `
  --query "imageDetails[*].imageTags" `
  --output table
```

## 12. Validacao feita na EC2

A EC2 foi acessada por SSH:

```powershell
ssh -i "C:\Users\sirius.alves\Downloads\cloud-commerce-key.pem" ec2-user@13.220.41.245
```

Dentro da EC2, foi feito login no ECR:

```bash
aws ecr get-login-password --region sa-east-1 | docker login --username AWS --password-stdin 382597877252.dkr.ecr.sa-east-1.amazonaws.com
```

Imagem baixada:

```bash
docker pull 382597877252.dkr.ecr.sa-east-1.amazonaws.com/microservicoscommerce-cloud-commerce:v1
```

Arquivo de ambiente criado:

```bash
nano /home/ec2-user/cloud-commerce.env
```

Conteudo usado:

```env
AMBIENTE=demonstracao
API_KEY=valor-ficticio-nao-utilizar-em-producao
```

Container executado:

```bash
docker run -d \
  --name cloud-commerce \
  --restart unless-stopped \
  --env-file /home/ec2-user/cloud-commerce.env \
  -p 8081:8081 \
  382597877252.dkr.ecr.sa-east-1.amazonaws.com/microservicoscommerce-cloud-commerce:v1
```

Validacoes feitas:

```bash
docker ps
curl http://localhost:8081/health
curl http://localhost:8081/config
```

Resultados obtidos:

```json
{"status":"ok","version":"v1","application":"cloud-commerce"}
```

```json
{"apiKeyConfigured":true,"ambiente":"demonstracao"}
```

Essa validacao comprova:

```text
a imagem publicada no ECR pode ser baixada pela EC2
o container inicia corretamente
a aplicacao responde HTTP
as variaveis de ambiente sao recebidas no container
```

## 13. Criacao ou acesso ao EKS

Esta etapa ainda esta pendente.

Comando sugerido para criar o cluster:

```powershell
eksctl create cluster `
  --name cloud-commerce-eks `
  --region sa-east-1 `
  --nodes 2 `
  --node-type t3.small
```

Depois de criado, configurar o `kubectl`:

```powershell
aws eks update-kubeconfig --region sa-east-1 --name cloud-commerce-eks
```

Validar:

```powershell
kubectl get nodes
```

Ponto de atencao:

```text
EKS, EC2 nodes e LoadBalancer geram custo enquanto estiverem ativos.
```

## 14. Aplicacao dos manifestos no Kubernetes

Esta etapa ainda esta pendente.

Na raiz do projeto:

```powershell
Set-Location "C:\Users\sirius.alves\Projetos\microservicoscommerce"
```

Aplicar arquivos:

```powershell
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/secret.yaml
kubectl apply -f k8s/deployment.yaml
kubectl apply -f k8s/service.yaml
```

Validar recursos:

```powershell
kubectl get deployments
kubectl get pods
kubectl get services
```

Ou em um comando:

```powershell
kubectl get deployments,pods,services
```

## 15. Acesso externo pelo LoadBalancer

Esta etapa ainda esta pendente.

Buscar o endereco externo:

```powershell
kubectl get service cloud-commerce
```

Quando aparecer `EXTERNAL-IP` ou DNS, testar:

```powershell
curl http://EXTERNAL-IP/health
curl http://EXTERNAL-IP/config
```

No navegador:

```text
http://EXTERNAL-IP/
http://EXTERNAL-IP/health
http://EXTERNAL-IP/config
```

## 16. Teste de recuperacao automatica

Esta etapa ainda esta pendente.

Listar Pods:

```powershell
kubectl get pods
```

Excluir um Pod:

```powershell
kubectl delete pod NOME_DO_POD
```

Acompanhar recriacao:

```powershell
kubectl get pods -w
```

Resultado esperado:

```text
um Pod entra em Terminating
um novo Pod e criado
o Deployment volta para 2 replicas
```

Esse teste demonstra uma das principais funcoes do Kubernetes: manter o estado desejado da aplicacao.

## 17. Evidencias exigidas pelo trabalho

Durante a execucao no EKS, coletar prints ou saidas de comando:

| N | Evidencia | Estado |
| ---: | --- | --- |
| 1 | Aplicacao respondendo localmente | Feito |
| 2 | `docker images` com imagem `v1` | Feito |
| 3 | `docker ps` com container local ou EC2 | Feito |
| 4 | ECR mostrando tag `v1` | Feito/validado pelo pull |
| 5 | `kubectl get nodes` | Pendente |
| 6 | `kubectl get deployment` com 2 replicas | Pendente |
| 7 | `kubectl get pods` com dois Pods Running | Pendente |
| 8 | `kubectl get service` com LoadBalancer | Pendente |
| 9 | Acesso externo pelo LoadBalancer | Pendente |
| 10 | `/config` comprovando ConfigMap e Secret | Pendente no EKS |
| 11 | Exclusao manual de Pod e recriacao automatica | Pendente |
| 12 | Remocao dos recursos para evitar custo | Pendente |

## 18. Limpeza de recursos

Depois de coletar evidencias, remover os objetos Kubernetes:

```powershell
kubectl delete -f k8s/service.yaml
kubectl delete -f k8s/deployment.yaml
kubectl delete -f k8s/configmap.yaml
kubectl delete -f k8s/secret.yaml
```

Depois remover o cluster:

```powershell
eksctl delete cluster --name cloud-commerce-eks --region sa-east-1
```

Validar no Console AWS se nao ficaram recursos ativos:

```text
EKS cluster
EC2 nodes
Load Balancer
Elastic IP
Volumes EBS
```

## 19. Estrutura sugerida para entrega final

O documento final pode seguir uma estrutura semelhante ao exemplo enviado:

```text
Titulo
Resumo
Introducao
Referencial teorico
Metodologia
Resultados e evidencias
Limitacoes, custos e limpeza
Consideracoes finais
Referencias
Anexos com comandos, Dockerfile e YAMLs
```

Conteudos minimos:

```text
explicar a aplicacao escolhida
explicar Dockerfile e imagem Docker
explicar ECR
explicar EKS
explicar Deployment, Pods, Service, ConfigMap e Secret
mostrar comandos executados
mostrar evidencias
mostrar teste de recuperacao de Pod
mostrar limpeza dos recursos
```

## 20. Pendencias reais do projeto

Ainda falta executar:

```text
criar ou acessar cluster EKS
configurar kubectl
aplicar manifestos k8s
testar LoadBalancer
testar /health e /config pelo endpoint externo
deletar Pod e comprovar recriacao
coletar prints e saidas finais
remover recursos para evitar custo
montar documento final com evidencias
```

## 21. Resumo para apresentacao

Resumo falado:

```text
O projeto Cloud Commerce foi preparado para demonstrar deploy em nuvem com Docker, ECR, EC2 e Kubernetes.
Para a etapa Kubernetes, foi escolhido o front cloud-commerce, pois ele responde HTTP e permite validar acesso externo de forma simples.
Foram criados endpoints de health check e configuracao para demonstrar que a aplicacao esta ativa e que recebeu variaveis por ConfigMap e Secret.
A imagem Docker v1 foi criada, publicada no ECR e validada em uma EC2.
O proximo passo e criar o cluster EKS, aplicar os manifestos Kubernetes e coletar as evidencias exigidas pelo trabalho.
```
