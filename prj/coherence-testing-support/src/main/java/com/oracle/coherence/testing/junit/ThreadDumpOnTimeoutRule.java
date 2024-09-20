/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.testing.junit;

import com.oracle.bedrock.runtime.Application;
import com.oracle.bedrock.runtime.LocalPlatform;
import com.oracle.bedrock.runtime.options.Arguments;
import com.oracle.bedrock.testsupport.MavenProjectFileUtils;
import com.oracle.coherence.common.util.Threads;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.io.File;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A JUnit 4 rule that creates a thread dump if a test takes too long to execute.
 * If the test times out, then the test will fail and a thread dump will be generated.
 * <p/>
 * If the rule is added as a static field annotated with {@link org.junit.ClassRule}
 * then the configured timeout will be for the whole test class.
 * <p/>
 * If the rule is added as a static field annotated with {@link org.junit.Rule}
 * then the configured timeout will be for each test individually.
 *
 * @author Jonathan Knight  2023.03.08
 * @since 23.03
 */
public class ThreadDumpOnTimeoutRule
        implements TestRule
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a timeout rule.
     * <p/>
     * If the {@code nTimeout} parameter is less than or equal to zero, no
     * timeout will be configured.
     *
     * @param nTimeout  the timeout value
     * @param units     the {@link TimeUnit units} for the timeout
     *
     * @throws NullPointerException if the {@code units} parameter is {@code null}
     */
    public ThreadDumpOnTimeoutRule(long nTimeout, TimeUnit units)
        {
        this(nTimeout, units, null);
        }

    /**
     * Create a timeout rule.
     * <p/>
     * If the {@code nTimeout} parameter is less than or equal to zero, no
     * timeout will be configured.
     *
     * @param nTimeout  the timeout value
     * @param units     the {@link TimeUnit units} for the timeout
     * @param runnable  a {@link Runnable} to execute when the test times out
     *
     * @throws NullPointerException if the {@code units} parameter is {@code null}
     */
    public ThreadDumpOnTimeoutRule(long nTimeout, TimeUnit units, Runnable runnable)
        {
        this(nTimeout, units, false, runnable);
        }

    /**
     * Create a timeout rule.
     * <p/>
     * If the {@code nTimeout} parameter is less than or equal to zero, no
     * timeout will be configured.
     *
     * @param nTimeout  the timeout value
     * @param units     the {@link TimeUnit units} for the timeout
     * @param fJcmdThreadDump {@code true} to generate a JCMD thread dump to a file
     *
     * @throws NullPointerException if the {@code units} parameter is {@code null}
     */
    public ThreadDumpOnTimeoutRule(long nTimeout, TimeUnit units, boolean fJcmdThreadDump)
        {
        this(nTimeout, units, fJcmdThreadDump, null);
        }

    /**
     * Create a timeout rule.
     * <p/>
     * If the {@code nTimeout} parameter is less than or equal to zero, no
     * timeout will be configured.
     *
     * @param nTimeout        the timeout value
     * @param units           the {@link TimeUnit units} for the timeout
     * @param fJcmdThreadDump {@code true} to generate a JCMD thread dump to a file
     * @param runnable        a {@link Runnable} to execute when the test times out
     *
     * @throws NullPointerException if the {@code units} parameter is {@code null}
     */
    public ThreadDumpOnTimeoutRule(long nTimeout, TimeUnit units, boolean fJcmdThreadDump, Runnable runnable)
        {
        f_nTimeout        = nTimeout;
        f_units           = Objects.requireNonNull(units);
        f_runnable        = runnable;
        f_fJcmdThreadDump = fJcmdThreadDump;
        }

    // ----- TestRule methods -----------------------------------------------

    @Override
    public Statement apply(Statement base, Description description)
        {
        if (f_nTimeout > 0)
            {
            return new FailOnTimeoutStatement(base, description, f_nTimeout, f_units, f_fJcmdThreadDump, f_runnable);
            }
        return base;
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Create a {@link ThreadDumpOnTimeoutRule} that times out after
     * a specific number of seconds.
     *
     * @param cSeconds  the number of seconds to timeout
     *
     * @return a {@link ThreadDumpOnTimeoutRule} that times out after
     *         a specific number of seconds.
     */
    public static ThreadDumpOnTimeoutRule afterSeconds(long cSeconds)
        {
        return new ThreadDumpOnTimeoutRule(cSeconds, TimeUnit.SECONDS);
        }

    /**
     * Create a {@link ThreadDumpOnTimeoutRule} that times out after
     * a specific number of seconds.
     *
     * @param nTimeout  the timeout value
     * @param units     the {@link TimeUnit units} for the timeout
     *
     * @return a {@link ThreadDumpOnTimeoutRule} that times out after
     *         a specific number of seconds.
     */
    public static ThreadDumpOnTimeoutRule after(long nTimeout, TimeUnit units)
        {
        return new ThreadDumpOnTimeoutRule(nTimeout, units);
        }

    public static ThreadDumpOnTimeoutRule after(long nTimeout, TimeUnit units, Runnable runnable)
        {
        return new ThreadDumpOnTimeoutRule(nTimeout, units, runnable);
        }

    /**
     * Create a timeout rule.
     * <p/>
     * If the {@code nTimeout} parameter is less than or equal to zero, no
     * timeout will be configured.
     *
     * @param nTimeout  the timeout value
     * @param units     the {@link TimeUnit units} for the timeout
     * @param fJcmd     {@code true} to generate a JCMD thread dump to a file
     *
     * @throws NullPointerException if the {@code units} parameter is {@code null}
     */
    public static ThreadDumpOnTimeoutRule after(long nTimeout, TimeUnit units, boolean fJcmd)
        {
        return new ThreadDumpOnTimeoutRule(nTimeout, units, fJcmd);
        }

    // ----- inner class: FailOnTimeoutStatement ----------------------------

    /**
     * A Junit {@link Statement} to fail a test on timeout.
     */
    protected static class FailOnTimeoutStatement
            extends Statement
        {
        // ----- constructors ---------------------------------------------------

        /**
         * Create a {@link FailOnTimeoutStatement}.
         *
         * @param delegate     the {@link Statement} that will run the test
         * @param description  the description of the test
         * @param nTimeout     the timeout to apply to the test
         * @param units        the {@link TimeUnit units} for the timeout
         */
        protected FailOnTimeoutStatement(Statement delegate, Description description,
                long nTimeout, TimeUnit units, boolean fJcmdThreadDump, Runnable runnable)
            {
            f_delegate        = delegate;
            f_description     = description;
            f_nTimeout        = nTimeout;
            f_units           = units;
            f_runnable        = runnable;
            f_fJcmdThreadDump = fJcmdThreadDump;
            }

        // ----- Statement methods ------------------------------------------

        @Override
        public void evaluate() throws Throwable
            {
            CallableStatement callable = new CallableStatement(f_delegate);
            FutureTask<Throwable> task = new FutureTask<Throwable>(callable);
            Thread thread = new Thread(task, "Time-limited test");
            thread.setDaemon(true);
            thread.start();
            callable.awaitStarted();
            Throwable throwable = getResult(task);
            if (throwable != null)
                {
                throw throwable;
                }
            }

        // ----- helper methods ---------------------------------------------

        /**
         * Wait for the test task, returning the exception thrown by the test if the
         * test failed, an exception indicating a timeout if the test timed out, or
         * {@code null} if the test passed.
         */
        private Throwable getResult(FutureTask<Throwable> task)
            {
            try
                {
                if (f_nTimeout > 0)
                    {
                    return task.get(f_nTimeout, f_units);
                    }
                else
                    {
                    return task.get();
                    }
                }
            catch (InterruptedException e)
                {
                return e; // caller will re-throw; no need to call Thread.interrupt()
                }
            catch (ExecutionException e)
                {
                // test failed; have caller re-throw the exception thrown by the test
                return e.getCause();
                }
            catch (TimeoutException e)
                {
                // Test timed out, so print a thread-dump
                System.err.println("Test timed out: " + f_description.getDisplayName());
                System.err.println(Threads.getThreadDump(true));
                if (f_fJcmdThreadDump)
                    {
                    jcmdThreadDump(f_description.getTestClass());
                    }
                if (f_runnable != null)
                    {
                    try
                        {
                        f_runnable.run();
                        }
                    catch (Throwable ex)
                        {
                        throw new RuntimeException(ex);
                        }
                    }
                return e;
                }
            }

        protected void jcmdThreadDump(Class<?> testClass)
            {
            try
                {
                File dir = MavenProjectFileUtils.ensureTestOutputFolder(testClass, "dump");
                dir.mkdirs();

                File          fileDump = new File(dir, "threads.txt");
                long          pid      = ProcessHandle.current().pid();
                LocalPlatform platform = LocalPlatform.get();

                try (Application application = platform.launch("jcmd", Arguments.of(
                        String.valueOf(pid), "Thread.dump_to_file", fileDump.getAbsolutePath())))
                    {
                    application.waitFor();
                    }
                }
            catch (Exception e)
                {
                System.err.println("Failed to capture JCMD thread dump");
                e.printStackTrace();
                }
            }

        // ----- data members -----------------------------------------------

        /**
         * The {@link Statement} that will run the test.
         */
        private final Statement f_delegate;

        /**
         * The description of the test.
         */
        private final Description f_description;

        /**
         * The timeout to apply.
         */
        private final long f_nTimeout;

        /**
         * The {@link TimeUnit units} for the timeout.
         */
        private final TimeUnit f_units;

        private final Runnable f_runnable;

        private final boolean f_fJcmdThreadDump;
        }

    // ----- inner class: CallableStatement ---------------------------------

    /**
     * A {@link Callable} that will execute the {@link Statement}
     */
    protected static class CallableStatement
            implements Callable<Throwable>
        {
        // ----- constructors -----------------------------------------------

        /**
         * Create a {@link CallableStatement}.
         *
         * @param delegate  the {@link Statement} that will execute the test
         */
        public CallableStatement(Statement delegate)
            {
            f_delegate = delegate;
            }

        // ----- Callable methods -------------------------------------------

        @Override
        public Throwable call() throws Exception
            {
            try
                {
                f_startLatch.countDown();
                f_delegate.evaluate();
                }
            catch (Exception e)
                {
                throw e;
                }
            catch (Throwable e)
                {
                return e;
                }
            return null;
            }

        // ----- helper methods ---------------------------------------------

        protected void awaitStarted() throws InterruptedException
            {
            f_startLatch.await();
            }

        // ----- data members -----------------------------------------------

        /**
         * The {@link Statement} that will execute the test.
         */
        private final Statement f_delegate;

        /**
         * A latch that is triggered when the callable starts.
         */
        private final CountDownLatch f_startLatch = new CountDownLatch(1);
        }

    // ----- data members ---------------------------------------------------

    /**
     * The timeout to apply.
     */
    private final long f_nTimeout;

    /**
     * The {@link TimeUnit units} for the timeout.
     */
    private final TimeUnit f_units;

    private final Runnable f_runnable;

    private final boolean f_fJcmdThreadDump;
    }
