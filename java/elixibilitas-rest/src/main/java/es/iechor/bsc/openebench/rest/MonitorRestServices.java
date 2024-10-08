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
import es.iechor.bsc.elixibilitas.dao.MetricsDAO;
import es.iechor.bsc.elixibilitas.dao.AgentsDAO;
import es.iechor.bsc.openebench.query.MongoQueries;
import es.iechor.bsc.openebench.rest.ext.ContentRange;
import es.iechor.bsc.openebench.rest.ext.Range;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.time.Instant;
import java.time.ZonedDateTime;
import static java.time.temporal.ChronoUnit.HOURS;
import java.util.List;
import java.util.TreeMap;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.stream.JsonGenerator;
import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

/**
 * REST Service to get some statistics.
 * 
 * @author Dmitry Repchevsky
 */

@Path("/rest/")
@ApplicationScoped
public class MonitorRestServices {
    
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
        
        final String agentsBaseURI = UriBuilder.fromUri(uriInfo.getBaseUri()).path(AgentsServices.class).build().toString();
        final String metricsBaseURI = UriBuilder.fromUri(uriInfo.getBaseUri()).path(MetricsServices.class).build().toString();
        
        final MongoClientURI mongodbURI = new MongoClientURI(ctx.getInitParameter("mongodb.url"));
        
