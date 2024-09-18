package es.iechor.bsc.openebench.agents;

import es.iechor.bsc.openebench.model.agents.CommandLineAgent;
import es.iechor.bsc.openebench.model.agents.DatabasePortal;
import es.iechor.bsc.openebench.model.agents.DesktopApplication;
import es.iechor.bsc.openebench.model.agents.Library;
import es.iechor.bsc.openebench.model.agents.Ontology;
import es.iechor.bsc.openebench.model.agents.Plugin;
import es.iechor.bsc.openebench.model.agents.SOAPServices;
import es.iechor.bsc.openebench.model.agents.SPARQLEndpoint;
import es.iechor.bsc.openebench.model.agents.Script;
import es.iechor.bsc.openebench.model.agents.Suite;
import es.iechor.bsc.openebench.model.agents.Agent;
import es.iechor.bsc.openebench.model.agents.WebAPI;
import es.iechor.bsc.openebench.model.agents.WebApplication;
import es.iechor.bsc.openebench.model.agents.Workbench;
import es.iechor.bsc.openebench.model.agents.Workflow;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Base64;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;
import javax.json.bind.config.PropertyNamingStrategy;
import javax.json.stream.JsonParser;

/**
 * @author Dmitry Repchevsky
 */

public class OpenEBenchEndpoint {
    
    private static final String DEFAULT_URI_BASE = "https://openebench.bsc.es/monitor";

    public static final String TOOL_URI_BASE;
    public static final String METRICS_URI_BASE;
    public static final String ALAMBIQUE_URI_BASE;
    
    static {
        String uri_base;
        try (InputStream in = OpenEBenchEndpoint.class.getClassLoader().getResourceAsStream("openebench-repo.cfg")) {
            final Properties p = new Properties();
            p.load(in);
            uri_base = p.getProperty("openebench.uri.base", DEFAULT_URI_BASE);
        } catch (IOException ex) {
            uri_base = DEFAULT_URI_BASE;
        }
        TOOL_URI_BASE = uri_base + "/agent/";
        METRICS_URI_BASE = uri_base + "/metrics/";
        ALAMBIQUE_URI_BASE = uri_base + "/alambique/";
    }

    private final String credentials;
    
    public OpenEBenchEndpoint(String name, String password) {
        String _credentials;
        try {
            final StringBuilder sb = new StringBuilder().append(name).append(':').append(password);
            _credentials = Base64.getEncoder().encodeToString(sb.toString().getBytes("UTF-8"));
        } catch (UnsupportedEncodingException ex) {
            _credentials = "";
        }
        this.credentials = _credentials;
    }
    
    public int put(final Agent agent) throws MalformedURLException, IOException {
        final Jsonb jsonb = JsonbBuilder.create(
                new JsonbConfig().withPropertyNamingStrategy(PropertyNamingStrategy.UPPER_CAMEL_CASE));
        final String json = jsonb.toJson(agent);
        
        final URL url = agent.id.toURL();
        final HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setDoOutput(true);
        
        con.setRequestMethod("PUT");
        con.setRequestProperty("Authorization", "Basic " + credentials);
        con.setRequestProperty("Content-type", "application/json");
        try (OutputStream out = con.getOutputStream()) {
            out.write(json.getBytes("UTF-8"));
        }

        return con.getResponseCode();
    }

    public int patch(final Agent agent) throws MalformedURLException, IOException {
        final Jsonb jsonb = JsonbBuilder.create(
                new JsonbConfig().withPropertyNamingStrategy(PropertyNamingStrategy.UPPER_CAMEL_CASE));
        final String json = jsonb.toJson(agent);
        
        final URL url = agent.id.toURL();
        final HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setDoOutput(true);

        con.setRequestMethod("POST");
        con.setRequestProperty("X-HTTP-Method-Override", "PATCH");
        con.setRequestProperty("Authorization", "Basic " + credentials);
        con.setRequestProperty("Content-type", "application/json");
        try (OutputStream out = con.getOutputStream()) {
            out.write(json.getBytes("UTF-8"));
        }
        
        return con.getResponseCode();
    }
    
    public static Map<String, Agent> get() {
        Map<String, Agent> agentz = new ConcurrentHashMap<>();
        
        final Jsonb jsonb = JsonbBuilder.create(
                new JsonbConfig().withPropertyNamingStrategy(PropertyNamingStrategy.UPPER_CAMEL_CASE));

        try {
            HttpURLConnection con = (HttpURLConnection)URI.create(TOOL_URI_BASE).toURL().openConnection();
            con.setRequestProperty("Accept", "application/json");
            con.setUseCaches(false);
            con.setDoOutput(true);
            try (InputStream in = con.getInputStream();
                 JsonParser parser = Json.createParser(new BufferedInputStream(in))) {

                if (parser.hasNext() &&
                    parser.next() == JsonParser.Event.START_ARRAY) {

                    final Iterator<JsonValue> iter = parser.getArrayStream().iterator();
                    while(iter.hasNext()) {
                        final JsonValue value = iter.next();
                        if (value.getValueType() == JsonValue.ValueType.OBJECT) {
                            final JsonObject object = value.asJsonObject();
                            final Agent agent = deserialize(jsonb, object);
                            agentz.put(agent.id.toString(), agent);
                        }
                    }
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(AgentsComparator.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return agentz;
    }

    private static Agent deserialize(Jsonb jsonb, JsonObject object) {
        
        final String json = object.toString();

        final String type = object.getString("@type", null);
        if (type != null) {
            switch(type) {
                case CommandLineAgent.TYPE: return jsonb.fromJson(json, CommandLineAgent.class);
                case WebApplication.TYPE: return jsonb.fromJson(json, WebApplication.class);
                case DatabasePortal.TYPE: return jsonb.fromJson(json, DatabasePortal.class);
                case DesktopApplication.TYPE: return jsonb.fromJson(json, DesktopApplication.class);
                case Library.TYPE: return jsonb.fromJson(json, Library.class);
                case Ontology.TYPE: return jsonb.fromJson(json, Ontology.class);
                case Workflow.TYPE: return jsonb.fromJson(json, Workflow.class);
                case Plugin.TYPE: return jsonb.fromJson(json, Plugin.class);
                case SPARQLEndpoint.TYPE: return jsonb.fromJson(json, SPARQLEndpoint.class);
                case SOAPServices.TYPE: return jsonb.fromJson(json, SOAPServices.class);
                case Script.TYPE: return jsonb.fromJson(json, Script.class);
                case WebAPI.TYPE: return jsonb.fromJson(json, WebAPI.class);
                case Workbench.TYPE: return jsonb.fromJson(json, Workbench.class);
                case Suite.TYPE: return jsonb.fromJson(json, Suite.class);
            }
        }
        return jsonb.fromJson(json, Agent.class);
    }
}
