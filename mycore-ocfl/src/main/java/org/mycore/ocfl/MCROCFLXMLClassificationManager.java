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

import java.io.IOException;
import java.io.InputStream;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.mycore.common.MCRException;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRUsageException;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.common.content.MCRStreamContent;
import org.mycore.common.events.MCREvent;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.impl.MCRCategoryImpl;
import org.mycore.datamodel.classifications2.utils.MCRCategoryTransformer;
import org.mycore.datamodel.common.MCRXMLClassificationManager;
import org.xml.sax.SAXException;

import edu.wisc.library.ocfl.api.MutableOcflRepository;
import edu.wisc.library.ocfl.api.OcflOption;
import edu.wisc.library.ocfl.api.OcflRepository;
import edu.wisc.library.ocfl.api.exception.NotFoundException;
import edu.wisc.library.ocfl.api.model.ObjectVersionId;
import edu.wisc.library.ocfl.api.model.VersionInfo;

/**
 * OCFL File Manager for MyCoRe Classifications
 * @author Tobias Lenhardt [Hammer1279]
 */
public class MCROCFLXMLClassificationManager implements MCRXMLClassificationManager {

    private static final Logger LOGGER = LogManager.getLogger(MCROCFLXMLClassificationManager.class);

    protected static final String CLASSIFICATION_PREFIX = "mcrclass:";

    // include the "/classification" directory
    private static final boolean INC_CLSDIR = false;

    private String rootFolder = INC_CLSDIR ? "classification/" : "";

    @MCRProperty(name = "Repository", required = true)
    public String repositoryKey;

    protected static final Map<String, Character> MESSAGE_TYPE_MAPPING = Collections.unmodifiableMap(Map.ofEntries(
        Map.entry(MCREvent.CREATE_EVENT, MCROCFLMetadataVersion.CREATED),
        Map.entry(MCREvent.UPDATE_EVENT, MCROCFLMetadataVersion.UPDATED),
        Map.entry(MCREvent.DELETE_EVENT, MCROCFLMetadataVersion.DELETED),
        Map.entry(MCREvent.REPAIR_EVENT, MCROCFLMetadataVersion.REPAIRED)));

    protected static char convertMessageToType(String message) throws MCRPersistenceException {
        if (!MESSAGE_TYPE_MAPPING.containsKey(message)) {
            throw new MCRPersistenceException("Cannot identify version type from message '" + message + "'");
        }
        return MESSAGE_TYPE_MAPPING.get(message);
    }

    protected MutableOcflRepository getRepository() {
        return (MutableOcflRepository) MCROCFLRepositoryProvider.getRepository(repositoryKey);
    }

    public void fileUpdate(MCRCategoryID mcrid, MCRCategory mcrCg, MCRContent clXml, MCRContent cgXml,
        MCREvent eventData) {

        String objName = getName(mcrid);
        String message = eventData.getEventType();
        Date lastModified = new Date();
        MCRContent xml = mcrCg.isClassification() ? clXml : cgXml;
        try {
            lastModified = new Date(TimeUnit.SECONDS.toMillis(xml.lastModified()));
        } catch (IOException e1) {
            LOGGER.throwing(Level.ERROR, new MCRException("Cannot Fetch last Modified"));
        }

        try (InputStream objectAsStream = xml.getInputStream()) {
            VersionInfo versionInfo = buildVersionInfo(message, lastModified);
            getRepository().stageChanges(ObjectVersionId.head(objName), versionInfo, updater -> {
                updater.writeFile(objectAsStream, buildFilePath(mcrCg), OcflOption.OVERWRITE);
            });
        } catch (IOException e) {
            throw new MCRPersistenceException("Failed to update object '" + objName + "'", e);
        } catch (IllegalArgumentException e) {
            // this gets thrown when the deduplication deletes an object and it tries to move it
            LOGGER.warn("IllegalArgumentException happened, Ignoring...");
            // dropChanges(mcrid);
            // LOGGER.error("Something has gone Wrong!", e);
        }

    }

