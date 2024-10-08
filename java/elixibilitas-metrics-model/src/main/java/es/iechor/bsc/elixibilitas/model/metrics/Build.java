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

import javax.json.bind.annotation.JsonbProperty;

/**
 * @author Dmitry Repchevsky
 */

public class Build {

    private Boolean instructions;
    private Boolean dependencies;
    private Boolean automated;
    private Boolean unix;

    @JsonbProperty("instructions")
    public Boolean getInstructions() {
        return instructions;
    }

    @JsonbProperty("instructions")
    public void setInstructions(Boolean instructions) {
        this.instructions = instructions;
    }
    
    @JsonbProperty("dependencies")
    public Boolean getDependencies() {
        return dependencies;
    }

    @JsonbProperty("dependencies")
    public void setDependencies(Boolean dependencies) {
        this.dependencies = dependencies;
    }

    @JsonbProperty("automated")
    public Boolean getAutomated() {
        return automated;
    }

    @JsonbProperty("automated")
    public void setAutomated(Boolean automated) {
        this.automated = automated;
    }
    
    @JsonbProperty("unix")
    public Boolean getUnix() {
        return unix;
    }

    @JsonbProperty("unix")
    public void setUnix(Boolean unix) {
        this.unix = unix;
    }
}
