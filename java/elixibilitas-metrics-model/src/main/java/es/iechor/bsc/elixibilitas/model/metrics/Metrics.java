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

package es.iechor.bsc.elixibilitas.model.metrics;

import java.time.ZonedDateTime;
import javax.json.bind.annotation.JsonbProperty;

/**
 * @author Dmitry Repchevsky
 */

public class Metrics {

    private Project project;
    private Distribution distribution;
    private Support support;

    private ZonedDateTime timestamp;
    
    @JsonbProperty("@timestamp")
    public ZonedDateTime getTimestamp() {
        return timestamp;
    }

    @JsonbProperty("@timestamp")
    public void setTimestamp(ZonedDateTime timestamp) {
        this.timestamp = timestamp;
    }

    @JsonbProperty("project")
    public Project getProject() {
        return project;
    }

    @JsonbProperty("project")
    public void setProject(Project project) {
        this.project = project;
    }

    @JsonbProperty("distribution")
    public Distribution getDistribution() {
        return distribution;
    }

    @JsonbProperty("distribution")
    public void setDistribution(Distribution distribution) {
        this.distribution = distribution;
    }
    
    @JsonbProperty("support")
    public Support getSupport() {
        return support;
    }

    @JsonbProperty("support")
    public void setSupport(Support support) {
        this.support = support;
    }
}
