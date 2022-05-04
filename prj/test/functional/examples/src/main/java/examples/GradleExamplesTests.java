/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package examples;

import com.oracle.bedrock.options.Timeout;
import com.oracle.bedrock.runtime.Application;
import com.oracle.bedrock.runtime.ApplicationConsole;
import com.oracle.bedrock.runtime.LocalPlatform;
import com.oracle.bedrock.runtime.Platform;
import com.oracle.bedrock.runtime.console.CapturingApplicationConsole;
import com.oracle.bedrock.runtime.console.FileWriterApplicationConsole;
import com.oracle.bedrock.runtime.options.Arguments;
import com.oracle.bedrock.runtime.options.Console;
import com.oracle.bedrock.runtime.options.WorkingDirectory;
import com.tangosol.io.FileHelper;
import com.tangosol.run.xml.SimpleDocument;
import com.tangosol.run.xml.XmlDocument;
import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;
import com.oracle.coherence.testing.AbstractTestInfrastructure;
import org.junit.Assume;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * This test run all of the Gradle builds for the examples.
 *
 * @author Jonathan Knight  2021.02.02
 */
@Disabled("Temporarily  disable due to Gradle test not getting correct dependencies")
class GradleExamplesTests
    {
    @BeforeAll
    static void setup()
        {
        s_fileOutDir = AbstractTestInfrastructure.ensureOutputDir("examples");

        if (s_fIsWindows)
            {
            s_sExecutable = "cmd.exe";
            s_args        = Arguments.of("/C", "gradlew.bat");
            }
        else
            {
            s_sExecutable = "sh";
            s_args        = Arguments.of("gradlew");
            }
        }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("findProjects")
    void shouldBuildGradleProject(String sName, File dir) throws Exception
        {
        Assume.assumeThat("Skipping this test on Windows", s_fIsWindows, is(false));
        Assume.assumeThat("Skipping test, cannot download Gradle", tryGradle(dir), is(true));

        File fileTestOutDir = new File(s_fileOutDir, sName);
        fileTestOutDir.mkdirs();
        assertThat(fileTestOutDir.exists(), is(true));
        assertThat(fileTestOutDir.isDirectory(), is(true));

        FileWriter         writer   = new FileWriter(new File(fileTestOutDir, "gradle.log"));
        ApplicationConsole console  = new FileWriterApplicationConsole(writer);
        Platform           platform = LocalPlatform.get();
        Arguments          args     = s_args.with("clean", "build", "--no-build-cache").with(proxyArguments());

        try (Application application = platform.launch(s_sExecutable,
                                                       args,
                                                       WorkingDirectory.at(dir),
                                                       Console.of(console)))
            {
            int nExitCode = application.waitFor(Timeout.after(5, TimeUnit.MINUTES));
            assertThat("Failed Gradle build " + dir, nExitCode, is(0));
            }
        finally
            {
            copyTestOutput(dir, fileTestOutDir);
            }
        }

    private void copyTestOutput(File dir, File fileTestOutDir)
        {
        File fileBuild   = new File(dir, "build");
        File fileResults = new File(fileBuild, "test-results");
        File fileCopy    = new File(fileTestOutDir, "test-results");

        if (fileResults.exists())
            {
            fileCopy.mkdirs();

            try
                {
                FileHelper.copyDir(fileResults, fileCopy);
                }
            catch (IOException e)
                {
                System.err.println("Error copying test results from " + fileResults);
                e.printStackTrace();
                }
            }
        }

    @SuppressWarnings("unchecked")
    static List<org.junit.jupiter.params.provider.Arguments> findProjects() throws Exception
        {
        File file = new File(".").getAbsoluteFile().getCanonicalFile();
        while (file != null && !"prj".equals(file.getName()))
            {
            file = file.getParentFile();
            }
        assertThat(file, is(notNullValue()));

        File        fileExamples = new File(file, "examples");
        File        filePom      = new File(fileExamples, "pom.xml");
        byte[]      bytes        = Files.readAllBytes(filePom.toPath());
        XmlDocument xmlPom       = new SimpleDocument();

        XmlHelper.loadXml(new String(bytes), xmlPom, false);

        XmlElement           xmlModules = xmlPom.getSafeElement("modules");
        Iterator<XmlElement> it         = xmlModules.getElements("module");

        List<org.junit.jupiter.params.provider.Arguments> list = new ArrayList<>();

        while(it.hasNext())
            {
            String sModule = it.next().getString();
            String sName   = sModule.replace(File.separator, "-");
            list.add(org.junit.jupiter.params.provider.Arguments.of(sName, new File(fileExamples, sModule)));
            }

        return list;
        }

    private static boolean tryGradle(File dir)
        {
        CapturingApplicationConsole console = new CapturingApplicationConsole();
        try (Application app = LocalPlatform.get().launch(s_sExecutable,
                                                          s_args.with("tasks").with(proxyArguments()),
                                                          WorkingDirectory.at(dir),
                                                          Console.of(console)))
            {
            int nExitCode = app.waitFor();
            if (nExitCode != 0)
                {
                for (String line : console.getCapturedErrorLines())
                    {
                    if (line.contains("java.io.IOException: Downloading"))
                        {
                        return false;
                        }
                    }
                }
            }
        return true;
        }

    private static Arguments proxyArguments()
        {
        Arguments          arguments          = Arguments.empty();
        String             sHttpProxyHost     = System.getProperty("http.proxyHost");
        String             sHttpProxyPort     = System.getProperty("http.proxyPort");
        String             sHttpNoProxyHosts  = System.getProperty("http.nonProxyHosts");
        String             sHttpsProxyHost    = System.getProperty("https.proxyHost");
        String             sHttpsProxyPort    = System.getProperty("https.proxyPort");
        String             sHttpsNoProxyHosts = System.getProperty("https.nonProxyHosts");

        if (sHttpProxyHost != null)
            {
            arguments = arguments.with("-Dhttp.proxyHost=" + sHttpProxyHost);
            }

        if (sHttpProxyPort != null)
            {
            arguments = arguments.with("-Dhttp.proxyPort=" + sHttpProxyPort);
            }

        if (sHttpNoProxyHosts != null)
            {
            arguments = arguments.with("-Dhttp.nonProxyHosts=" + sHttpNoProxyHosts);
            }

        if (sHttpsProxyHost != null)
            {
            arguments = arguments.with("-Dhttps.proxyHost=" + sHttpsProxyHost);
            }

        if (sHttpsProxyPort != null)
            {
            arguments = arguments.with("-Dhttps.proxyPort=" + sHttpsProxyPort);
            }

        if (sHttpsNoProxyHosts != null)
            {
            arguments = arguments.with("-Dhttps.nonProxyHosts=" + sHttpsNoProxyHosts);
            }

        return arguments;
        }

    // ----- data members ---------------------------------------------------

    private static final boolean s_fIsWindows = new StringTokenizer(System.getProperty("os.name").toLowerCase().trim()).nextToken().contains("windows");

    private static File s_fileOutDir;

    private static Arguments s_args;

    private static String s_sExecutable;
    }
