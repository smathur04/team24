import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.ServletConfig;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;

@WebServlet(name = "IndexServlet", urlPatterns = "/api/index")
public class IndexServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private DataSource dataSource;

    public void init(ServletConfig config) {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/moviedb");
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        HttpSession session = request.getSession();
        String sessionId = session.getId();
        long lastAccessTime = session.getLastAccessedTime();

        JsonObject responseJsonObject = new JsonObject();
        responseJsonObject.addProperty("sessionID", sessionId);
        responseJsonObject.addProperty("lastAccessTime", new Date(lastAccessTime).toString());

        // Previous items (cart) handling
        ArrayList<String> previousItems = (ArrayList<String>) session.getAttribute("previousItems");
        if (previousItems == null) {
            previousItems = new ArrayList<>();
        }
        JsonArray previousItemsJsonArray = new JsonArray();
        for (String item : previousItems) {
            previousItemsJsonArray.add(item);
        }
        responseJsonObject.add("previousItems", previousItemsJsonArray);

        // Fetch genres
        JsonArray genreJsonArray = new JsonArray();
        try (Connection conn = dataSource.getConnection();
             Statement statement = conn.createStatement();
             ResultSet rs = statement.executeQuery("SELECT name FROM genres ORDER BY name ASC")) {

            while (rs.next()) {
                genreJsonArray.add(rs.getString("name"));
            }

        } catch (Exception e) {
            JsonObject errorObject = new JsonObject();
            errorObject.addProperty("errorMessage", e.getMessage());
            out.write(errorObject.toString());
            response.setStatus(500);
            return;
        }

        responseJsonObject.add("genres", genreJsonArray);

        out.write(responseJsonObject.toString());
        response.setStatus(200);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        String item = request.getParameter("item");
        HttpSession session = request.getSession();
        ArrayList<String> previousItems = (ArrayList<String>) session.getAttribute("previousItems");
        if (previousItems == null) {
            previousItems = new ArrayList<>();
        }
        synchronized (previousItems) {
            previousItems.add(item);
        }
        session.setAttribute("previousItems", previousItems);

        JsonObject responseJsonObject = new JsonObject();
        JsonArray previousItemsJsonArray = new JsonArray();
        for (String prevItem : previousItems) {
            previousItemsJsonArray.add(prevItem);
        }
        responseJsonObject.add("previousItems", previousItemsJsonArray);

        out.write(responseJsonObject.toString());
        response.setStatus(200);
    }
}
