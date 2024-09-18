package es.iechor.bsc.openebench.checker.bioagents;

import es.bsc.inb.iechor.openebench.model.metrics.Metrics;
import es.bsc.inb.iechor.openebench.model.agents.Agent;
import es.bsc.inb.iechor.openebench.model.agents.Web;
import java.net.URI;
import org.junit.Test;

/**
 * @author Dmitry Repchevsky
 */

public class HomepageCheckerTest {
    
//    public final static String TOOL_URI = "https://openebench.bsc.es/monitor/metrics/bioagents:bar:3.0/web/bar.biocomp.unibo.it";
//    public final static String WEB_URL = "https://bar.biocomp.unibo.it/bar3/";
    
//    public final static String TOOL_URI = "https://dev-openebench.bsc.es/monitor/agent/bioconda:trimal:1.4.1/cmd/trimal.cgenomics.org";
//    public final static String WEB_URL = "http://trimal.cgenomics.org/";
    
//    public final static String TOOL_URI = "https://dev-openebench.bsc.es/monitor/metrics/bioagents:sarek:2.5.1/cmd/nf-co.re";
//    public final static String WEB_URL = "https://nf-co.re/sarek";
    
    public final static String TOOL_URI = "https://dev-openebench.bsc.es/monitor/metrics/bioagents:sarek:2.5.1/cmd/nf-co.re";
    public final static String WEB_URL = "https://nf-co.re/sarek";

    
    @Test
    public void test() {
        
        final HomepageChecker checker = new HomepageChecker();
        
        final Agent agent = new Agent(URI.create(TOOL_URI), "cmd");
        
        final Web web = new Web();
        web.setHomepage(URI.create(WEB_URL));
        agent.setWeb(web);
        
        final Metrics metrics = new Metrics();
        
        Boolean result = checker.check(null, null, agent, metrics);
        System.out.println("-> " + result);
        
    }
}
