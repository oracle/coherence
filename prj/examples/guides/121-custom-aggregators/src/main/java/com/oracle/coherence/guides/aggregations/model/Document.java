/*
 * Copyright (c) 2021 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.guides.aggregations.model;

import java.io.Serializable;

import java.util.Objects;

/**
 * A class to represent a document and its contents.
 */
// tag::class[]
public class Document
        implements Serializable {
    
    private String id;
    private String contents;
// end::class[]
    public Document(String id, String contents) {
        this.id = id;
        this.contents = contents;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getContents() {
        return contents;
    }

    public void setContents(String contents) {
        this.contents = contents;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Document document = (Document) o;
        return Objects.equals(id, document.id) && Objects.equals(contents, document.contents);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, contents);
    }

    @Override
    public String toString() {
        return "Document{" +
               "id='" + id + '\'' +
               ", contents='" + contents + '\'' +
               '}';
    }
}
