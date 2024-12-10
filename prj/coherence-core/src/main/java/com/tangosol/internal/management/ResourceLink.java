/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.management;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Describes a link from one resource to another.
 *
 * @author sr  2017.08.21
 * @since 12.2.1.4.0
 */
public class ResourceLink
    {
    // ----- constructors -----------------------------------------------------------------

    /**
     * Construct a ResourceLink instance.
     * @param sRel     the relationship of the link
     * @param uriLink  the link URI
     */
    public ResourceLink(String sRel, URI uriLink)
        {
        this.f_sRel = sRel;
        this.f_sUri = uriLink;
        }

    // ----- ResourceLink methods ----------------------------------------------------------

    /**
     * The relationship of the link
     *
     * @return the relationship of the link
     */
    public String getRelationship()
        {
        return this.f_sRel;
        }

    /**
     * Convert the link to a JSON object.
     *
     * @return  the JSON object
     */
    public Map<String, Object> toJson()
        {
        Map<String, Object> mapLink = new LinkedHashMap<>();
        mapLink.put(PROP_LINK_REL, getRelationship());
        mapLink.put(PROP_LINK_HREF, f_sUri.toASCIIString());
        return mapLink;
        }

    // ----- data members ------------------------------------------------------

    /**
     * The relationship of the link. Relationship is specified in terms
     * of "parent", "self", "canonical", or child links such as
     * "members"(in case of Cluster resource)
     */
    protected final String f_sRel;

    /**
     * The URI of the link
     */
    protected final URI f_sUri;

    // ----- constants ------------------------------------------------------

    /**
     * The relationship of the link.
     */
    public static final String PROP_LINK_REL   = "rel";

    /**
     * The property name of the link.
     */
    public static final String PROP_LINK_HREF = "href";
    }
