/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package extend;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.Callable;

/**
 * A custom {@link java.util.concurrent.Callable} to represent
 * if it is possible to connect to a {@link java.net.Socket}.
 *
 * @author bo  2014.11.20
 */
public class Connectable implements Callable<Boolean>
    {
    /**
     * Constructs a {@link Connectable}.
     *
     * @param sAddress
     * @param nPort
     */
    public Connectable(String sAddress, int nPort)
        {
        m_sAddress = sAddress;
        m_nPort    = nPort;
        }

    @Override
    public Boolean call()
        {
        try
            {
            new Socket(m_sAddress, m_nPort).close();

            return true;
            }
        catch (IOException e)
            {
            return false;
            }
        }

    private String m_sAddress;

    private int m_nPort;
    }
