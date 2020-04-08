/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */


package com.tangosol.run.xml;


/**
* An interface for URI serialization.
*
* @see <a href="http://www.ietf.org/rfc/rfc2396.txt">http://www.ietf.org/rfc/rfc2396.txt</a>
*
* @author cp  2000.11.10
*/
public interface UriSerializable
    {
    /**
    * Serialize the object into a URI.
    *
    * @return a String containing a URI-serialized form of the object
    */
    public String toUri();

    /**
    * Deserialize the object from a URI String.
    *
    * This method can throw one of several RuntimeExceptions.
    *
    * @param sUri  a String containing a URI-serialized form of the object
    *
    * @throws UnsupportedOperationException  if the operation is not supported
    * @throws IllegalStateException          if this is not an appropriate state
    * @throws IllegalArgumentException       if there is an illegal argument
    */
    public void fromUri(String sUri);


    // ----- constants for URI serialization --------------------------------

    /**
    * Delimiter between multiple fields in a URI.
    */
    public static final char URI_DELIM = '-';

    /**
    * Token to signify a null field in a URI.
    */
    public static final char URI_NULL  = '!';
    }
