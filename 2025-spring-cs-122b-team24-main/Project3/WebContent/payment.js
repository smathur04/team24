function loadCartTotal() {
    $.ajax({
        method: "GET",
        url: "api/cart",
        success: function(response) {
            let total = 0.0;
            response.cartItems.forEach(item => {
                total += item.price * item.quantity;
            });
            $("#cart_total").text(`Total: $${total.toFixed(2)}`);
        }
    });
}

$("#payment-form").submit(function(event) {
    event.preventDefault();

    $.ajax({
        method: "POST",
        url: "api/payment",
        data: $(this).serialize(),
        success: function(response) {
            if (response.success) {
                window.location.href = "confirmation.html";
            } else {
                $("#payment_error").text(response.message);
            }
        },
        error: function() {
            $("#payment_error").text("An error occurred. Please try again.");
        }
    });
});

$(document).ready(loadCartTotal);
