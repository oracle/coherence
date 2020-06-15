/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.security;

/**
 * Security token containing a username and password.
 *
 * @author as  2011.12.23
 */
public class UsernameAndPassword
    {

    // ---- constructors ----------------------------------------------------

    /**
     * Construct a new UsernameAndPassword instance.
     *
     * @param sUsername  the username
     * @param sPassword  the password
     */
    public UsernameAndPassword(String sUsername, String sPassword)
        {
        this(sUsername, sPassword == null ? null : sPassword.toCharArray());
        }

    /**
     * Construct a new UsernameAndPassword instance.
     *
     * @param sUsername   the username
     * @param acPassword  the password
     */
    public UsernameAndPassword(String sUsername, char[] acPassword)
        {
        m_sUsername  = sUsername;
        m_acPassword = acPassword;
        }

    // ---- accessors -------------------------------------------------------

    /**
     * Return the username.
     *
     * @return the username
     */
    public String getUsername()
        {
        return m_sUsername;
        }

    /**
     * Return the password.
     *
     * @return the password
     */
    public char[] getPassword()
        {
        return m_acPassword;
        }

    // ---- data members ----------------------------------------------------

    /**
     * The username.
     */
    private final String m_sUsername;

    /**
     * The password.
     */
    private final char[] m_acPassword;
    }
