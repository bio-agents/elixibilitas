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

/**
 * 
 * @author Dmitry Repchevsky
 */

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

@XmlType(name = "", propOrder = {"urlFtpType",
                                 "downloadType",
                                 "note",
                                 "version"})
public class Download {
    
    private String url;
    private DownloadType type;
    private String note;
    private String version;
    
    @XmlElement(name = "url")
    @XmlSchemaType(name = "urlftpType", namespace = "http://bio.agents")
    public String getUrlFtpType() {
        return url;
    }
    
    public void setUrlFtpType(String url) {
        this.url = url;
    }
    
    @XmlElement(name = "type")
    public DownloadType getDownloadType() {
        return type;
    }
    
    public void setDownloadType(DownloadType type) {
        this.type = type;
    }
    
    @XmlSchemaType(name = "textType", namespace = "http://bio.agents")
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    public String getNote() {
        return note;
    }
    
    public void setNote(String note) {
        this.note = note;
    }
    
    @XmlSchemaType(name = "versionType", namespace = "http://bio.agents")
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    public String getVersion() {
        return version;
    }
    
    public void setVersion(String version) {
        this.version = version;
    }
}