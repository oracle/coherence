/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package reporter;


import com.tangosol.coherence.reporter.Reporter;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.Cluster;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertTrue;


/**
* A collection of functional tests for the various {@link com.tangosol.coherence.reporter.Reporter}
* implementations.
*
* @author oew 05/05/2008
* @see com.tangosol.coherence.reporter.Reporter
*/
public class ReporterDeltaTests
        extends AbstractReporterTests
    {
    /**
    * Test the delta Reporter calculations with changing MBean inputs.
    */
    @Test
    public void testDelta() throws Exception
        {
        Cluster cluster = CacheFactory.ensureCluster();

        DeltaData[] aData = configDelta(cluster);

        String outputDir = m_temporaryFolder.newFolder().getCanonicalPath();
        
        String sReport   = "unit-test-delta.xml";
        String sOutput   = outputDir + File.separator + "unit-test-delta.txt";
        String sValidate = "./unit-test-delta-base.txt";
        String sError    =  "Incorrect Delta calculations";

        deleteFile(sOutput);

        Reporter reporter = new Reporter();
        reporter.run(sReport, outputDir + File.separator, 1, null);

        updateData(aData);

        reporter.run(sReport, outputDir + File.separator, 2, null);

        updateData(aData);

        reporter.run(sReport, outputDir + File.separator, 3, null);

        assertTrue(sError, compareFiles(sOutput, sValidate));
        }
    }
