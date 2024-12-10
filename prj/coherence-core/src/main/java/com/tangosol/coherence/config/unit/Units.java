/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.unit;

import com.oracle.coherence.common.util.MemorySize;

/**
 * {@link Units} is a union of {@link MemorySize} and unit count.
 *
 * Note: This class is provided to support the high-units configuration property
 * which can either be a memory size string or the number of units.  The default
 * unit calculator depends on whether the configuration explicitly specified
 * a memory size (e.g. &lt;high-units&gt;20M&lt;/high-units&gt;).
 *
 * @author bo  2012.08.27
 * @author pfm 2012.08.27
 * @since Coherence 12.1.2
 */
public class Units
    {
    /**
     * Construct a Units object.  If the sValue is a MemorySize string (e.g. 10M) then
     * this instance of Units explicitly represents a MemorySize.  Otherwise, this instance
     * contains a unit count.
     *
     * @param sValue  the unit count or memory size
     */
    public Units(String sValue)
        {
        // Attempt to parse the string (this will be without the expression
        // stuff by now) into either a value or a memory size.  An exception
        // will be thrown if the value is a memory size.
        try
            {
            m_cUnits = Long.parseLong(sValue);
            m_memorySize = null;
            }
        catch (NumberFormatException e)
            {
            m_memorySize = new MemorySize(sValue);
            m_cUnits = m_memorySize.getByteCount();
            }
        }

    /**
     * Construct a Units object with the given unit count.
     *
     * @param cUnits  the unit count
     */
    public Units(long cUnits)
        {
        m_cUnits = cUnits;
        m_memorySize = null;
        }

    /**
     * Construct a Units object with the given {@link MemorySize}.
     *
     * @param memorySize  the {@link MemorySize}
     */
    public Units(MemorySize memorySize)
        {
        m_memorySize = memorySize;
        m_cUnits = memorySize.getByteCount();
        }

    /**
     * Return the unit count.  If this object was constructed with MemorySize then the
     * count will be the number of bytes.
     *
     * @return the unit count
     */
    public long getUnitCount()
        {
        return m_cUnits;
        }

    /**
     * Return the {@link MemorySize}.  If this object was constructed with a unit count then
     * {@link MemorySize} will be null.
     *
     * @return  the {@link MemorySize} or null
     */
    public MemorySize getMemorySize()
        {
        return m_memorySize;
        }

    /**
     * Return true if Units contains a {@link MemorySize}.
     *
     * @return true if Units contains a {@link MemorySize}
     */
    public boolean isMemorySize()
        {
        return m_memorySize != null;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The number of units.
     */
    private long m_cUnits;

    /**
     * The MemorySize if the units represent memory size.
     */
    private MemorySize m_memorySize;
    }
