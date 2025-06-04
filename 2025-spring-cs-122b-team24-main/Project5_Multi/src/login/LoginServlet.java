package src.login;
import src.common.JwtUtil;
import com.google.gson.JsonObject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jasypt.util.password.StrongPasswordEncryptor;
import java.util.Date;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

@WebServlet(name = "LoginServlet", urlPatterns = "/api/login")
public class LoginServlet extends HttpServlet {
    private static final long serialVersionUID = 4L;

    private DataSource dataSource;

    public void init() throws ServletException {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/readDB");
        } catch (NamingException e) {
            throw new ServletException(e);
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        JsonObject responseJsonObject = new JsonObject();

        String gRecaptchaResponse = request.getParameter("g-recaptcha-response");

        try {
            RecaptchaVerifyUtils.verify(gRecaptchaResponse);
        } catch (Exception e) {
            responseJsonObject.addProperty("status", "fail");
            responseJsonObject.addProperty("message", "CAPTCHA verification failed.");
            out.write(responseJsonObject.toString());
            out.close();
            return;
        }

        // === User Login Verification ===
        String email = request.getParameter("email");
        String password = request.getParameter("password");

        try (Connection conn = dataSource.getConnection()) {
            String userType = request.getParameter("userType");
            String table = "customers";
            if ("employees".equals(userType)) {
                table = "employees";
            }
            String query = "SELECT * FROM " + table + " WHERE email = ?";
            PreparedStatement statement = conn.prepareStatement(query);
            statement.setString(1, email);

            ResultSet rs = statement.executeQuery();

            boolean success = false;
            if (rs.next()) {
                String encryptedPassword = rs.getString("password");
                success = new StrongPasswordEncryptor().checkPassword(password, encryptedPassword);
            }
            if (success) {
                String subject = email;

                Map<String, Object> claims = new HashMap<>();

                DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                claims.put("loginTime", dateFormat.format(new Date()));
                claims.put("email", email);
                claims.put("user", email);
                claims.put("logged", "yep");
                if (!"employees".equals(userType)) {
                    int customerId = rs.getInt("id");
                    claims.put("customerId", customerId);
                    claims.put("employeeId", null);
                }
                else{
                    claims.put("customerId", null);
                    claims.put("employeeId", "abc");
                }

                String token = JwtUtil.generateToken(subject, claims);
                JwtUtil.updateJwtCookie(request, response, token);

                responseJsonObject.addProperty("status", "success");
            } else {
                responseJsonObject.addProperty("status", "fail");
                responseJsonObject.addProperty("message", "Invalid email or password.");
            }

            rs.close();
            statement.close();
        } catch (Exception e) {
            e.printStackTrace();
            responseJsonObject.addProperty("status", "fail");
            responseJsonObject.addProperty("message", "Internal server error.");
        }

        out.write(responseJsonObject.toString());
        out.close();
    }
}