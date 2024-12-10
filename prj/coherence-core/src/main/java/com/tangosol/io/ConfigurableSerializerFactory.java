/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io;


import com.tangosol.coherence.config.builder.ParameterizedBuilder;

import com.tangosol.run.xml.XmlConfigurable;
import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;

import com.tangosol.util.Base;


/**
* A {@link SerializerFactory} implementation that creates instances of a
* Serializer class configured using an XmlElement of the following structure:
* <pre>
*   &lt;!ELEMENT instance ((class-name | (class-factory-name, method-name), init-params?)&gt;
*   &lt;!ELEMENT init-params (init-param*)&gt;
*   &lt;!ELEMENT init-param ((param-name | param-type), param-value, description?)&gt;
* </pre>
*
* <p>
* <strong>This class has now been deprecated and replaced with a
* {@link ParameterizedBuilder} that produces a {@link SerializerFactory}.
* </strong>
*
* @see com.tangosol.coherence.config.xml.processor.SerializerFactoryProcessor
*
* @author lh/jh  2010.11.30
*
* @since Coherence 3.7
*/
@Deprecated
public class ConfigurableSerializerFactory
        implements SerializerFactory, XmlConfigurable
    {
    // ----- SerializerFactory interface ------------------------------------

    /**
    * {@inheritDoc}
    */
    public Serializer createSerializer(ClassLoader loader)
        {
        Serializer serializer = (Serializer) XmlHelper.createInstance(getConfig(),
            loader, /*resolver*/ null, Serializer.class);

        if (serializer instanceof ClassLoaderAware)
            {
            try
                {
                ((ClassLoaderAware) serializer).setContextClassLoader(loader);
                }
            catch (Throwable t)
                {
                throw Base.ensureRuntimeException(t, "error creating class \""
                                                     + serializer.getClass().getName() + '"');
                }
            }

        return serializer;
        }

    @Override
    public String getName()
        {
        return null;
        }

    // ----- XmlConfigurable ------------------------------------------------

    /**
    * {@inheritDoc}
    */
    public void setConfig(XmlElement xml)
        {
        m_xmlConfig = xml;
        }

    /**
    * {@inheritDoc}
    */
    public XmlElement getConfig()
        {
        return m_xmlConfig;
        }


    // ----- Object methods -------------------------------------------------

    /**
    * {@inheritDoc}
    */
    public String toString()
        {
        return "ConfigurableSerializerFactory{Xml=" + getConfig() + "}";
        }


    // ---- data members ----------------------------------------------------

    /**
    * XML configuration for this ConfigurableSerializerFactory.
    */
    private XmlElement m_xmlConfig;
    }
