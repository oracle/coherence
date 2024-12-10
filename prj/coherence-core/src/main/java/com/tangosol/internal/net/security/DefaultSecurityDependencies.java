/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.security;

import com.tangosol.net.security.IdentityAsserter;
import com.tangosol.net.security.IdentityTransformer;
import com.tangosol.net.security.Authorizer;

/**
 * DefaultSecurityDependencies is a default implementation of
 * SecurityDependencies.
 *
 * @author der  2011.07.10
 * @since Coherence 12.1.2
 */
public class DefaultSecurityDependencies
        implements SecurityDependencies
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a {@link DefaultSecurityDependencies} object.
     */
    public DefaultSecurityDependencies()
        {
        this(null);
        }

    /**
     * Construct a {@link DefaultSecurityDependencies} object, copying the
     * values from the specified DefaultSecurityDependencies object.
     *
     * @param deps  the dependencies to copy, or null
     */
    public DefaultSecurityDependencies(SecurityDependencies deps)
        {
        if (deps != null)
            {
            m_fEnabled            = deps.isEnabled();
            m_fSubjectScoped      = deps.isSubjectScoped();
            m_identityAsserter    = deps.getIdentityAsserter();
            m_identityTransformer = deps.getIdentityTransformer();
            m_authorizer          = deps.getAuthorizer();
            m_sModel              = deps.getModel();
            }
        }

    // ----- accessors ------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEnabled()
        {
        return m_fEnabled;
        }

    /**
     * Set a flag indicating whether security is enabled.
     *
     * @param fEnabled  a flag indicating whether security is enabled
     *
     * @return this object
     */
    public DefaultSecurityDependencies setEnabled(boolean fEnabled)
        {
        m_fEnabled = fEnabled;
        return this;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSubjectScoped()
        {
        return m_fSubjectScoped;
        }

    /**
     * Set a flag indicating if the security configuration specifies subject
     * scoping.
     *
     * @param fSubjectScoped  a flag indicating if the security configuration
     * specifies subject scoping
     *
     * @return this object
     */
    public DefaultSecurityDependencies setSubjectScoped(boolean fSubjectScoped)
        {
        m_fSubjectScoped = fSubjectScoped;
        return this;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public IdentityAsserter getIdentityAsserter()
        {
        return m_identityAsserter;
        }

    /**
     * Set the identity asserter.
     *
     * @param asserter  the identity asserter.
     *
     * @return this object
     */
    public DefaultSecurityDependencies setIdentityAsserter(IdentityAsserter asserter)
        {
        m_identityAsserter = asserter;
        return this;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public IdentityTransformer getIdentityTransformer()
        {
        return m_identityTransformer;
        }

    /**
     * Set the identity transformer.
     *
     * @param transformer  the identity transformer.
     *
     * @return this object
     */
    public DefaultSecurityDependencies setIdentityTransformer(IdentityTransformer transformer)
        {
        m_identityTransformer = transformer;
        return this;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public Authorizer getAuthorizer()
        {
        return m_authorizer;
        }

    /**
     * Set the subject authorizer.
     *
     * @param authorizer  the subject authorizer.
     *
     * @return this object
     */
    public DefaultSecurityDependencies setAuthorizer(Authorizer authorizer)
        {
        m_authorizer = authorizer;
        return this;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getModel()
        {
        return m_sModel;
        }

    /**
     * Set the security model.
     *
     * @param sModel  the security model
     *
     * @return this object
     */
    protected DefaultSecurityDependencies setModel(String sModel)
        {
        m_sModel = sModel;
        return this;
        }

    // ----- DefaultSecurityDependencies methods ----------------------------

    /**
     * Validate the supplied dependencies.
     *
     * @return this object
     */
    public DefaultSecurityDependencies validate()
        {
        return this;
        }

    // ----- Object methods -------------------------------------------------

    /**
     * Return a human-readable String representation of the
     * DefaultSecurityDependencies.
     *
     * @return a String describing the DefaultSecurityDependencies
     */
    @Override
    public String toString()
        {
        return "DefaultSecurityDependencies{" + getDescription() + '}';
        }

    /**
     * Format the DefaultSecurityDependencies attributes into a String for
     * inclusion in the String returned from the {@link #toString} method.
     *
     * @return a String listing the attributes of the
     *         DefaultSecurityDependencies
     */
    protected String getDescription()
        {
        return "Enabled=" + isEnabled()
           + ", IdentityAssert=" + getIdentityAsserter()
           + ", IdentityTransformer=" + getIdentityTransformer()
           + ", Authorizer=" + getAuthorizer()
           + ", Model=" + getModel()
           + ", SubjectScoped=" + isSubjectScoped();
        }

    // ----- constants and data members -------------------------------------

    /**
     * The default value indicating if the security is enabled.
     */
    public static final boolean DEFAULT_ENABLED = false;

    /**
     * The default value for the security model.
     */
    public static final String DEFAULT_MODEL = "Standard";

    /**
     * The default value indicating if security configuration specifies
     * subject scoping.
     */
    public static final boolean DEFAULT_SUBJECT_SCOPED = false;

    /**
     * Validates a token in order to establish a user's identity.
     */
    private IdentityAsserter m_identityAsserter;

    /**
     * Transforms a Subject to a token that asserts identity.
     */
    private IdentityTransformer m_identityTransformer;

    /**
     * Supplies a Subject from an environment that may not
     * support the standard Java security context.
     */
    private Authorizer m_authorizer;

    /**
     * A flag indicating if security is enabled.
     */
    private boolean m_fEnabled = DEFAULT_ENABLED;

    /**
     * A flag indicating if the security configuration specifies subject
     * scoping.
     */
    private boolean m_fSubjectScoped = DEFAULT_SUBJECT_SCOPED;

    /**
     * The security model.
     */
    private String m_sModel = DEFAULT_MODEL;
    }
