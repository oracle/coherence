/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.cluster;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.net.Cluster;
import com.tangosol.net.GuardSupport;
import com.tangosol.net.Guardable;
import com.tangosol.net.Guardian.GuardContext;
import com.tangosol.net.Service;
import com.tangosol.net.ServiceFailurePolicy;

import com.tangosol.util.Base;
import com.tangosol.util.ClassHelper;

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
        String sHeader = "Coherence <Error>: ";

        switch (getPolicyType())
            {
            default:
            case POLICY_EXIT_CLUSTER:
                {
                System.err.println(sHeader +
                    "Halting this cluster node due to unrecoverable service failure");
                System.err.flush();

                try
                    {
                    // halt the cluster
                    ClassHelper.invoke(cluster, "halt", ClassHelper.VOID);
                    }
                catch (Exception e)
                    {
                    throw Base.ensureRuntimeException(e, "Unexpected exception while halting the cluster.");
                    }
                finally
                    {
                    // only after halting, print the cluster state
                    System.err.println(sHeader + "Halted the cluster:\n" + cluster.toString());
                    System.err.flush();
                    }
                }
                break;

            case POLICY_EXIT_PROCESS:
                System.err.println(sHeader +
                    "Halting JVM due to unrecoverable service failure");
                System.err.flush();
                Runtime.getRuntime().halt(-1);
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
    * This policy will cause the JVM to terminate if this service fails.
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
