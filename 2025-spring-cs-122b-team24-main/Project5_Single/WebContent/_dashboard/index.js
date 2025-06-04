fetch("../api/metadata")
    .then(res => res.json())
    .then(data => {
        const container = document.getElementById("metadata");

        data.forEach(table => {
            const tableHeader = document.createElement("h3");
            tableHeader.textContent = `Table: ${table.table}`;
            container.appendChild(tableHeader);

            const ul = document.createElement("ul");
            table.columns.forEach(col => {
                const li = document.createElement("li");
                li.textContent = `${col.name} (${col.type})`;
                ul.appendChild(li);
            });
            container.appendChild(ul);
        });
    })
    .catch(err => {
        document.getElementById("metadata").textContent = "Failed to load metadata.";
        console.error(err);
    });
