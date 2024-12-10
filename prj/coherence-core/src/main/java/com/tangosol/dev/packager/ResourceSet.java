/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.dev.packager;


import com.tangosol.util.Base;

import java.io.Serializable;


/**
* A ResourceSet is a logical collection of resources, each of which can
* be identified by a PackagerPath.
*/
public abstract class ResourceSet
        extends    Base
        implements Serializable
    {
    /**
    * Indicate whether the ResourceSet contains a resource identified by
    * the specified path.
    */
    public abstract boolean containsKey(PackagerPath path);
    }
