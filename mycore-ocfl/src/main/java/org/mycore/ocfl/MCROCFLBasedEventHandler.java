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

package org.mycore.ocfl;

import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandlerBase;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.utils.MCRCategoryTransformer;

/**
 * Event Handler for OCFL Events
 * @author Tobias Lenhardt [Hammer1279]
 */
@Deprecated(forRemoval = false)
public class MCROCFLBasedEventHandler extends MCREventHandlerBase {

    MCROCFLXMLClassificationManager manager = new MCROCFLXMLClassificationManager();

    // Classification Manager

    /**
     * {@inheritDoc}
     */
    @Override
    protected void handleClassificationCreated(MCREvent evt, MCRCategory obj) {
        classUpdate(evt, obj);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void handleClassificationUpdated(MCREvent evt, MCRCategory obj) {
        classUpdate(evt, obj);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void handleClassificationDeleted(MCREvent evt, MCRCategory obj) {
        classDelete(evt, obj);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void handleClassificationRepaired(MCREvent evt, MCRCategory obj) {
        classUpdate(evt, obj);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void undoClassificationCreated(MCREvent evt, MCRCategory obj) {
        classUndo(evt, obj);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void undoClassificationUpdated(MCREvent evt, MCRCategory obj) {
        classUndo(evt, obj);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void undoClassificationDeleted(MCREvent evt, MCRCategory obj) {
        classUndo(evt, obj);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void undoClassificationRepaired(MCREvent evt, MCRCategory obj) {
        classUndo(evt, obj);
    }


    private void classUpdate(MCREvent evt, MCRCategory obj) {
        MCRContent xml = new MCRJDOMContent(MCRCategoryTransformer.getMetaDataDocument(obj, false));
        MCRCategoryID mcrid = obj.getId();
        manager.fileUpdate(mcrid, obj, xml, evt);
    }
    private void classDelete(MCREvent evt, MCRCategory obj) {
        MCRContent xml = new MCRJDOMContent(MCRCategoryTransformer.getMetaDataDocument(obj, false));
        MCRCategoryID mcrid = obj.getId();
        manager.fileDelete(mcrid, obj, xml, evt);
    }

    private void classUndo(MCREvent evt, MCRCategory obj) {
        MCRContent xml = new MCRJDOMContent(MCRCategoryTransformer.getMetaDataDocument(obj, false));
        MCRCategoryID mcrid = obj.getId();
        manager.undoAction(mcrid, obj, xml, evt);
    }

}
