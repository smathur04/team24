import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;

@WebServlet(name = "MetadataServlet", urlPatterns = "/api/metadata")
public class MetadataServlet extends HttpServlet {
    private DataSource dataSource;

    public void init() throws ServletException {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/moviedb");
        } catch (NamingException e) {
            throw new ServletException(e);
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        JsonArray metadataArray = new JsonArray();

        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData dbMeta = conn.getMetaData();
            ResultSet tables = dbMeta.getTables(null, null, "%", new String[] { "TABLE" });

            while (tables.next()) {
                String tableName = tables.getString("TABLE_NAME");
                JsonObject tableObj = new JsonObject();
                tableObj.addProperty("table", tableName);

                JsonArray columns = new JsonArray();
                ResultSet cols = dbMeta.getColumns(null, null, tableName, null);
                while (cols.next()) {
                    JsonObject col = new JsonObject();
                    col.addProperty("name", cols.getString("COLUMN_NAME"));
                    col.addProperty("type", cols.getString("TYPE_NAME"));
                    columns.add(col);
                }
                cols.close();

                tableObj.add("columns", columns);
                metadataArray.add(tableObj);
            }

            tables.close();
        } catch (Exception e) {
            JsonObject error = new JsonObject();
            error.addProperty("error", e.getMessage());
            out.write(error.toString());
            response.setStatus(500);
            return;
        }

        out.write(metadataArray.toString());
        response.setStatus(200);
    }
}

