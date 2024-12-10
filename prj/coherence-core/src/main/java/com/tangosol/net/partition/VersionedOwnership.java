/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.partition;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import javax.json.bind.annotation.JsonbProperty;

/**
 * An extension of the partition Ownership object which also carries a change
 * version.  Ownership versions are used to track changes in the primary ownership
 * rather than the contents of a partition.
 * <p>
 * <b>Note: </b> A change in the primary ownership necessitates an increment
 * to the partition version, however a change to the backup ownership does not.
 * Thus {@code (1,2) -> (3,2)} does require a version increment but
 * {@code (1,2) -> (1,3)} does not.
 *
 * @since Coherence 12.1.3
 * @author rhl 2013.08.19
 */
public class VersionedOwnership
        extends Ownership
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor (needed for serialization).
     */
    public VersionedOwnership()
        {
        }

    /**
     * Constructs a VersionedOwnership with the provided backup count and version.
     *
     * @param cBackups  the number of backups
     * @param nVersion  the version of this VersionedOwnership
     */
    public VersionedOwnership(int cBackups, int nVersion)
        {
        super(cBackups);

        m_nVersion = nVersion;
        }

    /**
     * Copy constructor.
     *
     * @param that  the VersionedOwnership to copy from
     */
    public VersionedOwnership(VersionedOwnership that)
        {
        super(that);

        m_nVersion = that.m_nVersion;
        }


    // ----- accessors ------------------------------------------------------

    /**
     * Return the ownership version.
     *
     * @return the ownership version
     */
    public int getVersion()
        {
        return m_nVersion;
        }

    /**
     * Set the new ownership version.
     *
     * @param nVersion  the ownership version
     */
    public void setVersion(int nVersion)
        {
        m_nVersion = nVersion;
        }

    /**
     * {@inheritDoc}
     */
    public String getDescription()
        {
        return super.getDescription() + ", Version=" + m_nVersion;
        }

    // ----- Cloneable methods ----------------------------------------------

    /**
     * Create a clone of this Ownership.
     *
     * @return a clone of the Ownership object
     */
    public Object clone()
        {
        VersionedOwnership that = (VersionedOwnership) super.clone();

        that.m_nVersion = this.m_nVersion;

        return that;
        }

    // ----- Object methods -------------------------------------------------

    /**
     * {@inheritDoc}
     */
    public boolean equals(Object o)
        {
        if (o instanceof VersionedOwnership)
            {
            VersionedOwnership that = (VersionedOwnership) o;

            return super.equals(that) && that.m_nVersion == m_nVersion;
            }

        return false;
        }

    // ----- ExternalizableLite methods -------------------------------------

    /**
     * {@inheritDoc}
     */
    public void readExternal(DataInput in) throws IOException
        {
        super.readExternal(in);

        m_nVersion = in.readInt();
        }

    /**
     * {@inheritDoc}
     */
    public void writeExternal(DataOutput out) throws IOException
        {
        super.writeExternal(out);

        out.writeInt(m_nVersion);
        }

    // ----- PortableObject methods -----------------------------------------

    /**
     * {@inheritDoc}
     */
    public void readExternal(PofReader in) throws IOException
        {
        super.readExternal(in);

        m_nVersion = in.readInt(5);
        }

    /**
     * {@inheritDoc}
     */
    public void writeExternal(PofWriter out) throws IOException
        {
        super.writeExternal(out);

        out.writeInt(5, m_nVersion);
        }

    // ----- data members ---------------------------------------------------

    /**
     * The ownership version.
     */
    @JsonbProperty("version")
    protected int m_nVersion;
    }
