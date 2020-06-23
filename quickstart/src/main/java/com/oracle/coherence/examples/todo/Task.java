package com.oracle.coherence.examples.todo;

import java.io.Serializable;
import java.util.UUID;

import javax.json.bind.annotation.JsonbTransient;

/**
 * Data class representing single To Do list task.
 */
public class Task
        implements Serializable {

    private UUID id;
    private String description;
    private Boolean completed;

    @JsonbTransient
    private long createdAt;

    public Task() {
    }

    public Task(String description) {
        this.id = UUID.randomUUID();
        this.description = description;
        this.completed = false;
        this.createdAt = System.currentTimeMillis();
    }

    public UUID getId() {
        return id;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public String getDescription() {
        return description;
    }

    public Task setDescription(String description) {
        this.description = description;
        return this;
    }

    public Boolean isCompleted() {
        return completed;
    }

    public Task setCompleted(boolean completed) {
        this.completed = completed;
        return this;
    }
}
