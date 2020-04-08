/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util.extractor;


import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;


/**
* The EntryExtractor is a base abstract class for special purpose custom
* ValueExtractor implementations. It allows them to extract a desired value
* using all available information on the corresponding Map.Entry object and
* is intended to be used in advanced custom scenarios, when application code
* needs to look at both key and value at the same time or can make some very
* specific assumptions regarding to the implementation details of the underlying
* Entry object. As of Coherence 3.5, the same behavior can be achieved by
* subclasses of the AbstractExtractor by overriding the
* {@link AbstractExtractor#extractFromEntry(java.util.Map.Entry)
* AbstractExtractor.extractFromEntry()}.
*
* @author gg 2008.04.14
* @since Coherence 3.4
*/
public abstract class EntryExtractor
        extends AbstractExtractor
        implements PortableObject, ExternalizableLite
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor (for backward compatibility).
    */
    public EntryExtractor()
        {
        this(VALUE);
        }

    /**
    * Construct an EntryExtractor based on the entry extraction target.
    *
    * @param nTarget  one of the {@link #VALUE} or {@link #KEY} values
    *
    * @since Coherence 3.5
    */
    public EntryExtractor(int nTarget)
        {
        m_nTarget = nTarget;
        }


    // ----- ExternalizableLite interface -----------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(DataInput in)
            throws IOException
        {
        m_nTarget = readInt(in);
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(DataOutput out)
            throws IOException
        {
        writeInt(out, m_nTarget);
        }


    // ----- PortableObject interface ---------------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(PofReader in)
            throws IOException
        {
        m_nTarget = in.readInt(0);
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(PofWriter out)
            throws IOException
        {
        out.writeInt(0, m_nTarget);
        }
    }