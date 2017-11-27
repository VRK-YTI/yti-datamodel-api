package fi.vm.yti.datamodel.api.config;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.IOException;

public class AuthenticationFilter implements Filter {

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {

        if (servletRequest instanceof HttpServletRequest) {

            HttpServletRequest request = (HttpServletRequest) servletRequest;
            HttpSession session = request.getSession();
            ShibbolethAuthenticationDetails authenticationDetails = resolveAuthenticationDetails(request);

            if (authenticationDetails.isAuthenticated()) {
                AuthenticationHandler.initialize(session, authenticationDetails);
            } else {
                AuthenticationHandler.remove(session);
            }
        }

        filterChain.doFilter(servletRequest, servletResponse);
    }

    private static ShibbolethAuthenticationDetails resolveAuthenticationDetails(HttpServletRequest request) {

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
