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

package org.mycore.frontend.cli;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.jdom2.transform.JDOMSource;
import org.mycore.backend.jpa.MCREntityManagerProvider;
import org.mycore.common.MCRConstants;
import org.mycore.common.MCRException;
import org.mycore.common.content.MCRSourceContent;
import org.mycore.common.content.MCRURLContent;
import org.mycore.common.xml.MCRURIResolver;
import org.mycore.common.xml.MCRXMLParserFactory;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryDAO;
import org.mycore.datamodel.classifications2.MCRCategoryDAOFactory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.MCRLabel;
import org.mycore.datamodel.classifications2.MCRUnmappedCategoryRemover;
import org.mycore.datamodel.classifications2.impl.MCRCategoryDAOImpl;
import org.mycore.datamodel.classifications2.impl.MCRCategoryImpl;
import org.mycore.datamodel.classifications2.utils.MCRCategoryTransformer;
import org.mycore.datamodel.classifications2.utils.MCRXMLTransformer;
import org.mycore.datamodel.common.MCRActiveLinkException;
import org.mycore.frontend.cli.annotation.MCRCommand;
import org.mycore.frontend.cli.annotation.MCRCommandGroup;
import org.xml.sax.SAXParseException;

import jakarta.persistence.EntityManager;

/**
 * Commands for the classification system.
 *
 * @author Thomas Scheffler (yagee)
 */
@MCRCommandGroup(name = "Classification Commands")
public class MCRClassification2Commands extends MCRAbstractCommands {
    private static Logger LOGGER = LogManager.getLogger();

    private static final MCRCategoryDAO DAO = MCRCategoryDAOFactory.getInstance();

    /** Default transformer script */
    public static final String DEFAULT_TRANSFORMER = "save-classification.xsl";

    /**
     * Deletes a classification
     *
     * @param classID classification ID
     * @see MCRCategoryDAO#deleteCategory(MCRCategoryID)
     */
    @MCRCommand(syntax = "delete classification {0}",
        help = "The command remove the classification with MCRObjectID {0} from the system.",
        order = 30)
    public static void delete(String classID) {
        DAO.deleteCategory(MCRCategoryID.rootID(classID));
    }

    /**
     * Counts the classification categories on top level
     *
     * @param classID classification ID
     */
    @MCRCommand(syntax = "count classification children of {0}",
        help = "The command count the categoies of the classification with MCRObjectID {0} in the system.",
        order = 80)
    public static void countChildren(String classID) {
        MCRCategory category = DAO.getCategory(MCRCategoryID.rootID(classID), 1);
        System.out.printf(Locale.ROOT, "%s has %d children", category.getId(), category.getChildren().size());
    }

    /**
     * Adds a classification.
     *
     * Classification is built from a file.
     *
     * @param filename
     *            file in mcrclass xml format
     * @see MCRCategoryDAO#addCategory(MCRCategoryID, MCRCategory)
     */
    @MCRCommand(syntax = "load classification from file {0}",
        help = "The command adds a new classification from file {0} to the system.",
        order = 10)
    public static List<String> loadFromFile(String filename) throws URISyntaxException, MCRException, SAXParseException,
        IOException {
        String fileURL = Paths.get(filename).toAbsolutePath().normalize().toUri().toURL().toString();
        return Collections.singletonList("load classification from url " + fileURL);
    }

    @MCRCommand(syntax = "load classification from url {0}",
        help = "The command adds a new classification from URL {0} to the system.",
        order = 15)
    public static void loadFromURL(String fileURL) throws SAXParseException, MalformedURLException, URISyntaxException {
        Document xml = MCRXMLParserFactory.getParser().parseXML(new MCRURLContent(new URL(fileURL)));
        MCRCategory category = MCRXMLTransformer.getCategory(xml);
        DAO.addCategory(null, category);
    }

    @MCRCommand(syntax = "load classification from uri {0}",
        help = "The command adds a new classification from URI {0} to the system.",
        order = 17)
    public static void loadFromURI(String fileURI) throws SAXParseException, URISyntaxException, TransformerException {
        Document xml = MCRXMLParserFactory.getParser().parseXML(MCRSourceContent.getInstance(fileURI));
        MCRCategory category = MCRXMLTransformer.getCategory(xml);
        DAO.addCategory(null, category);
    }

