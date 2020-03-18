/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.security;

import com.tangosol.net.security.AccessController;

import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;

import com.tangosol.util.Base;
import com.tangosol.util.ClassHelper;
import com.tangosol.util.ExternalizableHelper;

import javax.security.auth.callback.CallbackHandler;

/**
 * LegacyXmlStandardHelper parses the {@code <security-config>} XML to
 * populate the DefaultStandardDependencies.
 *
 * NOTE: This code will eventually be replaced by CODI.
 *
 * @author der  2011.12.01
 * @since Coherence 12.1.2
 */
@SuppressWarnings("deprecation")
public class LegacyXmlStandardHelper
    {
    /**
     * Populate the DefaultStandardDependencies object from the XML
     * configuration.
     *
     * @param xml   the <{@code <security-config>} XML element
     * @param deps  the DefaultStandardDependencies to be populated
     *
     * @return the DefaultStandardDependencies object that was passed in.
     */
    public static DefaultStandardDependencies fromXml(XmlElement xml, DefaultStandardDependencies deps)
        {
        LegacyXmlSecurityHelper.fromXml(xml, deps);

        if (deps.isEnabled())
            {
            XmlElement xmlAC   = xml.getSafeElement("access-controller");
            XmlElement xmlCH   = xml.getSafeElement("callback-handler");

            AccessController controller = (AccessController) newInstance(xmlAC);
            if (controller == null)
                {
                throw new RuntimeException(
                    "The 'access-controller' configuration element must be specified");
                }
            CallbackHandler handler     = (CallbackHandler)  newInstance(xmlCH);

            deps.setAccessController(controller);
            deps.setCallbackHandler(handler);
            deps.setLoginModuleName(xml.getSafeElement("login-module-name").getString(deps.getLoginModuleName()));
            }

        return deps;
        }

    // ----- helpers --------------------------------------------------

    /**
     * Instantiate the callbackHandler and accessController objects
     *
     * @param xmlConfig  the xml configuration for accessController or
     * callbackHandler object
     */
    private static Object newInstance(XmlElement xmlConfig)
        {
        String sClass = xmlConfig.getSafeElement("class-name").getString();
        if (sClass.length() > 0)
            {
            XmlElement xmlParams = xmlConfig.getSafeElement("init-params");
            Object[]   aoParam   = XmlHelper.parseInitParams(xmlParams);
            try
                {
                Class clz = ExternalizableHelper.loadClass(sClass, null, null);
                return ClassHelper.newInstance(clz, aoParam);
                }
            catch (Exception e)
                {
                throw Base.ensureRuntimeException(e);
                }
            }
        else
            {
            return null;
            }
        }
      }
