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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRException;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandler;

@Deprecated(forRemoval = true)
public class MCROCFLRawEventHandler implements MCREventHandler {

    private static final Logger LOGGER = LogManager.getLogger(MCROCFLRawEventHandler.class);

    public void doHandleEvent(MCREvent evt) throws MCRException {
        LOGGER.debug("Event of Type {} from Obj Type {} was called!", evt.getEventType(), evt.getObjectType());
    }

    public void undoHandleEvent(MCREvent evt) throws MCRException {
        // TODO Undo the Log
    }
}