    /**
     * Replaces a classification with a new version
     *
     * @param filename
     *            file in mcrclass xml format
     * @see MCRCategoryDAO#replaceCategory(MCRCategory)
     */
    @MCRCommand(syntax = "update classification from file {0}",
        help = "The command updates a classification from file {0} to the system.",
        order = 20)
    public static List<String> updateFromFile(String filename)
        throws URISyntaxException, MCRException, SAXParseException,
        IOException {
        String fileURL = Paths.get(filename).toAbsolutePath().normalize().toUri().toURL().toString();
        return Collections.singletonList("update classification from url " + fileURL);
    }

    @MCRCommand(syntax = "update classification from url {0}",
        help = "The command updates a classification from URL {0} to the system.",
        order = 25)
    public static void updateFromURL(String fileURL)
        throws SAXParseException, MalformedURLException, URISyntaxException {
        Document xml = MCRXMLParserFactory.getParser().parseXML(new MCRURLContent(new URL(fileURL)));
        MCRCategory category = MCRXMLTransformer.getCategory(xml);
        if (DAO.exist(category.getId())) {
            DAO.replaceCategory(category);
        } else {
            // add if classification does not exist
            DAO.addCategory(null, category);
        }
    }

    @MCRCommand(syntax = "update classification from uri {0}",
        help = "The command updates a classification from URI {0} to the system.",
        order = 27)
    public static void updateFromURI(String fileURI) throws SAXParseException, URISyntaxException,
        TransformerException {
        Document xml = MCRXMLParserFactory.getParser().parseXML(MCRSourceContent.getInstance(fileURI));
        MCRCategory category = MCRXMLTransformer.getCategory(xml);
        if (DAO.exist(category.getId())) {
            DAO.replaceCategory(category);
        } else {
            // add if classification does not exist
            DAO.addCategory(null, category);
        }
    }

    /**
     * Loads MCRClassification from all XML files in a directory.
     *
     * @param directory
     *            the directory containing the XML files
     */
    @MCRCommand(syntax = "load all classifications from directory {0}",
        help = "The command add all classifications in the directory {0} to the system.",
        order = 40)
    public static List<String> loadFromDirectory(String directory) throws MCRActiveLinkException {
        return processFromDirectory(directory, false);
    }

    /**
     * Updates MCRClassification from all XML files in a directory.
     *
     * @param directory
     *            the directory containing the XML files
     */
    @MCRCommand(syntax = "update all classifications from directory {0}",
        help = "The command update all classifications in the directory {0} to the system.",
        order = 50)
    public static List<String> updateFromDirectory(String directory) throws MCRActiveLinkException {
        return processFromDirectory(directory, true);
    }

    /**
     * Loads or updates MCRClassification from all XML files in a directory.
     *
     * @param directory
     *            the directory containing the XML files
     * @param update
     *            if true, classification will be updated, else Classification
     *            is created
     * @throws MCRActiveLinkException
     */
    private static List<String> processFromDirectory(String directory, boolean update) throws MCRActiveLinkException {
        File dir = new File(directory);

        if (!dir.isDirectory()) {
            LOGGER.warn("{} ignored, is not a directory.", directory);
            return null;
        }

        String[] list = dir.list();

        if (list.length == 0) {
            LOGGER.warn("No files found in directory {}", directory);
            return null;
        }

        return Arrays.stream(list)
            .filter(file -> file.endsWith(".xml"))
            .map(file -> (update ? "update" : "load") + " classification from file " + new File(
                dir, file).getAbsolutePath())
            .collect(Collectors.toList());
    }

