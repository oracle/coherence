/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.rest.io;

/**
 * Jackson-based marshaller that marshals object to/from JSON.
 *
 * @deprecated  As of 12.2.1.0.0, replaced by {@link JacksonJsonMarshaller}
 *
 * @author as  2011.07.13
 */
@Deprecated
public class JsonJacksonMarshaller<T>
        extends JacksonJsonMarshaller<T>
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Construct JsonJacksonMarshaller instance.
     *
     * @param clzRoot  class of the root object this marshaller is for
     */
    public JsonJacksonMarshaller(Class<T> clzRoot)
        {
        super(clzRoot);
        }
    }
