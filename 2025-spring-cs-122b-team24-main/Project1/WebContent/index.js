/**
 * This example is following frontend and backend separation.
 *
 * Before this .js is loaded, the html skeleton is created.
 *
 * This .js performs two steps:
 *      1. Use jQuery to talk to backend API to get the json data.
 *      2. Populate the data to correct html elements.
 */


/**
 * Handles the data returned by the API, read the jsonObject and populate data into html elements
 * @param resultData jsonObject
 */
function handleMoviesResult(resultData) {
    console.log("handleMoviesResult: populating movies table from resultData");

    // Populate the movies table
    // Find the empty table body by id "movis_table_body"
    let moviesTableBodyElement = jQuery("#movies_table_body");

    // Iterate through resultData, no more than 10 entries
    for (let i = 0; i < Math.min(20, resultData.length); i++) {

        let v_genres = resultData[i]["genres"].split(', ');
        let three_genres = v_genres.slice(0, 3).join(', ');

        let v_stars_raw = resultData[i]["stars"].split(', ');
        let three_stars_links = v_stars_raw.slice(0, 3).map(star => {
            let [name, id] = star.split("::");
            return '<a href="single-star.html?id=' + id + '">' + name + '</a>';
        }).join(', ');

        // Concatenate the html tags with resultData jsonObject
        let rowHTML = "";
        rowHTML += "<tr>";
        rowHTML += "<th>" + '<a href="single-movie.html?id=' + resultData[i]['movie_id'] + '">' + resultData[i]["title"] + "</th>";
        rowHTML += "<th>" + resultData[i]["year"] + "</th>";
        rowHTML += "<th>" + resultData[i]["director"] + "</th>";
        rowHTML += "<th>" + three_genres + "</th>";
        rowHTML += "<th>" + three_stars_links + "</th>";
        rowHTML += "<th>" + resultData[i]["rating"] + "</th>";
        rowHTML += "</tr>";

        // Append the row created to the table body, which will refresh the page
        moviesTableBodyElement.append(rowHTML);
    }
}


/**
 * Once this .js is loaded, following scripts will be executed by the browser
 */

// Makes the HTTP GET request and registers on success callback function handleMoviesResult
jQuery.ajax({
    dataType: "json", // Setting return data type
    method: "GET", // Setting request method
    url: "api/movies", // Setting request url
    success: (resultData) => handleMoviesResult(resultData) // Setting callback function to handle data returned successfully by the MoviesServlet
});