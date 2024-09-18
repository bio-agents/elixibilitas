package es.iechor.bsc.bioagents.parser.model;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;

/**
 *
 * @author Dmitry Repchevsky
 */

@XmlEnum(EnumType.class)
public enum AgentLinkType {
    @XmlEnumValue("Browser") BROWSER("Browser"),
    @XmlEnumValue("Helpdesk") HELPDESK("Helpdesk"),
    @XmlEnumValue("Issue tracker") ISSUE_TRACKER("Issue tracker"),
    @XmlEnumValue("Mailing list") MAILING_LIST("Mailing list"),
    @XmlEnumValue("Mirror") MIRROR("Mirror"),
    @XmlEnumValue("Registry") REGISTRY("Registry"),
    @XmlEnumValue("Repository") REPOSITORY("Repository"),
    @XmlEnumValue("Social media") SOCIAL_MEDIA("Social media");
    
    public final String value;
    
    private AgentLinkType(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }

    public static AgentLinkType fromValue(String value) {
        for (AgentLinkType type: AgentLinkType.values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException(value);
    }
}
