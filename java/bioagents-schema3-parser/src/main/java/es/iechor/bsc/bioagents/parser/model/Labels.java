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

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;

/**
 * 
 * @author Dmitry Repchevsky
 */

@XmlType(name = "", propOrder = {"agentTypes",
                                 "topics",
                                 "operatingSystems",
                                 "languages",
                                 "license",
                                 "collectionIDs",
                                 "maturity",
                                 "cost",
                                 "accessibility",
                                 "iechorPlatforms",
                                 "iechorNodes"})
public class Labels {

    private List<AgentType> agentTypes;
    private List<Topic> topics;
    private List<OperatingSystemType> operatingSystems;
    private List<LanguageType> languages;
    private LicenseType license;
    private List<String> collectionIDs;
    private MaturityType maturity;
    private CostType cost;
    private List<AccessibilityType> accessibility;
    private List<iEchorPlatform> iechorPlatforms;
    private List<iEchorNode> iechorNodes;

    @XmlElement(name = "agentType", required = true)
    public List<AgentType> getAgentTypes() {
        if (agentTypes == null) {
            agentTypes = new ArrayList<>();
        }
        return agentTypes;
    }

    @XmlElement(name = "topic", required = true)
    public List<Topic> getTopics() {
        if (topics == null) {
            topics = new ArrayList<>();
        }
        return topics;
    }

    @XmlElement(name = "operatingSystem")
    public List<OperatingSystemType> getOperatingSystems() {
        if (operatingSystems == null) {
            operatingSystems = new ArrayList<>();
        }
        return operatingSystems;
    }

    @XmlElement(name = "language")
    public List<LanguageType> getLanguages() {
        if (languages == null) {
            languages = new ArrayList<>();
        }
        return languages;
    }

    public LicenseType getLicense() {
        return license;
    }

    public void setLicense(LicenseType license) {
        this.license = license;
    }

    @XmlElement(name = "collectionID")
    @XmlSchemaType(name = "nameType", namespace = "http://bio.agents")
    public List<String> getCollectionIDs() {
        if (collectionIDs == null) {
            collectionIDs = new ArrayList<>();
        }
        return collectionIDs;
    }

    @XmlElement(name = "maturity")
    public MaturityType getMaturity() {
        return maturity;
    }
    
    public void setMaturity(MaturityType maturity) {
        this.maturity = maturity;
    }
    
    @XmlElement(name = "cost")
    public CostType getCost() {
        return cost;
    }
    
    public void setCost(CostType cost) {
        this.cost = cost;
    }
    
    @XmlElement(name = "accessibility")
    public List<AccessibilityType> getAccessibility() {
        if (accessibility == null) {
            accessibility = new ArrayList<>();
        }
        return accessibility;
    }

    @XmlElement(name = "iechorPlatform")
    public List<iEchorPlatform> getiEchorPlatforms() {
        if (iechorPlatforms == null) {
            iechorPlatforms = new ArrayList<>();
        }
        return iechorPlatforms;
    }
    
    @XmlElement(name = "iechorNode")
    public List<iEchorNode> getiEchorNodes() {
        if (iechorNodes == null) {
            iechorNodes = new ArrayList<>();
        }
        return iechorNodes;
    }
}