package fi.vm.yti.datamodel.api.config;

import fi.vm.yti.datamodel.api.model.YtiUser;
import org.apache.commons.lang3.StringUtils;

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

            AuthenticationHandler.initializeUser(session, resolveAuthenticationDetails(request, session));
        }

        filterChain.doFilter(servletRequest, servletResponse);
    }

    private static ShibbolethAuthenticationDetails resolveAuthenticationDetails(HttpServletRequest request, HttpSession session) {

        ShibbolethAuthenticationDetails fakeAuthenticationDetails = resolveFakeAuthenticationDetails(request, session);

        if (fakeAuthenticationDetails != null) {
            return fakeAuthenticationDetails;
        } else {
            return new ShibbolethAuthenticationDetails(request);
        }
    }

    private static ShibbolethAuthenticationDetails resolveFakeAuthenticationDetails(HttpServletRequest request, HttpSession session) {

        if (ApplicationProperties.getDebugMode()) {

            YtiUser user = AuthenticationHandler.getUser(session);

            String mail = request.getParameter("fake.login.mail");
            String firstName = request.getParameter("fake.login.firstName");
            String lastName = request.getParameter("fake.login.lastName");

            String debugUserEmail = ApplicationProperties.getDebugUserEmail();

            if (mail != null) {
                return new ShibbolethAuthenticationDetails(mail, firstName, lastName);
            } else if (!user.isAnonymous()) { // keep previously logged in user still logged in
                return new ShibbolethAuthenticationDetails(user.getEmail(), user.getFirstName(), user.getLastName());
            } else if (StringUtils.isNotEmpty(debugUserEmail)) {
                return new ShibbolethAuthenticationDetails(debugUserEmail, ApplicationProperties.getDebugUserFirstname(), ApplicationProperties.getDebugUserLastname());
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void destroy() {
    }
}
