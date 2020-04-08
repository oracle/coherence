/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.persistence;

/**
 * A Snapshot is the abstraction of a full or partial snapshot representation
 * for the purposes of archiving.
 *
 * @since 12.2.1
 * @author jh/tam  2014.03.05
 */
public class Snapshot
    {

    // ----- constructors ---------------------------------------------------

    /**
     * Create a new Snapshot with the given name and set of PersistentStore
     * identifiers.
     *
     * @param sName     the name of the snapshot
     * @param asStores  the identifiers of the PersistentStores that comprise
     *                  the snapshot
     */
    public Snapshot(String sName, String[] asStores)
        {
        f_sName    = sName;
        f_asStores = asStores;
        }

    // ----- accessors -------------------------------------------------------

    /**
     * Return the name of the snapshot.
     *
     * @return the name of the snapshot
     */
    public String getName()
        {
        return f_sName;
        }

    /**
     * Return the identifiers of the PersistentStores that comprise the
     * snapshot.
     *
     * @return a list of the store identifiers
     */
    public String[] listStores()
        {
        return f_asStores.clone();
        }

    // ----- attributes -----------------------------------------------------

    /**
     * The name of the snapshot.
     */
    private final String f_sName;

    /**
     * The list of the stores known by this snapshot.
     */
    private final String[] f_asStores;
    }
