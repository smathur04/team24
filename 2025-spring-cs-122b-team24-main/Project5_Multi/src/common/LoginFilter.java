package src.common;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import io.jsonwebtoken.Claims;

@WebFilter(filterName = "LoginFilter", urlPatterns = "/*")
public class LoginFilter implements Filter {

    public void init(FilterConfig fConfig) {}

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String uri = httpRequest.getRequestURI();

        String token = JwtUtil.getCookieValue(httpRequest, "jwtToken");
        Claims claims = JwtUtil.validateToken(token);

        boolean isLoggedIn = claims != null && claims.get("logged") != null;
        boolean isEmployee = isLoggedIn && claims.get("employeeId") != null;
        boolean isCustomer = isLoggedIn && claims.get("customerId") != null;

        boolean isLoginPage = uri.endsWith("login.html") || uri.contains("api/login");
        boolean isDashboardLoginPage = uri.contains("_dashboard/login.html");
        boolean isDashboardPage = uri.contains("_dashboard") && !isDashboardLoginPage;

        boolean isStaticResource =
                uri.endsWith(".css") || uri.endsWith(".js") || uri.endsWith(".png") ||
                        uri.endsWith(".jpg") || uri.endsWith(".jpeg") || uri.endsWith(".webp") ||
                        uri.contains("recaptcha");

        if (isLoginPage || isDashboardLoginPage || isStaticResource) {
            chain.doFilter(request, response);
            return;
        }

        if (!isLoggedIn) {
            if (isDashboardPage) {
                httpResponse.sendRedirect("../_dashboard/login.html");
            } else {
                httpResponse.sendRedirect("login.html");
            }
            return;
        }

        if (isEmployee) {
            chain.doFilter(request, response);
            return;
        }

        if (isCustomer && isDashboardPage) {
            httpResponse.sendRedirect("login.html");
            return;
        }

        chain.doFilter(request, response);
    }

    public void destroy() {}
}
