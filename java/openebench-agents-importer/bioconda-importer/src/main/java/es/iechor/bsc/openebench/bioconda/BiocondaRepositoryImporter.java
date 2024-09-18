package es.iechor.bsc.openebench.bioconda;

import es.iechor.bsc.openebench.agents.OpenEBenchEndpoint;
import es.iechor.bsc.openebench.bioconda.BiocondaPackage.Metadata;
import es.iechor.bsc.openebench.model.agents.Distributions;
import es.iechor.bsc.openebench.model.agents.Publication;
import es.iechor.bsc.openebench.model.agents.Agent;
import es.iechor.bsc.openebench.model.agents.Web;
import es.iechor.bsc.openebench.agents.OpenEBenchRepository;
import es.iechor.bsc.openebench.agents.AgentsComparator;
import java.io.IOException;
import java.net.URI;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Dmitry Repchevsky
 */

public class BiocondaRepositoryImporter {
    
    public final static String ID_TEMPLATE = OpenEBenchEndpoint.TOOL_URI_BASE + "bioconda:%s:%s/%s/%s";
    
    private final Map<String, String> dois = OpenEBenchRepository.getDOIPublications();
    
    private OpenEBenchRepository repository;

    public BiocondaRepositoryImporter() {}

    public BiocondaRepositoryImporter(String username, String password) {
        repository = new OpenEBenchRepository(username, password);
    }
    
