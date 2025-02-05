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

package org.mycore.frontend.filter;

import java.io.IOException;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.config.MCRConfiguration2;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * Automatically closes HttpSession of certain user agents.
 * 
 * If the <code>User-Agent</code> header matches a regular expression
 * defined by the property <code>MCR.Filter.UserAgent</code>
 * (default: "<code>(bot|spider|crawler|mercator|slurp|seek|nagios)</code>") the
 * HTTP session is closed after the request.
 * @author Thomas Scheffler (yagee)
 */
public class MCRUserAgentFilter implements Filter {
    private static Pattern agentPattern;

    private static final Logger LOGGER = LogManager.getLogger(MCRUserAgentFilter.class);

    @Override
    public void init(final FilterConfig arg0) throws ServletException {
        final String agentRegEx = MCRConfiguration2.getString("MCR.Filter.UserAgent")
            .orElse("(bot|spider|crawler|mercator|slurp|seek|nagios|Java)");
        agentPattern = Pattern.compile(agentRegEx);
    }

    @Override
    public void destroy() {
    }

    @Override
    public void doFilter(final ServletRequest sreq, final ServletResponse sres, final FilterChain chain)
        throws IOException, ServletException {
        final HttpServletRequest request = (HttpServletRequest) sreq;
        final boolean newSession = request.getSession(false) == null;
        chain.doFilter(sreq, sres);
        final HttpSession session = request.getSession(false);
        if (session != null && newSession) {
            final String userAgent = request.getHeader("User-Agent");
            if (userAgent != null) {
                if (agentPattern.matcher(userAgent).find()) {
                    try {
                        LOGGER.info("Closing session: {} matches {}", userAgent, agentPattern);
                        session.invalidate();
                    } catch (IllegalStateException e) {
                        LOGGER.warn("Session was allready closed");
                    }
                } else {
                    LOGGER.debug("{} does not match {}", userAgent, agentPattern);
                }
            } else {
                LOGGER.warn("No User-Agent was send.");
            }
        }
    }

}
