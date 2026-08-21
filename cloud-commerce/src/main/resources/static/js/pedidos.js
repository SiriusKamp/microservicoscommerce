const PEDIDO_SERVICE = "http://localhost:8084";
const FORMATO_DATA = new Intl.DateTimeFormat("pt-BR");


async function carregarPedidos() {
    const lista =
        document.getElementById("orders-list");

    if (!lista) {
        return;
    }

    try {
        const pedidos =
            await buscarPedidos();

        renderizarPedidos(pedidos);
    } catch (erro) {
        console.error("Erro ao carregar pedidos:", erro);
        mostrarErroPedidos();
    }
}


async function buscarPedidos() {
    const resposta =
        await fetch(`${PEDIDO_SERVICE}/pedidos`);

    if (!resposta.ok) {
        throw new Error("Erro ao buscar pedidos");
    }

    return resposta.json();
}


function renderizarPedidos(pedidos) {
    const lista =
        document.getElementById("orders-list");

    lista.innerHTML = "";

    if (pedidos.length === 0) {
        lista.innerHTML = `
            <div class="empty">
                Nenhum pedido encontrado.
            </div>
        `;

        return;
    }

    pedidos.forEach(pedido => {
        lista.appendChild(criarCardPedido(pedido));
    });
}


function criarCardPedido(pedido) {
    const card =
        document.createElement("div");

    card.className = "order-card card border-0 shadow-sm";

    const status =
        pedido.status ?? "PENDENTE";

    const quantidadeItens =
        calcularQuantidadeItens(pedido);

    card.innerHTML = `
        <div class="card-body d-flex flex-column flex-md-row justify-content-between align-items-md-center gap-3">
            <div>
                <h2 class="h5 fw-semibold mb-2"
                    data-pedido-id></h2>

                <p class="text-secondary mb-1"
                   data-pedido-data></p>

                <p class="text-secondary mb-0"
                   data-pedido-itens></p>
            </div>

            <span class="status"
                  data-pedido-status></span>

            <strong class="h5 mb-0"
                    data-pedido-total></strong>
        </div>
    `;

    card.querySelector("[data-pedido-id]").textContent =
        `Pedido #${pedido.id}`;

    card.querySelector("[data-pedido-data]").textContent =
        `Data: ${formatarDataPedido(pedido.criadoEm)}`;

    card.querySelector("[data-pedido-itens]").textContent =
        `Itens: ${quantidadeItens}`;

    card.querySelector("[data-pedido-total]").textContent =
        formatarMoeda(pedido.valorTotal);

    configurarStatusPedido(
        card.querySelector("[data-pedido-status]"),
        status
    );

    return card;
}


function formatarDataPedido(dataPedido) {
    if (!dataPedido) {
        return "-";
    }

    const data =
        new Date(dataPedido);

    if (Number.isNaN(data.getTime())) {
        return "-";
    }

    return FORMATO_DATA.format(data);
}


function calcularQuantidadeItens(pedido) {
    if (!Array.isArray(pedido.itens)) {
        return 0;
    }

    return pedido.itens.reduce(
        (total, item) => total + Number(item.quantidade ?? 0),
        0
    );
}


function configurarStatusPedido(
    badge,
    status
) {
    const statusFinalizado =
        ["FINALIZADO", "CONFIRMADO"].includes(status);

    badge.textContent = status;
    badge.classList.add(
        statusFinalizado
            ? "finalizado"
            : "processando"
    );
}


function mostrarErroPedidos() {
    const lista =
        document.getElementById("orders-list");

    lista.innerHTML = `
        <div class="empty">
            Não foi possível carregar os pedidos.
        </div>
    `;
}


document.addEventListener(
    "DOMContentLoaded",
    carregarPedidos
);