    /**
     * Save a MCRClassification.
     *
     * @param id
     *            the ID of the MCRClassification to be save.
     * @param dirname
     *            the directory to export the classification to
     * @param style
     *            the name part of the stylesheet like <em>style</em>
     *            -classification.xsl
     * @return false if an error was occured, else true
     */
    @MCRCommand(syntax = "export classification {0} to directory {1} with {2}",
        help = "The command exports the classification with MCRObjectID {0} as xml file to directory named {1} "
            + "using the stylesheet {2}-object.xsl. For {2} save is the default.",
        order = 60)
    public static boolean export(String id, String dirname, String style) throws Exception {
        String dname = "";
        if (dirname.length() != 0) {
            try {
                File dir = new File(dirname);
                if (!dir.isDirectory()) {
                    dir.mkdir();
                }
                if (!dir.isDirectory()) {
                    LOGGER.error("Can't find or create directory {}", dir.getAbsolutePath());
                    return false;
                } else {
                    dname = dirname;
                }
            } catch (Exception e) {
                LOGGER.error("Can't find or create directory {}", dirname, e);
                return false;
            }
        }
        MCRCategory cl = DAO.getCategory(MCRCategoryID.rootID(id), -1);
        Document classDoc = MCRCategoryTransformer.getMetaDataDocument(cl, false);

        Transformer trans = getTransformer(style);
        File xmlOutput = new File(dname, id + ".xml");
        FileOutputStream out = new FileOutputStream(xmlOutput);
        if (trans != null) {
            StreamResult sr = new StreamResult(out);
            trans.transform(new JDOMSource(classDoc), sr);
        } else {
            XMLOutputter xout = new XMLOutputter(Format.getPrettyFormat());
            xout.output(classDoc, out);
            out.flush();
        }
        LOGGER.info("Classifcation {} saved to {}.", id, xmlOutput.getCanonicalPath());
        return true;
    }

    /**
     * The method search for a stylesheet mcr_<em>style</em>_object.xsl and
     * build the transformer. Default is <em>mcr_save-object.xsl</em>.
     *
     * @param style
     *            the style attribute for the transformer stylesheet
     * @return the transformer
     * @throws TransformerFactoryConfigurationError
     * @throws TransformerConfigurationException
     */
    private static Transformer getTransformer(String style) throws TransformerFactoryConfigurationError,
        TransformerConfigurationException {
        String xslfile = DEFAULT_TRANSFORMER;
        if (style != null && style.trim().length() != 0) {
            xslfile = style + "-classification.xsl";
        }
        Transformer trans = null;

        URL styleURL = MCRClassification2Commands.class.getResource("/" + xslfile);
        if (styleURL == null) {
            styleURL = MCRClassification2Commands.class.getResource(DEFAULT_TRANSFORMER);
        }
        if (styleURL != null) {
            StreamSource source;
            try {
                source = new StreamSource(styleURL.toURI().toASCIIString());
            } catch (URISyntaxException e) {
                throw new TransformerConfigurationException(e);
            }
            TransformerFactory transfakt = TransformerFactory.newInstance();
            transfakt.setURIResolver(MCRURIResolver.instance());
            trans = transfakt.newTransformer(source);
        }
        return trans;
    }

    /**
     * Save all MCRClassifications.
     *
     * @param dirname
     *            the directory name to store all classifications
     * @param style
     *            the name part of the stylesheet like <em>style</em>
     *            -classification.xsl
     * @return false if an error was occured, else true
     */
    @MCRCommand(syntax = "export all classifications to directory {0} with {1}",
        help = "The command store all classifications to the directory with name {0} with the stylesheet "
            + "{1}-object.xsl. For {1} save is the default.",
        order = 70)
    public static boolean exportAll(String dirname, String style) throws Exception {
        List<MCRCategoryID> allClassIds = DAO.getRootCategoryIDs();
        boolean ret = false;
        for (MCRCategoryID id : allClassIds) {
            ret = ret & export(id.getRootID(), dirname, style);
        }
        return ret;
    }

    /**
     * List all IDs of all classifications stored in the database
     */
    @MCRCommand(syntax = "list all classifications",
        help = "The command list all classification stored in the database.",
        order = 100)
    public static void listAllClassifications() {
        List<MCRCategoryID> allClassIds = DAO.getRootCategoryIDs();
        for (MCRCategoryID id : allClassIds) {
            LOGGER.info(id.getRootID());
        }
        LOGGER.info("");
    }

