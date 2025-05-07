/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.security;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.coherence.config.ParameterMacroExpressionParser;

import com.tangosol.coherence.config.builder.InstanceBuilder;
import com.tangosol.coherence.config.builder.ParameterizedBuilder;
import com.tangosol.coherence.config.builder.ParameterizedBuilderRegistry;

import com.tangosol.coherence.config.xml.OperationalConfigNamespaceHandler;

import com.tangosol.coherence.config.xml.processor.InstanceProcessor;
import com.tangosol.coherence.config.xml.processor.PasswordProviderBuilderProcessor;

import com.tangosol.config.xml.DefaultProcessingContext;
import com.tangosol.config.xml.DocumentProcessor;

import com.tangosol.net.ClusterDependencies;
import com.tangosol.net.PasswordProvider;

import com.tangosol.net.security.AccessController;

import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;

import com.tangosol.util.Base;
import com.tangosol.util.ClassHelper;
import com.tangosol.util.ExternalizableHelper;

import javax.security.auth.callback.CallbackHandler;

import java.util.Arrays;

/**
 * LegacyXmlStandardHelper parses the {@code <security-config>} XML to
 * populate the DefaultStandardDependencies.
 * <p>
 * NOTE: This code will eventually be replaced by CODI.
 *
 * @author der  2011.12.01
 * @since Coherence 12.1.2
 */
public class LegacyXmlStandardHelper
    {
    /**
     * Populate the DefaultStandardDependencies object from the XML
     * configuration.
     *
     * @param xml          the <{@code <security-config>} XML element
     * @param deps         the DefaultStandardDependencies to be populated
     * @param depsCluster  the cluster dependencies
     *
     * @return the DefaultStandardDependencies object that was passed in.
     */
    public static DefaultStandardDependencies fromXml(XmlElement xml, DefaultStandardDependencies deps,
            ClusterDependencies depsCluster)
        {
        LegacyXmlSecurityHelper.fromXml(xml, deps);

        if (deps.isEnabled())
            {
            XmlElement       xmlAC      = xml.getSafeElement("access-controller");
            AccessController controller = newAccessController(xmlAC, depsCluster);
            if (controller == null)
                {
                throw new RuntimeException("The 'access-controller' configuration element must be specified");
                }

            XmlElement      xmlCH   = xml.getSafeElement("callback-handler");
            CallbackHandler handler = newCallbackHandler(xmlCH, depsCluster);

            deps.setAccessController(controller);
            deps.setCallbackHandler(handler);
            deps.setLoginModuleName(xml.getSafeElement("login-module-name").getString(deps.getLoginModuleName()));
            }

        return deps;
        }

    // ----- helpers --------------------------------------------------

    /**
     * Instantiate the {@link AccessController} instance
     *
     * @param xmlConfig    the XML configuration for {@link AccessController}
     * @param depsCluster  the cluster dependencies
     */
    private static AccessController newAccessController(XmlElement xmlConfig, ClusterDependencies depsCluster)
        {
        String sClass = xmlConfig.getSafeElement("class-name").getString();

        if (sClass.isEmpty())
            {
            return null;
            }

        XmlElement xmlParams      = xmlConfig.getSafeElement("init-params");
        Object[]   aoParam        = XmlHelper.parseInitParams(xmlParams);
        XmlElement xmlPwdProvider = xmlConfig.getElement("password-provider");

        try
            {
            if (xmlPwdProvider != null)
                {
                ParameterizedBuilderRegistry      registry     = depsCluster.getBuilderRegistry();
                OperationalConfigNamespaceHandler nsHandler    = new OperationalConfigNamespaceHandler();
                DocumentProcessor.Dependencies    dependencies = new DocumentProcessor.DefaultDependencies(nsHandler)
                                                                        .setExpressionParser(new ParameterMacroExpressionParser());
                DefaultProcessingContext          ctx          = new DefaultProcessingContext(dependencies, null);

                ctx.ensureNamespaceHandler("", nsHandler);
                ctx.addCookie(ParameterizedBuilderRegistry.class, registry);

                ParameterizedBuilder<PasswordProvider> bldr        = new PasswordProviderBuilderProcessor().process(ctx, xmlPwdProvider);
                PasswordProvider                       pwdProvider = bldr.realize(null, null, null);

                int len = aoParam.length;
                if (len < 4)
                    {
                    aoParam      = Arrays.copyOf(aoParam, len + 1);
                    aoParam[len] = pwdProvider;
                    }
                else
                    {
                    if (aoParam[3] instanceof String)
                        {
                        String password = (String) aoParam[3];
                        if (!password.isEmpty())
                            {
                            Logger.warn("Both a password parameter and a PasswordProvider are configured for the AccessController. The PasswordProvider will be used.");
                            }
                        }

                    aoParam[3] = pwdProvider;
                    }
                }

            Class<?> clz = ExternalizableHelper.loadClass(sClass, null, null);
            return (AccessController) ClassHelper.newInstance(clz, aoParam);
            }
        catch (Exception e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }

    /**
     * Instantiate the {@link CallbackHandler} instance
     *
     * @param xmlConfig    the XML configuration for {@link AccessController}
     * @param depsCluster  the cluster dependencies
     */
    private static CallbackHandler newCallbackHandler(XmlElement xmlConfig, ClusterDependencies depsCluster)
        {
        ParameterizedBuilderRegistry      registry     = depsCluster.getBuilderRegistry();
        OperationalConfigNamespaceHandler nsHandler    = new OperationalConfigNamespaceHandler();
        DocumentProcessor.Dependencies    dependencies = new DocumentProcessor.DefaultDependencies(nsHandler)
                                                                .setExpressionParser(new ParameterMacroExpressionParser());
        DefaultProcessingContext          ctx          = new DefaultProcessingContext(dependencies, null);

        ctx.ensureNamespaceHandler("", nsHandler);
        ctx.addCookie(ParameterizedBuilderRegistry.class, registry);

        InstanceProcessor processor = new InstanceProcessor();
        ParameterizedBuilder<Object> builder = processor.process(ctx, xmlConfig);

        if (builder == null)
            {
            return null;
            }
        if (builder instanceof InstanceBuilder<Object> && ((InstanceBuilder<Object>) builder).isUndefined())
            {
            return null;
            }

        return (CallbackHandler) builder.realize(null, null, null);
        }
    }
