/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.performance;

import com.oracle.bedrock.Option;
import com.oracle.bedrock.OptionsByType;
import com.oracle.bedrock.runtime.Application;
import com.oracle.bedrock.runtime.Platform;
import com.oracle.bedrock.runtime.coherence.options.LocalHost;
import com.oracle.bedrock.runtime.java.ClassPath;
import com.oracle.bedrock.runtime.java.options.HeapSize;
import com.oracle.bedrock.runtime.java.options.JavaHome;
import com.oracle.bedrock.runtime.java.options.JvmOptions;
import com.oracle.bedrock.runtime.java.profiles.RemoteDebugging;
import com.oracle.bedrock.runtime.options.Argument;
import com.oracle.bedrock.runtime.options.Console;
import com.oracle.bedrock.runtime.options.WorkingDirectory;
import com.oracle.bedrock.runtime.remote.DeploymentArtifact;
import com.oracle.bedrock.runtime.remote.Password;
import com.oracle.bedrock.runtime.remote.RemotePlatform;
import com.oracle.bedrock.runtime.remote.java.options.JavaDeployment;
import com.oracle.bedrock.runtime.remote.options.Deployment;
import com.oracle.bedrock.runtime.remote.options.StrictHostChecking;
import com.oracle.bedrock.runtime.remote.ssh.JSchRemoteTerminal;
import com.oracle.bedrock.runtime.remote.ssh.SftpDeployer;
import com.tangosol.net.CacheFactory;
import com.tangosol.util.Base;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * @author jk 2016.02.12
 */
