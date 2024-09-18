/**
 * *****************************************************************************
 * Copyright (C) 2020 IECHOR ES, Spanish National Bioinformatics Institute (INB)
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
import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Dmitry Repchevsky
 */

public interface MetricsChecker {

    /**
     * Sets the metrics value for the agent.
     * 
     * @param agentsDAO
     * @param metricsDAO
     * @param agent the agent for which metrics is calculated.
     * @param metrics metrics object to set the metrics
     * 
     * @return calculated metrics value
     */
    Boolean check(AgentsDAO agentsDAO, MetricsDAO metricsDAO, Agent agent, Metrics metrics);
    
    public static void checkAll(AgentsDAO agentsDAO, MetricsDAO metricsDAO, Agent agent, Metrics metrics) {
            
        ServiceLoader<MetricsChecker> loader = ServiceLoader.load(MetricsChecker.class);
        Iterator<MetricsChecker> iterator = loader.iterator();
        while(iterator.hasNext()) {
            MetricsChecker checker = iterator.next();
            try {
                checker.check(agentsDAO, metricsDAO, agent, metrics);
            } catch (Exception ex) {
                Logger.getLogger(MetricsChecker.class.getName()).log(Level.SEVERE, "error in metrics: " + agent.id.toString(), ex);
            }
        }
    }
}
