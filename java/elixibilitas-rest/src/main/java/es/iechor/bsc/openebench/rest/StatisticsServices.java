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
import es.bsc.inb.iechor.openebench.model.metrics.HomepageAccess;
import es.iechor.bsc.elixibilitas.dao.MetricsDAO;
import es.iechor.bsc.elixibilitas.dao.AgentsDAO;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.servers.Server;
import java.io.BufferedWriter;
import java.io.CharArrayWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import static java.time.temporal.ChronoUnit.DAYS;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

/**
 * REST Service to get some statistics.
 * 
 * @author Dmitry Repchevsky
 */

@OpenAPIDefinition(info = @Info(title = "OpenEBench Statistics API services", 
                                version = "0.1", 
                                description = "OpenEBench Statistics API services",
                                license = @License(name = "LGPL 2.1", 
                                            url = "https://www.gnu.org/licenses/old-licenses/lgpl-2.1.html"),
                                contact = @Contact(url = "https://openebench.bsc.es")
                                ),
                    //security = @SecurityRequirement(name = "openid-connect"), 
                    servers = {@Server(url = "https://openebench.bsc.es/")})
@Path("/rest/")
@ApplicationScoped
public class StatisticsServices {
    
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
    
    private volatile long agents_uptime;
    private volatile long metrics_uptime;

    private CharArrayWriter agents_stat;
    private CharArrayWriter metrics_stat;
    
    @PostConstruct
    public void init() {
        
        final String agentsBaseURI = UriBuilder.fromUri(uriInfo.getBaseUri()).path(AgentsServices.class).build().toString();
        final String metricsBaseURI = UriBuilder.fromUri(uriInfo.getBaseUri()).path(MetricsServices.class).build().toString();
        
        final MongoClientURI mongodbURI = new MongoClientURI(ctx.getInitParameter("mongodb.url"));
        
        agentsDAO = new AgentsDAO(mc.getDatabase(mongodbURI.getDatabase()), agentsBaseURI);
        metricsDAO = new MetricsDAO(mc.getDatabase(mongodbURI.getDatabase()), metricsBaseURI);
    }
    
    @GET
    @Path("/metrics/statistics/")
    @Produces(MediaType.APPLICATION_JSON)
    public void getMetricsStatistics(
            @HeaderParam("Cache-Control") final String cache,
            @Suspended final AsyncResponse asyncResponse) {
        executor.submit(() -> {
            asyncResponse.resume(getMetricsStatisticsAsync(cache).build());
        });
    }
    
    private Response.ResponseBuilder getMetricsStatisticsAsync(final String cache) {
        StreamingOutput stream = (OutputStream out) -> {
            if (metrics_stat == null || "no-cache".equalsIgnoreCase(cache) || System.currentTimeMillis() - metrics_uptime > 86400000) {
                final CharArrayWriter stat = metricsDAO.statistics(new CharArrayWriter());
                if (stat != null) {
                    metrics_stat = stat;
                    metrics_uptime = System.currentTimeMillis();
                } else if (metrics_stat == null) {
                    return;
                }
            }
            
            try (Writer writer = new BufferedWriter(new OutputStreamWriter(out, "UTF-8"))) {
                metrics_stat.writeTo(writer);
            }
        };
        return Response.ok(stream);
    }
    
    @GET
    @Path("/agents/statistics/")
    @Produces(MediaType.APPLICATION_JSON)
    public void getAgentsStatistics(
            @HeaderParam("Cache-Control") final String cache,
            @Suspended final AsyncResponse asyncResponse) {
        executor.submit(() -> {
            asyncResponse.resume(getAgentsStatisticsAsync(cache).build());
        });
    }
    
    private Response.ResponseBuilder getAgentsStatisticsAsync(final String cache) {
        StreamingOutput stream = (OutputStream out) -> {
            if (agents_stat == null || "no-cache".equalsIgnoreCase(cache) || System.currentTimeMillis() - agents_uptime > 86400000) {
                final CharArrayWriter stat = agentsDAO.statistics(new CharArrayWriter());
                if (stat != null) {
                    agents_stat = stat;
                    agents_uptime = System.currentTimeMillis();
                } else if (agents_stat == null) {
                    return;
                }
            }
            
            try (Writer writer = new BufferedWriter(new OutputStreamWriter(out, "UTF-8"))) {
                agents_stat.writeTo(writer);
            }
        };
        return Response.ok(stream);
    }
    