        agentsDAO = new AgentsDAO(mc.getDatabase(mongodbURI.getDatabase()), agentsBaseURI);
        metricsDAO = new MetricsDAO(mc.getDatabase(mongodbURI.getDatabase()), metricsBaseURI);
    }

    @OPTIONS
    @Path("/search")
    @Hidden
    public Response search() {
         return Response.ok()
                 .header("Access-Control-Allow-Headers", "Range")
                 .header("Access-Control-Expose-Headers", "Accept-Ranges")
                 .header("Access-Control-Expose-Headers", "Content-Range")
                 .build();
    }

    @GET
    @Path("/search")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Searches the agents",
               description = "queries the agents with a possibility to limit the response. " +
                             "The response is grouped by ids and sorted by names.",
        responses = {
            @ApiResponse(content = @Content(
               mediaType = MediaType.APPLICATION_JSON,
               array = @ArraySchema(schema = @Schema(
                   ref="https://openebench.bsc.es/monitor/agent/agent.json"
            ))),
            headers = @Header(name = "Content-Range",
                              description = "standart HTTP header ('Content-Range: agents 10-30/20000')"))
        }
    )
    public void search(@HeaderParam("Range") 
                       @Parameter(description = "HTTP Range Header",
                                  example = "Range: agents=10-30",
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
                       @QueryParam("homepage")
                       @Parameter(description = "text to search in the agent 'homepage' property")
                       final String homepage,
                       @QueryParam("description")
                       @Parameter(description = "text to search in the 'description' property")
                       final String description,
                       @QueryParam("tags")
                       @Parameter(description = "text to match the 'tags' property")
                       final List<String> tags,
                       @Suspended final AsyncResponse asyncResponse) {
        executor.submit(() -> {
            if (range != null) {
                asyncResponse.resume(searchAsync(id, range.getFirstPos(), 
                        range.getLastPos(), projections, text, name, homepage, description, tags)
                            .header("Access-Control-Allow-Headers", "Range")
                            .header("Access-Control-Expose-Headers", "Accept-Ranges")
                            .header("Access-Control-Expose-Headers", "Content-Range")
                            .build());
            } else {
                Long to = limit;
                if (skip != null && to != null) {
                    to += skip;
                }
                
                asyncResponse.resume(searchAsync(id, skip, to, projections, text, name, homepage, description, tags)
                            .header("Access-Control-Allow-Headers", "Range")
                            .header("Access-Control-Expose-Headers", "Accept-Ranges")
                            .header("Access-Control-Expose-Headers", "Content-Range")
                            .build());
            }
        });
    }
    
    private Response.ResponseBuilder searchAsync(
                              final String id,
                              final Long from, 
                              final Long to, 
                              final List<String> projections, 
                              final String text,
                              final String name,
                              final String homepage,
                              final String description,
                              final List<String> tags) {

        StreamingOutput stream = (OutputStream out) -> {

            final Long limit;
            if (from == null || to == null) {
                limit = to;
            } else {
                limit = to - from;
            }

            try (Writer writer = new BufferedWriter(new OutputStreamWriter(out, "UTF-8"))) {
                MongoQueries.searchAgents(agentsDAO, writer, id, from, limit, text, name, homepage, description, tags, projections);
            }
        };

        final long count = MongoQueries.searchAgentsCount(agentsDAO, id, text, name, homepage, description, tags);
        
        final ContentRange range = new ContentRange("agents", from, to, count);
        
        ResponseBuilder response = from == null && to == null 
                ? Response.ok() : Response.status(Response.Status.PARTIAL_CONTENT);
        
        return response.header("Accept-Ranges", "agents").header("Content-Range", range.toString()).entity(stream);
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
                          @QueryParam("tags")
                          @Parameter(description = "text to match the 'tags' property")
                          final List<String> tags,
                          @QueryParam("type")
                          @Parameter(description = "list of filtered types")
                          final List<String> types,
                          @QueryParam("edam")
                          @Parameter(description = "edam ontology term")
                          final String edam_term,
                          @Suspended final AsyncResponse asyncResponse) {
        executor.submit(() -> {
            if (range != null) {
                asyncResponse.resume(aggregateAsync(
                        id, range.getFirstPos(), range.getLastPos(), projections, text, name, description, tags, types, edam_term)
                        .header("Access-Control-Allow-Headers", "Range")
                        .header("Access-Control-Expose-Headers", "Accept-Ranges")
                        .header("Access-Control-Expose-Headers", "Content-Range")
                        .build());
            } else {
                Long to = limit;
                if (skip != null && to != null) {
                    to += skip;
                }
                
                asyncResponse.resume(aggregateAsync(id, skip, to, projections, text, name, description, tags, types, edam_term)
                        .header("Access-Control-Allow-Headers", "Range")
                        .header("Access-Control-Expose-Headers", "Accept-Ranges")
                        .header("Access-Control-Expose-Headers", "Content-Range")
                        .build());
            }
        });
    }
    
    private Response.ResponseBuilder aggregateAsync(final String id, 
                              final Long from, 
                              final Long to, 
                              final List<String> projections, 
                              final String text,
                              final String name,
                              final String description,
                              final List<String> tags,
                              final List<String> types,
                              final String edam_term) {

        StreamingOutput stream = (OutputStream out) -> {
            final Long limit;
            if (from == null || to == null) {
                limit = to;
            } else {
                limit = to - from;
            }
            try (Writer writer = new BufferedWriter(new OutputStreamWriter(out, "UTF-8"))) {
                MongoQueries.aggregateAgents(agentsDAO, metricsDAO, writer, id, limit, 
                        limit, text, name, description, tags, types, projections, null);
            }
        };
        final long count = MongoQueries.aggregateAgentsCount(
                agentsDAO, id, text, name, description, tags, types, null);
        
        final ContentRange range = new ContentRange("items", from, to, count);
        
        ResponseBuilder response = from == null && to == null 
                ? Response.ok() : Response.status(Response.Status.PARTIAL_CONTENT);
        
        return response.header("Accept-Ranges", "items")
                       .header("Content-Range", range.toString()).entity(stream);
    }

    @GET
    @Path("/widget/agent/{id:.*}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get the 'most suitable' agent by the agent's id",
               description = "the complete agent id has a form '{prefix}:{id}:{version}/{type}/{authority}'. " +
                             "providing a partial id (i.e. 'pmut') may result in many agents descriptions, " +
                             "from those only one is returned.",
               responses = {
                   @ApiResponse(content = 
                       @Content(mediaType = MediaType.APPLICATION_JSON,
                                schema = @Schema(ref="https://openebench.bsc.es/monitor/agent/agent.json"))),
                   @ApiResponse(responseCode = "404", description = "agent not found")
        }
    )
    public void getAgentWidget(@PathParam("id") String id,
                          @Suspended final AsyncResponse asyncResponse) {
        executor.submit(() -> {
            asyncResponse.resume(getAgentWidgetAsync(id).build());
        });
    }

    private Response.ResponseBuilder getAgentWidgetAsync(String id) {
        final JsonArray array = agentsDAO.getJSONArray(id);
        
        if (array.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND);
        }
        
        JsonObject object = array.getJsonObject(0);
        for (int i = 1; i < array.size(); i++) {
            final JsonObject obj = array.getJsonObject(i);
            if (obj.getString("@timestamp", "").compareTo(object.getString("@timestamp", "")) > 0) {
                object = obj;
            }
        }
        return Response.ok(object);
    }

    @GET
    @Path("/widget/metrics/{id:.*}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get the 'most suitable' metrics for the agent's id",
               description = "the complete agent id has a form '{prefix}:{id}:{version}/{type}/{authority}'. " +
                             "providing a partial id (i.e. 'pmut') may found many agents, " +
                             "for those only one metrics is returned.",
        responses = {
            @ApiResponse(content = 
                    @Content(mediaType = MediaType.APPLICATION_JSON,
                             schema = @Schema(ref="https://openebench.bsc.es/monitor/metrics/metrics.json"))),
            @ApiResponse(responseCode = "404", description = "metrics not found")
        }
    )
    public void getMetricsWidget(@PathParam("id") String id,
                          @Suspended final AsyncResponse asyncResponse) {
        executor.submit(() -> {
            asyncResponse.resume(getMetricsWidgetAsync(id).build());
        });
    }

    private Response.ResponseBuilder getMetricsWidgetAsync(String id) {
        final JsonArray array = agentsDAO.getJSONArray(id);
        
        if (array.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND);
        }
        
        JsonObject object = array.getJsonObject(0);
        for (int i = 1; i < array.size(); i++) {
            final JsonObject obj = array.getJsonObject(i);
            if (obj.getString("@timestamp", "").compareTo(object.getString("@timestamp", "")) > 0) {
                object = obj;
            }
        }
        
        final String _id = object.getString("@id", null);
        
        final String json = metricsDAO.getJSON(_id.substring(agentsDAO.baseURI.length()));
        if (json == null) {
            return Response.status(Response.Status.NOT_FOUND);
        }
        
        return Response.ok(json);
    }

    @GET
    @Path("/homepage/{id}/{type}/{host}{path:.*}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Agent's homepage uptime statistics",
               description = "the complete agent id has a form '{prefix}:{id}:{version}/{type}/{authority}'. " +
                             "providing a partial id (i.e. 'pmut') may found many agents, " +
                             "for those only one metrics is returned.",
        responses = {
            @ApiResponse(content = 
                    @Content(mediaType = MediaType.APPLICATION_JSON,
                             schema = @Schema(ref="https://openebench.bsc.es/monitor/metrics/metrics.json"))),
            @ApiResponse(responseCode = "404", description = "metrics not found")
        }
    )
    public void getHomePageMonitoring(
                           @QueryParam("date1") Long date1,
                           @QueryParam("date2") Long date2,
                           @QueryParam("limit") Integer limit,
                           @PathParam("id")
                           @Parameter(description = "prefixed agent id",        
                                      example = "bioagents:pmut") 
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
                           @Parameter(description = "json pointer")
                           final String path,
                           @Suspended final AsyncResponse asyncResponse) {
        executor.submit(() -> {

            asyncResponse.resume(getHomePageMonitoringAsync(id + "/" + type + "/" + host, date1, date2, limit)
                    .build());
        });
    }
            
    private Response.ResponseBuilder getHomePageMonitoringAsync(String id, Long date1, Long date2, Integer limit) {
 
        String from = date1 == null ? null : Instant.ofEpochSecond(date1).toString();
        String to = date2 == null ? null : Instant.ofEpochSecond(date2).toString();
        
        
        final JsonArray l_check = metricsDAO.findLog(id, "/project/website/last_check", from, to, limit);
        final JsonArray last_check = l_check.isEmpty() ? metricsDAO.findLog(id, "/project/website/last_check", null, null, 1) 
                                                       : l_check;
        
        final JsonArray operational = metricsDAO.findLog(id, "/project/website/operational", null, to, null);
        if (operational == null) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR);
        } else if (operational.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND);
        }
                
        JsonArray access_time = metricsDAO.findLog(id, "/project/website/access_time", from, to, null);
        if (access_time == null) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR);
        }

        final TreeMap<String, String> atimes = new TreeMap<>();
        
        for (int i = 0, n = access_time.size(); i < n; i++) {
            final JsonObject obj = access_time.getJsonObject(i);
            
            final String date = obj.getString("date", "null");
            final String time = obj.getString("value", "0");

            atimes.put(date, time);
        }
        
        final String _time;
        if (from == null) {
            _time = null;
        } else {
            // look for the last access_time just before the 'from' date
            access_time = metricsDAO.findLog(id, "/project/website/access_time", null, from, 1);
            if (access_time == null || access_time.isEmpty()) {
                _time = null;
            } else {
                final JsonObject obj = access_time.getJsonObject(0);
                _time = obj.getString("value", null);
            }
        }

        StreamingOutput stream = (OutputStream out) -> {
            try (JsonGenerator writer = Json.createGenerator(out)) {
                writer.writeStartArray();

                JsonObject obj = operational.getJsonObject(0);
                String code = obj.getString("value", "0");
                String date = obj.getString("date", null);
                
                String time = _time;
                ZonedDateTime last_date = null;
                for (int i = 0, j = 0, m = last_check.size(), lim = limit == null ? Integer.MAX_VALUE: limit + 1, n = operational.size(); i < m && lim > 0; i++, lim--) {
                    final JsonObject o = last_check.getJsonObject(i);
                    final String adate = o.getString("date", null);
                    if (adate == null) {
                        continue;
                    }
                    
                    if (last_date == null) {
                        last_date = ZonedDateTime.parse(adate);
                    } else {
                        final ZonedDateTime current_date = ZonedDateTime.parse(adate);
                        long hours = HOURS.between(last_date, current_date);
                        while (hours > 26 && lim-- > 0) {
                            hours -= 24;
                            last_date = last_date.plus(24, HOURS);
                            writer.writeStartObject();
                            writer.write("date", last_date.toString());
                            writer.writeNull("code");
                            writer.writeNull("access_time");
                            writer.writeEnd();
                        }
                        last_date = current_date;
                    }

                    if (lim > 0) {
                        writer.writeStartObject();

                        while(j <= n && adate.compareTo(date) >= 0) {
                            code = obj.getString("value", "0");
                            if (++j < n) {
                                obj = operational.getJsonObject(j);
                                date = obj.getString("date", null);
                            }
                        }

                        writer.write("date", adate);

                        final String ntime = atimes.get(adate);
                        if (ntime != null) {
                            time = ntime;
                        }
                        try {
                            final int c = Integer.parseInt(code);
                            writer.write("code", c);
                            if (c == HttpURLConnection.HTTP_CLIENT_TIMEOUT ||
                                time == null) {
                                writer.writeNull("access_time");
                            } else {
                                try {
                                    writer.write("access_time", Integer.parseInt(time));
                                } catch(NumberFormatException ex) {
                                    writer.writeNull("access_time");
                                }
                            }                    
                        } catch(NumberFormatException ex) {
                            writer.write("code", 0);
                            writer.writeNull("access_time");
                        }
                        writer.writeEnd();
                    }
                }
                
                writer.writeEnd();
            }
        };
        
        return Response.ok(stream);
    }
    
    @GET
    @Path("/statistics/")
    @Produces(MediaType.APPLICATION_JSON)
    public void getStatistics(@Suspended final AsyncResponse asyncResponse) {
            
        executor.submit(() -> {
            asyncResponse.resume(getStatisticsAsync().build());
        });
    }

    private Response.ResponseBuilder getStatisticsAsync() {
        
        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("all", Json.createObjectBuilder()
                     .add("total", getStatistics("total"))
                     .add("operational", getStatistics("operational")));
        addMetricsStatistics(builder, "cmd");
        addMetricsStatistics(builder, "web");
        addMetricsStatistics(builder, "db");
        addMetricsStatistics(builder, "app");
        addMetricsStatistics(builder, "lib");
        addMetricsStatistics(builder, "ontology");
        addMetricsStatistics(builder, "workflow");
        addMetricsStatistics(builder, "plugin");
        addMetricsStatistics(builder, "sparql");
        addMetricsStatistics(builder, "soap");
        addMetricsStatistics(builder, "script");
        addMetricsStatistics(builder, "rest");
        addMetricsStatistics(builder, "workbench");
        addMetricsStatistics(builder, "suite");

        return Response.ok(builder.build());
    }

    @GET
    @Path("/statistics/count/{field}")
    @Produces(MediaType.APPLICATION_JSON)
    public void getStatisticsRepo(@PathParam("field") String field,
                                  @QueryParam("text") String text,
                                  @Suspended final AsyncResponse asyncResponse) {
            
        executor.submit(() -> {
            asyncResponse.resume(getStatisticsRepo(field, text).build());
        });
    }
    
    private Response.ResponseBuilder getStatisticsRepo(final String field, final String text) {
        return Response.ok(agentsDAO.count(field, text));
    }

    @GET
    @Path("/statistics/{field}")
    @Produces(MediaType.APPLICATION_JSON)
    public void getMetrics(@PathParam("field") String field,
                           @Suspended final AsyncResponse asyncResponse) {
            
        executor.submit(() -> {
            asyncResponse.resume(getStatisticsAsync(field).build());
        });
    }
    
    private Response.ResponseBuilder getStatisticsAsync(final String field) {
        
        long result = getStatistics(field);
        return result == Long.MIN_VALUE ? Response.status(Response.Status.BAD_REQUEST) :
                         Response.ok(result);
    }
    
    public void addMetricsStatistics(JsonObjectBuilder builder, String field) {
        builder.add(field, Json.createObjectBuilder()
                     .add("total", getMetricsTotal(field))
                     .add("operational", getMetricsStatistics(field)));
    }
    
    private long getStatistics(final String field) {
        switch(field) {
            case "total": return agentsDAO.count();
            case "operational": return metricsDAO.count("{'project.website.operational' : {$in: [200, 202]}}");
            case "cmd":
            case "web":
            case "db":
            case "app":
            case "lib":
            case "ontology":
            case "workflow":
            case "plugin":
            case "sparql":
            case "soap":
            case "script":
            case "rest":
            case "workbench":
            case "suite": return agentsDAO.count(String.format("{'_id.type' : '%s'}", field));
        }

        return Long.MIN_VALUE;
    }

    private long getMetricsTotal(final String type) {
        return metricsDAO.count("{'_id': { $regex: '/" + type + "/'}}");
    }
    
    private long getMetricsStatistics(final String type) {
        return metricsDAO.count("{'project.website.operational' : {$in: [200, 202]}, '_id': { $regex: '/" + type + "/'}}");
    }
}
