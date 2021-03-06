/*
COPYRIGHT STATUS:
Dec 1st 2001, Fermi National Accelerator Laboratory (FNAL) documents and
software are sponsored by the U.S. Department of Energy under Contract No.
DE-AC02-76CH03000. Therefore, the U.S. Government retains a  world-wide
non-exclusive, royalty-free license to publish or reproduce these documents
and software for U.S. Government purposes.  All documents and software
available from this server are protected under the U.S. and Foreign
Copyright Laws, and FNAL reserves all rights.

Distribution of the software available from this server is free of
charge subject to the user following the terms of the Fermitools
Software Legal Information.

Redistribution and/or modification of the software shall be accompanied
by the Fermitools Software Legal Information  (including the copyright
notice).

The user is asked to feed back problems, benefits, and/or suggestions
about the software to the Fermilab Software Providers.

Neither the name of Fermilab, the  URA, nor the names of the contributors
may be used to endorse or promote products derived from this software
without specific prior written permission.

DISCLAIMER OF LIABILITY (BSD):

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED  WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED  WARRANTIES OF MERCHANTABILITY AND FITNESS
FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL FERMILAB,
OR THE URA, OR THE U.S. DEPARTMENT of ENERGY, OR CONTRIBUTORS BE LIABLE
FOR  ANY  DIRECT, INDIRECT,  INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT
OF SUBSTITUTE  GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY  OF
LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT  OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE  POSSIBILITY OF SUCH DAMAGE.

Liabilities of the Government:

This software is provided by URA, independent from its Prime Contract
with the U.S. Department of Energy. URA is acting independently from
the Government and in its own private capacity and is not acting on
behalf of the U.S. Government, nor as its contractor nor its agent.
Correspondingly, it is understood and agreed that the U.S. Government
has no connection to this software and in no manner whatsoever shall
be liable for nor assume any responsibility or obligation for any claim,
cost, or damages arising out of or resulting from the use of the software
available from this server.

Export Control:

All documents and software available from this server are subject to U.S.
export control laws.  Anyone downloading information from this server is
obligated to secure any necessary Government licenses before exporting
documents or software obtained from this server.
 */
package org.dcache.restful.resources.alarms;

import org.json.JSONException;
import org.json.JSONObject;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Map;
import java.util.UUID;

import diskCacheV111.util.CacheException;
import org.dcache.restful.providers.alarms.AlarmsList;
import org.dcache.restful.services.alarms.AlarmsInfoService;
import org.dcache.restful.util.HttpServletRequests;
import org.dcache.restful.util.ServletContextHandlerAttributes;

import static org.dcache.restful.providers.SuccessfulResponse.successfulResponse;

/**
 * <p>RESTful API to the {@link AlarmsInfoService} service.</p>
 *
 * @version v1.0
 */
@Path("/alarms")
public final class AlarmsResources {
    /**
     * <p>Contains the reference to the {@link AlarmsInfoService} implementation.</p>
     */
    @Context
    ServletContext ctx;

    @Context
    HttpServletRequest request;

    /**
     * <p>Alarms.</p>
     *
     * <p>The Alarms endpoint returns a (filtered) list of alarms.</p>
     *
     * <p>Expected behavior:  if a token is given along with one of the
     *    query parameters (after, before, type), but the corresponding
     *    snapshop does not have the same values for those parameters,
     *    a new snapshot with a new token will be returned, setting offset
     *    back to 0.</p>
     *
     * @param token  Use the snapshot corresponding to this UUID.
     *               A <code>null</code> value indicates a request for
     *               refreshed (i.e., current) alarms.
     * @param offset Return alarms beginning at this index.
     * @param limit  Return at most this number of items.
     * @param after  Return no alarms before this datestamp.
     * @param before Return no alarms after this datestamp.
     * @param type   Return only alarms of this type.
     *
     * @return object containing list of transfers, along with token and
     *         offset information.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public AlarmsList getAlarms(@QueryParam("token") UUID token,
                                   @QueryParam("offset") Integer offset,
                                   @QueryParam("limit") Integer limit,
                                   @QueryParam("after") Long after,
                                   @QueryParam("before") Long before,
                                   @QueryParam("type") String type) {
        if (!HttpServletRequests.isAdmin(request)) {
            throw new ForbiddenException(
                            "Alarm service only accessible to admin users.");
        }

        try {
            return ServletContextHandlerAttributes.getAlarmsInfoService(ctx)
                                                  .get(token,
                                                       offset,
                                                       limit,
                                                       after,
                                                       before,
                                                       type);
        } catch (IllegalArgumentException | CacheException e) {
            throw new BadRequestException(e);
        } catch (Exception e) {
            throw new InternalServerErrorException(e);
        }
    }

    /**
     * <p>Request for current mapping of alarm types to priorities.</p>
     *
     * @return requested priority map
     */
    @GET
    @Path("/map")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, String> getAlarmsMap() {
        if (!HttpServletRequests.isAdmin(request)) {
            throw new ForbiddenException(
                            "Alarm service only accessible to admin users.");
        }

        return ServletContextHandlerAttributes.getAlarmsInfoService(ctx)
                                              .getMap();
    }

    /**
     * <p>Request to delete the indicated alarm from the service's store.</p>
     *
     * @param token  Use the snapshot corresponding to this UUID.
     * @param index  Index of the alarm in this snapshot.
     */
    @DELETE
    @Path("/{token : .*}-{index : .*}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteAlarmEntry(@NotNull
                                     @PathParam("token") UUID token,
                                     @PathParam("index") Integer index) throws
                    CacheException {
        if (!HttpServletRequests.isAdmin(request)) {
            throw new ForbiddenException(
                            "Alarm service only accessible to admin users.");
        }

        try {
            ServletContextHandlerAttributes.getAlarmsInfoService(ctx)
                                           .delete(token, index);
        } catch (IllegalArgumentException | CacheException e) {
            throw new BadRequestException(e);
        } catch (Exception e) {
            throw new InternalServerErrorException(e);
        }

        return successfulResponse(Response.Status.OK);
    }

    /**
     * <p>Request to update the indicated alarm.</p>
     *
     * @param token  Use the snapshot corresponding to this UUID.
     * @param index  Index of the alarm in this snapshot.
     * @param requestPayload indicating the action and the value to update.
     */
    @POST
    @Path("/{token : .*}-{index : .*}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateAlarmEntry(@PathParam("token") UUID token,
                                     @PathParam("index") Integer index,
                                     String requestPayload) throws
                    CacheException {

        if (!HttpServletRequests.isAdmin(request)) {
            throw new ForbiddenException(
                            "Alarm service only accessible to admin users.");
        }

        try {
            JSONObject reqPayload = new JSONObject(requestPayload);
            String action = (String) reqPayload.get("action");
            String value  = (String) reqPayload.get("value");
            AlarmsInfoService service =
                            ServletContextHandlerAttributes.getAlarmsInfoService(ctx);

            switch(action) {
                case "close":
                    service.update(token, index, Boolean.parseBoolean(value));
                    break;
                case "comment":
                    service.update(token, index, value);
                    break;
                default:
                    throw new IllegalArgumentException("unsupported request: "
                                                                       + action
                                                                       + ", "
                                                                       + value);
            }
        } catch (JSONException | IllegalArgumentException | CacheException e) {
            throw new BadRequestException(e);
        } catch (Exception e) {
            throw new InternalServerErrorException(e);
        }

        return successfulResponse(Response.Status.OK);
    }
}