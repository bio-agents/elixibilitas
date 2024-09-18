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

import es.bsc.inb.iechor.openebench.model.metrics.Deployment;
import es.bsc.inb.iechor.openebench.model.metrics.Metrics;
import es.bsc.inb.iechor.openebench.model.metrics.Project;
import es.bsc.inb.iechor.openebench.model.agents.Dependencies;
import es.bsc.inb.iechor.openebench.model.agents.Agent;
import es.iechor.bsc.elixibilitas.dao.MetricsDAO;
import es.iechor.bsc.elixibilitas.dao.AgentsDAO;
import es.iechor.bsc.openebench.checker.MetricsChecker;
import java.net.URI;
import java.util.List;

/**
 * @author Dmitry Repchevsky
 */

public class RuntimeDependenciesChecker implements MetricsChecker {
    
    @Override
    public Boolean check(AgentsDAO agentsDAO, MetricsDAO metricsDAO, Agent agent, Metrics metrics) {
        Boolean bool = check(agent);
        Project project = metrics.getProject();
        Deployment deployment;
        if (project == null) {
            project = new Project();
            project.setDeployment(deployment = new Deployment());
            metrics.setProject(project);
        } else {
            deployment = project.getDeployment();
            if (deployment == null) {
                project.setDeployment(deployment = new Deployment());
            }
        }
        deployment.setDependencies(bool);
        return bool;
    }
    
    private static Boolean check(Agent agent) {
        Dependencies dependencies = agent.getDependencies();
        if (dependencies == null) {
            return null;
        }
        
        final List<URI> list = dependencies.getRuntimeDependencies();
        return list != null && list.size() > 0;
    }

}
