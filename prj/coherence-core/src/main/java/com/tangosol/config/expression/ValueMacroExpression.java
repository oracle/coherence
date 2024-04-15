/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.config.expression;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * A {@link ValueMacroExpression} is a string value potentially containing expandable macros.
 * <p>
 * Resolving the expression performs macro expansion. The macro syntax is <tt><i>${macro-parameter default-value}</i></tt>.
 * Thus, a value of <tt>near-<i>${coherence.client direct}</i></tt> is macro expanded by default to <tt>near-direct</tt>.
 * If property <tt><i>coherence.client</i></tt> is set to <tt>remote</tt>, then the value would be expanded to <tt>near-remote</tt>.
 * <p>
 * As of Coherence 14.1.2.0.0, the following common shell parameter expansion capabilities have been added.
 * <pre>
 * <i>${macro-parameter ${macro-parameter-default default-value}}</i>
 * Supports nesting of macro parameters to enable the defaulting value to be configured as a macro-parameter.
 * </pre>
 * In addition to the default delimiter of a space character, the colon character indicates a modifier for macro expansion.<br>
 * Note that <tt><i>word</i></tt> referenced below is either a nested macro parameter default or the default value.
 * <p>
 * <pre>
 * <i>${macro-parameter:-word}</i>
 * If <i>macro-parameter</i> is unset or null, the expansion of <i>word</i>is substituted,
 * otherwise, the value of <i>macro-parameter</i> is substituted.
 *
 * <i>${macro-parameter:+word}</i>
 * If <i>macro-parameter</i> is null or unset, nothing is substituted,
 * otherwise the expansion of <i>word</i> is substituted.
 *
 * <i>${macro-parameter:offset}</i>
 * <i>${macro-parameter:offset:length}</i>
 * Note that <i>length</i> and <i>offset</i> are integer values.
 * </pre>
 * See {@link OffsetLengthSubstringExpansionProcessor#process(String, ParameterResolver, int)} for substring expansion details
 * and example usages.
 *
 * @author jf 2015.05.18
 * @since Coherence 12.2.1
 */
public class ValueMacroExpression
        implements Expression<String>, ExternalizableLite, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor needed for serialization.
     */
    public ValueMacroExpression()
        {
        }

    /**
     * Construct a {@link ValueMacroExpression}.
     *
     * @param value  the value that potentially contains a macro expression.
     */
    public ValueMacroExpression(String value)
        {
        m_sValue    = value;
        }

    // ----- Expression interface -------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public String evaluate(ParameterResolver resolver)
        {
        String sValue = m_sValue;
        try
            {
            return substitute(sValue, resolver, 1).toString();
            }
        catch (IllegalStateException e)
            {
            Logger.config("macro parameter value expansion failure expanding " + sValue +
                          " cause: " + e.getMessage());
            return sValue;
            }
        catch (StringIndexOutOfBoundsException e)
            {
            Logger.config("macro parameter value expansion failure expanding " + sValue +
                          " cause:" + e.getMessage());
            return sValue;
            }
        }

    // ----- ValueMacroExpression methods ------------------------------------

    /**
     * Return a string value containing no outstanding macro parameters.
     *
     * @param sValue    a value containing 0 or more macro parameters
     * @param resolver  a {@link ParameterResolver macro parameter resolver}
     * @param cDepth    current number of macro parameter resolutions
     *
     * @return a string value containing no outstanding macro parameters
     */
    protected String substitute(String sValue, ParameterResolver resolver, int cDepth)
        {
        boolean fLogged = false;

        if (sValue != null)
            {
            // replace in-lined properties i.e. ${prop-name default-value} using resolver
            for (int ofStart = sValue.indexOf(PARAMETER_PREFIX); ofStart >= 0; ofStart = sValue.indexOf(PARAMETER_PREFIX))
                {
                int ofEnd = sValue.indexOf(PARAMETER_SUFFIX, ofStart);

                if (ofEnd == -1)
                    {
                    // missing closing PARAMETER_SUFFIX so no macro to process here
                    break;
                    }

                // check for nested property
                int ofNext = sValue.indexOf(PARAMETER_PREFIX, ofStart + 1);

                if (ofNext != -1 && ofNext < ofEnd)
                    {
                    // nested property
                    // find balanced PARAMETER_SUFFIX from ofNext position.
                    int ofNextEnd = indexOfMatchingPropertyClose(sValue, ofNext);
                    if (ofNextEnd == -1)
                        {
                        // missing PARAMETER_CLOSE so no macro to process here
                        break;
                        }

                    String sNestedMacro = substitute(sValue.substring(ofNext, ofNextEnd + 1), resolver, cDepth + 1);

                    sValue = replaceMacroParameter(sValue, ofNext, ofNextEnd + 1, sNestedMacro);

                    // find next outer ofEnd
                    ofEnd = indexOfMatchingPropertyClose(sValue, ofStart);
                    }

                int ofStartMacro  = ofStart;
                int ofEndMacro    = ofEnd + 1;
                String sMacro     = sValue.substring(ofStartMacro, ofEndMacro);
                String sPropValue = processRegisteredMacroExpansions(sMacro, resolver, cDepth);

                sValue = replaceMacroParameter(sValue, ofStartMacro, ofEndMacro, sPropValue);
                cDepth++;
                }
            }
        return sValue;
        }

    // ----- helpers -----------------------------------------------------------

    /**
     * Process macro expansion of <code>sMacro</code> by {@link #s_mapRegistry registered
     * macro-expansion processors}.
     *
     * @param sMacro    macro parameter
     * @param resolver  resolve macro parameter within <code>sMacro</code>
     * @param cDepth    count of macro parameter expansions
     *
     * @return result of macro parameter expansion processing
     */
    protected String processRegisteredMacroExpansions(String sMacro, ParameterResolver resolver, int cDepth)
        {
        String sPropValue = sMacro;

        for (Entry<String, MacroExpansionProcessor> entry : s_mapRegistry.entrySet())
            {
            MacroExpansionProcessor processor = entry.getValue();

            if (processor.canProcess(sMacro))
                {
                return processor.process(sPropValue, resolver, cDepth);
                }
            }

        if (NO_DELIMITER_MACRO_EXPANSION_PROCESSOR.canProcess(sMacro))
            {
            return NO_DELIMITER_MACRO_EXPANSION_PROCESSOR.process(sMacro, resolver, cDepth);
            }

        return sPropValue;
        }


    /**
     * Check if this contains a macro.
     *
     * @return true iff this contains a macro
     */
    public boolean containsMacro()
        {
        return containsMacro(m_sValue);
        }


    // ----- static helpers ----------------------------------------------------

    /**
     * Return <code>sValue</code> with macro parameter offset range expanded to
     * <code>sReplacement</code> value.
     *
     * @param sValue        string containing a macro from ofStart to ofEnd
     * @param ofStart       start offset of macro parameter within sValue
     * @param ofEnd         end offset of macro parameter within sValue
     * @param sReplacement  replacement string value for macro parameter from
     *                      ofStart to ofEnd
     *
     * @return sValue with its <code>sReplacement</code>> value
     */
    static String replaceMacroParameter(String sValue, int ofStart, int ofEnd, String sReplacement)
        {
        return sValue.substring(0, ofStart) + sReplacement + (ofEnd + 1 > sValue.length()
                                                              ? ""
                                                              : sValue.substring(ofEnd));
        }

    /**
     * Return the offset within string <code>sValue</code> for property close
     * for property starting at <code>ofPropertyStart</code>.
     *
     * @param sValue           string to process
     * @param ofPropertyStart  offset within string of {@link #PARAMETER_PREFIX}
     *
     * @return offset within <code>sValue</code> of matching property close or
     * -1 if no matching close
     */
    static int indexOfMatchingPropertyClose(String sValue, int ofPropertyStart)
        {
        int ofCur   = ofPropertyStart + 1;
        int nNested = 0;

        while (ofCur < sValue.length())
            {
            if (sValue.regionMatches(ofCur, PARAMETER_PREFIX, 0, PARAMETER_PREFIX.length()))
                {
                nNested++;
                ofCur += PARAMETER_PREFIX.length();
                }
            else if (sValue.charAt(ofCur) == PARAMETER_SUFFIX)
                {
                if (nNested == 0)
                    {
                    return ofCur;
                    }
                else
                    {
                    nNested--;
                    }
                ofCur++;
                }
            else
                {
                ofCur++;
                }
            }

        // property closing bracket not found, return -1
        return -1;
        }

    /**
     * Check if string contains a macro.
     *
     * @param sValue  string potentially containing a macro
     *
     * @return true iff the string value contains a macro
     */
    public static boolean containsMacro(String sValue)
        {
        if (sValue == null)
            {
            return false;
            }

        int ofStart = sValue.indexOf(PARAMETER_PREFIX);

        return ofStart >= 0 && sValue.indexOf(PARAMETER_SUFFIX, ofStart) > 0;
        }

    /**
     * Return true if <code>sMacro</code> contains a {@link #s_mapRegistry registered delimiter}.
     *
     * @param sMacro  macro parameter
     *
     * @return true if <code>sMacro</code> string contains a registered delimiter
     */
     protected static boolean containsRegisteredDelimiter(String sMacro)
        {
        for (String sDelimter : s_mapRegistry.keySet())
            {
            if (sMacro.contains(sDelimter))
                {
                return true;
                }
            }
        return false;
        }

    /**
     * Register {@link MacroExpansionProcessor processor} by <code>sDelimiter</code>.
     * When a macro parameter contains this delimiter, the registered processor will perform macro expansion.
     *
     * @param sDelimiter  macro parameter delimiter
     * @param processor   macro parameter processor for <code>sDelimiter</code>
     */
     static void register(String sDelimiter, MacroExpansionProcessor processor)
        {
        s_mapRegistry.put(sDelimiter, processor);
        }

    // ----- ExternalizableLite interface -----------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public void readExternal(DataInput in)
            throws IOException
        {
        m_sValue = ExternalizableHelper.readObject(in);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeExternal(DataOutput out)
            throws IOException
        {
        ExternalizableHelper.writeObject(out, m_sValue);
        }

    // ----- PortableObject interface ---------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public void readExternal(PofReader reader)
            throws IOException
        {
        m_sValue = reader.readObject(0);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeExternal(PofWriter writer)
            throws IOException
        {
        writer.writeObject(0, m_sValue);
        }

    // ----- Object methods -------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
        {
        return String.format("ValueMacroExpression[value=%s]", m_sValue);
        }

    // ----- inner class: MacroExpansionProcessor ---------------------

    /**
     * A Macro Expansion Processor for a macro not containing any registered delimiters.
     */
    protected static class MacroExpansionProcessor
        {
        // ----- MacroExpansionProcessor methods ----------------------

        /**
         * Process macro parameter expansion on <code>sMacro</code> containing no registered delimiters.
         *
         * @param sMacro    a string starting with {@link #PARAMETER_PREFIX prefix}
         *                  and ending with {@link #PARAMETER_SUFFIX suffix}
         * @param resolver  macro parameter ParameterResolver
         * @param cDepth    expansion depth
         *
         * @return expanded macro parameter or emptry string if macro parameter evaluates to null
         */
        public String process(String sMacro, ParameterResolver resolver, int cDepth)
            {
            String sDefault = "";
            String sProp = sMacro.substring(PARAMETER_PREFIX.length(), sMacro.length() - 1);

            try
                {
                Parameter param      = resolver.resolve(sProp);
                String    sPropValue = param == null
                                           ? sDefault
                                           : param.evaluate(resolver).as(String.class);

                return validateMacroExpansion(sProp, sPropValue, sDefault, cDepth) ? sPropValue : sDefault;
                }
            catch (Exception e)
                {
                return sDefault;
                }
            }

        /**
         * Return true iff <code>sMacro</code> contains the delimiter that this processor handles.
         *
         * @param sMacro  the macro parameter
         *
         * @return true iff this processor can process <code>sMacro</code>
         */
        public boolean canProcess(String sMacro)
            {
            return sMacro.startsWith(PARAMETER_PREFIX);
            }

        /**
         * Return delimiter being used by the processor.
         *
         * @return delimiter used by the processor or empty string if processor does not have a delimiter
         */
        public String getDelimiter()
            {
            return "";
            }

        /**
         * Validate macro expansion is not self referencing or contain circular references that will never complete expansion.
         *
         * @param sProp       the property
         * @param sPropValue  the expanded property value
         * @param cDepth      count of macro expansions
         *
         * @return false if self referencing in macro expansion or exceed {#link #MAX_MACRO_EXPANSIONS}; otherwise return true
         */
        public boolean validateMacroExpansion(String sProp, String sPropValue, String sDefault, int cDepth)
            {
            if (sPropValue.contains(PARAMETER_PREFIX + sProp) && sPropValue.contains(PARAMETER_SUFFIX.toString()) ||
                cDepth > MAX_MACRO_EXPANSIONS)
                {
                Logger.err("SystemPropertyPreprocessor: using default value of \"" + sDefault + "\", detected "
                           + "recursive macro definition in property "
                           + sProp + " with the value of \"" + sPropValue + "\" ");
                return false;
                }

            return true;
            }
        }

    // ----- inner class: DefaultDelimiterExpansion ----------------------------

    /**
     * Process macro parameter default delimiter expansion.
     */
    protected static class DefaultDelimiterExpansionProcessor
            extends MacroExpansionProcessor
        {
        // ----- constructors --------------------------------------------------

        /**
         * Perform default delimiter expansion using <code>sDefaultDelimiter</code>.
         *
         * @param sDefaultDelimiter  default delimiter
         */
        public DefaultDelimiterExpansionProcessor(String sDefaultDelimiter)
            {
            f_sDefaultDelimiter        = sDefaultDelimiter;
            f_fUseDefaultIfPropertySet = f_sDefaultDelimiter.equals(":+");
            }

        // ----- MacroExpansionProcessor methods ----------------------

        @Override
        public String getDelimiter()
            {
            return f_sDefaultDelimiter;
            }

        @Override
        public boolean canProcess(String sMacro)
            {
            return super.canProcess(sMacro) && sMacro.contains(f_sDefaultDelimiter);
            }

        @Override
        public String process(String sMacro, ParameterResolver resolver, int cDepth)
            {
            int ofStart = sMacro.indexOf(f_sDefaultDelimiter);

            if (ofStart > 0)
                {
                String sDefault = sMacro.substring(ofStart + f_sDefaultDelimiter.length(), sMacro.length() - 1).trim();
                String sProp    = sMacro.substring(PARAMETER_PREFIX.length(), ofStart);

                try
                    {
                    Parameter param      = resolver.resolve(sProp);
                    String    sPropValue = param == null ? null : param.evaluate(resolver).as(String.class);

                    sPropValue = (sPropValue == null && !f_fUseDefaultIfPropertySet) || (sPropValue != null && f_fUseDefaultIfPropertySet)
                                     ? sDefault
                                     : sPropValue;

                    return validateMacroExpansion(sProp, sPropValue, sDefault, cDepth) ? sPropValue : sDefault;
                    }
                catch (Exception e)
                    {
                    return sDefault;
                    }
                }

            // no default delimiter to process, so return macro
            return sMacro;
            }

        // ----- data members --------------------------------------------------

        /**
         * Default delimiter to process in macro parameter.
         */
        private final String f_sDefaultDelimiter;

        /**
         * Use default if property is set and does not resolve to a null value.
         */
        private final boolean f_fUseDefaultIfPropertySet;
        }

    // ----- inner class: SpaceDefaultDelimiterExpansionProcessor --------------

    /**
     * {@link SpaceDefaultDelimiterExpansionProcessor} performs DefaultDelimiter expansion processing
     * and disambiguates <code>: -</code> for offset and length from space for default delimiter.
     */
    protected static class SpaceDefaultDelimiterExpansionProcessor
        extends DefaultDelimiterExpansionProcessor
        {
        // ----- constructors --------------------------------------------------

        /**
         * Construct a {@link SpaceDefaultDelimiterExpansionProcessor}.
         */
        public SpaceDefaultDelimiterExpansionProcessor()
            {
            super(" ");
            }

        // ----- MacroExpansionProcessor methods ----------------------

        @Override
        public boolean canProcess(String sMacro)
            {
            final String NEGATIVE_OFFSET_OR_LENGTH = ": -";

            return super.canProcess(sMacro) && !sMacro.contains(NEGATIVE_OFFSET_OR_LENGTH) && sMacro.contains(getDelimiter());
            }

        @Override
        public String process(String sMacro, ParameterResolver resolver, int cDepth)
            {
            return super.process(sMacro, resolver, cDepth);
            }
        }

    // ----- inner class: OffsetLengthSubstringExpansion -----------------------

    /**
     * Process <code>:offset</code> and <code>:length<code></code> substring expansion.
     *
     * @see #process(String, ParameterResolver, int)
     */
    protected static class OffsetLengthSubstringExpansionProcessor
        extends MacroExpansionProcessor
        {
        // ----- MacroExpansionProcessor methods ----------------------

        @Override
        public String getDelimiter()
            {
            return SUBSTRING_OFFSET_LENGTH_EXPANSION;
            }

        @Override
        public boolean canProcess(String sMacro)
            {
            return super.canProcess(sMacro) && sMacro.contains(getDelimiter()) && !sMacro.contains(":-");
            }

        /**
         * Perform substring expansion on <code>sMacro</code>.
         *
         * <pre>
         * <i>${macro-parameter:offset}</i>
         * <i>${macro-parameter:offset:length}</i>
         * Note that <i>length</i> and <i>offset</i> are integer values.
         * </pre>
         * Substring expansion expands to up to <tt><i>length</i></tt> characters of the value of <tt><i>macro-parameter</i></tt>
         * starting at the character specified by <tt><i>offset</i></tt>.
         * <br>
         * If <tt><i>length</i></tt> is omitted, it expands to the substring of the value of <tt><i>macro-parameter</i></tt>
         * starting at the character specified by <tt><i>offset</i></tt>
         * and extending to the end of the value.
         * <br>
         * If <tt><i>offset</i></tt> evaluates to a number less than zero, the value is used as an
         * offset in characters from the end of the value of <tt><i>macro-parameter</i></tt>.
         * <br>
         * If <tt><i>length</i></tt> evaluates to a number less than zero, it is interpreted as an
         * offset in characters from the end of the value of <tt><i>macro-parameter</i></tt>.
         * rather than a number of characters, and the expansion is the characters between offset and that result.
         * <p>
         * Note that a negative <tt><i>offset</i></tt> or <tt><i>length</i></tt> must be separated from the
         * colon by at least one space to avoid being confused with the macro parameter default delimiter <tt><i>:-</i></tt>.
         * <p>
         * Examples illustrating substring expansion of a parameter:
         * <pre>
         * Given property <tt><i>parameter</i> of string value <i>01234567890abcdefgh</i>.
         *
         * ${<i>parameter</i>:7} evaluates to <i>7890abcdefgh</i>
         * ${<i>parameter</i>:7:0} evaluates to <i>empty string</i>
         * ${<i>parameter</i>:7:2} evaluates to <i>78</i>
         * ${<i>parameter</i>:7: -2} evaluates to <i>7890abcdef</i>
         * ${<i>parameter</i>: -7} evaluates to <i>bcdefgh</i>
         * ${<i>parameter</i>: -7:0} evaluates to <i>empty string</i>
         * ${<i>parameter</i>: -7:2} evaluates to <i>bc</i>
         * ${<i>parameter</i>: -7: -2} evaluates to <i>bcdef</i>
         * </pre>
         *
         * @param sMacro    a string starting with {@link #PARAMETER_PREFIX prefix}
         *                  and ending with {@link #PARAMETER_SUFFIX suffix}
         * @param resolver  macro parameter resolver
         * @param cDepth    count of current macro parameter expansions
         *
         * @return the substring expanded value or the original parameter macro if it does not contain substring expansion
         */
        @Override
        public String process(String sMacro, ParameterResolver resolver, int cDepth)
            {
            int sLen           = sMacro.length();
            int ofSuffix       = sMacro.indexOf(getDelimiter());
            int ofLengthSuffix;
            int ofMacroClose   = sMacro.indexOf(PARAMETER_SUFFIX);
            int ofParsed       = Integer.MAX_VALUE;
            int cLength        = Integer.MAX_VALUE;
            int ofEnd          = 0;

            if (ofSuffix > 0)
                {
                if (ofSuffix + 1 < sLen)
                    {
                    char c = sMacro.charAt(ofSuffix + 1);
                    if (c == '+' || c == '-')
                        {
                        // found defaulting suffix :+ or :- so return unprocessed.
                        return sMacro;
                        }

                    ofLengthSuffix = sMacro.indexOf(':', ofSuffix + 1);
                    try
                        {
                        ofEnd    = ofLengthSuffix > 0 ? ofLengthSuffix : ofMacroClose;
                        ofParsed = Integer.parseInt(sMacro.substring(ofSuffix + 1, ofEnd).trim());
                        }
                    catch(NumberFormatException e)
                        {
                        throw new IllegalStateException("parsing error processing integer offset " +
                                                        sMacro.substring(ofSuffix + 1, ofEnd) + " within macro " + sMacro, e);
                        }

                    if (ofLengthSuffix != -1)
                        {
                        try
                            {
                            cLength = Integer.parseInt(sMacro.substring(ofLengthSuffix + 1, ofMacroClose).trim());
                            }
                        catch (NumberFormatException e)
                            {
                            throw new IllegalStateException("parsing error processing integer length  " +
                                                            sMacro.substring(ofLengthSuffix + 1, ofMacroClose) + " within macro " + sMacro, e);
                            }
                        }

                    String    sProp      = sMacro.substring(PARAMETER_PREFIX.length(), ofSuffix);
                    Parameter param      = resolver.resolve(sProp);
                    String    sPropValue = param == null ? null : param.evaluate(resolver).as(String.class);

                    cLength = cLength == Integer.MAX_VALUE ? sPropValue.length() : cLength;

                    ofParsed += ofParsed < 0 ? sPropValue.length() : 0;
                    cLength  += cLength  < 0 ? sPropValue.length() : ofParsed;

                    int ofComputedEnd = cLength > sPropValue.length() ? sPropValue.length() : cLength;

                    return ofParsed < 0 ? "" : sPropValue.substring(ofParsed, ofComputedEnd);
                    }
                }
            return sMacro;
            }
        }

    // ----- constants ------------------------------------------------------

    /**
     * Avoid recursive macro expansions that never return.  No need for more than 20 macro expansions on
     * one value.
     */
    public static int MAX_MACRO_EXPANSIONS = 20;

    /**
     * Prefix indicating the start of a property macro.
     */
    public final static String PARAMETER_PREFIX = "${";

    /**
     * Suffix indicating the close of a property macro.
     */
    public final static Character PARAMETER_SUFFIX = '}';

    /**
     * Delimiter introducing substring expansion of optional :offset and/or :length in a macro parameter.
     */
    public final static String SUBSTRING_OFFSET_LENGTH_EXPANSION = ":";

    // ----- data members ------------------------------------------------------

    /**
     * Registry of macro parameter delimiters to processors.
     */
    private static Map<String, MacroExpansionProcessor> s_mapRegistry =
            new HashMap<String, MacroExpansionProcessor>();

    /**
     *  No registered delimiter macro expansion processor.
     */
    private static final MacroExpansionProcessor NO_DELIMITER_MACRO_EXPANSION_PROCESSOR =
            new MacroExpansionProcessor();

    /**
     * The String value.
     */
    private String m_sValue;

    // ----- static initialization ---------------------------------------------

    static
        {
        register(":",  new OffsetLengthSubstringExpansionProcessor());
        register(":-", new DefaultDelimiterExpansionProcessor(":-"));
        register(":+", new DefaultDelimiterExpansionProcessor(":+"));
        register(" ",  new SpaceDefaultDelimiterExpansionProcessor());
        }
    }
