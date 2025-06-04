function updateCartTable(cartItems) {
    let tableBody = $("#cart_table_body");
    tableBody.empty();

    let grandTotal = 0.0;

    cartItems.forEach(item => {
        let movieTotal = item.price * item.quantity;
        grandTotal += movieTotal;

        let row = `
            <tr>
                <td>${item.title}</td>
                <td>
                    <button class="decrease btn btn-sm btn-secondary" data-id="${item.movieId}">-</button>
                    ${item.quantity}
                    <button class="increase btn btn-sm btn-secondary" data-id="${item.movieId}">+</button>
                </td>
                <td>$${item.price.toFixed(2)}</td>
                <td>$${movieTotal.toFixed(2)}</td>
                <td>
                    <button class="remove btn btn-sm btn-danger" data-id="${item.movieId}">Remove</button>
                </td>
            </tr>
        `;

        tableBody.append(row);
    });

    $("#grand_total").text(`Total: $${grandTotal.toFixed(2)}`);
}

function loadCart() {
    $.ajax({
        method: "GET",
        url: "api/cart",
        success: function (response) {
            updateCartTable(response.cartItems);
        }
    });
}

$(document).on('click', '.increase', function () {
    let movieId = $(this).data('id');
    $.post("api/cart", { movieId: movieId, action: "increase" }, loadCart);
});

$(document).on('click', '.decrease', function () {
    let movieId = $(this).data('id');
    $.post("api/cart", { movieId: movieId, action: "decrease" }, loadCart);
});

$(document).on('click', '.remove', function () {
    let movieId = $(this).data('id');
    $.post("api/cart", { movieId: movieId, action: "remove" }, loadCart);
});

$("#proceed-button").click(function () {
    window.location.href = "payment.html";
});

$(document).ready(loadCart);
