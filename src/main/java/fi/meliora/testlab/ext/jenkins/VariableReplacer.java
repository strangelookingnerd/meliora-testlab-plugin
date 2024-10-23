package fi.meliora.testlab.ext.jenkins;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A simple replacer class to replace tags with build environment variables in a String.
 *
 * @author Meliora Ltd
 */
public class VariableReplacer {
    private final static Logger log = Logger.getLogger(VariableReplacer.class.getName());

    protected Map<String, String> vars = null;

    /**
     * Creates a new variable replacer by combining key-value maps provided.
     *
     * @param vars variables
     */
    @SuppressWarnings("unchecked")
    public VariableReplacer(Map<String, String>... vars) {
        if(vars != null) {
            if(vars.length == 1) {
                this.vars = vars[0];
            } else {
                this.vars = new HashMap<>();
                for(Map<String, String> v : vars) {
                    this.vars.putAll(v);
                }
            }
        }
    }

    /**
     * Replaces all tags in format ${BUILD_NUMBER} with matching value from envVars.
     * If variable is missing, tag is left as it is.
     *
     * @param src source String
     * @return String with variables replaced
     */
    public String replace(String src) {
        if(vars == null || src == null || src.isEmpty())
            return src;

        String result = src;
        for(Map.Entry<String, String> e : vars.entrySet()) {
            String key = e.getKey();
            String value = e.getValue();
            String tag = "\\$\\{" + key + "\\}";
            result = result.replaceAll(tag, value);
        }

        if(log.isLoggable(Level.FINE) && !result.equals(src)) {
            log.fine("'" + src + "' replaced as '" + result + "'");
        }

        return result;
    }

    public Map<String, String> getVars() {
        return vars;
    }
}
