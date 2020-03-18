/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.rest.io;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;


/**
 * An interface that must be implemented by REST marshallers.
 *
 * @author as  2011.07.10
 */
public interface Marshaller<T>
    {
    /**
     * Write the specified object into the given stream.
     *
     * @param value        object to marshall
     * @param out          the {@link OutputStream} for the HTTP entity. The
     *                     implementation should not close the output stream.
     * @param httpHeaders  a mutable map of the HTTP message headers.
     *
     * @throws IOException  if an error occurs during marshalling
     */
    public void marshal(T value, OutputStream out, MultivaluedMap<String, Object> httpHeaders)
            throws IOException;

    /**
     * Write the specified object into the given stream as a fragment.
     * <p>
     * This method is called when marshalling collections of objects, as
     * marshalling behavior might differ when an object is serialized
     * directly or as element of a collection.
     * <p>
     * For example, when marshalling a collection of objects into XML, you
     * need to ensure that the XML declaration is emmitted only once, not for
     * each object in the collection.
     * <p>
     * In cases where there is no difference in output between fragments and
     * full objects (JSON, for example), this method could simply delegate
     * to {@link #marshal(Object, OutputStream, MultivaluedMap)}.
     *
     * @param value        object to marshall
     * @param out          the {@link OutputStream} for the HTTP entity. The
     *                     implementation should not close the output stream.
     * @param httpHeaders  a mutable map of the HTTP message headers.
     *
     * @throws IOException  if an error occurs during marshalling
     */
    public default void marshalAsFragment(T value, OutputStream out, MultivaluedMap<String, Object> httpHeaders)
            throws IOException
        {
        marshal(value, out, httpHeaders);
        }
    
    /**
     * Read an object from the specified stream.
     *
     * @param in         stream to read from
     * @param mediaType  the media type of the object to read
     *
     * @return unmarshalled object instance
     *
     * @throws IOException  if an error occurs during unmarshalling
     */
    public T unmarshal(InputStream in, MediaType mediaType)
            throws IOException;

    // ----- constants ------------------------------------------------------

    /**
     * The name of the system property that is used to determine whether the
     * marshaller output should be formatted for human readability.
     */
    public static final String FORMAT_OUTPUT = "coherence.rest.format-output";
    }
