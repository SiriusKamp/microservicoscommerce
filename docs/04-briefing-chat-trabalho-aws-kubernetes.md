# Briefing para orientar o trabalho AWS/Kubernetes

Este documento serve para orientar outro chat ou professor particular sobre o que precisa ser feito para transformar o projeto atual em uma entrega aderente ao desafio de Computacao em Nuvem e Orquestracao com Kubernetes.

## 1. Contexto do aluno

O aluno esta estudando arquitetura de sistemas com um projeto chamado Cloud Commerce.

O projeto atual possui:

```text
front Spring Boot/Thymeleaf
microservico produto
microservico estoque
microservico pedido
RabbitMQ local
Dockerfiles nos servicos
uso inicial de AWS ECR e EC2
front cloud-commerce:v1 publicado no ECR e validado na EC2
```

A etapa RabbitMQ foi finalizada. Agora o foco e o trabalho de AWS/Kubernetes.

## 2. Enunciado do novo trabalho

O desafio pede demonstrar o fluxo:

```text
Aplicacao -> Docker -> Registro de imagens -> Amazon EKS -> Deployment -> Service -> Acesso externo
```

O trabalho nao exige uma aplicacao complexa.

O foco e comprovar:

```text
aplicacao HTTP funcionando
imagem Docker criada e testada localmente
imagem publicada em registro Docker Hub ou ECR
kubectl conectado ao EKS
Deployment com duas replicas
Service LoadBalancer
ConfigMap
Secret ficticio
recuperacao automatica de Pod
documentacao com comandos e evidencias
limpeza dos recursos AWS
```

## 3. Decisao recomendada

Recomendacao principal:

```text
usar apenas uma aplicacao simples do projeto para o desafio EKS
```

Melhor opcao:

```text
cloud-commerce
```

Justificativa:

```text
porta HTTP definida: 8081
nao precisa conectar ao banco para subir a tela inicial
nao precisa RabbitMQ para demonstrar Kubernetes
serve como pagina web acessivel pelo LoadBalancer
reduz risco e custo de subir todo o ecossistema no EKS
```

Alternativa aceitavel:

```text
criar uma API simples separada apenas para o trabalho
```

Isso tambem atende o enunciado, porque a complexidade funcional da aplicacao nao sera avaliada.

## 4. Ajustes implementados no codigo antes do EKS

Para facilitar a comprovacao de ConfigMap e Secret, foram adicionados no servico escolhido dois endpoints simples:

| Endpoint | Funcao |
| --- | --- |
| `GET /health` | Retorna que a aplicacao esta no ar |
| `GET /config` | Mostra ambiente e se o Secret foi recebido |

Exemplo de resposta `/health`:

```json
{
  "status": "ok",
  "application": "cloud-commerce",
  "version": "v1"
}
```

Exemplo de resposta `/config`:

```json
{
  "ambiente": "demonstracao",
  "apiKeyConfigured": true
}
```

Importante:

```text
o endpoint /config nao deve retornar o valor do Secret
deve retornar apenas true/false informando que recebeu a variavel
```

Variaveis esperadas:

```text
AMBIENTE=demonstracao
API_KEY=valor-ficticio-nao-utilizar-em-producao
```

## 5. Artefatos Kubernetes criados

A pasta criada foi:

```text
k8s/
```

Arquivos criados:

```text
k8s/configmap.yaml
k8s/secret.yaml
k8s/deployment.yaml
k8s/service.yaml
```

### configmap.yaml

Deve conter uma configuracao nao sensivel:

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: cloud-commerce-config
data:
  AMBIENTE: demonstracao
```

### secret.yaml

Deve conter valor ficticio:

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: cloud-commerce-secret
type: Opaque
stringData:
  API_KEY: valor-ficticio-nao-utilizar-em-producao
```

### deployment.yaml

Deve:

```text
usar a imagem publicada no ECR ou Docker Hub
ter replicas: 2
expor containerPort: 8081
referenciar ConfigMap e Secret
```

Modelo:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: cloud-commerce
spec:
  replicas: 2
  selector:
    matchLabels:
      app: cloud-commerce
  template:
    metadata:
      labels:
        app: cloud-commerce
    spec:
      containers:
        - name: cloud-commerce
          image: <ACCOUNT_ID>.dkr.ecr.sa-east-1.amazonaws.com/microservicoscommerce-cloud-commerce:v1
          ports:
            - containerPort: 8081
          env:
            - name: AMBIENTE
              valueFrom:
                configMapKeyRef:
                  name: cloud-commerce-config
                  key: AMBIENTE
            - name: API_KEY
              valueFrom:
                secretKeyRef:
                  name: cloud-commerce-secret
                  key: API_KEY
```

### service.yaml

Deve criar LoadBalancer:

```yaml
apiVersion: v1
kind: Service
metadata:
  name: cloud-commerce
spec:
  type: LoadBalancer
  selector:
    app: cloud-commerce
  ports:
    - name: http
      protocol: TCP
      port: 80
      targetPort: 8081
```

## 6. Fluxo de comandos esperado

### Build local

```powershell
Set-Location "C:\Users\sirius.alves\Projetos\microservicoscommerce\cloud-commerce"
.\mvnw.cmd clean package -DskipTests
docker build -t microservicoscommerce-cloud-commerce:v1 .
```

### Teste local

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
docker images
```

