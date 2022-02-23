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

import org.mycore.common.events.MCREvent;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRObjectDerivate;
import org.mycore.datamodel.metadata.MCRObjectID;

/**
 * Class to manage Derivate storage for Objects in the OCFL Store
 * @author Tobias Lenhardt [Hammer1279]
 */
public class MCROCFLDerivateStoreManager extends MCROCFLXMLClassificationManager {

    // INFO extending from MCROCFLXMLClassificationManager to inherit the protected methods

    public void derivateUpdate(MCRObjectID derivateID, MCRDerivate derivate, MCRObjectDerivate objectDerivate, MCREvent event) {
        // TODO figure something out how to store this, see MCRMetalIFS, MCRMetalLink, MCRObjectDerivate
    }

}
