let cart = $("#cart");

function handleSessionData(resultDataJson) {
    console.log("handle session response");
    console.log(resultDataJson);

    $("#sessionID").text("Session ID: " + resultDataJson["sessionID"]);
    $("#lastAccessTime").text("Last access time: " + resultDataJson["lastAccessTime"]);

    handleCartArray(resultDataJson["previousItems"]);
    populateGenres(resultDataJson["genres"]);
}

function handleCartArray(resultArray) {
    console.log(resultArray);
    let item_list = $("#item_list");
    let res = "<ul>";
    for (let i = 0; i < resultArray.length; i++) {
        res += "<li>" + resultArray[i] + "</li>";
    }
    res += "</ul>";
    item_list.html("");
    item_list.append(res);
}

function populateGenres(genresArray) {
    console.log("Populating genres...");
    let genresDiv = $(".genres");
    genresDiv.empty();

    for (let i = 0; i < genresArray.length; i++) {
        let genreName = genresArray[i];
        let genreLink = $("<a></a>")
            .attr("href", "movie-list.html?genre=" + encodeURIComponent(genreName))
            .text(genreName);
        genresDiv.append(genreLink);
    }
}

function handleCartInfo(cartEvent) {
    console.log("submit cart form");
    cartEvent.preventDefault();

    $.ajax("api/index", {
        method: "POST",
        data: cart.serialize(),
        success: resultDataString => {
            let resultDataJson = JSON.parse(resultDataString);
            handleCartArray(resultDataJson["previousItems"]);
        }
    });

    cart[0].reset();
}

$.ajax("api/index", {
    method: "GET",
    success: handleSessionData
});

cart.submit(handleCartInfo);
