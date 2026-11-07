/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.config.unit;

/**
 * A low watermark expressed in units, as a memory size, or as a percentage.
 * Percentage syntax is deliberately confined to low-watermark configuration.
 *
 * @author Aleks Seovic  2026.11.06
 * @since 27.01
 */
public class LowUnits
        extends Units
    {
    /**
     * Construct a low watermark.
     *
     * @param sValue  a unit count, memory size, or percentage
     */
    public LowUnits(String sValue)
        {
        super(sValue.trim().endsWith("%") ? "0" : sValue);

        String s = sValue.trim();
        if (s.endsWith("%"))
            {
            if (!s.matches("[0-9]+(?:\\.[0-9]+)?%"))
                {
                throw new IllegalArgumentException("Invalid low-units percentage: " + sValue);
                }
            m_dflRatio = Double.parseDouble(s.substring(0, s.length() - 1)) / 100.0;
            if (!Double.isFinite(m_dflRatio) || m_dflRatio <= 0.0 || m_dflRatio >= 1.0)
                {
                throw new IllegalArgumentException("Low-units percentage must be strictly between zero and 100: " + sValue);
                }
            }
        }

    /**
     * Return the percentage as a fraction, or a negative value for absolute units.
     *
     * @return the ratio, or a negative value
     */
    public double getRatio()
        {
        return m_dflRatio;
        }

    /**
     * The percentage as a fraction, or a negative value for absolute units.
     */
    private double m_dflRatio = -1.0;
    }