    /**
     * List a MCRClassification.
     *
     * @param classid
     *            the MCRObjectID of the classification
     */
    @MCRCommand(syntax = "list classification {0}",
        help = "The command list the classification with MCRObjectID {0}.",
        order = 90)
    public static void listClassification(String classid) {
        MCRCategoryID clid = MCRCategoryID.rootID(classid);
        MCRCategory cl = DAO.getCategory(clid, -1);
        LOGGER.info(classid);
        if (cl != null) {
            listCategory(cl);
        } else {
            LOGGER.error("Can't find classification {}", classid);
        }
    }

    private static void listCategory(MCRCategory categ) {
        int level = categ.getLevel();
        StringBuilder sb = new StringBuilder(128);
        for (int i = 0; i < level * 2; i++) {
            sb.append(' ');
        }
        String space = sb.toString();
        if (categ.isCategory()) {
            LOGGER.info("{}  ID    : {}", space, categ.getId().getID());
        }
        for (MCRLabel label : categ.getLabels()) {
            LOGGER.info("{}  Label : ({}) {}", space, label.getLang(), label.getText());
        }
        List<MCRCategory> children = categ.getChildren();
        for (MCRCategory child : children) {
            listCategory(child);
        }
    }

    @MCRCommand(syntax = "repair category with empty labels",
        help = "fixes all categories with no labels (adds a label with categid as @text for default lang)",
        order = 110)
    public static void repairEmptyLabels() {
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        String deleteEmptyLabels = "delete from {h-schema}MCRCategoryLabels where text is null or trim(text) = ''";
        int affected = em.createNativeQuery(deleteEmptyLabels).executeUpdate();
        LOGGER.info("Deleted {} labels.", affected);
        String sqlQuery = "select cat.classid,cat.categid from {h-schema}MCRCategory cat "
            + "left outer join {h-schema}MCRCategoryLabels label on cat.internalid = label.category where "
            + "label.text is null";
        @SuppressWarnings("unchecked")
        List<Object[]> list = em.createNativeQuery(sqlQuery).getResultList();

        for (Object resultList : list) {
            Object[] arrayOfResults = (Object[]) resultList;
            String classIDString = (String) arrayOfResults[0];
            String categIDString = (String) arrayOfResults[1];

            MCRCategoryID mcrCategID = new MCRCategoryID(classIDString, categIDString);
            MCRLabel mcrCategLabel = new MCRLabel(MCRConstants.DEFAULT_LANG, categIDString, null);
            MCRCategoryDAOFactory.getInstance().setLabel(mcrCategID, mcrCategLabel);
            LOGGER.info("fixing category with class ID \"{}\" and category ID \"{}\"", classIDString, categIDString);
        }
        LOGGER.info("Fixing category labels completed!");
    }

