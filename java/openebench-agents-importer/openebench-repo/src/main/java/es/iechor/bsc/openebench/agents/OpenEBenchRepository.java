/**
 * *****************************************************************************
 * Copyright (C) 2018 IECHOR ES, Spanish National Bioinformatics Institute (INB)
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

package es.iechor.bsc.openebench.agents;

import es.iechor.bsc.openebench.model.agents.Publication;
import es.iechor.bsc.openebench.model.agents.Agent;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Dmitry Repchevsky
 */
public class OpenEBenchRepository {
    
    private static volatile Map<String, Agent> agents;
    
    private OpenEBenchEndpoint endpoint;
    
    public OpenEBenchRepository() {
    }
    
    public OpenEBenchRepository(String name, String password) {
        endpoint = new OpenEBenchEndpoint(name, password);
    }

    public int put(Agent agent) throws IOException {
        if (endpoint == null) {
            return 403;
        }

        final int code = endpoint.put(agent);
        if (code == 200) {
            if (OpenEBenchRepository.agents != null) {
                agents.put(agent.id.toString(), agent);
            }
        }
        return code;
    }

    public int patch(Agent agent) throws IOException {
        if (endpoint == null) {
            return 403;
        }

        final int code = endpoint.patch(agent);
        if (code == 200) {
            if (OpenEBenchRepository.agents != null) {
                agents.put(agent.id.toString(), agent);
            }
        }
        return code;
    }

    public static Map<String, Agent> getAgents() {
        Map<String, Agent> agentz = OpenEBenchRepository.agents;
        if (agentz == null) {
            synchronized(AgentsComparator.class) {
                agentz = OpenEBenchRepository.agents;
                if (agentz == null) {
                    OpenEBenchRepository.agents = agentz = OpenEBenchEndpoint.get();
                }
            }
        }
        return agentz;
    }
    
    public static Map<String, String> getDOIPublications() {
        final Map<String, String> publications = new ConcurrentHashMap<>();
        
        final Map<String, Agent> agentz = getAgents();
        for (Agent agent : agentz.values()) {
            String id = agent.id.toString();
            if (id.startsWith(OpenEBenchEndpoint.TOOL_URI_BASE)) {
                final String[] nodes = id.substring(OpenEBenchEndpoint.TOOL_URI_BASE.length()).split("/")[0].split(":");
                id = nodes.length == 1 ? nodes[0] : nodes[1];
                for (Publication publication : agent.getPublications()) {
                    final String doi = publication.getDOI();
                    if (doi != null) {
                        final String _id = publications.get(doi);
                        if (_id == null) {
                            publications.put(doi, id);
                        } else if (!id.equals(_id)) {
                            System.out.println(String.format(">openebench warning: %s vs %s, %s", id, _id, doi));
                        }
                    }
                }
            }
        }
        
        return publications;
    }
}
