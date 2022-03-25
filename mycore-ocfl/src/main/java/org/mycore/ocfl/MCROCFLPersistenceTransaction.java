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

import java.util.ArrayList;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRPersistenceTransaction;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.events.MCREvent;
import org.mycore.datamodel.common.MCRXMLClassificationManager;

/**
 * @author Tobias Lenhardt [Hammer1279]
 */
public class MCROCFLPersistenceTransaction implements MCRPersistenceTransaction {

    private static final Logger LOGGER = LogManager.getLogger(MCROCFLPersistenceTransaction.class);

    protected MCRSession currentSession;
    
    protected Optional<MCRXMLClassificationManager> managerOpt = MCRConfiguration2
    .<MCRXMLClassificationManager>getSingleInstanceOf("MCR.Classification.Manager");

    // private boolean active = false;

    private boolean rollbackOnly = false;

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isReady() {
        // TODO Auto-generated method stub
        LOGGER.debug("TRANSACTION READY CHECK - {}", managerOpt.isPresent());
        return managerOpt.isPresent();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void begin() {
        // TODO Auto-generated method stub
        LOGGER.debug("TRANSACTION BEGIN");
        if(isActive()){throw new IllegalStateException("TRANSACTION BEGIN");}
        currentSession = MCRSessionMgr.getCurrentSession();
        currentSession.put("classQueue", new ArrayList<MCREvent>());
        // active=true;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void commit() {
        // TODO Auto-generated method stub
        // read from current session what was modified to then call classmanager.commit on it
        LOGGER.debug("TRANSACTION COMMIT");
        if(!isActive()||getRollbackOnly()){throw new IllegalStateException("TRANSACTION COMMIT");}
        try {
            managerOpt.get().commitSession(currentSession);
        } catch (Exception e) {
            rollbackOnly = true;
            throw e;
        }
        // active=false;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void rollback() {
        // TODO Auto-generated method stub
        // read from current session what was modified to then call classmanager.rollback on it
        LOGGER.debug("TRANSACTION ROLLBACK");
        if(!isActive()){throw new IllegalStateException("TRANSACTION ROLLBACK");}
        managerOpt.get().rollbackSession(currentSession);
        rollbackOnly = false;
        // active=false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean getRollbackOnly() {
        // TODO Auto-generated method stub
        LOGGER.debug("TRANSACTION ROLLBACK CHECK - {}", rollbackOnly);
        if(!isActive()){throw new IllegalStateException("TRANSACTION ROLLBACK CHECK");}
        return rollbackOnly;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isActive() {
        // TODO Auto-generated method stub
        boolean active = MCRSessionMgr.getCurrentSession().get("classQueue") != null;
        LOGGER.debug("TRANSACTION ACTIVE CHECK - {}", active);
        // return active;
        return active;
    }

}
