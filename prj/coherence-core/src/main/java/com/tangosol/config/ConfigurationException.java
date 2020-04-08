/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.config;

/**
 * A {@link ConfigurationException} captures information concerning an invalid configuration of Coherence.
 * Specifically it details what the problem was and advice for resolving the issue.  Optionally
 * a {@link ConfigurationException} may be include the causing {@link Exception}.
 *
 * @author bo  2011.06.15
 * @since Coherence 12.1.2
 */
@SuppressWarnings("serial")
public class ConfigurationException
        extends RuntimeException
    {
    // ----- constructors -------------------------------------------------

    /**
     * Constructs a {@link ConfigurationException}.
     *
     * @param sProblem  the problem that occurred
     * @param sAdvice   the advice to fix the problem
     */
    public ConfigurationException(String sProblem, String sAdvice)
        {
        m_sProblem = sProblem;
        m_sAdvice  = sAdvice;
        }

    /**
     * Constructs a {@link ConfigurationException} (with a cause).
     *
     * @param sProblem  the problem that occurred
     * @param sAdvice   the advice to fix the problem
     * @param cause     the {@link Throwable} causing the problem
     */
    public ConfigurationException(String sProblem, String sAdvice, Throwable cause)
        {
        super(cause);

        m_sProblem = sProblem;
        m_sAdvice  = sAdvice;
        }

    // ----- Exception interface --------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMessage()
        {
        String sResult = "";

        sResult += "Configuration Exception\n";
        sResult += "-----------------------\n";
        sResult += "Problem   : " + getProblem() + "\n";
        sResult += "Advice    : " + getAdvice() + "\n";

        if (getCause() != null)
            {
            sResult += "Caused By : " + getCause().toString() + "\n";
            }

        return sResult;
        }

    // ----- ConfigurationException methods ---------------------------------

    /**
     * Returns what the problem was.
     *
     * @return A string detailing the problem
     */
    public String getProblem()
        {
        return m_sProblem;
        }

    /**
     * Returns advice to resolve the issue.
     *
     * @return A string detailing advice to resolve the issue
     */
    public String getAdvice()
        {
        return m_sAdvice;
        }

    // ----- data members ---------------------------------------------------

    /**
     * Advice for resolving the issue.
     */
    private String m_sAdvice;

    /**
     * The problem that occurred.
     */
    private String m_sProblem;
    }
