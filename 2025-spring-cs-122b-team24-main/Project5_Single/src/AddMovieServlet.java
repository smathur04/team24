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
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@WebServlet(name = "AddMovieServlet", urlPatterns = "/api/add-movie")
public class AddMovieServlet extends HttpServlet {
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

        String title = request.getParameter("title");
        String yearStr = request.getParameter("year");
        String director = request.getParameter("director");
        String starName = request.getParameter("starName");
        String genre = request.getParameter("genre");
        String birthYearStr = request.getParameter("starBirthYear");
        String priceStr = request.getParameter("price");


        try (Connection conn = dataSource.getConnection()) {
            CallableStatement stmt = conn.prepareCall("{CALL add_movie(?, ?, ?, ?, ?, ?, ?)}");

            stmt.setString(1, title);
            stmt.setInt(2, Integer.parseInt(yearStr));
            stmt.setString(3, director);
            stmt.setString(4, starName);
            stmt.setString(5, genre);

            if (birthYearStr == null || birthYearStr.isEmpty()) {
                stmt.setNull(6, java.sql.Types.INTEGER);
            } else {
                stmt.setInt(6, Integer.parseInt(birthYearStr));
            }

            if (priceStr == null || priceStr.isEmpty()) {
                stmt.setNull(7, java.sql.Types.FLOAT);
            } else {
                stmt.setFloat(7, Float.parseFloat(priceStr));
            }

            boolean hasResults = stmt.execute();
            String message = "No message returned.";

            if (hasResults) {
                ResultSet rs = stmt.getResultSet();
                if (rs.next()) {
                    message = rs.getString(1);
                }
                rs.close();
            }

            res.addProperty("message", message);
            response.setStatus(200);
        } catch (Exception e) {
            res.addProperty("message", "Error: " + e.getMessage());
            response.setStatus(500);
        }

        out.write(res.toString());
    }
}
