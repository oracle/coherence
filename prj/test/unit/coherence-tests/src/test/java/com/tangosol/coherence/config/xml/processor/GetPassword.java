/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.xml.processor;

import com.tangosol.net.PasswordProvider;

/*
 * Class implementing {@PasswordProvider}.get() method to fetch the password from underlying-source, in this case
 * its a simple class storing password as String but it could be DB/File/Password stores etc, on request.
 * Called from {@link PasswordProviderBuilderProcessor}s instance(s)
 *
 * @author spuneet
 * @since Coherence 12.2.1.4
 */
public class GetPassword implements PasswordProvider
    {
    // No arg constructor
    public GetPassword()
        {
        this (new String[] {});
        }

    // Single arg constructor
    public GetPassword(String arr)
        {
        this (new String[] {arr});
        }

    // 2 - arg constructor
    public GetPassword(String s1, String s2)
        {
        this (new String[] {s1, s2});
        }

    // 3 - arg constructor
    public GetPassword(String s1, String s2, String s3)
        {
        this (new String[] {s1, s2, s3});
        }

    /*
     * Private constructor to do the real processing with the input args.
     * Here we are simply appending all the args sequentially as a string to form the real password string.
     */
    private GetPassword(String[] arr )
        {
        m_sPassword = arr;

        for (String s: m_sPassword)
            m_sbufPassword.append(s);
        }

    @Override
    public char[] get()
        {
        return m_sbufPassword.toString().toCharArray();
        }

    private String[] m_sPassword;
    private StringBuffer m_sbufPassword = new StringBuffer();
    }