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

import java.util.ArrayList;
import java.util.List;
import javax.json.bind.annotation.JsonbProperty;

/**
 * @author Dmitry Repchevsky
 */

public class Project {

    private Identity identity;
    private Summary summary;
    private Website website;
    private Build build;
    private Deployment deployment;
    private License license;
    private Documentation documentation;
    private List<Publication> publications;
    private Boolean readme;
    private Boolean governance;

    @JsonbProperty("identity")
    public Identity getIdentity() {
        return identity;
    }

    @JsonbProperty("identity")
    public void setIdentity(Identity identity) {
        this.identity = identity;
    }

    @JsonbProperty("summary")
    public Summary getSummary() {
        return summary;
    }

    @JsonbProperty("summary")
    public void setSummary(Summary summary) {
        this.summary = summary;
    }

    @JsonbProperty("website")
    public Website getWebsite() {
        return website;
    }

    @JsonbProperty("website")
    public void setWebsite(Website website) {
        this.website = website;
    }

    @JsonbProperty("build")
    public Build getBuild() {
        return build;
    }

    @JsonbProperty("build")
    public void setBuild(Build build) {
        this.build = build;
    }

    @JsonbProperty("deployment")
    public Deployment getDeployment() {
        return deployment;
    }

    @JsonbProperty("deployment")
    public void setDeployment(Deployment deployment) {
        this.deployment = deployment;
    }
    
    @JsonbProperty("license")
    public License getLicense() {
        return license;
    }

    @JsonbProperty("license")
    public void setLicense(License license) {
        this.license = license;
    }

    @JsonbProperty("documentation")
    public Documentation getDocumentation() {
        return documentation;
    }

    @JsonbProperty("documentation")
    public void setDocumentation(Documentation documentation) {
        this.documentation = documentation;
    }

    @JsonbProperty("publications")
    public List<Publication> getPublications() {
        if (publications == null) {
            publications = new ArrayList<>();
        }
        return publications;
    }

    @JsonbProperty("publications")
    public void setPublications(List<Publication> publications) {
        this.publications = publications;
    }

    @JsonbProperty("readme")
    public Boolean getReadme() {
        return readme;
    }

    @JsonbProperty("readme")
    public void setReadme(Boolean readme) {
        this.readme = readme;
    }
    
    @JsonbProperty("governance")
    public Boolean getGovernance() {
        return governance;
    }

    @JsonbProperty("governance")
    public void setGovernance(Boolean governance) {
        this.governance = governance;
    }
}