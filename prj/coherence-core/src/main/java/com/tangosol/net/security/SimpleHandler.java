/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.security;


import com.tangosol.util.Base;

import java.security.Principal;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.ConfirmationCallback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.TextOutputCallback;
import javax.security.auth.callback.UnsupportedCallbackException;


/**
* The SimpleHandler class is a CallbackHandler implementation
* based on a specified user name and password.
*
* @author gg  2004.08.03
* @since Coherence 2.5
*/
public class SimpleHandler
        extends    Base
        implements CallbackHandler, Principal
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a SimpleHandler.
    *
    * @param sName       the user name
    * @param acPassword  the password
    */
    public SimpleHandler(String sName, char[] acPassword)
        {
        this(sName, acPassword, true);
        }

    /**
    * Construct a SimpleHandler.
    *
    * @param sName      the user name
    * @param sPassword  the password
    * @param fDispose   true if the password should be disposed as
    *                   soon as the PasswordCallback has been served
    */
    public SimpleHandler(String sName, String sPassword, boolean fDispose)
        {
        this(sName, sPassword.toCharArray(), fDispose);
        }

    /**
    * Construct a SimpleHandler.
    *
    * @param sName       the user name
    * @param acPassword  the password
    * @param fDispose    true if the password should be disposed as
    *                    soon as the PasswordCallback has been served
    */
    public SimpleHandler(String sName, char[] acPassword, boolean fDispose)
        {
        m_sName    = sName;
        m_acPwd    = acPassword;
        m_fDispose = fDispose;
        }


    // ----- CallbackHandler interface --------------------------------------

    /**
    * Invoke an array of Callbacks.  This implementation processes only the
    * NameCallback and PasswordCallback types. It completely ignores the
    * TextOutputCallback and ConfirmationCallback and throws the
    * UnsupportedCallbackException for any other type.
    *
    * @param aCallback  an array of <code>Callback</code> objects which contains
    *                   the information requested by an underlying security
    *                   service to be retrieved or displayed.
    *
    * @exception UnsupportedCallbackException if the implementation of this
    *           method does not support one or more of the Callbacks
    *           specified in the <code>callbacks</code> parameter.
    */
    public void handle(Callback[] aCallback)
            throws UnsupportedCallbackException
        {
        for (int i = 0, c = aCallback.length; i < c; i++)
            {
            Callback callback = aCallback[i];

            if (callback instanceof NameCallback)
                {
                ((NameCallback) callback).setName(m_sName);
                }
            else if (callback instanceof PasswordCallback)
                {
                ((PasswordCallback) callback).setPassword(m_acPwd);
                if (m_fDispose)
                    {
                    m_acPwd = null;
                    }
                }
            else if (callback instanceof TextOutputCallback ||
                     callback instanceof ConfirmationCallback)
                {
                // ignore
                }
            else
                {
                throw new UnsupportedCallbackException(callback,
                    "Unsupported Callback type");
                }
            }
        }


    // ----- Principal interface --------------------------------------------

    /**
    * Compare this principal to the specified object.  Returns true
    * if the object passed in matches the principal represented by
    * the implementation of this interface.
    *
    * @param o  principal to compare with
    *
    * @return true if the principal passed in is the same as that
    *         encapsulated by this principal; false otherwise
    */
    public boolean equals(Object o)
        {
        return o instanceof SimpleHandler ?
            equals(m_sName, ((SimpleHandler) o).m_sName) : false;
        }

    /**
    * Return a string representation of this principal.
    *
    * @return a string representation of this principal
    */
    public String toString()
        {
        return "SimpleHandler: " + m_sName;
        }

    /**
    * Return a hashcode for this principal.
    *
    * @return a hashcode for this principal
    */
    public int hashCode()
        {
        return m_sName.hashCode();
        }

    /**
    * Return the name of this principal.
    *
    * @return the name of this principal
    */
    public String getName()
        {
        return m_sName;
        }


    // ----- data fields ----------------------------------------------------

    /**
    * User name.
    */
    private String m_sName;

    /**
    * Password.
    */
    private char[] m_acPwd;

    /**
    * Dispose flag.
    */
    private boolean m_fDispose;
    }