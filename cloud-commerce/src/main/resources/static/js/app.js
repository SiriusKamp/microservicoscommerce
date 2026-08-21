const CART_STORAGE_KEY = "carrinho";


function obterCarrinho() {
    const carrinhoSalvo =
        localStorage.getItem(CART_STORAGE_KEY);

    if (!carrinhoSalvo) {
        return [];
    }

    try {
        return JSON.parse(carrinhoSalvo);
    } catch (erro) {
        console.warn("Carrinho inválido no localStorage.", erro);
        return [];
    }
}


function salvarCarrinho(carrinho) {
    localStorage.setItem(
        CART_STORAGE_KEY,
        JSON.stringify(carrinho)
    );

    atualizarContadorCarrinho();
}


function adicionarAoCarrinho(botao) {
    const produto =
        criarProdutoDoBotao(botao);

    const carrinho =
        obterCarrinho();

    const itemExistente =
        carrinho.find(item => item.id === produto.id);

    if (itemExistente) {
        itemExistente.quantidade += 1;
    } else {
        carrinho.push({
            ...produto,
            quantidade: 1
        });
    }

    salvarCarrinho(carrinho);
    mostrarToastCarrinho(
        "Produto adicionado",
        `${produto.nome} foi adicionado ao carrinho.`
    );
}


function criarProdutoDoBotao(botao) {
    return {
        id: Number(botao.dataset.id),
        nome: botao.dataset.nome,
        preco: Number(botao.dataset.preco),
        descricao: botao.dataset.descricao,
        categoria: botao.dataset.categoria
    };
}


function carregarCarrinho() {
    const lista =
        document.getElementById("cart-list");

    if (!lista) {
        return;
    }

    const carrinho =
        obterCarrinho();

    lista.innerHTML = "";

    if (carrinho.length === 0) {
        lista.innerHTML = `
            <div class="empty">
                Seu carrinho está vazio.
            </div>
        `;

        atualizarTotalCarrinho(0);
        return;
    }

    carrinho.forEach(item => {
        lista.appendChild(criarItemCarrinho(item));
    });

    atualizarTotalCarrinho(calcularTotal(carrinho));
}


function criarItemCarrinho(item) {
    const elemento =
        document.createElement("article");

    elemento.className = "cart-item card border-0 shadow-sm";

    elemento.innerHTML = `
        <div class="card-body d-flex flex-column flex-md-row justify-content-between gap-3">
            <div>
                <h2 class="h5 fw-semibold mb-2">
                    ${escaparHtml(item.nome)}
                </h2>

                <p class="text-secondary mb-3">
                    Preço unitário: ${formatarMoeda(item.preco)}
                </p>

                <div class="cart-actions d-flex align-items-center gap-2">
                    <button class="btn btn-outline-secondary btn-sm"
                            type="button"
                            onclick="diminuirQuantidade(${item.id})">
                        -
                    </button>

                    <span class="badge text-bg-light border px-3 py-2">
                        ${item.quantidade}
                    </span>

                    <button class="btn btn-outline-secondary btn-sm"
                            type="button"
                            onclick="aumentarQuantidade(${item.id})">
                        +
                    </button>

                    <button class="btn btn-outline-danger btn-sm ms-auto"
                            type="button"
                            onclick="removerDoCarrinho(${item.id})">
                        Remover
                    </button>
                </div>
            </div>

            <div class="text-md-end">
                <p class="text-secondary mb-1">
                    Subtotal
                </p>

                <strong class="h5">
                    ${formatarMoeda(calcularSubtotal(item))}
                </strong>
            </div>
        </div>
    `;

    return elemento;
}


function aumentarQuantidade(id) {
    const carrinho =
        obterCarrinho();

    const item =
        carrinho.find(item => item.id === id);

    if (!item) {
        return;
    }

    item.quantidade += 1;
    salvarCarrinho(carrinho);
    carregarCarrinho();
}


