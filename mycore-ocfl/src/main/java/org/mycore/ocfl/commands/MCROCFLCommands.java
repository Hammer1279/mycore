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

package org.mycore.ocfl.commands;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRUsageException;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.content.MCRContent;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.impl.MCRCategoryDAOImpl;
import org.mycore.datamodel.common.MCRAbstractMetadataVersion;
import org.mycore.datamodel.common.MCRXMLMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.frontend.cli.annotation.MCRCommand;
import org.mycore.frontend.cli.annotation.MCRCommandGroup;
import org.mycore.ocfl.MCROCFLPersistenceTransaction;
import org.mycore.ocfl.classification.MCROCFLXMLClassificationManager;
import org.mycore.ocfl.metadata.MCROCFLMigration;
import org.mycore.ocfl.metadata.MCROCFLXMLMetadataManager;
import org.mycore.ocfl.repository.MCROCFLRepositoryProvider;
import org.mycore.ocfl.user.MCROCFLXMLUserManager;
import org.mycore.ocfl.util.MCROCFLObjectIDPrefixHelper;
import org.mycore.user2.MCRUser;
import org.mycore.user2.MCRUserManager;

import edu.wisc.library.ocfl.api.OcflRepository;

@MCRCommandGroup(name = "OCFL Commands")
public class MCROCFLCommands {

    private static final Logger LOGGER = LogManager.getLogger();

    public static final String SUCCESS = "success";

    public static final String SUCCESS_BUT_WITHOUT_HISTORY = SUCCESS + " but without history";

    public static final String FAILED = "failed";

    public static final String FAILED_AND_NOW_INVALID_STATE = FAILED + " and now invalid state";

    private static boolean confirmPurgeMarked = false;

    @MCRCommand(syntax = "migrate metadata to repository {0}",
        help = "migrates all the metadata to the ocfl " +
            "repository with the id {0}")
    public static void migrateToOCFL(String repository) {
        MCROCFLMigration migration = new MCROCFLMigration(repository);

        migration.start();

        ArrayList<String> success = migration.getSuccess();
        ArrayList<String> failed = migration.getFailed();
        ArrayList<String> invalidState = migration.getInvalidState();
        ArrayList<String> withoutHistory = migration.getWithoutHistory();

        LOGGER.info("The migration resulted in \n" +
            SUCCESS + ": {}, \n" +
            FAILED + ": {} \n" +
            FAILED_AND_NOW_INVALID_STATE + ": {} \n" +
            SUCCESS_BUT_WITHOUT_HISTORY + ": {} \n",
            String.join(", ", success),
            String.join(", ", failed),
            String.join(", ", invalidState),
            String.join(", ", withoutHistory));

        LOGGER.info("The migration resulted in \n" +
            SUCCESS + ": {}, \n" +
            FAILED + ": {} \n" +
            FAILED_AND_NOW_INVALID_STATE + ": {} \n" +
            SUCCESS_BUT_WITHOUT_HISTORY + ": {} \n",
            success.size(),
            failed.size(),
            invalidState.size(),
            withoutHistory.size());
    }

    @MCRCommand(syntax = "update ocfl classifications",
        help = "Update all classifications in the OCFL store from database")
    public static List<String> updateOCFLClassifications() {
        List<MCRCategoryID> list = new MCRCategoryDAOImpl().getRootCategoryIDs();
        return list.stream()
            .map(id -> "update ocfl classification " + id)
            .collect(Collectors.toList());
    }

    @MCRCommand(syntax = "update ocfl classification {0}",
        help = "Update classification {0} in the OCFL Store from database")
    public static void updateOCFLClassification(String classId) {
        final MCRCategoryID rootID = MCRCategoryID.rootID(classId);
        MCROCFLPersistenceTransaction.addClassficationEvent(rootID, MCRAbstractMetadataVersion.UPDATED);
    }

