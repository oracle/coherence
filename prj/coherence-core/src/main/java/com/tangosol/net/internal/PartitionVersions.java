/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.internal;


import com.tangosol.io.ExternalizableLite;

import com.tangosol.net.partition.PartitionSet;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.HashMap;
import java.util.Map;


/**
 * PartitionVersions represents a collection of (partition, version) pairs.
 *
 * @author rhl 2013.10.02
 */
public class PartitionVersions
        implements ExternalizableLite
    {
    // ----- constructors -------------------------------------------------

    /**
     * Default constructor (needed for serialization).
     */
    public PartitionVersions()
        {}

    /**
     * Construct a PartitionVersions to track the partition ownership versions
     * for the specified partition-set.
     *
     * @param parts  the partition-set
     */
    public PartitionVersions(PartitionSet parts, int[] anVersions)
        {
        if (parts.cardinality() != anVersions.length)
            {
            throw new IllegalArgumentException(
                    "Versions array does not match the partition set");
            }

        Map<Integer, Integer> mapVersions = new HashMap<Integer, Integer>();
        for (int iPart = parts.next(0), iVersion = 0; iPart >= 0; iPart = parts.next(iPart + 1))
            {
            mapVersions.put(iPart, anVersions[iVersion++]);
            }

        m_parts       = new PartitionSet(parts);
        m_mapVersions = mapVersions;
        }

    // ----- accessors ----------------------------------------------------

    /**
     * Return the set of partitions represented by this PartitionVersions.
     *
     * @return the set of partitions represented by this PartitionVersions
     */
    public PartitionSet getPartitions()
        {
        return m_parts;
        }

    /**
     * Return the version for the specified partition
     *
     * @param nPartition  the partition to return the version for, or -1
     *                    if the partition is not represented
     */
    public int getVersion(int nPartition)
        {
        Integer NVersion = m_mapVersions.get(nPartition);
        return NVersion == null ? -1 : NVersion.intValue();
        }


    // ----- Object methods -----------------------------------------------

    /**
     * {@inheritDoc}
     */
    public String toString()
        {
        StringBuilder sb = new StringBuilder();

        sb.append("PartitionVersions{");

        PartitionSet parts = getPartitions();
        for (int iPart = parts.next(0); iPart >= 0; iPart = parts.next(iPart + 1))
            {
            sb.append(iPart).append("=").append(getVersion(iPart)).append(", ");
            }

        sb.append("}");

        return sb.toString();
        }


    // ----- ExternalizableLite methods -----------------------------------

    /**
     * {@inheritDoc}
     */
    public void readExternal(DataInput in)
            throws IOException
        {
        PartitionSet parts = new PartitionSet();
        parts.readExternal(in);
        m_parts = parts;

        readSupplemental(in, parts);
        }

    /**
     * Read the supplemental information (see {@link #writeSupplemental}) for
     * this PartitionVersions from the input-stream.
     *
     * @param in     the input stream
     * @param parts  the partition set to read supplemental versions for
     */
    public void readSupplemental(DataInput in, PartitionSet parts)
            throws IOException
        {
        Map<Integer, Integer> mapVersions = new HashMap<Integer, Integer>();
        for (int iPart = parts.next(0); iPart >= 0; iPart = parts.next(iPart + 1))
            {
            mapVersions.put(iPart, in.readInt());
            }

        m_mapVersions = mapVersions;
        }

    /**
     * {@inheritDoc}
     */
    public void writeExternal(DataOutput out)
            throws IOException
        {
        PartitionSet parts = m_parts;

        parts.writeExternal(out);

        writeSupplemental(out, parts);
        }

    /**
     * Write the supplemental information (see {@link #readSupplemental}) for
     * the specified partition set to the output-stream.
     *
     * @param out    the output stream
     * @param parts  the partition set to write supplemental versions for
     */
    public void writeSupplemental(DataOutput out, PartitionSet parts)
            throws IOException
        {
        Map<Integer, Integer> mapVersions = m_mapVersions;
        for (int iPart = parts.next(0); iPart >= 0; iPart = parts.next(iPart + 1))
            {
            out.writeInt(mapVersions.get(iPart));
            }
        }

    // ----- data members -------------------------------------------------

    /**
     * The set of partitions.
     */
    protected PartitionSet m_parts;

    /**
     * The map of partition versions.
     */
    protected Map<Integer, Integer> m_mapVersions;
    }
