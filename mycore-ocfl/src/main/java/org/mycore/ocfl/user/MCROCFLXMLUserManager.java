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

package org.mycore.ocfl.user;

import java.io.IOException;
import java.io.InputStream;
import java.time.ZoneOffset;
import java.util.Date;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.MCRSystemUserInformation;
import org.mycore.common.MCRUsageException;
import org.mycore.common.content.MCRJAXBContent;
import org.mycore.ocfl.MCROCFLObjectIDPrefixHelper;
import org.mycore.ocfl.MCROCFLRepositoryProvider;
import org.mycore.user2.MCRTransientUser;
import org.mycore.user2.MCRUser;
import org.mycore.user2.MCRUserManager;
import org.mycore.user2.utils.MCRUserTransformer;

import edu.wisc.library.ocfl.api.OcflOption;
import edu.wisc.library.ocfl.api.OcflRepository;
import edu.wisc.library.ocfl.api.exception.OverwriteException;
import edu.wisc.library.ocfl.api.model.ObjectVersionId;
import edu.wisc.library.ocfl.api.model.VersionInfo;

/**
 * XML Manager to handle MCRUsers in a MyCoRe OCFL Repository
 * @author Tobias Lenhardt [Hammer1279]
 */
public class MCROCFLXMLUserManager {

    private static final Logger LOGGER = LogManager.getLogger();

    public static final String MESSAGE_CREATED = "Created";

    public static final String MESSAGE_UPDATED = "Updated";

    public static final String MESSAGE_DELETED = "Deleted";

    private final OcflRepository repository;

    public MCROCFLXMLUserManager(String repositoryKey) {
        this.repository = MCROCFLRepositoryProvider.getRepository(repositoryKey);
    }

    // @SuppressWarnings({ "java:S5411", "java:S2639" })
    public void updateUser(MCRUser user) {
        MCRUser currentUser = MCRUserManager.getCurrentUser();
        String ocflUserID = MCROCFLObjectIDPrefixHelper.USER + user.getUserID();

        if (MCRSystemUserInformation.getGuestInstance().getUserID().equals(currentUser.getUserID())) {
            LOGGER.debug("----------Login Detected----------");
            return;
        }

        if (user instanceof MCRTransientUser) {
            return;
        }

        if (!exists(ocflUserID)) {
            createUser(user);
        }

        VersionInfo info = new VersionInfo() // FIXME show current not modified user
            .setMessage(MESSAGE_UPDATED)
            .setCreated((new Date()).toInstant().atOffset(ZoneOffset.UTC))
            .setUser(currentUser.getUserName(), currentUser.getEMailAddress());
        // if (MCRConfiguration2.getBoolean("MCR.Users.Manager.OCFL.MaskPasswd").orElseThrow()) {
        //     // this is not invalid RegEx, each char is getting replaced with a star
        //     user.setPassword(user.getPassword().replaceAll(".", "*"));
        //     user.setSalt(user.getSalt().replaceAll(".", "*"));
        // }
        MCRJAXBContent<MCRUser> content = new MCRJAXBContent<>(MCRUserTransformer.JAXB_CONTEXT, user);
        try (InputStream userAsStream = content.getInputStream()) {
            repository.updateObject(ObjectVersionId.head(ocflUserID), info,
                updater -> {
                    updater.writeFile(userAsStream, user.getUserID() + ".xml", OcflOption.OVERWRITE);
                });
        } catch (IOException | OverwriteException e) {
            throw new MCRPersistenceException("Failed to update user '" + ocflUserID + "'", e);
        }
    }

    public void createUser(MCRUser user) {
        // TODO add already created check

        if (user instanceof MCRTransientUser) {
            return;
        }

        MCRUser currentUser = MCRUserManager.getCurrentUser();
        String ocflUserID = MCROCFLObjectIDPrefixHelper.USER + user.getUserID();

        if (exists(ocflUserID)) {
            throw new MCRUsageException("The User '" + user.getUserID() + "' already exists in OCFL Repository");
        }

        VersionInfo info = new VersionInfo() // FIXME show current not modified user
            .setMessage(MESSAGE_CREATED)
            .setCreated((new Date()).toInstant().atOffset(ZoneOffset.UTC))
            .setUser(currentUser.getUserName(), currentUser.getEMailAddress());
        MCRJAXBContent<MCRUser> content = new MCRJAXBContent<>(MCRUserTransformer.JAXB_CONTEXT, user);
        try (InputStream userAsStream = content.getInputStream()) {
            repository.updateObject(ObjectVersionId.head(ocflUserID), info,
                updater -> {
                    updater.writeFile(userAsStream, user.getUserID() + ".xml");
                });
        } catch (IOException | OverwriteException e) {
            throw new MCRPersistenceException("Failed to update user '" + ocflUserID + "'", e);
        }
    }

    public void deleteUser(MCRUser user) {
        // TODO add existing and not deleted check

        if (user instanceof MCRTransientUser) {
            return;
        }

        MCRUser currentUser = MCRUserManager.getCurrentUser();
        String ocflUserID = MCROCFLObjectIDPrefixHelper.USER + user.getUserID();
        VersionInfo info = new VersionInfo() // FIXME show current not modified user
            .setMessage(MESSAGE_DELETED)
            .setCreated((new Date()).toInstant().atOffset(ZoneOffset.UTC))
            .setUser(currentUser.getUserName(), currentUser.getEMailAddress());
        repository.updateObject(ObjectVersionId.head(ocflUserID), info,
            updater -> {
                updater.removeFile(user.getUserID() + ".xml");
            });
    }

    private boolean exists(String ocflUserID) {
        return repository.containsObject(ocflUserID)
            && !repository.describeVersion(ObjectVersionId.head(ocflUserID)).getVersionInfo().getMessage()
                .equals(MESSAGE_DELETED);
    }
}
