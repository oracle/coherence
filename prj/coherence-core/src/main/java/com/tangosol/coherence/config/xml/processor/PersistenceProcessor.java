/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.xml.processor;

import com.oracle.coherence.persistence.PersistenceEnvironment;

import com.tangosol.coherence.config.Config;

import com.tangosol.coherence.config.builder.ParameterizedBuilder;
import com.tangosol.coherence.config.builder.PersistenceEnvironmentParamBuilder;

import com.tangosol.config.ConfigurationException;
import com.tangosol.config.xml.ElementProcessor;
import com.tangosol.config.xml.ProcessingContext;
import com.tangosol.config.xml.XmlSimpleName;

import com.tangosol.internal.net.service.grid.DefaultPersistenceDependencies;
import com.tangosol.internal.net.service.grid.PersistenceDependencies;

import com.tangosol.net.OperationalContext;

import com.tangosol.persistence.SnapshotArchiverFactory;

import com.tangosol.run.xml.XmlElement;

/**
 * An {@link ElementProcessor} that will parse a &lt;persistence&gt; element to
 * produce a {@link PersistenceDependencies} instance.
 *
 * @author bo  2013.03.12
 * @since Coherence 12.1.3
 */
@XmlSimpleName("persistence")
public class PersistenceProcessor
        extends AbstractEmptyElementProcessor<PersistenceDependencies>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a {@link PersistenceProcessor}.
     */
    public PersistenceProcessor()
        {
        super(EmptyElementBehavior.PROCESS);
        }

    // ----- AbstractEmptyElementProcessor methods --------------------------

    @Override
    public PersistenceDependencies onProcess(ProcessingContext context, XmlElement xmlElement)
            throws ConfigurationException
        {
        DefaultPersistenceDependencies depsPersistence = new DefaultPersistenceDependencies();
        OperationalContext             ctxOp           = context.getCookie(OperationalContext.class);

        // process the <environment>
            {
            XmlElement xmlEnvironment = xmlElement.getSafeElement("environment");

            String sMode    = Config.getProperty("coherence.distributed.persistence.mode", () ->
                                  Config.getProperty("coherence.distributed.persistence-mode",
                                      "on-demand"));
            String sEnvName = xmlEnvironment.isEmpty()
                    ? "default-" + sMode
                    : xmlEnvironment.getString();

            PersistenceEnvironmentParamBuilder bldr = (PersistenceEnvironmentParamBuilder)
                    ctxOp.getBuilderRegistry().getBuilder(PersistenceEnvironment.class, sEnvName);

            if (bldr == null)
                {
                throw new IllegalArgumentException("Persistence environment (\"" + sEnvName
                        + "\") must be defined in the operational config");
                }

            depsPersistence.setPersistenceMode(bldr.getPersistenceMode());
            depsPersistence.setPersistenceEnvironmentBuilder((ParameterizedBuilder) bldr);
            }

        // process the <archiver>
            {
            String sArchiver = xmlElement.getSafeElement("archiver").getString();
            if (!sArchiver.isEmpty())
                {
                //
                SnapshotArchiverFactory factory = ctxOp.getSnapshotArchiverMap().get(sArchiver);

                if (factory == null)
                    {
                    throw new IllegalArgumentException("Snapshot archiver (\"" + sArchiver +
                            "\") must be defined in the operational configuration");
                    }

                depsPersistence.setArchiverFactory(factory);
                }
            }

        // process the persistence exception handling
        String sFailureMode = xmlElement.getSafeElement("active-failure-mode").getString("stop-service");
        switch (sFailureMode)
            {
            case "stop-service":
                depsPersistence.setFailureMode(PersistenceDependencies.FAILURE_STOP_SERVICE);
                break;
            case "stop-persistence":
                depsPersistence.setFailureMode(PersistenceDependencies.FAILURE_STOP_PERSISTENCE);
                break;
            default:
                throw new IllegalArgumentException("Unknown persistence active-failure-mode: " + sFailureMode);
            }

        return depsPersistence;
        }
    }
