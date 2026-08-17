// ==========================================
// OBTER CARRINHO
// ==========================================

function obterCarrinho() {

    const carrinho =
        localStorage.getItem("carrinho");


    if (!carrinho) {
        return [];
    }


    return JSON.parse(carrinho);
}


// ==========================================
// SALVAR CARRINHO
// ==========================================

function salvarCarrinho(carrinho) {

    localStorage.setItem(
        "carrinho",
        JSON.stringify(carrinho)
    );
}


// ==========================================
// ADICIONAR PRODUTO AO CARRINHO
// ==========================================

function adicionarAoCarrinho(botao) {

    const produto = {

        id: Number(botao.dataset.id),

        nome: botao.dataset.nome,

        preco: Number(botao.dataset.preco),

        descricao: botao.dataset.descricao,

        categoria: botao.dataset.categoria

    };


    const carrinho =
        obterCarrinho();


    const itemExistente =
        carrinho.find(
            item => item.id === produto.id
        );


    if (itemExistente) {

        itemExistente.quantidade++;

    } else {

        carrinho.push({

            id: produto.id,

            nome: produto.nome,

            preco: produto.preco,

            descricao: produto.descricao,

            categoria: produto.categoria,

            quantidade: 1

        });

    }


    salvarCarrinho(carrinho);


    alert(
        produto.nome +
        " foi adicionado ao carrinho!"
    );
}


// ==========================================
// CARREGAR CARRINHO
// ==========================================

function carregarCarrinho() {

    const lista =
        document.getElementById("cart-list");


    // Não estamos na página do carrinho

    if (!lista) {
        return;
    }


    const totalElemento =
        document.getElementById("cart-total");


    const carrinho =
        obterCarrinho();


    lista.innerHTML = "";


    // ======================================
    // CARRINHO VAZIO
    // ======================================

    if (carrinho.length === 0) {

        lista.innerHTML = `
            <p>
                Seu carrinho está vazio.
            </p>
        `;


        if (totalElemento) {
            totalElemento.textContent = "0.00";
        }


        return;
    }


    let total = 0;


    // ======================================
    // PRODUTOS
    // ======================================

    carrinho.forEach(item => {

        const subtotal =
            item.preco *
            item.quantidade;


        total += subtotal;


        const elemento =
            document.createElement("div");


        elemento.classList.add(
            "cart-item"
        );


        elemento.innerHTML = `

            <div>

                <h3>
                    ${item.nome}
                </h3>

                <p>
                    Preço unitário:
                    R$ ${item.preco.toFixed(2)}
                </p>

                <div class="cart-quantity">

                    <button
                        type="button"
                        onclick="diminuirQuantidade(${item.id})">

                        −

                    </button>


                    <span>
                        ${item.quantidade}
                    </span>


                    <button
                        type="button"
                        onclick="aumentarQuantidade(${item.id})">

                        +

                    </button>


                    <button
                        type="button"
                        onclick="removerDoCarrinho(${item.id})">

                        🗑️ Remover

                    </button>

                </div>

            </div>


            <strong>

                R$
                ${subtotal.toFixed(2)}

            </strong>

        `;


        lista.appendChild(elemento);

    });


    // ======================================
    // TOTAL
    // ======================================

    if (totalElemento) {

        totalElemento.textContent =
            total.toFixed(2);

    }
}


// ==========================================
// AUMENTAR QUANTIDADE
// ==========================================

function aumentarQuantidade(id) {

    const carrinho =
        obterCarrinho();


    const item =
        carrinho.find(
            item => item.id === id
        );


    if (!item) {
        return;
    }


    item.quantidade++;


    salvarCarrinho(
        carrinho
    );


    carregarCarrinho();
}


// ==========================================
// DIMINUIR QUANTIDADE
// ==========================================

function diminuirQuantidade(id) {

    const carrinho =
        obterCarrinho();


    const item =
        carrinho.find(
            item => item.id === id
        );


    if (!item) {
        return;
    }


    item.quantidade--;


    // Se chegar a zero,
    // remove o produto

    if (item.quantidade <= 0) {

        const novoCarrinho =
            carrinho.filter(
                item => item.id !== id
            );


        salvarCarrinho(
            novoCarrinho
        );

    } else {

        salvarCarrinho(
            carrinho
        );

    }


    carregarCarrinho();
}


// ==========================================
// REMOVER PRODUTO
// ==========================================

function removerDoCarrinho(id) {

    const carrinho =
        obterCarrinho();


    const novoCarrinho =
        carrinho.filter(
            item => item.id !== id
        );


    salvarCarrinho(
        novoCarrinho
    );


    carregarCarrinho();
}


// ==========================================
// FINALIZAR PEDIDO
// ==========================================

function finalizarPedido() {

    const carrinho =
        obterCarrinho();


    if (carrinho.length === 0) {

        alert(
            "Seu carrinho está vazio."
        );

        return;
    }


    const confirmar =
        confirm(
            "Deseja finalizar o pedido?"
        );


    if (!confirmar) {
        return;
    }


    console.log(
        "Pedido:",
        carrinho
    );


    alert(
        "Pedido enviado!"
    );
}


// ==========================================
// INICIALIZAÇÃO
// ==========================================

document.addEventListener(
    "DOMContentLoaded",
    function () {

        carregarCarrinho();

    }
);