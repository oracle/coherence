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

import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;

/**
 * LegacyXmlSecurityHelper parses the {@code <security-config>} XML to
 * populate the DefaultSecurityDependencies.
 *
 * NOTE: This code will eventually be replaced by CODI.
 *
 * @author der  2011.12.01
 * @since Coherence 12.1.2
 */
@SuppressWarnings("deprecation")
public class LegacyXmlSecurityHelper
    {
    /**
     * Populate the DefaultSecurityDependencies object from the XML
     * configuration.
     *
     * @param xml   the <{@code <security-config>} XML element
     * @param deps  the DefaultSecurityDependencies to be populated
     *
     * @return the DefaultSecurityDependencies object that was passed in.
     */
    public static DefaultSecurityDependencies fromXml(XmlElement xml, DefaultSecurityDependencies deps)
        {
        deps.setEnabled(xml.getSafeElement("enabled").getBoolean(deps.isEnabled()));
        deps.setSubjectScoped(xml.getSafeElement("subject-scope").getBoolean(deps.isSubjectScoped()));
        deps.setModel(xml.getSafeElement("model").getString(deps.getModel()));

        XmlElement xmlIdentityAsserter    = xml.findElement("identity-asserter");
        XmlElement xmlIdentityTransformer = xml.findElement("identity-transformer");
        XmlElement xmlAuthorizer          = xml.findElement("authorizer");

        if (xmlIdentityAsserter != null)
            {
            deps.setIdentityAsserter((IdentityAsserter) XmlHelper.createInstance(
                    xmlIdentityAsserter, null, null));
            }

        if (xmlIdentityTransformer != null)
            {
            deps.setIdentityTransformer((IdentityTransformer) XmlHelper.createInstance(
                    xmlIdentityTransformer, null, null));
            }

        if (xmlAuthorizer != null)
            {
            deps.setAuthorizer((Authorizer) XmlHelper.createInstance(
                    xmlAuthorizer, null, null));
            }

        return deps;
        }
    }