    public void fileDelete(MCRCategoryID mcrid, MCRCategory mcrCg, MCRContent clXml, MCRContent cgXml,
        MCREvent eventData) {
        String objName = getName(mcrid);
        String message = eventData.getEventType();
        Date lastModified = new Date();
        try {
            lastModified = new Date(TimeUnit.SECONDS.toMillis(clXml.lastModified()));
        } catch (IOException e1) {
            LOGGER.throwing(Level.ERROR, new MCRException("Cannot Fetch last Modified"));
        }
        VersionInfo versionInfo = buildVersionInfo(message, lastModified);
        getRepository().stageChanges(ObjectVersionId.head(objName), versionInfo, updater -> {
            updater.removeFile(buildFilePath(mcrCg));
        });
    }

    public void fileMove(Map<String, Object> data, MCREvent eventData) {
        MCRCategoryImpl oldParent = (MCRCategoryImpl)data.get("ctg");
        MCRCategoryImpl newParent = (MCRCategoryImpl)eventData.get("parent");
        int index = (int)eventData.get("index");
        MCRCategoryImpl mcrCg = (MCRCategoryImpl)data.get("ctg");
        fileDelete(mcrCg.getId(), mcrCg, (MCRContent)data.get("xml"), eventData);
        mcrCg.setParent(newParent);
        newParent.getChildren().add(index, mcrCg);
        MCRContent newParentXml = new MCRJDOMContent(MCRCategoryTransformer.getMetaDataElement(newParent, true));
        MCRContent oldParentXml = new MCRJDOMContent(MCRCategoryTransformer.getMetaDataElement(oldParent, true));
        fileUpdate(newParent.getId(), newParent, newParentXml, eventData);
        fileUpdate(oldParent.getId(), oldParent, oldParentXml, eventData);
    }

    public void commitChanges(MCREvent evt, Date lastModified) {
        Map<String, Object> data = MCROCFLEventHandler.getEventData(evt, true);
        MCRCategoryID mcrid = (MCRCategoryID) data.get("mid");
        MCRCategory mcrCg = (MCRCategory) data.get("ctg");
        MCRContent rtXml = (MCRContent) data.get("rtx"); // class
        MCREvent event = (MCREvent) evt.get("event");
        if (event == null) {
            event = evt;
        }
        if (mcrCg.isCategory()) {
            // with every single change, the main file gets added again and again
            fileUpdate(mcrCg.getRoot().getId(), mcrCg.getRoot(), rtXml, evt);
            // MCREvent event2 = new MCREvent(MCREvent.CLASS_TYPE, MCREvent.UPDATE_EVENT);
            // commitChanges(event2, lastModified);
        }
        VersionInfo versionInfo = buildVersionInfo(event.getEventType(), lastModified);
        getRepository().commitStagedChanges(getName(mcrid), versionInfo);
    }

    // public void commitSession(Optional<MCRSession> sessionOpt) {
    //     MCRSession session = sessionOpt.orElse(MCRSessionMgr.getCurrentSession());
    //     ArrayList<MCREvent> list = (ArrayList<MCREvent>)session.get("classQueue");
    //     Set<MCRCategory> parentSet = new HashSet<>();
    //     list.forEach(event -> {
    //         // every staged change calls a commit, is it necessary?
    //         commitChanges(event, new Date());
    //     });
    //     session.deleteObject("classQueue");
    // }

    public void dropChanges(MCREvent evt) {
        dropChanges(evt, MCROCFLEventHandler.getEventData(evt));
    }

    public void dropChanges(MCREvent evt, Map<String, Object> data) {
        MCRCategoryID mcrid = (MCRCategoryID) data.get("mid");
        dropChanges(mcrid);
    }

    private void dropChanges(MCRCategoryID mcrid) {
        if (getRepository().hasStagedChanges(getName(mcrid))) {
            getRepository().purgeStagedChanges(getName(mcrid));
            LOGGER.debug("Dropped changes of {}", getName(mcrid));
        } else {
            LOGGER.debug("No changes to Drop for {}", getName(mcrid));
        }
    }

