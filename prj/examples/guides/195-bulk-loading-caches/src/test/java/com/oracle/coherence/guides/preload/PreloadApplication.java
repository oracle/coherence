/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.guides.preload;

import com.oracle.coherence.common.base.Logger;
import com.oracle.coherence.guides.preload.loaders.CustomerJdbcLoader;
import com.oracle.coherence.guides.preload.loaders.OrderJdbcLoader;
import com.tangosol.net.Coherence;
import com.tangosol.net.Session;

import java.sql.Connection;
import java.sql.DriverManager;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class PreloadApplication
        extends AbstractPreloadApplication
    {
    public PreloadApplication(Collection<Runnable> tasks, Duration timeout)
        {
        super(tasks, timeout);
        }

    public static void main(String[] args) throws Exception
        {
        // Start a Coherence custer member (and wait for it to be fully started)
        try (Coherence coherence = Coherence.clusterMember().start().join())
            {
            Session session = coherence.getSession();

            String url = System.getProperty("jdbc.url");
            if (url == null || url.isBlank())
                {
                throw new IllegalStateException("The jdbc.url system property has not been set");
                }
            Logger.info("Preload application connecting to database: " + url);
            Connection connection = DriverManager.getConnection(url);

            // create the list of preload tasks that will run
            List<Runnable> tasks = new ArrayList<>();
            tasks.add(new CustomerJdbcLoader(connection, session, 10));
            tasks.add(new OrderJdbcLoader(connection, session, 10));

            PreloadApplication application = new PreloadApplication(tasks, Duration.ofMinutes(10));

            application.run();
            }
        }
    }
