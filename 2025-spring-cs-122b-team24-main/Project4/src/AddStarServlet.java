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
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@WebServlet(name = "AddStarServlet", urlPatterns = "/api/add-star")
public class AddStarServlet extends HttpServlet {
    private DataSource dataSource;

    public void init() throws ServletException {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/writeDB");
        } catch (NamingException e) {
            throw new ServletException(e);
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        JsonObject res = new JsonObject();

        String name = request.getParameter("name");
        String birthYearStr = request.getParameter("birthYear");

        try (Connection conn = dataSource.getConnection()) {
            String newId = null;
            try (PreparedStatement stmt = conn.prepareStatement("SELECT MAX(CAST(SUBSTRING(id, 3) AS UNSIGNED)) AS max_id FROM stars")) {
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    int nextIdNum = rs.getInt("max_id") + 1;
                    newId = String.format("nm%07d", nextIdNum);
                }
                rs.close();
            }

            String insert = "INSERT INTO stars (id, name, birthYear) VALUES (?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(insert)) {
                stmt.setString(1, newId);
                stmt.setString(2, name);
                if (birthYearStr == null || birthYearStr.isEmpty()) {
                    stmt.setNull(3, java.sql.Types.INTEGER);
                } else {
                    stmt.setInt(3, Integer.parseInt(birthYearStr));
                }
                stmt.executeUpdate();
            }

            res.addProperty("message", "Star added with ID " + newId);
            response.setStatus(200);
        } catch (Exception e) {
            res.addProperty("message", "Error: " + e.getMessage());
            response.setStatus(500);
        }

        out.write(res.toString());
    }
}
