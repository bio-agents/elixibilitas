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

package es.iechor.bsc.openebench.checker;

import es.bsc.inb.iechor.openebench.model.metrics.Metrics;
import es.bsc.inb.iechor.openebench.model.agents.Agent;
import es.bsc.inb.iechor.openebench.repository.OpenEBenchEndpoint;
import es.iechor.bsc.elixibilitas.dao.MetricsDAO;
import es.iechor.bsc.elixibilitas.dao.AgentsDAO;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Dmitry Repchevsky
 */

public class MetricsCheckTask implements Callable<Metrics> {

    private final Agent agent;
    
    private final AgentsDAO agentsDAO;
    private final MetricsDAO metricsDAO;
    
    public MetricsCheckTask(Agent agent, AgentsDAO agentsDAO, MetricsDAO metricsDAO) {
        this.agent = agent;
        this.agentsDAO = agentsDAO;
        this.metricsDAO = metricsDAO;
    }

    @Override
    public Metrics call() throws Exception {
        try {
            final String id = agent.id.toString().substring(OpenEBenchEndpoint.TOOL_URI_BASE.length());

            Metrics metrics = metricsDAO.get(id);
            if (metrics == null) {
                metrics = new Metrics();
            }
            MetricsChecker.checkAll(agentsDAO, metricsDAO, agent, metrics);
            
            Boolean vetoed = metrics.getVetoed();
            if (vetoed != null && vetoed) {
                vetoed = agent.getVetoed();
                if (vetoed == null || !vetoed) {
                    agent.setVetoed(true);
                    agentsDAO.put("bioagents", agent);
                }
            }
            return metrics;
        } catch(Exception ex) {
            Logger.getLogger(MetricsCheckTask.class.getName()).log(Level.WARNING, "error in metrics: " + agent.id.toString(), ex);
        }
        return null;
    }
}
