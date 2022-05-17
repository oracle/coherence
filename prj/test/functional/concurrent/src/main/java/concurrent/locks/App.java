/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package concurrent.locks;

import com.oracle.coherence.concurrent.locks.Locks;
import com.oracle.coherence.concurrent.locks.RemoteLock;
import com.oracle.coherence.concurrent.locks.RemoteReadWriteLock;
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
             case "READ":
                 doReadLock(args);
                 break;
             case "UREAD":
                 doReadUnlock(args);
                 break;
             case "WRITE":
                 doWriteLock(args);
                 break;
             case "UWRITE":
                 doWriteUnlock(args);
                 break;
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
        RemoteLock lock = Locks.remoteLock(name);

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
        RemoteLock lock = Locks.remoteLock(name);

        System.out.printf("\n%s: unlock() => ", name);
        lock.unlock();
        System.out.printf("owned: %s, count: %d", lock.isHeldByCurrentThread(), lock.getHoldCount());
        System.out.printf("\n%s: owner=%s", name, lock.getOwner());
        }

    protected static void doReadLock(String[] args) throws InterruptedException
        {
        if (args.length == 0)
            {
            System.out.println("Usage: READ <name> [<timeoutInSeconds>]");
            return;
            }

        String                   name = args[0];
        RemoteReadWriteLock lock = Locks.remoteReadWriteLock(name);

        if (args.length == 2)  // READ name timeout
            {
            long timeout = Long.parseLong(args[1]);
            System.out.printf("\n%s: readLock().tryLock(%d, SECONDS) => ", name, timeout);
            boolean fLock   = lock.readLock().tryLock(timeout, TimeUnit.SECONDS);
            System.out.printf("acquired: %s, count: %d, holds: %d", fLock, lock.getReadLockCount(), lock.getReadHoldCount());
            }
        else
            {
            System.out.printf("\n%s: readLock().lock() => ", name);
            lock.readLock().lock();
            System.out.printf("count: %d, holds: %d", lock.getReadLockCount(), lock.getReadHoldCount());
            }
        System.out.printf("\n%s: owner=%s", name, lock.getOwner());
        }

    protected static void doReadUnlock(String[] args)
        {
        if (args.length == 0)
            {
            System.out.println("Usage: UREAD <name>");
            return;
            }

        String                   name = args[0];
        RemoteReadWriteLock lock = Locks.remoteReadWriteLock(name);

        System.out.printf("\n%s: readLock().unlock() => ", name);
        lock.readLock().unlock();
        System.out.printf("count: %d, holds: %d", lock.getReadLockCount(), lock.getReadHoldCount());
        System.out.printf("\n%s: owner=%s", name, lock.getOwner());
        }

    protected static void doWriteLock(String[] args) throws InterruptedException
        {
        if (args.length == 0)
            {
            System.out.println("Usage: WRITE <name> [<timeoutInSeconds>]");
            return;
            }

        String                   name = args[0];
        RemoteReadWriteLock lock = Locks.remoteReadWriteLock(name);

        if (args.length == 2)  // WRITE name timeout
            {
            long timeout = Long.parseLong(args[1]);
            System.out.printf("\n%s: writeLock().tryLock(%d, SECONDS) => ", name, timeout);
            boolean fLock   = lock.writeLock().tryLock(timeout, TimeUnit.SECONDS);
            System.out.printf("acquired: %s, count: %d", fLock, lock.getWriteHoldCount());
            }
        else
            {
            System.out.printf("\n%s: writeLock().lock() => ", name);
            lock.writeLock().lock();
            System.out.printf("owned: %s, count: %d", lock.isWriteLockedByCurrentThread(), lock.getWriteHoldCount());
            }
        System.out.printf("\n%s: owner=%s", name, lock.getOwner());
        }

    protected static void doWriteUnlock(String[] args)
        {
        if (args.length == 0)
            {
            System.out.println("Usage: UWRITE <name>");
            return;
            }

        String                   name = args[0];
        RemoteReadWriteLock lock = Locks.remoteReadWriteLock(name);

        System.out.printf("\n%s: writeLock().unlock() => ", name);
        lock.writeLock().unlock();
        System.out.printf("owned: %s, count: %d", lock.isWriteLockedByCurrentThread(), lock.getWriteHoldCount());
        System.out.printf("\n%s: owner=%s", name, lock.getOwner());
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
