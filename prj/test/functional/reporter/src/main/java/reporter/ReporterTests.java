/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package reporter;

import com.tangosol.coherence.reporter.Reporter;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.Cluster;

import org.junit.Test;

import org.junit.runner.RunWith;

import org.junit.runners.Parameterized;

import static org.junit.Assert.*;

import java.io.File;

import java.text.SimpleDateFormat;

import java.util.Arrays;
import java.util.Collection;
import java.util.TimeZone;

/**
 * A collection of functional tests for the various {@link Reporter}
 * implementations.
 *
 * @author oew 05/05/2008
 * @see Reporter
 */
@RunWith(Parameterized.class)
public class ReporterTests
        extends AbstractReporterTests
    {

    /**
     * Test the output of the Reporter with static MBean inputs
     */
    @Test
    public void testReport()
            throws Exception
        {
        Cluster cluster = CacheFactory.ensureCluster();

        configData(cluster);

        String sOutputPath   = m_temporaryFolder.newFolder().getCanonicalPath();

        String sFQOutputFile = sOutputPath + File.separator + m_sOutputFile;

        deleteFile(sFQOutputFile);

        // Explicitly set the date format so that tests run correctly
        // in all time zones.  The prj/tests/reporter/unit-test-list-base.txt has
        // hardcoded GMT times that must match the test.
        SimpleDateFormat df = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy");

        df.setTimeZone(TimeZone.getTimeZone("GMT"));

        Reporter reporter = new Reporter();

        reporter.setDateFormat(df);
        reporter.run(m_sReport, sOutputPath + File.separator, 1, null);
        assertTrue(m_sError, compareFiles(sFQOutputFile, m_sValidate));
        }

    /**
     * Create the test parameters that will be used to run the various report
     * combinations.
     *
     * @return the test parameters
     */
    @Parameterized.Parameters(name = "{index}: {0}")
    public static Collection<Object[]> data()
        {
        return Arrays.asList(new Object[][]
            {
            {"unit-test-function-constant.xml", "unit-test-function-constant.txt",
             "./unit-test-function-constant-base.txt", "Incorrect function calculations"},

            {"unit-test-aggregates.xml", "unit-test-aggregates.txt", "./unit-test-aggregates-base.txt",
             "Incorrect aggregate calculations"},

            {"unit-test-subquery.xml", "unit-test-subquery.txt", "./unit-test-subquery-base.txt",
             "Incorrect subquery calculations"},

            {"unit-test-filter1.xml", "unit-test-filter1.txt", "./unit-test-filter1-base.txt",
             "Incorrect Int to long less filter"},

            {"unit-test-filter2.xml", "unit-test-filter2.txt", "./unit-test-filter2-base.txt",
             "Incorrect Double to Constant less Filter"},

            {"unit-test-filter3.xml", "unit-test-filter3.txt", "./unit-test-filter3-base.txt",
             "Incorrect int to long Greater Filter"},

            {"unit-test-filter4.xml", "unit-test-filter4.txt", "./unit-test-filter4-base.txt",
             "Incorrect string Equals Filter"},

            {"unit-test-filter5.xml", "unit-test-filter5.txt", "./unit-test-filter5-base.txt",
             "Incorrect string Not Equals Filter"},

            {"unit-test-filter6.xml", "unit-test-filter6.txt", "./unit-test-filter6-base.txt", "Incorrect Or Filter"},

            {"unit-test-filter7.xml", "unit-test-filter7.txt", "./unit-test-filter7-base.txt", "Incorrect And Filter"},

            {"unit-test-list.xml", "unit-test-list.txt", "./unit-test-list-base.txt", "Incorrect attribute list"},

            {"unit-test-group.xml", "unit-test-group.txt", "./unit-test-group-base.txt", "Incorrect Grouping"},

            {"unit-test-join.xml", "unit-test-join.txt", "./unit-test-join-base.txt", "Incorrect Reporter Join"}
            });
        }

    /** Injected by JUnit for each set of test parameters */
    @Parameterized.Parameter(value = 0)
    public String m_sReport;

    /** Injected by JUnit for each set of test parameters */
    @Parameterized.Parameter(value = 1)
    public String m_sOutputFile;

    /** Injected by JUnit for each set of test parameters */
    @Parameterized.Parameter(value = 2)
    public String m_sValidate;

    /** Injected by JUnit for each set of test parameters */
    @Parameterized.Parameter(value = 3)
    public String m_sError;
    }
