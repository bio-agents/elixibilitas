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

package es.iechor.bsc.openebench.bioagents;

import es.bsc.inb.iechor.bioagents.model.CostType;
import es.bsc.inb.iechor.bioagents.model.DocumentationType;
import es.bsc.inb.iechor.bioagents.model.DownloadType;
import es.bsc.inb.iechor.bioagents.model.EntityType;
import es.bsc.inb.iechor.bioagents.model.LicenseType;
import es.bsc.inb.iechor.bioagents.model.MaturityType;
import es.bsc.inb.iechor.bioagents.model.OperatingSystemType;
import es.bsc.inb.iechor.bioagents.model.RoleType;
import es.bsc.inb.iechor.bioagents.model.AgentLinkType;
import es.bsc.inb.iechor.openebench.model.agents.Community;
import es.bsc.inb.iechor.openebench.model.agents.Contact;
import es.bsc.inb.iechor.openebench.model.agents.Container;
import es.bsc.inb.iechor.openebench.model.agents.Credit;
import es.bsc.inb.iechor.openebench.model.agents.Datatype;
import es.bsc.inb.iechor.openebench.model.agents.Distributions;
import es.bsc.inb.iechor.openebench.model.agents.Documentation;
import es.bsc.inb.iechor.openebench.model.agents.Publication;
import es.bsc.inb.iechor.openebench.model.agents.Semantics;
import es.bsc.inb.iechor.openebench.model.agents.Support;
import es.bsc.inb.iechor.openebench.model.agents.Agent;
import es.bsc.inb.iechor.openebench.model.agents.AgentType;
import es.bsc.inb.iechor.openebench.model.agents.VMImage;
import es.bsc.inb.iechor.openebench.model.agents.Web;
import es.bsc.inb.iechor.openebench.repository.OpenEBenchEndpoint;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonString;

/**
 * @author Dmitry Repchevsky
 */

public class BioagentsConverter {
   
