jQuery("#login_form").submit((event) => {
    console.log("submit login form");

    event.preventDefault();

    jQuery.ajax({
        method: "POST",
        url: "api/login",
        data: jQuery("#login_form").serialize(),
        dataType: "json",
        success: (resultData) => handleLoginResult(resultData),
        error: (xhr, status, error) => {
            console.log("Login request failed");
            console.log(xhr.responseText);
            alert("Login failed: " + xhr.responseText);
        }
    });
});

function handleLoginResult(resultData) {
    console.log("handle login response");
    console.log(resultData);

    if (resultData["status"] === "success") {
        window.location.replace("index.html");
    } else {
        jQuery("#login_error_message").text(resultData["message"]);
    }
}
