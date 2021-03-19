/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.performance;

import com.tangosol.util.Base;

import javax.management.DynamicMBean;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * @author jk 2016.04.18
 */
public class JavaFlightRecorder
    {
    public synchronized static boolean ensureLoaded()
        {
        if (s_mapCommands != null)
            {
            return true;
            }

        if (DIAGNOSTIC_BEAN == null)
            {
            return false;
            }

        MBeanInfo          mBeanInfo             = DIAGNOSTIC_BEAN.getMBeanInfo();
        MBeanOperationInfo operationInfoUnlock   = null;
        MBeanOperationInfo operationInfoJFRCheck = null;
        MBeanOperationInfo operationInfoJFRStart = null;
        MBeanOperationInfo operationInfoJFRStop  = null;
        MBeanOperationInfo operationInfoJFRDump  = null;

        for (MBeanOperationInfo operationInfo : mBeanInfo.getOperations())
            {
            switch (operationInfo.getName())
                {
                case "vmUnlockCommercialFeatures":
                    operationInfoUnlock = operationInfo;
                    break;
                case "jfrCheck":
                    operationInfoJFRCheck = operationInfo;
                    break;
                case "jfrDump":
                    operationInfoJFRDump = operationInfo;
                    break;
                case "jfrStart":
                    operationInfoJFRStart = operationInfo;
                    break;
                case "jfrStop":
                    operationInfoJFRStop = operationInfo;
                    break;
                default:
                    break;
                }
            }

        boolean fFailure = false;

        if (operationInfoJFRCheck == null)
            {
            fFailure = true;
            Base.err("Unable to find jfr check command");
            }

        if (operationInfoJFRDump == null)
            {
            fFailure = true;
            Base.err("Unable to find jfr dump command");
            }

        if (operationInfoJFRStart == null)
            {
            fFailure = true;
            Base.err("Unable to find jfr start command");
            }

        if (operationInfoJFRStop == null)
            {
            fFailure = true;
            Base.err("Unable to find jfr stop command");
            }

        if (!fFailure)
            {
            HashMap<String, MBeanOperationInfo> map = new HashMap<>();

            if (operationInfoUnlock != null)
                {
                map.put("unlock", operationInfoUnlock);
                }
            map.put("check", operationInfoJFRCheck);
            map.put("start", operationInfoJFRStart);
            map.put("stop", operationInfoJFRStop);
            map.put("dump", operationInfoJFRDump);

            s_mapCommands = map;

            if (operationInfoUnlock != null)
                {
                return invokeUnlock() != FAILURE;
                }
            else
                {
                return true;
                }
            }

        return false;
        }

    private synchronized static Object invokeCommand(String sName, Object[] aoParams, String[] asSignature)
        {
        try
            {
            if (DIAGNOSTIC_BEAN != null)
                {
                return DIAGNOSTIC_BEAN.invoke(sName, aoParams, asSignature);
                }
            return FAILURE;
            }
        catch (Throwable e)
            {
            Base.err(String.format("Unable to invoke %s (%s / %s)", sName, Arrays.toString(aoParams), Arrays.toString(asSignature)));
            Base.err(e);
            return FAILURE;
            }
        }

    private synchronized static Object invokeSimpleCommand(String sName)
        {
        return invokeCommand(sName, new Object[0], new String[0]);
        }

    private synchronized static Object invokeUnlock()
        {
        MBeanOperationInfo operationInfo = s_mapCommands.get("unlock");

        return invokeSimpleCommand(operationInfo.getName());
        }

    private synchronized static Object record(String sName, int durationIsSeconds, String outputFile)
        {
        String             fileNameArg         = durationIsSeconds == 0 ? "" : "filename=\"" + outputFile + '"';
        String             commandLineArgument = String.format("name=%s delay=0s compress=true duration=%ds ", sName, durationIsSeconds, fileNameArg);
        MBeanOperationInfo operationInfo       = s_mapCommands.get("start");

        return invokeCommand(operationInfo.getName(),
                             new Object[]{new String[]{commandLineArgument}},
                             getSignature(operationInfo));
        }

    public synchronized static boolean recordFor(String sName, int cSeconds, String sOutputFile)
        {
        if (cSeconds < 0)
            {
            throw new IllegalArgumentException("Time must be positive (was " + cSeconds + ")");
            }

        String outputFileNameWithExtension = sOutputFile.endsWith(".jfr") ? sOutputFile : sOutputFile + ".jfr";

        return ensureLoaded() && record(sName, cSeconds, outputFileNameWithExtension) != FAILURE;
        }

    public synchronized static boolean recordFor(String sName, int cSeconds)
        {
        try
            {
            Path pathTemp = Files.createTempFile("jfr-recordings", ".jfr");

            return recordFor(sName, cSeconds, pathTemp.toFile().getCanonicalPath());
            }
        catch (IOException e)
            {
            throw new RuntimeException(e);
            }
        }


    public synchronized static String stopRecording(String sName)
        {
        try
            {
            Path   tempPath = Files.createTempFile("jfr-recordings", ".jfr");
            String fileName = tempPath.toFile().getCanonicalPath();

            stopRecording(sName, fileName);

            return fileName;
            }
        catch (IOException e)
            {
            throw new RuntimeException(e);
            }
        }


    public synchronized static Object stopRecording(String sName, String sFileName)
        {
        StringBuilder sCommandLine = new StringBuilder("name=").append(sName)
                                                .append(" discard=false");

        if (sFileName != null && !sFileName.trim().isEmpty())
            {
            sCommandLine.append(" filename=\"" + sFileName + '"');
            }

        MBeanOperationInfo operationInfo = s_mapCommands.get("stop");

        return invokeCommand(operationInfo.getName(),
                             new Object[]{new String[]{sCommandLine.toString()}},
                             getSignature(operationInfo));
        }


    private static String[] getSignature(MBeanOperationInfo operationInfo)
        {
        MBeanParameterInfo[] aParameterInfo = operationInfo.getSignature();
        String[]             asTypes        = new String[aParameterInfo.length];

        for (int i = 0; i < aParameterInfo.length; i++)
            {
            asTypes[i] = aParameterInfo[i].getType();
            }

        return asTypes;
        }

    public synchronized static String checkRecordings()
        {
        if (ensureLoaded())
            {
            MBeanOperationInfo operationInfo = s_mapCommands.get("check");
            Object             oResult       = invokeCommand(operationInfo.getName(),
                                                             new Object[]{new String[]{"verbose=false"}},
                                                             getSignature(operationInfo));

            if (oResult != FAILURE && oResult instanceof String)
                {
                return (String) oResult;
                }
            }

        return "Unable to check recordings";
        }

    private static Class<?> getClassForName(String sName)
        {
        try
            {
            return JavaFlightRecorder.class.getClassLoader().loadClass(sName);
            }
        catch (ClassNotFoundException e)
            {
            Base.err("Cannot load class " + sName);
            return null;
            }
        }

    private static final Object FAILURE = new Object();

    private static final DynamicMBean DIAGNOSTIC_BEAN;

    private static Map<String, MBeanOperationInfo> s_mapCommands;

    static
        {
        final String className  = "sun.management.ManagementFactoryHelper";
        Class<?>     clz        = getClassForName(className);
        final String methodName = "getDiagnosticCommandMBean";
        DynamicMBean tmp        = null;

        if (clz != null)
            {
            try
                {
                Method m = clz.getMethod(methodName);

                m.setAccessible(true);

                tmp = (DynamicMBean) m.invoke(null);
                }
            catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException | ClassCastException e)
                {
                Base.err("Cannot call " + className + "#" + methodName);
                }
            }

        DIAGNOSTIC_BEAN = tmp;
        }
    }