    /**
     * Undoes an Action after a Failed Event
     * @param data {@link MCROCFLEventHandler#getEventData(MCREvent, boolean)}
     * @param evt {@link MCREvent} | {@link MCREvent}
     */
    public void undoAction(Map<String, Object> data, MCREvent evt) {
        dropChanges(evt, data);
    }

    /**
    * Undoes an Action after a Failed Event
    * @param mcrId MCRCategoryID
    * @param mcrCg MCRCategory
    * @param xml MCRContent
    * @param eventData MCREvent
    * @return Bool - if undo was successful
    * @deprecated use {@link #undoAction(Map, MCREvent)}
    */
    @Deprecated(forRemoval = true)
    public Boolean undoAction(MCRCategoryID mcrId, MCRCategory mcrCg, MCRContent xml, MCREvent eventData) {
        // TODO unfinished, make something to undo changes if something failed without dataloss
        undoAction(MCROCFLEventHandler.getEventData(eventData), eventData);
        return true;
    }

    /**
     * Load a Classification from the OCFL Store.
     * @param mcrid ID of the Category
     * @param revision Revision of the Category
     * @return Content of the Classification
     */
    public MCRContent retrieveContent(MCRCategoryID mcrid, String revision) {
        String objName = getName(mcrid);
        OcflRepository repo = getRepository();
        ObjectVersionId vId = revision != null ? ObjectVersionId.version(objName, revision)
            : ObjectVersionId.head(objName);

        try {
            repo.getObject(vId);
        } catch (NotFoundException e) {
            throw new MCRUsageException("Object '" + objName + "' could not be found", e);
        }

        if (convertMessageToType(repo.getObject(vId).getVersionInfo().getMessage()) == MCROCFLMetadataVersion.DELETED) {
            throw new MCRUsageException("Cannot read already deleted object '" + objName + "'");
        }

        try (InputStream storedContentStream = repo.getObject(vId).getFile(buildFilePath(mcrid)).getStream()) {
            Document xml = new MCRStreamContent(storedContentStream).asXML();
            if (revision != null) {
                xml.getRootElement().setAttribute("rev", revision);
            }
            return new MCRJDOMContent(xml);
        } catch (JDOMException | SAXException | IOException e) {
            throw new MCRPersistenceException("Can not parse XML from OCFL-Store", e);
        }
    }

    protected String getName(MCRCategoryID mcrid) {
        return CLASSIFICATION_PREFIX + mcrid.getRootID();
    }

    @Deprecated(forRemoval = false)
    protected String buildFilePath(MCRCategoryID mcrid) {
        if (mcrid.isRootID()) {
            return rootFolder + mcrid.toString() + ".xml";
        } else {
            return rootFolder + mcrid.getRootID() + '/' + mcrid.toString() + ".xml";
        }
    }

    protected String buildFilePath(MCRCategory mcrCg) {
        StringBuilder builder = new StringBuilder(rootFolder);
        if (mcrCg.isClassification()) {
            builder.append(mcrCg.getId()).append(".xml");
        } else if (mcrCg.isCategory()) {
            ArrayList<String> list = new ArrayList<>();
            list.add(".xml");
            MCRCategory cg = mcrCg;
            int level = mcrCg.getLevel();
            while (level > 0) {
                MCRCategory cgt = cg;
                list.add(cg.getId().toString());
                list.add("/");
                cg = cgt.getParent();
                level = cg.getLevel();
            }
            list.add(cg.getId().toString());
            for (int i = list.size() - 1; i >= 0; i--) {
                builder.append(list.get(i));
            }
        } else {
            throw new MCRException("The MCRClass is of Invalid Type, it must be either a Class or Category");
        }
        return builder.toString();
    }

    protected VersionInfo buildVersionInfo(String message, Date versionDate) {
        VersionInfo versionInfo = new VersionInfo();
        versionInfo.setMessage(message);
        versionInfo.setCreated((versionDate == null ? new Date() : versionDate).toInstant().atOffset(ZoneOffset.UTC));
        return versionInfo;
    }

}
