package org.mycore.ocfl;

import javax.persistence.EntityTransaction;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MCROCFLEntityTransaction implements EntityTransaction {

    private static final Logger LOGGER = LogManager.getLogger(MCROCFLEntityTransaction.class);

    @Override
    public void begin() {
        // TODO Auto-generated method stub
        LOGGER.debug("MCR ENTITY TRANSACTION - BEGIN TRANSACTION");
    }

    @Override
    public void commit() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void rollback() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setRollbackOnly() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public boolean getRollbackOnly() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isActive() {
        // TODO Auto-generated method stub
        return false;
    }
    
}
