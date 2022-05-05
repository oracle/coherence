/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package rwbm;


import com.oracle.coherence.common.base.StackTrace;
import com.oracle.coherence.testing.TestCacheStore;

import java.util.HashSet;
import java.util.Set;


/**
 * Subclass of {@link TestCacheStore} which conditionally delays its
 * operations based on the contents of the call stack.
 */
public class ConditionalDelayCacheStore extends TestCacheStore
    {
    /**
     * Return duration (in milliseconds) of store operations, provided the store
     * method is invoked with a call stack that contains required classes and
     * methods. If these conditions are not met, return zero delay. This method
     * should be called only from the store method itself.
     *
     * @return  the number of milliseconds that a store operation will take.
     */
    public long getDurationStore()
        {
        if (isDelayStore())
            {
            return super.getDurationStore();
            }
        else
            {
            return 0l;
            }
        }

    /**
     * Add a class name to the set of frames that must be on call
     * stack to the store method in order to trigger a delay.
     *
     * @param sClzName  the class name that must be on call stack.
     */
    public void addStoreDelayConditionClass(String sClzName)
        {
        setDelayCondClzNames.add(sClzName);
        }

    /**
     * Add a method name to the set of frames that must be on call
     * stack to the store method in order to trigger a delay.
     *
     * @param sMetName  the class name that must be on call stack.
     */
    public void addStoreDelayConditionMethod(String sMetName)
        {
        setDelayCondMetNames.add(sMetName);
        }

    /**
     * Check if the call stack to a store method contains the required classes
     * and methods in order to trigger a delay. This method should be called
     * only from the store method itself.
     *
     * @return  true iff all required classes and methods are present
     *          on the call stack.
     */
    public boolean isDelayStore()
        {
        int nClzCond = setDelayCondClzNames.size();
        int nMetCond = setDelayCondMetNames.size();
        if (nMetCond == 0 && nClzCond == 0) return true;

        Set<String> setClzes = new HashSet<String>();
        Set<String> setMets = new HashSet<String>();

        for (StackTrace.StackFrame fr : getStackFrames())
            {
            String sClzName = fr.getShortClassName();
            String sMetName = fr.getMethodName();
            if (setDelayCondClzNames.contains(sClzName))
                {
                setClzes.add(sClzName);
                }
            if (setDelayCondMetNames.contains(sMetName))
                {
                setMets.add(sMetName);
                }
            if (setClzes.size() == nClzCond && setMets.size() == nMetCond)
                break;
            }

        return setClzes.size() == nClzCond && setMets.size() == nMetCond;
        }

    /**
     * Remove all previously set delay conditions for the store operation.
     */
    public void removeStoreDelayConditions()
        {
        setDelayCondClzNames = new HashSet<String>();
        setDelayCondMetNames = new HashSet<String>();
        }

    // ----- data members ---------------------------------------------------

    /**
     * Set of class names that must be present on the call stack for a store
     * operation to be delayed.
     */
    private Set<String> setDelayCondClzNames = new HashSet<String>();

    /**
     * Set of method names that must be present on the call stack for a store
     * operation to be delayed.
     */
    private Set<String> setDelayCondMetNames = new HashSet<String>();

    }

