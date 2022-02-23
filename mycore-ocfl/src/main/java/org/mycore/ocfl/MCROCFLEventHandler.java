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
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRObjectDerivate;
import org.mycore.datamodel.metadata.MCRObjectID;

/**
 * Event Handler for (Classification) Events
 * @author Tobias Lenhardt [Hammer1279]
 */
public class MCROCFLEventHandler extends MCREventHandlerBase {

    MCROCFLXMLClassificationManager manager = new MCROCFLXMLClassificationManager();
    MCROCFLDerivateStoreManager storeManager = new MCROCFLDerivateStoreManager();

    @Override
    protected void handleClassificationCreated(MCREvent evt, MCRCategory obj) {
        classUpdate(evt, obj);
    }

    @Override
    protected void handleClassificationUpdated(MCREvent evt, MCRCategory obj) {
        classUpdate(evt, obj);
    }

    @Override
    protected void handleClassificationDeleted(MCREvent evt, MCRCategory obj) {
        classDelete(evt, obj);
    }

    @Override
    protected void handleClassificationRepaired(MCREvent evt, MCRCategory obj) {
        classUpdate(evt, obj);
    }


    // protected void handleDerivateCreated(MCREvent evt, MCRDerivate der) {
    //     // TODO implement DerivateStoreManager here
    // }

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

    private void derivateUpdate(MCREvent evt, MCRDerivate der) {
        MCRObjectDerivate objectDerivate = der.getDerivate();
        MCRObjectID derivateID = der.getId();
        storeManager.derivateUpdate(derivateID, der, objectDerivate, evt);
    }
    private void derivateDelete(MCREvent evt, MCRDerivate der) {

    }

}
