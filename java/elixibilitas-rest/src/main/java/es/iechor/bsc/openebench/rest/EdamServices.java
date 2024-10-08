/**
 * *****************************************************************************
 * Copyright (C) 2017 IECHOR ES, Spanish National Bioinformatics Institute (INB)
 * and Barcelona Supercomputing Center (BSC)
 *
 * Modifications to the initial code base are copyright of their respective
 * authors, or their employers as appropriate.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 *****************************************************************************
 */

package es.iechor.bsc.openebench.rest;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import es.bsc.inb.iechor.openebench.model.agents.Datatype;
import es.bsc.inb.iechor.openebench.model.agents.Semantics;
import es.bsc.inb.iechor.openebench.model.agents.Agent;
import es.iechor.bsc.elixibilitas.dao.MetricsDAO;
import es.iechor.bsc.elixibilitas.dao.AgentsDAO;
import es.iechor.bsc.openebench.query.MongoQueries;
import es.iechor.bsc.openebench.rest.ext.ContentRange;
import es.iechor.bsc.openebench.rest.ext.Range;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonValue;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;
import javax.json.bind.config.PropertyNamingStrategy;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParserFactory;
import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.lucene.LuceneSail;
import org.eclipse.rdf4j.sail.memory.MemoryStore;

/**
 * @author Dmitry Repchevsky
 */

@Path("/rest/edam/")
@ApplicationScoped
public class EdamServices {
    
    private final static String QUERY = "PREFIX search: <http://www.openrdf.org/contrib/lucenesail#>\n" +
                                        "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                                        "PREFIX oboInOwl: <http://www.geneontology.org/formats/oboInOwl#>\n" +
			                "SELECT ?subj ?text ?property ?label WHERE {\n" +
                                        "?subj rdfs:label ?label . \n" + 
			                "?subj search:matches [\n" +
					    "search:query ?term;\n" +
					    "search:snippet ?text;\n" +
                                            "search:property ?property;" + " ] }";

    @Inject 
    private ServletContext ctx;
    private SailRepository repository;

    @Context
    private UriInfo uriInfo;

    @Inject
    private MongoClient mc;

    @Resource
    private ManagedExecutorService executor;
    
    private AgentsDAO agentsDAO;
    private MetricsDAO metricsDAO;
 
