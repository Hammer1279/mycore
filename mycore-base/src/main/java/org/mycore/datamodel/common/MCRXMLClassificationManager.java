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
import java.util.Date;
import java.util.Map;

import org.mycore.common.MCRSession;
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

    void fileMove(MCRCategoryID mcrid, MCRCategory mcrCg, MCRContent clXml, MCRContent cgXml,
        MCREvent eventData);

    @SuppressWarnings("unchecked")
    default void commitSession(MCRSession session) {
        ArrayList<MCREvent> list = (ArrayList<MCREvent>)session.get("classQueue");
        list.forEach(event -> {
            commitChanges(event, new Date());
        });
        session.deleteObject("classQueue");
    }
    
    void commitChanges(MCREvent evt, Date lastModified);

    @SuppressWarnings("unchecked")
    default void rollbackSession(MCRSession session) {
        ArrayList<MCREvent> list = (ArrayList<MCREvent>)session.get("classQueue");
        list.forEach(event -> {
            dropChanges(event);
        });
        session.deleteObject("classQueue");
    }

    void dropChanges(MCREvent evt);

    void dropChanges(MCREvent evt, Map<String, Object> data);

    void undoAction(Map<String, Object> data, MCREvent evt);

    /**
     * Undoes an Action after a Failed Event
     * @param mcrId MCRCategoryID
     * @param mcrCg MCRCategory
     * @param xml MCRContent
     * @param eventData MCREvent
     * @return Bool - if undo was successful
     * @deprecated use {@link #undoAction(Map, MCREvent)} with
     * {@link MCROCFLEventHandler#getEventData(MCREvent, boolean)}
     */
    @Deprecated(forRemoval = true)
    Boolean undoAction(MCRCategoryID mcrId, MCRCategory mcrCg, MCRContent xml, MCREvent eventData);

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
