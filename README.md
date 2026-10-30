# ♻️ ReUse Hub

**Equipamentos circulando, oportunidades crescendo.**

API REST para organizar doações de equipamentos entre laboratórios, escolas, projetos
sociais e pessoas. Um doador anuncia um lote; outra pessoa reserva uma quantidade;
o doador confirma a retirada. Cancelamentos e expirações devolvem unidades ao estoque.

Um exemplo: um laboratório tem **cinco kits ESP32** parados. Um projeto educacional
reserva dois. A API mantém três disponíveis, impede uma segunda retirada das mesmas
unidades e registra o que aconteceu com cada reserva.

**Java 21 · Spring Boot 3.5.16 · PostgreSQL 17 · React · TypeScript · Vite · MIT**

A aplicação reúne um frontend web responsivo e uma API REST, com documentação interativa pelo Swagger UI. A retirada
física é combinada fora da plataforma. O fluxo atende laboratórios, escolas e grupos
que precisam organizar a disponibilidade e a entrega de equipamentos.

## Comece por aqui

- [Executar em poucos passos](#executar-em-poucos-passos)
- [Frontend e backend](#frontend-e-backend)
- [Usar a aplicação pelo navegador](#usar-a-aplicação-pelo-navegador)
- [Da interface ao HTTP](#da-interface-ao-http)
- [Experimentar a API](#experimentar-a-api)
- [Regras de negócio](#regras-de-negócio)
- [Arquitetura e leitura do código](#arquitetura-e-leitura-do-código)
- [Concorrência e idempotência](#concorrência-e-idempotência)
- [Testes e evidências](#testes-e-evidências)
- [Usar como template](#usar-como-template)
- [Problemas comuns](#problemas-comuns)

## Por que este projeto?

O domínio aproxima desenvolvimento de software e reaproveitamento de hardware.
O problema é concreto e permite estudar regras de negócio que também aparecem em
bibliotecas, e-commerce, agendamento e gestão de estoque.

O código usa nomes em inglês e explicações em português. Classes explícitas,
injeção por construtor e poucos mecanismos implícitos ajudam na leitura. Maven
foi escolhido pelas convenções de projeto e pela execução reproduzível com o Wrapper.

## O que está implementado

| Área | Comportamento |
| --- | --- |
| Identidade | Cadastro, normalização de e-mail, BCrypt e login com JWT de curta duração |
| Catálogo | Criação, consulta pública, filtros, paginação e arquivamento pelo dono |
| Reservas | Quantidade, prazo, retirada, cancelamento e expiração automática |
| Estoque | Transação e bloqueio pessimista por oferta para evitar alocação duplicada |
| Repetição de pedidos | Chave de idempotência persistida e vinculada ao usuário |
| Histórico | Eventos de cada transição, na mesma transação da reserva |
| Permissões | Apenas participantes consultam reservas; somente o doador confirma retirada |
| Operação | Migrações Flyway, health checks, request ID, desligamento gracioso e Docker |
| Qualidade | JUnit, MockMvc, testes concorrentes, ArchUnit, JaCoCo e CI |

Não há pagamento, frete, chat, geolocalização, validação de organizações, recuperação
de senha ou administração global. Não há cálculo de CO₂ evitado: uma retirada
registrada não fornece dados suficientes para estimar esse impacto.

## Executar em poucos passos

**Requisitos:** Docker Engine/Desktop com Compose v2 e Python 3.10+.
O caminho completo com Docker não exige Java, Maven ou Node.js instalados no computador.
A primeira execução baixa dependências e imagens, portanto precisa de internet.

Para obter o projeto:

```bash
git clone --branch master https://github.com/m-m-legend/reuse-hub.git
cd reuse-hub
```

Com o Docker em execução, dentro da pasta `reuse-hub`:

```bash
python scripts/setup_env.py
docker compose up --build -d
```

No Windows, use `py` no lugar de `python` se esse for o comando disponível.
O script de configuração cria `.env` com senha de banco e chave JWT aleatórias.
Se já existir, ele preserva os valores. O arquivo é ignorado pelo Git.

Aguarde a aplicação iniciar. Para acompanhar:

```bash
docker compose logs -f app
```

Quando aparecer `Started ReuseHubApplication`, abra **http://localhost:3000** para usar o frontend. Use `Ctrl+C` para
sair dos logs; os contêineres continuam funcionando.

O preenchimento com dados fictícios é opcional:

```bash
python scripts/demo.py
```

Esse script testa o fluxo da API; ele não inicia o servidor.

| Endereço | Finalidade |
| --- | --- |
| <http://localhost:3000> | Frontend: catálogo, contas, doações e reservas |
| <http://localhost:8080/swagger-ui.html> | Explorar os contratos e executar chamadas |
| <http://localhost:8080/v3/api-docs> | Documento OpenAPI gerado pela aplicação |
| <http://localhost:8080/actuator/health> | Saúde da API e acesso ao banco |
| <http://localhost:8080/api/v1/offers> | Catálogo público em JSON |

A porta **3000** entrega a interface. A porta **8080** entrega o backend; a raiz `/` dessa porta não é uma página inicial.
O Compose publica as portas apenas em `127.0.0.1`, adequado à demonstração local.
O banco usa volume persistente: reiniciar a aplicação não apaga os registros.

### Iniciar e parar no dia a dia

Com o Docker em execução, use os comandos abaixo dentro da pasta do projeto:

```bash
# Iniciar novamente
docker compose up -d

# Parar sem remover os contêineres nem o volume do banco
docker compose stop
```

Não é necessário clonar o projeto, recriar `.env` ou executar a demonstração a cada
inicialização. Fechar o terminal ou a aba do navegador não encerra os contêineres.
Após alterar código Java, frontend, dependências ou Dockerfile, reconstrua as imagens com
`docker compose up --build -d`.

`docker compose down` remove os contêineres e a rede, mas mantém os dados. **`docker compose down -v` apaga o volume e todos os dados
locais**; use apenas quando quiser realmente reiniciar a demonstração do zero.

### Executar o Java pela IDE ou pelo terminal

Instale um **JDK 21**, configure `JAVA_HOME` e confirme com `java -version` e
`javac -version`. O Maven Wrapper dispensa uma instalação separada do Maven.

Neste modo, somente o banco roda em Docker. A aplicação Java precisa acessar uma
porta do banco publicada no computador. Se o serviço `db` estiver sem `ports`,
adicione esse bloco **dentro de `db`** no `compose.yml`. Se já existir, ajuste o
mapeamento em vez de criar um segundo bloco:

```yaml
ports:
  - "127.0.0.1:5433:5432"
```

Neste exemplo, `5433` é a porta do computador e `5432` continua sendo a porta interna
do PostgreSQL. Use outra porta do computador caso `5433` esteja indisponível.

Execute `python scripts/setup_env.py` se ainda não houver `.env`. Depois, acrescente
ou ajuste a URL no `.env`:

```dotenv
DB_URL=jdbc:postgresql://localhost:5433/reusehub
```

Se o contêiner da aplicação estiver rodando, pare-o para liberar a porta 8080.
Então inicie o banco e execute o Java:

```bash
docker compose stop app
docker compose up -d db
python scripts/run_local.py
```

O script carrega `.env` e executa `mvnw spring-boot:run`, ou `mvnw.cmd` no Windows.
Variáveis já presentes no ambiente têm precedência. O Spring Boot não lê arquivos
`.env` automaticamente; o Compose e esse script fazem essa parte.

Na IDE, importe `pom.xml`, selecione JDK 21 e execute `ReuseHubApplication` com
`DB_URL`, `DB_PASSWORD` e `JWT_SECRET` configurados no ambiente. O banco precisa
estar disponível antes de iniciar a aplicação. Ao terminar uma execução pelo
terminal, use `Ctrl+C` para encerrar o Java.

Quando a aplicação também roda no Docker, o Compose usa explicitamente
`jdbc:postgresql://db:5432/reusehub`. A porta publicada no computador não altera
esse endereço interno.

### Configuração

| Variável/propriedade | Padrão | Explicação |
| --- | --- | --- |
| `DB_URL` | `jdbc:postgresql://localhost:5432/reusehub` | Endereço JDBC; no Compose aponta para `db` |
| `DB_USER` | `reusehub` | Usuário do banco |
| `DB_PASSWORD` | Obrigatória | Sem senha de aplicação embutida no código |
| `JWT_SECRET` | Obrigatória | Ao menos 32 bytes aleatórios codificados em Base64 |
| `SPRING_PROFILES_ACTIVE` | `default` | `prod` desativa Swagger e OpenAPI |
| `APP_SECURITY_TOKEN_TTL` | `15m` | Expiração do JWT; máximo de uma hora |
| `APP_RESERVATION_TTL` | `24h` | Prazo de uma nova reserva |
| `APP_RESERVATION_EXPIRATION_DELAY_MS` | `60000` | Intervalo entre lotes de expiração |
| `APP_RESERVATION_SCHEDULING_ENABLED` | `true` | Permite desativar o agendador |

Os nomes `APP_*` seguem o relaxed binding do Spring para as propriedades de
`application.yml`. Se quiser passá-los ao contêiner, adicione-os ao bloco
`environment` de `app` no Compose; variáveis do host não entram nele automaticamente.

O perfil `test` vive em `src/test/resources` e utiliza uma chave conhecida apenas
para testes. O H2 também é dependência exclusiva de teste. Nenhum deles é incluído
como modo de demonstração inseguro no JAR de produção.

## Frontend e backend

O repositório usa uma separação por diretórios e por processo. O backend Java continua
na raiz para preservar os comandos, as migrations e a infraestrutura existentes.
O frontend vive em `frontend/` e possui dependências, testes e build próprios.

| Caminho | Responsabilidade |
| --- | --- |
| `src/main/java/` | API Spring Boot, autenticação, transações e regras de negócio |
| `src/main/resources/db/migration/` | Evolução do banco com Flyway |
| `src/test/` | Testes Java e integração da API |
| `pom.xml`, `mvnw`, `mvnw.cmd` | Build Maven do backend |
| `frontend/src/pages/` | Catálogo e acompanhamento de reservas |
| `frontend/src/components/` | Formulários, diálogos e ilustrações de categoria |
| `frontend/src/lib/api.ts` | Cliente HTTP, ProblemDetails e chaves de idempotência |
| `frontend/src/lib/session.tsx` | Sessão, login, logout e expiração |
| `frontend/src/lib/types.ts` | Tipos dos contratos JSON e rótulos em português |
| `frontend/src/styles.css` | Identidade visual, estados e layouts responsivos |
| `frontend/package.json`, `frontend/package-lock.json` | Dependências e scripts npm |
| `frontend/nginx.conf`, `frontend/Dockerfile` | Build estático e proxy da API |
| `compose.yml` | PostgreSQL, Spring Boot e frontend no mesmo ambiente |

```mermaid
flowchart TD
    A["Navegador · localhost:3000"] --> B["Frontend · React / Nginx"]
    B -->|"/api/v1 · JSON + Bearer"| C["Backend · Spring Boot:8080"]
    C --> D["PostgreSQL:5432"]
```

O frontend não acessa o banco. Quem valida permissões, calcula estoque e confirma
transações é o Spring Boot. Esconder um botão é apenas conveniência da interface;
a autorização continua sendo verificada no servidor.

No Docker, o Nginx entrega os arquivos HTML/CSS/JS e encaminha `/api/` para
`app:8080`. O navegador enxerga uma única origem (`localhost:3000`), sem necessidade
de liberar CORS. O endereço `app` existe apenas na rede Docker.

No desenvolvimento, o Vite faz o mesmo encaminhamento para `localhost:8080`.
Nenhum segredo do banco ou `JWT_SECRET` é colocado no código do frontend.

### Desenvolver o frontend com recarga automática

Use **Node.js 24** e npm. Primeiro, com `.env` configurado, inicie banco e API:

```bash
docker compose up -d db app
```

Se o backend ainda não foi construído, use `docker compose up --build -d db app`.
Em outro terminal:

```bash
cd frontend
npm ci
npm run dev
```

Abra **http://localhost:5173**. O backend deve responder em `localhost:8080`.
Também é possível executar o Java pela IDE conforme a seção anterior e manter
somente o banco no Docker.

Se a API estiver em outro endereço, configure o proxy **no processo do Vite**:

```powershell
# PowerShell, dentro de frontend/
$env:API_PROXY_TARGET = 'http://localhost:8081'
npm run dev
```

```bash
# Linux/macOS, dentro de frontend/
API_PROXY_TARGET=http://localhost:8081 npm run dev
```

A variável altera apenas o servidor de desenvolvimento. No Docker, o destino é
configurado em `frontend/nginx.conf`. Não use `npm run preview` como substituto
desse proxy: esse comando serve o build para inspeção, sem o encaminhamento de
API configurado para desenvolvimento. Para testar o build integrado, use Compose.

### Build e testes do frontend

Dentro de `frontend/`:

```bash
npm ci
npm test
npm run build
```

O build verifica TypeScript e gera `frontend/dist/`. Os testes verificam o cliente
HTTP, o tratamento de 401/ProblemDetails, falhas de rede e o isolamento/reuso das
chaves de idempotência. A CI possui um job `frontend` independente do job Java:
instala pelo lockfile, executa testes, compila a interface, constrói sua imagem
Docker e confere se o Nginx entrega a página inicial. O job Java também executa o cliente HTTP do frontend contra a API real
no contêiner, cobrindo cadastro, login, filtros, replay, permissões, estoque,
cancelamento, retirada, histórico e arquivamento. Os arquivos compilados ficam no
artefato `frontend-dist` da execução.

O teste integrado é opcional fora da CI: sem `LIVE_API_URL`, esse caso é marcado
como ignorado e somente os testes isolados rodam. Para executá-lo localmente,
aponte para uma **API de testes com dados descartáveis** (ele cria duas contas,
uma oferta e reservas, e não limpa os registros):

```powershell
# PowerShell, dentro de frontend/
$env:LIVE_API_URL = 'http://localhost:8080'
npm test
Remove-Item Env:LIVE_API_URL
```

```bash
# Linux/macOS, dentro de frontend/
LIVE_API_URL=http://localhost:8080 npm test
```

### Sessão e comportamento da interface

- O JWT fica **somente em memória**, sem `localStorage` nem cookie. Atualizar a
  página ou fechar a aba exige novo login. O prazo informado pela API também
  encerra a sessão; um `401` em chamada autenticada solicita novo acesso.
- Sair remove o token da interface, mas não revoga um JWT já emitido no backend.
- Senhas não são salvas pela aplicação. O formulário respeita também o limite
  de 72 bytes UTF-8 do BCrypt.
- A chave de uma reserva pendente fica em `sessionStorage`, separada por usuário,
  oferta e quantidade. Se houver dúvida por falha de rede, repetir a mesma intenção
  reutiliza a chave. Após resposta confirmada com sucesso, a chave é removida.
- Cadastro e criação de oferta não têm idempotência no backend. Se a conexão cair
  durante a publicação, confira o catálogo antes de repetir para evitar duplicação.
- Há estados de carregamento, erro e catálogo vazio. A aplicação não inventa
  anúncios, quantidades ou métricas quando o banco está vazio ou indisponível.
- As imagens são **ilustrações de categoria**, identificadas na interface, e não
  fotos dos anúncios. O contrato atual não possui upload de imagens.
- A navegação entre catálogo e reservas ocorre na página principal; não há rotas
  compartilháveis de detalhes nesta versão. O Nginx entrega a página inicial.
- As fontes são carregadas do Google Fonts com fallback local. Se esse serviço
  estiver bloqueado, a interface continua utilizável com fontes do sistema.

## Usar a aplicação pelo navegador

1. Abra **http://localhost:3000** (Docker) ou **http://localhost:5173** (Vite).
2. Explore o catálogo sem login. Selecione categoria e preencha busca/cidade;
   clique em **Buscar**. A cidade precisa corresponder ao nome cadastrado, sem
   distinguir maiúsculas. A busca procura no título.
3. Clique em **Entrar → Cadastre-se** e crie uma conta doadora. O cadastro bem
   sucedido é seguido por login; se o login falhar, a conta criada permanece válida.
4. Clique em **Quero doar**, preencha título, categoria, condição, descrição,
   cidade e quantidade. A nova oferta aparece no catálogo.
5. Saia da conta e crie outra, como solicitante. Uma pessoa não pode reservar sua
   própria oferta. Use perfis ou navegadores separados se quiser manter as duas
   sessões abertas ao mesmo tempo.
6. Abra a oferta, escolha uma quantidade e clique em **Reservar equipamento**.
   O estoque disponível é reduzido no backend. Acompanhe em **Minhas reservas**.
7. Abra a reserva para consultar prazo e histórico. Como solicitante, é possível
   **Cancelar reserva**. Para confirmar a entrega, entre como doador, abra o pedido
   e use **Confirmar retirada**. As ações pedem confirmação na interface.
8. Para testar cancelamento e retirada, crie reservas diferentes: uma reserva
   retirada não pode ser cancelada. A retirada física é combinada fora da plataforma;
   não há chat ou exposição dos contatos dos participantes.
9. Para arquivar uma oferta sua, abra seus detalhes com a conta doadora e use
   **Arquivar oferta**. Ela sai do catálogo, preservando reservas e histórico.

**Exemplo de conferência:** publique cinco unidades, reserve duas e verifique três
no catálogo. Cancele essa reserva e confira cinco novamente. Reserve duas com uma
nova intenção e confirme a retirada como doador: ficam três disponíveis, porque
retirar não repõe estoque.

O botão **Atualizar** em Minhas reservas busca o estado atual. O catálogo também
é recarregado após ações da interface, mas não usa WebSocket nem atualização
contínua: alterações de outras pessoas exigem uma nova consulta.

## Da interface ao HTTP

Os caminhos abaixo usam o prefixo **`/api/v1`**. `GET` consulta; `POST` cria um
recurso ou solicita uma transição. Este backend não oferece `PUT`, `PATCH` ou
`DELETE` para essas funcionalidades; arquivar e cancelar são ações de domínio
via `POST`, preservando os registros.

| Ação no frontend | Método e endpoint | Dados / efeito |
| --- | --- | --- |
| Abrir catálogo, buscar, filtrar, paginar | `GET /offers` | Query: `search`, `city`, `category`, `availableOnly`, `page`, `size` |
| Abrir detalhes | `GET /offers/{id}` | Carrega inclusive oferta arquivada quando acessada por uma reserva |
| Criar conta | `POST /auth/register` | JSON: `name`, `email`, `password` |
| Entrar | `POST /auth/login` | JSON: `email`, `password`; recebe JWT e duração |
| Identificar usuário após login | `GET /auth/me` | Envia Bearer; retorna a conta atual |
| Publicar doação | `POST /offers` | JSON com título, descrição, categoria, condição, cidade e quantidade |
| Reservar | `POST /reservations` | JSON: `offerId`, `quantity`; cabeçalho `Idempotency-Key` |
| Abrir Minhas reservas / filtrar status | `GET /reservations` | Bearer; query `status`, `page`, `size` |
| Identificar equipamentos nas reservas | `GET /offers/{id}` | Uma consulta por oferta distinta na página |
| Abrir histórico da reserva | `GET /reservations/{id}/events` | Somente participantes; lista de eventos ordenados |
| Cancelar | `POST /reservations/{id}/cancel` | Somente solicitante; devolve estoque uma vez |
| Confirmar retirada | `POST /reservations/{id}/collect` | Somente doador; encerra sem devolver estoque |
| Arquivar | `POST /offers/{id}/archive` | Somente dono; impede novas reservas |
| Sair | Nenhuma chamada | Descarta o token local; não existe endpoint de logout |

Exemplo equivalente ao botão **Reservar equipamento**:

```http
POST /api/v1/reservations
Authorization: Bearer TOKEN_RECEBIDO_NO_LOGIN
Content-Type: application/json
Idempotency-Key: 16a77021-b2ca-42a5-99ae-de0f726e458f

{"offerId":"UUID_DA_OFERTA","quantity":2}
```

Uma nova reserva retorna `201`; repetir a mesma chave e conteúdo retorna `200`
com a reserva existente. A interface trata ambos como sucesso e não repete
mutações automaticamente. Um `409` apresenta o motivo retornado pelo servidor,
como falta de estoque ou transição inválida.

Para observar as chamadas, abra **F12 → Network/Rede → Fetch/XHR**, faça uma ação
e selecione a requisição. Confira **Method**, **Status**, **Payload** e **Response**.
O endereço começa com a porta do frontend porque o proxy encaminha a chamada.
Tokens e senhas aparecem nas requisições de autenticação: não publique capturas
ou arquivos HAR contendo esses valores.

## Experimentar a API

### Demonstração automática

```bash
python scripts/demo.py
```

O script espera a aplicação responder, cria duas contas fictícias e executa:

1. Cadastro e login do laboratório e do projeto educacional.
2. Publicação de dois kits ESP32.
3. Reserva de um kit e repetição da chamada com a mesma chave.
4. Tentativa de retirada por quem não tem permissão: resposta `403`.
5. Retirada confirmada pelo doador.
6. Segunda reserva, cancelamento e repetição do cancelamento.
7. Conferência do histórico e do estoque final: uma unidade disponível.

Cada execução cria novos registros com e-mails em `example.test`. O script não
apaga esses dados nem mostra tokens e senhas. Ele modifica a API indicada; use
`--base-url URL_DA_INSTANCIA` para apontar a outra instância de testes.

### Pelo Swagger UI

1. Execute `POST /api/v1/auth/register` com nome, e-mail e senha de 12 a 72 caracteres.
   Por causa do BCrypt, o limite adicional é de **72 bytes UTF-8**, relevante para acentos.
2. Execute `POST /api/v1/auth/login` com o e-mail e a senha.
3. Copie `accessToken`, clique em **Authorize** e cole apenas o token.
4. Crie uma oferta com a conta doadora e guarde o `id` retornado.
5. Cadastre uma segunda conta, faça login e substitua o token em **Authorize**.
   O login sozinho não troca a autorização ativa do Swagger. Confira a identidade
   atual em `GET /api/v1/auth/me`.
6. Crie uma reserva com o ID da oferta e uma quantidade disponível. Em
   `Idempotency-Key`, informe um UUID novo e guarde o ID da reserva retornada.
7. Repita a mesma chamada com a mesma chave: a resposta deve ser `200`, com o mesmo
   ID de reserva e sem nova redução do estoque. A primeira chamada retorna `201`.
8. Para confirmar a retirada, volte ao token do doador e use
   `POST /api/v1/reservations/{id}/collect`. Para cancelar, use o token do solicitante
   em `POST /api/v1/reservations/{id}/cancel` antes de finalizar a reserva.

Uma reserva retirada não pode ser cancelada. Para testar os dois caminhos, crie
reservas distintas com chaves diferentes. O ID da reserva e o ID da oferta não são
intercambiáveis. Ao receber `401`, faça login novamente e atualize a autorização.

Com uma oferta inicial de cinco unidades, os resultados esperados são:

| Ação | Estoque disponível | Resultado |
| --- | --- | --- |
| Reservar duas unidades | 3 | `201`, estado `RESERVED` |
| Repetir o mesmo pedido e chave | 3 | `200`, mesma reserva |
| Confirmar retirada como solicitante | 3 | `403` |
| Confirmar retirada como doador | 3 | `200`, estado `COLLECTED` |
| Criar outra reserva de uma unidade | 2 | `201`, nova reserva |
| Cancelar a nova reserva como solicitante | 3 | `200`, estado `CANCELLED` |
| Repetir esse cancelamento | 3 | `200`, sem reposição adicional |

Exemplo de oferta:

```json
{
  "title": "Kit ESP32 + sensores",
  "description": "Kit funcional para aulas de robótica. Retirada a combinar.",
  "category": "ELECTRONICS",
  "condition": "GOOD",
  "city": "Rio de Janeiro",
  "quantity": 5
}
```

As categorias são `COMPUTERS`, `ELECTRONICS`, `TOOLS`, `EDUCATION` e `OTHER`.
As condições são `NEW`, `GOOD` e `NEEDS_REPAIR`.

Exemplo de reserva, substituindo o UUID pelo `id` da oferta criada:

```http
POST /api/v1/reservations
Authorization: Bearer SEU_TOKEN
Idempotency-Key: 16a77021-b2ca-42a5-99ae-de0f726e458f
Content-Type: application/json

{"offerId":"UUID_DA_OFERTA","quantity":2}
```

Há uma coleção com chamadas encadeadas em [http/reuse-hub.http](http/reuse-hub.http),
para a extensão REST Client do VS Code. Ela usa contas fictícias com nomes fixos:
na segunda execução, o cadastro retorna `409`; o login continua funcionando.

### Rotas

O prefixo é `/api/v1`. Não se envia `ownerId` ou `requesterId` na criação: a API
obtém o usuário a partir do JWT validado.

| Método e rota | Acesso | Resultado |
| --- | --- | --- |
| `POST /auth/register` | Público | `201`, conta sem hash de senha |
| `POST /auth/login` | Público | `200`, token Bearer e validade em segundos |
| `GET /auth/me` | Autenticado | Dados da própria conta |
| `GET /offers` | Público | Página de ofertas ativas |
| `GET /offers/{id}` | Público | Uma oferta, inclusive se arquivada |
| `POST /offers` | Autenticado | `201` e cabeçalho `Location` |
| `POST /offers/{id}/archive` | Dono da oferta | Arquiva; repetir é seguro |
| `POST /reservations` | Autenticado | `201`, ou `200` em replay |
| `GET /reservations` | Autenticado | Reservas em que o usuário é participante |
| `GET /reservations/{id}` | Participante | Detalhe da reserva |
| `GET /reservations/{id}/events` | Participante | Histórico de estados |
| `POST /reservations/{id}/cancel` | Solicitante | Cancela e devolve estoque |
| `POST /reservations/{id}/collect` | Doador | Confirma retirada sem devolver estoque |

Catálogo: `category`, `city`, `search`, `availableOnly`, `page` e `size`.
A cidade usa igualdade sem distinguir maiúsculas; a busca procura no título,
sem remover acentos. `%` e `_` são caracteres literais, não curingas fornecidos
pelo cliente. `availableOnly=true` omite estoque zero por padrão.

Reservas: `status`, `page` e `size`. Nas duas listagens, a página começa em zero,
o tamanho padrão é 20 e o máximo é 100. A ordenação é `createdAt DESC, id DESC`.
O limite de página é 10.000; catálogos grandes devem evoluir para paginação por cursor.

```json
{"content": [], "page": 0, "size": 20, "totalElements": 0, "totalPages": 0}
```

### Erros previsíveis

A API usa `application/problem+json`, com um `code` estável para o cliente e um
`requestId` também presente no cabeçalho `X-Request-Id`.

```json
{
  "type": "urn:reuse-hub:problem:insufficient_stock",
  "title": "Conflict",
  "status": 409,
  "detail": "Quantidade indisponível.",
  "instance": "/api/v1/reservations",
  "code": "INSUFFICIENT_STOCK",
  "requestId": "IDENTIFICADOR_DA_REQUISICAO"
}
```

| HTTP | Exemplos |
| --- | --- |
| `400` | Campos inválidos, UUID malformado, chave ausente ou JSON desconhecido |
| `401` | Credencial incorreta, token inválido ou autenticação ausente |
| `403` | Participante sem permissão para aquela ação |
| `404` | Recurso inexistente ou reserva de terceiros |
| `405` | Método HTTP incompatível com a rota |
| `409` | Estoque insuficiente, estado final, e-mail duplicado ou chave reutilizada |
| `415` | Tipo de conteúdo incompatível com a rota |
| `500` | Falha inesperada; detalhes internos não são enviados ao cliente |

## Regras de negócio

```mermaid
stateDiagram-v2
    [*] --> RESERVED: estoque disponível
    RESERVED --> COLLECTED: doador confirma antes do prazo
    RESERVED --> CANCELLED: solicitante cancela
    RESERVED --> EXPIRED: agendador encontra prazo vencido
    COLLECTED --> [*]
    CANCELLED --> [*]
    EXPIRED --> [*]
```

- O lote contém de 1 a 10.000 unidades. Sua quantidade inicial é imutável.
- Uma conta pode ser doadora e solicitante, mas não reservar a própria oferta.
- Criar reserva subtrai estoque imediatamente. Cada reserva pertence a uma oferta.
- Retirada é irreversível nesta versão. Uma eventual devolução precisa de um novo caso de uso.
- Cancelar ou expirar libera unidades exatamente uma vez por transição confirmada.
- Repetir a mesma retirada ou o mesmo cancelamento retorna o estado existente.
- Trocar um estado final por outro retorna `409`.
- Arquivar impede novas reservas e tira a oferta da listagem. Não apaga histórico,
  não cancela reservas existentes e não impede sua retirada ou cancelamento.
- O histórico registra ator, instante e sequência: `0` para criação e `1` para
  finalização. Assim, instantes iguais não invertem a ordem. Expiração tem `actorId=null`.
- O catálogo não expõe e-mail nem senha. Uma reserva não fica pública por conhecer seu UUID.

O agendador busca até 100 reservas por ciclo. Cada item tem sua própria transação;
uma falha não desfaz os outros. O prazo bloqueia a retirada imediatamente, mas o
estoque é liberado quando o agendador processa a expiração. Portanto, após o prazo,
pode existir um pequeno atraso na disponibilidade. Com fila grande ou indisponibilidade,
o atraso pode ultrapassar um minuto.

Enquanto uma reserva ainda estiver em `RESERVED`, o solicitante pode cancelá-la,
mesmo após o prazo. Se o agendador chegar primeiro, ela será `EXPIRED` e o
cancelamento retornará `409`. Ambos os caminhos preservam o estoque.

## Arquitetura e leitura do código

É um **monólito modular por funcionalidade**: uma aplicação, um banco e um artefato
de implantação. Os módulos são pacotes Java, não serviços separados nem subprojetos Maven.
Os testes ArchUnit verificam dependências e ausência de ciclos.

| Caminho dentro de `src/main/java/io/github/mmlegend/reusehub` | Papel |
| --- | --- |
| `identity` | Contas, autenticação e emissão de tokens |
| `offers` | Catálogo e regras de estoque |
| `reservations` | Ciclo da reserva, histórico e expiração |
| `shared` | Erros, paginação e configuração transversal |
| `*/api` | Controllers, entrada HTTP, validação e DTOs de requisição |
| `*/application` | Casos de uso, transações e DTOs de resposta |
| `*/domain` | Estado e regras das entidades |
| `*/persistence` | Repositórios Spring Data JPA |

A separação é pragmática: as entidades de domínio têm anotações JPA e os serviços
usam repositórios Spring Data. **Não é uma implementação estrita de Clean Architecture.**
Se o projeto crescer e precisar de domínio independente de ORM, extraia portas e
adaptadores com mapeamento explícito. Fazer isso agora duplicaria modelos sem uma
necessidade concreta.

O módulo de reservas acessa identidade por `AccountAccess` e estoque por
`OfferInventory`, contratos da camada de aplicação. Não importa os repositórios
nem as entidades desses módulos. `Propagation.MANDATORY` exige que essas operações
participem da transação já iniciada pelo caso de uso.

### Caminho de uma requisição

1. `SecurityConfig` valida assinatura, expiração, emissor, audiência e UUID do JWT.
2. `ReservationController` valida JSON/cabeçalho e extrai o ator do token.
3. `ReservationService` abre a transação e coordena identidade, estoque e reserva.
4. `Offer` e `Reservation` aplicam suas regras sem conhecer HTTP.
5. Os repositórios persistem entidades e evento. O commit acontece antes da resposta.
6. O controller devolve um DTO; entidades JPA e hashes de senha não são serializados.

Comece lendo `Offer.reserve`, depois `Reservation.cancel`, `ReservationService.create`
e, por último, `ApiIntegrationTest`. É uma boa sequência para ligar regra, persistência
e comportamento observável.

### Persistência

| Tabela | Dados principais |
| --- | --- |
| `accounts` | Nome, e-mail normalizado, hash BCrypt e criação |
| `offers` | Doador, categoria, condição, cidade e estoque |
| `reservations` | Participantes, quantidade, chave, estado e prazo |
| `reservation_events` | Transições confirmadas e seus atores |

As migrations ficam em `src/main/resources/db/migration`. Flyway controla a ordem e
o checksum dos arquivos. Depois de aplicada uma migration em ambiente compartilhado,
crie outra para alterar o schema; não reescreva a antiga. O Hibernate usa
`ddl-auto=validate`, portanto não cria nem altera tabelas silenciosamente.

As referências usam UUIDs e chaves estrangeiras. Índices apoiam consultas de catálogo,
participantes e vencimentos. Constraints garantem limites de estoque, unicidade de
e-mail e da chave de idempotência. `@Version` adiciona detecção de atualização obsoleta.
O fuso usado para persistência é UTC; a interface consumidora converte a apresentação.

## Concorrência e idempotência

### Disputa pela última unidade

Ler `available=1` e depois subtrair sem coordenação permitiria que duas pessoas
recebessem confirmação. Por isso, a API utiliza `PESSIMISTIC_WRITE` na oferta.
A segunda transação aguarda a primeira e verifica o estoque já atualizado.
Se acabou, recebe `409 INSUFFICIENT_STOCK`.

O PostgreSQL tem `lock_timeout=5s` nas conexões da aplicação. O limite se aplica à espera
por bloqueios, não à duração total da requisição; uma falha de concorrência pode resultar em `409 CONCURRENT_OPERATION`.
A transação inclui estoque, reserva e evento: falhar ao salvar o histórico também
reverte a alteração de estoque.

### Pedido repetido

Uma queda de conexão pode fazer o cliente não saber se a reserva foi criada.
Em vez de gerar outra chave, ele repete a mesma requisição com o mesmo UUID.

| Situação | Resposta |
| --- | --- |
| Chave nova para aquela conta | `201` e `Idempotency-Replayed: false` |
| Mesma chave, oferta e quantidade | `200`, mesma reserva e `Idempotency-Replayed: true` |
| Mesma chave, conteúdo diferente | `409 IDEMPOTENCY_KEY_REUSED` |
| Mesma chave em outra conta | Pedido independente |

O replay retorna o **estado atual** da reserva, não uma cópia congelada da resposta
original. Uma reserva cancelada continua cancelada no replay. Para uma nova intenção,
o cliente usa outra chave. A chave dura enquanto o registro existir; não há limpeza
ou janela de expiração de idempotência nesta versão.

A criação bloqueia primeiro a conta solicitante e depois a oferta. Isso serializa
pedidos simultâneos da mesma conta, inclusive se uma chave for usada em duas ofertas.
A constraint única é uma proteção adicional. Esse desenho privilegia clareza e
consistência; uma conta com alto volume pode precisar de uma tabela específica de
idempotência no futuro.

Nas transições, a ordem é **oferta → reserva**. Uma projeção com IDs é lida primeiro;
a entidade mutável da reserva só é carregada depois do lock, evitando um estado
antigo no cache de primeiro nível do JPA. O agendador usa a mesma ordem. Instâncias
simultâneas podem selecionar o mesmo vencimento, mas apenas uma confirma a transição.

## Segurança e operação

- BCrypt com custo 12; senhas não são salvas em texto simples.
- JWT HS256 validado pelo Resource Server do Spring Security, sem filtro JWT artesanal.
- O emissor e a API compartilham a chave neste monólito. Serviços separados podem
  evoluir para um provedor OIDC e chaves assimétricas.
- Sem sessões de servidor ou cookies de autenticação. CSRF está desativado para o
  contrato Bearer; ao introduzir autenticação por cookie, essa decisão deve ser revista.
- Não há CORS permissivo. Um frontend em outra origem exige configuração explícita.
- Tokens expiram em 15 minutos. Não há refresh token ou revogação individual;
  sair do cliente remove a cópia local, mas não invalida um token ainda válido.
- O cadastro informa e-mail duplicado. Para um serviço público sensível, avalie um
  fluxo com verificação de e-mail e respostas que reduzam enumeração de contas.
- Logs não registram corpo, senha ou token. O request ID ajuda a localizar falhas.
- O contêiner executa com usuário não root. O Compose restringe escrita e capabilities.

Para disponibilizar na internet, configure HTTPS em proxy/ingress, proteção contra
abuso e limitação de requisições, política de retenção de dados, backups com teste
de restauração e monitoramento. Verifique as atualizações de dependências e execute
a CI.

As versões das dependências estão definidas no `pom.xml`. Mudanças de versão devem
ser acompanhadas pela leitura das notas de compatibilidade e pela execução da suíte.
O Dependabot está configurado para propor atualizações de Maven, npm, Docker (backend e frontend) e GitHub Actions. As atualizações menores e de correção do npm são agrupadas; mudanças principais continuam exigindo revisão.

## Testes e evidências

Os testes locais não exigem Docker ou PostgreSQL: usam H2 em modo de compatibilidade,
com as mesmas migrations e a aplicação Spring real. JDK 21 é necessário.

```bash
./mvnw verify
```

No PowerShell:

```powershell
.\mvnw.cmd verify
```

| Grupo | O que verifica |
| --- | --- |
| `OfferTest` | Limites, insuficiência, reposição e arquivamento |
| `ReservationTest` | Permissões, estados finais, prazo e repetição de ações |
| `ArchitectureTest` | Camadas, módulos sem ciclos e acesso apenas por contratos |
| `ApiIntegrationTest` | HTTP, JWT real, Flyway/JPA, rollback, isolamento e concorrência |

Os testes concorrentes usam threads e transações distintas, sincronizadas por
latches, sem `sleep` para tentar provocar a corrida. Há cenários de última unidade,
mesma chave, chave em ofertas diferentes, cancelamentos simultâneos, disputa entre
retirada e cancelamento e expiração repetida.

O Mockito é carregado por `-javaagent` na inicialização dos testes; o JaCoCo mantém
seu próprio agente. Isso evita depender de autoanexação dinâmica na JVM.
O relatório de cobertura fica em `target/site/jacoco/index.html`. Cobertura é evidência
complementar; o build não impõe uma porcentagem arbitrária como prova de qualidade.

**H2 não substitui PostgreSQL.** O workflow em `.github/workflows/ci.yml` está
configurado para executar a mesma suíte com H2 e PostgreSQL 17, construir a imagem
Docker e rodar a demonstração via HTTP no contêiner. Ele é acionado em pushes para
`master`, pull requests ou manualmente na aba Actions, quando habilitado no repositório.
Consulte os resultados da execução correspondente à versão que pretende utilizar.

Para rodar a suíte contra um PostgreSQL local, use um **banco exclusivo de teste**.
Os testes apagam os registros das quatro tabelas a cada caso. Nunca aponte estas
variáveis para um banco com dados que queira preservar.

```bash
# Depois de criar reusehub_test com um usuário autorizado:
TEST_DB_URL=jdbc:postgresql://localhost:5432/reusehub_test \
TEST_DB_USER=reusehub TEST_DB_PASSWORD=SENHA_DO_BANCO \
TEST_DB_INIT_SQL="SET lock_timeout TO '5s'" ./mvnw verify
```

No PowerShell, atribua as mesmas variáveis por `$env:TEST_DB_URL = '...'` e execute
`.\mvnw.cmd verify`. O workflow contém um exemplo completo de banco de testes isolado.

## Usar como template

A estrutura pode ser adaptada para outros back-ends Spring Boot:

1. Crie um novo repositório a partir da base e renomeie o projeto e o pacote Java
   usando a refatoração da IDE.
2. Atualize `groupId`, `artifactId`, nome da aplicação, nome do JAR no Dockerfile,
   banco, OpenAPI, testes e CI.
3. Mantenha a configuração externa, o tratamento de erros e a infraestrutura que
   fizerem sentido para o novo domínio.
4. Substitua ofertas e reservas por módulos com casos de uso próprios. Defina
   permissões e invariantes antes de copiar mecanismos de lock ou idempotência.
5. Em um banco novo, crie suas migrations iniciais. Em um banco já utilizado,
   preserve as migrations aplicadas e acrescente novas versões.
6. Gere novos segredos, escreva os testes das novas regras e atualize a documentação.

Um sistema de agendamento, por exemplo, precisa modelar capacidade por horário;
um CRM pode precisar de isolamento por organização. Essas regras exigem modelagem
própria, além da troca de nomes de classes.

## Problemas comuns

| Sintoma | Causa provável / ação |
| --- | --- |
| `JWT_SECRET` ou `DB_PASSWORD` ausente | Gere `.env` e use Compose ou `run_local.py` |
| Erro de Base64 ou tamanho de chave | Gere novamente uma chave aleatória de 32 bytes; não use uma frase |
| Banco recusa senha depois de mudar `.env` | O volume existente mantém a senha antiga; altere-a no banco, ou recrie apenas dados descartáveis |
| `Connection refused` | Confirme `docker compose ps` e endereço/porta do banco |
| Porta 8080 indisponível | Pare a outra execução da aplicação ou altere a porta publicada de `app` e a URL de acesso |
| Porta 5432 ocupada ou reservada no Windows | Na execução completa em Docker, remova apenas `ports` do serviço `db`; para executar Java pela IDE, publique uma porta disponível e ajuste `DB_URL` |
| `401` depois de algum tempo | Token expirou; execute login novamente |
| `409` ao reservar | Veja `code`: falta de estoque, chave reutilizada ou estado inválido |
| Swagger não abre em `prod` | Documentação é desativada nesse perfil; use `default` no ambiente de estudo |
| `release version 21 not supported` | Maven está usando outro Java; confira `JAVA_HOME` e `./mvnw -version` |
| `Permission denied` em `mvnw` após extrair ZIP | No Linux/macOS, execute `chmod +x mvnw`; o clone Git preserva esse atributo |
| Frontend abre, mas catálogo falha | Confirme `docker compose ps` e `docker compose logs app frontend`; a API pode ainda estar iniciando |
| Porta 3000 ocupada | Altere apenas o mapeamento de `frontend`, por exemplo `127.0.0.1:3001:80`, e abra a nova porta |
| Vite responde, mas `/api` falha | Inicie o backend na porta 8080 ou ajuste `API_PROXY_TARGET` e reinicie o Vite |
| Login some após atualizar a página | Comportamento esperado: JWT apenas em memória; faça login novamente |
| CI de PostgreSQL falha | Leia os relatórios; não considere o teste em H2 suficiente para liberar a mudança |

### Conflito na porta do PostgreSQL

Na execução completa em Docker, aplicação e banco se comunicam por `db:5432`.
Se o Windows impedir a publicação da porta 5432, remova somente este bloco do
serviço `db` em `compose.yml`:

```yaml
ports:
  - "127.0.0.1:5432:5432"
```

Mantenha a publicação da porta 8080 do serviço `app` e execute `docker compose up -d`.
Essa alteração não exige reconstruir a imagem nem apagar o volume do banco.
A execução pela IDE exige uma porta publicada, conforme a seção específica acima.

## Referências e próximos passos

Documentação oficial: [Spring Boot 3.5](https://docs.spring.io/spring-boot/3.5/),
[Spring Security JWT](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html),
[Spring Data JPA](https://docs.spring.io/spring-data/jpa/reference/),
[springdoc](https://springdoc.org/) e [PostgreSQL](https://www.postgresql.org/docs/17/).

Evoluções possíveis: verificação de e-mail, moderação, coordenação da retirada,
fotos com armazenamento externo, paginação por cursor, métricas de itens efetivamente
retirados e outbox para notificações confiáveis. Cada item deve vir com uma necessidade
concreta e testes; não é necessário transformá-los em microserviços.

Licenciado sob [MIT](LICENSE).
