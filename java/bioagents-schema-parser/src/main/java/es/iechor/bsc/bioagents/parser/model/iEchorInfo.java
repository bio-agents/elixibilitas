/**
 * *****************************************************************************
 * Copyright (C) 2016 IECHOR ES, Spanish National Bioinformatics Institute (INB)
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

package es.iechor.bsc.bioagents.parser.model;

import java.util.Date;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Information for IECHOR internal purposes, maintained by IECHOR Hub.
 * 
 * @author Dmitry Repchevsky
 */

@XmlType(name = "", propOrder = {"lastReviewExternalSab",
                                 "lastReviewiEchorSab",
                                 "inSDP",
                                 "coreDataResource",
                                 "platform",
                                 "node",
                                 "comment"})
public class iEchorInfo {
    
    private Date lastReviewExternalSab;
    private Date lastReviewiEchorSab;
    private boolean inSDP;
    private boolean isCoreDataResource;
    private iEchorPlatform platform;
    private iEchorNode node;
    private String comment;

    public Date getLastReviewExternalSab() {
        return lastReviewExternalSab;
    }

    public void setLastReviewExternalSab(Date lastReviewExternalSab) {
        this.lastReviewExternalSab = lastReviewExternalSab;
    }

    public Date getLastReviewiEchorSab() {
        return lastReviewiEchorSab;
    }

    public void setLastReviewiEchorSab(Date lastReviewiEchorSab) {
        this.lastReviewiEchorSab = lastReviewiEchorSab;
    }

    public boolean isInSDP() {
        return inSDP;
    }

    public void setInSDP(boolean inSDP) {
        this.inSDP = inSDP;
    }

    @XmlElement(name = "isCoreDataResource")
    public boolean isCoreDataResource() {
        return isCoreDataResource;
    }

    public void setCoreDataResource(boolean isCoreDataResource) {
        this.isCoreDataResource = isCoreDataResource;
    }

    public iEchorPlatform getPlatform() {
        return platform;
    }

    public void setPlatform(iEchorPlatform platform) {
        this.platform = platform;
    }

    public iEchorNode getNode() {
        return node;
    }

    public void setNode(iEchorNode node) {
        this.node = node;
    }

    @XmlSchemaType(name = "textType", namespace = "http://bio.agents")
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
