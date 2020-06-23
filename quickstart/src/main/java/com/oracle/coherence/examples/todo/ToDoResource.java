package com.oracle.coherence.examples.todo;

import java.util.Collection;
import java.util.Comparator;
import java.util.Objects;
import java.util.UUID;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import com.tangosol.net.NamedMap;
import com.tangosol.util.Filter;
import com.tangosol.util.Filters;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

/**
 * REST API for To Do list management.
 */
@Path("/tasks")
public class ToDoResource {
    @Inject
    private NamedMap<UUID, Task> tasks;

    @GET
    @Produces(APPLICATION_JSON)
    public Collection<Task> getTasks(@QueryParam("completed") Boolean completed) {
        Filter<Task> filter = completed == null
                              ? Filters.always()
                              : Filters.equal(Task::isCompleted, completed);

        return tasks.values(filter, Comparator.comparingLong(Task::getCreatedAt));
    }

    @POST
    @Consumes(APPLICATION_JSON)
    public void createTask(Task task) {
        task = new Task(task.getDescription());
        tasks.put(task.getId(), task);
    }

    @DELETE
    @Path("{id}")
    public void deleteTask(@PathParam("id") UUID id) {
        tasks.remove(id);
    }

    @PUT
    @Path("{id}")
    @Consumes(APPLICATION_JSON)
    public Task updateTask(@PathParam("id") UUID id, Task task) {
        String  description = task.getDescription();
        Boolean completed   = task.isCompleted();

        return tasks.compute(id, (k, v) -> {
            Objects.requireNonNull(v);
            
            if (description != null) {
                v.setDescription(description);
            }
            if (completed != null) {
                v.setCompleted(completed);
            }
            return v;
        });
    }
}
