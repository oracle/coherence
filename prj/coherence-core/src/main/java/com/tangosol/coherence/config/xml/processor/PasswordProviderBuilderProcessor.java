/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.xml.processor;

import com.tangosol.coherence.config.ParameterList;
import com.tangosol.coherence.config.ParameterMacroExpression;
import com.tangosol.coherence.config.ResolvableParameterList;
import com.tangosol.coherence.config.builder.InstanceBuilder;
import com.tangosol.coherence.config.builder.ParameterizedBuilder;

import com.tangosol.coherence.config.builder.ParameterizedBuilderRegistry;
import com.tangosol.config.ConfigurationException;
import com.tangosol.config.expression.Expression;
import com.tangosol.config.expression.Parameter;
import com.tangosol.config.xml.ElementProcessor;
import com.tangosol.config.xml.ProcessingContext;
import com.tangosol.config.xml.XmlSimpleName;

import com.tangosol.net.PasswordProvider;
import com.tangosol.run.xml.XmlElement;
import com.tangosol.util.Base;

import java.util.Arrays;

/**
 * An {@link ElementProcessor} for &lt;password-provider&gt; elements defined by
 * a Coherence Operational Configuration file.
 *
 * @author spuneet
 * @since Coherence 12.12.1.4
 */
@XmlSimpleName("password-provider")
public class PasswordProviderBuilderProcessor
        extends AbstractEmptyElementProcessor<ParameterizedBuilder<PasswordProvider>>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a {@link PasswordProviderBuilderProcessor}.
     */
    public PasswordProviderBuilderProcessor()
        {
        super(EmptyElementBehavior.IGNORE);
        }

    // ----- AbstractEmptyElementProcessor methods --------------------------

    @Override
    public ParameterizedBuilder<PasswordProvider> onProcess(ProcessingContext context, XmlElement xmlElement)
            throws ConfigurationException
        {
        // assume the <password-provider> contains a builder definition
        ParameterizedBuilder<?> bldr = ElementProcessorHelper.processParameterizedBuilder(context, xmlElement);

        if (bldr == null)
            {
            ParameterizedBuilderRegistry registry = context.getCookie(ParameterizedBuilderRegistry.class);

            // Lookup builderRegistry using the name supplied.
            String name =  getNameFromXML(xmlElement);
            Base.azzert(name != null , "<name>valid-id</name> is missing/empty. Failed to lookup a builder for PasswordProvider");

            ParameterizedBuilder<?> bldrFound = registry.getBuilder(PasswordProvider.class, name);

            if (bldrFound != null && bldrFound instanceof InstanceBuilder)
                {
                bldr = newPasswordProviderBuilderFromExisitng(context, xmlElement, bldrFound);
                }
            }

        if (bldr == null)
            {
            // Either a valid <password-provider> is not defined
            // Or <password-provider>provider-id</password-provider> doesn't have a valid name to look up the builder.
            throw new ConfigurationException("<password-provider> fails to correctly define a PasswordProvider implementation: "
                    + xmlElement, "Please define a valid <password-provider>");
            }

        return (ParameterizedBuilder<PasswordProvider>) bldr;
        }

    /**
     * Returns "null" if "name" element doesn't exist, else trimmed value of the name.
     *
     * @param xmlElement
     * @return sName
     */
    private String getNameFromXML (XmlElement xmlElement)
        {
        Object sName = xmlElement.getElement("name").getValue();  //luk could be null?
        return null == sName ? null : sName.toString().trim();
        }

    /*
     * Returns a ParameterizedBuilder<PasswordProvider> by copying the contents of an existing builder.
     * - Fetches constructor params of mapped builder
     * - Get a list of override params and overide in ResolvableParameterList.
     *      - If a param-name exists, its param-value will be overwritten.
     *      - If a param-name doesn't exists, then its added to the list after the default listConstructorParameters.
     *      - For additional params, the class-name provided should have a supporting/relevant constructor.
     *      Else it will fail at runtime to realize.
     * - Class-name is used from existing builder
      *
     * returns ParameterizedBuilder<PasswordProvider>
     */
    private ParameterizedBuilder<PasswordProvider> newPasswordProviderBuilderFromExisitng(ProcessingContext context,
                                                                                          XmlElement xmlElement,
                                                                                          ParameterizedBuilder bldrFound)
        {
        InstanceBuilder bldr = (InstanceBuilder)bldrFound;

        ParameterList listConstructorParams = bldr.getConstructorParameterList();
        ResolvableParameterList resolvableParameterList =
                listConstructorParams instanceof ResolvableParameterList ?
                        (ResolvableParameterList) listConstructorParams :
                        new ResolvableParameterList(listConstructorParams);

        // Get the Overriding params from input override-XML
        XmlElement element = xmlElement.getSafeElement("init-params");
        ResolvableParameterList listOverrideParams = (ResolvableParameterList) context.processElement(element);

        // Add / Update params to the list
        for (Parameter parameter : listOverrideParams)
            {
            resolvableParameterList.add(parameter);
            }

        InstanceBuilder bldrOverridden = new InstanceBuilder();
        bldrOverridden.setClassName(bldr.getClassName());
        bldrOverridden.setConstructorParameterList(resolvableParameterList);
        return bldrOverridden;
        }

    /**
     * Generates a ParameterizedBuilder using the DefaultPasswordProvider.class
     * by passing the string-input to its single-param constructor.
     *
     * @param password  the clear text password
     *
     * @return ParameterizedBuilder of PsswordProvider
     */
    public static ParameterizedBuilder<PasswordProvider> getPasswordProviderBuilderForPasswordStr(String password)
        {
        Expression<String> expr =
                new ParameterMacroExpression<String>(DefaultPasswordProvider.class.getName(), String.class);

        ResolvableParameterList resolvableParameterList = new ResolvableParameterList();
        if (null != password)
            {
            resolvableParameterList.add(new Parameter("param1", password));
            }

        InstanceBuilder bldr = new InstanceBuilder();
        bldr.setClassName(expr);
        bldr.setConstructorParameterList(resolvableParameterList);
        return bldr;
        }

    /**
     * Rerturns a builder for null password values.
     *
     * @return ParameterizedBuilder of PasswordProvider
     */
    public static ParameterizedBuilder<PasswordProvider> getNullPasswordProviderBuilder()
        {
        return ((resolver, loader, listParameters) -> PasswordProvider.NullImplementation);
        }

    /**
     * This class is to wrap the existing password into the password-provider approach.
     * The single-arg constructor will accept the {@link String} password and store it as a char[],
     * to be returned when the "get()", implemented as part of {@link PasswordProvider}, is called.
     *
     * @author spuneet  2017.08.18
     * @since Coherence 12.2.1.4
     */
    public static class DefaultPasswordProvider
        implements PasswordProvider
        {
        /*
         * Default constructor, set the password to null.
         */
        public DefaultPasswordProvider()
            {
            this(null);
            }

        /**
         * Constructor sets the password-string as a char[] when "get()" is called.
         *
         * @param pass  the clear text password
         */
        public DefaultPasswordProvider(String pass)
            {
            f_achPass = (null == pass ? null : pass.toCharArray());
            }

        /**
         * Returns the password.
         *
         * @return  a copy of password char[]. The consumer can zero the char[] after usage;
         * and/but may call PasswordProvider#get if it requires the password again.
         */
        @Override
        public char[] get()
            {
            return null == f_achPass
                   ? null
                   : Arrays.copyOf(f_achPass, f_achPass.length);
            }

        // char[] to store the password
        private final char[] f_achPass;
        }
    }