public class PsrRemoteEnvironment<E extends PsrRemoteEnvironment>
        extends PsrPerformanceEnvironment<E>
    {
    // ----- constructors ---------------------------------------------------

    public PsrRemoteEnvironment()
        {
        String sJavaHome = System.getProperty("test.java.home");

        withJavaHome(JavaHome.at(sJavaHome));
        withConsolePlatform(getConsolePlatform());
        withRunnerCount(4);
        withStorageMembersPerHost(3);
        withRunnerPlatforms(getRunnerPlatforms());
        withClusterPlatforms(getClusterPlatforms());
        withClusterMemberHeap(HeapSize.of(6, HeapSize.Units.GB, 6, HeapSize.Units.GB));
        withRunnerHeap(HeapSize.of(4, HeapSize.Units.GB, 4, HeapSize.Units.GB));
        withTestClasspath(new ClassPath(getLibFolder() + "/*"));
        withClientConfiguration("client-cache-config-java-WithNearCache-4gx4.xml");
        withClusterJvmOptions(JvmOptions.include("-server", "-verbose:gc", "-XX:+UseConcMarkSweepGC", "-XX:+UseLargePages"));
        }

    // ----- RemoteEnvironment methods --------------------------------------

    /**
     * Deploy all of the required libraries to the remote platform.
     *
     * @param listLibDirs  the list of Coherence JAR files to be deployed
     */
    @Override
    public void deploy(List<File> listLibDirs)
            throws Exception
        {
        String                sLibFolder      = getLibFolder();
        File                  fileTmp         = new File(sLibFolder);
        JavaDeployment        deployment      = JavaDeployment.automatic();
        SftpDeployer          deployer        = new SftpDeployer();
        ProtectionDomain      pdCOH           = CacheFactory.class.getProtectionDomain();
        File                  fileCOH         = new File(pdCOH.getCodeSource().getLocation().toURI());

        List<ClassPath> paths = StreamSupport.stream(ClassPath.ofSystem().spliterator(), false)
                .filter((p) -> !(p.contains("prj/coherence") && p.endsWith("target/classes/")))
                .filter((p) -> !(p.contains("prj/fmw") && p.endsWith("target/classes/")))
                .filter((p) -> !p.contains("coherence-core"))
                .filter((p) -> !p.contains("coherence-discovery"))
                .filter((p) -> !p.contains("coherence-transaction"))
                .map(ClassPath::new)
                .collect(Collectors.toList());

        ClassPath classPath = new ClassPath(paths);

        OptionsByType optionsCP = OptionsByType.of();
        optionsCP.add(WorkingDirectory.at(fileTmp));
        optionsCP.add(classPath);

        deployment.exclude("junit-rt.jar");
        deployment.exclude(fileCOH.getName().toLowerCase());

        File fileLib = new File(fileTmp, "lib");

        boolean fSkipDeploy = Boolean.getBoolean("test.deploy.skip");

        if (!fSkipDeploy)
            {
            List<Platform> listTargets = new ArrayList<>();
            Set<String>    hostNames   = new HashSet<>();

            String consoleHostName = getConsoleHostName();
            if (consoleHostName != null && !consoleHostName.isEmpty())
                {
                hostNames.add(getConsoleHostName());
                listTargets.add(getRemotePlatform(getConsoleHostName(), JavaDeployment.empty()));
                }

            if (Boolean.getBoolean("test.deploy.all"))
                {
                Collections.addAll(hostNames, getRunnerHostNames());

                Collections.addAll(hostNames, getClusterHostNames());

                for (String hostName : hostNames)
                    {
                    listTargets.add(getRemotePlatform(hostName, JavaDeployment.empty()));
                    }
                }


            System.err.println("Deploying jar files to " + getConsoleHostName() + " " + hostNames + "...");

            for (Platform targetPlatform : listTargets)
                {
                System.err.println("Deploying to " + targetPlatform.getName());

                List<DeploymentArtifact> listArtifacts = deployment.getDeploymentArtifacts(targetPlatform, optionsCP);
                JSchRemoteTerminal       terminal      = new JSchRemoteTerminal((RemotePlatform) targetPlatform);

                terminal.makeDirectories(sLibFolder, targetPlatform.getOptions());
                terminal.makeDirectories(sLibFolder + File.separator + "lib", targetPlatform.getOptions());

                for (File cohFolder : listLibDirs)
                        {
                        File fileSrc = new File(cohFolder, "coherence.jar");
                        File fileParent = new File(fileLib, cohFolder.getName());
                        File fileDest = new File(fileParent, "coherence.jar");
                        DeploymentArtifact artifact = new DeploymentArtifact(fileSrc, fileDest);

                        terminal.makeDirectories(fileParent.getAbsolutePath(), targetPlatform.getOptions());

                        listArtifacts.add(artifact);
                        }

                try (Application application = targetPlatform.launch("rm",
                                                                     Argument.of(sLibFolder + File.separator + "*.jar"),
                                                                     EmptyDeployment.INSTANCE,
                                                                     Console.system()))
                    {
                    application.waitFor();
                    }

                deployer.deploy(listArtifacts, sLibFolder, targetPlatform);
                }

            System.err.println("Deployed");
            }
        }


    /**
     * Apply any environment specific modifications to the schema.
     *
     * @param platform  the {@link Platform} the process will run on
     * @param schema    the {@link OptionsByType} to modify
     */
    @Override
    protected void modifySchema(Platform platform, OptionsByType schema)
        {
        schema.add(LocalHost.of(platform.getAddress().getHostAddress()));
        }


    protected RemotePlatform getRemotePlatform(String sHost, Option... options)
        {
        try
            {
            InetAddress   addressConsole = InetAddress.getByName(sHost);
            OptionsByType opts           = OptionsByType.of(options);
            String        sName          = sHost.split("\\.")[0];
            String        sUserName      = System.getProperty("test.remote.username");
            String        sPassword      = System.getProperty("test.remote.password");

            opts.addIfAbsent(StrictHostChecking.disabled());
            opts.addIfAbsent(JavaDeployment.empty());
            opts.add(getJavaHome());
            opts.add(RemoteDebugging.disabled());

            return new RemotePlatform(sName, addressConsole, sUserName, new Password(sPassword), opts.asArray());
            }
        catch (UnknownHostException e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }

    /**
     * Obtain the {@link RemotePlatform} that the Console will run on.
     *
     * @return the {@link RemotePlatform} that the Console will run on
     */
    protected synchronized Platform getConsolePlatform()
        {
        if (m_platformConsole == null)
            {
            String sHostName = getConsoleHostName();

            if (sHostName != null && !sHostName.isEmpty())
                {
                m_platformConsole = getRemotePlatform(sHostName);
                }
            }

        return m_platformConsole;
        }


    /**
     * Obtain the {@link RemotePlatform}s that the Runner clients will run on.
     *
     * @return the {@link RemotePlatform}s that the Runner clients will run on
     */
    protected synchronized Platform[] getRunnerPlatforms()
        {
        if (m_aPlatformRunners == null)
            {
            String[] asRunnerHosts = getRunnerHostNames();

            m_aPlatformRunners = new Platform[asRunnerHosts.length];

            for (int i=0; i<asRunnerHosts.length; i++)
                {
                m_aPlatformRunners[i] = getRemotePlatform(asRunnerHosts[i]);
                }
            }

        return m_aPlatformRunners;
        }


    /**
     * Obtain the {@link RemotePlatform}s that the cluster will run on.
     *
     * @return the {@link RemotePlatform}s that the cluster will run on
     */
    protected synchronized Platform[] getClusterPlatforms()
        {
        if (m_aPlatformCluster == null)
            {
            String[] asClusterHosts = getClusterHostNames();

            m_aPlatformCluster = new Platform[asClusterHosts.length];

            for (int i=0; i<asClusterHosts.length; i++)
                {
                m_aPlatformCluster[i] = getRemotePlatform(asClusterHosts[i]);
                }
            }

        return m_aPlatformCluster;
        }


    /**
     * Obtain the folder containing the different Coherence version to test.
     *
     * @return  the folder containing the different Coherence version to test
     */
    protected String getLibFolder()
        {
        String sFolder = System.getProperty("test.lib.folder");

        if (sFolder == null || sFolder.isEmpty())
            {
            throw new RuntimeException("The test.lib.folder property must be set to the location of " +
                                               "the lib folder on the remote host");
            }

        return sFolder;
        }


    protected String getConsoleHostName()
        {
        return System.getProperty("test.host.console");
        }

    protected String[] getRunnerHostNames()
        {
        String sHost = System.getProperty("test.host.runners");

        if (sHost == null || sHost.isEmpty())
            {
            throw new RuntimeException("The test.host.runners property must be set to a comma delimited list of " +
                                               "host names of the remote hosts to use to run the clients");
            }

        return sHost.split(",");
        }

    @Override
    protected String[] getClusterHostNames()
        {
        String sHost = System.getProperty("test.host.cluster");

        if (sHost == null || sHost.isEmpty())
            {
            throw new RuntimeException("The test.host.cluster property must be set to a comma delimited list of " +
                                               "host names of the remote hosts to use to run the Cluster members");
            }

        return sHost.split(",");
        }

    // ----- inner class: EmptyDeployment -----------------------------------

    public static class EmptyDeployment
            implements Deployment
        {
        private EmptyDeployment()
            {
            }

        @Override
        public List<DeploymentArtifact> getDeploymentArtifacts(Platform platform, OptionsByType optionsByType) throws IOException
            {
            return Collections.emptyList();
            }

        public static final EmptyDeployment INSTANCE = new EmptyDeployment();
        }

    // ----- data members ---------------------------------------------------

    private Platform m_platformConsole;

    private Platform[] m_aPlatformCluster;

    private Platform[] m_aPlatformRunners;
    }