    @MCRCommand(syntax = "delete ocfl classification {0}",
        help = "Delete classification {0} in the OCFL Store")
    public static void deleteOCFLClassification(String classId) {
        final MCRCategoryID rootID = MCRCategoryID.rootID(classId);
        MCROCFLPersistenceTransaction.addClassficationEvent(rootID, MCRAbstractMetadataVersion.DELETED);
    }

    @MCRCommand(syntax = "sync ocfl classifications",
        help = "Update all classifications and remove deleted Classifications to resync OCFL Store to the Database")
    public static List<String> syncClassificationRepository() {
        List<String> commands = new ArrayList<>();
        commands.add("update ocfl classifications");
        List<String> outOfSync = getStaleOCFLClassificationIDs();
        commands.addAll(
            outOfSync.stream()
                .map(id -> "delete ocfl classification " + id).collect(Collectors.toList()));
        return commands;
    }

    @MCRCommand(syntax = "update ocfl users",
        help = "Update all users in the OCFL store from database")
    public static List<String> updateOCFLUsers() {
        List<MCRUser> list = MCRUserManager.listUsers("*", null, null, null);

        return list.stream()
            .map(usr -> "update ocfl user " + usr.getUserID())
            .collect(Collectors.toList());
    }

    @MCRCommand(syntax = "update ocfl user {0}",
        help = "Update user {0} in the OCFL Store from database")
    public static void updateOCFLUser(String userId) {
        if (MCRUserManager.getUser(userId) == null) {
            throw new MCRUsageException("The User '" + userId + "' does not exist!");
        }
        new MCROCFLXMLUserManager().updateUser(MCRUserManager.getUser(userId));
    }

    @MCRCommand(syntax = "delete ocfl user {0}",
        help = "Delete user {0} in the OCFL Store")
    public static void deleteOCFLUser(String userId) {
        new MCROCFLXMLUserManager().deleteUser(userId);
    }

    @MCRCommand(syntax = "sync ocfl users",
        help = "Update all users and remove deleted users to resync OCFL Store to the Database")
    public static List<String> syncUserRepository() {
        List<String> commands = new ArrayList<>();
        commands.add("update ocfl users");
        List<String> outOfSync = getStaleOCFLUserIDs();
        commands.addAll(
            outOfSync.stream()
                .map(id -> "delete ocfl user " + id).collect(Collectors.toList()));
        return commands;
    }

    @MCRCommand(syntax = "restore user {0} from ocfl with version {1}",
        help = "restore a specified revision of a ocfl user backup to the primary user store")
    public static void writeUserToDbVersioned(String userId, String revision) throws IOException {
        MCRUser user = new MCROCFLXMLUserManager().retrieveContent(userId, revision);
        MCRUserManager.updateUser(user);
    }

    @MCRCommand(syntax = "restore user {0} from ocfl",
        help = "restore the latest revision of a ocfl user backup to the primary user store")
    public static void writeUserToDb(String userId) throws IOException {
        MCRUser user = new MCROCFLXMLUserManager().retrieveContent(userId, null);
        MCRUserManager.updateUser(user);
    }

    @MCRCommand(syntax = "restore object {0} rev {1} from ocfl history",
        help = "restore mcrobject {0} with version {1} to current store from ocfl history")
    public static void restoreObjFromOCFL(String mcridString, String revision) throws IOException {
        MCRObjectID mcrid = MCRObjectID.getInstance(mcridString);
        MCROCFLXMLMetadataManager manager = new MCROCFLXMLMetadataManager();
        MCRContent content = manager.retrieveContent(mcrid, revision);
        try {
            MCRXMLMetadataManager.instance().update(mcrid, content, new Date(content.lastModified()));
        } catch (MCRUsageException e) {
            MCRXMLMetadataManager.instance().create(mcrid, content, new Date(content.lastModified()));
        }
    }

    @MCRCommand(syntax = "purge object {0} from ocfl",
        help = "Permanently delete object {0} and its history from ocfl")
    public static void purgeObject(String mcridString) throws IOException {
        MCRObjectID mcrid = MCRObjectID.getInstance(mcridString);
        new MCROCFLXMLMetadataManager().purge(mcrid, new Date(), MCRUserManager.getCurrentUser().getUserName(), true);
    }

