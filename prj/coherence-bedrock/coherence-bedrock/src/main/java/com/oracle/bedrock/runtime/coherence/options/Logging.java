/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.bedrock.runtime.coherence.options;

import com.oracle.bedrock.ComposableOption;
import com.oracle.bedrock.Option;
import com.oracle.bedrock.OptionsByType;
import com.oracle.bedrock.runtime.Application;
import com.oracle.bedrock.runtime.MetaClass;
import com.oracle.bedrock.runtime.Platform;
import com.oracle.bedrock.runtime.Profile;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.java.options.SystemProperties;
import com.oracle.bedrock.runtime.java.options.SystemProperty;
import com.oracle.coherence.common.base.Logger;

public class Logging
        implements Profile, ComposableOption<Logging>
    {
    /**
     * The tangosol.coherence.log property.
     */
    public static final String PROPERTY = "coherence.log";

    /**
     * The tangosol.coherence.log.level property.
     */
    public static final String PROPERTY_LEVEL = "coherence.log.level";

    /**
     * The logging destination (null if not set).
     */
    private String destination;

    /**
     * The logging level (null if not set).
     */
    private Integer level;


    /**
     * Constructs a {@link Logging} for the specified destination and level.
     *
     * @param destination the destination  (null means use default)
     * @param level       the level (null means use default)
     */
    private Logging(
            String destination,
            Integer level)
        {
        this.destination = destination;
        this.level = level;
        }


    /**
     * Obtains a {@link Logging} for a specified destination.
     *
     * @param destination the destination of the {@link Logging}
     * @return a {@link Logging} for the specified destination
     */
    public static Logging to(String destination)
        {
        return new Logging(destination, null);
        }


    /**
     * Obtains a {@link Logging} for the stderr.
     *
     * @return a {@link Logging} for the stderr
     */
    public static Logging toStdErr()
        {
        return new Logging("stderr", null);
        }


    /**
     * Obtains a {@link Logging} for the stdout.
     *
     * @return a {@link Logging} for the stdout
     */
    public static Logging toStdOut()
        {
        return new Logging("stdout", null);
        }


    /**
     * Obtains a {@link Logging} for the specified level.
     *
     * @param level the level of the {@link Logging}
     * @return a {@link Logging} for the specified level
     */
    public static Logging at(int level)
        {
        return new Logging(null, level);
        }

    /**
     * Obtains a {@link Logging} option to set logging to error level.
     *
     * @return a {@link Logging} option to set logging to error level
     */
    public static Logging atError()
        {
        return new Logging(null, Logger.ERROR);
        }

    /**
     * Obtains a {@link Logging} option to set logging to warning level.
     *
     * @return a {@link Logging} option to set logging to warning level
     */
    public static Logging atWarn()
        {
        return new Logging(null, Logger.WARNING);
        }

    /**
     * Obtains a {@link Logging} option to set logging to info level.
     *
     * @return a {@link Logging} option to set logging to info level
     */
    public static Logging atInfo()
        {
        return new Logging(null, Logger.INFO);
        }

    /**
     * Obtains a {@link Logging} option to set logging to config level.
     *
     * @return a {@link Logging} option to set logging to config level
     */
    public static Logging atConfig()
        {
        return new Logging(null, Logger.CONFIG);
        }

    /**
     * Obtains a {@link Logging} option to set logging to fine level.
     *
     * @return a {@link Logging} option to set logging to fine level
     */
    public static Logging atFine()
        {
        return new Logging(null, Logger.FINE);
        }

    /**
     * Obtains a {@link Logging} option to set logging to finer level.
     *
     * @return a {@link Logging} option to set logging to finer level
     */
    public static Logging atFiner()
        {
        return new Logging(null, Logger.FINER);
        }

    /**
     * Obtains a {@link Logging} option to set logging to finest level.
     *
     * @return a {@link Logging} option to set logging to finest level
     */
    public static Logging atFinest()
        {
        return new Logging(null, Logger.FINEST);
        }

    /**
     * Obtains a {@link Logging} option to set logging to the highest level.
     *
     * @return a {@link Logging} option to set logging to the highest level
     */
    public static Logging atMax()
        {
        return new Logging(null, 9);
        }


    @Override
    public void onLaunching(
            Platform platform,
            MetaClass metaClass,
            OptionsByType optionsByType)
        {
        SystemProperties systemProperties = optionsByType.get(SystemProperties.class);

        if (systemProperties != null && destination != null)
            {
            optionsByType.add(SystemProperty.of(PROPERTY, destination));
            }

        if (systemProperties != null && level != null)
            {
            optionsByType.add(SystemProperty.of(PROPERTY_LEVEL, level));
            }
        }


    @Override
    public void onLaunched(
            Platform platform,
            Application application,
            OptionsByType optionsByType)
        {
        }


    @Override
    public void onClosing(
            Platform platform,
            Application application,
            OptionsByType optionsByType)
        {
        }


    @Override
    public Logging compose(Logging other)
        {
        return new Logging(this.destination == null ? other.destination : this.destination,
                           this.level == null ? other.level : this.level);
        }


    @Override
    public boolean equals(Object o)
        {
        if (this == o)
            {
            return true;
            }

        if (!(o instanceof Logging))
            {
            return false;
            }

        Logging logging = (Logging) o;

        if (destination != null ? !destination.equals(logging.destination) : logging.destination != null)
            {
            return false;
            }

        return level != null ? level.equals(logging.level) : logging.level == null;

        }


    @Override
    public int hashCode()
        {
        int result = destination != null ? destination.hashCode() : 0;

        result = 31 * result + (level != null ? level.hashCode() : 0);

        return result;
        }


    @Override
    public String toString()
        {
        return "Logging(" +
                "destination='" + destination + '\'' +
                ", level=" + level +
                ')';
        }
    }
