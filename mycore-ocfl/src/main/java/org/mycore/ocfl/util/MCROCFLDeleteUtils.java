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

package org.mycore.ocfl.util;

import org.mycore.common.config.MCRConfiguration2;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.metadata.MCRObjectID;

/**
 * @author Tobias Lenhardt [Hammer1279]
 */
public final class MCROCFLDeleteUtils {

    // private static final String PROPERTY_PREFIX = "MCR.OCFL.instantPurge.";
    private static final String PROPERTY_PREFIX = "MCR.OCFL.dropHistory.";
    // private static final String PROPERTY_PREFIX = "MCR.OCFL.forceDelete.";

    private MCROCFLDeleteUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static boolean checkPurgeObject(MCRObjectID mcrid) {
        String prefix = MCROCFLObjectIDPrefixHelper.MCROBJECT;
        return checkPurgeObject(mcrid, prefix);
    }

    public static boolean checkPurgeDerivate(MCRObjectID mcrid) {
        String prefix = MCROCFLObjectIDPrefixHelper.MCRDERIVATE;
        return checkPurgeObject(mcrid, prefix);
    }

    public static boolean checkPurgeObject(MCRObjectID mcrid, String prefix) {
        String ocflType = prefix.replace(":", "");

        boolean doPurge = false;
        doPurge
            = MCRConfiguration2.getBoolean(PROPERTY_PREFIX.substring(0, PROPERTY_PREFIX.length() - 1)).orElse(doPurge);
        doPurge = MCRConfiguration2.getBoolean(PROPERTY_PREFIX + ocflType).orElse(doPurge);
        doPurge = MCRConfiguration2.getBoolean(PROPERTY_PREFIX + mcrid.getProjectId()).orElse(doPurge);
        doPurge = MCRConfiguration2.getBoolean(PROPERTY_PREFIX + mcrid.getTypeId()).orElse(doPurge);
        doPurge = MCRConfiguration2.getBoolean(PROPERTY_PREFIX + mcrid.getBase()).orElse(doPurge);
        doPurge
            = MCRConfiguration2.getBoolean(PROPERTY_PREFIX + ocflType + "." + mcrid.getProjectId()).orElse(doPurge);
        doPurge = MCRConfiguration2.getBoolean(PROPERTY_PREFIX + ocflType + "." + mcrid.getTypeId()).orElse(doPurge);
        doPurge = MCRConfiguration2.getBoolean(PROPERTY_PREFIX + ocflType + "." + mcrid.getBase()).orElse(doPurge);
        doPurge = MCRConfiguration2.getBoolean(PROPERTY_PREFIX + mcrid).orElse(doPurge);
        doPurge = MCRConfiguration2.getBoolean(PROPERTY_PREFIX + ocflType + "." + mcrid).orElse(doPurge);
        return doPurge;
    }

    public static boolean checkPurgeClass(MCRCategoryID mcrid) {
        String prefix = MCROCFLObjectIDPrefixHelper.CLASSIFICATION;
        return checkPurgeClass(mcrid, prefix);
    }

    public static boolean checkPurgeClass(MCRCategoryID mcrid, String prefix) {
        String ocflType = prefix.replace(":", "");

        boolean doPurge = false;
        doPurge
            = MCRConfiguration2.getBoolean(PROPERTY_PREFIX.substring(0, PROPERTY_PREFIX.length() - 1)).orElse(doPurge);
        doPurge = MCRConfiguration2.getBoolean(PROPERTY_PREFIX + ocflType).orElse(doPurge);
        doPurge = MCRConfiguration2.getBoolean(PROPERTY_PREFIX + mcrid.getRootID()).orElse(doPurge);
        doPurge = MCRConfiguration2.getBoolean(PROPERTY_PREFIX + ocflType + "." + mcrid.getRootID()).orElse(doPurge);
        return doPurge;
    }

    public static boolean checkPurgeUser(String userID) {
        String prefix = MCROCFLObjectIDPrefixHelper.USER;
        return checkPurgeUser(userID, prefix);
    }

    public static boolean checkPurgeUser(String userID, String prefix) {
        String ocflType = prefix.replace(":", "");

        boolean doPurge = false;
        doPurge
            = MCRConfiguration2.getBoolean(PROPERTY_PREFIX.substring(0, PROPERTY_PREFIX.length() - 1)).orElse(doPurge);
        doPurge = MCRConfiguration2.getBoolean(PROPERTY_PREFIX + ocflType).orElse(doPurge);
        doPurge = MCRConfiguration2.getBoolean(PROPERTY_PREFIX + userID).orElse(doPurge);
        doPurge = MCRConfiguration2.getBoolean(PROPERTY_PREFIX + ocflType + "." + userID).orElse(doPurge);
        return doPurge;
    }
}
