/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.util;

import java.lang.management.LockInfo;
import java.lang.management.ManagementFactory;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Thread related helpers.
 *
 * @author  mf  2016.08.17
 */
public final class Threads
    {
    /**
    * Get the full thread dump.
    *
    * @return a string containing the thread dump
    */
    public static String getThreadDump()
        {
        return getThreadDump(DUMP_LOCKS);
        }

    /**
    * Get the full thread dump.
    *
    * @param fLocks  true if lock related information should be gathered.
    *
    * @return a string containing the thread dump
    */
    public static String getThreadDump(boolean fLocks)
        {
        return getThreadDump(fLocks ? LockAnalysis.FULL : LockAnalysis.NONE);
        }
    /**
    * Get the full thread dump.
    *
    * @param locks  the lock analysis mode
    *
    * @return a string containing the thread dump
    */
    public static String getThreadDump(LockAnalysis locks)
        {
        String sMsg;
        boolean fLocks;
        switch (locks)
            {
            case FULL:
                fLocks = true;
                sMsg = "";
                break;
            case OWNERSHIP:
                fLocks = true;
                sMsg = "(excluding deadlock analysis)";
                break;
            default:
                fLocks = false;
                sMsg = "(excluding locks and deadlock analysis)";
                break;
            }

        long          ldtStart      = System.currentTimeMillis();
        ThreadMXBean  bean          = ManagementFactory.getThreadMXBean();
        Set<Long>     setDeadLocked = locks == LockAnalysis.FULL ? collectDeadlockedIds(bean) : null;

        StringBuilder sbAll       = new StringBuilder("Full Thread Dump: ").append(sMsg);
        StringBuilder sbThreads   = new StringBuilder();
        StringBuilder sbDeadlocks = new StringBuilder();
        StringBuilder sbTemp      = new StringBuilder();
        ThreadInfo[]  ainfo       = bean.dumpAllThreads(fLocks && bean.isObjectMonitorUsageSupported(),
                fLocks && bean.isSynchronizerUsageSupported());

        for (ThreadInfo info : ainfo)
            {
            boolean fDeadlocked = locks == LockAnalysis.FULL && setDeadLocked.contains(info.getThreadId());

            sbTemp.setLength(0);
            collectThreadHeader(info, sbTemp, fDeadlocked);
            collectStackTrace(info, sbTemp);
            collectLockedSyncs(info, sbTemp);

            sbThreads.append(sbTemp);
            if (fDeadlocked)
                {
                sbDeadlocks.append(sbTemp);
                }
            }

        long cMillis = System.currentTimeMillis() - ldtStart;
        if (cMillis > 1000)
            {
            sbAll.append(" took ").append(new Duration(cMillis, Duration.Magnitude.MILLI));
            }

        sbAll.append("\n")
             .append(sbThreads);

        if (locks == LockAnalysis.FULL && !setDeadLocked.isEmpty())
            {
            sbAll.append("\n Found following deadlocked threads:\n");
            sbAll.append(sbDeadlocks);
            }

        return sbAll.toString();
        }

    /**
     * Collect the deadlocked thread ids.
     *
     * @param bean  the ThreadMXBean
     *
     * @return a set of thread ids
     */
    protected static Set<Long> collectDeadlockedIds(ThreadMXBean bean)
        {
        long[] alThreadId = bean.isSynchronizerUsageSupported() ?
            bean.findDeadlockedThreads() :
            bean.findMonitorDeadlockedThreads();

        if (alThreadId == null || alThreadId.length == 0)
            {
            return Collections.emptySet();
            }

        Set<Long> setIds = new HashSet<Long>();

        ThreadInfo[] ainfo = bean.getThreadInfo(alThreadId, Integer.MAX_VALUE);
        for (ThreadInfo info : ainfo)
            {
            if (info != null)
                {
                setIds.add(info.getThreadId());
                }
            }
        return setIds;
        }

     /**
      * Collect the header information for a given thread.
      *
      * @param infoThread   the ThreadInfo that the thread is associated with
      * @param sbTrace      the StringBuilder to append the info to
      * @param fDeadlocked  true iff the thread is in deadlock
      */
      protected static void collectThreadHeader(ThreadInfo infoThread, StringBuilder sbTrace, boolean fDeadlocked)
          {
          sbTrace.append("\n\"")
                 .append(infoThread.getThreadName())
                 .append("\" id=")
                 .append(infoThread.getThreadId())
                 .append(" State:")
                 .append(fDeadlocked ? "DEADLOCKED" : infoThread.getThreadState());

          if (infoThread.isSuspended())
              {
              sbTrace.append(" (suspended)");
              }
          if (infoThread.isInNative())
              {
              sbTrace.append(" (in native)");
              }
          sbTrace.append("\n");
          }

      /**
      * Collect locked synchronizers for a given thread.
      *
      * @param infoThread  the ThreadInfo that the thread is associated with
      * @param sbTrace     the StringBuilder to append the info to
      */
      protected static void collectLockedSyncs(ThreadInfo infoThread, StringBuilder sbTrace)
          {
          LockInfo[] aLock = infoThread.getLockedSynchronizers();
          if (aLock.length > 0)
              {
              sbTrace.append("\n\tLocked synchronizers:\n");
              for (LockInfo info : aLock)
                  {
                  sbTrace.append("\t").append(info).append("\n");
                  }
              }
          }

      /**
      * Collect stack trace for a given thread.
      *
      * @param infoThread  the ThreadInfo that the thread is associated with
      * @param sbTrace     the StringBuilder to append the info to
      */
      protected static void collectStackTrace(ThreadInfo infoThread, StringBuilder sbTrace)
          {
          StackTraceElement[] aStackElement = infoThread.getStackTrace();
          MonitorInfo[]       aMonitor      = infoThread.getLockedMonitors();
          LockInfo            infoLock      = infoThread.getLockInfo();

          for (int iDepth = 0, c = aStackElement.length; iDepth < c; iDepth++)
              {
              sbTrace.append("\tat ")
                     .append(aStackElement[iDepth])
                     .append("\n");

              if (iDepth == 0 && infoLock != null)
                  {
                  String sOwner = infoThread.getLockOwnerName();

                  sbTrace.append("\t-  ")
                         .append(sOwner != null ? "waiting to lock " : "waiting on ")
                         .append(infoLock);

                  if (sOwner != null)
                      {
                      sbTrace.append(" owned by:\"")
                             .append(sOwner)
                             .append("\" id=")
                             .append(infoThread.getLockOwnerId());
                      }

                  sbTrace.append("\n");
                  }

              for (MonitorInfo info : aMonitor)
                  {
                  if (info.getLockedStackDepth() == iDepth)
                      {
                      sbTrace.append("\t-  locked ")
                             .append(info)
                             .append("\n");
                      }
                  }
              }
          }

    /**
     * Enum for various forms of lock analysis.
     */
    public enum LockAnalysis
        {
        /**
         * Perform no lock analysis.
         */
        NONE,

        /**
         * List the locks held by a thread.
         */
        OWNERSHIP,

        /**
         * List the locks held by a thread and perform deadlock detection.
         */
        FULL
        }

    // ----- constants ------------------------------------------------------

    /**
     * If true then {@link #getThreadDump() thread dumps} will include lock and synchronizer information.  Such
     * dumps can be more expensive to obtain, and can ultimately pause the JVM.
     */
    private static final LockAnalysis DUMP_LOCKS ;

    static
        {
        String sLock = System.getProperty(Threads.class.getName() + ".dumpLocks", LockAnalysis.OWNERSHIP.name());
        if ("true".equalsIgnoreCase(sLock))
            {
            DUMP_LOCKS = LockAnalysis.FULL;
            }
        else if ("false".equalsIgnoreCase(sLock))
            {
            DUMP_LOCKS = LockAnalysis.NONE;
            }
        else
            {
            DUMP_LOCKS = LockAnalysis.valueOf(sLock);
            }
        }
    }
