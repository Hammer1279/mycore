/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mycore.access.strategies;

import static org.mycore.common.MCRConstants.XLINK_NAMESPACE;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.mycore.access.MCRAccessManager;
import org.mycore.access.MCRRuleAccessInterface;
import org.mycore.datamodel.common.MCRXMLMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.xml.sax.SAXException;

/**
 * Use this class if you want to have a fallback to ancestor access rules.
 * 
 * First a check is done for the MCRObjectID. If no rule for the ID is specified
 * it will be tried to check the permission agains the MCRObjectID of the parent
 * object and so on.
 * 
 * @author Thomas Scheffler (yagee)
 * 
 * @version $Revision$ $Date$
 */
public class MCRParentRuleStrategy implements MCRAccessCheckStrategy {

    private static final Logger LOGGER = LogManager.getLogger(MCRParentRuleStrategy.class);

    /*
     * (non-Javadoc)
     * 
     * @see org.mycore.access.strategies.MCRAccessCheckStrategy#checkPermission(java.lang.String,
     *      java.lang.String)
     */
    public boolean checkPermission(String id, String permission) {
        String currentID;
        MCRRuleAccessInterface mcrRuleAccessInterface = MCRAccessManager.requireRulesInterface();
        for (currentID = id; currentID != null && !mcrRuleAccessInterface.hasRule(currentID, permission);
            currentID = getParentID(currentID)) {
            LOGGER.debug("No access rule specified for: {}. Trying to use parent ID.", currentID);
        }
        LOGGER.debug("Using access rule defined for: {}", currentID);
        return MCRAccessManager.getAccessImpl().checkPermission(currentID, permission);
    }

    private static String getParentID(String objectID) {
        Document parentDoc;
        try {
            parentDoc = MCRXMLMetadataManager.instance().retrieveXML(MCRObjectID.getInstance(objectID));
        } catch (IOException | JDOMException | SAXException e) {
            LOGGER.error("Could not read object: {}", objectID, e);
            return null;
        }
        final Element parentElement = parentDoc.getRootElement().getChild("structure").getChild("parents");
        if (parentElement != null) {
            return parentElement.getChild("parent").getAttributeValue("href", XLINK_NAMESPACE);
        }
        return null;
    }

}
