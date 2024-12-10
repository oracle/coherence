/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.xml.processor;

import com.tangosol.coherence.config.ResolvableParameterList;
import com.tangosol.config.ConfigurationException;
import com.tangosol.config.expression.Parameter;
import com.tangosol.config.expression.ParameterResolver;
import com.tangosol.config.xml.ElementProcessor;
import com.tangosol.config.xml.ProcessingContext;
import com.tangosol.config.xml.XmlSimpleName;


import com.tangosol.run.xml.XmlElement;


import java.util.List;

/**
 * An {@link InitParamsProcessor} is responsible for processing &lt;init-params&gt; {@link XmlElement}s to produce
 * {@link ResolvableParameterList}s.
 *
 * @author bo  2011.06.24
 * @since Coherence 12.1.2
 */
@XmlSimpleName("init-params")
public class InitParamsProcessor
        implements ElementProcessor<ParameterResolver>
    {
    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public ResolvableParameterList process(ProcessingContext context, XmlElement element)
            throws ConfigurationException
        {
        ResolvableParameterList listParameters = new ResolvableParameterList();

        for (XmlElement elementChild : ((List<XmlElement>) element.getElementList()))
            {
            Object oValue = context.processElement(elementChild);

            if (oValue instanceof Parameter)
                {
                listParameters.add((Parameter) oValue);
                }
            else
                {
                throw new ConfigurationException(String.format("Invalid parameter definition [%s] in [%s]",
                    elementChild, element), "Please ensure the parameter is correctly defined");
                }
            }

        return listParameters;
        }
    }
