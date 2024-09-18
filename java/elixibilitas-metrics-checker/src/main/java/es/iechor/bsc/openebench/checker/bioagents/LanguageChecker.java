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

package es.iechor.bsc.openebench.checker.bioagents;

import es.bsc.inb.iechor.openebench.model.metrics.Distribution;
import es.bsc.inb.iechor.openebench.model.metrics.Metrics;
import es.bsc.inb.iechor.openebench.model.metrics.Sourcecode;
import es.bsc.inb.iechor.openebench.model.agents.Agent;
import es.iechor.bsc.elixibilitas.dao.MetricsDAO;
import es.iechor.bsc.elixibilitas.dao.AgentsDAO;
import es.iechor.bsc.openebench.checker.MetricsChecker;

import java.util.List;

/**
 * @author Dmitry Repchevsky
 */

public class LanguageChecker implements MetricsChecker {

    @Override
    public Boolean check(AgentsDAO agentsDAO, MetricsDAO metricsDAO, Agent agent, Metrics metrics) {
        Boolean bool = check(agent);
        if (bool != null) {
            Distribution distribution = metrics.getDistribution();
            if (distribution == null) {
                metrics.setDistribution(distribution = new Distribution());
            }
            Sourcecode sourcecode = distribution.getSourcecode();
            if (sourcecode == null) {
                distribution.setSourcecode(sourcecode = new Sourcecode());
            }
            sourcecode.setInterpreted(bool);
        }
        return bool;
    }
    
    private static Boolean check(Agent agent) {
        final List<String> languages = agent.getLanguages();
        
        if (languages.isEmpty()) {
            return null;
        }

        return !(languages.contains("C++") || 
                 languages.contains("C") || 
                 languages.contains("Java") ||
                 languages.contains("C#"));
    }
}
