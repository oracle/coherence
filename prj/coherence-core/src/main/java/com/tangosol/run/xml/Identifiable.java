/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */


package com.tangosol.run.xml;


/**
* An interface for access to a key of a value object
*
* @author gg  12.26.2000
*/
public interface Identifiable
    {
    /**
    * Determine the key for this value object
    *
    * @return the key
    */
    public Key getKey();

    /**
    * Set this value object to match to the specified Key
    *
    * @param key  the specified key
    */
    public void setKey(Key key);
    }
