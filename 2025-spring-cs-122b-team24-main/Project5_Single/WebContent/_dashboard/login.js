jQuery("#login_form").submit((event) => {
    event.preventDefault();

    const captchaResponse = grecaptcha.getResponse();

    if (!captchaResponse) {
        jQuery("#login_error_message").text("Please complete the CAPTCHA.");
        return;
    }

    const userType = jQuery("input[name='userType']").val();
    const formData = jQuery("#login_form").serialize() +
        `&g-recaptcha-response=${captchaResponse}` +
        `&userType=${userType}`;

    jQuery.ajax({
        method: "POST",
        url: "../api/login",
        data: formData,
        dataType: "json",
        success: (resultData) => handleLoginResult(resultData),
        error: (xhr, status, error) => {
            console.log("Login request failed:", xhr.responseText);
            jQuery("#login_error_message").text("Login failed. Please try again.");
            if (typeof grecaptcha !== "undefined") {
                grecaptcha.reset();
            }
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

        if (typeof grecaptcha !== "undefined") {
            grecaptcha.reset();
        }
    }
}

