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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.common.events.MCREvent;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.impl.MCRCategoryDAOImpl;
import org.mycore.datamodel.classifications2.model.MCRClassEvent;
import org.mycore.datamodel.classifications2.utils.MCRCategoryTransformer;
import org.mycore.datamodel.common.MCRXMLClassificationManager;
import org.mycore.frontend.cli.annotation.MCRCommand;
import org.mycore.frontend.cli.annotation.MCRCommandGroup;
import org.mycore.ocfl.MCROCFLXMLClassificationManager;

@MCRCommandGroup(name = "OCFL Development Commands")
public class MCROCFLDevCommands {

    private static final Logger LOGGER = LogManager.getLogger(MCROCFLDevCommands.class);

    private static final Path EXPORT_DIR = Path.of(MCRConfiguration2.getStringOrThrow("MCR.savedir"), "class-export");

    private static MCRXMLClassificationManager manager = MCRConfiguration2
        .getSingleInstanceOf("MCR.Classification.Manager", MCRXMLClassificationManager.class)
        .orElse(new MCROCFLXMLClassificationManager());

    @MCRCommand(syntax = "load ver classification {0} rev {1}",
        help = "load ver classification {0} rev {1}",
        order = 2)
    public static void readVerClass(String mclass, String rev) throws IOException {
        MCRContent content = manager.retrieveContent(MCRCategoryID.fromString(mclass), rev);
        createDir(EXPORT_DIR);
        Files.write(Path.of(EXPORT_DIR.toString(), mclass + ".xml"), content.asByteArray(), StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING);
        LOGGER.info("Command Run!");

    }

    @MCRCommand(syntax = "load ver classification {0}",
        help = "load ver classification {0}",
        order = 4)
    public static void readVerClass(String mclass) throws IOException {
        MCRContent content = manager.retrieveContent(MCRCategoryID.fromString(mclass));
        createDir(EXPORT_DIR);
        Files.write(Path.of(EXPORT_DIR.toString(), mclass + ".xml"), content.asByteArray(), StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING);
        LOGGER.info("Command Run!");

    }

    private static void createDir(Path dir) throws IOException {
        if (Files.notExists(dir)) {
            Files.createDirectories(dir);
        }
    }

    @MCRCommand(syntax = "rebuild ocfl class store")
    public static void rebuildClassStore() {
        String repositoryKey = MCRConfiguration2.getStringOrThrow("MCR.Classification.Manager.Repository");
        String ocflRoot = MCRConfiguration2
            .getStringOrThrow("MCR.OCFL.Repository." + repositoryKey + ".RepositoryRoot");
        Path classDir = Path.of(ocflRoot, "mcrclass");
        try (Stream<Path> walker = Files.walk(classDir)) {
            walker.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        } catch (Exception e) {
            //TODO: handle exception
        }
        List<MCRCategoryID> list = new MCRCategoryDAOImpl().getRootCategoryIDs();
        list.forEach(cId -> {
            MCREvent evt = new MCREvent(MCREvent.CLASS_TYPE, MCREvent.CREATE_EVENT);
            MCRCategory category = new MCRCategoryDAOImpl().getCategory(cId, -1); // try this on the Event Handler Data Retrieve
            evt.put("class", category);
            manager.fileUpdate(category.getId(), category,
                new MCRJDOMContent(MCRCategoryTransformer.getMetaDataDocument(category, true)), evt);
        });
        list.forEach(category -> {
            MCREvent evt = new MCREvent(MCREvent.CLASS_TYPE, MCRClassEvent.COMMIT_EVENT);
            evt.put("class", category);
            manager.commitChanges(evt, evt.getEventType(), null);
        });
        LOGGER.info("Updated {} Objects in OCFL Store", list.size());
    }
}
