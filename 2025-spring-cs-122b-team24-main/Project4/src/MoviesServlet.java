import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import jakarta.servlet.http.HttpSession;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@WebServlet(name = "MoviesServlet", urlPatterns = "/api/movies")
public class MoviesServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private DataSource dataSource;

    public void init(ServletConfig config) {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/readDB");
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        HttpSession session = request.getSession();

        try (Connection conn = dataSource.getConnection()) {
            String title = request.getParameter("title");
            String year = request.getParameter("year");
            String director = request.getParameter("director");
            String star = request.getParameter("star");
            String genre = request.getParameter("genre");
            String firstLetter = request.getParameter("firstLetter");

            String sort = request.getParameter("sort");
            String limitParam = request.getParameter("limit");
            String pageParam = request.getParameter("page");

            if (title == null && year == null && director == null && star == null && genre == null && firstLetter == null && sort == null && limitParam == null && pageParam == null) {
                title = (String) session.getAttribute("title");
                year = (String) session.getAttribute("year");
                director = (String) session.getAttribute("director");
                star = (String) session.getAttribute("star");
                genre = (String) session.getAttribute("genre");
                firstLetter = (String) session.getAttribute("firstLetter");
                sort = (String) session.getAttribute("sort");
                limitParam = (String) session.getAttribute("limit");
                pageParam = (String) session.getAttribute("page");
            } else {
                session.setAttribute("title", title);
                session.setAttribute("year", year);
                session.setAttribute("director", director);
                session.setAttribute("star", star);
                session.setAttribute("genre", genre);
                session.setAttribute("firstLetter", firstLetter);
                session.setAttribute("sort", sort);
                session.setAttribute("limit", limitParam);
                session.setAttribute("page", pageParam);
            }

            String sort1 = "title";
            String order1 = "ASC";
            String sort2 = "rating";
            String order2 = "ASC";

            if (sort != null) {
                String[] parts = sort.split("_");
                if (parts.length == 4) {
                    sort1 = parts[0];
                    order1 = parts[1].toUpperCase();
                    sort2 = parts[2];
                    order2 = parts[3].toUpperCase();
                }
            }

            int limit = (limitParam != null) ? Integer.parseInt(limitParam) : 10;
            int page = (pageParam != null) ? Integer.parseInt(pageParam) : 1;
            int offset = (page - 1) * limit;

            StringBuilder queryBuilder = new StringBuilder(
                    "SELECT m.id, m.title, m.year, m.director, " +
                            "GROUP_CONCAT(DISTINCT g.name ORDER BY g.name SEPARATOR ', ') AS genres, " +
                            "GROUP_CONCAT(DISTINCT CONCAT(s.name, '::', s.id) ORDER BY s.star_count DESC, s.name ASC SEPARATOR ', ') AS stars, " +
                            "r.rating " +
                            "FROM movies m " +
                            "LEFT JOIN genres_in_movies gm ON m.id = gm.movieId " +
                            "LEFT JOIN genres g ON gm.genreId = g.id " +
                            "LEFT JOIN stars_in_movies sm ON m.id = sm.movieId " +
                            "LEFT JOIN ( " +
                            "   SELECT s.id, s.name, COUNT(sim.movieId) AS star_count " +
                            "   FROM stars s " +
                            "   LEFT JOIN stars_in_movies sim ON s.id = sim.starId " +
                            "   GROUP BY s.id, s.name " +
                            ") AS s ON sm.starId = s.id " +
                            "LEFT JOIN ratings r ON m.id = r.movieId " +
                            "WHERE 1=1 "
            );

            if (title != null && !title.trim().isEmpty()) {
                queryBuilder.append("AND m.title LIKE ? ");
            }
            if (year != null && !year.trim().isEmpty()) {
                queryBuilder.append("AND m.year = ? ");
            }
            if (director != null && !director.trim().isEmpty()) {
                queryBuilder.append("AND m.director LIKE ? ");
            }
            if (star != null && !star.trim().isEmpty()) {
                queryBuilder.append("AND s.name LIKE ? ");
            }
            if (genre != null && !genre.trim().isEmpty()) {
                queryBuilder.append("AND m.id IN (SELECT gm.movieId FROM genres_in_movies gm JOIN genres g2 ON gm.genreId = g2.id WHERE g2.name = ?) ");
            }
            if (firstLetter != null && !firstLetter.trim().isEmpty()) {
                if (firstLetter.equals("*")) {
                    queryBuilder.append("AND (LEFT(m.title, 1) REGEXP '[^a-zA-Z0-9]') ");
                } else {
                    queryBuilder.append("AND m.title LIKE ? ");
                }
            }

            queryBuilder.append("GROUP BY m.id, m.title, m.year, m.director, r.rating ");
            queryBuilder.append("ORDER BY " + sort1 + " " + order1 + ", " + sort2 + " " + order2 + " ");
            queryBuilder.append("LIMIT ? OFFSET ?");

            PreparedStatement statement = conn.prepareStatement(queryBuilder.toString());

            int paramIndex = 1;
            if (title != null && !title.trim().isEmpty()) {
                statement.setString(paramIndex++, "%" + title + "%");
            }
            if (year != null && !year.trim().isEmpty()) {
                statement.setInt(paramIndex++, Integer.parseInt(year));
            }
            if (director != null && !director.trim().isEmpty()) {
                statement.setString(paramIndex++, "%" + director + "%");
            }
            if (star != null && !star.trim().isEmpty()) {
                statement.setString(paramIndex++, "%" + star + "%");
            }
            if (genre != null && !genre.trim().isEmpty()) {
                statement.setString(paramIndex++, genre);
            }
            if (firstLetter != null && !firstLetter.trim().isEmpty() && !firstLetter.equals("*")) {
                statement.setString(paramIndex++, firstLetter + "%");
            }
            statement.setInt(paramIndex++, limit);
            statement.setInt(paramIndex++, offset);

            ResultSet rs = statement.executeQuery();
            JsonArray jsonArray = new JsonArray();

            while (rs.next()) {
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("movie_id", rs.getString("id"));
                jsonObject.addProperty("title", rs.getString("title"));
                jsonObject.addProperty("year", rs.getString("year"));
                jsonObject.addProperty("director", rs.getString("director"));
                jsonObject.addProperty("genres", rs.getString("genres"));
                jsonObject.addProperty("stars", rs.getString("stars"));
                jsonObject.addProperty("rating", rs.getString("rating"));
                jsonArray.add(jsonObject);
            }

            rs.close();
            statement.close();
            out.write(jsonArray.toString());
            response.setStatus(200);

        } catch (Exception e) {
            JsonObject errorObject = new JsonObject();
            errorObject.addProperty("errorMessage", e.getMessage());
            out.write(errorObject.toString());
            response.setStatus(500);
        } finally {
            out.close();
        }
    }
}