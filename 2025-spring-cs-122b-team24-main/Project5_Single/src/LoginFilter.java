import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@WebFilter(filterName = "LoginFilter", urlPatterns = "/*")
public class LoginFilter implements Filter {

    public void init(FilterConfig fConfig) {}

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String uri = httpRequest.getRequestURI();
        HttpSession session = httpRequest.getSession(false);

        boolean isLoggedIn = session != null && session.getAttribute("user") != null;
        boolean isEmployee = isLoggedIn && session.getAttribute("userId") == null;
        boolean isCustomer = isLoggedIn && session.getAttribute("userId") != null;

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