### Publicacao no ECR

```powershell
aws ecr get-login-password --region sa-east-1 | docker login --username AWS --password-stdin <ACCOUNT_ID>.dkr.ecr.sa-east-1.amazonaws.com
docker tag microservicoscommerce-cloud-commerce:v1 <ACCOUNT_ID>.dkr.ecr.sa-east-1.amazonaws.com/microservicoscommerce-cloud-commerce:v1
docker push <ACCOUNT_ID>.dkr.ecr.sa-east-1.amazonaws.com/microservicoscommerce-cloud-commerce:v1
```

Validar:

```powershell
aws ecr describe-images --repository-name microservicoscommerce-cloud-commerce --region sa-east-1
```

### Acesso ao EKS

O roteiro guiado do professor deve indicar como criar/acessar o cluster.

Validar:

```powershell
kubectl get nodes
```

### Aplicar manifestos

```powershell
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/secret.yaml
kubectl apply -f k8s/deployment.yaml
kubectl apply -f k8s/service.yaml
```

Validar:

```powershell
kubectl get deployments
kubectl get pods
kubectl get services
```

## 7. Evidencias obrigatorias

Coletar prints ou saidas de comando:

| Nº | Evidencia |
| ---: | --- |
| 1 | Aplicacao respondendo localmente |
| 2 | `docker images` mostrando a imagem `v1` |
| 3 | `docker ps` mostrando container local |
| 4 | ECR ou Docker Hub mostrando tag `v1` |
| 5 | `kubectl get nodes` |
| 6 | `kubectl get deployment` mostrando `2/2` |
| 7 | `kubectl get pods` mostrando dois Pods `Running` |
| 8 | `kubectl get service` mostrando `EXTERNAL-IP` |
| 9 | Navegador ou `curl` acessando o LoadBalancer |
| 10 | `/config` comprovando ConfigMap e Secret |
| 11 | Exclusao manual de Pod e recriacao automatica |
| 12 | Remocao dos recursos para evitar custo |

## 8. Teste de recuperacao automatica

Listar Pods:

```powershell
kubectl get pods
```

Excluir um Pod:

```powershell
kubectl delete pod NOME_DO_POD
```

Observar recriacao:

```powershell
kubectl get pods -w
```

Resultado esperado:

```text
um Pod entra em Terminating
outro Pod novo e criado
Deployment volta para 2 replicas
```

## 9. Limpeza para evitar custos

Remover objetos Kubernetes:

```powershell
kubectl delete -f k8s/service.yaml
kubectl delete -f k8s/deployment.yaml
kubectl delete -f k8s/configmap.yaml
kubectl delete -f k8s/secret.yaml
```

Depois remover o cluster conforme o roteiro usado para criar o EKS.

Ponto critico:

```text
Service LoadBalancer pode gerar custo enquanto existir.
Cluster EKS e nodes tambem podem gerar custo.
```

## 10. Estrutura sugerida do documento final

O documento final pode seguir este formato:

```text
Titulo
Resumo
Introducao
Referencial teorico curto
Metodologia
Resultados e evidencias
Limitacoes e custos
Consideracoes finais
Referencias
Anexos com comandos e manifestos
```

Mas para a entrega, o foco deve ser objetivo:

```text
o que foi feito
quais comandos foram usados
quais evidencias comprovam
quais recursos foram removidos
```

## 11. Pontos que o chat deve cuidar

O chat orientador deve:

1. Nao tentar subir todos os microservicos de uma vez no EKS.
2. Priorizar uma aplicacao HTTP simples para cumprir o desafio.
3. Usar ECR como registro de imagens, pois ele ja foi validado no projeto.
4. Garantir tag clara, como `v1`.
5. Criar manifestos Kubernetes pequenos e legiveis.
6. Usar Secret ficticio.
7. Garantir `replicas: 2`.
8. Garantir `Service` do tipo `LoadBalancer`.
9. Coletar evidencias antes de apagar recursos.
10. Registrar a limpeza para evitar custo.

## 12. Criterios de avaliacao mapeados

| Criterio | Como atender |
| --- | --- |
| Aplicacao e teste local - 10 pts | Rodar container local e acessar `/health` |
| Docker e registro - 20 pts | Dockerfile, build, tag `v1`, push no ECR |
| Deployment e replicas - 20 pts | `deployment.yaml` com `replicas: 2` e Pods Running |
| Service e acesso externo - 15 pts | `service.yaml` LoadBalancer e acesso por EXTERNAL-IP |
| Configuracao e recuperacao - 20 pts | ConfigMap, Secret ficticio e teste de recriacao de Pod |
| Documentacao e evidencias - 15 pts | Documento final com diagrama, comandos, prints e conclusao |

## 13. Proximo passo recomendado

Proximo passo tecnico:

```text
criar ou acessar o cluster EKS e configurar o kubectl
```

Depois:

```text
criar ou acessar o cluster EKS
configurar kubectl
aplicar os manifestos da pasta k8s/
coletar evidencias
remover os recursos AWS para evitar custos
```
