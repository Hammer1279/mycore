/**
 * 
 */
package org.mycore.solr.commands;

import java.util.ArrayList;
import java.util.List;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.impl.ConcurrentUpdateSolrServer;
import org.mycore.common.MCRObjectUtils;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.frontend.cli.MCRAbstractCommands;
import org.mycore.frontend.cli.MCRObjectCommands;
import org.mycore.frontend.cli.annotation.MCRCommand;
import org.mycore.frontend.cli.annotation.MCRCommandGroup;
import org.mycore.solr.MCRSolrServerFactory;
import org.mycore.solr.index.MCRSolrIndexer;

/**
 * Class provides useful solr related commands.
 * 
 * @author shermann
 *
 */
@MCRCommandGroup(name = "MCR SOLR Commands")
public class MCRSolrCommands extends MCRAbstractCommands {

    @MCRCommand(syntax = "rebuild solr metadata and content index", help = "rebuilds solr's metadata and content index", order = 10)
    public static void rebuildMetadataAndContentIndex() throws Exception {
        MCRSolrIndexer.rebuildMetadataAndContentIndex();
    }

    @MCRCommand(syntax = "rebuild solr metadata index", help = "rebuilds solr's metadata index", order = 20)
    public static void rebuildMetadataIndex() {
        MCRSolrIndexer.rebuildMetadataIndex();
    }

    @MCRCommand(syntax = "rebuild solr content index", help = "rebuilds solr's content index", order = 30)
    public static void rebuildContentIndex() {
        MCRSolrIndexer.rebuildContentIndex();
    }

    @MCRCommand(syntax = "restricted rebuild solr metadata index for objecttype {0}", help = "rebuilds solr's metadata index for the given type in {0}", order = 40)
    public static void rebuildMetadataIndex(String type) {
        MCRSolrIndexer.rebuildMetadataIndex(type);
    }

    @MCRCommand(syntax = "optimize solr index", help = "An optimize is like a hard commit except that it forces all of the index segments to be merged into a single segment first. "
            + "Depending on the use cases, this operation should be performed infrequently (like nightly), "
            + "if at all, since it is very expensive and involves reading and re-writing the entire index", order = 80)
    public static void optimize() {
        MCRSolrIndexer.optimize();
    }

    @MCRCommand(syntax = "drop solr index", help = "Deletes an existing index from solr", order = 90)
    public static void dropIndex() throws Exception {
        MCRSolrIndexer.dropIndex();
    }

    @MCRCommand(syntax = "delete index part for type {0}", help = "Deletes an existing index from solr but only for the given object type.", order = 100)
    public static void dropIndexByType(String type) throws Exception {
        MCRSolrIndexer.dropIndexByType(type);
    }

    @MCRCommand(syntax = "delete from solr index by id {0}", help = "Deletes an document from the index by id", order = 110)
    public static void deleteByIdFromSolr(String solrID) {
        MCRSolrIndexer.deleteByIdFromSolr(solrID);
    }

    @MCRCommand(syntax = "set solr server {0}", help = "Sets a new SOLR server, {0} specifies the URL of the SOLR Server", order = 130)
    public static void setSolrServer(String solrServerURL) {
        MCRSolrServerFactory.setSolrServer(solrServerURL);
    }

    @MCRCommand(syntax = "restricted rebuild solr metadata index for selected", help = "rebuilds solr's metadata index for selected objects", order = 50)
    public static void rebuildMetadataIndexForSelected() {
        List<String> selectedObjects = MCRObjectCommands.getSelectedObjectIDs();
        MCRSolrIndexer.rebuildMetadataIndex(selectedObjects);
    }

    @MCRCommand(syntax = "restricted rebuild solr content index for selected", help = "rebuilds solr's content index for selected objects and or derivates", order = 60)
    public static void rebuildContentIndexForSelected() {
        List<String> selectedObjects = MCRObjectCommands.getSelectedObjectIDs();
        MCRSolrIndexer.rebuildContentIndex(selectedObjects);
    }

    @MCRCommand(syntax = "restricted rebuild solr metadata index for object {0}", help = "rebuilds solr's metadata index for object and all its children", order = 70)
    public static void rebuildMetadataIndexForObject(String id) {
        MCRObject mcrObject = MCRMetadataManager.retrieveMCRObject(MCRObjectID.getInstance(id));
        List<MCRObject> objectList = MCRObjectUtils.getDescendantsAndSelf(mcrObject);
        List<String> idList = new ArrayList<>();
        for (MCRObject obj : objectList) {
            idList.add(obj.getId().toString());
        }
        MCRSolrIndexer.rebuildMetadataIndex(idList);
    }

    @MCRCommand(syntax = "create solr metadata and content index at {0}", help = "create solr's metadata and content index on specific solr server", order = 120)
    public static void createIndex(String url) throws Exception {
        SolrServer cuss = MCRSolrServerFactory.createConcurrentUpdateSolrServer(url);
        SolrServer hss = MCRSolrServerFactory.createSolrServer(url);
        MCRSolrIndexer.rebuildMetadataIndex(cuss);
        MCRSolrIndexer.rebuildContentIndex(hss);
        if (cuss instanceof ConcurrentUpdateSolrServer) {
            ((ConcurrentUpdateSolrServer) cuss).blockUntilFinished();
        }
        hss.optimize();
    }

    @MCRCommand(syntax = "synchronize metadata index", help = "synchronizes the database and solr server", order = 150)
    public static void synchronizeMetadataIndex() throws Exception {
        MCRSolrIndexer.synchronizeMetadataIndex();
    }

    @MCRCommand(syntax = "synchronize metadata index for objecttype {0}", help = "synchronizes the database and solr server", order = 160)
    public static void synchronizeMetadataIndex(String objectType) throws Exception {
        MCRSolrIndexer.synchronizeMetadataIndex(objectType);
    }

}
