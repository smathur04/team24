package src.movies;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
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
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@WebServlet(name = "PaymentServlet", urlPatterns = "/api/payment")
public class PaymentServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private DataSource dataSource;
    public void init() {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/writeDB");
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        String firstName = request.getParameter("firstName");
        String lastName = request.getParameter("lastName");
        String ccId = request.getParameter("ccId");
        String expiration = request.getParameter("expiration");

        JsonObject responseJson = new JsonObject();

        try (Connection conn = dataSource.getConnection()) {
            String query = "SELECT * FROM creditcards WHERE id = ? AND firstName = ? AND lastName = ? AND expirationDate = ?";
            PreparedStatement statement = conn.prepareStatement(query);
            statement.setString(1, ccId);
            statement.setString(2, firstName);
            statement.setString(3, lastName);
            statement.setString(4, expiration);

            ResultSet rs = statement.executeQuery();

            if (!rs.next()) {
                responseJson.addProperty("success", false);
                responseJson.addProperty("message", "Invalid credit card information.");
                out.write(responseJson.toString());
                return;
            }

            rs.close();
            statement.close();

            HttpSession session = request.getSession();
            Map<String, Integer> cart = (Map<String, Integer>) session.getAttribute("cart");
            if (cart == null || cart.isEmpty()) {
                responseJson.addProperty("success", false);
                responseJson.addProperty("message", "Shopping cart is empty.");
                out.write(responseJson.toString());
                return;
            }

            Integer customerId = (Integer) session.getAttribute("userId");

            String insertQuery = "INSERT INTO sales (customerId, movieId, saleDate) VALUES (?, ?, ?)";
            PreparedStatement insertStatement = conn.prepareStatement(insertQuery, PreparedStatement.RETURN_GENERATED_KEYS);

            Timestamp now = Timestamp.from(Instant.now());

            JsonArray saleArray = new JsonArray();
            for (Map.Entry<String, Integer> entry : cart.entrySet()) {
                String movieId = entry.getKey();
                int quantity = entry.getValue();
                for (int i = 0; i < quantity; i++) {
                    insertStatement.setInt(1, customerId);
                    insertStatement.setString(2, movieId);
                    insertStatement.setTimestamp(3, now);
                    int affectedRows = insertStatement.executeUpdate();
                    if (affectedRows == 0) {
                        throw new RuntimeException("Creating sale failed, no rows affected.");
                    }
                    try (ResultSet generatedKeys = insertStatement.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            saleArray.add(new JsonPrimitive(generatedKeys.getInt(1)));
                        } else {
                            throw new RuntimeException("Creating sale failed, no ID obtained.");
                        }
                    }
                }
            }

            insertStatement.close();

            session.setAttribute("saleIds", saleArray);
            session.setAttribute("confirmedCart", cart);
            session.setAttribute("cart", new HashMap<>());

            responseJson.addProperty("success", true);
            out.write(responseJson.toString());

        } catch (Exception e) {
            e.printStackTrace();
            responseJson.addProperty("success", false);
            responseJson.addProperty("message", "Internal server error.");
            out.write(responseJson.toString());
        }
    }
}