    @MCRCommand(syntax = "repair position in parent",
        help = "fixes all categories gaps in position in parent",
        order = 120)
    @SuppressWarnings("unchecked")
    public static void repairPositionInParent() {
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        // this SQL-query find missing numbers in positioninparent
        String sqlQuery = "select parentid, min(cat1.positioninparent+1) from {h-schema}MCRCategory cat1 "
            + "where cat1.parentid is not null and not exists" + "(select 1 from {h-schema}MCRCategory cat2 "
            + "where cat2.parentid=cat1.parentid and cat2.positioninparent=(cat1.positioninparent+1))"
            + "and cat1.positioninparent not in "
            + "(select max(cat3.positioninparent) from {h-schema}MCRCategory cat3 "
            + "where cat3.parentid=cat1.parentid) group by cat1.parentid";

        for (List<Object[]> parentWithErrorsList = em.createNativeQuery(sqlQuery)
            .getResultList(); !parentWithErrorsList
                .isEmpty();
            parentWithErrorsList = em.createNativeQuery(sqlQuery).getResultList()) {
            for (Object[] parentWithErrors : parentWithErrorsList) {
                Number parentID = (Number) parentWithErrors[0];
                Number firstErrorPositionInParent = (Number) parentWithErrors[1];
                LOGGER.info("Category {} has the missing position {} ...", parentID, firstErrorPositionInParent);
                repairCategoryWithGapInPos(parentID, firstErrorPositionInParent);
                LOGGER.info("Fixed position {} for category {}.", firstErrorPositionInParent, parentID);
            }
        }

        sqlQuery = "select parentid, min(cat1.positioninparent-1) from {h-schema}MCRCategory cat1 "
            + "where cat1.parentid is not null and not exists" + "(select 1 from {h-schema}MCRCategory cat2 "
            + "where cat2.parentid=cat1.parentid and cat2.positioninparent=(cat1.positioninparent-1))"
            + "and cat1.positioninparent not in "
            + "(select max(cat3.positioninparent) from {h-schema}MCRCategory cat3 "
            + "where cat3.parentid=cat1.parentid) and cat1.positioninparent > 0 group by cat1.parentid";

        while (true) {
            List<Object[]> parentWithErrorsList = em.createNativeQuery(sqlQuery).getResultList();

            if (parentWithErrorsList.isEmpty()) {
                break;
            }

            for (Object[] parentWithErrors : parentWithErrorsList) {
                Number parentID = (Number) parentWithErrors[0];
                Number wrongStartPositionInParent = (Number) parentWithErrors[1];
                LOGGER.info("Category {} has the the starting position {} ...", parentID, wrongStartPositionInParent);
                repairCategoryWithWrongStartPos(parentID, wrongStartPositionInParent);
                LOGGER.info("Fixed position {} for category {}.", wrongStartPositionInParent, parentID);
            }
        }
        LOGGER.info("Repair position in parent finished!");
    }

    public static void repairCategoryWithWrongStartPos(Number parentID, Number wrongStartPositionInParent) {
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        String sqlQuery = "update {h-schema}MCRCategory set positioninparent= positioninparent -"
            + wrongStartPositionInParent
            + "-1 where parentid=" + parentID + " and positioninparent > " + wrongStartPositionInParent;

        em.createNativeQuery(sqlQuery).executeUpdate();
    }

    private static void repairCategoryWithGapInPos(Number parentID, Number firstErrorPositionInParent) {
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        // the query decrease the position in parent with a rate.
        // eg. posInParent: 0 1 2 5 6 7
        // at 3 the position get faulty, 5 is the min. of the position greather
        // 3
        // so the reate is 5-3 = 2
        String sqlQuery = "update {h-schema}MCRCategory "
            + "set positioninparent=(positioninparent - (select min(positioninparent) from "
            + "{h-schema}MCRCategory where parentid="
            + parentID
            + " and positioninparent > "
            + firstErrorPositionInParent
            + ")+"
            + firstErrorPositionInParent
            + ") where parentid=" + parentID + " and positioninparent > " + firstErrorPositionInParent;

        em.createNativeQuery(sqlQuery).executeUpdate();
    }

    @MCRCommand(syntax = "repair left right values for classification {0}",
        help = "fixes all left and right values in the given classification",
        order = 130)
    public static void repairLeftRightValue(String classID) {
        if (!(DAO instanceof MCRCategoryDAOImpl)) {
            LOGGER.error("Command not compatible with {}", DAO.getClass().getName());
            return;
        }
        ((MCRCategoryDAOImpl) DAO).repairLeftRightValue(classID);
    }

    @MCRCommand(syntax = "check all classifications",
        help = "checks if all redundant information are stored without conflicts",
        order = 140)
    public static List<String> checkAllClassifications() {
        List<MCRCategoryID> classifications = MCRCategoryDAOFactory.getInstance().getRootCategoryIDs();
        List<String> commands = new ArrayList<>(classifications.size());
        for (MCRCategoryID id : classifications) {
            commands.add("check classification " + id.getRootID());
        }
        return commands;
    }

