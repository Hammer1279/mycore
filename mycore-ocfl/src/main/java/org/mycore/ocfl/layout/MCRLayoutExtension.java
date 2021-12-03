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

package org.mycore.ocfl.layout;

import org.mycore.common.config.MCRConfigurationException;

import edu.wisc.library.ocfl.core.extension.OcflExtensionConfig;
import edu.wisc.library.ocfl.core.extension.storage.layout.OcflStorageLayoutExtension;

public class MCRLayoutExtension implements OcflStorageLayoutExtension {

    public static final String EXTENSION_NAME = "MCRLayout";

    private MCRLayoutConfig config;

    public MCRLayoutExtension() {
        
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getExtensionName() {
        return EXTENSION_NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Description coming Soon™";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void init(OcflExtensionConfig config) {
        // if (this.config == null) {
        //     throw new MCRConfigurationException("config cannot be null");
        // }

        // check if correct config

        MCRLayoutConfig castConfig = (MCRLayoutConfig) config;

        // validate config
        this.config = castConfig;
    }

    @Override
    public Class<? extends OcflExtensionConfig> getExtensionConfigClass() {
        return MCRLayoutConfig.class;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String mapObjectId(String objectId) {
        // mcrobject:ULURZ_mods_00001122
        // mcrobject:ULURZ_derivate_00001122
        if (config == null) {
            throw new MCRConfigurationException("Extension must be initialized before usage!");
        }
        StringBuilder builder = new StringBuilder();
        String mcrid = objectId.replaceAll(".*:","");
        String[] idParts = mcrid.split("_");
        builder.append(idParts[0]).append("/").append(idParts[1]).append("/");
        String pattern = config.getPattern();
        String id = idParts[2];
        String[] layers = config.getSlotLayout().split("-");
        int position = 0;
        int i = 1;
        for (String layer: layers) {
            if(i==layers.length){break;}
            int layernum = Integer.parseInt(layer);
            String layerid = id.substring(position, position+layernum);
            builder.append(layerid).append("/");
            position+=layernum;
            i++;
        }
        builder.append(mcrid);
        return builder.toString();
    }
}
