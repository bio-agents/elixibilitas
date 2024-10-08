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

import es.bsc.inb.iechor.openebench.model.metrics.Metrics;
import es.bsc.inb.iechor.openebench.model.metrics.Project;
import es.bsc.inb.iechor.openebench.model.agents.Documentation;
import es.bsc.inb.iechor.openebench.model.agents.Agent;
import es.iechor.bsc.elixibilitas.dao.MetricsDAO;
import es.iechor.bsc.elixibilitas.dao.AgentsDAO;
import es.iechor.bsc.openebench.checker.MetricsChecker;

/**
 * @author Dmitry Repchevsky
 */

public class DocumentationTermsOfUseChecker implements MetricsChecker {
    
    @Override
    public Boolean check(AgentsDAO agentsDAO, MetricsDAO metricsDAO, Agent agent, Metrics metrics) {
        Boolean bool = check(agent);
        Project project = metrics.getProject();
        if (Boolean.TRUE.equals(bool)) {
            es.bsc.inb.iechor.openebench.model.metrics.Documentation documentation;
            if (project == null) {
                metrics.setProject(project = new Project());
                project.setDocumentation(documentation = new es.bsc.inb.iechor.openebench.model.metrics.Documentation());
            } else {
                documentation = project.getDocumentation();
                if (documentation == null) {
                    project.setDocumentation(documentation = new es.bsc.inb.iechor.openebench.model.metrics.Documentation());
                }
            }
            documentation.setTermsOfUse(true);
        } else if (project != null && project.getDocumentation() != null) {
            project.getDocumentation().setTermsOfUse(bool);
        }
        return bool;
    }
    
    private static Boolean check(Agent agent) {
        Documentation documentation = agent.getDocumentation();
        if (documentation == null) {
            return null;
        }
        return documentation.getTermsOfUse()== null ? null : true;
    }
}
