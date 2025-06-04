import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;

@WebServlet(name = "ConfirmationServlet", urlPatterns = "/api/confirmation")
public class ConfirmationServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private DataSource dataSource;

    public void init() {
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
        JsonObject responseJson = new JsonObject();

        JsonArray saleIds = (JsonArray) session.getAttribute("saleIds");
        Map<String, Integer> confirmedCart = (Map<String, Integer>) session.getAttribute("confirmedCart");

        responseJson.add("saleIds", saleIds);

        JsonArray cartItemsArray = new JsonArray();
        double total = 0.0;

        try (Connection conn = dataSource.getConnection()) {
            for (String movieId : confirmedCart.keySet()) {
                String query = "SELECT title, price FROM movies WHERE id = ?";
                PreparedStatement statement = conn.prepareStatement(query);
                statement.setString(1, movieId);
                ResultSet rs = statement.executeQuery();

                if (rs.next()) {
                    JsonObject item = new JsonObject();
                    item.addProperty("title", rs.getString("title"));
                    item.addProperty("quantity", confirmedCart.get(movieId));
                    double price = rs.getDouble("price");
                    item.addProperty("price", price);
                    total += price * confirmedCart.get(movieId);
                    cartItemsArray.add(item);
                }

                rs.close();
                statement.close();
            }
        } catch (Exception e) {
            response.setStatus(500);
            responseJson.addProperty("message", "Error retrieving order details.");
            out.write(responseJson.toString());
            return;
        }

        responseJson.add("cartItems", cartItemsArray);
        responseJson.addProperty("total", total);

        out.write(responseJson.toString());
        response.setStatus(200);
    }
}
