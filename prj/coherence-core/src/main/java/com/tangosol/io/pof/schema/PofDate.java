/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io.pof.schema;

import com.tangosol.io.pof.DateMode;

/**
 * Schema representation of POF date/time.
 *
 * @author as  2013.11.18
 */
public class PofDate
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Construct {@code PofDate} instance.
     */
    public PofDate()
        {
        }

    /**
     * Construct {@code PofArray} instance.
     *
     * @param mode              date/time mode to use
     * @param fIncludeTimezone  flag specifying whether to include time zone info
     */
    public PofDate(DateMode mode, boolean fIncludeTimezone)
        {
        m_mode = mode;
        m_fIncludeTimezone = fIncludeTimezone;
        }

    // ---- accessors -------------------------------------------------------

    /**
     * Return the date/time mode to use.
     *
      * @return the date/time mode to use
     */
    public DateMode getMode()
        {
        return m_mode;
        }

    /**
     * Set the date/time mode to use.
     *
     * @param mode  the date/time mode to use
     */
    public void setMode(DateMode mode)
        {
        m_mode = mode;
        }

    /**
     * Return whether to include the time zone info.
     *
     * @return whether to include the time zone info
     */
    public boolean isIncludeTimezone()
        {
        return m_fIncludeTimezone;
        }

    /**
     * Set whether to include the time zone info.
     *
     * @param fIncludeTimezone  whether to include the time zone info
     */
    public void setIncludeTimezone(boolean fIncludeTimezone)
        {
        m_fIncludeTimezone = fIncludeTimezone;
        }

    // ---- data members ----------------------------------------------------

    /**
     * The date/time mode to use.
     */
    private DateMode m_mode = DateMode.DATE_TIME;

    /**
     * Flag specifying whether to include the time zone info
     */
    private boolean m_fIncludeTimezone = false;
    }
