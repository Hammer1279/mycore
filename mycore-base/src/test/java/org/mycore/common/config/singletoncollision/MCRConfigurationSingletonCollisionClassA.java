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

package org.mycore.common.config.singletoncollision;

import static org.junit.Assert.assertEquals;

import org.mycore.common.config.MCRConfiguration2;

/**
 * Utility Class to load Class B
 * @author Tobias Lenhardt [Hammer1279]
 */
public class MCRConfigurationSingletonCollisionClassA {
    static {
        Object classB = MCRConfiguration2.getSingleInstanceOf(
            MCRConfiguration2.getStringOrThrow("MCR.Configuration.SingletonCollision.PropertyName")).get();
        assertEquals("Wrong or no Class Loaded!", "MCRConfigurationSingletonCollisionClassB",
            classB.getClass().getSimpleName());
    }
}
