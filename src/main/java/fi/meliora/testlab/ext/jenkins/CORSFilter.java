package fi.meliora.testlab.ext.jenkins;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 * Adds CORS headers to Jenkins response. Adapted from CORS-filter and included in
 * this plugin because the original plugin is not published in public repositories.
 *
 * @author Marko Kanala
 */
public class CORSFilter implements Filter {
    private final static Logger log = Logger.getLogger(CORSFilter.class.getName());

    private boolean enabled = false;
    private List<String> origins = null;     // by default, no origins allowed

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<String> getOrigins() {
        return origins;
    }

    public void setOrigins(List<String> origins) {
        this.origins = origins;
    }

    private static final String CORS_HANDLE_OPTIONS_METHOD = System.getProperty("cors.options", "true");
    private static final String CORS_METHODS = System.getProperty("cors.methods", "GET, POST, PUT, DELETE");
    private static final String CORS_HEADERS = System.getProperty("cors.headers", "Authorization, .crumb, Origin");
    private static final String CORS_CREDENTIALS = System.getProperty("cors.credentials", "true");

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if(enabled) {
            log.fine("doFilter: CORSFilter enabled for origins " + origins);

            if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
                if(origins != null && origins.size() > 0) {
                    final HttpServletRequest req = (HttpServletRequest)request;
                    final HttpServletResponse resp = (HttpServletResponse) response;

                    String origin = req.getHeader("Origin");
                    if(origin != null) {
                        log.fine("doFilter: CORSFilter processing request for Origin: " + origin);

                        //
                        // note: as the cors header supports only a single Origin value, we
                        //  support multiple values here by echoing back the valid Origin values
                        //  ourselves
                        //
                        if(origins.contains("*") || origins.contains(origin)) {
                            log.fine("doFilter: CORSFilter adding headers.");
                            resp.addHeader("Access-Control-Allow-Origin", origin);
                            resp.addHeader("Access-Control-Allow-Methods", CORS_METHODS);
                            resp.addHeader("Access-Control-Allow-Headers", CORS_HEADERS);
                            resp.addHeader("Access-Control-Allow-Credentials", CORS_CREDENTIALS);
                            if(Boolean.valueOf(CORS_HANDLE_OPTIONS_METHOD)) {
                                if("OPTIONS".equals(req.getMethod())) {
                                    resp.setStatus(200);
                                    return;
                                }
                            }
                        }
                    }
                }
            }
        }
        chain.doFilter(request, response);
    }

    /**
     * Called by the web container to indicate to a filter that it is being placed into
     * service. The servlet container calls the init method exactly once after instantiating the
     * filter. The init method must complete successfully before the filter is asked to do any
     * filtering work. <br><br>
     * The web container cannot place the filter into service if the init method either<br>
     * 1.Throws a ServletException <br>
     * 2.Does not return within a time period defined by the web container
     *
     * @param filterConfig
     */
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        log.fine("CORSFilter.init()");
    }

    /**
     * Called by the web container to indicate to a filter that it is being taken out of service. This
     * method is only called once all threads within the filter's doFilter method have exited or after
     * a timeout period has passed. After the web container calls this method, it will not call the
     * doFilter method again on this instance of the filter. <br><br>
     * <p/>
     * This method gives the filter an opportunity to clean up any resources that are being held (for
     * example, memory, file handles, threads) and make sure that any persistent state is synchronized
     * with the filter's current state in memory.
     */
    @Override
    public void destroy() {
        log.fine("CORSFilter.destroy()");
    }
}
