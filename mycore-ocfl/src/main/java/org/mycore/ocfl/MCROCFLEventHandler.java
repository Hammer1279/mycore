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

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRException;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandler;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryDAOFactory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.model.MCRClassEvent;
import org.mycore.datamodel.classifications2.utils.MCRCategoryTransformer;
import org.mycore.datamodel.common.MCRXMLClassificationManager;

/**
 * @author Tobias Lenhardt [Hammer1279]
 */
public class MCROCFLEventHandler implements MCREventHandler {

    private static final Logger LOGGER = LogManager.getLogger(MCROCFLEventHandler.class);

    protected MCRXMLClassificationManager manager = MCRConfiguration2
        .getSingleInstanceOf("MCR.Classification.Manager", MCRXMLClassificationManager.class)
        .orElse(new MCROCFLXMLClassificationManager());

    // public void getTransaction() {
    //     ServiceLoader<MCRPersistenceTransaction> loader = ServiceLoader.load(MCRPersistenceTransaction.class);
    //     Iterator<MCRPersistenceTransaction> iterator = loader.iterator();
        
    // }

    @Override
    @SuppressWarnings(value = "PMD.SwitchStmtsShouldHaveDefault")
    public void doHandleEvent(MCREvent evt) throws MCRException {
        if (Objects.equals(evt.getObjectType(), MCREvent.CLASS_TYPE)) {
            Map<String, Object> data = getEventData(evt, true);
            MCRCategoryID mcrid = (MCRCategoryID) data.get("mid");
            MCRCategory mcrCg = (MCRCategory) data.get("ctg");
            MCRContent clXml = (MCRContent) data.get("rtx");
            MCRContent cgXml = (MCRContent) data.get("cgx");
            LOGGER.debug("{} handling {} {}", getClass().getName(), mcrid, evt.getEventType());
            LOGGER.debug("\n\nCLASS:\nID: {}\nRootID: {}\n", ((MCRCategory) evt.get("class")).getId(),
                ((MCRCategory) evt.get("class")).getRoot().getId());
            LOGGER.debug("\n\nID-{} is:\nRoot: {}\nRootID: {}\n", mcrid.getID(), mcrid.isRootID(), mcrid.getRootID());
            LOGGER.debug("\n\nCG-{} is:\nClass: {}\nCateg: {}\n", mcrCg.getId(), mcrCg.isClassification(),
                mcrCg.isCategory());
            switch (evt.getEventType()) {
                case MCREvent.CREATE_EVENT:
                case MCREvent.UPDATE_EVENT:
                    if (!evt.containsKey("type")) {
                        manager.fileUpdate(mcrid, mcrCg, clXml, cgXml, evt);
                    } else {
                        switch ((String)evt.get("type")) {
                            case "move":
                                manager.fileMove(mcrid, mcrCg, clXml, cgXml, evt);
                            break;
                        }
                    }
                    break;
                case MCREvent.DELETE_EVENT:
                    manager.fileDelete(mcrid, mcrCg, clXml, cgXml, evt);
                    break;
                case MCREvent.REPAIR_EVENT:
                    break;
                case MCRClassEvent.COMMIT_EVENT:
                    // manager.commitChanges(mcrid.toString(), evt.getEventType(), new Date(), evt);
                    manager.commitChanges(evt, new Date());
                    break;
                default:
                    LOGGER.error("No Method available for {}", evt.getEventType());
            }
        }
    }

    @Override
    public void undoHandleEvent(MCREvent evt) throws MCRException {
        if (Objects.equals(evt.getObjectType(), MCREvent.CLASS_TYPE)) {
            LOGGER.debug("{} handling {} {}", getClass().getName(), ((MCRCategory) evt.get("class")).getId(),
                evt.getEventType());
            manager.undoAction(getEventData(evt), evt);
        }
    }

    /**
     * Returns Event Data in form of an Map. This is used to extract the category data from the MyCoRe Event
     * @param evt MCREvent
     * @return Map:
     * <p>ctg - Category / Classification</p>
     * <p>rtx - Root Document</p>
     * <p>cgx - Category Element</p>
     * <p>mid - MyCoRe ID</p>
     */
    public static final Map<String, Object> getEventData(MCREvent evt) {
        return getEventData(evt, false);
    }

    /**
     * Returns Event Data in form of an Map. This is used to extract the category data from the MyCoRe Event
     * @param evt MCREvent
     * @param counter Append Usage Counters (Resource Intensive)
     * @return Map:
     * <p>ctg - Category / Classification</p>
     * <p>rtx - Root Document</p>
     * <p>cgx - Category Element</p>
     * <p>mid - MyCoRe ID</p>
     */
    public static final Map<String, Object> getEventData(MCREvent evt, boolean counter) {
        MCRCategory cl = (MCRCategory) evt.get("class");
        MCRContent rtxml = new MCRJDOMContent(MCRCategoryTransformer
            .getMetaDataDocument(MCRCategoryDAOFactory.getInstance().getCategory(cl.getRoot().getId(), -1), counter));
        MCRContent cgxml = new MCRJDOMContent(MCRCategoryTransformer.getMetaDataElement(cl, counter));
        MCRCategoryID mcrid = cl.getId();
        Map<String, Object> rtVal = new HashMap<>();
        rtVal.put("ctg", cl);
        rtVal.put("xml", rtxml); // compatibility
        rtVal.put("rtx", rtxml);
        rtVal.put("cgx", cgxml);
        rtVal.put("mid", mcrid);
        return rtVal;
    }
}
