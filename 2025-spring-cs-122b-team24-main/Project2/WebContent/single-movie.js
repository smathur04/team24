function getParameterByName(target) {
    let url = window.location.href;
    target = target.replace(/[\[\]]/g, "\\$&");
    let regex = new RegExp("[?&]" + target + "(=([^&#]*)|&|#|$)"),
        results = regex.exec(url);
    if (!results) return null;
    if (!results[2]) return '';
    return decodeURIComponent(results[2].replace(/\+/g, " "));
}

function handleResult(resultData) {
    console.log("handleResult: populating movie info from resultData");

    let movieInfoElement = jQuery("#movie_info");
    movieInfoElement.append("<p>Movie Name: " + resultData[0]["title"] + "</p>");

    console.log("handleResult: populating movie table from resultData");

    let movieTableBodyElement = jQuery("#single_movie_table_body");

    for (let i = 0; i < resultData.length; i++) {
        let rowHTML = "";

        let v_genres_raw = resultData[i]["genres"].split(', ');
        let all_genres_links = v_genres_raw.map(genre => {
            return '<a href="movie-list.html?genre=' + encodeURIComponent(genre) + '">' + genre + '</a>';
        }).join(', ');

        let v_stars_raw = resultData[i]["stars"].split(', ');
        let all_stars_links = v_stars_raw.map(star => {
            let [name, id] = star.split("::");
            return '<a href="single-star.html?id=' + id + '">' + name + '</a>';
        }).join(', ');

        rowHTML += "<tr>";
        rowHTML += "<th>" + resultData[i]["title"] + "</th>";
        rowHTML += "<th>" + resultData[i]["year"] + "</th>";
        rowHTML += "<th>" + resultData[i]["director"] + "</th>";
        rowHTML += "<th>" + all_genres_links + "</th>";
        rowHTML += "<th>" + all_stars_links + "</th>";
        rowHTML += "<th>" + resultData[i]["rating"] + "</th>";
        rowHTML += `<th><button class="add-to-cart" data-movie-id="${resultData[i]['movie_id']}">ADD</button></th>`;
        rowHTML += "</tr>";

        movieTableBodyElement.append(rowHTML);
    }
}

let movieId = getParameterByName('id');

jQuery.ajax({
    dataType: "json",
    method: "GET",
    url: "api/single-movie?id=" + movieId,
    success: (resultData) => handleResult(resultData)
});

$(document).on('click', '.add-to-cart', function () {
    const movieId = $(this).data('movie-id');
    console.log("Adding movie ID to cart:", movieId);

    $.ajax({
        method: "POST",
        url: "api/cart",
        data: { movieId: movieId },
        success: function(response) {
            alert("Added to cart!");
        },
        error: function() {
            alert("Failed to add to cart.");
        }
    });
});