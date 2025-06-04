$(document).ready(function() {
    $.ajax({
        method: "GET",
        url: "api/confirmation",
        success: function(response) {
            let summaryDiv = $("#order_summary");
            let html = "<h3>Order Summary</h3>";
            html += "<p>Sale ID(s): " + response.saleIds.join(", ") + "</p>";
            html += "<ul>";
            response.cartItems.forEach(item => {
                html += `<li>${item.title} - Quantity: ${item.quantity}</li>`;
            });
            html += "</ul>";
            html += `<h4>Total Paid: $${response.total.toFixed(2)}</h4>`;
            summaryDiv.html(html);
        },
        error: function() {
            $("#order_summary").html("<p>Error loading order details.</p>");
        }
    });
});
