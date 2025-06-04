function handleMoviesResult(resultData) {
    console.log("handleMoviesResult: populating movies table from resultData");

    let moviesTableBodyElement = jQuery("#movies_table_body");
    moviesTableBodyElement.empty();

    for (let i = 0; i < resultData.length; i++) {
        let v_genres = resultData[i]["genres"] ? resultData[i]["genres"].split(', ') : [];
        let three_genres = v_genres.slice(0, 3).join(', ');

        let v_stars_raw = resultData[i]["stars"] ? resultData[i]["stars"].split(', ') : [];
        let three_stars_links = v_stars_raw.slice(0, 3).map(star => {
            let [name, id] = star.split("::");
            return `<a href="single-star.html?id=${id}">${name}</a>`;
        }).join(', ');

        let rowHTML = `
            <tr>
                <th><a href="single-movie.html?id=${resultData[i]['movie_id']}">${resultData[i]["title"]}</a></th>
                <th>${resultData[i]["year"]}</th>
                <th>${resultData[i]["director"]}</th>
                <th>${three_genres}</th>
                <th>${three_stars_links}</th>
                <th>${resultData[i]["rating"]}</th>
                <th><button class="add-to-cart" data-movie-id="${resultData[i]['movie_id']}">ADD</button></th>
            </tr>
        `;

        moviesTableBodyElement.append(rowHTML);
    }
}

function getSearchApiUrl() {
    const urlParams = new URLSearchParams(window.location.search);

    let apiUrl = `api/movies?`;
    for (const [key, value] of urlParams.entries()) {
        apiUrl += `&${key}=${encodeURIComponent(value)}`;
    }
    apiUrl = apiUrl.replace("?&", "?");
    console.log("Final API URL:", apiUrl);
    return apiUrl;
}

function getCurrentPage() {
    const urlParams = new URLSearchParams(window.location.search);
    return parseInt(urlParams.get("page")) || 1;
}

function changePage(newPage) {
    const urlParams = new URLSearchParams(window.location.search);
    urlParams.set("page", newPage);
    window.location.search = urlParams.toString();
}

jQuery.ajax({
    dataType: "json",
    method: "GET",
    url: getSearchApiUrl(),
    success: (resultData) => handleMoviesResult(resultData)
});

jQuery("#prev-button").click(function () {
    let currentPage = getCurrentPage();
    if (currentPage > 1) {
        changePage(currentPage - 1);
    }
});

jQuery("#next-button").click(function () {
    let currentPage = getCurrentPage();
    changePage(currentPage + 1);
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

function setSelectedSortOption() {
    const urlParams = new URLSearchParams(window.location.search);
    const sortValue = urlParams.get("sort");
    if (sortValue) {
        const sortSelect = document.querySelector('select[name="sort"]');
        if (sortSelect) {
            sortSelect.value = sortValue;
        }
    }
}
setSelectedSortOption();

