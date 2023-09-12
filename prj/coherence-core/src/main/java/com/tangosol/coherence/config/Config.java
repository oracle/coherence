/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config;

import com.oracle.coherence.common.util.Duration;

import com.oracle.coherence.common.util.MemorySize;
import java.util.function.Supplier;

/**
 * {@link Config} is a helper class for processing a Coherence configuration
 * system properties.
 * <p>
 * As of Coherence 12.2.1, all Coherence system properties start with <code>coherence.</code>.
 * There is backwards compatibility support that if the property is not found,
 * then the property is looked up again with <code>tansogol.</code> prepended and
 * lastly by replacing the <code>coherence.</code> in the system property name to
 * support Coherence system property naming conventions prior to Coherence 12.2.1.
 * <p>
 * Note:  These methods should only be used when the system property name may begin with "coherence.*" or "tangosol.*".
 * The native method for looking up a system or environment property should be used to lookup up a OS/Language
 * native property.
 *
 * @author jf 2015.04.21
 * @since Coherence 12.2.1
 */
public abstract class Config
    {
    // ----- Config methods -------------------------------------------------

    /**
     * Get the value of Coherence property <code>sName</code>
     * <p>
     * This implementation differs from {@link System#getProperty(String)} that a
     * {@link java.lang.SecurityException} is handled and logged as a warning
     * and null is returned as the property's value.
     * <p>
     * Backwards compatibility support is described in {@link Config}.
     *
     * @param sName  Coherence system property name beginning with <code>coherence.</code>
     *
     * @return value of property <code>sName</code> or null if property lookup
     *         fails or property does not exist
     */
    public static String getProperty(String sName)
        {
        return getPropertyInternal(sName, SYS_PROPS, ENV_VARS);
        }

    /**
     * Get a Coherence property value, return default if property lookup fails.
     * <p>
     * Backwards compatibility support is described in {@link Config}.
     *
     * @param sName     Coherence system property name beginning with <code>coherence.</code>
     * @param sDefault  default value returned if property lookup fails
     *
     * @return value of property <code>sName</code>, or <code>sDefault</code> if
     *         property lookup fails or no property defined
     */
    public static String getProperty(String sName, String sDefault)
        {
        String sValue = getProperty(sName);

        return sValue == null ? sDefault : sValue;
        }

    /**
     * Get a Coherence property value, returning the value provided by the supplier
     * if property lookup fails.
     * <p>
     * Backwards compatibility support is described in {@link Config}.
     *
     * @param sName       Coherence system property name beginning with <code>coherence.</code>
     * @param supDefault  the supplier that provides a default value if property lookup fails
     *
     * @return value of property <code>sName</code>, or value provided by the <code>supDefault</code>
     *         if property lookup fails or no property defined
     */
    public static String getProperty(String sName, Supplier<String> supDefault)
        {
        String sValue = getProperty(sName);

        return sValue == null ? supDefault.get() : sValue;
        }

    /**
     * Returns true if coherence system property <code>sName</code> exists and
     * value is equal to string true.
     * <p>
     * Backwards compatibility support is described in {@link Config}.
     *
     * @param sName  Coherence system property name beginning with <code>coherence.</code>
     *
     * @return <code>true</code> if system property exists and equal to true
     */
    public static boolean getBoolean(String sName)
        {
        return getBoolean(sName, false);
        }

    /**
     * Return true if property <code>sName</code> exists and its value is string true.
     * <p>
     * Backwards compatibility support is described in {@link Config}.
     *
     * @param sName     Coherence system property name beginning with <code>coherence.</code>
     * @param fDefault  default value if property value lookup or conversion fails.
     *
     * @return true if <code>sName</code> exists and its value is string true; otherwise,
     * return <code>sDefault</code>.
     */
    public static boolean getBoolean(String sName, boolean fDefault)
        {
        String sValue = getProperty(sName);
        return sValue == null ? fDefault : Boolean.parseBoolean(sValue);
        }

    /**
     * Return Coherence system property value as an Integer.
     * <p>
     * Backwards compatibility support is described in {@link Config}.
     *
     * @param sName  Coherence system property name beginning with <code>coherence.</code>
     *
     * @return property value as integer if  property lookup and conversion
     *         of the String value to integer succeeds; otherwise, return null
     */
    public static Integer getInteger(String sName)
        {
        String sValue = getProperty(sName);
        try
            {
            return sValue == null ? null : Integer.parseInt(sValue);
            }
        catch (RuntimeException e)
            {
            return null;
            }
        }

    /**
     * Return Coherence system property value as an Integer.
     * <p>
     * Backwards compatibility support is described in {@link Config}.
     *
     * @param sName     Coherence system property name beginning with <code>coherence.</code>
     * @param iDefault  integer default value
     *
     * @return property value as integer if property lookup and conversion
     *         of the String value to integer succeeds; otherwise, return <code>iDefault</code>
     */
    public static Integer getInteger(String sName, int iDefault)
        {
        Integer i = getInteger(sName);

        return i == null ? Integer.valueOf(iDefault) : i;
        }

    /**
     * Return Coherence system property value as a Long.
     * <p>
     * Backwards compatibility support is described in {@link Config}.
     *
     * @param sName  Coherence system property name beginning with <code>coherence.</code>
     *
     * @return property value as long if property lookup and conversion
     *         of the String value to long succeeds; otherwise, return null
     */
    public static Long getLong(String sName)
        {
        String sValue = getProperty(sName);
        try
            {
            return sValue == null ? null : Long.parseLong(getProperty(sName));
            }
        catch (RuntimeException e)
            {
            return null;
            }
        }

    /**
     * Return Coherence system property value as a long.
     * <p>
     * Backwards compatibility support is described in {@link Config}.
     *
     * @param sName     Coherence system property name beginning with <code>coherence.</code>
     * @param lDefault  long default value
     *
     * @return property value as long if property lookup and conversion
     *         of the String value to long succeeds; otherwise, return <code>lDefault</code>
     */
    public static Long getLong(String sName, long lDefault)
        {
        Long l = getLong(sName);

        return l == null ? Long.valueOf(lDefault) : l;
        }

    /**
     * Return Coherence system property value as a Float.
     * <p>
     * Backwards compatibility support is described in {@link Config}.
     *
     * @param sName  Coherence system property name beginning with <code>coherence.</code>
     *
     * @return property value as float if property lookup and conversion
     *         of the String value to float succeeds; otherwise, return null
     */
    public static Float getFloat(String sName)
        {
        String sValue = getProperty(sName);
        try
            {
            return sValue == null ? null : Float.parseFloat(getProperty(sName));
            }
        catch (RuntimeException e)
            {
            return null;
            }
        }

    /**
     * Return Coherence system property value as a float.
     * <p>
     * Backwards compatibility support is described in {@link Config}.
     *
     * @param sName     Coherence system property name beginning with <code>coherence.</code>
     * @param fDefault  float default value
     *
     * @return property value as long if property lookup and conversion
     *         of the String value to float succeeds; otherwise, return <code>fDefault</code>
     */
    public static Float getFloat(String sName, float fDefault)
        {
        Float d = getFloat(sName);

        return d == null ? fDefault : d;
        }

    /**
     * Return Coherence system property value as a Double.
     * <p>
     * Backwards compatibility support is described in {@link Config}.
     *
     * @param sName  Coherence system property name beginning with <code>coherence.</code>
     *
     * @return property value as double if property lookup and conversion
     *         of the String value to double succeeds; otherwise, return null
     */
    public static Double getDouble(String sName)
        {
        String sValue = getProperty(sName);
        try
            {
            return sValue == null ? null :  Double.parseDouble(sValue);
            }
        catch (RuntimeException e)
            {
            return null;
            }
        }

    /**
     * Return Coherence system property value as a double.
     * <p>
     * Backwards compatibility support is described in {@link Config}.
     *
     * @param sName     Coherence system property name beginning with <code>coherence.</code>
     * @param dDefault  double default value
     *
     * @return property value as double if property lookup and conversion
     *         of the String value to double succeeds; otherwise, return <code>dDefault</code>
     */
    public static Double getDouble(String sName, double dDefault)
        {
        Double d = getDouble(sName);

        return d == null ? dDefault : d;
        }

    /**
     * Return Coherence system property value as a {@link Duration}.
     * <p>
     * Backwards compatibility support is described in {@link Config}.
     *
     * @param sName  Coherence system property name beginning with <code>coherence.</code>
     *
     * @return property value as {@link Duration} if property lookup and conversion
     *         of the String value to {@link Duration} succeeds;
     *         otherwise, return null
     */
    public static Duration getDuration(String sName)
        {
        String sValue = getProperty(sName);
        try
            {
            return sValue == null || sValue.isEmpty() ? null :  new Duration(sValue);
            }
        catch (RuntimeException e)
            {
            return null;
            }
        }

    /**
     * Return Coherence system property value as a {@link Duration}.
     * <p>
     * Backwards compatibility support is described in {@link Config}.
     *
     * @param sName     Coherence system property name beginning with <code>coherence.</code>
     * @param dDefault  default {@link Duration} value
     *
     * @return property value as {@link Duration} if property lookup and conversion
     *         of the String value to {@link Duration} succeeds; otherwise,
     *         return <code>dDefault</code>
     */
    public static Duration getDuration(String sName, Duration dDefault)
        {
        Duration d = getDuration(sName);

        return d == null ? dDefault : d;
        }

    /**
     * Return Coherence system property value as a {@link MemorySize}.
     * <p>
     * Backwards compatibility support is described in {@link Config}.
     *
     * @param sName  Coherence system property name beginning with <code>coherence.</code>
     *
     * @return property value as {@link MemorySize} if property lookup and conversion
     *         of the String value to {@link MemorySize} succeeds;
     *         otherwise, return null
     *
     * @since 23.09
     */
    public static MemorySize getMemorySize(String sName)
        {
        String sValue = getProperty(sName);
        try
            {
            return sValue == null || sValue.isEmpty() ? null : new MemorySize(sValue);
            }
        catch (RuntimeException e)
            {
            return null;
            }
        }

    /**
     * Return Coherence system property value as a {@link MemorySize}.
     * <p>
     * Backwards compatibility support is described in {@link Config}.
     *
     * @param sName     Coherence system property name beginning with <code>coherence.</code>
     * @param sDefault  default {@link MemorySize} value as string
     *
     * @return property value as {@link MemorySize} if property lookup and conversion
     *         of the String value to {@link MemorySize} succeeds; otherwise,
     *         return the specified default
     *
     * @since 23.09
     */
    public static MemorySize getMemorySize(String sName, String sDefault)
        {
        MemorySize d = getMemorySize(sName);

        return d == null ? new MemorySize(sDefault) : d;
        }

    /**
     * Coherence enhanced system environment getter
     * Use instead of {@link System#getenv(String)}.
     *
     * @param sName  Coherence system environment property name
     *
     * @return value for system environment property if it exists or null
     */
    public static String getenv(String sName)
        {
        return getEnvInternal(sName, ENV_VARS);
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Get the value of Coherence property <code>sName</code>
     * <p>
     * This implementation differs from {@link System#getProperty(String)} that a
     * {@link java.lang.SecurityException} is handled and logged as a warning
     * and null is returned as the property's value.
     * <p>
     * Backwards compatibility support is described in {@link Config}.
     *
     * @param sName     Coherence system property name beginning with <code>coherence.</code>
     * @param sysProps  the {@link SystemPropertyResolver} that will resolve system properties
     * @param envVars   the {@link EnvironmentVariableResolver} to resolve environment variables
     *
     * @return value of property <code>sName</code> or null if property lookup
     *         fails or property does not exist
     */
    static String getPropertyInternal(String sName, SystemPropertyResolver sysProps, EnvironmentVariableResolver envVars)
        {
        String sValue = sysProps.getProperty(sName);

        if (sValue == null)
            {
            if (sName.startsWith("coherence."))
                {
                // initial property lookup failed.
                // trying using alternative coherence system property naming convention.
                sValue = getPropertyBackwardsCompatibleMode(sName, sysProps);
                }
            else if (sName.startsWith("tangosol.coherence."))
                {
                // handle cases that sName is still following pre 12.2.1 coherence
                // system property conventions, try 12.2.1 naming convention.
                sValue = sysProps.getProperty(sName.replaceFirst("tangosol.", ""));
                }
            else if (sName.startsWith("tangosol."))
                {
                sValue = sysProps.getProperty(sName.replaceFirst("tangosol", "coherence"));
                }
            }

        // if System property not found try environment variable
        if (sValue == null)
            {
            sValue = getEnvInternal(sName, envVars);
            }

        return sValue;
        }

    /**
     * Coherence enhanced system environment getter
     * Use instead of {@link System#getenv(String)}.
     *
     * @param sName    Coherence system environment property name
     * @param envVars  the {@link EnvironmentVariableResolver} to resolve environment variables
     *
     * @return value for system environment property if it exists or null
     */
    static String getEnvInternal(String sName, EnvironmentVariableResolver envVars)
        {
        String sValue = envVars.getEnv(sName);

        if (sValue == null)
            {
            // initial property lookup failed.
            // trying using alternative coherence system property naming convention.
            if (sName.startsWith("coherence."))
                {
                sValue = getEnvironmentBackwardsCompatibleMode(sName, envVars);
                }
            else if (sName.startsWith("tangosol.coherence."))
                {
                // handle cases that sName is still following pre 12.2.1 coherence
                // system property conventions, try 12.2.1 naming convention.
                sValue = envVars.getEnv(sName.replaceFirst("tangosol.", ""));
                }
            else if (sName.startsWith("tangosol."))
                {
                sValue = envVars.getEnv(sName.replaceFirst("tangosol", "coherence"));
                }
            }

        if (sValue == null)
            {
            // try uppercase with underscores
            String sNameUpper = sName.toUpperCase().replaceAll("\\.", "_");
            sValue = envVars.getEnv(sNameUpper);

            if (sValue == null)
                {
                // initial property lookup failed.
                // trying using alternative coherence system property naming convention.
                if (sNameUpper.startsWith("COHERENCE_"))
                    {
                    // check for tangosol.coherence.* backwards compatibility
                    sValue = envVars.getEnv("TANGOSOL_" + sNameUpper);
                    return sValue == null
                            ? envVars.getEnv(sNameUpper.replaceFirst("COHERENCE", "TANGOSOL"))
                            : sValue;
                    }

                // handle cases that sName is still following pre 12.2.1 coherence
                // system property conventions, try 12.2.1 naming convention.
                if (sNameUpper.startsWith("TANGOSOL_COHERENCE_"))
                    {
                    return envVars.getEnv(sNameUpper.replaceFirst("TANGOSOL_", ""));
                    }
                else if (sNameUpper.startsWith("TANGOSOL_"))
                    {
                    return envVars.getEnv(sNameUpper.replaceFirst("TANGOSOL", "COHERENCE"));
                    }
                }
            }

        return sValue;
        }

    /**
     * Resolve sName using backwards compatibility rules.
     * Checks for backwards compatible properties "tangosol.coherence.*" and "tangosol.*".
     *
     * @param sName  Coherence system property name beginning with "coherence."
     * @param sysProps  the {@link SystemPropertyResolver} that will resolve system properties
     *
     * @return String for backwards compatibility conversion of sName or null if not defined
     *
     * @since Coherence 12.2.1
     */
    private static String getPropertyBackwardsCompatibleMode(String sName, SystemPropertyResolver sysProps)
        {
        // check for tangosol.coherence.* backwards compatibility
        String sValue = sysProps.getProperty("tangosol." + sName);

        return sValue == null ? sysProps.getProperty(sName.replaceFirst("coherence", "tangosol")) : sValue;
        }

    /**
     * Resolve sName using backwards compatibility rules.
     * Checks for backwards compatible properties "tangosol.coherence.*" and "tangosol.*".
     *
     * @param sName    Coherence system property name beginning with "coherence."
     * @param envVars  the {@link EnvironmentVariableResolver} to resolve environment variables
     *
     * @return String for backwards compatibility conversion of sName or null if not defined
     *
     * @since Coherence 12.2.1
     */
    private static String getEnvironmentBackwardsCompatibleMode(String sName, EnvironmentVariableResolver envVars)
        {
        // check for tangosol.coherence.* backwards compatibility
        String sValue = envVars.getEnv("tangosol." + sName);

        return sValue == null ? envVars.getEnv(sName.replaceFirst("coherence", "tangosol")) : sValue;
        }

    
    // ----- static members -------------------------------------------------

    /**
     * {@link SystemPropertyResolver} to use.
     */
    private static final SystemPropertyResolver SYS_PROPS
            = SystemPropertyResolver.getInstance();

    /**
     * {@link EnvironmentVariableResolver} to use.
     */
    private static final EnvironmentVariableResolver ENV_VARS
            = EnvironmentVariableResolver.getInstance();
    }
