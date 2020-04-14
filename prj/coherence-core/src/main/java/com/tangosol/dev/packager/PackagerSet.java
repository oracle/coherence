/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.dev.packager;


import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Serializable;

import java.util.Collection;
import java.util.Hashtable;
import java.util.Properties;


/**
* A PackagerSet provides an interface for collecting and materializing
* a collection of resources for a Packager.
*/
public abstract class PackagerSet
        extends ResourceSet
    {
    /**
    * Construct a PackagerSet.
    */
    protected PackagerSet()
        {
        }

    /**
    * Returns the map of attribute names to attribute values
    * to be used by the PackagerSet.
    */
    public Properties getAttributes()
        {
        return(attributes);
        }

    /**
    * Sets the map of attribute names to attribute values
    * to be used by the PackagerSet.
    */
    public void setAttributes(Properties attributes)
        {
        this.attributes = attributes;
        }

    /**
    * Indicate whether or not an entry with the specified path is a member
    * or has been recorded as a member of the PackagerSet.
    */
    public boolean containsKey(PackagerPath path)
        {
        String pathName = path.getPathName();
        return(packagerEntries.get(pathName) != null);
        }

    /**
    * Return the member PackagerEntry with the specified path, or null
    * if there is no member of the PackagerSet with that path.
    */
    public PackagerEntry getPackagerEntry(PackagerPath path)
        {
        String pathName = path.getPathName();
        return((PackagerEntry)packagerEntries.get(pathName));
        }

    /**
    * Record the PackagerEntry locally, but don't do anything more than
    * that until the PackagerSet is materialized.
    */
    public void recordEntry(PackagerEntry entry)
        {
        String pathName = entry.getPath().getPathName();
        packagerEntries.put(pathName, entry);
        }

    /**
    * Return the collected PackagerEntries as a Collection.
    */
    public Collection getCollectedEntries()
        {
        return(packagerEntries.values());
        }

    /**
    * Materialize the PackagerSet from its specified entrys in the context
    * of the specified ClassLoader.
    */
    public abstract void materialize(ClassLoader classLoader)
            throws IOException, UnexpectedPackagerException;


    // ----- data members ---------------------------------------------------

    private Hashtable       packagerEntries  = new Hashtable();
    private Properties      attributes       = new Properties();
    }
