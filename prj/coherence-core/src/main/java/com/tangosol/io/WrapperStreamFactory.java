/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io;


import java.io.InputStream;
import java.io.OutputStream;


/**
* Provides the means to wrap an InputStream and OutputStream, such that
* functionality such as compression and encryption can be implemented
* in a layered, pluggable fashion.
*
* @deprecated As of Coherence 3.7, deprecated with no replacement.
*
* @author cp  2002.08.19
*/
@Deprecated
public interface WrapperStreamFactory
    {
    /**
    * Requests an InputStream that wraps the passed InputStream.
    *
    * @param  stream  the java.io.InputStream to be wrapped
    *
    * @return an InputStream that delegates to ("wraps") the passed
    *         InputStream
    */
    public InputStream getInputStream(InputStream stream);

    /**
    * Requests an OutputStream that wraps the passed OutputStream.
    *
    * @param  stream  the java.io.OutputStream to be wrapped
    *
    * @return an OutputStream that delegates to ("wraps") the passed
    *         OutputStream
    */
    public OutputStream getOutputStream(OutputStream stream);
    }