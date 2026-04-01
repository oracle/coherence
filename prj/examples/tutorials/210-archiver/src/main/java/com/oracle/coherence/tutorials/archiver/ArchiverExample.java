/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.tutorials.archiver;

import com.oracle.coherence.tutorials.archiver.persistence.PersistenceHelper;
import com.oracle.coherence.tutorials.archiver.pof.Contact;
import com.oracle.coherence.tutorials.archiver.pof.ContactId;

import com.tangosol.net.NamedCache;
import com.tangosol.net.Session;
import com.tangosol.util.Base;

import java.io.Console;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.oracle.coherence.tutorials.archiver.persistence.PersistenceHelper.populateData;
import static com.tangosol.net.cache.TypeAssertion.withTypes;

/**
 * Example showing how to use SFTP Snapshot Archiver.
 *
 * @author si, tm 2026.02.17
 * @since  15.1.2
 */
public class ArchiverExample
    {
    // ----- constants -----------------------------------------------------

    /**
     * Logger for this example.
     */
    private static final Logger LOGGER = Logger.getLogger(ArchiverExample.class.getName());

    // ----- static methods -------------------------------------------------

    // #tag::main[]
    /**
     * Execute Persistence SFTP Snapshot Archiver example.
     *
     * @param asArgs  command line arguments
     */
    public static void main(String[] asArgs)
        {
        // obtain the helper class to issue snapshot operations
        PersistenceHelper helper = new PersistenceHelper();

        try (Session session = Session.create())
            {
            new ArchiverExample().runExample(helper,
                session.getCache("contacts", withTypes(ContactId.class, Contact.class)));
            }
        catch (Exception e)
            {
            LOGGER.log(Level.SEVERE, "Error running archiver example", e);
            }
        }
        // #end::main[]

    // ----- ArchiverExample methods -------------------------------------------------

    /**
     * Run the example.
     *
     * @param helper the PersistenceHelper for issuing operations
     * @param nc     the NamedCache to use
     */
    public void runExample(PersistenceHelper helper, NamedCache<ContactId, Contact> nc)
        {
        final int DATA_SIZE     = 10000;
        String    sServiceName  = "PartitionedPofCache";
        String    sDataSnapshot = "snapshot-10000";

        log("");
        logHeader("archive example begin");

        try
            {
            populateData(nc, DATA_SIZE);
            logHeader("Populated data size is " + nc.size());

            // check for existing snapshots from previous runs
            String[] asSnapshots = helper.listSnapshots(sServiceName);

            // check to see if there is a snapshot left behind from previous run
            if (asSnapshots.length != 0 && Arrays.asList(asSnapshots).contains(sDataSnapshot))
                {
                logHeader("removing existing snapshot " + sDataSnapshot);
                helper.invokeOperationWithWait(PersistenceHelper.REMOVE_SNAPSHOT, sDataSnapshot, sServiceName);
                }

            asSnapshots = helper.listArchivedSnapshots(sServiceName);

            // check to see if there is an archived snapshot left behind from previous run
            if (asSnapshots.length != 0 && Arrays.asList(asSnapshots).contains(sDataSnapshot))
                {
                logHeader("removing existing archived snapshot " + sDataSnapshot);

                log("Removing snapshot " + sDataSnapshot);
                helper.invokeOperationWithWait(PersistenceHelper.REMOVE_ARCHIVED_SNAPSHOT, sDataSnapshot,
                                               sServiceName);
                }

            logHeader("create new snapshot " + sDataSnapshot);

            helper.invokeOperationWithWait(PersistenceHelper.CREATE_SNAPSHOT, sDataSnapshot, sServiceName);

            log("snapshots = " + Arrays.toString(helper.listSnapshots(sServiceName)));

            logHeader("archive snapshot " + sDataSnapshot);
            helper.invokeOperationWithWait(PersistenceHelper.ARCHIVE_SNAPSHOT, sDataSnapshot, sServiceName);
            log("archived snapshots = " + Arrays.toString(helper.listArchivedSnapshots(sServiceName)));

            logHeader("remove snapshot " + sDataSnapshot);
            helper.invokeOperationWithWait(PersistenceHelper.REMOVE_SNAPSHOT, sDataSnapshot, sServiceName);

            logHeader("clear cache");
            nc.clear();
            log("cache size is now " + nc.size());

            log("look at archived snapshot on remote SFTP machine then press RETURN to continue");

            Console console = System.console();
            if (console != null)
                {
                System.console().readLine();
                }

            logHeader("retrieve archived snapshot " + sDataSnapshot);
            helper.invokeOperationWithWait(PersistenceHelper.RETRIEVE_ARCHIVED_SNAPSHOT, sDataSnapshot,
                                           sServiceName);
            log("snapshots = " + Arrays.toString(helper.listSnapshots(sServiceName)));

            log("Recover snapshot " + sDataSnapshot + " containing 10000 entries");

            helper.invokeOperationWithWait(PersistenceHelper.RECOVER_SNAPSHOT, sDataSnapshot, sServiceName);

            log("cache size is now " + nc.size());

            logHeader("removing archived and local snapshots");

            helper.invokeOperationWithWait(PersistenceHelper.REMOVE_SNAPSHOT, sDataSnapshot, sServiceName);
            helper.invokeOperationWithWait(PersistenceHelper.REMOVE_ARCHIVED_SNAPSHOT, sDataSnapshot,
                                           sServiceName);

            logHeader("archiver example completed");
            }
        catch (Exception e)
            {
            throw Base.ensureRuntimeException(e, "Error running archiver example");
            }
        }

    /**
     * Log a message as a header.
     *
     * @param sMessage the message to log
     */
    private static void logHeader(String sMessage)
        {
        // mimic the previous log format for readability in the tutorial
        LOGGER.info("\n------" + sMessage + "------");
        }

    /**
     * Log a message.
     *
     * @param sMessage the message to log
     */
    private static void log(String sMessage)
        {
        // mimic the previous log format for readability in the tutorial
        LOGGER.info("      " + sMessage);
        }
    }