    @MCRCommand(syntax = "check classification {0}",
        help = "checks if all redundant information are stored without conflicts",
        order = 150)
    public static void checkClassification(String id) {
        LOGGER.info("Checking classifcation {}", id);
        ArrayList<String> log = new ArrayList<>();
        LOGGER.info("{}: checking for missing parentID", id);
        checkMissingParent(id, log);
        LOGGER.info("{}: checking for empty labels", id);
        checkEmptyLabels(id, log);
        if (log.isEmpty()) {
            MCRCategoryImpl category = (MCRCategoryImpl) MCRCategoryDAOFactory.getInstance().getCategory(
                MCRCategoryID.rootID(id), -1);
            LOGGER.info("{}: checking left, right and level values and for non-null children", id);
            checkLeftRightAndLevel(category, 0, 0, log);
        }
        if (log.size() > 0) {
            LOGGER.error("Some errors occured on last test, report will follow");
            StringBuilder sb = new StringBuilder();
            for (String msg : log) {
                sb.append(msg).append('\n');
            }
            LOGGER.error("Error report for classification {}\n{}", id, sb);
        } else {
            LOGGER.info("Classifcation {} has no errors.", id);
        }
    }

    @MCRCommand(syntax = "remove unmapped categories from classification {0}",
        help = "Deletes all Categories of classification {0} which can not be mapped from all other classifications!",
        order = 160)
    public static void filterClassificationWithMapping(String id) {
        new MCRUnmappedCategoryRemover(id).filter();
    }

    private static int checkLeftRightAndLevel(MCRCategoryImpl category, int leftStart, int levelStart,
        List<String> log) {
        int curValue = leftStart;
        final int nextLevel = levelStart + 1;
        if (leftStart != category.getLeft()) {
            log.add("LEFT of " + category.getId() + " is " + category.getLeft() + " should be " + leftStart);
        }
        if (levelStart != category.getLevel()) {
            log.add("LEVEL of " + category.getId() + " is " + category.getLevel() + " should be " + levelStart);
        }
        int position = 0;
        for (MCRCategory child : category.getChildren()) {
            if (child == null) {
                log.add("NULL child of parent " + category.getId() + " on position " + position);
                continue;
            }
            LOGGER.debug(child.getId());
            curValue = checkLeftRightAndLevel((MCRCategoryImpl) child, ++curValue, nextLevel, log);
            position++;
        }
        ++curValue;
        if (curValue != category.getRight()) {
            log.add("RIGHT of " + category.getId() + " is " + category.getRight() + " should be " + curValue);
        }
        return curValue;
    }

    private static void checkEmptyLabels(String classID, List<String> log) {
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        String sqlQuery = "select cat.categid from {h-schema}MCRCategory cat "
            + "left outer join {h-schema}MCRCategoryLabels label on cat.internalid = label.category where "
            + "cat.classid='"
            + classID + "' and (label.text is null or trim(label.text) = '')";
        @SuppressWarnings("unchecked")
        List<String> list = em.createNativeQuery(sqlQuery).getResultList();

        for (String categIDString : list) {
            log.add("EMPTY lables for category " + new MCRCategoryID(classID, categIDString));
        }
    }

    private static void checkMissingParent(String classID, List<String> log) {
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        String sqlQuery = "select cat.categid from {h-schema}MCRCategory cat WHERE cat.classid='"
            + classID + "' and cat.level > 0 and cat.parentID is NULL";
        @SuppressWarnings("unchecked")
        List<String> list = em.createNativeQuery(sqlQuery).getResultList();

        for (String categIDString : list) {
            log.add("parentID is null for category " + new MCRCategoryID(classID, categIDString));
        }
    }

    @MCRCommand(syntax = "repair missing parent for classification {0}",
        help = "restores parentID from information in the given classification, if left right values are correct",
        order = 130)
    public static void repairMissingParent(String classID) {
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        String sqlQuery = "update {h-schema}MCRCategory cat set cat.parentID=(select parent.internalID from "
            + "{h-schema}MCRCategory parent WHERE parent.classid='"
            + classID
            + "' and parent.leftValue<cat.leftValue and parent.rightValue>cat.rightValue and "
            + "parent.level=(cat.level-1)) WHERE cat.classid='"
            + classID + "' and cat.level > 0 and cat.parentID is NULL";
        int updates = em.createNativeQuery(sqlQuery).executeUpdate();
        LOGGER.info(() -> "Repaired " + updates + " parentID columns for classification " + classID);
    }
}
