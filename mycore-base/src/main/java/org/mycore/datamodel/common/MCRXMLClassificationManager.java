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

package org.mycore.datamodel.common;

import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.content.MCRContent;
import org.mycore.common.events.MCREvent;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryID;

/**
 * Interface for Native Drive Storage Manager for MyCoRe Classifications
 * @author Tobias Lenhardt [Hammer1279]
 */


public interface MCRXMLClassificationManager {

    default void fileUpdate(MCRCategoryID mcrid, MCRCategory mcrCg, MCRContent xml, MCREvent eventData) {
        fileUpdate(mcrid, mcrCg, xml, xml, eventData);
    }

    void fileUpdate(MCRCategoryID mcrid, MCRCategory mcrCg, MCRContent clXml, MCRContent cgXml,
        MCREvent eventData);

    default void fileDelete(MCRCategoryID mcrid, MCRCategory mcrCg, MCRContent xml, MCREvent eventData) {
        fileDelete(mcrid, mcrCg, xml, xml, eventData);
    }

    void fileDelete(MCRCategoryID mcrid, MCRCategory mcrCg, MCRContent clXml, MCRContent cgXml,
        MCREvent eventData);

    void fileMove(Map<String, Object> data, MCREvent eventData);

    default void commitSession(MCRSession session) {
        commitSession(Optional.ofNullable(session));
    }

    @SuppressWarnings("unchecked")
    default void commitSession(Optional<MCRSession> sessionOpt) {
        MCRSession session = sessionOpt.orElse(MCRSessionMgr.getCurrentSession());
        ArrayList<MCREvent> list = (ArrayList<MCREvent>) session.get("classQueue");
        list.forEach(this::commitChanges);
        session.deleteObject("classQueue");
    }

    void commitChanges(MCREvent evt);

    default void rollbackSession(MCRSession session) {
        rollbackSession(Optional.ofNullable(session));
    }

    @SuppressWarnings("unchecked")
    default void rollbackSession(Optional<MCRSession> sessionOpt) {
        MCRSession session = sessionOpt.orElse(MCRSessionMgr.getCurrentSession());
        ArrayList<MCREvent> list = (ArrayList<MCREvent>) session.get("classQueue");
        if (list == null) {
            LogManager.getLogger(MCRXMLClassificationManager.class).error("List is empty!");
            return;
        }
        list.forEach(this::dropChanges);
        session.deleteObject("classQueue");
    }

    void dropChanges(MCREvent evt);

    void dropChanges(MCREvent evt, Map<String, Object> data);

    void dropChanges(MCRCategoryID mcrid);

    void undoAction(Map<String, Object> data, MCREvent evt);

    /**
     * Load a Classification from the OCFL Store.
     * @param MCRCategoryID ID of the Category
     * @return MCRContent
     */
    default MCRContent retrieveContent(MCRCategoryID mcrid) {
        return retrieveContent(mcrid, null);
    }

    /**
     * Load a Classification from the OCFL Store.
     * @param mcrid ID of the Category
     * @param revision Revision of the Category
     * @return Content of the Classification
     */
    MCRContent retrieveContent(MCRCategoryID mcrid, String revision);

}
