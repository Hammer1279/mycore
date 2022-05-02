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

import org.mycore.common.content.MCRContent;
import org.mycore.datamodel.classifications2.MCRCategoryID;

/**
 * Interface for Native Drive Storage Manager for MyCoRe Classifications
 * @author Tobias Lenhardt [Hammer1279]
 */
public interface MCRXMLClassificationManager {

    // void rollbackSession(Optional<MCRSession> sessionOpt);

    // void commitChanges(MCREvent evt);

    // @SuppressWarnings("unchecked")
    // default void commitSession(Optional<MCRSession> sessionOpt) {
    //     MCRSession session = sessionOpt.orElse(MCRSessionMgr.getCurrentSession());
    //     ArrayList<MCREvent> list = (ArrayList<MCREvent>) session.get("classQueue");
    //     list.forEach(this::commitChanges);
    //     session.deleteObject("classQueue");
    // }

    /**
     * Load a Classification from the Store
     * @param MCRCategoryID ID of the Category
     * @return MCRContent of the Classification
     */
    default MCRContent retrieveContent(MCRCategoryID mcrid) {
        return retrieveContent(mcrid, null);
    }

    /**
     * Load a Classification from the Store
     * @param mcrid ID of the Category
     * @param revision Revision of the Category
     * @return MCRContent of the Classification
     */
    MCRContent retrieveContent(MCRCategoryID mcrid, String revision);

}
