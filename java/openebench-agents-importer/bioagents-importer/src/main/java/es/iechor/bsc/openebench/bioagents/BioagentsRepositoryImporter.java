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

package es.iechor.bsc.openebench.bioagents;

import es.bsc.inb.iechor.openebench.model.agents.Agent;
import es.bsc.inb.iechor.openebench.repository.OpenEBenchAlambiqueEndpoint;
import es.bsc.inb.iechor.openebench.repository.OpenEBenchEndpoint;
import es.bsc.inb.iechor.openebench.repository.OpenEBenchRepository;
import java.io.IOException;
import java.net.URI;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.json.JsonObject;

/**
 * The bio.agents data model importer.
 * 
 * @author Dmitry Repchevsky
 */

public class BioagentsRepositoryImporter {
    
    private OpenEBenchRepository repository;
    private OpenEBenchAlambiqueEndpoint alambique;
    
    public BioagentsRepositoryImporter() {}

    public BioagentsRepositoryImporter(String username, String password) {
        repository = new OpenEBenchRepository(username, password);
        alambique = new OpenEBenchAlambiqueEndpoint(username, password);
    }
    
    public void load() {
        
        boolean exception = false;
        
        final Set<URI> ids = new HashSet<>();
        
        BioagentsRepositoryIterator iter = new BioagentsRepositoryIterator();
        while (iter.hasNext()) {
            final JsonObject jagent = iter.next();
            final List<Agent> agents;
            try {
                if (alambique != null && alambique.put(jagent) != 200) {
                    exception = true;
                    continue;
                }
                agents = BioagentsConverter.convert(jagent);
            } catch (Exception ex) {
                exception = true;
                Logger.getLogger(BioagentsRepositoryImporter.class.getName()).log(Level.SEVERE, jagent.getString("bioagentsID", null), ex);
                continue;
            }
            for (Agent agent : agents) {
                try {
                    System.out.println(LocalTime.now() + ": (PUT) " + agent.id);
                    if (repository != null) {
                        repository.patch(agent);
                    }
                    ids.add(agent.id);
                } catch (Exception ex) {
                    exception = true;
                    Logger.getLogger(BioagentsRepositoryImporter.class.getName()).log(Level.SEVERE, agent.id.toString(), ex);
                }
            }
        }

        if (!exception) {
            for (Agent agent : OpenEBenchRepository.getAgents().values()) {
                final Boolean deprecated = agent.getDepricated();

                final String id = agent.id.toString();
                if (!id.startsWith(OpenEBenchEndpoint.TOOL_URI_BASE)) {
                    Logger.getLogger(BioagentsRepositoryImporter.class.getName()).log(Level.WARNING, "dubious id: {0}", id);
                    continue;
                }
                if (id.regionMatches(OpenEBenchEndpoint.TOOL_URI_BASE.length(), "bioagents:", 0, 9)) {
                    try {
                        if (ids.contains(agent.id)) {
                            if (Boolean.TRUE.equals(deprecated)) {
                                System.out.println("> REMOVE DEPRECATE: " + agent.id);
                                agent.setDepricated(null);
                                if (repository != null) {
                                    repository.put(agent);
                                }
                            }
                        } else if (!Boolean.TRUE.equals(deprecated)) {
                            agent.setDepricated(true);

                            System.out.println("> DEPRECATE: " + agent.id);
                            if (repository != null) {
                                repository.patch(agent);
                            }
                        }
                    } catch (IOException ex) {
                        Logger.getLogger(BioagentsRepositoryImporter.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }
    }
}