function diminuirQuantidade(id) {
    const carrinho =
        obterCarrinho();

    const item =
        carrinho.find(item => item.id === id);

    if (!item) {
        return;
    }

    item.quantidade -= 1;

    const carrinhoAtualizado =
        item.quantidade <= 0
            ? carrinho.filter(itemCarrinho => itemCarrinho.id !== id)
            : carrinho;

    salvarCarrinho(carrinhoAtualizado);
    carregarCarrinho();
}


function removerDoCarrinho(id) {
    const carrinhoAtualizado =
        obterCarrinho()
            .filter(item => item.id !== id);

    salvarCarrinho(carrinhoAtualizado);
    carregarCarrinho();
}


function finalizarPedido() {
    const carrinho =
        obterCarrinho();

    if (carrinho.length === 0) {
        alert("Seu carrinho está vazio.");
        return;
    }

    if (!confirm("Deseja finalizar o pedido?")) {
        return;
    }

    console.log("Pedido:", carrinho);
    alert("Pedido enviado.");
}


function atualizarContadorCarrinho() {
    const totalItens =
        obterCarrinho()
            .reduce(
                (total, item) => total + Number(item.quantidade ?? 0),
                0
            );

    document
        .querySelectorAll("#cart-count")
        .forEach(contador => {
            contador.textContent = totalItens;
        });
}


function atualizarTotalCarrinho(total) {
    const totalElemento =
        document.getElementById("cart-total");

    if (totalElemento) {
        totalElemento.textContent = formatarMoeda(total);
    }
}


function calcularSubtotal(item) {
    return Number(item.preco ?? 0) * Number(item.quantidade ?? 0);
}


function calcularTotal(carrinho) {
    return carrinho.reduce(
        (total, item) => total + calcularSubtotal(item),
        0
    );
}


function formatarMoeda(valor) {
    return Number(valor ?? 0).toLocaleString(
        "pt-BR",
        {
            style: "currency",
            currency: "BRL"
        }
    );
}


function escaparHtml(valor) {
    const elemento =
        document.createElement("div");

    elemento.textContent = valor ?? "";

    return elemento.innerHTML;
}


function mostrarToastCarrinho(
    titulo,
    mensagem
) {
    if (!window.bootstrap) {
        alert(mensagem);
        return;
    }

    const container =
        obterContainerToast();

    const toast =
        document.createElement("div");

    toast.className = "toast border-0 shadow";
    toast.setAttribute("role", "status");
    toast.setAttribute("aria-live", "polite");
    toast.setAttribute("aria-atomic", "true");

    toast.innerHTML = `
        <div class="toast-header">
            <strong class="me-auto text-success">
                ${escaparHtml(titulo)}
            </strong>

            <button class="btn-close"
                    type="button"
                    data-bs-dismiss="toast"
                    aria-label="Fechar"></button>
        </div>

        <div class="toast-body d-grid gap-2">
            <span>
                ${escaparHtml(mensagem)}
            </span>

            <a class="btn btn-sm btn-outline-primary w-auto"
               href="/carrinho">
                Ver carrinho
            </a>
        </div>
    `;

    container.appendChild(toast);

    const toastBootstrap =
        new window.bootstrap.Toast(
            toast,
            {
                delay: 2500
            }
        );

    toast.addEventListener(
        "hidden.bs.toast",
        function () {
            toast.remove();
        }
    );

    toastBootstrap.show();
}


function obterContainerToast() {
    const containerExistente =
        document.getElementById("toast-container");

    if (containerExistente) {
        return containerExistente;
    }

    const container =
        document.createElement("div");

    container.id = "toast-container";
    container.className = "toast-container position-fixed top-0 end-0 p-3";
    container.style.zIndex = "1080";

    document.body.appendChild(container);

    return container;
}


document.addEventListener(
    "DOMContentLoaded",
    function () {
        atualizarContadorCarrinho();
        carregarCarrinho();
    }
);
