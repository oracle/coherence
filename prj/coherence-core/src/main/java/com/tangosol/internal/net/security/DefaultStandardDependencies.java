/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.security;

import com.tangosol.net.security.AccessController;

import javax.security.auth.callback.CallbackHandler;

/**
 * DefaultStandardDependencies is a default implementation of
 * StandardDependencies.
 *
 * @author der  2011.07.10
 * @since Coherence 12.1.2
 */
public class DefaultStandardDependencies
        extends DefaultSecurityDependencies
        implements StandardDependencies
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a {@link DefaultStandardDependencies} object.
     */
    public DefaultStandardDependencies()
        {
        this(null);
        }

    /**
     * Construct a {@link DefaultStandardDependencies} object, copying the
     * values from the specified DefaultStandardDependencies object.
     *
     * @param deps  the dependencies to copy, or null
     */
    public DefaultStandardDependencies(StandardDependencies deps)
        {
        super(deps);

        if (deps != null)
            {
            m_accessController = deps.getAccessController();
            m_callbackHandler  = deps.getCallbackHandler();
            m_sLoginModuleName = deps.getLoginModuleName();
            }
        }

    // ----- accessors ------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public AccessController getAccessController()
        {
        return m_accessController;
        }

    /**
     * Set the security AccessController.
     *
     * @param accessController  the access controller
     *
     * @return this object
     */
    public DefaultSecurityDependencies setAccessController(AccessController accessController)
        {
        m_accessController = accessController;
        return this;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public CallbackHandler getCallbackHandler()
        {
        return m_callbackHandler;
        }

    /**
     * Set the security callback handler.
     *
     * @param callbackHandler  the security callback handler
     *
     * @return this object
     */
    public DefaultSecurityDependencies setCallbackHandler(CallbackHandler callbackHandler)
        {
        m_callbackHandler = callbackHandler;
        return this;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getLoginModuleName()
        {
        return m_sLoginModuleName;
        }

    /**
     * Set the login module name.
     *
     * @param sLoginModuleName  the login module name
     *
     * @return this object
     */
    public DefaultStandardDependencies setLoginModuleName(String sLoginModuleName)
        {
        m_sLoginModuleName = sLoginModuleName;
        return this;
        }

    // ----- DefaultStandardDependencies methods ----------------------------

    /**
     * Validate the supplied dependencies.
     *
     * @return this object
     */
    public DefaultStandardDependencies validate()
        {
        super.validate();

        return this;
        }

    // ----- Object methods -------------------------------------------------

    /**
     * Return a human-readable String representation of the
     * DefaultStandardDependencies.
     *
     * @return a String describing the DefaultStandardDependencies
     */
    @Override
    public String toString()
        {
        return "DefaultStandardDependencies{" + getDescription() + '}';
        }

    /**
     * Format the DefaultStandardDependencies attributes into a String for
     * inclusion in the String returned from the {@link #toString} method.
     *
     * @return a String listing the attributes of the
     *         DefaultStandardDependencies
     */
    protected String getDescription()
        {
        String sAccessControllerDesc = m_accessController == null ? "null" : m_accessController.toString();
        String sCallbackHandlerDesc = m_callbackHandler == null ? "null" : m_callbackHandler.toString();
        return super.getDescription()
          + ",   AccessController=" + sAccessControllerDesc
          + ",   CallbackHandler="  + sCallbackHandlerDesc
          + ",   LoginModuleName="  + m_sLoginModuleName;
        }

    // ----- constants and data members -------------------------------------

    /**
     * The default value of the login module name.
     */
    public static final String DEFAULT_LOGIN_MODULE_NAME = "Coherence";

    /**
     * The security accessController.
     */
    private AccessController m_accessController;

    /**
     * The default CallbackHandler.
     */
    private CallbackHandler m_callbackHandler;

    /**
     * The login module name.
     * */
    private String m_sLoginModuleName = DEFAULT_LOGIN_MODULE_NAME;
    }
