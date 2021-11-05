/*
 * Copyright (c) 2021 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.locks;

import com.tangosol.net.Coherence;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * Client app for testing lock support.
 */
public class App 
    {
     public static void processCommand(String sCmd, String[] args)
             throws Exception
         {
         switch (sCmd.toUpperCase())
             {
             case "SERVER":
                 Coherence.clusterMember().start().join();
                 System.out.println("MEMBER ID: " + Coherence.getInstance().getCluster().getLocalMember().getUid());
                 break;
             case "LOCK":
                 doExclusiveLock(args);
                 break;
             case "UNLOCK":
                 doExclusiveUnlock(args);
                 break;
             //case "READ":
             //    doReadLock(args);
             //    break;
             //case "UREAD":
             //    doReadUnlock(args);
             //    break;
             //case "WRITE":
             //    doWriteLock(args);
             //    break;
             //case "UWRITE":
             //    doWriteUnlock(args);
             //    break;
             case "DUMP":
                 doDump();
                 break;
             case "EXIT":
                 System.out.println("Bye...");
                 Coherence.getInstance().close();
                 System.exit(0);
                 break;
             }
         }

    protected static void doExclusiveLock(String[] args)
            throws InterruptedException
        {
        if (args.length == 0)
            {
            System.out.println("Usage: LOCK <name> [<timeoutInSeconds>]");
            return;
            }

        String          name = args[0];
        DistributedLock lock = Locks.exclusive(name);

        if (args.length == 2)  // LOCK name timeout
            {
            long timeout = Long.parseLong(args[1]);
            System.out.printf("\n%s: tryLock(%d, SECONDS) => ", name, timeout);
            boolean fLock   = lock.tryLock(timeout, TimeUnit.SECONDS);
            System.out.printf("acquired: %s, count: %d", fLock, lock.getHoldCount());
            }
        else
            {
            System.out.printf("\n%s: lock() => ", name);
            lock.lock();
            System.out.printf("owned: %s, count: %d", lock.isHeldByCurrentThread(), lock.getHoldCount());
            }
        System.out.printf("\n%s: owner=%s", name, lock.getOwner());
        }

    protected static void doExclusiveUnlock(String[] args)
        {
        if (args.length == 0)
            {
            System.out.println("Usage: UNLOCK <name>");
            return;
            }

        String          name = args[0];
        DistributedLock lock = Locks.exclusive(name);

        System.out.printf("\n%s: unlock() => ", name);
        lock.unlock();
        System.out.printf("owned: %s, count: %d", lock.isHeldByCurrentThread(), lock.getHoldCount());
        System.out.printf("\n%s: owner=%s", name, lock.getOwner());
        }

    protected static void doReadLock(String[] args)
        {
        doLock(args, /*fRead*/ true, /*fLock*/ true);
        }

    protected static void doReadUnlock(String[] args)
        {
        doLock(args, /*fRead*/ true, /*fLock*/ false);
        }

    protected static void doWriteLock(String[] args)
        {
        doLock(args, /*fRead*/ false, /*fLock*/ true);
        }

    protected static void doWriteUnlock(String[] args)
        {
        doLock(args, /*fRead*/ false, /*fLock*/ false);
        }

    protected static void doLock(String[] args, boolean fRead, boolean fLock)
        {
        }

    protected static void doDump()
        {
        System.out.println("EXCLUSIVE:");
        Locks.exclusiveLocksMap().entrySet().forEach(System.out::println);

        System.out.println("\nREAD/WRITE:");
        Locks.readWriteLocksMap().entrySet().forEach(System.out::println);
        }

    public static void main(String[] asArgs)
        {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in)))
            {
            String sLine;

            System.out.print("> ");
            while ((sLine = reader.readLine()) != null)
                {
                String[] args = sLine.split(" ");

                processCommand(args[0], Arrays.copyOfRange(args, 1, args.length));

                System.out.print("\n> ");
                }
            }
        catch (Exception e)
            {
            e.printStackTrace();
            }
        }
    }
