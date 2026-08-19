const PEDIDO_SERVICE = "http://localhost:8084";


async function carregarPedidos() {

    const lista =
        document.getElementById("orders-list");

    if (!lista) {
        return;
    }

    try {

        const respostaPedidos =
            await fetch(`${PEDIDO_SERVICE}/pedidos`);

        if (!respostaPedidos.ok) {
            throw new Error("Erro ao buscar pedidos");
        }

        const pedidos =
            await respostaPedidos.json();

        lista.innerHTML = "";

        if (pedidos.length === 0) {

            lista.innerHTML = `
                <div class="empty">
                    Nenhum pedido encontrado.
                </div>
            `;

            return;
        }

        pedidos.forEach(criarCardPedido);

    } catch (erro) {

        console.error(
            "Erro ao carregar pedidos:",
            erro
        );

        lista.innerHTML = `
            <div class="empty">
                Não foi possível carregar os pedidos.
            </div>
        `;

    }
}


function criarCardPedido(pedido) {

    const lista =
        document.getElementById("orders-list");

    const card =
        document.createElement("div");

    card.classList.add("order-card");

    const status =
        pedido.status ?? "PENDENTE";

    const statusClasse =
        ["FINALIZADO", "CONFIRMADO"].includes(status)
            ? "finalizado"
            : "processando";

    const total =
        Number(pedido.valorTotal ?? 0);

    const quantidadeItens =
        Array.isArray(pedido.itens)
            ? pedido.itens.length
            : 0;

    card.innerHTML = `
        <div>
            <h3>
                Pedido #${pedido.id}
            </h3>

            <p>
                Data: ${formatarData(pedido.criadoEm)}
            </p>

            <p>
                Itens: ${quantidadeItens}
            </p>
        </div>

        <span class="status ${statusClasse}">
            ${status}
        </span>

        <strong>
            R$ ${total.toFixed(2)}
        </strong>
    `;

    lista.appendChild(card);
}


function formatarData(dataPedido) {

    if (!dataPedido) {
        return "-";
    }

    const data =
        new Date(dataPedido);

    if (Number.isNaN(data.getTime())) {
        return "-";
    }

    return data.toLocaleDateString("pt-BR");
}


document.addEventListener(
    "DOMContentLoaded",
    carregarPedidos
);