    public static List<Agent> convert(JsonObject jagent) {
        
        final String id = jagent.getString("bioagentsID", null);
        if (id == null || id.isEmpty()) {
            return Collections.EMPTY_LIST;
        }

        final List<Agent> agents = new ArrayList<>();

        final String _id = id.toLowerCase().trim();
        
        final String name = jagent.getString("name", null);
        final String jhomepage = jagent.getString("homepage", null);
        
        /* inset 'generic' agent with @type: null*/
        Agent generic_agent = new Agent(URI.create(OpenEBenchEndpoint.TOOL_URI_BASE + _id), null);
        generic_agent.setName(name);
        if (jhomepage != null) {
            try {
                Web web = new Web();
                web.setHomepage(new URI(jhomepage));
                generic_agent.setWeb(web);
            } catch(URISyntaxException ex) {
                Logger.getLogger(BioagentsConverter.class.getName()).log(Level.INFO, "invalid homepage: {0}", jhomepage);
            }
        }

        generic_agent.setDescription(jagent.getString("description", null));
        agents.add(generic_agent);

        final Set<String> tags = new HashSet<>();

        final JsonArray versions = jagent.getJsonArray("version");
        for (int j = 0, m = (versions == null || versions.isEmpty()) ? 1 : versions.size(); j < m; j++) {
            StringBuilder idTemplate = new StringBuilder(OpenEBenchEndpoint.TOOL_URI_BASE).append("bioagents:").append(_id);

            final String version = (versions != null && versions.size() > 0) ? versions.getJsonString(j).getString() : null;
            if (version != null) {
                idTemplate.append(':').append(version.replace(' ', '_'));
            }

            idTemplate.append("/%s");
            
            URI homepage = null;
            if (jhomepage != null) {
                try {
                    homepage = URI.create(jhomepage);
                    idTemplate.append('/').append(homepage.getHost());
                } catch (IllegalArgumentException ex) {
                    Logger.getLogger(BioagentsConverter.class.getName()).log(Level.INFO, "invalid homepage: {0}", jhomepage);
                }
            }

            JsonArray jagentTypes = jagent.getJsonArray("agentType");
            for (int i = 0, n = jagentTypes.size(); i < n; i++) {
                final String agent_type;
                final JsonString jagentType = jagentTypes.getJsonString(i);
                try {
                    switch(es.bsc.inb.iechor.bioagents.model.AgentType.fromValue(jagentType.getString())) {
                        case COMMAND_LINE: agent_type = AgentType.COMMAND_LINE.type; break;
                        case WEB_APPLICATION: agent_type = AgentType.WEB_APPLICATION.type; break;
                        case DESKTOP_APPLICATION: agent_type = AgentType.DESKTOP_APPLICATION.type; break;
                        case DATABASE_PORTAL: agent_type = AgentType.DATABASE_PORTAL.type; break;
                        case LIBRARY: agent_type = AgentType.LIBRARY.type; break;
                        case WEB_SERVICE: agent_type = AgentType.WEB_SERVICE.type; break;
                        case WEB_API: agent_type = AgentType.WEB_API.type; break;
                        case SPARQL_ENDPOINT: agent_type = AgentType.SPARQL_ENDPOINT.type; break;
                        case ONTOLOGY: agent_type = AgentType.ONTOLOGY.type; break;
                        case WORKFLOW: agent_type = AgentType.WORKFLOW.type; break;
                        case SCRIPT: agent_type = AgentType.SCRIPT.type; break;
                        case PLUGIN: agent_type = AgentType.PLUGIN.type; break;
                        case SUITE: agent_type = AgentType.SUITE.type; break;
                        case WORKBENCH: agent_type = AgentType.WORKBENCH.type; break;

                        default: continue;
                    }
                } catch(IllegalArgumentException ex) {
                    Logger.getLogger(BioagentsConverter.class.getName()).log(Level.INFO, "unrecognized agentType: {0} for the {1}", new String[] {jagentType.getString(), name});
                    continue;
                }
                final Agent agent = new Agent(URI.create(String.format(idTemplate.toString(), agent_type)), agent_type);
                
                if (!id.equals(_id)) {
                    agent.setExternalId(version == null || version.isEmpty() ? id : id + ":" + version);
                }

                agent.setName(name);
                if (homepage != null) {
                    Web web = new Web();
                    web.setHomepage(homepage);
                    agent.setWeb(web);
                }

                agent.setDescription(jagent.getString("description", null));

                setLanguages(agent.getLanguages(), jagent);
                setOperatingSystems(agent.getOperatingSystems(), jagent);

                addTags(agent, jagent);
                tags.addAll(agent.getTags());
                
                addDocumentation(agent, jagent);
                addPublications(agent, jagent);
//                addContacts(agent, jagent);
                addCredits(agent, jagent);
                addSemantics(agent, jagent);
                addDownloads(agent, jagent);
                addLinks(agent, jagent);
                setLicense(agent, jagent);
                setMaturity(agent, jagent);
                setCost(agent, jagent);

    //                    addOperatingSystems(agent, jagent);
    //                    addAccessibility(agent, jagent);
    //                    addCollectionIDs(agent, jagent);

                agents.add(agent);
            }
        }
        
        if (!tags.isEmpty()) {
            generic_agent.getTags().addAll(tags);
        }
        
        return agents;
    }
    
    private static void addTags(Agent agent, JsonObject jagent) {
        final JsonArray jcollections = jagent.getJsonArray("collectionID");
        if (jcollections.size() > 0) {
            for (int i = 0, n = jcollections.size(); i < n; i++) {
                final JsonString collectionID = jcollections.getJsonString(i);
                agent.getTags().add(collectionID.getString());
            }
        }
    }
    
