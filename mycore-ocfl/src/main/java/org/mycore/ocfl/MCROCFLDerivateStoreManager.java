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

import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.events.MCREvent;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRObjectDerivate;
import org.mycore.datamodel.metadata.MCRObjectID;

import edu.wisc.library.ocfl.api.OcflRepository;
import edu.wisc.library.ocfl.api.model.VersionInfo;

/**
 * Class to manage Derivate storage for Objects in the OCFL Store
 * @author Tobias Lenhardt [Hammer1279]
 */
public class MCROCFLDerivateStoreManager {

    private static final Logger LOGGER = LogManager.getLogger(MCROCFLDerivateStoreManager.class);

    protected static final String DERIVATE_PREFIX = "mcrderivate:";

    private final String repositoryKey = MCRConfiguration2.getString("MCR.Classification.Manager.Repository")
        .orElse("Default");
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
    
        public OcflRepository getRepository() {
            return MCROCFLRepositoryProvider.getRepository(repositoryKey);
        }

        public void derivateUpdate(MCRObjectID derivateID, MCRDerivate derivate, MCRObjectDerivate objectDerivate, MCREvent event) {
            // TODO figure something out how to store this, see MCRMetalIFS, MCRMetalLink, MCRObjectDerivate
            // manager.create(derivateID, xml, lastmodified);
        }

        // public void fileUpdate(MCRCategoryID mcrid, MCRCategory mcrCg, MCRContent xml, MCREvent eventData) {
    
        //     String objName = getName(mcrid);
        //     String message = eventData.getEventType();
        //     Date lastModified = new Date();
        //     try {
        //         lastModified = new Date(TimeUnit.SECONDS.toMillis(xml.lastModified()));
        //     } catch (IOException e1) {
        //         LOGGER.throwing(Level.ERROR, new MCRException("Cannot Fetch last Modified"));
        //     }
    
        //     try (InputStream objectAsStream = xml.getInputStream()) {
        //         VersionInfo versionInfo = buildVersionInfo(message, lastModified);
        //         getRepository().updateObject(ObjectVersionId.head(objName), versionInfo, updater -> {
        //             updater.writeFile(objectAsStream, buildFilePath(mcrid), OcflOption.OVERWRITE);
        //         });
        //     } catch (IOException e) {
        //         throw new MCRPersistenceException("Failed to update object '" + objName + "'", e);
        //     }
    
        // }
    
        // public void fileDelete(MCRCategoryID mcrid, MCRCategory mcrCg, MCRContent xml, MCREvent eventData) {
        //     String objName = getName(mcrid);
        //     String message = eventData.getEventType();
        //     Date lastModified = new Date();
        //     try {
        //         lastModified = new Date(TimeUnit.SECONDS.toMillis(xml.lastModified()));
        //     } catch (IOException e1) {
        //         LOGGER.throwing(Level.ERROR, new MCRException("Cannot Fetch last Modified"));
        //     }
        //     VersionInfo versionInfo = buildVersionInfo(message, lastModified);
        //     getRepository().updateObject(ObjectVersionId.head(objName), versionInfo, updater -> {
        //         updater.removeFile(buildFilePath(mcrid));
        //     });
        // }
    
        // public Boolean undoAction(MCRCategoryID mcrId, MCRCategory mcrCg, MCRContent xml, MCREvent eventData) {
        //     // TODO unfinished, make something to undo changes if something failed without dataloss
        //     switch (eventData.getEventType()) {
        //         case MCREvent.DELETE_EVENT:
        //             return false;
    
        //         default:
        //             fileDelete(mcrId, mcrCg, xml, eventData);
        //             return true;
        //     }
        // }
    
        // public MCRContent retrieveContent(MCRCategoryID mcrid) {
        //     return retrieveContent(mcrid, null);
        // }
    
        // public MCRContent retrieveContent(MCRCategoryID mcrid, String revision) {
        //     String objName = getName(mcrid);
        //     OcflRepository repo = getRepository();
        //     ObjectVersionId vId = revision != null ? ObjectVersionId.version(objName, revision)
        //         : ObjectVersionId.head(objName);
    
        //     try {
        //         repo.getObject(vId);
        //     } catch (NotFoundException e) {
        //         throw new MCRUsageException("Object '" + objName + "' could not be found", e);
        //     }
    
        //     if (convertMessageToType(repo.getObject(vId).getVersionInfo().getMessage()) == MCROCFLMetadataVersion.DELETED) {
        //         throw new MCRUsageException("Cannot read already deleted object '" + objName + "'");
        //     }
    
        //     try (InputStream storedContentStream = repo.getObject(vId).getFile(buildFilePath(mcrid)).getStream()) {
        //         Document xml = new MCRStreamContent(storedContentStream).asXML();
        //         if (revision != null) {
        //             xml.getRootElement().setAttribute("rev", revision);
        //         }
        //         return new MCRJDOMContent(xml);
        //     } catch (JDOMException | SAXException | IOException e) {
        //         throw new MCRPersistenceException("Can not parse XML from OCFL-Store", e);
        //     }
        // }
    
        protected String getName(MCRObjectID mcrid) {
            return DERIVATE_PREFIX + mcrid.toString();
        }
    
        protected String buildFilePath(MCRCategoryID mcrid) {
            return "files/" + mcrid.toString() + ".xml";
        }
    
        protected VersionInfo buildVersionInfo(String message, Date versionDate) {
            VersionInfo versionInfo = new VersionInfo();
            versionInfo.setMessage(message);
            versionInfo.setCreated((versionDate == null ? new Date() : versionDate).toInstant().atOffset(ZoneOffset.UTC));
            return versionInfo;
        }

}
