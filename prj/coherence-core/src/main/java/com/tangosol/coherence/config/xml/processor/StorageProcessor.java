/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.xml.processor;

import com.tangosol.coherence.config.builder.UnitCalculatorBuilder;
import com.tangosol.coherence.config.scheme.CachingScheme;
import com.tangosol.coherence.config.scheme.FlashJournalScheme;
import com.tangosol.coherence.config.scheme.LocalScheme;
import com.tangosol.coherence.config.scheme.RamJournalScheme;

import com.tangosol.config.ConfigurationException;
import com.tangosol.config.expression.Expression;
import com.tangosol.config.expression.LiteralExpression;
import com.tangosol.config.expression.Parameter;
import com.tangosol.config.expression.ParameterResolver;
import com.tangosol.config.xml.ElementProcessor;
import com.tangosol.config.xml.ProcessingContext;
import com.tangosol.config.xml.XmlSimpleName;

import com.tangosol.run.xml.XmlElement;

/**
 * A {@link StorageProcessor} is responsible for processing
 * &lt;storage&gt; {@link XmlElement}s to produce
 * a {@link CachingScheme}.
 *
 * @author jf  2016.02.15
 * @since Coherence 14.1.1
 */

@XmlSimpleName("storage")
public class StorageProcessor implements ElementProcessor<CachingScheme> {
    /**
     * {@inheritDoc}
     */
    @Override
    public CachingScheme process(ProcessingContext context, XmlElement xmlElement) throws ConfigurationException
        {
        String                sValue = (String) xmlElement.getValue();
        UnitCalculatorBuilder bldr   = getUnitCalculatorBuilder(context.getDefaultParameterResolver());

        if (sValue == null || "on-heap".equals(sValue))
            {
            LocalScheme scheme = new LocalScheme();
            scheme.setUnitCalculatorBuilder(bldr);

            return scheme;
            }
        else if ("flashjournal".equals(sValue))
            {
            FlashJournalScheme scheme = new FlashJournalScheme();
            scheme.setUnitCalculatorBuilder(bldr);

            return scheme;
            }
        else if ("ramjournal".equals(sValue))
            {
            RamJournalScheme scheme = new RamJournalScheme();
            scheme.setUnitCalculatorBuilder(bldr);

            return scheme;
            }
        else
            {
            // never should get here due to xml schema validation. however, make ide happy
            XmlElement xmlParent = xmlElement.getParent();
            String sParent = xmlParent != null ? '<' + xmlParent.getName() + '>' : "";

            throw new ConfigurationException("invalid value " + sValue + " for " + sParent + "<storage> element",
                                             "Provide a valid value of \"on-heap\", \"flashjournal\" or \"ramjournal\"");
            }
    }

    // ----- helpers --------------------------------------------------------

    private UnitCalculatorBuilder getUnitCalculatorBuilder(ParameterResolver resolver)
        {
        UnitCalculatorBuilder bldr = new UnitCalculatorBuilder();
        Parameter             parm = resolver.resolve("unit-calculator");
        Expression<String>    expr = parm == null ? new LiteralExpression<>("BINARY") : parm.evaluate(resolver).as(Expression.class);

        bldr.setUnitCalculatorType(expr);

        return bldr;
        }
    }