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
package org.mycore.services.queuedjob;

import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.mycore.frontend.jersey.filter.access.MCRRestrictedAccess;

import jakarta.inject.Singleton;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlValue;

/**
 * @author Ren\u00E9 Adler (eagle)
 *
 */
@Path("jobqueue")
@Singleton
public class MCRJobQueueResource {

    @GET()
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @MCRRestrictedAccess(MCRJobQueuePermission.class)
    public Response listJSON() {
        try {
            Queues queuesEntity = new Queues();
            queuesEntity.addAll(
                MCRJobQueue.INSTANCES.keySet().stream().map(Queue::new).collect(Collectors.toList()));

            return Response
                .ok()
                .status(Response.Status.OK)
                .entity(queuesEntity)
                .build();
        } catch (Exception e) {
            final StreamingOutput so = (OutputStream os) -> e
                .printStackTrace(new PrintStream(os, false, StandardCharsets.UTF_8.name()));
            return Response
                .status(Response.Status.INTERNAL_SERVER_ERROR)
                .type(MediaType.TEXT_PLAIN_TYPE)
                .entity(so)
                .build();
        }
    }

    @GET()
    @Path("{name:.+}")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @MCRRestrictedAccess(MCRJobQueuePermission.class)
    public Response queueJSON(@PathParam("name") String name) {
        try {
            Queue queue = MCRJobQueue.INSTANCES.entrySet().stream().filter(e -> e.getKey().equals(name)).findFirst()
                .map(e -> {
                    Queue q = new Queue(e.getKey());

                    MCRJobQueue jq = e.getValue();
                    Iterable<MCRJob> iterable = () -> jq.iterator(null);
                    q.jobs = StreamSupport.stream(iterable.spliterator(), false).map(Job::new)
                        .collect(Collectors.toList());

                    return q;
                }).orElse(null);

            return Response
                .ok()
                .status(Response.Status.OK)
                .entity(queue)
                .build();
        } catch (Exception e) {
            final StreamingOutput so = (OutputStream os) -> e
                .printStackTrace(new PrintStream(os, false, StandardCharsets.UTF_8.name()));
            return Response
                .status(Response.Status.INTERNAL_SERVER_ERROR)
                .type(MediaType.TEXT_PLAIN_TYPE)
                .entity(so)
                .build();
        }
    }

    @XmlRootElement(name = "queues")
    static class Queues {
        @XmlElement(name = "queue")
        List<Queue> queues;

        void add(Queue queue) {
            if (queues == null) {
                queues = new ArrayList<>();
            }

            queues.add(queue);
        }

        void addAll(List<Queue> queues) {
            if (this.queues == null) {
                this.queues = new ArrayList<>();
            }

            this.queues.addAll(queues);
        }
    }

    @XmlRootElement(name = "queue")
    static class Queue {
        @XmlAttribute
        String name;

        @XmlElement(name = "job")
        List<Job> jobs;

        Queue() {
        }

        Queue(String name) {
            this.name = name;
        }
    }

    @XmlRootElement(name = "job")
    static class Job {
        @XmlAttribute
        long id;

        @XmlAttribute
        String status;

        @XmlElement(name = "date")
        List<TypedDate> dates;

        @XmlElement(name = "parameter")
        List<Parameter> parameters;

        Job() {
        }

        Job(MCRJob job) {
            this.id = job.getId();
            this.status = job.getStatus().toString().toLowerCase(Locale.ROOT);

            List<TypedDate> dates = new ArrayList<>();
            if (job.getAdded() != null) {
                dates.add(new TypedDate("added", job.getAdded()));
            }
            if (job.getStart() != null) {
                dates.add(new TypedDate("start", job.getStart()));
            }
            if (job.getFinished() != null) {
                dates.add(new TypedDate("finished", job.getFinished()));
            }

            this.parameters = job.getParameters().entrySet().stream().map(e -> new Parameter(e.getKey(), e.getValue()))
                .collect(Collectors.toList());

            if (!dates.isEmpty()) {
                this.dates = dates;
            }
        }
    }

    @XmlRootElement(name = "date")
    static class TypedDate {
        @XmlAttribute
        String type;

        @XmlValue
        Date date;

        TypedDate() {
        }

        TypedDate(String type, Date date) {
            this.type = type;
            this.date = date;
        }
    }

    @XmlRootElement(name = "parameter")
    static class Parameter {
        @XmlAttribute
        String name;

        @XmlValue
        String value;

        Parameter() {
        }

        Parameter(String name, String value) {
            this.name = name;
            this.value = value;
        }
    }

}
