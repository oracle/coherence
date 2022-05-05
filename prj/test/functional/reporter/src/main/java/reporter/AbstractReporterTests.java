/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package reporter;


import com.tangosol.net.Cluster;

import com.tangosol.net.management.Registry;

import com.tangosol.util.Resources;

import com.oracle.coherence.testing.AbstractFunctionalTest;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.net.URL;


/**
* A collection of functional tests for the various {@link com.tangosol.coherence.reporter.Reporter}
* implementations.
*
* @author oew 05/05/2008
* @see com.tangosol.coherence.reporter.Reporter
*/
public class AbstractReporterTests
        extends AbstractFunctionalTest
    {
    // ----- test lifecycle -------------------------------------------------

    /**
    * Initialize the test class.
    */
    @BeforeClass
    public static void _startup()
        {
        System.setProperty("coherence.management", "local-only");

        AbstractFunctionalTest._startup();
        }

    // ----- helper methods -------------------------------------------------

    /**
    * Configure the static data for the test
    *
    * @param cluster The cluster to register the MBeans.
    */
    protected void configData(Cluster cluster)
        {
        Registry registry = cluster.getManagement();

        registry.register("Coherence:type=Test,nodeId=1", new Data(100L, "Ora"));
        registry.register("Coherence:type=Test,nodeId=2", new Data(200L, "Cle"));
        registry.register("Coherence:type=Test,nodeId=3", new Data(300L, "Ora"));
        registry.register("Coherence:type=Test,nodeId=4", new Data(400L, "Cle"));
        registry.register("Coherence:type=Test,nodeId=5", new Data(500L, "Ora"));

        registry.register("Coherence:type=TestJoin,nodeId=1", new Data(150L, "Cle"));
        registry.register("Coherence:type=TestJoin,nodeId=2", new Data(250L, "Ora"));
        registry.register("Coherence:type=TestJoin,nodeId=3", new Data(350L, "Cle"));
        registry.register("Coherence:type=TestJoin,nodeId=4", new Data(450L, "Ora"));
        registry.register("Coherence:type=TestJoin,nodeId=5", new Data(550L, "Cle"));
        }

    /**
    * Configure the dynamic data for the test
    *
    * @param cluster The cluster to register the MBeans.
    */
    protected DeltaData[] configDelta(Cluster cluster)
        {
        Registry registry = cluster.getManagement();
        DeltaData[] aDelta = {new DeltaData(100L, "Ora"),
                              new DeltaData(200L, "Cle"),
                              new DeltaData(300L, "Cle"),
                              new DeltaData(400L, "Ora"),
                              new DeltaData(500L, "Cle"),
                              new DeltaData(150L, "Cle"),
                              new DeltaData(250L, "Cle"),
                              new DeltaData(350L, "Cle"),
                              new DeltaData(450L, "Cle"),
                              new DeltaData(550L, "Cle")};

        for (int i = 0; i < 10; i++)
            {
            registry.register("Coherence:type=Delta,nodeId="+ Integer.toString(i+1),aDelta[i]);
            }
        return aDelta;
        }

    /**
    * Force Dynamic MBeans to change
    *
    * @param aDelta an array of Delta MBeans
    */
    protected void updateData(DeltaData[] aDelta)
        {
        for (int i = 0; i < 10; i++)
            {
            aDelta[i].setData();
            }
        }

    /**
    * Compare two files to determine if the contents are equal
    *
    * @param sFile1 first text file to compare.
    * @param sFile2 second text file.
    * @return  true if the files are equal
    */
    protected boolean compareFiles(String sFile1, String sFile2)
        {
        try
            {
            URL url1 = Resources.findFileOrResource(sFile1, null);
            URL url2 = Resources.findFileOrResource(sFile2, null);
            InputStream is1  = url1.openStream();
            InputStream is2  = url2.openStream();

            BufferedReader d1 = new BufferedReader(new InputStreamReader(is1));
            BufferedReader d2 = new BufferedReader(new InputStreamReader(is2));
            String sCurrent1 = d1.readLine();
            String sCurrent2 = d2.readLine();
            while (sCurrent1 != null || sCurrent2 != null)
                {
                if (sCurrent1 != null)
                    {
                    if (!sCurrent1.equals(sCurrent2))
                        {
                        return false;
                        }
                    }
                else
                    {
                    return false;
                    }
                sCurrent1 = d1.readLine();
                sCurrent2 = d2.readLine();
                }
            }
        catch (IOException e)
            {
            e.printStackTrace();
            return false;
            }
        return true;
        }

    /**
    * Remove the output file prior results.
    *
    * @param sFilename the output file to remove.
    */
    protected void deleteFile(String sFilename)
        {
        File file = new File(sFilename);
        if (file.exists())
            {
            file.delete();
            }
        }

    /**
     * JUnit rule to create a temporary folder for test output
     */
    @Rule
    public TemporaryFolder m_temporaryFolder = new TemporaryFolder();
    }
