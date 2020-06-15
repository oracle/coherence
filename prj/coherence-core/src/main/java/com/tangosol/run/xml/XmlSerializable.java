/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */


package com.tangosol.run.xml;


/**
* An interface for XML serialization.
*
* @author cp  2000.10.22
*/
public interface XmlSerializable
    {
    /**
    * Serialize the object into an XmlElement.
    *
    * @return an XmlElement that contains the serialized form of the object
    */
    public XmlElement toXml();

    /**
    * Deserialize the object from an XmlElement.
    *
    * This method can throw one of several RuntimeExceptions.
    *
    * @param xml  an XmlElement that contains the serialized form of the
    *             object
    *
    * @throws UnsupportedOperationException  if the operation is not supported
    * @throws IllegalStateException          if this is not an appropriate state
    * @throws IllegalArgumentException       if there is an illegal argument
    */
    public void fromXml(XmlElement xml);
    }
