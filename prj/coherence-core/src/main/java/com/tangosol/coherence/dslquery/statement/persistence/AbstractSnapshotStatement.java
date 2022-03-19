/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dslquery.statement.persistence;

import com.tangosol.coherence.dslquery.CohQLException;
import com.tangosol.coherence.dslquery.internal.PersistenceToolsHelper;
import com.tangosol.coherence.dslquery.statement.AbstractStatement;

import com.tangosol.io.FileHelper;

import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Abstract implementation of an {@link AbstractStatement} providing functionality
 * useful for generic snapshot statements.
 */
public abstract class AbstractSnapshotStatement
        extends AbstractStatement
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a AbstractSnapshotStatement that will prove base functionality for
     * other snapshot commands.
     *
     * @param sSnapshotName  the snapshot name to create
     * @param sServiceName   the service to snapshot
     */
    public AbstractSnapshotStatement(String sSnapshotName, String sServiceName)
        {
        f_sSnapshotName = sSnapshotName;
        f_sServiceName  = sServiceName;
        }

    // ----- AbstractStatement methods --------------------------------------

    @Override
    public void showPlan(PrintWriter out)
        {
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Return a confirmation message.
     *
     * @param sAction the action to be performed
     *
     * @return a confirmation message
     */
    protected String getConfirmationMessage(String sAction)
        {
        return "Are you sure you want to " + sAction + " a snapshot called '" + f_sSnapshotName + "' for "
               + "service '" + f_sServiceName + "'? (y/n): ";
        }

    /**
     * Validate that the snapshot name conforms to standard.
     *
     * @param sSnapshotName  the name of snapshot to validate
     *
     * @throws CohQLException if the name is not valid
     */
    protected void validateSnapshotName(String sSnapshotName)
            throws CohQLException
        {
        String sSafeSnapshotName = FileHelper.toFilename(sSnapshotName);

        if (!sSafeSnapshotName.equals(sSnapshotName))
            {
            throw new CohQLException("The supplied snapshot name " + sSnapshotName + " is not a valid "
                                     + "file name. Consider using " + sSafeSnapshotName);
            }
        }

    /**
     * Validate that the service f_sServiceName exists.
     *
     * @param helper  the {@link PersistenceToolsHelper} instance to use to validate
     */
    protected void validateServiceExists(PersistenceToolsHelper helper)
        {
        if (!helper.serviceExists(f_sServiceName))
            {
            throw new CohQLException("Service '" + f_sServiceName + "' does not exist");
            }
        }

    /**
     * Validate that a snapshot f_sSnapshotName exists for the given service
     * f_sServiceName.
     *
     * @param helper  the {@link PersistenceToolsHelper} instance to use to validate
     */
    protected void validateSnapshotExistsForService(PersistenceToolsHelper helper)
        {
        if (!helper.snapshotExists(f_sServiceName, f_sSnapshotName))
            {
            throw new CohQLException("Snapshot '" + f_sSnapshotName + "' does not exist for service '" + f_sServiceName
                                     + "'");
            }
        }

    /**
     * Validate that an archived snapshot f_sSnapshotName exists for the given service
     * f_sServiceName.
     *
     * @param helper  the {@link PersistenceToolsHelper} instance to use to validate
     */
    protected void validateArchivedSnapshotExistsForService(PersistenceToolsHelper helper)
        {
        if (!helper.archivedSnapshotExists(f_sServiceName, f_sSnapshotName))
            {
            throw new CohQLException("Snapshot '" + f_sSnapshotName + "' does not exist for service '" + f_sServiceName
                                     + "'");
            }
        }

    /**
     * Replace the following macro's for the snapshot name:<br>
     * <ul>
     *     <li>%y  - Year</li>
     *     <li>%m  - Month</li>
     *     <li>%d  - Day of month</li>
     *     <li>%w  - Day of week. mon,tues,wed, etc</li>
     *     <li>%M  - Month name - Jan, Feb, etc</li>
     *     <li>%hh - Hour</li>
     *     <li>%mm - Minute</li>
     * </ul>
     *
     * @param sSnapshotName the snapshot name to replace macros
     *
     * @return the formatted snapshot name
     */
    protected static String replaceDateMacros(String sSnapshotName)
        {
        String   sFinalName = new String(sSnapshotName);
        Calendar calendar   = Calendar.getInstance();

        sFinalName = sFinalName.replaceAll("%mm", String.format("%02d", calendar.get(Calendar.MINUTE))).
                                replaceAll("%m", String.format("%02d", calendar.get(Calendar.MONTH))).
                                replaceAll("%y", String.format("%d", calendar.get(Calendar.YEAR))).
                                replaceAll("%d", String.format("%02d", calendar.get(Calendar.DAY_OF_MONTH))).
                                replaceAll("%hh", String.format("%02d", calendar.get(Calendar.HOUR_OF_DAY))).
                                replaceAll("%w", WEEKDAY_NAME.format(calendar.getTime())).
                                replaceAll("%M", MONTH_NAME.format(calendar.getTime()));

        return sFinalName;
        }

    // ----- constants ------------------------------------------------------

    /**
     * Result to output on command success.
     */
    protected static final String SUCCESS = "Success";

    /**
     * Format month name.
     */
    public static final SimpleDateFormat MONTH_NAME = new SimpleDateFormat("MMMM");

    /**
     * Format weekday name.
     */
    public static final SimpleDateFormat WEEKDAY_NAME = new SimpleDateFormat("E");

    /**
     * Sleep time between checking operation completion.
     */
    public static final long SLEEP_TIME = 500L;

    // ----- data members ---------------------------------------------------

    /**
     * Snapshot name to utilize.
     */
    protected final String f_sSnapshotName;

    /**
     * Service name to carry out operations for.
     */
    protected final String f_sServiceName;
    }