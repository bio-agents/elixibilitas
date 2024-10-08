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

public class Website {

    private Integer operational;
    private Boolean robots;
    private Boolean https;
    private Boolean ssl;
    private Long accessTime;
    private ZonedDateTime lastCheck;
    private Boolean copyright;
    private Boolean acknowledgement;
    private Boolean license;
    private Boolean resources;
    private Boolean bioschemas;
    
    @JsonbProperty("operational")
    public Integer getOperational() {
        return operational;
    }

    @JsonbProperty("operational")
    public void setOperational(Integer operational) {
        this.operational = operational;
    }

    @JsonbProperty("robots")
    public Boolean getRobotsAllowed() {
        return robots;
    }

    @JsonbProperty("robots")
    public void setRobotsAllowed(Boolean robots) {
        this.robots = robots;
    }
    
    @JsonbProperty(value="https")
    public Boolean getHTTPS() {
        return https;
    }

    @JsonbProperty(value="https")
    public void setHTTPS(Boolean https) {
        this.https = https;
    }

    @JsonbProperty(value="ssl", nillable=true)
    public Boolean getSSL() {
        return ssl;
    }

    @JsonbProperty(value="ssl", nillable=true)
    public void setSSL(Boolean ssl) {
        this.ssl = ssl;
    }

    @JsonbProperty(value="access_time", nillable=true)
    public Long getAccessTime() {
        return accessTime;
    }

    @JsonbProperty(value="access_time", nillable=true)
    public void setAccessTime(Long accessTime) {
        this.accessTime = accessTime;
    }
    
    @JsonbProperty("last_check")
    public ZonedDateTime getLastCheck() {
        return lastCheck;
    }

    @JsonbProperty("last_check")
    public void setLastCheck(ZonedDateTime lastCheck) {
        this.lastCheck = lastCheck;
    }
    
    @JsonbProperty("copyright")
    public Boolean getCopyright() {
        return copyright;
    }

    @JsonbProperty("copyright")
    public void setCopyright(Boolean copyright) {
        this.copyright = copyright;
    }

    @JsonbProperty("license")
    public Boolean getLicense() {
        return license;
    }

    @JsonbProperty("license")
    public void setLicense(Boolean license) {
        this.license = license;
    }
    
    @JsonbProperty("acknowledgement")
    public Boolean getAcknowledgement() {
        return acknowledgement;
    }

    @JsonbProperty("acknowledgement")
    public void setAcknowledgement(Boolean acknowledgement) {
        this.acknowledgement = acknowledgement;
    }

    @JsonbProperty("resources")
    public Boolean getResources() {
        return resources;
    }

    @JsonbProperty("resources")
    public void setResources(Boolean resources) {
        this.resources = resources;
    }
    
    @JsonbProperty("bioschemas")
    public Boolean getBioschemas() {
        return bioschemas;
    }

    @JsonbProperty("bioschemas")
    public void setBioschemas(Boolean bioschemas) {
        this.bioschemas = bioschemas;
    }
}
