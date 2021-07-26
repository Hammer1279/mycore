/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.mcr.acl.accesskey.restapi.v2;

import static org.mycore.restapi.v2.MCRRestAuthorizationFilter.PARAM_MCRID;

import org.mycore.access.MCRAccessManager;
import org.mycore.mcr.acl.accesskey.restapi.v2.annotation.MCRRequireAccessKeyAuthorization;
import org.mycore.restapi.v2.MCRErrorResponse;

import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

@Provider
@MCRRequireAccessKeyAuthorization
public class MCRRestAccessKeyAuthorizationFilter implements ContainerRequestFilter {

    @Context
    ResourceInfo resourceInfo;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        if (!requestContext.getMethod().equals(HttpMethod.OPTIONS)) {
            final MultivaluedMap<String, String> pathParameters = requestContext.getUriInfo().getPathParameters();
            if (MCRAccessManager.checkPermission(pathParameters.
                getFirst(PARAM_MCRID), MCRAccessManager.PERMISSION_WRITE)) {
                return;
            }
            throw MCRErrorResponse.fromStatus(Response.Status.FORBIDDEN.getStatusCode())
                .toException();
        }
    }
}
