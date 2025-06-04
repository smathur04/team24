package src.movies;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import javax.naming.Context;
import javax.naming.InitialContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;

@WebServlet(name = "MovieSuggestionServlet", urlPatterns = "/api/movie-suggestion")
public class MovieSuggestionServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private DataSource dataSource;

    public void init() {
        try {
            Context initContext = new InitialContext();
            Context envContext  = (Context) initContext.lookup("java:/comp/env");
            dataSource = (DataSource) envContext.lookup("jdbc/readDB");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");

        PrintWriter out = response.getWriter();

        try (Connection conn = dataSource.getConnection()) {
            String query = request.getParameter("query");
            if (query == null || query.trim().isEmpty()) {
                out.write("[]");
                return;
            }

            String[] terms = query.trim().split("\\s+");
            StringBuilder booleanQuery = new StringBuilder();
            for (String term : terms) {
                booleanQuery.append("+").append(term).append("* ");
            }

            String fullTextQuery = booleanQuery.toString().trim();
            System.out.println("Full-text query: " + fullTextQuery);

            String sql = "SELECT id, title FROM movies WHERE MATCH(title) AGAINST (? IN BOOLEAN MODE) LIMIT 10";

            try (PreparedStatement statement = conn.prepareStatement(sql)) {
                statement.setString(1, fullTextQuery);

                try (ResultSet rs = statement.executeQuery()) {
                    JsonArray suggestions = new JsonArray();

                    while (rs.next()) {
                        JsonObject movie = new JsonObject();
                        movie.addProperty("value", rs.getString("title"));
                        movie.addProperty("data", rs.getString("id"));
                        suggestions.add(movie);
                    }

                    out.write(suggestions.toString());
                }
            }

        } catch (Exception e) {
            JsonObject error = new JsonObject();
            error.addProperty("errorMessage", e.getMessage());
            out.write(error.toString());
            response.setStatus(500);
        }

        out.close();
    }
}