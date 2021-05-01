/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net;

/**
 * MessageComponent is an internal interface used to expose methods on the
 * Component.Net.Message TDE component.
 *
 * @author hr  2016.02.24
 * @since 12.2.1.1
 */
public interface MessageComponent
    {
    // ----- version utility methods ----------------------------------------

    /**
     * Determine whether the sender of the content of this BufferInput
     * runs a version that supersedes (greater or equal to) the specified
     * version.
     *
     * @return true iff the sender's version is greater or equal to the
     *         specified one
     */
    public boolean isSenderCompatible(int nYear, int nMonth, int nPatch);

    /**
     * Determine whether the sender of the content of this BufferInput
     * runs a version that supersedes (greater or equal to) the specified
     * version.
     *
     * @return true iff the sender's version is greater or equal to the
     *         specified one
     */
    public boolean isSenderCompatible(int nMajor, int nMinor, int nMicro, int nPatchSet, int nPatch);

    /**
     * Determine whether all the recipients of the content of this BufferOutput
     * run versions that supersede (greater or equal to) the specified
     * version.
     *
     * @return true iff all the recipients' versions are greater or equal
     *         to the specified one
     */
    public boolean isRecipientCompatible(int nYear, int nMonth, int nPatch);

    /**
     * Determine whether all the recipients of the content of this BufferOutput
     * run versions that supersede (greater or equal to) the specified
     * version.
     *
     * @return true iff all the recipients' versions are greater or equal
     *         to the specified one
     */
    public boolean isRecipientCompatible(int nMajor, int nMinor, int nMicro, int nPatchSet, int nPatch);
    }
