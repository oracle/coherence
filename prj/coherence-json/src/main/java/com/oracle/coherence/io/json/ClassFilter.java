package com.oracle.coherence.io.json;

import com.tangosol.internal.util.extractor.ReflectionAllowedFilter;

/**
 * This class allows/disallows types to be serialized/deserialized by
 * the JsonSerializer.  This is a process-wide filter whose value
 * will be read from the system property {@code jdk.serialFilter} as defined
 * by JEP-290.  This implementation does not include graph filtering.
 *
 * @since 20.12
 * @author rl 10.10.2020
 */
public class ClassFilter
        extends ReflectionAllowedFilter
        implements com.oracle.coherence.io.json.genson.ClassFilter
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a new {@code ClassFilter}.
     */
    public ClassFilter()
        {
        super(System.getProperty("jdk.serialFilter"));
        }

    // ----- methods from ReflectionAllowedFilter ---------------------------


    /**
     * Strips filter limits before passing the super class.  These aren't enforced.
     *
     * @param sPattern  a single pattern
     *
     * @return {@link AbstractReflectionAllowedFilter} for {@code sPattern}
     */
    @Override
    protected AbstractReflectionAllowedFilter createPatternFilter(String sPattern)
        {
        int idx = sPattern.indexOf('=');
        return super.createPatternFilter(idx == -1 ? sPattern : sPattern.substring(0, idx));
        }

    @Override
    public boolean evaluate(Class<?> clz)
        {
        return super.evaluate(clz);
        }
    }