    private static void addDocumentation(Agent agent, JsonObject jagent) {
        final JsonArray jdocumentations = jagent.getJsonArray("documentation");
        if (jdocumentations.size() > 0) {
            final Documentation documentation = new Documentation();
            agent.setDocumentation(documentation);
            for (int i = 0, n = jdocumentations.size(); i < n; i++) {
                final JsonObject jdocumentation = jdocumentations.getJsonObject(i);
                final String url = jdocumentation.getString("url", null);
                if (url != null) {
                    final String type = jdocumentation.getString("type", null);
                    if (type != null) {
                        try {
                            final URI uri = URI.create(url);
                            final DocumentationType documentationType = DocumentationType.fromValue(type);
                            switch (documentationType) {
                                case API_DOCUMENTATION: documentation.setAPIDocumentation(uri); break;
                                case CITATION_INSTRUCTIONS: documentation.setCitationInstructions(uri); break;
                                case CODE_OF_CONDUCT: documentation.setCodeOfConduct(uri); break;
                                case COMMANDLINE_OPTIONS: documentation.setCommandlineOptions(uri); break;
                                case CONTRIBUTIONS_POLICY: documentation.setContributionsPolicy(uri); break;
                                case FAQ: documentation.setFaq(uri); break;
                                case GENERAL: documentation.setGeneral(uri); break;
                                case GOVERNANCE: documentation.setGovernance(uri); break;
                                case INSTALLATION_INSTRUCTIONS: documentation.setInstallationInstructions(uri); break;
                                case USER_MANUAL: documentation.setManual(uri); break;
                                case RELEASE_NOTES: documentation.setReleaseNotes(uri); break;
                                case TERMS_OF_USE: documentation.setTermsOfUse(uri); break;
                                case TRAINING_MATERIAL: documentation.setTrainingMaterial(uri); break;
                                case OTHER: documentation.getDocumentationLinks().add(uri); break;
                            }

                         } catch(IllegalArgumentException ex) {
                             Logger.getLogger(BioagentsConverter.class.getName()).log(Level.INFO, "unrecognized type: {0}", type);
                         }
                    }
                }
            }
        }
    }
    
    private static void addPublications(Agent agent, JsonObject jagent) {
        JsonArray jpublications = jagent.getJsonArray("publication");
        for (int i = 0, n = jpublications.size(); i < n; i++) {
            Publication publication = new Publication();
            JsonObject jpublication = jpublications.getJsonObject(i);
            
//            try {
//                publication.setType(PublicationType.fromValue(jpublication.getString("type", "")));
//            } catch(IllegalArgumentException ex) {}

            publication.setDOI(jpublication.getString("doi", null));
            publication.setPmid(jpublication.getString("pmid", null));
            publication.setPMCID(jpublication.getString("pmcid", null));
            
            agent.getPublications().add(publication);
        }
    }
    
    private static void addContacts(Agent agent, JsonObject jagent) {
        JsonArray jcontacts = jagent.getJsonArray("contact");
        if (jcontacts != null) {
            for (int i = 0, n = jcontacts.size(); i < n; i++) {
                JsonObject jcontact = jcontacts.getJsonObject(i);
                Contact contact = new Contact();

                contact.setName(jcontact.getString("name", null));
                contact.setEmail(jcontact.getString("email", null));
                contact.setPhone(jcontact.getString("tel", null));

                final String url = jcontact.getString("url", null);
                if (url != null) {
                    try {
                        contact.setUrl(URI.create(url));
                    } catch(IllegalArgumentException ex) {
                        Logger.getLogger(BioagentsConverter.class.getName()).log(Level.INFO, "incorrect contact url: {0}", url);
                    }
                }

                agent.getContacts().add(contact);
            }
        }
    }

    private static void addCredits(Agent agent, JsonObject jagent) {
        JsonArray jcredits= jagent.getJsonArray("credit");
        for (int i = 0, n = jcredits.size(); i < n; i++) {
            JsonObject jcredit = jcredits.getJsonObject(i);
            Credit credit = new Credit();
            
            credit.setName(jcredit.getString("name", null));
            
            credit.setEmail(jcredit.getString("email", null));
//            credit.setGridId(jcredit.getString("gridId", null));
            credit.setOrcid(jcredit.getString("orcidId", null));
            credit.setComment(jcredit.getString("comment", null));
            
            final String type = jcredit.getString("typeEntity", null);
            if (type != null) {
                try {
                    EntityType.fromValue(type);
                    credit.setType(type);
                } catch(IllegalArgumentException ex) {
                    Logger.getLogger(BioagentsConverter.class.getName()).log(Level.INFO, "unrecognized credit type: {0}", type);
                }
            }
            
            final String role = jcredit.getString("typeRole", null);
            if (role != null) {
                try {
                    RoleType.fromValue(role);
                    credit.setRole(role);
                } catch(IllegalArgumentException ex) {
                    Logger.getLogger(BioagentsConverter.class.getName()).log(Level.INFO, "unrecognized credit role: {0}", type);
                }
            }
            
            final String url = jcredit.getString("url", null);
            if (url != null) {
                try {
                    credit.setUrl(URI.create(url));
                } catch(IllegalArgumentException ex) {
                    Logger.getLogger(BioagentsConverter.class.getName()).log(Level.INFO, "incorrect credit url: {0}", url);
                }
            }
            
            agent.getCredits().add(credit);
        }
    }

