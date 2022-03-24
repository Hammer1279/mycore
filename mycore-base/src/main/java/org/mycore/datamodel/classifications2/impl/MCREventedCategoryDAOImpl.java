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

package org.mycore.datamodel.classifications2.impl;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;

import javax.persistence.EntityManager;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventManager;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.MCRLabel;

/**
 * Category DAO Implementation with Event Handlers
 * @author Tobias Lenhardt [Hammer1279]
 */
public class MCREventedCategoryDAOImpl extends MCRCategoryDAOImpl {

    private static long LAST_MODIFIED = System.currentTimeMillis();

    private static final Logger LOGGER = LogManager.getLogger();

    private static HashMap<String, Long> LAST_MODIFIED_MAP = new HashMap<>();

    private static MCREventManager manager = MCREventManager.instance();

    private static final String EVENT_OBJECT = MCREvent.CLASS_TYPE;

    @Override
    public MCRCategory addCategory(MCRCategoryID parentID, MCRCategory category) {
        int position = -1;
        if (category instanceof MCRCategoryImpl) {
            position = ((MCRCategoryImpl) category).getPositionInParent();
        }
        return addCategory(parentID, category, position);
    }

    @Override
    public MCRCategory addCategory(MCRCategoryID parentID, MCRCategory category, int position) {
        MCRCategory cg = super.addCategory(parentID, category, position);
        MCREvent evt = new MCREvent(EVENT_OBJECT, MCREvent.CREATE_EVENT);
        evt.put("class", category);
        manager.handleEvent(evt);
        callOnCommit(evt);
        return cg;
    }

    @Override
    public void deleteCategory(MCRCategoryID id) {
        MCREvent evt = new MCREvent(MCREvent.CLASS_TYPE, MCREvent.DELETE_EVENT);
        evt.put("class", super.getCategory(id, -1));
        super.deleteCategory(id);
        manager.handleEvent(evt, MCREventManager.BACKWARD);
        callOnCommit(evt);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mycore.datamodel.classifications2.MCRCategoryDAO#exist(org.mycore.datamodel.classifications2.MCRCategoryID)
     */
    @Override
    public boolean exist(MCRCategoryID id) {
        return super.exist(id);
    }

    @Override
    public void moveCategory(MCRCategoryID id, MCRCategoryID newParentID, int index) {
        super.moveCategory(id, newParentID, index);
        MCREvent evt = new MCREvent(EVENT_OBJECT, MCREvent.UPDATE_EVENT);
        evt.put("class", super.getCategory(id, -1));
        // Type is used for specifying a special Update operation
        // originally named UType (Update Type), it is an Optional Value
        evt.put("type", "move");
        manager.handleEvent(evt);
        callOnCommit(evt);
    }

    @Override
    public MCRCategory removeLabel(MCRCategoryID id, String lang) {
        MCRCategory category = super.removeLabel(id, lang);
        MCREvent evt = new MCREvent(EVENT_OBJECT, MCREvent.UPDATE_EVENT);
        evt.put("class", super.getCategory(id, -1));
        manager.handleEvent(evt);
        callOnCommit(evt);
        return category;
    }

    @Override
    public Collection<MCRCategoryImpl> replaceCategory(MCRCategory newCategory) throws IllegalArgumentException {
        Collection<MCRCategoryImpl> colList = super.replaceCategory(newCategory);
        MCREvent evt = new MCREvent(EVENT_OBJECT, MCREvent.UPDATE_EVENT);
        evt.put("class", newCategory);
        manager.handleEvent(evt);
        callOnCommit(evt);
        return colList;
    }

    @Override
    public MCRCategory setLabel(MCRCategoryID id, MCRLabel label) {
        MCRCategory category = super.setLabel(id, label);
        MCREvent evt = new MCREvent(EVENT_OBJECT, MCREvent.UPDATE_EVENT);
        evt.put("class", super.getCategory(id, -1));
        manager.handleEvent(evt);
        callOnCommit(evt);
        return category;
    }

    @Override
    public MCRCategory setLabels(MCRCategoryID id, Set<MCRLabel> labels) {
        MCRCategory category = super.setLabels(id, labels);
        MCREvent evt = new MCREvent(EVENT_OBJECT, MCREvent.UPDATE_EVENT);
        evt.put("class", super.getCategory(id, -1));
        manager.handleEvent(evt);
        callOnCommit(evt);
        return category;
    }

    @Override
    public MCRCategory setURI(MCRCategoryID id, URI uri) {
        MCRCategory category = super.setURI(id, uri);
        MCREvent evt = new MCREvent(EVENT_OBJECT, MCREvent.UPDATE_EVENT);
        evt.put("class", super.getCategory(id, -1));
        manager.handleEvent(evt);
        callOnCommit(evt);
        return category;
    }

    // @Override
    // public void repairLeftRightValue(String classID) {
    //     final MCRCategoryID rootID = MCRCategoryID.rootID(classID);
    //     super.repairLeftRightValue(classID);
    //     MCREvent evt = new MCREvent(EVENT_OBJECT, MCREvent.UPDATE_EVENT);
    //     evt.put("class", super.getCategory(rootID, -1));
    //     manager.handleEvent(evt);
    //     callOnCommit(evt);
    // }

    @Override
    public long getLastModified() {
        return LAST_MODIFIED;
    }

    /**
     * returns database backed MCRCategoryImpl
     * 
     * every change to the returned MCRCategory is reflected in the database.
     */
    public static MCRCategoryImpl getByNaturalID(EntityManager entityManager, MCRCategoryID id) {
        return MCRCategoryDAOImpl.getByNaturalID(entityManager, id);
    }

    /**
     * Method updates the last modified timestamp, for the given root id.
     * 
     */
    protected synchronized void updateLastModified(String root) {
        LAST_MODIFIED_MAP.put(root, System.currentTimeMillis());
    }

    /**
     * Gets the timestamp for the given root id. If there is not timestamp at the moment -1 is returned.
     * 
     * @return the last modified timestamp (if any) or -1
     */
    @Override
    public long getLastModified(String root) {
        Long long1 = LAST_MODIFIED_MAP.get(root);
        if (long1 != null) {
            return long1;
        }
        return -1;
    }

    protected boolean enQueue = false;

    @SuppressWarnings("unchecked")
    protected void callOnCommit(MCREvent evt) {
        // if (!enQueue) {
        //     // enqueue zur session hinzufügen
        //     // events als liste zur session hinzufügen, und dann abarbeiten bei commit
        //     // Session.put() um elemente hinzuzufügen
        //     MCRSession currentSession = MCRSessionMgr.getCurrentSession();
        //     currentSession.onCommit(() -> {
        //         MCRClassEvent evnt = new MCRClassEvent(EVENT_OBJECT, MCRClassEvent.COMMIT_EVENT);
        //         evnt.put("class", evt.get("class"));
        //         evnt.put("event", evt);
        //         manager.handleEvent(evnt);
        //         enQueue = false;
        //     });

        //     enQueue = true;
        // } else {
        //     LOGGER.debug("CallOnCommit already queued, skipping.");
        // }

        String classQueue = "classQueue";

        MCRSession currentSession = MCRSessionMgr.getCurrentSession();
        // if (currentSession.get(classQueue) == null) {
        //     currentSession.put(classQueue, new ArrayList<MCREvent>().add(evt));
        // } else {
        //     currentSession.put(classQueue, ((ArrayList<MCREvent>) currentSession.get(classQueue)).add(evt));
        // }
        ((ArrayList<MCREvent>)currentSession.get(classQueue)).add(evt);
    }
}
