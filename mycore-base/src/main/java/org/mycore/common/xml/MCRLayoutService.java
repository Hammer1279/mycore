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

package org.mycore.common.xml;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Locale;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRException;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRTransactionHelper;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.transformer.MCRContentTransformer;
import org.mycore.common.content.transformer.MCRParameterizedTransformer;
import org.mycore.common.xsl.MCRParameterCollector;
import org.xml.sax.SAXException;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Does the layout for other MyCoRe servlets by transforming XML input to
 * various output formats, using XSL stylesheets.
 * 
 * @author Frank Lützenkirchen
 * @author Thomas Scheffler (yagee)
 */
public class MCRLayoutService {

    private static final int INITIAL_BUFFER_SIZE = 32 * 1024;

    static final Logger LOGGER = LogManager.getLogger(MCRLayoutService.class);

    private static final MCRLayoutService SINGLETON = new MCRLayoutService();

    private static final String TRANSFORMER_FACTORY_PROPERTY = "MCR.Layout.Transformer.Factory";

    public static MCRLayoutService instance() {
        return SINGLETON;
    }

    public void sendXML(HttpServletRequest req, HttpServletResponse res, MCRContent xml) throws IOException {
        res.setContentType("text/xml; charset=UTF-8");
        try {
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.INDENT, "no");
            StreamResult result = new StreamResult(res.getOutputStream());
            transformer.transform(xml.getSource(), result);
        } catch (TransformerException e) {
            throw new MCRException(e);
        }
        res.flushBuffer();
    }

    public void doLayout(HttpServletRequest req, HttpServletResponse res, MCRContent source) throws IOException,
        TransformerException, SAXException {
        if (res.isCommitted()) {
            LOGGER.warn("Response already committed: {}:{}", res.getStatus(), res.getContentType());
            return;
        }
        String docType = source.getDocType();
        try {
            MCRParameterCollector parameter = new MCRParameterCollector(req);
            MCRContentTransformer transformer = getContentTransformer(docType, parameter);
            String filename = getFileName(req, parameter);
            transform(res, transformer, source, parameter, filename);
        } catch (IOException | TransformerException | SAXException ex) {
            throw ex;
        } catch (MCRException ex) {
            // Check if it is an error page to suppress later recursively
            // generating an error page when there is an error in the stylesheet
            if (!"mcr_error".equals(docType)) {
                throw ex;
            }

            String msg = "Error while generating error page!";
            LOGGER.warn(msg, ex);
            res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, msg);
        } catch (Exception e) {
            throw new MCRException(e);
        }
    }

    public MCRContent getTransformedContent(HttpServletRequest req, HttpServletResponse res, MCRContent source)
        throws IOException, TransformerException, SAXException {
        String docType = source.getDocType();
        try {
            MCRParameterCollector parameter = new MCRParameterCollector(req);
            MCRContentTransformer transformer = getContentTransformer(docType, parameter);
            String filename = getFileName(req, parameter);
            return transform(transformer, source, parameter, filename);
        } catch (IOException | TransformerException | SAXException ex) {
            throw ex;
        } catch (MCRException ex) {
            // Check if it is an error page to suppress later recursively
            // generating an error page when there is an error in the stylesheet
            if (!"mcr_error".equals(docType)) {
                throw ex;
            }

            String msg = "Error while generating error page!";
            LOGGER.warn(msg, ex);
            res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, msg);
            return null;
        } catch (Exception e) {
            throw new MCRException(e);
        }
    }

    public static MCRContentTransformer getContentTransformer(String docType, MCRParameterCollector parameter)
        throws Exception {
        String transformerId = parameter.getParameter("Transformer", null);
        if (transformerId == null) {
            String style = parameter.getParameter("Style", "default");
            transformerId = new MessageFormat("{0}-{1}", Locale.ROOT).format(new Object[] { docType, style });
        }
        MCRLayoutTransformerFactory factory = MCRConfiguration2.<MCRLayoutTransformerFactory>getInstanceOf(
            TRANSFORMER_FACTORY_PROPERTY)
            .orElseGet(MCRLayoutTransformerFactory::new);
        return factory.getTransformer(transformerId);
    }

    private String getFileName(HttpServletRequest req, MCRParameterCollector parameter) {
        String filename = parameter.getParameter("FileName", null);
        if (filename != null) {
            if (req.getServletPath().contains(filename)) {
                //filter out MCRStaticXMLFileServlet as it defines "FileName"
                return extractFileName(req.getServletPath());
            }
            return filename;
        }
        if (req.getPathInfo() != null) {
            return extractFileName(req.getPathInfo());
        }
        return new MessageFormat("{0}-{1}", Locale.ROOT).format(
            new Object[] { extractFileName(req.getServletPath()), String.valueOf(System.currentTimeMillis())});
    }

    private String extractFileName(String filename) {
        int filePosition = filename.lastIndexOf('/') + 1;
        filename = filename.substring(filePosition);
        filePosition = filename.lastIndexOf('.');
        if (filePosition > 0) {
            filename = filename.substring(0, filePosition);
        }
        return filename;
    }

    private void transform(HttpServletResponse response, MCRContentTransformer transformer, MCRContent source,
        MCRParameterCollector parameter, String filename) throws IOException, TransformerException, SAXException {
        try {
            String fileExtension = transformer.getFileExtension();
            if (fileExtension != null && fileExtension.length() > 0) {
                filename += "." + fileExtension;
            }
            response.setHeader("Content-Disposition",
                transformer.getContentDisposition() + ";filename=\"" + filename + "\"");
            String ct = transformer.getMimeType();
            String enc = transformer.getEncoding();
            if (enc != null) {
                response.setCharacterEncoding(enc);
                response.setContentType(ct + "; charset=" + enc);
            } else {
                response.setContentType(ct);
            }
            LOGGER.debug("MCRLayoutService starts to output {}", response.getContentType());
            ServletOutputStream servletOutputStream = response.getOutputStream();
            long start = System.currentTimeMillis();
            try {
                ByteArrayOutputStream bout = new ByteArrayOutputStream(INITIAL_BUFFER_SIZE);
                if (transformer instanceof MCRParameterizedTransformer) {
                    MCRParameterizedTransformer paramTransformer = (MCRParameterizedTransformer) transformer;
                    paramTransformer.transform(source, bout, parameter);
                } else {
                    transformer.transform(source, bout);
                }
                endCurrentTransaction();
                response.setContentLength(bout.size());
                bout.writeTo(servletOutputStream);
            } finally {
                LOGGER.debug("MCRContent transformation took {} ms.", System.currentTimeMillis() - start);
            }
        } catch (TransformerException | IOException | SAXException e) {
            throw e;
        } catch (Exception e) {
            Throwable cause = e.getCause();
            while (cause != null) {
                if (cause instanceof TransformerException) {
                    throw (TransformerException) cause;
                } else if (cause instanceof SAXException) {
                    throw (SAXException) cause;
                } else if (cause instanceof IOException) {
                    throw (IOException) cause;
                }
                cause = cause.getCause();
            }
            throw new IOException(e);
        }
    }

    private MCRContent transform(MCRContentTransformer transformer, MCRContent source, MCRParameterCollector parameter,
        String filename) throws IOException, TransformerException, SAXException {
        LOGGER.debug("MCRLayoutService starts to output {}", getMimeType(transformer));
        long start = System.currentTimeMillis();
        try {
            if (transformer instanceof MCRParameterizedTransformer) {
                MCRParameterizedTransformer paramTransformer = (MCRParameterizedTransformer) transformer;
                return paramTransformer.transform(source, parameter);
            } else {
                return transformer.transform(source);
            }
        } finally {
            LOGGER.debug("MCRContent transformation took {} ms.", System.currentTimeMillis() - start);
        }
    }

    private String getMimeType(MCRContentTransformer transformer) throws IOException, TransformerException,
        SAXException {
        try {
            return transformer.getMimeType();
        } catch (IOException | TransformerException | SAXException | RuntimeException e) {
            throw e;
        } catch (Exception e) {
            return "application/octet-stream";
        }
    }

    /**
     * Called before sending data to end hibernate transaction.
     */
    private static void endCurrentTransaction() {
        MCRSessionMgr.getCurrentSession();
        MCRTransactionHelper.commitTransaction();
    }
}
