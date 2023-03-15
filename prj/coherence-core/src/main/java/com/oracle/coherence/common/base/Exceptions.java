/*
 * Copyright (c) 2020, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.base;

import java.rmi.RemoteException;

/**
 * Class for providing exception support.
 *
 * @author cp  2000.08.02
 * @since 20.06
 */

public abstract class Exceptions
    {
    // ----- exception support ----------------------------------------------

    /**
     * Convert the passed exception to a RuntimeException if necessary.
     *
     * @param e  any Throwable object
     *
     * @return a RuntimeException
     */
    public static RuntimeException ensureRuntimeException(Throwable e)
        {
        if (e instanceof RuntimeException)
            {
            return (RuntimeException) e;
            }
        else
            {
            return ensureRuntimeException(e, e.getMessage());
            }
        }

    /**
     * Convert the passed exception to a RuntimeException if necessary.
     *
     * @param e  any Throwable object
     * @param s  an additional description
     *
     * @return a RuntimeException
     */
    public static RuntimeException ensureRuntimeException(Throwable e, String s)
        {
        if (e instanceof RuntimeException && s == null)
            {
            return (RuntimeException) e;
            }
        else
            {
            return new RuntimeException(s, e);
            }
        }

    /**
     * Unwind the wrapper (runtime) exception to extract the original
     *
     * @param e  Runtime exception (wrapper)
     *
     * @return an original wrapped exception
     */
    public static Throwable getOriginalException(RuntimeException e)
        {
        Throwable t = e;

        while (true)
            {
            if (t instanceof RuntimeException)
                {
                t = t.getCause();
                }
            else if (t instanceof RemoteException)
                {
                t = ((RemoteException) t).detail;
                }
            // we do not want to have runtime dependency on j2ee classes
            else if (t.getClass().getName().equals("javax.ejb.EJBException"))
                {
                try
                    {
                    t = (Throwable) Classes.invoke(t,
                            "getCausedByException", Classes.VOID);
                    }
                catch (Exception x)
                    {
                    return t;
                    }
                }
            else
                {
                return t;
                }
            }
        }

    /**
     * Re-throw the specified exception if it is a fatal unrecoverable exception.
     *
     * @param t  the exception to check
     */
    public static void throwIfFatal(Throwable t)
        {
        if (t instanceof VirtualMachineError)
            {
            throw (VirtualMachineError)t;
            }
        else if (t instanceof ThreadDeath)
            {
            throw (ThreadDeath)t;
            }
        else if (t instanceof LinkageError)
            {
            throw (LinkageError)t;
            }
        }
    }
