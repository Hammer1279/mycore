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

package org.mycore.ocfl.classification;

import java.util.Collection;

import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventManager;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.impl.MCRCategoryDAOImpl;
import org.mycore.datamodel.classifications2.impl.MCRCategoryImpl;

/**
 * Add Event Callers to the Category DAO Implementation
 * @author Tobias Lenhardt [Hammer1279]
 */
@Deprecated(forRemoval = false)
public class MCROCFLCategoryDAOImpl extends MCRCategoryDAOImpl {

    /**
     * {@inheritDoc}
     */
    @Override
    public MCRCategory addCategory(MCRCategoryID parentID, MCRCategory category, int position) {
        MCRCategory cg = super.addCategory(parentID, category, position);
        MCREvent evt = new MCREvent(MCREvent.CLASS_TYPE, MCREvent.CREATE_EVENT);
        evt.put("class", cg);
        MCREventManager.instance().handleEvent(evt);
        return cg;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteCategory(MCRCategoryID id) {
        MCREvent evt = new MCREvent(MCREvent.CLASS_TYPE, MCREvent.DELETE_EVENT);
        evt.put("class", super.getCategory(id, -1));
        MCREventManager.instance().handleEvent(evt, MCREventManager.BACKWARD);
        super.deleteCategory(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<MCRCategoryImpl> replaceCategory(MCRCategory newCategory) throws IllegalArgumentException {
        Collection<MCRCategoryImpl> col = super.replaceCategory(newCategory);
        MCREvent evt = new MCREvent(MCREvent.CLASS_TYPE, MCREvent.UPDATE_EVENT);
        evt.put("class", newCategory);
        MCREventManager.instance().handleEvent(evt);
        return col;
    }
}
