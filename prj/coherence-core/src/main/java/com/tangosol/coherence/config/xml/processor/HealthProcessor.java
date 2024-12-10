/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.xml.processor;

import com.tangosol.config.ConfigurationException;
import com.tangosol.config.annotation.Injectable;
import com.tangosol.config.xml.ElementProcessor;
import com.tangosol.config.xml.ProcessingContext;
import com.tangosol.config.xml.XmlSimpleName;
import com.tangosol.internal.health.HealthCheckDependencies;
import com.tangosol.run.xml.XmlElement;

@XmlSimpleName("health")
public class HealthProcessor
        implements ElementProcessor<HealthCheckDependencies>
    {
    @Override
    public HealthCheckDependencies process(ProcessingContext ctx, XmlElement element) throws ConfigurationException
        {
        XMLHealthCheckDependencies dependencies = new XMLHealthCheckDependencies();
        ctx.inject(dependencies, element);
        return dependencies;
        }

    // ----- inner class: XMLHealthCheckDependencies ------------------------

    public static class XMLHealthCheckDependencies
            implements HealthCheckDependencies
        {
        @Injectable("member-health-check")
        public void setMemberHealthCheck(boolean fEnabled)
            {
            m_fMemberHealthCheck = fEnabled;
            }

        @Override
        public boolean isMemberHealthCheck()
            {
            return m_fMemberHealthCheck;
            }

        @Injectable("allow-endangered")
        public void setAllowEndangered(boolean fAllowEndangered)
            {
            m_fAllowEndangered = fAllowEndangered;
            }

        @Override
        public boolean allowEndangered()
            {
            return m_fAllowEndangered;
            }

        // ----- data members -----------------------------------------------

        private boolean m_fMemberHealthCheck = true;

        private boolean m_fAllowEndangered;
        }
    }
