/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package guardian;


import com.oracle.coherence.testing.junit.ThreadDumpOnTimeoutRule;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.CacheService;
import com.tangosol.net.Cluster;
import com.tangosol.net.Guardable;
import com.tangosol.net.management.MBeanHelper;

import com.tangosol.net.Guardian;
import com.tangosol.net.NamedCache;
import com.tangosol.net.Service;
import com.tangosol.net.ServiceFailurePolicy;
import com.tangosol.net.RequestTimeoutException;
import com.tangosol.net.ServiceStoppedException;

import com.tangosol.util.Base;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.processor.AbstractProcessor;

import java.net.ServerSocket;

import java.util.Enumeration;
import java.util.Properties;

import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.tangosol.coherence.component.util.Daemon;
import com.tangosol.coherence.component.util.SafeService;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache;
import com.tangosol.internal.net.cluster.DefaultServiceFailurePolicy;

import java.util.concurrent.TimeUnit;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.*;

/**
* Test the Guardian.
*
* @author rhl 2010.03.17
*/
public class GuardianTests
    {

    // ----- test methods -------------------------------------------------

    /**
    * Test that the numbers of soft-timeout and hard-timeout are correctly recorded by the
    * GuardSoftTimeout and GuardHardTimeout attributes in the JMX.
    */
    @Test
    public void testGuardTimeoutDisplay()
        {
        logWarn("testGuardTimeoutDisplay", true);

        try
            {
            System.setProperty("test.guardian.GuardianTests.timeout", "3000");
            System.setProperty("test.guardian.GuardianTests.threads", "2");
            System.setProperty("test.guardian.GuardianTests.request.timeout", "15s");
            System.setProperty("coherence.log.level", "9");
            System.setProperty("coherence.management", "all");

            CacheService service = startService("PartitionedCacheDefaultPolicies");
            NamedCache   cache   = service.ensureCache("foo", null);
            Cluster      cluster = CacheFactory.getCluster();
            MBeanServer  server  = MBeanHelper.findMBeanServer();

            checkNodeGuardTimeout(cluster, server, "Recover", 0);
            checkNodeGuardTimeout(cluster, server, "Terminate", 0);

            // Note: the timeouts (60s) are significantly larger than the guardian
            //       timeout (3s) in the hope it will accommodate for late guardians

            // check soft-timeouts
            for (int i = 1; i <= 5; i++)
                {
                cache.invoke("SoftKey-" + i, new LoserTask(60000L, /*fInterruptible*/ true));
                checkNodeGuardTimeout(cluster, server, "Recover", i);
                }

            // reset statistics
            resetNodeGuardTimeoutCount(cluster, server);
            checkNodeGuardTimeout(cluster, server, "Recover", 0);

            // check hard-timeouts
            for (int i = 1; i <= 5; i++)
                {
                try
                    {
                    cache.invoke("HardKey-" + i, new LoserTask(60000L, /*fInterruptible*/ false));
                    }
                catch (RuntimeException e)
                    {
                    }

                checkNodeGuardTimeout(cluster, server, "Recover", i);
                checkNodeGuardTimeout(cluster, server, "Terminate", i);
                }
            }
        finally
            {
            CacheFactory.shutdown();
            }

        logWarn("testGuardTimeoutDisplay", false);
        }

    /**
    * Test that guardian-timeout of shorter than an EntryProcessor task will
    * cause the EP to be interrupted.
    */
    @Test
    public void testRecovery()
        {
        CacheService               service;
        NamedCache                 cache;
        Object                     oResult;
        CustomServiceFailurePolicy policy;
        long                       cDelay = 10000L;

        logWarn("testRecovery", true);

        // test interruptible service-thread task
        try
            {
            System.setProperty("test.guardian.GuardianTests.timeout", "3000");
            System.setProperty("test.guardian.GuardianTests.threads", "0");

            service = startService("PartitionedCacheDefaultPolicies");
            cache   = service.ensureCache("foo", null);
            oResult = cache.invoke("key", new LoserTask(cDelay, /*fInterruptible*/ true));
            assertNotSame("Slow task was not interrupted after " + cDelay + "ms", 0, oResult);
            }
        finally
            {
            CacheFactory.shutdown();
            }

        // test interruptible worker-thread task
        try
            {
            System.setProperty("test.guardian.GuardianTests.timeout", "3000");
            System.setProperty("test.guardian.GuardianTests.threads", "2");

            service = startService("PartitionedCacheDefaultPolicies");
            cache   = service.ensureCache("foo", null);
            oResult = cache.invoke("key", new LoserTask(cDelay, /*fInterruptible*/ true));
            assertNotSame("Slow task was not interrupted after " + cDelay + "ms", 0, oResult);
            }
        finally
            {
            CacheFactory.shutdown();
            }

        // test custom policy recovery on service thread
        try
            {
            System.setProperty("test.guardian.GuardianTests.timeout", "3000");
            System.setProperty("test.guardian.GuardianTests.threads", "0");
            System.setProperty("test.guardian.GuardianTests.custompolicy.initparam0",
                               "" + CustomServiceFailurePolicy.TYPE_DEFAULT);

            service = startService("PartitionedCacheCustomPolicy");
            cache   = service.ensureCache("foo", null);
            oResult = cache.invoke("key", new LoserTask(cDelay, /*fInterruptible*/ true));
            policy  = getServicePolicy(service);

            assertNotSame("Slow task was not interrupted after " + cDelay + "ms", 0, oResult);
            assertNotNull(policy);
            assertEquals(1, policy.m_cRecover);
            assertEquals(0, policy.m_cTerminate);
            }
        finally
            {
            CacheFactory.shutdown();
            }

        // test custom policy recovery on worker thread
        try
            {
            System.setProperty("test.guardian.GuardianTests.timeout", "3000");
            System.setProperty("test.guardian.GuardianTests.threads", "2");
            System.setProperty("test.guardian.GuardianTests.custompolicy.initparam0",
                               "" + CustomServiceFailurePolicy.TYPE_DEFAULT);

            service = startService("PartitionedCacheCustomPolicy");
            cache   = service.ensureCache("foo", null);
            oResult = cache.invoke("key", new LoserTask(cDelay, /*fInterruptible*/ true));
            policy  = getServicePolicy(service);

            assertNotSame("Slow task was not interrupted after " + cDelay + "ms", 0, oResult);
            assertNotNull(policy);
            assertEquals(1, policy.m_cRecover);
            assertEquals(0, policy.m_cTerminate);
            }
        finally
            {
            CacheFactory.shutdown();
            }

        // test custom policy recovery on service thread
        try
            {
            System.setProperty("test.guardian.GuardianTests.timeout", "3000");
            System.setProperty("test.guardian.GuardianTests.threads", "0");
            System.setProperty("test.guardian.GuardianTests.custompolicy.initparam0",
                               "" + CustomServiceFailurePolicy.TYPE_SKIP_ONE);

            service = startService("PartitionedCacheCustomPolicy");
            cache   = service.ensureCache("foo", null);
            oResult = cache.invoke("key", new LoserTask(15000, /*fInterruptible*/ true));
            policy  = getServicePolicy(service);

            assertNotSame("Slow task was not interrupted after " + cDelay + "ms", 0, oResult);
            assertNotNull(policy);
            assertEquals(2, policy.m_cRecover);
            assertEquals(0, policy.m_cTerminate);
            }
        finally
            {
            CacheFactory.shutdown();
            }

        // test custom policy recovery on worker thread
        try
            {
            System.setProperty("test.guardian.GuardianTests.timeout", "3000");
            System.setProperty("test.guardian.GuardianTests.threads", "2");
            System.setProperty("test.guardian.GuardianTests.custompolicy.initparam0",
                               "" + CustomServiceFailurePolicy.TYPE_SKIP_ONE);

            service = startService("PartitionedCacheCustomPolicy");
            cache   = service.ensureCache("foo", null);
            oResult = cache.invoke("key", new LoserTask(15000, /*fInterruptible*/ true));
            policy  = getServicePolicy(service);

            assertNotSame("Slow task was not interrupted after " + cDelay + "ms", 0, oResult);
            assertNotNull(policy);
            assertEquals(2, policy.m_cRecover);
            assertEquals(0, policy.m_cTerminate);
            }
        finally
            {
            CacheFactory.shutdown();
            }

        // test custom policy recovery on worker thread, different task-timeout
        try
            {
            System.setProperty("test.guardian.GuardianTests.timeout", "60000");
            System.setProperty("test.guardian.GuardianTests.task.timeout", "3000");
            System.setProperty("test.guardian.GuardianTests.threads", "2");
            System.setProperty("test.guardian.GuardianTests.custompolicy.initparam0",
                               "" + CustomServiceFailurePolicy.TYPE_DEFAULT);

            service = startService("PartitionedCacheCustomPolicy");
            cache   = service.ensureCache("foo", null);
            oResult = cache.invoke("key", new LoserTask(15000, /*fInterruptible*/ true));
            policy  = getServicePolicy(service);

            assertNotSame("Slow task was not interrupted after " + cDelay + "ms", 0, oResult);
            assertNotNull(policy);
            assertEquals(1, policy.m_cRecover);
            assertEquals(0, policy.m_cTerminate);
            }
        finally
            {
            CacheFactory.shutdown();
            }

        logWarn("testRecovery", false);
        }

    /**
    * Test terminate
    */
    @Test
    public void testTerminate()
        {
        CacheService               service;
        NamedCache                 cache;
        CustomServiceFailurePolicy policy;
        long                       cDelay = 10000L;

        logWarn("testTerminate", true);

        // test terminate of a task on the service thread
        try
            {
            System.setProperty("test.guardian.GuardianTests.timeout", "3000");
            System.setProperty("test.guardian.GuardianTests.threads", "0");

            service = startService("PartitionedCacheDefaultPolicies");
            cache   = service.ensureCache("foo", null);

            try
                {
                // service termination will result in task retry
                cache.invoke("key", new LoserTask(cDelay, /*fInterruptible*/ false));
                }
            catch (ServiceStoppedException e)
                {
                }
            assertFalse("Service was not terminated", service.isRunning());
            }
        finally
            {
            CacheFactory.shutdown();
            }

        // test terminate of a task on a worker thread
        try
            {
            System.setProperty("test.guardian.GuardianTests.timeout", "3000");
            System.setProperty("test.guardian.GuardianTests.threads", "2");
            System.setProperty("test.guardian.GuardianTests.request.timeout", "15s");

            service = startService("PartitionedCacheDefaultPolicies");
            cache   = service.ensureCache("foo", null);

            try
                {
                // worker thread task termination should not
                cache.invoke("key", new LoserTask(cDelay, /*fInterruptible*/ false));
                }
            catch (RuntimeException e)
                {
                if (Base.getOriginalException(e) instanceof RequestTimeoutException)
                    {
                    // expected
                    }
                else
                    {
                    throw e;
                    }
                }
            assertTrue("Service was terminated", service.isRunning());
            }
        finally
            {
            CacheFactory.shutdown();
            }

        // test terminate of a task on a service thread with a custom policy
        try
            {
            System.setProperty("test.guardian.GuardianTests.timeout", "3000");
            System.setProperty("test.guardian.GuardianTests.threads", "0");
            System.setProperty("test.guardian.GuardianTests.custompolicy.initparam0",
                               "" + CustomServiceFailurePolicy.TYPE_DEFAULT);

            service = startService("PartitionedCacheCustomPolicy");
            cache   = service.ensureCache("foo", null);
            // pre-fetch the policy, as we expect the service to be terminated
            policy  = getServicePolicy(service);

            try
                {
                // service termination will result in task retry
                cache.invoke("key", new LoserTask(cDelay, /*fInterruptible*/ false));
                }
            catch (ServiceStoppedException e)
                {
                }

            assertNotNull(policy);
            assertEquals(1, policy.m_cRecover);
            assertEquals(1, policy.m_cTerminate);
            assertEquals(1, policy.m_cServiceFailed);
            assertFalse("Service was not terminated", service.isRunning());
            }
        finally
            {
            CacheFactory.shutdown();
            }

        logWarn("testTerminate", false);
        }

    /**
    * Test terminate using the inherited global settings from the cluster.
    */
    @Test
    public void testTerminateInheritGlobal()
        {
        CacheService               service;
        NamedCache                 cache;
        CustomServiceFailurePolicy policy;
        long                       cDelay = 10000L;

        logWarn("testTerminateInheritGlobal", true);

        // test terminate of a task on the service thread
        try
            {
            System.setProperty("coherence.override", "guardian-coherence-override.xml");
            System.setProperty("test.guardian.GuardianTests.global.timeout", "3000");
            System.setProperty("test.guardian.GuardianTests.global.policy", "exit-cluster");
            System.setProperty("test.guardian.GuardianTests.threads", "0");

            service = startService("PartitionedCacheDefaultPolicies");
            cache   = service.ensureCache("foo", null);

            try
                {
                // service termination will result in task retry
                cache.invoke("key", new LoserTask(cDelay, /*fInterruptible*/ false));
                }
            catch (ServiceStoppedException e)
                {
                }
            assertFalse("Service was not terminated", service.isRunning());
            }
        finally
            {
            System.clearProperty("coherence.override");
            System.clearProperty("test.guardian.GuardianTests.global.timeout");
            System.clearProperty("test.guardian.GuardianTests.global.policy");
            CacheFactory.shutdown();
            }

        // test terminate of a task on a worker thread
        try
            {
            System.setProperty("coherence.override", "guardian-coherence-override.xml");
            System.setProperty("test.guardian.GuardianTests.global.timeout", "3000");
            System.setProperty("test.guardian.GuardianTests.global.policy", "exit-cluster");
            System.setProperty("test.guardian.GuardianTests.threads", "2");
            System.setProperty("test.guardian.GuardianTests.request.timeout", "15s");

            service = startService("PartitionedCacheDefaultPolicies");
            cache   = service.ensureCache("foo", null);

            try
                {
                // worker thread task termination should not
                cache.invoke("key", new LoserTask(cDelay, /*fInterruptible*/ false));
                }
            catch (RuntimeException e)
                {
                if (Base.getOriginalException(e) instanceof RequestTimeoutException)
                    {
                    // expected
                    }
                else
                    {
                    throw e;
                    }
                }
            assertTrue("Service was terminated", service.isRunning());
            }
        finally
            {
            System.clearProperty("coherence.override");
            System.clearProperty("test.guardian.GuardianTests.global.timeout");
            System.clearProperty("test.guardian.GuardianTests.global.policy");
            CacheFactory.shutdown();
            }

        // test terminate of a task on a service thread with a custom policy
        try
            {
            System.setProperty("coherence.override", "guardian-coherence-override.xml");
            System.setProperty("test.guardian.GuardianTests.global.timeout", "3000");
            System.setProperty("test.guardian.GuardianTests.global.policy", "exit-cluster");
            System.setProperty("test.guardian.GuardianTests.threads", "0");
            System.setProperty("test.guardian.GuardianTests.custompolicy.initparam0",
                               "" + CustomServiceFailurePolicy.TYPE_DEFAULT);

            service = startService("PartitionedCacheCustomPolicy");
            cache   = service.ensureCache("foo", null);
            // pre-fetch the policy, as we expect the service to be terminated
            policy  = getServicePolicy(service);

            try
                {
                // service termination will result in task retry
                cache.invoke("key", new LoserTask(cDelay, /*fInterruptible*/ false));
                }
            catch (ServiceStoppedException e)
                {
                }

            assertNotNull(policy);
            Eventually.assertThat(invoking(policy).getRecoverCount(), is(1));
            Eventually.assertThat(invoking(policy).getTerminateCount(), is(1));
            Eventually.assertThat(invoking(policy).getServiceFailedCount(), is(1));
            assertFalse("Service was not terminated", service.isRunning());
            }
        finally
            {
            System.clearProperty("coherence.override");
            System.clearProperty("test.guardian.GuardianTests.global.timeout");
            System.clearProperty("test.guardian.GuardianTests.global.policy");
            CacheFactory.shutdown();
            }

        logWarn("testTerminateInheritGlobal", false);
        }

    /**
    * Test that guardian-timeout of 0 causes a default "logging"
    * policy to be used.  (COH-3090)
    */
    @Test
    public void testDefaultLogging()
        {
        logWarn("testDefaultLogging", true);

        Properties   props        = new Properties();
        final String sServiceName = "PartitionedCacheDefaultPolicies";
        try
            {
            props.setProperty("test.guardian.GuardianTests.timeout", "0");

            // test service-thread task
            props.setProperty("test.guardian.GuardianTests.threads", "0");
            doTestLogging(sServiceName, props, new Runnable()
                {
                public void run()
                    {
                    // re-register the guardable with a 3sec timeout
                    CacheService     service     = startService(sServiceName);
                    SafeService      serviceSafe = (SafeService) service;
                    PartitionedCache serviceReal = (PartitionedCache) serviceSafe.getService();

                    Base.out("Modifying guardian-timeout to 3 seconds");
                    Daemon.Guard guard = serviceReal.getGuardable();

                    Eventually.assertThat(invoking(guard).getContext(), is(notNullValue()));
                    Guardian.GuardContext ctx      = guard.getContext();
                    Guardian              guardian = ctx.getGuardian();
                    guardian.guard(guard, 3000L, 0.9F);
                    }
                });

            // test worker-thread task
            props.setProperty("test.guardian.GuardianTests.threads", "2");
            doTestLogging(sServiceName, props, new Runnable()
                {
                public void run()
                    {
                    // change the guard-timeout to 3seconds
                    CacheService     service     = startService(sServiceName);
                    SafeService      serviceSafe = (SafeService) service;
                    PartitionedCache serviceReal = (PartitionedCache) serviceSafe.getService();

                    Base.out("Modifying task-timeout to 3 seconds");
                    serviceReal.getDaemonPool().setTaskTimeout(3000L);
                    }
                });
            }
        finally
            {
            clearProps(props);
            }

        logWarn("testDefaultLogging", false);
        }

    /**
    * Test the "logging" policy.
    */
    @Test
    public void testLogging()
        {
        logWarn("testLogging", true);

        Properties props = new Properties();
        try
            {
            props.setProperty("test.guardian.GuardianTests.timeout", "3s");

            // test service thread
            doTestLogging("PartitionedCacheLoggingPolicy", props, null);
            props.setProperty("test.guardian.GuardianTests.threads", "0");

            // test worker thread
            props.setProperty("test.guardian.GuardianTests.threads", "2");
            doTestLogging("PartitionedCacheLoggingPolicy", props, null);
            }
        finally
            {
            clearProps(props);
            }

        logWarn("testLogging", false);
        }

    /**
    * Test the "logging" policy overriding a global policy.
    */
    @Test
    public void testLoggingOverridingGlobal()
        {
        logWarn("testLoggingOverridingGlobal", true);

        Properties props = new Properties();
        try
            {
            props.setProperty("coherence.override",
                              "guardian-coherence-override.xml");
            props.setProperty("test.guardian.GuardianTests.global.timeout", "60000");
            props.setProperty("test.guardian.GuardianTests.global.policy", "exit-cluster");
            props.setProperty("test.guardian.GuardianTests.timeout", "3s");

            // test service thread
            doTestLogging("PartitionedCacheLoggingPolicy", props, null);
            props.setProperty("test.guardian.GuardianTests.threads", "0");

            // test worker thread
            props.setProperty("test.guardian.GuardianTests.threads", "2");
            doTestLogging("PartitionedCacheLoggingPolicy", props, null);
            }
        finally
            {
            clearProps(props);
            }

        logWarn("testLoggingOverridingGlobal", false);
        }

    /**
    * Test the "logging" policy overriding a global policy.
    */
    @Test
    public void testInheritGlobalLogging()
        {
        logWarn("testInheritGlobalLogging", true);

        Properties props = new Properties();
        try
            {
            props.setProperty("coherence.override",
                              "guardian-coherence-override.xml");
            props.setProperty("test.guardian.GuardianTests.global.timeout", "3000");
            props.setProperty("test.guardian.GuardianTests.global.policy", "logging");

            // test service thread
            doTestLogging("PartitionedCacheNoPolicy", props, null);
            props.setProperty("test.guardian.GuardianTests.threads", "0");

            // test worker thread
            props.setProperty("test.guardian.GuardianTests.threads", "2");
            doTestLogging("PartitionedCacheNoPolicy", props, null);
            }
        finally
            {
            clearProps(props);
            }

        logWarn("testInheritGlobalLogging", false);
        }

    /**
    * Clear the specified System properties.
    *
    * @param props  the properties to remove
    */
    protected void clearProps(Properties props)
        {
        for (Enumeration enumNames = props.propertyNames(); enumNames.hasMoreElements();)
            {
            System.clearProperty((String) enumNames.nextElement());
            }
        }


    /**
    * Helper method for testing "logging" configuration
    */
    protected void doTestLogging(String sServiceName, Properties props, Runnable runnable)
        {
        CacheService service;
        NamedCache   cache;
        Object       oResult;

        // test service-thread task
        try
            {
            System.getProperties().putAll(props);

            service = startService(sServiceName);

            SafeService      serviceSafe = (SafeService) service;
            PartitionedCache serviceReal = (PartitionedCache) serviceSafe.getService();
            // check to verify that the policy type is logging

            DefaultServiceFailurePolicy oPolicy = (DefaultServiceFailurePolicy) serviceReal.getServiceFailurePolicy();
            int                         oType   = oPolicy.getPolicyType();

            // see Cluster$DefaultFailurePolicy.POLICY_LOGGING
            assertEquals(3, oType);

            if (runnable != null)
                {
                runnable.run();
                }

            // Note: send an invocable with a duration that is longer than
            //       the timeout, but not too long (so as to cause request-timeout)
            //       as the task will not be recovered/interrupted (just logging)
            cache   = service.ensureCache("foo", null);
            oResult = cache.invoke("key", new LoserTask(5000, /*fInterruptible*/ true));

            assertEquals("Logging policy should not result in recovery",
                         0, oResult);
            }
        finally
            {
            CacheFactory.shutdown();
            }
        }

    // ----- inner class: CustomServiceFailurePolicy ----------------------

    /**
    * Instantiate a CustomServiceFailurePolicy of the specified type.
    *
    * @param nPolicyType  the policy type
    *
    * @return a CustomServiceFailurePolicy
    */
    public static ServiceFailurePolicy instantiateCustomPolicy(int nPolicyType)
        {
        return new CustomServiceFailurePolicy(nPolicyType);
        }

    /**
    *
    */
    protected static class CustomServiceFailurePolicy
            implements ServiceFailurePolicy
        {
        /**
        * Construct a CustomServiceFailurePolicy of the specified type
        *
        * @param nPolicyType  the policy type
        */
        public CustomServiceFailurePolicy(int nPolicyType)
            {
            m_nPolicyType = nPolicyType;
            }

        // ----- ServiceFailurePolicy methods ----------------------------

        /**
        * {@inheritDoc}
        */
        public void onGuardableRecovery(Guardable guardable, Service service)
            {
            Base.out("CustomServiceFailurePolicy: onGuardableRecovery() " + guardable);
            ++m_cRecover;
            switch (m_nPolicyType)
                {
                default:
                case TYPE_DEFAULT:
                    guardable.recover();
                    break;

                case TYPE_SKIP_ONE:
                    if (m_cRecover > 1)
                        {
                        guardable.recover();
                        }
                    else
                        {
                        guardable.getContext().heartbeat();
                        }
                    break;
                }
            }

        /**
        * {@inheritDoc}
        */
        public void onGuardableTerminate(Guardable guardable, Service service)
            {
            Base.out("CustomServiceFailurePolicy: onGuardableTerminate() " + guardable);
            ++m_cTerminate;
            switch (m_nPolicyType)
                {
                default:
                case TYPE_DEFAULT:
                case TYPE_SKIP_ONE:
                    guardable.terminate();
                    break;
                }
            }

        /**
        * {@inheritDoc}
        */
        public void onServiceFailed(Cluster cluster)
            {
            ++m_cServiceFailed;
            }

        // ----- accessors ------------------------------------------------

        public int getRecoverCount()
            {
            return m_cRecover;
            }

        public int getTerminateCount()
            {
            return m_cTerminate;
            }

        public int getServiceFailedCount()
            {
            return m_cServiceFailed;
            }

        // ----- constants and data members ------------------------------

        /**
        * Default policy type (delegate recover() and terminate())
        */
        public static final int TYPE_DEFAULT  = 0;

        /**
        * Policy type that skips one recovery period (soft-timeout)
        */
        public static final int TYPE_SKIP_ONE = 1;

        /**
        * The policy type
        */
        protected int m_nPolicyType;

        /**
        * The number of times this policy has been asked to recover a guardable
        */
        protected int m_cRecover;

        /**
        * The number of times this policy has been asked to terminate a guardable
        */
        protected int m_cTerminate;

        /**
        * The number of times this policy has been asked to handle a failed
        * service termination.
        */
        protected int m_cServiceFailed;
        }


    // ----- inner class: LoserTask ---------------------------------------

    /**
    * LoserTask is the entry processor used to simulate a long-running (hung) task.
    */
    protected static class LoserTask
            extends AbstractProcessor
        {
        public LoserTask(long cDelay, boolean fInterruptible)
            {
            m_cDelay         = cDelay;
            m_fInterruptible = fInterruptible;
            }

        // ----- EntryProcessor methods -----------------------------------

        /**
        * {@inheritDoc}
        */
        public Object process(InvocableMap.Entry entry)
            {
            long cWait      = m_cDelay;
            int  cInterrupt = 0;
            long ldtNow     = Base.getSafeTimeMillis();
            while (cWait >= 0)
                {
                long ldtLast = ldtNow;
                try
                    {
                    Thread.sleep(cWait);
                    }
                catch (InterruptedException e)
                    {
                    ++cInterrupt;
                    if (m_fInterruptible)
                        {
                        // let this thread be interrupted
                        break;
                        }

                    // Note: intentionally do not reset the interrupted
                    //       flag, as we simulate "handling" the interrupt
                    }
                cWait -= ((ldtNow = Base.getSafeTimeMillis()) - ldtLast);
                if (cWait > 120000L)        // make sure wait does not exceed 2 minutes.
                    {
                    System.out.println("GuardianTests.LoserTask.process(), cWait: " + cWait + ", ldtNow: " + ldtNow + ", ldtLast: " + ldtLast + ", cInterrupt: " + cInterrupt);
                    cWait = 120000L;
                    }
                }

            // return the number of times this thread was interrupted
            return cInterrupt;
            }

        // ----- data members ---------------------------------------------

        /**
         * The amount of time the task should take to run.
         */
        protected long m_cDelay;

        /**
        * Is this task interruptible?
        */
        protected boolean m_fInterruptible;
        }

    // ----- helpers ------------------------------------------------------

    /**
    * Check the whether the timeout number in JMX is correct.
    *
    * @param cluster      the current cluster
    * @param server       the MBean server
    * @param sType        there are only two types of the timeout, "Soft" and "Hard"
    * @param cTrueNumber  the expected number of timeout
    */
    protected void checkNodeGuardTimeout(Cluster cluster, MBeanServer server, String sType, int cTrueNumber)
        {
        try
            {
            int         mNodeId   = cluster.getLocalMember().getId();
            ObjectName  oBeanName = new ObjectName("Coherence:type=Node,nodeId=" + mNodeId);

            Eventually.assertThat(invoking(this).getMBeanAttribute(server, oBeanName, "Guard" + sType + "Count"), is(cTrueNumber));
            }
        catch (Exception e)
            {
            Assert.fail(Base.printStackTrace(e));
            }
        }

    public Object getMBeanAttribute(MBeanServer server, ObjectName oBeanName, String sName)
        {
        try
            {
            return server.getAttribute(oBeanName, sName);
            }
        catch (Exception e)
            {
            return null;
            }
        }

    /**
    * Reset all the monitored statistics
    *
    * @param cluster     the current cluster
    * @param server      the MBean server
    */
    protected void resetNodeGuardTimeoutCount(Cluster cluster, MBeanServer server)
        {
        try
            {
            int         mNodeId   = cluster.getLocalMember().getId();
            ObjectName  oBeanName = new ObjectName("Coherence:type=Node,nodeId=" + mNodeId);

            server.invoke(oBeanName, "resetStatistics", null, null);
            }
        catch (Exception e)
            {
            Assert.fail(Base.printStackTrace(e));
            }
        }

    /**
    * Return a CacheService by the specified name.
    *
    * @return a CacheService that has been started
    */
    protected CacheService startService(String sName)
        {
        return (CacheService) CacheFactory.getService(sName);
        }

    /**
    * Log a warning (that can be easily scraped) that guardian errors (and
    * stack traces) are expected.
    *
    * @param sTestName  the name of the test
    * @param fHeader    if true, log the header, else the footer
    */
    protected void logWarn(String sTestName, boolean fHeader)
        {
        // Note: use System.out instead of Base.out here to avoid interfering
        //       with the tests' initialization of Coherence
        if (fHeader)
            {
            System.out.println("+++ " + sTestName + ": This test is expected to produce guardian error messages +++");
            }
        else
            {
            System.out.println("--- " + sTestName + " ---");
            }
        }

    /**
    * Return the custom ServiceFailurePolicy configured on the specified service, or null
    *
    * @param service  the service
    *
    * @return the CustomServiceFailurePolicy on the specified service, or null
    */
    protected CustomServiceFailurePolicy getServicePolicy(Service service)
        {
        SafeService      serviceSafe = (SafeService) service;
        PartitionedCache serviceReal = (PartitionedCache) serviceSafe.getService();

        return (CustomServiceFailurePolicy) serviceReal.getServiceFailurePolicy();
        }

    /**
     * A JUnit rule that will cause the test to fail if it runs too long.
     * A thread dump will be generated on failure.
     */
    @ClassRule
    public static final ThreadDumpOnTimeoutRule timeout
            = ThreadDumpOnTimeoutRule.after(15, TimeUnit.MINUTES, true);
    }
