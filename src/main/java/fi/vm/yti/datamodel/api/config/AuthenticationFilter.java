package fi.vm.yti.datamodel.api.config;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AuthenticationFilter implements Filter {

    private static final Logger logger = Logger.getLogger(AuthenticationFilter.class.getName());

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {

        if (servletRequest instanceof HttpServletRequest) {

            HttpServletRequest request = (HttpServletRequest) servletRequest;
            HttpSession session = request.getSession();

            AuthenticationHandler.initializeUser(session, resolveAuthenticationDetails(request));
        }

        filterChain.doFilter(servletRequest, servletResponse);
    }

    private static ShibbolethAuthenticationDetails resolveAuthenticationDetails(HttpServletRequest request) {

        // TODO remove logging after debugging is done
        logger.log(Level.INFO, "Debug mode: " + ApplicationProperties.getDebugMode());
        logger.log(Level.INFO, "Mail: " + request.getAttribute("mail"));

        if (ApplicationProperties.getDebugMode()) {
            return new ShibbolethAuthenticationDetails(ApplicationProperties.getDebugUserEmail(), ApplicationProperties.getDebugUserFirstname(), ApplicationProperties.getDebugUserLastname());
        } else {
            return new ShibbolethAuthenticationDetails(request);
        }
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void destroy() {
    }
}
