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
import java.util.HashMap;
import java.util.Map;

@WebServlet(name = "CartServlet", urlPatterns = "/api/cart")
public class CartServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private DataSource dataSource;
    public void init() {
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
        Map<String, Integer> cart = (Map<String, Integer>) session.getAttribute("cart");
        if (cart == null) {
            cart = new HashMap<>();
        }
        JsonArray cartItems = new JsonArray();
        try (Connection conn = dataSource.getConnection()) {
            for (String movieId : cart.keySet()) {
                String query = "SELECT title, price FROM movies WHERE id = ?";
                PreparedStatement statement = conn.prepareStatement(query);
                statement.setString(1, movieId);
                ResultSet rs = statement.executeQuery();

                if (rs.next()) {
                    String title = rs.getString("title");
                    double price = rs.getDouble("price");

                    JsonObject item = new JsonObject();
                    item.addProperty("movieId", movieId);
                    item.addProperty("title", title);
                    item.addProperty("quantity", cart.get(movieId));
                    item.addProperty("price", price);
                    cartItems.add(item);
                }

                rs.close();
                statement.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            JsonObject errorJson = new JsonObject();
            errorJson.addProperty("errorMessage", e.getMessage());
            out.write(errorJson.toString());
            response.setStatus(500);
            return;
        }

        JsonObject responseJson = new JsonObject();
        responseJson.add("cartItems", cartItems);

        out.write(responseJson.toString());
        response.setStatus(200);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        String movieId = request.getParameter("movieId");
        String action = request.getParameter("action");

        if (movieId == null || movieId.isEmpty()) {
            response.setStatus(400);
            JsonObject errorJson = new JsonObject();
            errorJson.addProperty("message", "Missing movieId parameter");
            out.write(errorJson.toString());
            return;
        }

        HttpSession session = request.getSession();
        Map<String, Integer> cart = (Map<String, Integer>) session.getAttribute("cart");
        if (cart == null) {
            cart = new HashMap<>();
        }

        if (action == null || action.isEmpty() || action.equals("increase")) {
            cart.put(movieId, cart.getOrDefault(movieId, 0) + 1);
        } else if (action.equals("decrease")) {
            int quantity = cart.getOrDefault(movieId, 0);
            if (quantity > 1) {
                cart.put(movieId, quantity - 1);
            } else {
                cart.remove(movieId);
            }
        } else if (action.equals("remove")) {
            cart.remove(movieId);
        }

        session.setAttribute("cart", cart);

        JsonObject responseJson = new JsonObject();
        responseJson.addProperty("message", "Cart updated");
        out.write(responseJson.toString());
        response.setStatus(200);
    }
}
