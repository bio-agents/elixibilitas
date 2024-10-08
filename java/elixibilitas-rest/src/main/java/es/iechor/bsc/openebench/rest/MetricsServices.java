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
import com.mongodb.client.MongoDatabase;
import es.iechor.bsc.elixibilitas.dao.MetricsDAO;
import es.iechor.bsc.elixibilitas.dao.AgentsDAO;
import es.iechor.bsc.elixibilitas.meta.MetricsMetaWriter;
import es.iechor.bsc.openebench.rest.validator.JsonSchema;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.security.Principal;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.annotation.security.RolesAllowed;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonException;
import javax.json.JsonObject;
import javax.json.JsonPatch;
import javax.json.JsonPointer;
import javax.json.JsonReader;
import javax.json.JsonStructure;
import javax.json.JsonValue;
import javax.json.JsonWriter;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;
import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

/**
 * REST Service to operate over Metrics objects.
 * 
 * @author Dmitry Repchevsky
 */

@Path("/metrics/")
@ApplicationScoped
public class MetricsServices {
    
    @Inject
    private MongoClient mc;

    @Inject 
    private ServletContext ctx;

    @Context
    private UriInfo uriInfo;

    @Resource
    private ManagedExecutorService executor;
    
    private AgentsDAO agentsDAO;
    private MetricsDAO metricsDAO;
    
    @PostConstruct
    public void init() {
        final String baseURI = UriBuilder.fromUri(uriInfo.getBaseUri()).path(MetricsServices.class).build().toString();

        final MongoClientURI mongodbURI = new MongoClientURI(ctx.getInitParameter("mongodb.url"));
        final MongoDatabase db = mc.getDatabase(mongodbURI.getDatabase());
        metricsDAO = new MetricsDAO(db, baseURI);
        
        // we will generate ids for "/metrics"
        agentsDAO = new AgentsDAO(db, baseURI);
    }

