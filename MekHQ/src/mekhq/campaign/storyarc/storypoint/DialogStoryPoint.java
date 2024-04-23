package mekhq.campaign.storyarc.storypoint;

import megamek.Version;
import mekhq.campaign.Campaign;
import mekhq.campaign.storyarc.Personality;
import mekhq.campaign.storyarc.StoryPoint;
import mekhq.campaign.storyarc.StorySplash;
import mekhq.utilities.MHQXMLUtility;
import org.apache.logging.log4j.LogManager;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.awt.*;
import java.io.PrintWriter;
import java.text.ParseException;
import java.util.UUID;

public abstract class DialogStoryPoint extends StoryPoint {

    /** A StorySplash image to display in a dialog. It can return a null image */
    private StorySplash storySplash;

    /**
     * The id of a personality who is associated with this StoryPoint. May be null.
     */
    private UUID personalityId;

    public DialogStoryPoint() {
        super();
        storySplash = new StorySplash();
    }


    public Image getImage() {
        if(storySplash.isDefault()) {
            return null;
        }
        return storySplash.getImage();
    }

    /**
     * Get the {@link Personality Personality} associated with this StoryPoint.
     * @return A {@link Personality Personality} or null if no Personality is associated with the StoryPoint.
     */
    public Personality getPersonality() {
        if (null == personalityId) {
            return null;
        }
        return getStoryArc().getPersonality(personalityId);
    }

    @Override
    protected void writeToXmlBegin(PrintWriter pw1, int indent) {
        super.writeToXmlBegin(pw1, indent);
        MHQXMLUtility.writeSimpleXMLTag(pw1, ++indent, "personalityId", personalityId);
        storySplash.writeToXML(pw1, indent);
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn, Campaign c, Version version) throws ParseException {
        NodeList nl = wn.getChildNodes();

        for (int x = 0; x < nl.getLength(); x++) {
            Node wn2 = nl.item(x);
            try {
                if (wn2.getNodeName().equalsIgnoreCase("personalityId")) {
                    personalityId = UUID.fromString(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase(StorySplash.XML_TAG)) {
                    storySplash = StorySplash.parseFromXML(wn2);
                }
            } catch (Exception e) {
                LogManager.getLogger().error(e);
            }
        }
    }

}