    private static void addSemantics(Agent agent, JsonObject jagent) {
        Semantics semantics = new Semantics();
        
        addTopics(semantics, jagent);
        
        JsonArray jfunctions = jagent.getJsonArray("function");
        for (int i = 0, n = jfunctions.size(); i < n; i++) {
            JsonObject jfunction = jfunctions.getJsonObject(i);
            addOperations(semantics, jfunction);
            
            JsonArray jinputs = jfunction.getJsonArray("input");
            for (int j = 0, m = jinputs.size(); j < m; j++) {
                Datatype input = new Datatype();
                addDatatype(input, jinputs.getJsonObject(j));
                semantics.getInputs().add(input);
            }

            JsonArray joutputs = jfunction.getJsonArray("output");
            for (int j = 0, m = joutputs.size(); j < m; j++) {
                Datatype output = new Datatype();
                addDatatype(output, joutputs.getJsonObject(j));
                semantics.getOutputs().add(output);
            }
        }

        agent.setSemantics(semantics);
    }
    
    private static void addTopics(Semantics semantics, JsonObject jagent) {
        final JsonArray jtopics = jagent.getJsonArray("topic");
        for (int i = 0, n = jtopics.size(); i < n; i++) {
            final JsonObject jtopic = jtopics.getJsonObject(i);
            final String topic = jtopic.getString("uri", null);
            if (topic != null) {
                try {
                    final URI uri = URI.create(topic);
                    semantics.getTopics().add(uri);
                } catch(IllegalArgumentException ex) {
                    Logger.getLogger(BioagentsConverter.class.getName()).log(Level.INFO, "incorrect topic uri: {0}", topic);
                }
            }
        }
    }

    private static void addOperations(Semantics semantics, JsonObject jfunction) {
        final JsonArray joperations = jfunction.getJsonArray("operation");
        for (int i = 0, n = joperations.size(); i < n; i++) {
            final JsonObject joperation = joperations.getJsonObject(i);
            final String operation = joperation.getString("uri", null);
            if (operation != null) {
                try {
                    final URI uri = URI.create(operation);
                    semantics.getOperations().add(uri);
                } catch(IllegalArgumentException ex) {
                    Logger.getLogger(BioagentsConverter.class.getName()).log(Level.INFO, "incorrect operation uri: {0}", operation);
                }
            }
        }
    }

