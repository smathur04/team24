jQuery("#add_star_form").submit((event) => {
    event.preventDefault();
    const formData = jQuery("#add_star_form").serialize();

    jQuery.ajax({
        method: "POST",
        url: "../api/add-star",
        data: formData,
        dataType: "json",
        success: (resultData) => {
            jQuery("#add_star_message").text(resultData.message);
        },
        error: (xhr, status, error) => {
            jQuery("#add_star_message").text("Failed to add star.");
        }
    });
});
