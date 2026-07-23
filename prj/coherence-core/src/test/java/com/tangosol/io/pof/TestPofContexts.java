/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io.pof;

import com.tangosol.net.partition.PartitionSet;

import com.tangosol.run.xml.XmlHelper;

import com.tangosol.util.SimpleQueryRecord;
import com.tangosol.util.extractor.IdentityExtractor;
import com.tangosol.util.extractor.ReflectionExtractor;
import com.tangosol.util.filter.BetweenFilter;
import com.tangosol.util.filter.GreaterEqualsFilter;
import com.tangosol.util.filter.GreaterFilter;
import com.tangosol.util.filter.LessEqualsFilter;
import com.tangosol.util.filter.LessFilter;

/**
 * POF contexts for unit tests that need a narrow type table.
 *
 * @author Aleks Seovic  2026.05.14
 */
public final class TestPofContexts
    {
    private TestPofContexts()
        {
        }

    public static ConfigurablePofContext emptyConfigurablePofContext()
        {
        return new ConfigurablePofContext(XmlHelper.loadXml(EMPTY_CONFIG));
        }

    public static SafeConfigurablePofContext emptySafeConfigurablePofContext()
        {
        return new SafeConfigurablePofContext(XmlHelper.loadXml(EMPTY_CONFIG));
        }

    public static PofContext queryRecordPofContext()
        {
        SimplePofContext context = new SimplePofContext();

        register(context, 52,  IdentityExtractor.class);
        register(context, 55,  ReflectionExtractor.class);
        register(context, 64,  BetweenFilter.class);
        register(context, 69,  GreaterEqualsFilter.class);
        register(context, 70,  GreaterFilter.class);
        register(context, 75,  LessEqualsFilter.class);
        register(context, 76,  LessFilter.class);
        register(context, 171, PartitionSet.class);
        register(context, 260, SimpleQueryRecord.class);
        register(context, 261, SimpleQueryRecord.PartialResult.class);
        register(context, 262, SimpleQueryRecord.PartialResult.Step.class);
        register(context, 263, SimpleQueryRecord.PartialResult.IndexLookupRecord.class);

        return context;
        }

    private static void register(SimplePofContext context, int nType, Class clz)
        {
        context.registerUserType(nType, clz, new PortableObjectSerializer(nType));
        }

    private static final String EMPTY_CONFIG =
            "<pof-config xmlns=\"http://xmlns.oracle.com/coherence/coherence-pof-config\">"
          + "  <user-type-list/>"
          + "</pof-config>";
    }
