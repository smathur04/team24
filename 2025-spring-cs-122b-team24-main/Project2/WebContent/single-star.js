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
    console.log("handleResult: populating star info from resultData");

    let starInfoElement = jQuery("#star_info");
    starInfoElement.append("<p>Star Name: " + resultData[0]["star_name"] + "</p>");

    console.log("handleResult: populating movie table from resultData");

    let movieTableBodyElement = jQuery("#movie_table_body");

    for (let i = 0; i < resultData.length; i++) {
        let birth = resultData[i]["birth_year"];
        if (!birth || birth.trim() === "null") {
            birth = "N/A";
        }
        let v_movies_raw = resultData[i]["movies"].split(', ');
        let all_movies_links = v_movies_raw.map(movie => {
            let [name, id] = movie.split("::");
            return '<a href="single-movie.html?id=' + id + '">' + name + '</a>';
        }).join(', ');

        let rowHTML = "<tr>";
        rowHTML += "<th>" + resultData[i]["star_name"] + "</th>";
        rowHTML += "<th>" + birth + "</th>";
        rowHTML += "<th>" + all_movies_links + "</th>";
        rowHTML += "</tr>";

        movieTableBodyElement.append(rowHTML);
    }
}

let starId = getParameterByName('id');

jQuery.ajax({
    dataType: "json",
    method: "GET",
    url: "api/single-star?id=" + starId,
    success: (resultData) => handleResult(resultData)
});