    private static void addDatatype(Datatype datatype, JsonObject jdatatype) {
        
        final JsonObject jdata = jdatatype.getJsonObject("data");
        if (jdata != null) {
            final String data = jdata.getString("uri", null);
            if (data != null) {
                try {
                    datatype.setDatatype(URI.create(data));
                } catch(IllegalArgumentException ex) {
                    Logger.getLogger(BioagentsConverter.class.getName()).log(Level.INFO, "incorrect data uri: {0}", data);
                }
            }
        }
            
        final JsonArray jformats = jdatatype.getJsonArray("format");
        for (int i = 0, n = jformats.size(); i < n; i++) {
            final JsonObject jformat = jformats.getJsonObject(i);
            final String format = jformat.getString("uri", null);
            if (format != null) {
                try {
                    final URI uri = URI.create(format);
                    datatype.getFormats().add(uri);
                } catch(IllegalArgumentException ex) {
                    Logger.getLogger(BioagentsConverter.class.getName()).log(Level.INFO, "incorrect datatype format uri: {0}", format);
                }
            }
        }
    }

    
    private static void addDownloads(Agent agent, JsonObject jagent) {
        JsonArray jdownloads = jagent.getJsonArray("download");
        
        for (int i = 0, n = jdownloads.size(); i < n; i++) {
            final JsonObject jdownload = jdownloads.getJsonObject(i);
            
            final String url = jdownload.getString("url", null);
            if (url == null || url.isEmpty()) {
                Logger.getLogger(BioagentsConverter.class.getName()).log(Level.INFO, "empty download url");
                continue;                
            }

            final URI uri;
            try {
                uri = URI.create(url);
            } catch(IllegalArgumentException ex) {
                Logger.getLogger(BioagentsConverter.class.getName()).log(Level.INFO, "incorrect download url: {0}", url);
                continue;
            }
            
            final String type = jdownload.getString("type", null);
            if (type == null || type.isEmpty()) {
                Logger.getLogger(BioagentsConverter.class.getName()).log(Level.INFO, "no download type set");
                continue;                
            }
            
            try {
                DownloadType downloadType = DownloadType.fromValue(type);
                
                Distributions distributions = agent.getDistributions();
                if (distributions == null) {
                    distributions = new Distributions();
                }
                
                switch(downloadType) {
                    case BINARIES:       distributions.getBinaryDistributions().add(uri);
                                         break;
                    case SOURCE_CODE:    distributions.getSourcecodeDistributions().add(uri);
                                         break;
                    case CONTAINER_FILE: final String cformat = jdownload.getString("containerFormat", null);
                                         Container container;
//                                         if (cformat == null) {
//                                             Logger.getLogger(BioagentsRepositoryIterator.class.getName()).log(Level.INFO, "no container format set");
//                                             container = new Container("unknown");
//                                         } else {
//                                             try {
//                                                ContainerFormatType.fromValue(cformat);
//                                                container = new Container(cformat);
//                                             } catch(IllegalArgumentException ex) {
//                                                Logger.getLogger(BioagentsRepositoryIterator.class.getName()).log(Level.INFO, "unrecognized container format: {0}", cformat);
//                                                container = new Container("unknown");
//                                             }
//                                         }
                                         container = new Container("unknown");
                                         container.setURI(uri);
                                         distributions.getContainers().add(container);
                                         break;
                    case VM_IMAGE:       final String dformat = jdownload.getString("diskFormat", null);
                                         VMImage vmImage;
//                                         if (dformat == null) {
//                                             Logger.getLogger(BioagentsRepositoryIterator.class.getName()).log(Level.INFO, "no vm image format set");
//                                             vmImage = new VMImage("unknown");
//                                         } else {
//                                             try {
//                                                DiskFormatType.fromValue(dformat);
//                                                vmImage = new VMImage(dformat);
//                                             } catch(IllegalArgumentException ex) {
//                                                Logger.getLogger(BioagentsRepositoryIterator.class.getName()).log(Level.INFO, "unrecognized vm image  format: {0}", dformat);
//                                                vmImage = new VMImage("unknown");
//                                             }
//                                         }
                                         vmImage = new VMImage("unknown");
                                         vmImage.setURI(uri);
                                         distributions.getVirtualMachineImages().add(vmImage);
                                         break;

                    default: continue;
                }
                
                agent.setDistributions(distributions);
            } catch(IllegalArgumentException ex) {
                Logger.getLogger(BioagentsConverter.class.getName()).log(Level.INFO, "unrecognized download type: {0}", type);
            }
        }
    }

