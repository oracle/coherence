/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io.pof.generator;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.dev.tools.CommandLineTool;

import com.tangosol.io.pof.generator.PofConfigGenerator.Dependencies;

import com.tangosol.util.Base;
import com.tangosol.util.ListMap;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Executor provides command line initiation of the
 * {@link PofConfigGenerator} routine. The various configuration parameters
 * of the {@link PofConfigGenerator} are read as arguments to this process
 * and passed on to the generator. Once the generator has complete this
 * process will output the absolute location of the generated file.
 * <p>
 * To see usage information of this class execute the following:
 * <pre>
 *     java com.tangosol.io.generator.Executor -help
 * </pre>
 *
 * @author hr  2012.07.12
 *
 * @since Coherence 12.1.2
 */
public class Executor
    {

    /**
     * Execute the PofConfigGenerator or GarPofConfigGenerator based on the
     * arguments provided.
     */
    public void execute()
        {
        String[]    asArgs  = getArguments();
        ListMap     mapArgs = CommandLineTool.parseArguments(asArgs, COMMANDS, true);

        if (mapArgs.containsKey("help"))
            {
            usage(null);
            return;
            }

        // validate mandatory args
        String sRoot = (String) mapArgs.get("root");
        if (sRoot == null)
            {
            usage("-root is a mandatory argument");
            return;
            }

        // null means all packages
        String      sPackages   = (String) mapArgs.get("packages");
        Set<String> setPackages = new HashSet<String>(Arrays.asList(sPackages == null
                ? new String[]{""} : sPackages.split(",")));

        // parse the start type id
        Integer ITypeId = null;
        String  sTypeId = (String) mapArgs.get("startTypeId");
        if (sTypeId != null && !sTypeId.isEmpty())
            {
            try
                {
                ITypeId = Integer.parseInt(sTypeId);
                }
            catch (NumberFormatException e)
                {
                usage("-startTypeId must be a valid integer");
                }
            }

        Dependencies deps = new Dependencies()
            .setPathRoot(Arrays.asList((sRoot.split(File.pathSeparator))))
            .setOutputPath((String) mapArgs.get("out"))
            .setPofConfig((String) mapArgs.get("config"))
            .setInclude(mapArgs.containsKey("include"))
            .setPackages(setPackages)
            .setStartTypeId(ITypeId);

        Logger.fine("Generating POF Configuration...");
        PofConfigGenerator generator = createPofConfigGenerator(deps);
        generator.generate();

        try
            {
            Logger.fine("POF Configuration generated: "
                    + new File(generator.getWrittenPath()).getCanonicalPath());
            }
        catch (IOException e)
            {
            throw Base.ensureRuntimeException(e, "Error in generating POF configuration");
            }
        }

    // ----- accessors ------------------------------------------------------

    /**
     * Returns the arguments passed to this class for interpretation and
     * ultimately used by the {@link PofConfigGenerator}.
     *
     * @return the arguments passed to this class
     */
    public String[] getArguments()
        {
        return m_asArguments;
        }

    /**
     * Sets the arguments passed to this class for interpretation and
     * ultimately used by the {@link PofConfigGenerator}.
     *
     * @param asArgs  the arguments passed to this class
     *
     * @return a reference to this instance
     */
    public Executor setArguments(String[] asArgs)
        {
        m_asArguments = asArgs;
        return this;
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Using the provided {@link Dependencies} construct the appropriate
     * {@link PofConfigGenerator} implementation.
     *
     * @param deps  dependencies taken from the command line
     *
     * @return an appropriate PofConfigGenerator implementation
     */
    protected PofConfigGenerator createPofConfigGenerator(Dependencies deps)
        {
        return new PofConfigGenerator(deps);
        }

    /**
     * Help information displayed to the configured {@link PrintStream}.
     *
     * @param sMsg  a message to display along side the usage
     */
    protected void usage(String sMsg)
        {
        StringBuilder sb = new StringBuilder();

        sb.append("POF Configuration Generator\n")
          .append("Copyright (C) Oracle Coherence\n")
          .append("http://coherence.oracle.com\n\n");

        if (sMsg != null)
            {
            sb.append("Error: ").append(sMsg).append("\n\n");
            }

        sb.append("POF Configuration Generator is tool to generate a POF configuration XML file.\n")
          .append("The configuration is generated based upon scanning a referenced directory\n")
          .append("or the classpath of this application for classes annotated with a Portable\n")
          .append("annotation.\n\n");

        sb.append("Usage: pof-config-gen.sh  [OPTIONS] -root\n")
          .append("  or   pof-config-gen.bat [OPTIONS] -root\n")
          .append("  or   java ").append(getClass().getName()).append(" [OPTIONS] -root\n")
          .append("Search for all classes annotated with Portable outputting the generated file\n")
          .append("to specified (-out) directory or the current working directory.\n")
          .append("Example: pof-config-gen.sh -root /tmp/jar-folder\n\n");

        sb.append("Required\n")
          .append("  -root           root may be one of the following:\n")
          .append("                    a path separator separated string of directories or jar\n")
          .append("                    files\n")
          .append("                    a directory containing jar files or the root of classes\n")
          .append("                    a jar file\n")
          .append("                    a gar file\n\n")
          .append("Options\n")
          .append("  -out            destination of the generated file. Either a file or \n")
          .append("                  directory with the current working directory as the\n")
          .append("                  default\n")
          .append("  -config         a POF Configuration file to honor user types\n")
          .append("  -include        indicates to include the previous POF configuration\n")
          .append("                  by reference opposed to inline the user types\n")
          .append("  -packages       comma separated list of packages to scan for\n")
          .append("  -startTypeId    the user type id to start allocations from\n")
          .append("  -help           show this help\n");

        Logger.log(sb.toString(), Logger.ALWAYS);
        }

    // ----- constants ------------------------------------------------------

    /**
     * The start of this program to generate a POF configuration based on the
     * parameters provided.
     *
     * @param asArgs  the parameters to generate a POF configuration
     */
    public static void main(String[] asArgs)
        {
        // remove Coherence logging
        System.setProperty("coherence.log.level", "0");

        new Executor().setArguments(asArgs).execute();
        }

    /**
     * Recognized commands.
     */
    protected static final String[] COMMANDS = new String[]
        {
        "root", "out", "config", "include", "packages", "startTypeId", "help"
        };

    // ----- data members ---------------------------------------------------

    /**
     * The arguments used to configure the POF configuration generation.
     */
    protected String[]    m_asArguments;
    }
