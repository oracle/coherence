/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.internal;


import com.tangosol.io.ExternalizableLite;

import com.tangosol.net.Member;
import com.tangosol.net.PartitionedService;

import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.LongArray;
import com.tangosol.util.SparseArray;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.HashSet;
import java.util.Set;


/**
 * QuorumInfo holds information about the "last good" partition ownership.
 * It's used by the dynamic active persistence policy {@link
 * com.tangosol.net.ConfigurableQuorumPolicy.PartitionedCacheQuorumPolicy}
 *
 * @author gg 2015.11.20
 *
 * @since Coherence 12.2.1.1
 */
public class QuorumInfo
        implements ExternalizableLite
    {
    /**
     * Default constructor (for serialization).
     */
    public QuorumInfo()
        {
        }

    /**
     * Construct the QuorumInfo snapshot for a given service.
     *
     * @param service to collect the quorum data for
     */
    public QuorumInfo(PartitionedService service)
        {
        // clone the member set
        m_setMembers = new HashSet<>(service.getOwnershipEnabledMembers());

        // extract the ownership info
        int cParts = service.getPartitionCount();

        int[] anOwner = new int[cParts];
        for (int iPart = 0; iPart < cParts; iPart++)
            {
            Member member = service.getPartitionOwner(iPart);

            anOwner[iPart] = member == null ? 0 : member.getId();
            }
        m_anOwner = anOwner;

        int[] anVersion = new int[cParts];
        for (int iPart = 0; iPart < cParts; iPart++)
            {
            anVersion[iPart] = service.getOwnershipVersion(iPart);
            }
        m_anVersion = anVersion;
        }


    // ----- accessors ------------------------------------------------------

    /**
     * @return the set of members
     */
    public Set<Member> getMembers()
        {
        return m_setMembers;
        }

    /**
     * @return an array of owners indexed by partition
     */
    public int[] getOwners()
        {
        return m_anOwner;
        }

    /**
     * @return an array of partition versions indexed by partition
     */
    public int[] getVersions()
        {
        return m_anVersion;
        }

    /**
     * @return a long array of Members indexed by the corresponding ids
     */
    public LongArray<Member> getMemberArray()
        {
        LongArray laMembers = new SparseArray<>();
        for (Member member : m_setMembers)
            {
            laMembers.set(member.getId(), member);
            }
        return laMembers;
        }


    // ----- ExternalizableLite ---------------------------------------------

    @Override
    public void readExternal(DataInput in)
            throws IOException
        {
        m_nVersion = ExternalizableHelper.readInt(in);

        // while the following check is unnecessary, it shows the pattern
        // that must be used for all serialization format changes
        if (m_nVersion >= V_12_2_1_1_0)
            {
            m_setMembers = new HashSet<>();
            ExternalizableHelper.readCollection(in, m_setMembers, null);

            m_anOwner   = readIntArray(in);
            m_anVersion = readIntArray(in);
            }
        }

    @Override
    public void writeExternal(DataOutput out)
            throws IOException
        {
        ExternalizableHelper.writeInt(out, m_nVersion);

        // inception version V_12_2_1_1_0
        ExternalizableHelper.writeCollection(out, m_setMembers);

        writeIntArray(out, m_anOwner);
        writeIntArray(out, m_anVersion);
        }


    // ----- helpers --------------------------------------------------------

    /*
     * This belongs to ExternalizableHelper.
     */
    private int[] readIntArray(DataInput in)
            throws IOException
        {
        int c = ExternalizableHelper.readInt(in);

        // JEP-290 - ensure we can allocate this array
        ExternalizableHelper.validateLoadArray(int[].class, c, in);

        return c <= 0
                   ? new int[0]
                   : c < ExternalizableHelper.CHUNK_THRESHOLD >> 2
                       ? readIntArray(in, c)
                       : readLargeIntArray(in, c);
        }

    /**
     * Read an array of the specified number of ints by calling
     * {@link ExternalizableHelper#readInt(DataInput)}.
     *
     * @param in  a DataInput stream to read from
     * @param c   length to read
     *
     * @return an array of ints
     *
     * @throws IOException  if an I/O exception occurs
     * 
     * @since 22.09
     */
    private static int[] readIntArray(DataInput in, int c)
            throws IOException
        {
        int[] ai = new int[c];
        for (int i = 0; i < c; i++)
            {
            ai[i] = ExternalizableHelper.readInt(in);
            }

        return ai;
        }

    /**
     * Read an array of ints with length larger than {@link ExternalizableHelper#CHUNK_THRESHOLD} {@literal >>} 2.
     *
     * @param in       a DataInput stream to read from
     * @param cLength  length to read
     *
     * @return an array of ints
     *
     * @throws IOException  if an I/O exception occurs
     * 
     * @since 22.09
     */
    private static int[] readLargeIntArray(DataInput in, int cLength)
            throws IOException
        {
        int    cBatchMax = ExternalizableHelper.CHUNK_SIZE >> 2;
        int    cBatch    = cLength / cBatchMax + 1;
        int[]  aMerged   = null;
        int    cRead     = 0;
        int    cAllocate = cBatchMax;
        int[]  ai;

        for (int i = 0; i < cBatch && cRead < cLength; i++)
            {
            ai      = readIntArray(in, cAllocate);
            aMerged = ExternalizableHelper.mergeIntArray(aMerged, ai);
            cRead  += ai.length;

            cAllocate = Math.min(cLength - cRead, cBatchMax);
            }

        return aMerged;
        }

    /*
     * This belongs to ExternalizableHelper.
     */
    private void writeIntArray(DataOutput out, int[] ai)
            throws IOException
        {
        int c = ai.length;

        ExternalizableHelper.writeInt(out, c);
        for (int i = 0; i < c; i++)
            {
            ExternalizableHelper.writeInt(out, ai[i]);
            }
        }

    /**
     * Constants representing Coherence version that made changes
     * to this class serialization format.
     *
     * Those constants should be used to make this class "hand-made" evolvable.
     * The rules are:
     * <ul>
     *   <li>no removal of fields is allowed;
     *   <li>new fields could only be serialized after all older fields are;
     *   <li>deserialization of new fields should start with a version check;
     *   <li>new fields should be initialized by default constructor
     * </ul>
     * All new data fields
     */
    private final int V_12_2_1_1_0 = 1;

    /**
     * The current class version.
     */
    private int m_nVersion = V_12_2_1_1_0; // 12.2.1.1.0

    /**
     * The member set.
     */
    private Set<Member> m_setMembers;

    /**
     * An array of partition owner ids.
     */
    private int[] m_anOwner;

    /**
     *  An array of partition version
     */
    private int[] m_anVersion;
    }
