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

package org.mycore.pi.urn.rest;

import java.util.Optional;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.events.MCRShutdownHandler;
import org.mycore.common.events.MCRStartupHandler;
import org.mycore.pi.MCRPIRegistrationInfo;

import jakarta.servlet.ServletContext;

/**
 *  @author Huu Chi Vu
 *  @author shermann
 */
public class MCRURNGranularRESTRegistrationStarter
    implements MCRStartupHandler.AutoExecutable, MCRShutdownHandler.Closeable {

    private static final Logger LOGGER = LogManager.getLogger();

    private final long period;

    private final TimeUnit timeUnit;

    private ScheduledExecutorService scheduler;

    public MCRURNGranularRESTRegistrationStarter() {
        this(1, TimeUnit.MINUTES);
    }

    public MCRURNGranularRESTRegistrationStarter(long taskPeriod, TimeUnit timeUnit) {
        this.period = taskPeriod;
        this.timeUnit = timeUnit;
    }

    @Override
    public String getName() {
        return "URN Registration Service";
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public void startUp(ServletContext servletContext) {
        MCRShutdownHandler.getInstance().addCloseable(this);

        MCRDNBURNRestClient client = new MCRDNBURNRestClient(getBundleProvider(), getUsernamePasswordCredentials());
        MCRURNGranularRESTRegistrationTask task = new MCRURNGranularRESTRegistrationTask(client);
        Optional.of(startTimerTask(task))
            .orElseGet(this::couldNotStartTask)
            .accept(LOGGER);
    }

    private Consumer<Logger> couldNotStartTask() {
        return logger -> logger.warn("Could not start Task {}",
            MCRURNGranularRESTRegistrationTask.class.getSimpleName());
    }

    private ScheduledExecutorService getScheduler() {
        if (scheduler == null) {
            LOGGER.info("Starting executor service...");
            scheduler = Executors.newSingleThreadScheduledExecutor();
        }

        return scheduler;
    }

    private Consumer<Logger> startTimerTask(TimerTask task) {
        getScheduler().scheduleAtFixedRate(task, 0, period, timeUnit);
        return logger -> logger.info("Started task {}, refresh every {} {}", task.getClass().getSimpleName(), period,
            timeUnit);
    }

    /**
     * @deprecated Use {@link MCRURNGranularRESTRegistrationStarter#getBundleProvider()}
     * */
    @Deprecated
    public Function<MCRPIRegistrationInfo, MCREpicurLite> getEpicureProvider(UsernamePasswordCredentials credentials) {
        return urn -> MCREpicurLite.instance(urn, MCRDerivateURNUtils.getURL(urn))
            .setCredentials(credentials);
    }

    public Function<MCRPIRegistrationInfo, MCRURNJsonBundle> getBundleProvider() {
        return urn -> MCRURNJsonBundle.instance(urn, MCRDerivateURNUtils.getURL(urn));
    }

    /**
     * @deprecated Reading of properties moved to
     * {@link MCRURNGranularRESTRegistrationStarter#getUsernamePasswordCredentials()}
     * */
    @Deprecated
    public Optional<UsernamePasswordCredentials> getUsernamePassword() {
        return getUsernamePasswordCredentials();
    }

    public Optional<UsernamePasswordCredentials> getUsernamePasswordCredentials() {
        String username = MCRConfiguration2.getString("MCR.PI.DNB.Credentials.Login").orElse(null);
        String password = MCRConfiguration2.getString("MCR.PI.DNB.Credentials.Password").orElse(null);

        if (username == null || password == null || username.length() == 0 || password.length() == 0) {
            LOGGER.warn("Could not instantiate {} as required credentials are unset", this.getClass().getName());
            LOGGER.warn("Please set MCR.PI.DNB.Credentials.Login and MCR.PI.DNB.Credentials.Password");
            return Optional.empty();
        }

        return Optional.of(new UsernamePasswordCredentials(username, password));
    }

    @Override
    public void prepareClose() {
        if (scheduler != null) {
            scheduler.shutdown();
        }
    }

    @Override
    public void close() {
        if (scheduler != null && !scheduler.isShutdown()) {
            try {
                scheduler.awaitTermination(60, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                LOGGER.error("Interrupted while waiting.", e);
            }
            scheduler.shutdownNow();
        }
    }
}
