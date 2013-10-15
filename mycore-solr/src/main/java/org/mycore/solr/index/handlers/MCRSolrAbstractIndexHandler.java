package org.mycore.solr.index.handlers;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.mycore.solr.MCRSolrServerFactory;
import org.mycore.solr.index.MCRSolrIndexHandler;

public abstract class MCRSolrAbstractIndexHandler implements MCRSolrIndexHandler {

    protected SolrServer solrServer;

    protected int commitWithin;

    public MCRSolrAbstractIndexHandler() {
        this(MCRSolrServerFactory.getSolrServer());
    }

    public MCRSolrAbstractIndexHandler(SolrServer solrServer) {
        this.solrServer = solrServer;
        this.commitWithin = -1;
    }

    public SolrServer getSolrServer() {
        return this.solrServer;
    }

    public abstract void index() throws IOException, SolrServerException;

    @Override
    public List<MCRSolrIndexHandler> getSubHandlers() {
        return Collections.emptyList();
    }

    /**
     * Time in milliseconds solr should index the stream. -1 by default,
     * says that solr decide when to commit.
     */
    public void setCommitWithin(int commitWithin) {
        this.commitWithin = commitWithin;
    }

    public int getCommitWithin() {
        return commitWithin;
    }

    @Override
    public void setSolrServer(SolrServer solrServer) {
        this.solrServer = solrServer;
    }

    @Override
    public int getDocuments() {
        return 1;
    }

    protected UpdateRequest getUpdateRequest(String path) {
        UpdateRequest req = path != null ? new UpdateRequest(path) : new UpdateRequest();
        req.setCommitWithin(getCommitWithin());
        return req;
    }

}