    @PostConstruct
    public void init() {

        final String ontologyURI = ctx.getInitParameter("ontology.uri");
        
        final URL url;
        try {
            url = URI.create(ontologyURI).toURL();
        } catch (MalformedURLException ex) {
            Logger.getLogger(EdamServices.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }
        
        LuceneSail lucenesail = new LuceneSail();
        lucenesail.setParameter(LuceneSail.LUCENE_RAMDIR_KEY, "true");
        lucenesail.setBaseSail(new MemoryStore());
        repository = new SailRepository(lucenesail);
        repository.initialize();
        
        RepositoryConnection con = repository.getConnection();
        
        try (InputStream in = url.openStream()) {
            con.add(in, ontologyURI, RDFFormat.RDFXML, con.getValueFactory().createIRI(ontologyURI));
            lucenesail.reindex();
        } catch (Exception ex) {
            Logger.getLogger(EdamServices.class.getName()).log(Level.SEVERE, null, ex);
            // fallback to locally stored EDAM ontology
            try (InputStream in = ctx.getResourceAsStream("/META-INF/resources/EDAM.owl")) {
                con.add(in, ontologyURI, RDFFormat.RDFXML, con.getValueFactory().createIRI(ontologyURI));
                lucenesail.reindex();
            } catch (Exception ex2) {
                Logger.getLogger(EdamServices.class.getName()).log(Level.SEVERE, null, ex2);
            }
        }
        
        final MongoClientURI mongodbURI = new MongoClientURI(ctx.getInitParameter("mongodb.url"));
        final String agentsBaseURI = UriBuilder.fromUri(uriInfo.getBaseUri()).path(AgentsServices.class).build().toString();
        final String metricsBaseURI = UriBuilder.fromUri(uriInfo.getBaseUri()).path(MetricsServices.class).build().toString();

        agentsDAO = new AgentsDAO(mc.getDatabase(mongodbURI.getDatabase()), agentsBaseURI);
        metricsDAO = new MetricsDAO(mc.getDatabase(mongodbURI.getDatabase()), metricsBaseURI);
    }

    @PreDestroy
    public void destroy() {
        if (repository != null) {
            try {
                repository.shutDown();
            } catch(RepositoryException ex) {
                Logger.getLogger(EdamServices.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    @OPTIONS
    @Path("/aggregate")
    @Hidden
    public Response aggregate() {
         return Response.ok()
                 .header("Access-Control-Allow-Headers", "Range")
                 .header("Access-Control-Expose-Headers", "Accept-Ranges")
                 .header("Access-Control-Expose-Headers", "Content-Range")
                 .build();
    }

    @GET
    @Path("/aggregate")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Searches and groups agents by their id",
               description = "The same as '/search' with a difference in the output format",
        responses = {
            @ApiResponse(content = @Content(mediaType = MediaType.APPLICATION_JSON),
                headers = @Header(name = "Content-Range",
                                  description = "standart HTTP header ('Content-Range: items 10-30/20000')"))

        }
    )
    public void aggregate(@HeaderParam("Range")
                          @Parameter(description = "HTTP Range Header",
                                     example = "Range: items=10-30",
                                     schema = @Schema(type = "string"))
                          final Range range,
                          @QueryParam("id") 
                          @Parameter(description = "prefixed agent id", required = true)
                          final String id,
                          @QueryParam("skip") final Long skip,
                          @QueryParam("limit") final Long limit,
                          @QueryParam("projection")
                          @Parameter(description = "agents properties to return")
                          final List<String> projections,
                          @QueryParam("text")
                          @Parameter(description = "text to search")
                          final String text,
                          @QueryParam("name")
                          @Parameter(description = "text to search in the 'name' property")
                          final String name,
                          @QueryParam("description")
                          @Parameter(description = "text to search in the 'description' property")
                          final String description,
                          @QueryParam("type")
                          @Parameter(description = "list of filtered types")
                          final List<String> types,
                          @QueryParam("label")
                          @Parameter(description = "edam ontology term label")
                          final String label,
                          @Suspended final AsyncResponse asyncResponse) {
        executor.submit(() -> {
            if (range != null) {
                asyncResponse.resume(aggregateAsync(id, range.getFirstPos(), range.getLastPos(), projections, text, name, description, types, label)
                        .header("Access-Control-Allow-Headers", "Range")
                        .header("Access-Control-Expose-Headers", "Accept-Ranges")
                        .header("Access-Control-Expose-Headers", "Content-Range")
                        .build());
            } else {
                Long to = limit;
                if (skip != null && to != null) {
                    to += skip;
                }
                
                asyncResponse.resume(aggregateAsync(id, skip, to, projections, text, name, description, types, label)
                        .header("Access-Control-Allow-Headers", "Range")
                        .header("Access-Control-Expose-Headers", "Accept-Ranges")
                        .header("Access-Control-Expose-Headers", "Content-Range")
                        .build());
            }
        });
    }
    
    private Response.ResponseBuilder aggregateAsync(
                            final String id, 
                            final Long from, 
                            final Long to, 
                            final List<String> projections, 
                            final String text,
                            final String name,
                            final String description,
                            final List<String> types,
                            final String label) {

        final String[] terms = label_search(label);
        if (terms.length == 0) {
            // no edam label found
            final ContentRange range = new ContentRange("items", 0L, 0L, 0L);
            return Response.ok("[]").type(MediaType.APPLICATION_JSON)
                       .header("Accept-Ranges", "items")
                       .header("Content-Range", range.toString());
        }

        StreamingOutput stream = (OutputStream out) -> {
            final Long limit;
            if (from == null || to == null) {
                limit = to;
            } else {
                limit = to - from;
            }
            try (Writer writer = new BufferedWriter(new OutputStreamWriter(out, "UTF-8"))) {
                MongoQueries.aggregateAgents(
                        agentsDAO, metricsDAO, writer, id, from, limit, text, 
                        name, description, types, projections, terms);
            }
        };
        final long count = MongoQueries.aggregateAgentsCount(
                agentsDAO, id, text, name, description, types, terms);
        
        final ContentRange range = new ContentRange("items", from, to, count);
        
        Response.ResponseBuilder response = from == null && to == null 
                ? Response.ok() : Response.status(Response.Status.PARTIAL_CONTENT);
        
        return response.header("Accept-Ranges", "items")
                       .header("Content-Range", range.toString()).entity(stream);
    }

    
    
    @GET
    @Path("/search")
    @Produces(MediaType.APPLICATION_JSON)
    public void search(@QueryParam("text") final String text, 
                       @Suspended final AsyncResponse asyncResponse) {
        executor.submit(() -> {
            asyncResponse.resume(searchAsync(text, null).build());
        });
    }
    
    private Response.ResponseBuilder searchAsync(final String text, final String filter) {
        final StreamingOutput stream = (OutputStream out) -> {
            search(out, text, filter);
        };
                
        return Response.ok(stream);
    }

    @GET
    @Path("/description")
    @Produces(MediaType.APPLICATION_JSON)
    public void description(@QueryParam("term") final String term, 
                            @Suspended final AsyncResponse asyncResponse) {
        executor.submit(() -> {
            asyncResponse.resume(descriptionAsync(term).build());
        });
    }

    private Response.ResponseBuilder descriptionAsync(final String term) {
        final StreamingOutput stream = (OutputStream out) -> {
            try (JsonGenerator gen = Json.createGenerator(out);
                 RepositoryConnection con = repository.getConnection()) {

                gen.writeStartObject();
                writeDescriptions(con, gen, term);
                gen.writeEnd();
            }
        };
                
        return Response.ok(stream);
    }

    private void search(final OutputStream out, final String text) {

        final Map<String, List<String[]>> map = search(text);
        
        try (JsonGenerator gen = Json.createGenerator(out)) {
            gen.writeStartArray();
            for (Map.Entry<String, List<String[]>> subjects : map.entrySet()) {
                gen.writeStartObject();
                gen.write("subject", subjects.getKey());
                gen.writeStartArray("result");

                //String[] entries = subjects.getValue();
                for (String[] entry : subjects.getValue()) {
                    gen.writeStartObject();
                    gen.write("label", entry[0]);
                    gen.write("property", entry[1]);
                    gen.write("sniplet", entry[2]);
                    gen.writeEnd();
                }
                gen.writeEnd();
                gen.writeEnd();
            }
            gen.writeEnd();
        }
    }

    private void search(final OutputStream out, final String text, final String filter) {

        final Map<String, String> map = search(text, filter);
        
        try (JsonGenerator gen = Json.createGenerator(out)) {
            gen.writeStartArray();
            for (Map.Entry<String, String> subjects : map.entrySet()) {
                gen.writeStartObject();
                gen.write("subject", subjects.getKey());
                gen.write("label", subjects.getValue());
                gen.writeEnd();
            }
            gen.writeEnd();
        }
    }
    
    private String[] label_search(final String text) {

        final List<BindingSet> results;
        try (RepositoryConnection con = repository.getConnection()) {
            ValueFactory vf = con.getValueFactory();

            TupleQuery tq = con.prepareTupleQuery(QueryLanguage.SPARQL, QUERY);
            tq.setBinding("term", vf.createLiteral(text.trim().replaceAll("\\s","* AND ") + "*"));
            results = QueryResults.asList(tq.evaluate());
        }

        List<String> terms = new ArrayList<>();
        results.forEach(res -> {
            final String property = res.getValue("property").stringValue();
            if ("http://www.w3.org/2000/01/rdf-schema#label".equals(property)) {
                final String subject = res.getValue("subj").stringValue();
                terms.add(subject);
            }
        });
        return terms.toArray(new String[terms.size()]);
    }

    private Map<String, List<String[]>> search(final String text) {

        final List<BindingSet> results;
        try (RepositoryConnection con = repository.getConnection()) {
            ValueFactory vf = con.getValueFactory();

            TupleQuery tq = con.prepareTupleQuery(QueryLanguage.SPARQL, QUERY);
            tq.setBinding("term", vf.createLiteral(text.trim().replaceAll("\\s","* AND ") + "*"));
            results = QueryResults.asList(tq.evaluate());
        }

        final Map<String, List<String[]>> map = new HashMap<>();
        results.forEach(res -> {
            final String subject = res.getValue("subj").stringValue();
            List<String[]> descriptions = map.get(subject);
            if (descriptions == null) {
                map.put(subject, descriptions = new ArrayList<>());
            }

            String label = res.getValue("label").stringValue();
            if (label == null) {
                label = "";
            }
            final String property = res.getValue("property").stringValue();
            final String snip = res.getValue("text").stringValue();
            descriptions.add(new String[] {label, property, snip});

        });
        return map;
    }

    private Map<String, String> search(final String text, final String filter) {

        final List<BindingSet> results;
        try (RepositoryConnection con = repository.getConnection()) {
            ValueFactory vf = con.getValueFactory();

            TupleQuery tq = con.prepareTupleQuery(QueryLanguage.SPARQL, QUERY);
            tq.setBinding("term", vf.createLiteral(text.trim().replaceAll("\\s","* AND ") + "*"));
            results = QueryResults.asList(tq.evaluate());
        }

        final Map<String, String> map = new HashMap<>();
        results.forEach(res -> {
            final String subject = res.getValue("subj").stringValue();
            if (filter == null || subject.startsWith(filter, "http://edamontology.org/".length())) {
                String label = res.getValue("label").stringValue();
                if (label == null) {
                    label = "";
                }
                map.put(subject, label);
            }
        });
        return map;
    }
    
    @GET
    @Path("/agent/search")
    @Produces(MediaType.APPLICATION_JSON)
    public void searchAgents(@QueryParam("text") final String text, 
                            @Suspended final AsyncResponse asyncResponse) {
        executor.submit(() -> {
            asyncResponse.resume(searchAgentsAsync(text).build());
        });
    }

    private Response.ResponseBuilder searchAgentsAsync(final String text) {
        final StreamingOutput stream = (OutputStream out) -> {
            final Map<String, List<String[]>> map = search(text);
            if (!map.isEmpty()) {
                try (Writer writer = new BufferedWriter(new OutputStreamWriter(out, "UTF-8"))) {
                    writer.append("[");
                    agentsDAO.filter(writer, map);
                    writer.append("]");
                }
            }
        };
                
        return Response.ok(stream);
    }

    @GET
    @Path("/agent/")
    @Produces(MediaType.APPLICATION_JSON)
    public void getAgentsSemantics(@Context final UriInfo uriInfo, 
                                  @Suspended final AsyncResponse asyncResponse) {
        executor.submit(() -> {
            asyncResponse.resume(getAgentsSemanticsAsync().build());
        });
    }

    @GET
    @Path("/agent/{id: .*}")
    @Produces(MediaType.APPLICATION_JSON)
    public void getAgentSemantics(@PathParam("id") final String id,
                        @Context final UriInfo uriInfo, 
                        @Suspended final AsyncResponse asyncResponse) {
        if (id == null || id.isEmpty()) {
            executor.submit(() -> {
                asyncResponse.resume(getAgentsSemanticsAsync().build());
            });
        } else {
            executor.submit(() -> {
                asyncResponse.resume(getAgentSemanticsAsync(id).build());
            });
        }
    }
    
    private Response.ResponseBuilder getAgentsSemanticsAsync() {
        
        final StreamingOutput stream = (OutputStream out) -> {

            try (PipedWriter writer = new PipedWriter();
                 PipedReader reader = new PipedReader(writer)) {

                executor.submit(() -> {
                    MongoQueries.searchAgents(agentsDAO, writer, null, null, null, null, null, null, null, Arrays.asList("semantics"));
                });

                final Jsonb jsonb = JsonbBuilder.create(new JsonbConfig()
                    .withPropertyNamingStrategy(PropertyNamingStrategy.UPPER_CAMEL_CASE));

                JsonParserFactory factory = Json.createParserFactory(Collections.EMPTY_MAP);
                JsonParser parser = factory.createParser(reader);
               
                parser.next();
                Stream<JsonValue> values = parser.getArrayStream();
                
                try (JsonGenerator gen = Json.createGenerator(out)) {
                    gen.writeStartArray();
                    values.forEach(value->{
                        Agent agent = jsonb.fromJson(value.toString(), Agent.class);
                        writeSemantics(gen, agent);
                    });
                    gen.writeEnd();
                }
            } catch(IOException ex) {
                Logger.getLogger(EdamServices.class.getName()).log(Level.SEVERE, null, ex);
            }
        };
                
        return Response.ok(stream);
    }

    private Response.ResponseBuilder getAgentSemanticsAsync(final String id) {
        final Agent agent = agentsDAO.get(id);
        if (agent == null) {
            return Response.status(Response.Status.NOT_FOUND);
        }
        
        final StreamingOutput stream = (OutputStream out) -> {
            try (JsonGenerator gen = Json.createGenerator(out)) {
                writeSemantics(gen, agent);
            }
        };
                
        return Response.ok(stream);
    }
    
    private void writeSemantics(JsonGenerator gen, Agent agent) {
        try (RepositoryConnection con = repository.getConnection()) {

            gen.writeStartObject();
            
            gen.write("@id", agent.id.toString());
            
            Semantics semantics = agent.getSemantics();
            if (semantics != null) {
                
                
                gen.writeStartArray("topics");
                for (URI uri : semantics.getTopics()) {
                    gen.writeStartObject();
                    gen.write("topic", uri.toString());
                    writeDescriptions(con, gen, uri.toString());
                    gen.writeEnd();
                }
                gen.writeEnd(); // topics
                
                gen.writeStartArray("operations");
                for (URI uri : semantics.getOperations()) {
                    gen.writeStartObject();
                    gen.write("operation", uri.toString());
                    writeDescriptions(con, gen, uri.toString());
                    gen.writeEnd();
                }
                gen.writeEnd(); // operations
                
                gen.writeStartArray("inputs");
                writeDatatypes(gen, con, semantics.getInputs());
                gen.writeEnd(); // inputs
                
                gen.writeStartArray("outputs");
                writeDatatypes(gen, con, semantics.getOutputs());
                gen.writeEnd(); // outputs
            }

            gen.writeEnd(); // object
        }
    }
    
    private void writeDatatypes(JsonGenerator gen, RepositoryConnection con, List<Datatype> datatypes) {
        for (Datatype output : datatypes) {
            gen.writeStartObject();

            final URI uri = output.getDatatype();
            if (uri != null) {
                gen.write("datatype", uri.toString());
                writeDescriptions(con, gen, uri.toString());
            }

            gen.writeStartArray("formats");
            for (URI format : output.getFormats()) {
                gen.writeStartObject();
                gen.write("format", format.toString());
                writeDescriptions(con, gen, format.toString());
                gen.writeEnd();
            }
            gen.writeEnd(); // formats
            
            gen.writeEnd();
        }
    }
    
    private void writeDescriptions(RepositoryConnection con, final JsonGenerator gen, final String term) {

        final ValueFactory vf = repository.getValueFactory();
        final IRI iri = vf.createIRI(term);
        RepositoryResult<Statement> statements = con.getStatements(iri, RDFS.LABEL, null, false);

        gen.writeStartArray("labels");
        while (statements.hasNext()) {
            final Statement statement = statements.next();
            final Value value = statement.getObject();
            if (value instanceof Literal) {
                Literal literal = (Literal)value;
                gen.write(literal.stringValue());
            }
        }
        gen.writeEnd(); // labels

        statements = con.getStatements(iri, RDFS.COMMENT, null, true);
        gen.writeStartArray("comments");
        while (statements.hasNext()) {
            final Statement statement = statements.next();
            final Value value = statement.getObject();
            if (value instanceof Literal) {
                Literal literal = (Literal)value;
                gen.write(literal.stringValue());
            }
        }
        gen.writeEnd(); // comments
        
        statements = con.getStatements(iri, vf.createIRI("http://www.geneontology.org/formats/oboInOwl#hasDefinition"), null, true);
        gen.writeStartArray("definitions");
        while (statements.hasNext()) {
            final Statement statement = statements.next();
            final Value value = statement.getObject();
            if (value instanceof Literal) {
                Literal literal = (Literal)value;
                gen.write(literal.stringValue());
            }
        }

        gen.writeEnd(); // definitions
    }
}
