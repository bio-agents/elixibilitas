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
import es.bsc.inb.iechor.openebench.model.agents.Agent;
import es.iechor.bsc.elixibilitas.dao.MetricsDAO;
import es.iechor.bsc.elixibilitas.dao.AgentsDAO;
import es.iechor.bsc.openebench.checker.MetricsChecker;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * @author Dmitry Repchevsky
 */

public class VetoedChecker implements MetricsChecker {
        
    private static String[] vetoed;

    static {
        try (InputStream in = VetoedChecker.class.getClassLoader().getResourceAsStream("META-INF/vetoed.properties")) {
            if (in != null) {
                final Properties properties = new Properties();
                properties.load(in);
                vetoed = properties.getProperty("vetoed").trim().toLowerCase().split("\\s*,\\s*");
            }
        } catch(IOException ex) {
            Logger.getLogger(VetoedChecker.class.getName()).info(ex.getMessage());
        }
    }
        
    @Override
    public Boolean check(AgentsDAO agentsDAO, MetricsDAO metricsDAO, Agent agent, Metrics metrics) {
        if (check(agent)) {
            metrics.setVetoed(true);
            return true;
        }
        return false;
    }
    
    private static boolean check(Agent agent) {
        final String name = agent.getName();

        if (name != null && vetoed != null) {
            final String lname = name.toLowerCase();
            for (String forbidden : vetoed) {
//                if (lname.contains(forbidden.trim())) {
//                    System.out.println(agent.id + " " + forbidden.trim());
//                    return true;
//                }
                for (int i = 0, len = forbidden.length(); (i = lname.indexOf(forbidden, i)) >= 0; i += len) {
                    if ((i == 0 || !Character.isLetter(lname.charAt(i - 1))) &&
                        (i + len >= lname.length() || !Character.isLetter(lname.charAt(i + len)))) {
                        return true;
                    }
                }
            }
        }
        
        return false;
    }
}
