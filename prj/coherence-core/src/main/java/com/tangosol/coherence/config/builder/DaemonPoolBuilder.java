/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.config.builder;

import com.tangosol.coherence.config.ParameterList;
import com.tangosol.config.annotation.Injectable;
import com.tangosol.config.expression.Expression;
import com.tangosol.config.expression.ParameterResolver;
import com.tangosol.internal.util.DaemonPool;
import com.tangosol.internal.util.Daemons;
import com.tangosol.internal.util.DefaultDaemonPoolDependencies;

/**
 * A {@link ParameterizedBuilder} that builds a {@link DaemonPool}.
 *
 * If the minimum thread count is not configured it will be set to
 * the {@link Runtime#availableProcessors() number of processors}.
 */
public class DaemonPoolBuilder
        implements ParameterizedBuilder<DaemonPool>
    {
    @Override
    public DaemonPool realize(ParameterResolver resolver, ClassLoader loader, ParameterList listParameters)
        {
        int cThreadsMax = Integer.MAX_VALUE;
        int cThreads    = Runtime.getRuntime().availableProcessors();
        int cThreadsMin = cThreads;

        if (m_cThreadsMax != null)
            {
            Integer n = m_cThreadsMax.evaluate(resolver);
            cThreadsMax = (n == null || n <= 0) ? Integer.MAX_VALUE : n;
            }

        if (m_cThreadsMin != null)
            {
            Integer n = m_cThreadsMin.evaluate(resolver);
            cThreadsMin = (n == null || n <= 0) ? cThreads : n;
            }

        cThreads = Math.max(cThreads, cThreadsMin);
        cThreads = Math.min(cThreads, cThreadsMax);

        DefaultDaemonPoolDependencies deps = new DefaultDaemonPoolDependencies();
        deps.setName("CoherenceCommonPool");
        deps.setThreadCount(cThreads);
        deps.setThreadCountMax(cThreadsMax);
        deps.setThreadCountMin(cThreadsMin);

        return Daemons.newDaemonPool(deps);
        }

    public Expression<Integer> getMaxThreadCount()
        {
        return m_cThreadsMax;
        }

    @Injectable("thread-count-max")
    public void setMaxThreadCount(Expression<Integer> cThreadsMax)
        {
        m_cThreadsMax = cThreadsMax;
        }

    public Expression<Integer> getMinThreadCount()
        {
        return m_cThreadsMin;
        }

    @Injectable("thread-count-min")
    public void setMinThreadCount(Expression<Integer> cThreadsMin)
        {
        m_cThreadsMin = cThreadsMin;
        }

    // ----- data members ---------------------------------------------------

    /**
     * An {@link Expression} for the maximum thread count.
     */
    private Expression<Integer> m_cThreadsMax;

    /**
     * An {@link Expression} for the minimum thread count.
     */
    private Expression<Integer> m_cThreadsMin;
    }