    public void load() {

        final ExecutorService executor = Executors.newFixedThreadPool(32);
                
        try {
            final Collection<BiocondaPackage> packages = BiocondaRepository.getPackages();
            final CountDownLatch latch = new CountDownLatch(packages.size());

            for (BiocondaPackage pack : packages) {
                executor.execute(() -> {
                    try {
                        Agent agent = find(pack);
                        if (agent == null) {
                            agent = create(pack, null);
                            if (agent != null) {
                                String id = agent.id.toString();
                                if (id.startsWith(OpenEBenchEndpoint.TOOL_URI_BASE)) {
                                    final String[] nodes = id.substring(OpenEBenchEndpoint.TOOL_URI_BASE.length()).split("/")[0].split(":");
                                    id = nodes.length == 1 ? nodes[0] : nodes[1];
                                    
                                    if (repository != null) {
                                        // insert 'common' agent
                                        final Agent common = new Agent(URI.create(OpenEBenchEndpoint.TOOL_URI_BASE + id), null);
                                        common.setName(agent.getName());
                                        common.setDescription(agent.getDescription());
                                        common.setWeb(agent.getWeb());
                                        repository.patch(common);
                                    }
                                }
                            }
                        }
                        if (agent != null) {
                            checkDOIs(agent);

                            System.out.println(LocalTime.now() + ": (PUT) " + agent.id.toString());
                            if (repository != null) {
                                repository.patch(agent);
                            } else {
                                System.out.println("    name: " + agent.getName());
                                Web web = agent.getWeb();
                                if (web != null) {
                                    System.out.println("    homepage: " + web.getHomepage());
                                }
                                System.out.println("    description: " + agent.getDescription());
//                                final Jsonb jsonb = JsonbBuilder.create(
//                                        new JsonbConfig().withPropertyNamingStrategy(PropertyNamingStrategy.UPPER_CAMEL_CASE)
//                                        .withFormatting(true));
//                                System.out.println(jsonb.toJson(agent));
                            }
                            
                        }
                    } catch (Exception ex) {
                        Logger.getLogger(BiocondaRepositoryImporter.class.getName()).log(Level.SEVERE, pack.toString(), ex);
                    }
                    latch.countDown();
                });
            }

            latch.await(8, TimeUnit.HOURS);
        } catch (InterruptedException ex) {
            Logger.getLogger(BiocondaRepositoryImporter.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            executor.shutdown();
        }
        System.out.println(LocalTime.now() + ": finished.");
    }
    
    private void checkDOIs(final Agent agent) {
        final List<Publication> publications = agent.getPublications();
        if (!publications.isEmpty()) {
            String id = agent.id.toString();
            if (id.startsWith(OpenEBenchEndpoint.TOOL_URI_BASE)) {
                final String[] nodes = id.substring(OpenEBenchEndpoint.TOOL_URI_BASE.length()).split("/")[0].split(":");
                id = nodes.length == 1 ? nodes[0] : nodes[1];
                for (Publication publication : publications) {
                    final String doi = publication.getDOI();
                    if (doi != null) {
                        final String _id = dois.get(doi);
                        if (!id.equals(_id)) {
                            System.out.println(String.format(">openebench warning: %s vs %s, %s", id, _id, doi));
                        }
                    }
                }
            }
        }
    }
    
    public static Agent find(final BiocondaPackage pack) throws IOException {
        
        String id = search(pack);
        if (id == null) {
            final Agent tmp = create(pack, null);
            if (tmp != null) {
                double score = 0;
                for (Agent t : OpenEBenchRepository.getAgents().values()) {
                    if (t.id.toString().substring(OpenEBenchEndpoint.TOOL_URI_BASE.length()).startsWith("bioagents:")) {
                        final double s = AgentsComparator.compare(tmp, t);
                        if (s > score) {
                            score = s;
                            id = t.id.toString();
                        }
                    }
                }
                if (score < 0.5) {
                    return null;
                }
            }
            if (id == null) {
                return null;
            }
        }
        
        if (id.startsWith(OpenEBenchEndpoint.TOOL_URI_BASE)) {
            final String[] nodes = id.substring(OpenEBenchEndpoint.TOOL_URI_BASE.length()).split("/");
            if (nodes.length > 0) {
                final String[] _id = nodes[0].split(":");
                final Agent agent = create(pack, _id.length > 1 ? _id[1] : _id[0]);
                if (agent != null) {
                    agent.setExternalId(pack.name + ":" + pack.version);
                    return agent;
                }
            }
        }
        return null;
    }
    
    /*
     *  Searches the bio.agents Agent for the bioconda package
     */
    private static String search(final BiocondaPackage pack) {
        
        if (pack.name != null && pack.name.startsWith("bioconductor-")) {
            final String name = pack.name.substring("bioconductor-".length());
            
            for (Agent t : OpenEBenchRepository.getAgents().values()) {
                final String uri = t.id.getPath();

                if (uri.endsWith("bioconductor.org")) {
                    final int idx = uri.indexOf("bioagents:" + name + ":");
                    if (idx > 0) {
                        return t.id.toString();
                    }
                }
            }
        }
        
        if (pack.name != null && pack.name.startsWith("r-")) {
            final String name = pack.name.substring("r-".length());
            
            for (Agent t : OpenEBenchRepository.getAgents().values()) {
                final String uri = t.id.getPath();
                
                if (uri.endsWith("cran.r-project.org")) {
                    final int idx = uri.indexOf("bioagents:" + name + ":");
                    if (idx > 0) {
                        return t.id.toString();
                    }
                }
            }
        }
        
        return null;
    }

    /*
     * Creates the bio.agents Agent from the bioconda package and the provided id.
     */    
    private static Agent create(BiocondaPackage pack, String id) throws IOException {

        final Metadata metadata = pack.getMetadata();
        if (metadata == null) {
            return null;
        }
        
        URI homepage = null;

        if (metadata.home != null && !metadata.home.isEmpty()) {
            try {
                homepage = URI.create(metadata.home);
            } catch(IllegalArgumentException ex) {}
        }
        if (homepage == null) {
            try {
                homepage = URI.create(BiocondaRepository.LINUX64_REPO);
            } catch(IllegalArgumentException ex) {}
        }

        final String authority = homepage != null ? homepage.getHost() : "";

        String _id = id != null ? id : pack.name.toLowerCase();
        String doi = null;
        if (metadata.identifiers != null && metadata.identifiers.length > 0) {
            for (String identifier : metadata.identifiers) {
                if (identifier != null) {
                    if (id == null && identifier.startsWith("bioagents:")) {
                        _id = identifier.substring("bioagents:".length()).toLowerCase();
                    } else if (identifier.startsWith("doi:")) {
                        doi = identifier.substring("doi:".length());
                    }
                }
            }
        }
        
        Agent agent = new Agent(URI.create(String.format(ID_TEMPLATE, _id, pack.version, "cmd", authority)), "cmd");

        if (!_id.equals(pack.name)) {
            agent.setExternalId(pack.version == null || pack.version.isEmpty() ? pack.name : pack.name + ":" + pack.version);
        }
        
        if (doi != null) {
            boolean hasPublication = false;
            for (Publication publication : agent.getPublications()) {
                if (doi.equals(publication.getDOI())) {
                    hasPublication = true;
                    break;
                }
            }
            if (!hasPublication) {
                Publication publication = new Publication();
                publication.setDOI(doi);
                agent.getPublications().add(publication);
            }
        }
        
        agent.setName(pack.name);
        if (homepage != null) {
            Web web = new Web();
            web.setHomepage(homepage);
            agent.setWeb(web);
        }

        agent.setLicense(metadata.license != null ? metadata.license : pack.license);
        agent.setDescription(metadata.summary);

        // set repository
        if (metadata.git != null && !metadata.git.isEmpty()) {
            try {
                final URI repository = URI.create(metadata.git);
                agent.getRepositories().add(repository);
            } catch(IllegalArgumentException ex) {}
        }

        if (metadata.src_urls != null && metadata.src_urls.length > 0) {
            for (String src_url : metadata.src_urls) {
                try {
                    final URI source = URI.create(src_url);
                    Distributions distributions = agent.getDistributions();
                    if (distributions == null) {
                        agent.setDistributions(distributions = new Distributions());
                    }
                    distributions.getSourcecodeDistributions().add(source);

                } catch(IllegalArgumentException ex) {}

            }
        }
        if (pack.file != null && !pack.file.isEmpty()) {
            try {
                final URI conda = URI.create(pack.toString());
                Distributions distributions = agent.getDistributions();
                if (distributions == null) {
                    agent.setDistributions(distributions = new Distributions());
                }
                distributions.getBinaryPackagesDistributions().add(conda);
            } catch(IllegalArgumentException ex) {}
        }        

        return agent;
    }
}
