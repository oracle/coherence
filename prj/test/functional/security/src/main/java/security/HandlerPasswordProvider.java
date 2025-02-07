/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package security;


import com.tangosol.net.PasswordProvider;

import com.tangosol.util.Base;
import com.tangosol.util.ClassHelper;
import com.tangosol.util.ExternalizableHelper;

import java.security.Principal;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.ConfirmationCallback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.TextOutputCallback;
import javax.security.auth.callback.UnsupportedCallbackException;


/**
* The HandlerPasswordProvider is a CallbackHandler implementation
* based on a specified username and a password provider.  A file based
* password provider, FileBasedPasswordProvider, is used.
*
* @author lh, jk  2025.02.04
* @since Coherence 14.1.2.0.2
*/
public class HandlerPasswordProvider
        extends    Base
        implements CallbackHandler, Principal
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a SimpleHandler.
     *
     * @param sName   the username
     * @param sClass  the password provider class name
     * @param sParam  the parameter for the password provider
     *
     * @since 14.1.2.0.2
     */
    public HandlerPasswordProvider(String sName, String sClass, String sParam)
        {
        m_sName    = sName;
        m_provider = null;

        try
            {
            Object[] aoParam = new Object[]{sParam};
            Class    clz     = ExternalizableHelper.loadClass(sClass, null, null);

            m_provider = (PasswordProvider) ClassHelper.newInstance(clz, aoParam);
            }
        catch (Exception e)
            {
            }
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
                ((PasswordCallback) callback).setPassword(m_provider.get());
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
        return o instanceof HandlerPasswordProvider ?
            equals(m_sName, ((HandlerPasswordProvider) o).m_sName) : false;
        }

    /**
    * Return a string representation of this principal.
    *
    * @return a string representation of this principal
    */
    public String toString()
        {
        return "HandlerPasswordProvider: " + m_sName;
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
     * password provider.
     */
    private PasswordProvider m_provider;
    }