    /**
     * Proxy method to return Metrics JSON Schema.
     * 
     * @param ctx injected servlet context.
     * 
     * @return JSON Schema for the Metrics
     */
    @GET
    @Path("/metrics.json")
    @Produces(MediaType.APPLICATION_JSON)
    @Hidden
    public Response getMetricsJsonSchema(@Context ServletContext ctx) {
        return Response.ok(ctx.getResourceAsStream("/META-INF/resources/metrics.json")).build();
    }

    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Returns all agents metrics.",
        parameters = {
            @Parameter(in = ParameterIn.PATH, name = "projection", description = "fields to return", required = false)
        },
        responses = {
            @ApiResponse(content = @Content(mediaType = MediaType.APPLICATION_JSON,
                                            schema = @Schema(ref="https://openebench.bsc.es/monitor/metrics/metrics.json")),
                         description = "JSON array of metrics"
            ),
            @ApiResponse(responseCode = "404", description = "metrics not found")
        }
    )
    public void getMetrics(@QueryParam("projection")
                           @Parameter(description = "properties to be returned",
                                      example = "project.license.open_source")
                           final List<String> projections,
                           @Suspended final AsyncResponse asyncResponse) {
        executor.submit(() -> {
            asyncResponse.resume(getMetricsAsync(projections).build());
        });
    }

    private ResponseBuilder getMetricsAsync(List<String> projections) {
        StreamingOutput stream = (OutputStream out) -> {
            try (Writer writer = new BufferedWriter(new OutputStreamWriter(out, "UTF-8"))) {
                metricsDAO.write(writer, projections);
            }
        };
                
        return Response.ok(stream);
    }
    
    @GET
    @Path("/{id}/")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Returns agent's metrics by the unprefixed agent's id.",
        responses = {
            @ApiResponse(content = @Content(mediaType = MediaType.APPLICATION_JSON,
                                            schema = @Schema(ref="https://openebench.bsc.es/monitor/metrics/metrics.json")),
                         description = "Metrics JSON description"
            ),
            @ApiResponse(responseCode = "404", description = "metrics object not found")
        }
    )
    public void getMetrics(@PathParam("id")
                           @Parameter(description = "unprefixed agent id",
                                      example = "pmut")
                           final String id,
                           @Suspended final AsyncResponse asyncResponse) {
        executor.submit(() -> {
            asyncResponse.resume(getMetricsAsync(id, null).build());
        });
    }

    @GET
    @Path("/{id:[^:/]*}/{path:.*}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Returns metrics by the agent's id and json path.",
        responses = {
            @ApiResponse(content = @Content(mediaType = MediaType.APPLICATION_JSON,
                                            schema = @Schema(ref="https://openebench.bsc.es/monitor/metrics/metrics.json")),
                         description = "Metrics JSON description or 'null' if no metrics found"
            ),
            @ApiResponse(responseCode = "404", description = "metrics object not found")
        }
    )
    public void getMetrics(@PathParam("id")
                           @Parameter(description = "unprefixed agent id",
                                      example = "pmut")
                           final String id,
                           @PathParam("path")
                           @Parameter(description = "json pointer",
                                      example = "project")
                           final String path,
                           @Suspended final AsyncResponse asyncResponse) {
        executor.submit(() -> {
            asyncResponse.resume(getMetricsAsync(id, path).build());
        });
    }

    @GET
    @Path("/{id:.*:+[^/]*}/{type}/{host}{path:.*}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Returns metrics by the agent's id and (optionally) json path.",
        responses = {
            @ApiResponse(content = @Content(mediaType = MediaType.APPLICATION_JSON,
                                            schema = @Schema(ref="https://openebench.bsc.es/monitor/metrics/metrics.json")),
                         description = "Metrics JSON description or 'null' if no metrics found"
            ),
            @ApiResponse(responseCode = "404", description = "metrics object not found")
        }
    )
    public void getMetrics(@PathParam("id")
                           @Parameter(description = "prefixed agent id",
                                      example = "bioagents:pmut:2017")
                           final String id,
                           @PathParam("type")
                           @Parameter(description = "agent type",
                                      example = "web") 
                           final String type,
                           @PathParam("host")
                           @Parameter(description = "agent authority",
                                      example = "mmb.irbbarcelona.org")
                           final String host,
                           @PathParam("path")
                           @Parameter(description = "json pointer",
                                      example = "project")
                           final String path,
                           @Suspended final AsyncResponse asyncResponse) {
        executor.submit(() -> {
            asyncResponse.resume(getMetricsAsync(id + '/' + type + '/' + host, path).build());
        });
    }

    private ResponseBuilder getMetricsAsync(String id, String path) {
        final String json = metricsDAO.getJSON(id);
        if (json == null || json.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND);
        }
        if (path != null && path.length() > 0) {
            if (path.charAt(0) != '/') {
                path = "/" + path;
            }

            JsonPointer pointer;
            try {
                pointer = Json.createPointer(path);
            } catch(Exception ex) {
                return Response.status(Status.BAD_REQUEST);
            }

            JsonStructure structure = Json.createReader(new StringReader(json)).read();
            try {
                if (!pointer.containsValue(structure)) {
                    return Response.ok("null");
                }
            } catch(JsonException ex) {
                return Response.ok("null");
            }
            final JsonValue value = pointer.getValue(structure);
            StreamingOutput stream = (OutputStream out) -> {
                try (JsonWriter writer = Json.createWriter(out)) {
                    writer.write(value);
                }
            };
            return Response.ok(stream);
        }
        
        return Response.ok(json);
    }
    
    @PUT
    @Path("/{id : .*}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Inserts the metrics into the database."
    )
    @RolesAllowed("metrics_submitter")
    public void putMetrics(@PathParam("id")
                           @Parameter(description = "full agent id",
                               example = "bioagents:pmut:2017/web/mmb.irbbarcelona.org") 
                           final String id,
                           @RequestBody(description = "json metrics object",
                              content = @Content(schema = @Schema(ref="https://openebench.bsc.es/monitor/metrics/metrics.json")),
                              required = true) 
                           @JsonSchema(location="metrics.json") final String json,
                           @Context SecurityContext security,
                           @Suspended final AsyncResponse asyncResponse) {

        final Principal principal = security.getUserPrincipal();
        final String user = principal != null ? principal.getName() : null;
        
        executor.submit(() -> {
            asyncResponse.resume(
                    putMetricsAsync(user, id, json).build());
        });
    }

    private Response.ResponseBuilder putMetricsAsync(String source, String id, String json) {
        metricsDAO.put(source, id, json);
        return Response.ok();
    }

    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Updates metrics in the database.",
        description = "Accepts an array of JSON documents with defined @id " +
                      "(i.e. 'https://openebench.bsc.es/monitor/metrics/bioagents:pmut:2017/web/mmb.irbbarcelona.org'). " +
                      "If metrics document is already exists - properties are merged."
    )
    @RolesAllowed("metrics_submitter")
    public void updateMetrics(@RequestBody(
                                  description = "batch update of metrics properties",
                                  content = @Content(
                                      mediaType = MediaType.APPLICATION_JSON),
                                  required = true) final Reader reader,
                              @Context SecurityContext security,
                              @Suspended final AsyncResponse asyncResponse) {

        final Principal principal = security.getUserPrincipal();
        final String user = principal != null ? principal.getName() : null;
        
        executor.submit(() -> {
            asyncResponse.resume(
                updateMetricsAsync(user, reader).build());
        });
    }

    private ResponseBuilder updateMetricsAsync(final String user, final Reader reader) {
        final JsonParser parser = Json.createParser(reader);
                
        if (parser.hasNext() &&
            parser.next() == JsonParser.Event.START_ARRAY) {
            try {
                Stream<JsonValue> stream = parser.getArrayStream();
                stream.forEach(item->{
                    if (JsonValue.ValueType.OBJECT == item.getValueType()) {
                        final JsonObject object = item.asJsonObject();
                        metricsDAO.upsert(user, object);
                    }
                });
            } catch (Exception ex) {
                Response.status(Response.Status.BAD_REQUEST);
                Logger.getLogger(MetricsServices.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            Response.status(Response.Status.BAD_REQUEST);
        }
        return Response.ok();
    }

    @PATCH
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Updates metrics in the database.",
        description = "Accepts an array of JSON documents with defined @id " +
                      "(i.e. 'https://openebench.bsc.es/monitor/metrics/bioagents:pmut:2017/web/mmb.irbbarcelona.org'). " +
                      "Method uses mongodb 'upsert' operation."
    )
    @RolesAllowed("metrics_submitter")
    public void patchMetrics(@RequestBody(
                                 description = "batch update of metrics properties",
                                 content = @Content(
                                     mediaType = MediaType.APPLICATION_JSON),
                                 required = true) final Reader reader,
                             @Context SecurityContext security,
                             @Suspended final AsyncResponse asyncResponse) {

        final Principal principal = security.getUserPrincipal();
        final String user = principal != null ? principal.getName() : null;
        
        executor.submit(() -> {
            asyncResponse.resume(
                patchMetricsAsync(user, reader).build());
        });
    }
    
    private ResponseBuilder patchMetricsAsync(final String user, final Reader reader) {
        final JsonParser parser = Json.createParser(reader);
                
        if (parser.hasNext() &&
            parser.next() == JsonParser.Event.START_ARRAY) {
            try {
                Stream<JsonValue> stream = parser.getArrayStream();
                stream.forEach(item->{
                    if (JsonValue.ValueType.OBJECT == item.getValueType()) {
                        final JsonObject object = item.asJsonObject();
                        metricsDAO.merge(user, object);
                    }
                });
            } catch (Exception ex) {
                Response.status(Response.Status.BAD_REQUEST);
                Logger.getLogger(MetricsServices.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            Response.status(Response.Status.BAD_REQUEST);
        }
        return Response.ok();
    }

    @PATCH
    @Path("/{id}/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Updates metrics in the database.",
        description = "Accepts JSON document as an input."
    )
    @RolesAllowed("metrics_submitter")
    public void patchMetrics(@PathParam("id")
                             @Parameter(description = "prefixed agent id",
                                        example = "bioagents:pmut:2017") 
                             final String id,
                             @RequestBody(description = "partial metrics document",
                                    content = @Content(mediaType = MediaType.APPLICATION_JSON),
                                required = true) final String json,
                             @Context SecurityContext security,
                             @Suspended final AsyncResponse asyncResponse) {

        final Principal principal = security.getUserPrincipal();
        final String user = principal != null ? principal.getName() : null;
        
        executor.submit(() -> {
            asyncResponse.resume(patchMetricsAsync(user, id, null, json).build());
        });
    }

    @PATCH
    @Path("/{id}/{type}/{host}{path:.*}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Updates metrics in the database.",
        description = "Accepts JSON document as an input. " +
                      "Merges the documents if the 'path' is empty or uses JSON Patch otherwise."
    )
    @RolesAllowed("metrics_submitter")
    public void patchMetrics(@PathParam("id")
                             @Parameter(description = "prefixed agent id",
                                        example = "bioagents:pmut:2017") 
                             final String id,
                             @PathParam("type")
                             @Parameter(description = "agent type",
                                       example = "web") 
                             final String type,
                             @PathParam("host")
                             @Parameter(description = "agent authority",
                                        example = "mmb.irbbarcelona.org")
                             final String host,
                             @PathParam("path")
                             @Parameter(description = "json pointer",
                                        example = "project")
                             final String path,
                             @RequestBody(description = "metrics property value",
                                    content = @Content(mediaType = MediaType.APPLICATION_JSON),
                                required = true) final String json,
                             @Context SecurityContext security,
                             @Suspended final AsyncResponse asyncResponse) {

        final Principal principal = security.getUserPrincipal();
        final String user = principal != null ? principal.getName() : null;
        
        executor.submit(() -> {
            asyncResponse.resume(patchMetricsAsync(user, id + '/' + type + '/' + host, path, json).build());
        });
    }
    
    private ResponseBuilder patchMetricsAsync(String user, String id, String path, String json) {
        
        final String result;
        
        if (path == null || path.isEmpty()) {
            result = metricsDAO.merge(user, id, json);
        } else {
            final JsonValue value;
            try {
                value = Json.createReader(new StringReader(json)).readValue();
            } catch(Exception ex) {
                return Response.status(Response.Status.BAD_REQUEST.getStatusCode(), ex.getMessage());
            }

            final JsonPatch patch = Json.createPatchBuilder().replace(path, value).build();
            result = metricsDAO.patch(user, id, patch);
        }
        
        return Response.status(result == null ? Status.NOT_MODIFIED : Status.OK);
    }
    
    @GET
    @Path("/log/{id}/{type}/{host}{path:.*}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Retrieves metrics changes log"
    )
    public void getMetricsLog(@PathParam("id") String id,
                              @PathParam("type") String type,
                              @PathParam("host") String host,
                              @PathParam("path") String path,
                              @QueryParam("from") final String from,
                              @QueryParam("to") final String to,
                              @QueryParam("limit") final Integer limit,
                              @Suspended final AsyncResponse asyncResponse) {
        if (path == null || path.isEmpty()) {
            asyncResponse.resume(Response.status(Response.Status.BAD_REQUEST).build());
        }
        executor.submit(() -> {
            asyncResponse.resume(getMetricsLogAsync(id + "/" + type + "/" + host, path, from, to, limit).build());
        });
    }

    private Response.ResponseBuilder getMetricsLogAsync(String id, String field, String from, String to, Integer limit) {
        final JsonArray array = metricsDAO.findLog(id, field, from, to, limit);
        if (array == null) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR);
        }
        if (array.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND);
        }
        
        return Response.ok(array);
    }

    @GET
    @Path("/meta")
    @Produces(MediaType.APPLICATION_JSON)
    public void getMetricsMeta(@Suspended final AsyncResponse asyncResponse) {
        executor.submit(() -> {
            asyncResponse.resume(getMetricsMetaAsync(null).build());
        });
    }
    
    @GET
    @Path("/meta/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public void getMetricsMeta(@PathParam("id") String id,
                               @Suspended final AsyncResponse asyncResponse) {
        executor.submit(() -> {
            asyncResponse.resume(getMetricsMetaAsync(id).build());
        });
    }

    private Response.ResponseBuilder getMetricsMetaAsync(String id) {
        StreamingOutput stream = (OutputStream out) -> {
            JsonGenerator generator = Json.createGenerator(new BufferedWriter(new OutputStreamWriter(out, "UTF-8")));
            if (id == null) {
               generator.writeStartArray();
            }
            try (MetricsMetaWriter writer = new MetricsMetaWriter(generator);
                 JsonGenerator gen = Json.createGenerator(out);
                 PipedReader reader = new PipedReader();
                 BufferedReader ids = new BufferedReader(reader)) {
                
                final PipedWriter pwriter = new PipedWriter(reader);
                executor.submit(() -> {
                    try {
                        agentsDAO.group(pwriter, id);
                    }
                    finally {
                        try {
                            pwriter.close();
                        } catch(IOException ex) {}
                    }
                });
                
                final int baseURIindex = metricsDAO.baseURI.length();
                String line;
                while((line = ids.readLine()) != null) {
                    final String json = metricsDAO.getJSON(line.substring(baseURIindex));
                    final JsonReader jsonReader = Json.createReader(new StringReader(json));
                    writer.write(jsonReader.read());
                }
            } catch(Exception ex) {
                Logger.getLogger(AgentsServices.class.getName()).log(Level.SEVERE, null, ex);
            }

            if (id == null) {
               generator.writeEnd();
            }

        };
        return Response.ok(stream);
    }
}
