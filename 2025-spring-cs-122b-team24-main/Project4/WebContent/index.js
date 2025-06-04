let cart = $("#cart");

function handleSessionData(resultDataJson) {

    $("#sessionID").text("Session ID: " + resultDataJson["sessionID"]);
    $("#lastAccessTime").text("Last access time: " + resultDataJson["lastAccessTime"]);

    handleCartArray(resultDataJson["previousItems"]);
    populateGenres(resultDataJson["genres"]);
}

function handleCartArray(resultArray) {

    let item_list = $("#item_list");
    let res = "<ul>";
    for (let i = 0; i < resultArray.length; i++) {
        res += "<li>" + resultArray[i] + "</li>";
    }
    res += "</ul>";
    item_list.html("").append(res);
}

function populateGenres(genresArray) {

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

function handleLookup(query, doneCallback) {
    console.log("Autocomplete initiated for query:", query);

    const cached = sessionStorage.getItem(query);
    if (cached) {
        console.log("Using cached results for query:", query);
        const suggestions = JSON.parse(cached);
        console.log("Used suggestion list:", suggestions);
        doneCallback({ suggestions });
        return;
    }

    console.log("Sending AJAX request to backend for:", query);

    jQuery.ajax({
        method: "GET",
        url: "api/movie-suggestion?query=" + encodeURIComponent(query),
        success: function (data) {
            let parsed;
            try {
                parsed = typeof data === "string" ? JSON.parse(data) : data;
            } catch (e) {
                console.error("Failed to parse suggestions:", e);
                doneCallback({ suggestions: [] });
                return;
            }

            if (!Array.isArray(parsed)) {
                console.warn("Unexpected data format:", parsed);
                doneCallback({ suggestions: [] });
                return;
            }

            sessionStorage.setItem(query, JSON.stringify(parsed));
            console.log("Used suggestion list:", parsed);

            doneCallback({ suggestions: parsed });
        },
        error: function (errorData) {
            console.log("AJAX error:", errorData);
        }
    });
}

function handleSelectSuggestion(suggestion) {
    const movieId = suggestion.data;
    window.location.href = "single-movie.html?id=" + encodeURIComponent(movieId);
}

$(document).ready(function () {
    const $titleInput = $("input[name='title']");

    $titleInput.autocomplete({
        lookup: function (query, doneCallback) {
            if (query.length >= 3) {
                handleLookup(query, doneCallback);
            }
        },
        onSelect: function (suggestion) {
            handleSelectSuggestion(suggestion);
        },
        deferRequestBy: 300,
        minChars: 3,
        maxHeight: 300,
        width: 300,
        autoSelectFirst: false
    });

    $titleInput.keypress(function (event) {
        if (event.keyCode === 13) {
            event.preventDefault();

            const $selected = $(".autocomplete-suggestion.autocomplete-selected");

            if ($selected.length > 0) {
                const selectedText = $selected.text().trim();

                for (let key in sessionStorage) {
                    try {
                        const cached = JSON.parse(sessionStorage.getItem(key));
                        if (!Array.isArray(cached)) continue;

                        const match = cached.find(item => item.value === selectedText);
                        if (match) {
                            window.location.href = "single-movie.html?id=" + encodeURIComponent(match.data);
                            return;
                        }
                    } catch (e) {
                        continue;
                    }
                }
            } else {
                const query = $titleInput.val();
                window.location.href = "movie-list.html?title=" + encodeURIComponent(query);
            }
        }
    });

    $("#search-form").submit(function (event) {
        event.preventDefault();
        const formData = $(this).serialize();
        window.location.href = "movie-list.html?" + formData;
    });
});
