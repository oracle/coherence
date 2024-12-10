/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.cluster;

import com.oracle.coherence.common.base.Logger;
import com.oracle.coherence.common.util.Duration;

import com.tangosol.coherence.config.Config;

import com.tangosol.net.Cluster;
import com.tangosol.net.GuardSupport;
import com.tangosol.net.Guardable;
import com.tangosol.net.Guardian.GuardContext;
import com.tangosol.net.Service;
import com.tangosol.net.ServiceFailurePolicy;

import com.tangosol.util.Base;
import com.tangosol.util.ClassHelper;

import java.util.Timer;
import java.util.TimerTask;

/**
* DefaultServiceFailurePolicy is the default implementation of ServiceFailurePolicy.
*
* @author pfm
* @since  Coherence 3.7.1
*/
public class DefaultServiceFailurePolicy
        implements ServiceFailurePolicy
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a new DefaultServiceFailurePolicy and set the policy type.
     *
     * @param nPolicyType  the policy type
     */
    public DefaultServiceFailurePolicy(int nPolicyType)
        {
        m_nPolicyType = nPolicyType;
        }

    // ----- ServiceFailurePolicy interface ---------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public void onGuardableRecovery(Guardable guardable, Service service)
        {
         // Always log stack traces
         GuardSupport.logStackTraces();

         switch (getPolicyType())
             {
             default:
             case POLICY_EXIT_CLUSTER:
             case POLICY_EXIT_PROCESS:
                 // default policies simply delegate recover()
                 Logger.warn("Attempting recovery of " + guardable);
                 guardable.recover();
                 break;

             case POLICY_LOGGING:
                 // issue a synthetic heartbeat here that will cause the
                 // guardable to become "healthy" once again.  This allows
                 // the guardable to remain guarded after logging.
                 Logger.warn("Logging stacktrace due to soft-timeout");

                 // COH-3420: the guardable could have been already abandoned/released
                 //           by its guardian
                 GuardContext context = guardable.getContext();
                 if (context != null)
                     {
                     context.heartbeat();
                     }
                 break;
             }
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onGuardableTerminate(Guardable guardable, Service service)
        {
         // always log stack traces
         GuardSupport.logStackTraces();

         switch (getPolicyType())
             {
             default:
             case POLICY_EXIT_CLUSTER:
             case POLICY_EXIT_PROCESS:
                 // default policies simply delegate terminate()
                 Logger.warn("Terminating " + guardable);
                 guardable.terminate();
                 break;

             case POLICY_LOGGING:
                 // shouldn't be able to get here, as recovery should have
                 // issued a heartbeat to keep this guardable "healthy"
                 break;
             }
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onServiceFailed(Cluster cluster)
        {
        // Note: we are running on a post-termination thread
        String sHeader = " Oracle Coherence <Error>: ";
        switch (getPolicyType())
            {
            default:
            case POLICY_EXIT_CLUSTER:
                {
                System.err.println(getTimestamp() + sHeader +
                    "Halting this cluster node due to unrecoverable service failure");
                System.err.flush();

                try
                    {
                    // halt the cluster
                    ClassHelper.invoke(cluster, "halt", ClassHelper.VOID);
                    }
                catch (Exception e)
                    {
                    System.err.println(getTimestamp() + sHeader + "Unexpected exception while halting the cluster:\nStackTrace:\n"
                                       + Base.printStackTrace(e));
                    System.err.flush();
                    throw Base.ensureRuntimeException(e, "Unexpected exception while halting the cluster.");
                    }
                finally
                    {
                    // only after halting, print the cluster state
                    System.err.println(getTimestamp() + sHeader + "Halted the cluster: " + cluster.toString());
                    System.err.flush();
                    }
                }
                break;

            case POLICY_EXIT_PROCESS:
                System.err.println(getTimestamp() + sHeader +
                    "Exiting JVM due to unrecoverable service failure");
                System.err.flush();
                ensureExit(cluster, -1, getShutdownTimeout(cluster));
                break;
            }
        }

    // ----- DefaultServiceFailurePolicy methods ----------------------------

    /**
     * Return the policy type.
     *
     * @return the policy type
     */
    public int getPolicyType()
        {
        return m_nPolicyType;
        }

    /**
     * Return timestamp to use in System.err messages.
     *
     * @since Coherence 12.2.1.4.18
     */
    protected static String getTimestamp()
        {
        try
            {
            return Base.formatDateTime(Base.getSafeTimeMillis());
            }
        catch (Throwable ignored)
            {
            // ensure if exiting cluster due to OOM, that this does not prevent printing out log message
            return "";
            }
        }

    /**
    * Ensure this process exits the JVM, initially gracefully, with a timeout that triggers halting the process abruptly.
    * This method prevent a hung shutdown hook from preventing exiting the process.
    *
    * @param cluster   the cluster
    * @param status    the exit status, zero for OK, non-zero for error
    * @param cTimeout  in millis, if positive value, ensure exit process within this amount of time
    */
    private static void ensureExit(Cluster cluster, int status, long cTimeout)
        {
        String sHeader = " Oracle Coherence <Error>: ";

        try
            {
            if (cTimeout >= 0)
                {
                System.err.println(getTimestamp() + sHeader + "Schedule timer task to halt process in " +
                                   cTimeout + " millis");
                System.err.flush();

                Timer timer = new Timer();

                // ensure if graceful exit does not complete in cTimeout, abrupt halt of JVM
                timer.schedule(new TimerTask()
                    {
                    @Override
                    public void run()
                        {
                        System.err.println(getTimestamp() + sHeader + " Failed to exit gracefully after " +
                                           cTimeout + " millis, halting process");
                        System.err.flush();
                        Runtime.getRuntime().halt(status);
                        }
                    }, cTimeout);
                }
            System.err.println(getTimestamp() + sHeader + "Begin graceful exit from JVM ...");
            System.err.flush();
            System.exit(status);
            }
        catch (Throwable ex)
            {
            String sCause = ex.getCause() == null ? "" : " Cause: " + ex.getCause();
            System.err.println("Handled unexpected exception " + ex.getClass().getName() + ex.getMessage() + sCause);
            System.err.flush();
            Runtime.getRuntime().halt(status);
            }
        finally
            {
            // could get here if security exception thrown
            Runtime.getRuntime().halt(status);
            }
        }

    /**
     * Return the cluster shutdown timeout in millis.
     *
     * @param cluster  the cluster
     *
     * @returns the cluster shutdown timeout in millis
     *
     * @since 12.2.1.4.18
     */
    protected static long getShutdownTimeout(Cluster cluster)
        {
        try
            {
            Object oCluster           = ClassHelper.invoke(cluster, "getCluster", ClassHelper.VOID);
            Long   ldtShutdownTimeout = (Long) ClassHelper.invoke(oCluster, "getShutdownTimeout", ClassHelper.VOID);

            return ldtShutdownTimeout.longValue();
            }
        catch (Throwable ignore)
            {
            // unable to get shutdown timeout from Cluster so get it from same system property that Cluster uses
            return Config.getDuration("coherence.shutdown.timeout",
                                      new Duration(2, Duration.Magnitude.MINUTE)).as(Duration.Magnitude.MILLI);
            }
        }

    // ----- Object methods -------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
        {
        String sType;
        switch (getPolicyType())
            {
            case POLICY_EXIT_CLUSTER:
                sType = "exit cluster";
                break;

            case POLICY_EXIT_PROCESS:
                sType = "exit process";
                break;

            case POLICY_LOGGING:
                sType = "logging";
                break;

            default:
                sType = "unknown";
                break;
            }

        return "DefaultServiceFailurePolicy{PolicyType=" + sType + "}";
        }

    // ----- constants ------------------------------------------------------

    /**
    * This policy will cause the cluster service to be shut down if this service fails.
    */
    public static final int POLICY_EXIT_CLUSTER = 1;

    /**
    * This policy will allow the JVM to terminate gracefully if this service fails.
    * If {@link #getShutdownTimeout(Cluster) configured shutdown timeout} is exceeded, the JVM is then halted.
    *
    * See {@link #ensureExit(Cluster, int, long)}.
    */
    public static final int POLICY_EXIT_PROCESS = 2;

    /**
    * This policy will cause an error being logged if this service fails.
    */
    public static final int POLICY_LOGGING = 3;

    // ----- data members ---------------------------------------------------

    /**
     * The policy type specifies the action to take when an abnormally behaving service
     * cannot be terminated gracefully by the service guardian.
     */
    private int m_nPolicyType;

    /**
     * The default timeout to use with a "logging" policy.  This timeout is used when the logging policy
     *  is enforced as a result of a (mis)configured guardian-timeout of 0ms.
     */
    public static final int DEFAULT_LOGGING_INTERVAL = 60000;
    }
