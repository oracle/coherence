/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io;


/**
* The SerializerAware interface provides the ability to configure a
* Serializer to be used by the implementing object when serializing or
* deserializing objects.
*
* @author jh  2008.03.25
*
* @since Coherence 3.4
*/
public interface SerializerAware
    {
    /**
    * Retrieve the context Serializer for this object. The context Serializer
    * is provided by the creator of the object for use by the object when
    * serializing or deserializing objects.
    *
    * @return the context Serializer for this object
    */
    public Serializer getContextSerializer();

    /**
    * Specify the context Serializer for this object. The context Serializer
    * can be set when the object is created, and allows the creator to
    * provide the appropriate Serializer to be used by the object when
    * serializing or deserializing objects.
    *
    * @param serializer  the context Serializer for this object
    */
    public void setContextSerializer(Serializer serializer);
    }