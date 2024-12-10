/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.xml.processor;

import com.tangosol.coherence.config.ParameterList;
import com.tangosol.coherence.config.ResolvableParameterList;
import com.tangosol.coherence.config.builder.InstanceBuilder;
import com.tangosol.coherence.config.builder.NamedResourceBuilder;
import com.tangosol.coherence.config.builder.ParameterizedBuilder;

import com.tangosol.coherence.config.builder.ParameterizedBuilderRegistry;
import com.tangosol.coherence.config.builder.StaticFactoryInstanceBuilder;
import com.tangosol.config.ConfigurationException;

import com.tangosol.config.expression.Parameter;
import com.tangosol.config.xml.ProcessingContext;
import com.tangosol.config.xml.XmlSimpleName;

import com.tangosol.net.OperationalContext;

import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlValue;

/**
 * An {@link com.tangosol.config.xml.ElementProcessor} for &lt;resource&gt; elements defined by
 * a Coherence Operational Configuration file.
 *
 * @author Jonathan Knight  2022.03.01
 * @since 22.06
 */
@XmlSimpleName("resource")
public class ResourceBuilderProcessor<T>
        extends AbstractEmptyElementProcessor<ParameterizedBuilder<T>>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a {@link ResourceBuilderProcessor}.
     */
    public ResourceBuilderProcessor()
        {
        super(EmptyElementBehavior.PROCESS);
        }

    // ----- AbstractEmptyElementProcessor methods --------------------------

    @Override
    @SuppressWarnings("unchecked")
    public ParameterizedBuilder<T> onProcess(ProcessingContext context, XmlElement element)
            throws ConfigurationException
        {
        ParameterizedBuilder<T> builder = null;

        // assume the <resource> contains a builder definition
        ParameterizedBuilder<T> builderInner =
                (ParameterizedBuilder<T>) ElementProcessorHelper.processParameterizedBuilder(context, element);

        if (builderInner == null)
            {
            // this should be a "named" resource, lookup builderRegistry using the name supplied.
            ParameterizedBuilderRegistry registry = context.getCookie(ParameterizedBuilderRegistry.class);

            if (registry == null)
                {
                // grab the operational context from which we can look up the serializer
                OperationalContext ctxOperational = context.getCookie(OperationalContext.class);
                if (ctxOperational == null)
                    {
                    throw new ConfigurationException("Attempted to resolve the OperationalContext in [" + element
                        + "] but it was not defined", "The registered ElementHandler for the <" + element.getName()
                        + "> element is not operating in an OperationalContext");
                    }
                registry = ctxOperational.getBuilderRegistry();
                }

            String sName = getNameFromXML(element);

            if (sName == null || sName.isEmpty())
                {
                throw new ConfigurationException("<resource> fails to correctly define a named resource " + element,
                        "Please define a valid named <resource> by specifying a name in the 'id' attribute,"
                        + " using the <name> child element, or as the <resource> element's value");
                }

            NamedResourceBuilder<T> namedBuilder = (NamedResourceBuilder<T>) registry.getBuilder(NamedResourceBuilder.class, sName);
            if (namedBuilder == null)
                {
                throw new ConfigurationException("No cluster resource has been configured with the name " + sName +
                        " configured in " + element,
                        "Please define a valid named <resource> by specifying a name in the 'id' attribute,"
                        + " using the <name> child element, or as the <resource> element's value");
                }

            builder = namedBuilder.getDelegate();
            if (builder instanceof InstanceBuilder)
                {
                builder = newInstanceBuilder(context, element, (InstanceBuilder<T>) builder);
                }
            else if (builder instanceof StaticFactoryInstanceBuilder)
                {
                builder = newStaticFactoryInstanceBuilder(context, element, (StaticFactoryInstanceBuilder<T>) builder);
                }
            }
        else
            {
            XmlValue xmlName = element.getAttribute("id");

            if (xmlName == null || xmlName.isEmpty())
                {
                throw new ConfigurationException("<resource> does not include an id attribute: "
                        + element, "Please correctly define the <resource>");
                }

            String sName = xmlName.getString();
            if (sName == null || sName.isEmpty())
                {
                throw new ConfigurationException("<resource> does not include an id attribute: "
                        + element, "Please correctly define the <resource>");
                }

            builder = new NamedResourceBuilder<>(builderInner, sName);
            }

        if (builder == null)
            {
            // Either a valid <resource> is not defined
            // Or <resource>id</resource> doesn't have a valid name to look up the builder.
            throw new ConfigurationException("<resource> fails to correctly define a resource implementation: "
                    + element, "Please define a valid <resource>");
            }

        return builder;
        }

    /**
     * Returns the value of the child {@code name} element or {@code id} attribute,
     * or {@code null} if neither the {@code name} element, nor the {@code id}
     * attribute exist.
     *
     * @param xmlElement  the {@link XmlElement} to get the chile {@code name} element from
     *
     * @return the value of the child {@code name} element, or {@code id} attribute
     */
    private String getNameFromXML (XmlElement xmlElement)
        {
        XmlValue xmlName = xmlElement.getElement("name");

        if (xmlName == null)
            {
            // no name child element, try the id attribute
            xmlName = xmlElement.getAttribute("id");
            }

        String sName = xmlName == null ? null : xmlName.getString();

        if (sName == null && xmlElement.getElementList().isEmpty())
            {
            // no name, try the element value
            sName = xmlElement.getString();
            }

        return sName == null ? null : sName.trim();
        }

    /*
     * Returns a InstanceBuilder<T> by copying the contents of an existing builder.
     * - Fetches constructor params of mapped builder
     * - Get a list of override params and override in ResolvableParameterList.
     *      - If a param-name exists, its param-value will be overwritten.
     *      - If a param-name doesn't exist, then it's added to the list after the default listConstructorParameters.
     *      - For additional params, the class-name provided should have a supporting/relevant constructor.
     *      Else it will fail at runtime to realize.
     * - Class-name is used from existing builder
      *
     * returns InstanceBuilder<T>
     */
    private InstanceBuilder<T> newInstanceBuilder(ProcessingContext  context,
                                                  XmlElement         xmlElement,
                                                  InstanceBuilder<T> builder)
        {
        ResolvableParameterList parameterList = extractParameters(context, xmlElement,
                builder.getConstructorParameterList());

        if (parameterList == null)
            {
            return builder;
            }

        InstanceBuilder<T> bldrOverridden = new InstanceBuilder<>();
        bldrOverridden.setClassName(builder.getClassName());
        bldrOverridden.setConstructorParameterList(parameterList);

        return bldrOverridden;
        }

    /*
     * Returns a InstanceBuilder<T> by copying the contents of an existing builder.
     * - Fetches constructor params of mapped builder
     * - Get a list of override params and override in ResolvableParameterList.
     *      - If a param-name exists, its param-value will be overwritten.
     *      - If a param-name doesn't exist, then it's added to the list after the default listConstructorParameters.
     *      - For additional params, the class-name provided should have a supporting/relevant constructor.
     *      Else it will fail at runtime to realize.
     * - Class-name is used from existing builder
      *
     * returns StaticFactoryInstanceBuilder<T>
     */
    private StaticFactoryInstanceBuilder<T> newStaticFactoryInstanceBuilder(ProcessingContext               context,
                                                               XmlElement                      xmlElement,
                                                               StaticFactoryInstanceBuilder<T> builder)
        {
        ResolvableParameterList parameterList = extractParameters(context, xmlElement,
                builder.getFactoryMethodParameters());

        if (parameterList == null)
            {
            return builder;
            }

        StaticFactoryInstanceBuilder<T> bldrOverridden = new StaticFactoryInstanceBuilder<>();
        bldrOverridden.setFactoryClassName(builder.getFactoryClassName());
        bldrOverridden.setFactoryMethodName(builder.getFactoryMethodName());
        bldrOverridden.setFactoryMethodParameters(parameterList);

        return bldrOverridden;
        }

    private ResolvableParameterList extractParameters(ProcessingContext context,
                                                      XmlElement        xmlElement,
                                                      ParameterList     list)
        {
        if (xmlElement.getElement("init-params") == null)
            {
            return null;
            }

        ResolvableParameterList parameterList = new ResolvableParameterList(list);

        // Get the Overriding params from input override-XML
        XmlElement              element            = xmlElement.getSafeElement("init-params");
        ResolvableParameterList listOverrideParams = (ResolvableParameterList) context.processElement(element);

        // Add / Update params to the list
        for (Parameter parameter : listOverrideParams)
            {
            parameterList.add(parameter);
            }

        return parameterList;
        }
    }
