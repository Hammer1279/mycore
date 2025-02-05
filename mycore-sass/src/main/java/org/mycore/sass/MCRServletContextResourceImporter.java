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

package org.mycore.sass;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.util.IOUtils;
import org.mycore.common.MCRDeveloperTools;

import io.bit3.jsass.importer.Import;
import io.bit3.jsass.importer.Importer;
import jakarta.servlet.ServletContext;

/**
 * Imports scss files using {@link ServletContext}.
 */
public class MCRServletContextResourceImporter implements Importer {

    private static final Logger LOGGER = LogManager.getLogger();

    private final ServletContext context;

    /**
     * Initialize MCRServletContextResourceImporter
     * @param context - the servlet context
     */
    public MCRServletContextResourceImporter(ServletContext context) {
        this.context = context;
    }

    @Override
    public Collection<Import> apply(String url, Import previous) {
        try {

            final String absolute = previous != null ? previous.getAbsoluteUri().resolve(url).toString() : url;

            List<String> possibleNameForms = getPossibleNameForms(absolute);

            Optional<URL> firstPossibleName = possibleNameForms.stream()
                .map(form -> {
                    try {
                        if (MCRDeveloperTools.overrideActive()) {
                            final Optional<Path> overriddenFilePath = MCRDeveloperTools
                                .getOverriddenFilePath(form.startsWith("/") ? form.substring(1) : form, true);

                            if (overriddenFilePath.isPresent()) {
                                return overriddenFilePath.get().toUri().toURL();
                            }
                        }

                        return context.getResource(normalize(form));
                    } catch (MalformedURLException e) {
                        // ignore exception because it seems to be a not valid name form
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .findFirst();

            if (!firstPossibleName.isPresent()) {
                return null;
            }
            URL resource = firstPossibleName.get();

            String contents = getStringContent(resource);
            URI absoluteUri = resource.toURI();

            LOGGER.debug("Resolved {} to {}", url, absoluteUri);
            return buildImport(absolute, contents);
        } catch (IOException | URISyntaxException e) {
            LOGGER.error("Error while resolving {}", url, e);
            return null;
        }
    }

    private List<Import> buildImport(String absolute, String contents) throws URISyntaxException {
        return Stream.of(new Import(absolute, absolute, contents)).collect(Collectors.toList());
    }

    private List<String> getPossibleNameForms(String relative) {
        ArrayList<String> nameFormArray = new ArrayList<>();

        int lastSlashPos = relative.lastIndexOf('/');
        if (lastSlashPos != -1) {
            String form = relative.substring(0, lastSlashPos) + "/_" + relative.substring(lastSlashPos + 1);
            nameFormArray.add(form);
            nameFormArray.add(form + ".scss");
        }

        nameFormArray.add(relative);
        nameFormArray.add(relative + ".scss");

        return nameFormArray;
    }

    private String getStringContent(URL resource) throws IOException {
        try (InputStream resourceAsStream = resource.openStream()) {
            InputStreamReader inputStreamReader = new InputStreamReader(resourceAsStream, StandardCharsets.UTF_8);
            return IOUtils.toString(inputStreamReader);
        }
    }

    private String normalize(String resource) {
        return !resource.startsWith("/") ? "/" + resource : resource;
    }
}