    @MCRCommand(syntax = "purge class {0} from ocfl",
        help = "Permanently delete class {0} and its history from ocfl")
    public static void purgeClass(String mcrCgIdString) throws IOException {
        MCRCategoryID mcrCgId = MCRCategoryID.fromString(mcrCgIdString);
        if (!mcrCgId.isRootID()) {
            throw new MCRUsageException("You can only purge root classifications!");
        }
        new MCROCFLXMLClassificationManager().purge(mcrCgId);

    }

    @MCRCommand(syntax = "purge user {0} from ocfl",
        help = "Permanently delete user {0} and its history from ocfl")
    public static void purgeUser(String userId) throws IOException {
        new MCROCFLXMLUserManager().purgeUser(userId);
    }

    @MCRCommand(syntax = "purge all marked from ocfl",
        help = "Permanently delete all hidden/archived ocfl entries")
    public static void purgeMarked() throws IOException {
        if (!confirmPurgeMarked) {
            LOGGER.info("\n"
                + "\u001B[93m" + "Enter the command again to confirm \u001B[4mPERMANENTLY\u001B[24m deleting ALL"
                + " hidden/archived OCFL entries." + "\u001B[0m" + "\n"
                + "\u001B[41m" + "THIS ACTION CANNOT BE UNDONE!" + "\u001B[0m");
            confirmPurgeMarked = true;
            return;
        }
        purgeMarkedObjects();
        confirmPurgeMarked = true;
        purgeMarkedClasses();
        confirmPurgeMarked = true;
        purgeMarkedUsers();
        confirmPurgeMarked = false;
    }

    @MCRCommand(syntax = "purge marked metadata from ocfl",
        help = "Permanently delete all hidden/archived ocfl objects")
    public static void purgeMarkedObjects() throws IOException {
        if (!confirmPurgeMarked) {
            LOGGER.info("\n"
                + "\u001B[93m" + "Enter the command again to confirm \u001B[4mPERMANENTLY\u001B[24m deleting ALL"
                + " hidden/archived OCFL objects." + "\u001B[0m" + "\n"
                + "\u001B[41m" + "THIS ACTION CANNOT BE UNDONE!" + "\u001B[0m");
            confirmPurgeMarked = true;
            return;
        }
        MCROCFLXMLMetadataManager mcrMM = new MCROCFLXMLMetadataManager();
        mcrMM.setRepositoryKey(MCRConfiguration2.getStringOrThrow("MCR.Metadata.Manager.Repository"));
        mcrMM.listIDs().stream()
            .map(s -> s.replaceFirst("^mcr.+:",""))
            .map(o -> MCRObjectID.getInstance(o))
            .filter(m -> mcrMM.listRevisions(m).size() > 1)
            .filter(m -> {
                try {
                    mcrMM.retrieveContent(m);
                } catch (Exception e) {
                    return true;
                }
                return false;
            })
            .forEach(m -> mcrMM.purge(m, new Date(), MCRUserManager.getCurrentUser().getUserName(), true));
        confirmPurgeMarked = false;
    }

    @MCRCommand(syntax = "purge marked classes from ocfl",
        help = "Permanently delete all hidden/archived ocfl classes")
    public static void purgeMarkedClasses() throws IOException {
        if (!confirmPurgeMarked) {
            LOGGER.info("\n"
                + "\u001B[93m" + "Enter the command again to confirm \u001B[4mPERMANENTLY\u001B[24m deleting ALL"
                + " hidden/archived OCFL classes." + "\u001B[0m" + "\n"
                + "\u001B[41m" + "THIS ACTION CANNOT BE UNDONE!" + "\u001B[0m");
            confirmPurgeMarked = true;
            return;
        }

        String repositoryKey = MCRConfiguration2.getStringOrThrow("MCR.Classification.Manager.Repository");
        OcflRepository repository = MCROCFLRepositoryProvider.getRepository(repositoryKey);
        repository.listObjectIds()
            .filter(obj -> obj.startsWith(MCROCFLObjectIDPrefixHelper.CLASSIFICATION))
            .filter(obj -> MCROCFLXMLUserManager.MESSAGE_DELETED.equals(repository.describeObject(obj)
                .getHeadVersion().getVersionInfo().getMessage()))
            .map(obj -> obj.replace(MCROCFLObjectIDPrefixHelper.USER, ""))

            .forEach(u -> new MCROCFLXMLUserManager().purgeUser(u));
        confirmPurgeMarked = false;
    }