    private static void addLinks(Agent agent, JsonObject jagent) {
        final JsonArray jlinks = jagent.getJsonArray("link");
        for (int i = 0, n = jlinks.size(); i < n; i++) {
            final JsonObject jlink = jlinks.getJsonObject(i);
            
            final String url = jlink.getString("url", null);
            if (url == null || url.isEmpty()) {
                Logger.getLogger(BioagentsConverter.class.getName()).log(Level.INFO, "empty link url");
                continue;                
            }
            
            final URI uri;
            try {
                uri = URI.create(url);
            } catch(IllegalArgumentException ex) {
                Logger.getLogger(BioagentsConverter.class.getName()).log(Level.INFO, "incorrect link url: {0}", url);
                continue;
            }
            
            final String type = jlink.getString("type", null);
            if (type == null || type.isEmpty()) {
                Logger.getLogger(BioagentsConverter.class.getName()).log(Level.INFO, "no download type set");
                continue;                
            }
            try {
                final AgentLinkType agentLinkType = AgentLinkType.fromValue(type);
                
                Support support = agent.getSupport();
                if (support == null) {
                    support = new Support();
                }
                
                Community community = agent.getCommunity();
                if (community == null) {
                    community = new Community();
                }
                
                switch(agentLinkType) {
                    case REPOSITORY:    agent.getRepositories().add(uri);
                                        break;
                    case HELPDESK:      agent.setSupport(support);
                                        support.setHelpdesk(uri);
                                        break;
                    case ISSUE_TRACKER: agent.setSupport(support);
                                        support.setIssueTracker(uri);
                                        break;
                    case MAILING_LIST:  agent.setSupport(support);
                                        support.setMailingList(uri);
                                        break;
                    case SOCIAL_MEDIA:  agent.setCommunity(community);
                                        community.getSocialMedia().add(uri);
                                        break;
                    
                }
            } catch(IllegalArgumentException ex) {
                Logger.getLogger(BioagentsConverter.class.getName()).log(Level.INFO, "unrecognized link type: {0}", type);
            }
        }
    }

    private static void setLicense(Agent agent, JsonObject jagent) {
        final String license = jagent.getString("license", null);
        if (license != null) {
            try {
                LicenseType.fromValue(license); // just to check if valid.
                agent.setLicense(license);
            } catch(IllegalArgumentException ex) {
                Logger.getLogger(BioagentsConverter.class.getName()).log(Level.INFO, "unrecognized license: {0}", license);
            }
        }
    }
    
    private static void setMaturity(Agent agent, JsonObject jagent) {
        final String maturity = jagent.getString("maturity", null);
        if (maturity != null) {
            try {
                MaturityType.fromValue(maturity);
                agent.setMaturity(maturity);
            } catch(IllegalArgumentException ex) {
                Logger.getLogger(BioagentsConverter.class.getName()).log(Level.INFO, "unrecognized maturity: {0}", maturity);
            }
        }
    }

    private static void setCost(Agent agent, JsonObject jagent) {
        final String cost = jagent.getString("cost", null);
        if (cost != null) {
            try {
                CostType.fromValue(cost);
                agent.setCost(cost);
            } catch(IllegalArgumentException ex) {
                Logger.getLogger(BioagentsConverter.class.getName()).log(Level.INFO, "unrecognized cost: {0}", cost);
            }
        }
    }
    
    private static void setOperatingSystems(List<String> operatingSystems, JsonObject jagent) {
        final JsonArray jOperatingSystems = jagent.getJsonArray("operatingSystem");
        if (jOperatingSystems != null) {
            for (int i = 0, n = jOperatingSystems.size(); i < n; i++) {
                final String operatingSystem = jOperatingSystems.getString(i, null);
                if (operatingSystem == null || operatingSystem.isEmpty()) {
                    Logger.getLogger(BioagentsConverter.class.getName()).log(Level.INFO, "empty operating system in agent : {0}", jagent.getString("@id", ""));
                } else {
                    try {
                        OperatingSystemType.fromValue(operatingSystem);
                        operatingSystems.add(operatingSystem);
                    } catch(IllegalArgumentException ex) {
                        Logger.getLogger(BioagentsConverter.class.getName()).log(Level.INFO, "unrecognized operating system : {0}", operatingSystem);
                    }
                }
            }
        }
    }
    
    private static void setLanguages(List<String> list, JsonObject jagent) {
        final JsonArray jlanguages = jagent.getJsonArray("language");
        if (jlanguages != null) {
            for (int i = 0, n = jlanguages.size(); i < n; i++) {
               final String language = jlanguages.getString(i, null);
               if (language != null) {
                   list.add(language);
               }
            }
        }
    }

}