    @GET
    @Path("/metrics/availability/{id:.*}")
    @Produces(MediaType.APPLICATION_JSON)
    public void getAvailability(
            @PathParam("id")
                        @Parameter(description = "agent id",
                                   example = "'trimal', 'bioagents:trimal:1.4/cmd/trimal.cgenomics.org'")
                        final String id,
            @Suspended final AsyncResponse asyncResponse) {
        executor.submit(() -> {
            HomepageAccess homepage_access = getAvailabilityAsync(id);
            try (Jsonb jsonb = JsonbBuilder.create()) {
                asyncResponse.resume(jsonb.toJson(homepage_access, HomepageAccess.class));
            } catch (Exception ex) {
                asyncResponse.cancel();
            }
        });
    }
    
    private HomepageAccess getAvailabilityAsync(final String id) {

        final HomepageAccess homepage_access = new HomepageAccess();
        
        final String month_ago_time = LocalDate.now().minusMonths(1).plusDays(1).toString();
        
        final JsonArray access_time = metricsDAO.findLog(id, "/project/website/access_time", month_ago_time, null, null);
        if(access_time != null && !access_time.isEmpty()) {
            int total_access_time = 0;
            int access_time_measures = 0;
            for (int i = 0, n = access_time.size(); i < n; i++) {
                final JsonObject obj = access_time.getJsonObject(i);
                final String time = obj.getString("value", "0");
                try {
                    final int t = Integer.parseInt(time);
                    if (t > 0) {
                        total_access_time += t;
                        access_time_measures++;
                    }
                } catch(NumberFormatException ex) {}
            }
            
            if (access_time_measures > 0) {
                homepage_access.setAverageAccessTime(total_access_time / access_time_measures);
            }
        }
        
        final JsonArray last_check = metricsDAO.findLog(id, "/project/website/last_check", month_ago_time, null, null);
        if (last_check == null || last_check.isEmpty()) {
            return homepage_access;
        }
        
        final JsonArray operational = metricsDAO.findLog(id, "/project/website/operational", null, null, null);        
        if (operational == null || operational.isEmpty()) {
            return homepage_access;
        }

        int operational_days = 0;
        int unoperational_days = 0;
        
        JsonObject obj = operational.getJsonObject(0);
        String code = obj.getString("value", "0");
        String date = obj.getString("date", null);

        ZonedDateTime last_date = null;
        for (int i = 0, j = 0, m = last_check.size(), n = operational.size(); i < m; i++) {
            final JsonObject o = last_check.getJsonObject(i);
            final String adate = o.getString("date", null);
            if (adate == null) {
                continue;
            }

            if (last_date == null) {
                last_date = ZonedDateTime.parse(adate);
            } else {
                final ZonedDateTime current_date = ZonedDateTime.parse(adate);
                
                // do not consider hh:mm:ss, so 23:00 - 01:00 (2h) is a 1 (next) day.
                long days = DAYS.between(last_date.toLocalDate(), current_date.toLocalDate());

                try {
                    final int c = Integer.parseInt(code);
                    while (--days > 0) {
                        if (c >= 200 && c < 300) {
                            operational_days++;
                        } else {
                            unoperational_days++;
                        }
                    }
                } catch(NumberFormatException ex) {}

                last_date = current_date;
            }

            while(j <= n && adate.compareTo(date) >= 0) {
                code = obj.getString("value", "0");
                if (++j < n) {
                    obj = operational.getJsonObject(j);
                    date = obj.getString("date", null);
                }
            }

            try {
                final int c = Integer.parseInt(code);
                if (c >= 200 && c < 300) {
                    operational_days++;
                } else {
                    unoperational_days++;
                }
            } catch(NumberFormatException ex) {}
        }
        
        homepage_access.setUptimeDays(operational_days);
        homepage_access.setDowntimeDays(unoperational_days);
        
        return homepage_access;
    }
}
