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

package org.mycore.common.config;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map;
import java.util.jar.Manifest;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.status.StatusLogger;
import org.mycore.common.MCRClassTools;
import org.mycore.common.MCRException;

/**
 * This class abstracts different MyCoRe component types.
 * As every component (mycore component, application module) holds it configuration in different places,
 * you can use this class to get uniform access to these configuration resources.
 * 
 * As this class is immutable it could be used as key in a {@link Map}
 * @author Thomas Scheffler (yagee)
 * @since 2013.12
 * @see MCRRuntimeComponentDetector
 */
public class MCRComponent implements Comparable<MCRComponent> {

    private static final Logger LOGGER = StatusLogger.getLogger();

    private static final String ATT_PRIORITY = "Priority";

    private static final NumberFormat PRIORITY_FORMAT = getPriorityFormat();

    private static NumberFormat getPriorityFormat() {
        NumberFormat format = NumberFormat.getIntegerInstance(Locale.ROOT);
        format.setGroupingUsed(false);
        format.setMinimumIntegerDigits(3);
        return format;
    }

    private static final String DEFAULT_PRIORITY = "99";

    private enum Type {
        base, component, module
    }

    private Type type;

    private String name;

    private File jarFile;

    private String sortCriteria;

    private String artifactId;

    private Manifest manifest;

    public MCRComponent(String artifactId, Manifest manifest) {
        this(artifactId, manifest, null);
    }

    public MCRComponent(String artifactId, Manifest manifest, File jarFile) {
        if (artifactId.startsWith("mycore-")) {
            if (artifactId.endsWith("base")) {
                type = Type.base;
                setName("base");
            } else {
                type = Type.component;
                setName(artifactId.substring("mycore-".length()));
            }
        } else {
            type = Type.module;
            setName(artifactId.replaceAll("[_-]?module", ""));
        }
        setJarFile(jarFile);
        this.artifactId = artifactId;
        this.manifest = manifest;
        buildSortCriteria();
        LOGGER.debug("{} is of type {} and named {}: {}", artifactId, type, getName(), jarFile);
    }

    private void buildSortCriteria() {
        String priorityAtt = manifest.getMainAttributes().getValue(ATT_PRIORITY);
        if (priorityAtt == null) {
            priorityAtt = DEFAULT_PRIORITY;
            LOGGER.debug("{} has DEFAULT priority {}", artifactId, priorityAtt);
        } else {
            LOGGER.debug("{} has priority {}", artifactId, priorityAtt);
        }
        int priority = Integer.parseInt(priorityAtt);
        if (priority > 99 || priority < 0) {
            throw new MCRException(artifactId + " has unsupported priority: " + priority);
        }
        switch (type) {
        case base:
            priority += 100;
            break;
        case component:
            priority += 200;
            break;
        case module:
            priority += 300;
            break;
        default:
            throw new MCRException("Do not support MCRComponenty of type: " + type);
        }
        this.sortCriteria = PRIORITY_FORMAT.format(priority) + getName();
    }

    public InputStream getConfigFileStream(String filename) {
        String resourceBase = getResourceBase();
        if (resourceBase == null) {
            return null;
        }
        String resourceName = resourceBase + filename;
        InputStream resourceStream = MCRClassTools.getClassLoader().getResourceAsStream(resourceName);
        if (resourceStream != null) {
            LOGGER.info("Reading config resource: {}", resourceName);
        }
        return resourceStream;
    }

    public URL getConfigURL(String filename) {
        String resourceBase = getResourceBase();
        if (resourceBase == null) {
            return null;
        }
        String resourceName = resourceBase + filename;
        URL resourceURL = MCRClassTools.getClassLoader().getResource(resourceName);
        if (resourceURL != null) {
            LOGGER.info("Reading config resource: {}", resourceName);
        }
        return resourceURL;
    }

    /**
     * Returns resource base path to this components config resources.
     */
    public String getResourceBase() {
        switch (type) {
        case base:
            return "config/";
        case component:
            return "components/" + getName() + "/config/";
        case module:
            return "config/" + getName() + "/";
        default:
            LOGGER.debug("{}: there is no resource base for type {}", getName(), type);
            break;
        }
        return null;
    }

    /**
     * Returns true, if this component is part of MyCoRe
     */
    public boolean isMyCoReComponent() {
        return type == Type.base || type == Type.component;
    }

    /**
     * Returns true, if this component is application module 
     */
    public boolean isAppModule() {
        return type == Type.module;
    }

    /**
     * A short name for this component.
     * E.g. mycore-base would return "base" here.
     */
    public String getName() {
        return name;
    }

    private void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the jar file or <code>null</code> if nothing was set.
     * 
     * @return the jar file
     */
    public File getJarFile() {
        return jarFile;
    }

    private void setJarFile(File jarFile) {
        this.jarFile = jarFile;
    }

    /**
     * Returns the mainfest main attribute value for given attribute name.
     * 
     * @param name the attribute name
     * @return the attribute value
     */
    public String getManifestMainAttribute(String name) {
        return manifest.getMainAttributes().getValue(name);
    }

    /**
     * Compares this component to other component.
     * Basic order is:
     * <ol>
     *  <li>complete</li>
     *  <li>base</li>
     *  <li>component</li>
     *  <li>module</li>
     * </ol>
     * If more than one component is in one of these groups, they are sorted alphabetically via {@link #getName()}.
     */
    @Override
    public int compareTo(MCRComponent o) {
        return this.sortCriteria.compareTo(o.sortCriteria);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof MCRComponent)) {
            return false;
        }
        MCRComponent other = (MCRComponent) obj;
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        return type == other.type;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        switch (type) {
        case base:
        case component:
            sb.append("mcr:");
            break;
        case module:
            sb.append("app:");
            break;
        default:
            break;
        }
        sb.append(artifactId);
        return sb.toString();
    }
}