    @MCRCommand(syntax = "purge marked users from ocfl",
        help = "Permanently delete all hidden/archived ocfl users")
    public static void purgeMarkedUsers() throws IOException {
        if (!confirmPurgeMarked) {
            LOGGER.info("\n"
                + "\u001B[93m" + "Enter the command again to confirm \u001B[4mPERMANENTLY\u001B[24m deleting ALL"
                + " hidden/archived OCFL users." + "\u001B[0m" + "\n"
                + "\u001B[41m" + "THIS ACTION CANNOT BE UNDONE!" + "\u001B[0m");
            confirmPurgeMarked = true;
            return;
        }

        // MCRUserManager.listUsers("*", null, null, null).stream()
        String repositoryKey = MCRConfiguration2.getStringOrThrow("MCR.Users.Manager.Repository");
        OcflRepository repository = MCROCFLRepositoryProvider.getRepository(repositoryKey);
        repository.listObjectIds()
            .filter(obj -> obj.startsWith(MCROCFLObjectIDPrefixHelper.USER))
            .filter(obj -> MCROCFLXMLUserManager.MESSAGE_DELETED.equals(repository.describeObject(obj)
                .getHeadVersion().getVersionInfo().getMessage()))
            .map(obj -> obj.replace(MCROCFLObjectIDPrefixHelper.USER, ""))

            .forEach(u -> new MCROCFLXMLUserManager().purgeUser(u));
        confirmPurgeMarked = false;
    }

    private static List<String> getStaleOCFLClassificationIDs() {
        String repositoryKey = MCRConfiguration2.getStringOrThrow("MCR.Classification.Manager.Repository");
        List<String> classDAOList = new MCRCategoryDAOImpl().getRootCategoryIDs().stream()
            .map(MCRCategoryID::toString)
            .collect(Collectors.toList());
        OcflRepository repository = MCROCFLRepositoryProvider.getRepository(repositoryKey);
        return repository.listObjectIds()
            .filter(obj -> obj.startsWith(MCROCFLObjectIDPrefixHelper.CLASSIFICATION))
            .filter(obj -> !MCROCFLXMLClassificationManager.MESSAGE_DELETED.equals(repository.describeObject(obj)
                .getHeadVersion().getVersionInfo().getMessage()))
            .map(obj -> obj.replace(MCROCFLObjectIDPrefixHelper.CLASSIFICATION, ""))
            .filter(Predicate.not(classDAOList::contains))
            .collect(Collectors.toList());
    }

    private static List<String> getStaleOCFLUserIDs() {
        String repositoryKey = MCRConfiguration2.getStringOrThrow("MCR.Users.Manager.Repository");
        List<String> userEMList = MCRUserManager.listUsers("*", null, null, null).stream()
            .map(MCRUser::getUserID)
            .collect(Collectors.toList());
        OcflRepository repository = MCROCFLRepositoryProvider.getRepository(repositoryKey);
        return repository.listObjectIds()
            .filter(obj -> obj.startsWith(MCROCFLObjectIDPrefixHelper.USER))
            .filter(obj -> !MCROCFLXMLUserManager.MESSAGE_DELETED.equals(repository.describeObject(obj)
                .getHeadVersion().getVersionInfo().getMessage()))
            .map(obj -> obj.replace(MCROCFLObjectIDPrefixHelper.USER, ""))
            .filter(Predicate.not(userEMList::contains))
            .collect(Collectors.toList());
    }
}
