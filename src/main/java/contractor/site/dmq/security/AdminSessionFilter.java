package contractor.site.dmq.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 * Guards every /api/admin/** endpoint with the session set at /login (the "adminEmail"
 * attribute). Everything under that path is blocked with 401 UNLESS it's in PUBLIC_PATHS.
 *
 * This is "secure by default": a new controller method you add under /api/admin/** is
 * automatically protected the moment it exists. If it's meant to be reachable before
 * login (like registration or the PIN endpoints), you must add its exact path here —
 * forgetting to do so fails safe (blocks it) rather than failing open (exposes it).
 *
 * This does NOT touch pages themselves (adminDashboard.html, etc. are still just static
 * files anyone can open) — it only protects the API calls those pages make. A page with
 * no session still loads, but its protected fetch() calls come back 401, which is what
 * adminDashboard.html already treats as "redirect to /adminLogin.html".
 */
@Component
public class AdminSessionFilter implements Filter {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String SESSION_KEY = "adminEmail";

    /** Exact paths under /api/admin/ that must work without a session. */
    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/api/admin/account/register",
            "/api/admin/account/verify",
            "/api/admin/account/login",
            "/api/admin/account/password-reset/request",
            "/api/admin/account/password-reset/status",
            "/api/admin/account/password-reset/resend",
            "/api/admin/account/password-reset/verify-pin",
            "/api/admin/account/password-reset/confirm"
    );

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)throws IOException, ServletException 
    {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        String path = request.getRequestURI();

        boolean isAdminApi = path.startsWith("/api/admin/");
        if (!isAdminApi || PUBLIC_PATHS.contains(path)) {
            chain.doFilter(request, response);
            return;
        }

        HttpSession session = request.getSession(false);
        Object email = session == null ? null : session.getAttribute(SESSION_KEY);
        if (email == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write(MAPPER.writeValueAsString(Map.of(
                    "status", "NOT_AUTHENTICATED",
                    "message", "Please log in to continue.")));
            return;
        }

        chain.doFilter(request, response);
    }
}
