const PRODUTO_SERVICE = "http://localhost:8082";
const ESTOQUE_SERVICE = "http://localhost:8083";


async function carregarProdutos() {

    const grid = document.getElementById("product-grid");

    if (!grid) {
        return;
    }

    try {

        // ==============================
        // BUSCAR PRODUTOS
        // ==============================

        const respostaProdutos =
            await fetch(`${PRODUTO_SERVICE}/produtos`);

        if (!respostaProdutos.ok) {
            throw new Error("Erro ao buscar produtos");
        }

        const produtos =
            await respostaProdutos.json();


        // ==============================
        // BUSCAR ESTOQUE
        // ==============================

        const respostaEstoque =
            await fetch(`${ESTOQUE_SERVICE}/estoque`);

        if (!respostaEstoque.ok) {
            throw new Error("Erro ao buscar estoque");
        }

        const estoques =
            await respostaEstoque.json();


        console.log("PRODUTOS:", produtos);
        console.log("ESTOQUES:", estoques);


        // ==============================
        // MAPEAR ESTOQUE POR PRODUTO
        // ==============================

        const estoquePorProduto = new Map();

        estoques.forEach(estoque => {

            estoquePorProduto.set(
                Number(estoque.produtoId),
                Number(estoque.quantidade)
            );

        });


        console.log(
            "ESTOQUE POR PRODUTO:",
            estoquePorProduto
        );


        // ==============================
        // LIMPAR GRID
        // ==============================

        grid.innerHTML = "";


        // ==============================
        // CRIAR CARDS
        // ==============================

        produtos.forEach(produto => {

            const quantidadeEstoque =
                estoquePorProduto.get(
                    Number(produto.id)
                ) ?? 0;


            criarCardProduto(
                produto,
                quantidadeEstoque
            );

        });


    } catch (erro) {

        console.error(
            "Erro ao carregar produtos/estoque:",
            erro
        );

        grid.innerHTML = `
            <p>
                Não foi possível carregar os produtos.
            </p>
        `;
    }
}


function criarCardProduto(
    produto,
    quantidadeEstoque
) {

    const grid =
        document.getElementById("product-grid");


    const card =
        document.createElement("div");


    card.classList.add("product-card");


    card.innerHTML = `

        <div class="product-content">

            <h3>
                ${produto.nome}
            </h3>

            <p>
                ${produto.descricao}
            </p>

            <strong>

                R$

                ${Number(produto.preco).toFixed(2)}

            </strong>

            <p>

                Categoria:

                ${produto.categoria}

            </p>

            <p>

                Estoque:

                ${quantidadeEstoque}

            </p>

            <button
                class="primary-button"
                data-id="${produto.id}"
                data-nome="${produto.nome}"
                data-preco="${produto.preco}"
                data-descricao="${produto.descricao}"
                data-categoria="${produto.categoria}"
                onclick="adicionarAoCarrinho(this)">

                🛒 Pedir

            </button>

        </div>

    `;


    grid.appendChild(card);
}


document.addEventListener(
    "DOMContentLoaded",
    carregarProdutos
);