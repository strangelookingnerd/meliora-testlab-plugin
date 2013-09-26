package fi.meliora.testlab.ext.crest;

import org.codegist.crest.CRestBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.Class;
import java.lang.String;

/**
 * A helper class to provide crest endpoints for calling external rest endpoints.
 *
 * @author Marko Kanala
 */
public class CrestEndpointFactory {
    private static final Logger log = LoggerFactory.getLogger(CrestEndpointFactory.class);

    private static CrestEndpointFactory instance = new CrestEndpointFactory();

    public static CrestEndpointFactory getInstance() {
        return instance;
    }

    private CrestEndpointFactory() {
    }

    /**
     * Constructs a new CRest endpoint, caches it and returns it for use.
     *
     * @param url
     * @param username
     * @param password
     * @return
     */
    public <T>T getEndpoint(String url, String username, String password, Class<T> endpointClass) {
        T endpoint;
        CRestBuilder b = new CRestBuilder().endpoint(url);
        if(username != null && password != null) {
            b = b.basicAuth(username, password);
        }
        endpoint = b.build().build(endpointClass);

        if(log.isDebugEnabled())
            log.debug("Returning endpoint to {}: {}", url, endpoint);

        return endpoint;
    }

    /**
     * Returns an endpoint to Testlab.
     *
     * This methods peeks for TESTLAB_<companyid in upper case> system environment
     * variable for testlab api address. If none is set a default of https://companyid.melioratestlab.com/api
     * is used.
     *
     * @param companyId
     * @param apiKey
     * @param endpointClass
     * @param <T>
     * @return
     */
    public <T>T getTestlabEndpoint(String companyId, String apiKey, Class<T> endpointClass) {
        String url = System.getProperty(
                "TESTLAB_" + companyId.toUpperCase(),
                "https://" + companyId.toLowerCase() + ".melioratestlab.com/api"
        );
        if(log.isDebugEnabled())
            log.debug("Using url {} as testlab endpoint.", url);
        return getEndpoint(url, companyId, apiKey, endpointClass);
    }

}
