/*
 * Copyright (c) 2021 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.locks;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.DefaultCacheServer;
import com.tangosol.net.NamedCache;
import com.tangosol.net.NamedMap;
import com.tangosol.util.Base;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.concurrent.locks.Lock;

/**
 * Client app for testing lock support.
 */
public class App 
    {
     public static void processCommand(String sCmd, String[] asCmds)
         {
         switch (sCmd.toUpperCase())
             {
             case "SERVER":
                 DefaultCacheServer.startServerDaemon().waitForServiceStart();
                 break;
             case "MAP":
                 doMap(asCmds);
                 break;
             case "READ":
                 doReadLock(asCmds);
                 break;
             case "UREAD":
                 doReadUnlock(asCmds);
                 break;
             case "WRITE":
                 doWriteLock(asCmds);
                 break;
             case "UWRITE":
                 doWriteUnlock(asCmds);
                 break;
             case "DUMP":
                 doDump();
                 break;
             }
         }

    protected static void doMap(String[] asCmds)
        {
        if (asCmds.length == 1)
            {
            s_oSink = CacheFactory.getCache(asCmds[0]);
            }
        else
            {
            System.out.println("Unexpected args for queue command: " + Arrays.toString(asCmds));
            }
        }

    protected static void doReadLock(String[] asCmds)
        {
        doLock(asCmds, /*fRead*/ true, /*fLock*/ true);
        }

    protected static void doReadUnlock(String[] asCmds)
        {
        doLock(asCmds, /*fRead*/ true, /*fLock*/ false);
        }

    protected static void doWriteLock(String[] asCmds)
        {
        doLock(asCmds, /*fRead*/ false, /*fLock*/ true);
        }

    protected static void doWriteUnlock(String[] asCmds)
        {
        doLock(asCmds, /*fRead*/ false, /*fLock*/ false);
        }

    protected static void doLock(String[] asCmds, boolean fRead, boolean fLock)
        {
        // Usage: read lock-name ?timeout ?thread-id

        int iArg      = 0;
        int cCommands = asCmds.length;

        CoherenceReadWriteLock lock;
        if (s_oSink instanceof CoherenceReadWriteLock)
            {
            lock = (CoherenceReadWriteLock) s_oSink;
            }
        else
            {
            NamedCache cache = (NamedCache) s_oSink;
            if (cache == null)
                {
                System.out.println("Try running map command first");
                return;
                }

            if (cCommands == 0)
                {
                System.out.println("lock-name is required");
                return;
                }

            s_oSink = lock = new CoherenceReadWriteLock(asCmds[0], cache);
            iArg = 1;
            }

        long   cTimeout  = -1;
        long   lThreadId = Thread.currentThread().getId();

        if (cCommands > iArg)
            {
            String sCmd = null;
            try
                {
                cTimeout = Long.parseLong(sCmd = asCmds[iArg++]);
                }
            catch (RuntimeException e)
                {
                System.out.println("Invalid timeout format: " + sCmd);
                return;
                }
            }
        if (cCommands > iArg)
            {
            String sCmd = null;
            try
                {
                lThreadId = Long.parseLong(sCmd = asCmds[iArg]);
                }
            catch (RuntimeException e)
                {
                System.out.println("Invalid thread-id format: " + sCmd);
                return;
                }
            }

        lock.withThreadId(lThreadId);
        try
            {
            Lock lockReal = (fRead ? lock.readLock() : lock.writeLock());
            try
                {
                if (fLock)
                    {
                    lockReal.lock();
                    }
                else
                    {
                    lockReal.unlock();
                    }
                }
            catch (RuntimeException e)
                {
                System.err.println("ERR: " + e.getMessage());
                }
            }
        finally
            {
            lock.resetThreadId();
            }

        System.out.printf("%s lock %s for %s by thread-id %d %s\n",
                fRead ? "read" : "write",
                fLock ? "acquired" : "released",
                lock.getName(), lThreadId,
                cTimeout >= 0 ? "in " + cTimeout + "ms" : "");

        System.out.println();
        }

    protected static void doDump()
        {
        NamedMap cache;
        if (s_oSink instanceof CoherenceReadWriteLock)
            {
            cache = ((CoherenceReadWriteLock) s_oSink).getMap();
            }
        else
            {
            cache = (NamedMap) s_oSink;
            }

        System.out.println(cache.entrySet());
        }

    protected static void test()
        {
        }

    public static void main(String[] asArgs)
        {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in)))
            {
            String sLine;

            System.out.print("> ");
            while ((sLine = reader.readLine()) != null)
                {
                String[] asCmds = sLine.split(" ");

                processCommand(asCmds[0], Arrays.copyOfRange(asCmds, 1, asCmds.length));

                System.out.print("> ");
                }
            }
        catch (IOException e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }

    protected static Object s_oSink;
    }
