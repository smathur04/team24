jQuery("#add_movie_form").submit((event) => {
    event.preventDefault();
    const formData = jQuery("#add_movie_form").serialize();

    jQuery.ajax({
        method: "POST",
        url: "../api/add-movie",
        data: formData,
        dataType: "json",
        success: (resultData) => {
            jQuery("#add_movie_message").text(resultData.message);
        },
        error: () => {
            jQuery("#add_movie_message").text("Failed to add movie.");
        }
    });
});
