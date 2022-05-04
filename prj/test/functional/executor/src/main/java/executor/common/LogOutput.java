/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package executor.common;

import com.oracle.bedrock.Option;
import com.oracle.bedrock.OptionsByType;

import com.oracle.bedrock.runtime.Application;
import com.oracle.bedrock.runtime.ApplicationConsole;
import com.oracle.bedrock.runtime.MetaClass;
import com.oracle.bedrock.runtime.Platform;
import com.oracle.bedrock.runtime.Profile;

import com.oracle.bedrock.runtime.console.FileWriterApplicationConsole;

import com.oracle.bedrock.runtime.java.options.SystemProperties;
import com.oracle.bedrock.runtime.java.options.SystemProperty;

import com.oracle.bedrock.runtime.options.Console;
import com.oracle.bedrock.runtime.options.Discriminator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * {@link Option} that allows finer control over log destination that leverages
 * a {@link Discriminator} to calculate the target log location.
 *
 * @author  rl 8.4.2021
 * @since 21.12
 */
public class LogOutput
        implements Profile, Option
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a new {@code LogOutput} for the specified label and log name.
     *
     * Log contents will be written to /target/test-output/functional/{sLabel}/{sLogName}.log
     *
     * @param sLabel    the subdirectory that all related log files will be written to
     * @param sLogName  the name of the log file without an extension
     */
    protected LogOutput(String sLabel, String sLogName)
        {
        Objects.requireNonNull(sLabel,   "Must specify a label for the log file(s)");
        Objects.requireNonNull(sLogName, "Must specify the name of the log");

        f_sLabel   = sLabel;
        f_sLogName = sLogName;
        }

    // ----- factories ------------------------------------------------------

    /**
     * Obtains a {@code LogOutput} for the specified label and log name.
     *
     * Log contents will be written to /target/test-output/functional/{sLabel}/{sLogName}.log
     *
     * @param sLabel    the subdirectory that all related log files will be written to
     * @param sLogName  the name of the log file without an extension
     */
    public static LogOutput to(String sLabel, String sLogName)
        {
        return new LogOutput(sLabel, sLogName);
        }

    // ----- Profile interface ----------------------------------------------

    @Override
    public void onLaunching(Platform platform, MetaClass metaClass, OptionsByType optionsByType)
        {
        SystemProperties systemProperties = optionsByType.get(SystemProperties.class);

        if (systemProperties != null)
            {
            // include the discriminator in the name
            Discriminator discriminator = optionsByType.get(Discriminator.class);
            String sDiscriminatedName   = (discriminator != null ? '-' + discriminator.getValue() : "");

            String sLogName = f_sLogName + sDiscriminatedName + ".log";
            File   logDest  = new File(getBaseLoggingDirectory(), sLogName);

            try
                {
                if (logDest.exists())
                    {
                    int nCount = 1;
                    do
                        {
                        sLogName = f_sLogName + sDiscriminatedName + "-restart-" + nCount++ + ".log";
                        logDest  = new File(getBaseLoggingDirectory(), sLogName);
                        }
                    while (logDest.exists());
                    }

                if (!logDest.createNewFile())
                    {
                    throw new IllegalStateException("Unable to create log file [" + logDest.getAbsolutePath() + ']');
                    }

                FileWriter         writer  = new FileWriter(logDest, false);
                ApplicationConsole console = new FileWriterApplicationConsole(writer);

                // hold a reference so we can close it later
                m_colConsoles.add(console);

                optionsByType.add(Console.of(console));
                }
            catch (IOException e)
                {
                e.printStackTrace();
                }

            optionsByType.add(SystemProperty.of(PROPERTY, logDest.getAbsolutePath()));
            }
        }

    @Override
    public void onLaunched(Platform platform, Application application, OptionsByType optionsByType)
        {
        // no-op
        }

    @Override
    public void onClosing(Platform platform, Application application, OptionsByType optionsByType)
        {
        if (!m_colConsoles.isEmpty())
            {
            // close any consoles that were saved
            m_colConsoles.forEach(ApplicationConsole::close);
            m_colConsoles.clear();
            }
        }

    // ----- Object methods ------------------------------------------------

    @Override
    public boolean equals(Object o)
        {
        if (this == o)
            {
            return true;
            }
        if (o == null || getClass() != o.getClass())
            {
            return false;
            }
        LogOutput logOutput = (LogOutput) o;
        return f_sLabel.equals(logOutput.f_sLabel) && f_sLogName.equals(logOutput.f_sLogName);
        }

    @Override
    public int hashCode()
        {
        return Objects.hash(f_sLabel, f_sLogName);
        }

    @Override
    public String toString()
        {
        return "LogOutput{" +
               "Label='" + f_sLabel + '\'' +
               ", LogName='" + f_sLogName + '\'' +
               '}';
        }

    // ----- helper methods ------------------------------------------------

    /**
     * Return a {@link File} representing the base logging directory.
     *
     * @return a {@link File} representing the base logging directory
     */
    protected File getBaseLoggingDirectory()
        {
        File project = new File(LogOutput.class.getProtectionDomain().getCodeSource().getLocation().getFile());
        File logDir  = new File(project.getParentFile(), "test-output" + File.separatorChar
                                                         + "functional" + File.separatorChar + f_sLabel);

        if (!logDir.exists())
            {
            if (!logDir.mkdirs())
                {
                throw new IllegalStateException("Unable to create directory [" + logDir.getAbsolutePath() + ']');
                }
            }
        return logDir;
        }

    // ----- constants -----------------------------------------------------

    /**
     * The tangosol.coherence.log property.
     */
    protected static final String PROPERTY = "coherence.log";

    // ----- data members --------------------------------------------------

    /**
     * Subdirectory under which log files will be stored.
     */
    protected final String f_sLabel;

    /**
     * The name of the log file without extension.
     */
    protected final String f_sLogName;

    /**
     * Collection of {@link ApplicationConsole} that may have been created.
     */
    protected List<ApplicationConsole> m_colConsoles = new ArrayList<>();
    }
