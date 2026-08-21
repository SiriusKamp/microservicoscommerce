const PRODUTO_SERVICE = "http://localhost:8082";
const ESTOQUE_SERVICE = "http://localhost:8083";


async function carregarProdutos() {
    const grid =
        document.getElementById("product-grid");

    if (!grid) {
        return;
    }

    try {
        const produtos =
            await buscarProdutos();

        const estoques =
            await buscarEstoques();

        const estoquePorProduto =
            criarMapaEstoque(estoques);

        renderizarProdutos(
            produtos,
            estoquePorProduto
        );
    } catch (erro) {
        console.error("Erro ao carregar produtos:", erro);
        mostrarErroProdutos();
    }
}


async function buscarProdutos() {
    const resposta =
        await fetch(`${PRODUTO_SERVICE}/produtos`);

    if (!resposta.ok) {
        throw new Error("Erro ao buscar produtos");
    }

    return resposta.json();
}


async function buscarEstoques() {
    try {
        const resposta =
            await fetch(`${ESTOQUE_SERVICE}/estoque`);

        if (!resposta.ok) {
            throw new Error("Erro ao buscar estoque");
        }

        return resposta.json();
    } catch (erro) {
        console.warn(
            "Estoque indisponível. Produtos serão exibidos com estoque 0.",
            erro
        );

        return [];
    }
}


function criarMapaEstoque(estoques) {
    const estoquePorProduto =
        new Map();

    estoques.forEach(estoque => {
        estoquePorProduto.set(
            Number(estoque.produtoId),
            Number(estoque.quantidade ?? 0)
        );
    });

    return estoquePorProduto;
}


function renderizarProdutos(
    produtos,
    estoquePorProduto
) {
    const grid =
        document.getElementById("product-grid");

    grid.innerHTML = "";

    if (produtos.length === 0) {
        grid.innerHTML = `
            <div class="col">
                <div class="empty">
                    Nenhum produto encontrado.
                </div>
            </div>
        `;

        return;
    }

    produtos.forEach(produto => {
        const quantidadeEstoque =
            estoquePorProduto.get(Number(produto.id)) ?? 0;

        grid.appendChild(
            criarCardProduto(
                produto,
                quantidadeEstoque
            )
        );
    });
}


function criarCardProduto(
    produto,
    quantidadeEstoque
) {
    const coluna =
        document.createElement("div");

    coluna.className = "col";
    coluna.innerHTML = `
        <article class="card product-card h-100 shadow-sm">
            <div class="card-body d-flex flex-column">
                <div class="d-flex justify-content-between align-items-start gap-3 mb-3">
                    <div>
                        <p class="section-eyebrow text-secondary fw-semibold mb-1"
                           data-produto-categoria></p>

                        <h2 class="h5 card-title fw-semibold mb-0"
                            data-produto-nome></h2>
                    </div>

                    <span class="badge stock-badge"
                          data-produto-estoque></span>
                </div>

                <p class="card-text text-secondary"
                   data-produto-descricao></p>

                <p class="h5 fw-bold mb-4"
                   data-produto-preco></p>

                <button class="btn btn-primary mt-auto"
                        type="button"
                        data-produto-botao></button>
            </div>
        </article>
    `;

    coluna.querySelector("[data-produto-categoria]").textContent =
        produto.categoria ?? "Sem categoria";

    coluna.querySelector("[data-produto-nome]").textContent =
        produto.nome;

    coluna.querySelector("[data-produto-descricao]").textContent =
        produto.descricao ?? "Sem descrição.";

    coluna.querySelector("[data-produto-preco]").textContent =
        formatarMoeda(produto.preco);

    configurarBadgeEstoque(
        coluna.querySelector("[data-produto-estoque]"),
        quantidadeEstoque
    );

    configurarBotaoCarrinho(
        coluna.querySelector("[data-produto-botao]"),
        produto,
        quantidadeEstoque
    );

    return coluna;
}


function configurarBadgeEstoque(
    badge,
    quantidadeEstoque
) {
    badge.textContent = `Estoque: ${quantidadeEstoque}`;
    badge.classList.add(
        quantidadeEstoque > 0
            ? "text-bg-success"
            : "text-bg-secondary"
    );
}


function configurarBotaoCarrinho(
    botao,
    produto,
    quantidadeEstoque
) {
    botao.disabled = quantidadeEstoque <= 0;
    botao.textContent =
        quantidadeEstoque > 0
            ? "Adicionar ao carrinho"
            : "Sem estoque";

    botao.dataset.id = produto.id;
    botao.dataset.nome = produto.nome;
    botao.dataset.preco = produto.preco;
    botao.dataset.descricao = produto.descricao ?? "";
    botao.dataset.categoria = produto.categoria ?? "";

    botao.addEventListener(
        "click",
        function () {
            adicionarAoCarrinho(botao);
        }
    );
}


function mostrarErroProdutos() {
    const grid =
        document.getElementById("product-grid");

    grid.innerHTML = `
        <div class="col">
            <div class="empty">
                Não foi possível carregar os produtos.
            </div>
        </div>
    `;
}


document.addEventListener(
    "DOMContentLoaded",
    carregarProdutos
);
