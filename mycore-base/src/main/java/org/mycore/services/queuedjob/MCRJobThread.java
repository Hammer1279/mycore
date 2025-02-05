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

package org.mycore.services.queuedjob;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.backend.jpa.MCREntityManagerProvider;
import org.mycore.common.MCRClassTools;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRSystemUserInformation;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.processing.MCRAbstractProcessable;
import org.mycore.common.processing.MCRProcessableStatus;

/**
 * A slave thread of {@link MCRJobMaster}.
 *
 * This class execute the specified action for {@link MCRJob} and performs {@link MCRJobAction#rollback()}
 * if an error occurs. 
 *
 * @author Ren\u00E9 Adler
 *
 */
public class MCRJobThread extends MCRAbstractProcessable implements Runnable {

    private static Logger LOGGER = LogManager.getLogger(MCRJobThread.class);

    protected final MCRJobQueue queue;

    protected MCRJob job = null;

    private List<MCRJobStatusListener> listeners;

    public MCRJobThread(MCRJob job) {
        this.job = job;
        setName(this.job.getId() + " - " + this.job.getAction().getSimpleName());
        setStatus(MCRProcessableStatus.created);
        this.queue = MCRJobQueue.getInstance(job.getAction());
        job.getParameters().forEach((k, v) -> this.getProperties().put(k, v));

        listeners = new ArrayList<>();
        MCRConfiguration2.getString("MCR.QueuedJob." + job.getAction().getSimpleName() + ".Listeners")
            .ifPresent(classNames -> {
                for (String className : classNames.split(",")) {
                    try {
                        MCRJobStatusListener instance = (MCRJobStatusListener) MCRClassTools.forName(className)
                            .getDeclaredConstructor().newInstance();
                        listeners.add(instance);
                    } catch (Exception e) {
                        LOGGER.error("Could not load class {}", className, e);
                    }
                }
            });
    }

    public void run() {
        MCRSessionMgr.unlock();
        MCRSession mcrSession = MCRSessionMgr.getCurrentSession();
        mcrSession.setUserInformation(MCRSystemUserInformation.getSystemUserInstance());
        EntityManager em = MCREntityManagerProvider.getEntityManagerFactory().createEntityManager();
        EntityTransaction transaction = em.getTransaction();
        try {
            Class<? extends MCRJobAction> actionClass = job.getAction();
            Constructor<? extends MCRJobAction> actionConstructor = actionClass.getConstructor(MCRJob.class);
            MCRJobAction action = actionConstructor.newInstance(job);

            transaction.begin();

            try {
                setStatus(MCRProcessableStatus.processing);
                job.setStart(new Date());
                listeners.forEach(l -> l.onProcessing(job));

                action.execute();

                job.setFinished(new Date());
                job.setStatus(MCRJobStatus.FINISHED);
                setStatus(MCRProcessableStatus.successful);
                listeners.forEach(l -> l.onSuccess(job));
            } catch (ExecutionException ex) {
                LOGGER.error("Exception occured while try to start job. Perform rollback.", ex);
                setError(ex);
                action.rollback();
                listeners.forEach(l -> l.onError(job));
            } catch (Exception ex) {
                LOGGER.error("Exception occured while try to start job.", ex);
                setError(ex);
                listeners.forEach(l -> l.onError(job));
            }
            em.merge(job);
            transaction.commit();

            // notify the queue we have processed the job
            synchronized (queue) {
                queue.notifyAll();
            }
        } catch (Exception e) {
            LOGGER.error("Error while getting next job.", e);
            if (transaction != null) {
                transaction.rollback();
            }
        } finally {
            em.close();
            MCRSessionMgr.releaseCurrentSession();
            mcrSession.close();
        }
    }

}
