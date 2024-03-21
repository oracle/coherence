/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util.filter;


import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;

import com.tangosol.util.ChainedCollection;
import com.tangosol.util.Filter;
import com.tangosol.util.MapIndex;
import com.tangosol.util.ValueExtractor;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

import javax.json.bind.annotation.JsonbCreator;
import javax.json.bind.annotation.JsonbProperty;


/**
* Filter which compares the result of a method invocation with a value for
* pattern match. A pattern can include regular characters and wildcard
* characters '_' and '%'.
* <p>
* During pattern matching, regular characters must exactly match the
* characters in an evaluated string. Wildcard character '_' (underscore) can
* be matched with any single character, and wildcard character '%' can be
* matched with any string fragment of zero or more characters.
*
* @param <T> the type of the input argument to the filter
* @param <E> the type of the extracted attribute to use for comparison
*
* @author cp/gg 2002.10.27
*/
@SuppressWarnings({"unchecked", "rawtypes"})
public class LikeFilter<T, E>
        extends    ComparisonFilter<T, E, String>
        implements IndexAwareFilter<Object, T>
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor (necessary for the ExternalizableLite interface).
    */
    public LikeFilter()
        {
        }

    /**
    * Construct a LikeFilter for pattern match.
    *
    * @param sMethod  the name of the method to invoke via reflection
    * @param sPattern the string pattern to compare the result with
    */
    public LikeFilter(String sMethod, String sPattern)
        {
        this(sMethod, sPattern, (char) 0, false);
        }

    /**
    * Construct a LikeFilter for pattern match.
    *
    * @param sMethod      the name of the method to invoke via reflection
    * @param sPattern     the string pattern to compare the result with
    * @param fIgnoreCase  true to be case-insensitive
    */
    public LikeFilter(String sMethod, String sPattern, boolean fIgnoreCase)
        {
        this(sMethod, sPattern, (char) 0, fIgnoreCase);
        }

    /**
    * Construct a LikeFilter for pattern match.
    *
    * @param sMethod      the name of the method to invoke via reflection
    * @param sPattern     the string pattern to compare the result with
    * @param chEscape     the escape character for escaping '%' and '_'
    * @param fIgnoreCase  true to be case-insensitive
    */
    public LikeFilter(String sMethod, String sPattern, char chEscape, boolean fIgnoreCase)
        {
        super(sMethod, sPattern);
        init(chEscape, fIgnoreCase);
        }

    /**
    * Construct a LikeFilter for pattern match.
    *
    * @param extractor  the {@link ValueExtractor} used by this filter
    * @param sPattern   the string pattern to compare the result with
    */
    public LikeFilter(ValueExtractor<? super T, ? extends E> extractor, String sPattern)
        {
        this(extractor, sPattern, (char) 0, false);
        }

    /**
    * Construct a LikeFilter for pattern match.
    *
    * @param extractor    the {@link ValueExtractor} used by this filter
    * @param sPattern     the string pattern to compare the result with
    * @param chEscape     the escape character for escaping '%' and '_'
    * @param fIgnoreCase  true to be case-insensitive
    */
    @JsonbCreator
    public LikeFilter(@JsonbProperty("extractor")
                              ValueExtractor<? super T, ? extends E> extractor,
                      @JsonbProperty("value")
                              String sPattern,
                      @JsonbProperty("escapeChar")
                              char chEscape,
                      @JsonbProperty("ignoreCase")
                              boolean fIgnoreCase)
        {
        super(extractor, sPattern);
        init(chEscape, fIgnoreCase);
        }

    /**
    * Initialize this filter.
    *
    * @param chEscape     the escape character for escaping '%' and '_'
    * @param fIgnoreCase  true to be case-insensitive
    */
    private void init(char chEscape, boolean fIgnoreCase)
        {
        m_chEscape    = chEscape;
        m_fIgnoreCase = fIgnoreCase;
        buildPlan();
        }

    // ----- Filter interface -----------------------------------------------

    protected String getOperator()
        {
        return "LIKE";
        }

    // ----- ExtractorFilter methods ----------------------------------------

    /**
    * {@inheritDoc}
    */
    protected boolean evaluateExtracted(E extracted)
        {
        try
            {
            String sValue = extracted == null ? null : String.valueOf(extracted);
            return isMatch(sValue);
            }
        catch (ClassCastException e)
            {
            return false;
            }
        }


    // ----- IndexAwareFilter interface -------------------------------------

    /**
    * {@inheritDoc}
    */
    public int calculateEffectiveness(Map mapIndexes, Set setKeys)
        {
        int nPlan = m_nPlan;
        if (m_nPlan == ALWAYS_FALSE)
            {
            return 0;
            }

        MapIndex index = (MapIndex) mapIndexes.get(getValueExtractor());
        if (index == null)
            {
            // there is no relevant index
            return -1;
            }

        Map<E, Set<?>> mapContents = index.getIndexContents();
        if (mapContents.isEmpty())
            {
            return 0;
            }

        switch (nPlan)
            {
            case ALWAYS_TRUE:
                return mapContents.values().stream().mapToInt(Set::size).sum();

            case EXACT_MATCH:
                return ensureSafeSet(mapContents.get(m_sPart)).size();

            case STARTS_WITH_CHAR:
            case STARTS_WITH_STRING:
                {
                try
                    {
                    if (index.isOrdered())
                        {
                        String sPrefix = nPlan == STARTS_WITH_STRING
                                         ? m_sPart
                                         : String.valueOf(m_chPart);

                        SortedMap<?, Set<?>> mapTail = ((SortedMap) mapContents).tailMap(sPrefix);
                        int cMatch = 0;
                        for (Map.Entry<?, Set<?>> entry : mapTail.entrySet())
                            {
                            String sValue = (String) entry.getKey();

                            if (sValue != null && sValue.startsWith(sPrefix))
                                {
                                cMatch += ensureSafeSet(entry.getValue()).size();
                                }
                            else
                                {
                                break;
                                }
                            }
                        return cMatch;
                        }
                    }
                catch (ClassCastException e)
                    {
                    // incompatible types; go the long way
                    }

                // fall through
                }

            default:
                {
                int cMatch = 0;
                for (Map.Entry<E, Set<?>> entry : mapContents.entrySet())
                    {
                    E value = entry.getKey();

                    String sValue = value == null
                                    ? null
                                    : String.valueOf(value);
                    if (isMatch(sValue))
                        {
                        cMatch += ensureSafeSet(entry.getValue()).size();
                        }
                    }

                return cMatch;
                }
            }
        }

    /**
    * {@inheritDoc}
    */
    public Filter applyIndex(Map mapIndexes, Set setKeys)
        {
        int nPlan = m_nPlan;
        switch (nPlan)
            {
            case ALWAYS_FALSE:
                setKeys.clear();
                return null;

            case ALWAYS_TRUE:
                return null;
            }

        MapIndex index = (MapIndex) mapIndexes.get(getValueExtractor());
        if (index == null)
            {
            // there is no relevant index
            return this;
            }
        else if (index.getIndexContents().isEmpty())
           {
           // there are no entries in the index, which means no entries match this filter
           setKeys.clear();
           return null;
           }

        if (nPlan == EXACT_MATCH)
            {
            Set setEquals = (Set) index.getIndexContents().get(m_sPart);
            if (setEquals == null || setEquals.isEmpty())
                {
                setKeys.clear();
                }
            else
                {
                setKeys.retainAll(setEquals);
                }
            return null;
            }

        Map<E, Set<?>> mapContents = index.getIndexContents();

        if ((nPlan == STARTS_WITH_STRING || nPlan == STARTS_WITH_CHAR) && index.isOrdered())
            {
            try
                {
                String sPrefix = nPlan == STARTS_WITH_STRING ? m_sPart : String.valueOf(m_chPart);

                SortedMap<String, Set<?>> mapTail   = ((SortedMap<String, Set<?>>) mapContents).tailMap(sPrefix);
                List<Set<?>>              listMatch = new ArrayList<>(mapTail.size());
                for (Map.Entry<String, Set<?>> entry : mapTail.entrySet())
                    {
                    String sValue = entry.getKey();

                    if (sValue != null && sValue.startsWith(sPrefix))
                        {
                        listMatch.add(ensureSafeSet(entry.getValue()));
                        }
                    else
                        {
                        break;
                        }
                    }
                setKeys.retainAll(new ChainedCollection<>(listMatch.toArray(Set[]::new)));
                return null;
                }
            catch (ClassCastException e)
                {
                // incompatible types; go the long way
                }
            }

        List<Set<?>> listMatch = new ArrayList<>(mapContents.size());
        for (Map.Entry<E, Set<?>> entry : mapContents.entrySet())
            {
            E value = entry.getKey();

            String sValue = value == null ? null : String.valueOf(value);
            if (isMatch(sValue))
                {
                listMatch.add(ensureSafeSet(entry.getValue()));
                }
            }
        setKeys.retainAll(new ChainedCollection<>(listMatch.toArray(Set[]::new)));

        return null;
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Obtain the filter's pattern string.
    *
    * @return the pattern string
    */
    public String getPattern()
        {
        return getValue();
        }

    /**
    * Check whether or not the filter is case incensitive.
    *
    * @return true iff case insensitivity is specifically enabled
    */
    public boolean isIgnoreCase()
        {
        return m_fIgnoreCase;
        }

    /**
    * Obtain the escape character that is used for escaping '%' and '_' in
    * the pattern or zero if there is no escape.
    *
    * @return the escape character
    */
    public char getEscapeChar()
        {
        return m_chEscape;
        }

    /**
    * Display the execution plan that the LikeFilter has selected.
    */
    public void showPlan()
        {
        out("Plan for case-" + (isIgnoreCase() ? "in" : "")
            + "sensitive LIKE \"" + getPattern() + "\" (escape=\""
            + getEscapeChar() + "\") is \"" + PLAN_NAMES[m_nPlan] + "\"");
        out("initial step: " + m_stepFront);
        MatchStep[] astep = m_astepMiddle;
        if (astep != null && astep.length > 0)
            {
            for (int i = 0, c = astep.length; i < c; ++i)
                {
                out("step " + (i+1) + ": " + astep[i]);
                }
            }
        out("final step: " + m_stepBack);
        out();
        }


    // ----- ExternalizableLite interface -----------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(DataInput in)
            throws IOException
        {
        super.readExternal(in);

        m_fIgnoreCase = in.readBoolean();
        m_chEscape    = in.readChar();

        buildPlan();
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(DataOutput out)
            throws IOException
        {
        super.writeExternal(out);

        out.writeBoolean(m_fIgnoreCase);
        out.writeChar(m_chEscape);
        }


    // ----- PortableObject interface ---------------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(PofReader in)
            throws IOException
        {
        super.readExternal(in);

        m_fIgnoreCase = in.readBoolean(2);
        m_chEscape    = in.readChar(3);

        buildPlan();
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(PofWriter out)
            throws IOException
        {
        super.writeExternal(out);

        out.writeBoolean(2, m_fIgnoreCase);
        out.writeChar(3, m_chEscape);
        }


    // ----- Serializable pseudo-interface ----------------------------------

    /**
    * Special handling during standard serialization. This is necessary in
    * case this class is used outside of the ExternalizableLite framework.
    */
    private void readObject(java.io.ObjectInputStream in)
            throws IOException, ClassNotFoundException
        {
        in.defaultReadObject();

        buildPlan();
        }


    // ----- internal methods -----------------------------------------------

    /**
    * Build a plan for processing the LIKE functionality.
    */
    protected void buildPlan()
        {
        String sPattern = getPattern();
        if (sPattern == null)
            {
            // the result of "v LIKE NULL" is false for all values of "v"
            m_nPlan = ALWAYS_FALSE;
            return;
            }

        char[]        achPattern  = sPattern.toCharArray();
        int           cchPattern  = achPattern.length;
        char          chEscape    = getEscapeChar();
        boolean       fEscape     = false;
        boolean       fIgnoreCase = isIgnoreCase();
        StringBuilder sb          = null;
        BitSet        bitset      = null;
        List          list        = new ArrayList();

        // parse the pattern into a list of steps
        for (int of = 0; of < cchPattern; ++of)
            {
            char ch = achPattern[of];
            if (fEscape)
                {
                fEscape = false;
                }
            else if (ch == chEscape)
                {
                fEscape = true;
                continue;
                }
            else if (ch == '%')
                {
                if (sb != null)
                    {
                    list.add(new MatchStep(sb, bitset, fIgnoreCase));
                    sb     = null;
                    bitset = null;
                    }

                if (list.isEmpty() || list.get(list.size()-1) != ANY)
                    {
                    list.add(ANY);
                    }
                continue;
                }
            else if (ch == '_')
                {
                if (bitset == null)
                    {
                    bitset = new BitSet();
                    }
                bitset.set(sb == null ? 0 : sb.length());
                }

            if (sb == null)
                {
                sb = new StringBuilder();
                }
            sb.append(ch);
            }

        // check for unclosed escape
        if (fEscape)
            {
            throw new IllegalArgumentException("pattern ends with an unclosed escape: \""
                    + sPattern + "\"");
            }

        // store off the last match step (if there is one)
        if (sb != null)
            {
            list.add(new MatchStep(sb, bitset, fIgnoreCase));
            }

        // check for simple optimizations
        switch (list.size())
            {
            case 0:
                // case sensistive     case insensitive    pattern
                // ------------------  ------------------  -------
                // EXACT_MATCH         EXACT_MATCH         ""
                m_nPlan = EXACT_MATCH;
                m_sPart = "";
                return;

            case 1:
                // case sensistive     case insensitive    pattern
                // ------------------  ------------------  -------
                // EXACT_MATCH         INSENS_MATCH        "xyz"  (no wildcards)
                // ALWAYS_TRUE         ALWAYS_TRUE         "%"    (only '%' wildcards)
                {
                Object o = list.get(0);
                if (o == ANY)
                    {
                    m_nPlan = ALWAYS_TRUE;
                    return;
                    }

                MatchStep matchstep = (MatchStep) o;
                if (matchstep.isLiteral())
                    {
                    m_nPlan = fIgnoreCase ? INSENS_MATCH : EXACT_MATCH;

                    // matchstep may contain escaped chars (such as '_')
                    m_sPart = matchstep.m_sMatch;
                    return;
                    }
                }
                break;

            case 2:
                // case sensistive     case insensitive    pattern
                // ------------------  ------------------  -------
                // STARTS_WITH_CHAR    STARTS_WITH_INSENS  "x%"
                // STARTS_WITH_STRING  STARTS_WITH_INSENS  "xyz%"
                // ENDS_WITH_CHAR      ENDS_WITH_INSENS    "%x"
                // ENDS_WITH_STRING    ENDS_WITH_INSENS    "%xyz"
                {
                MatchStep matchstep;
                boolean   fStartsWith;
                Object    o = list.get(0);
                if (o == ANY)
                    {
                    fStartsWith = false;
                    matchstep   = (MatchStep) list.get(1);
                    }
                else
                    {
                    fStartsWith = true;
                    matchstep   = (MatchStep) o;
                    }
                if (matchstep.isLiteral())
                    {
                    if (fIgnoreCase)
                        {
                        m_nPlan = fStartsWith ? STARTS_WITH_INSENS : ENDS_WITH_INSENS;
                        m_sPart = matchstep.getString();
                        }
                    else if (matchstep.getLength() == 1)
                        {
                        m_nPlan  = fStartsWith ? STARTS_WITH_CHAR : ENDS_WITH_CHAR;
                        m_chPart = matchstep.getString().charAt(0);
                        }
                    else
                        {
                        m_nPlan = fStartsWith ? STARTS_WITH_STRING : ENDS_WITH_STRING;
                        m_sPart = matchstep.getString();
                        }
                    return;
                    }
                }
                break;

            case 3:
                // case sensistive     case insensitive    pattern
                // ------------------  ------------------  -------
                // CONTAINS_CHAR       n/a                 "%x%"
                // CONTAINS_STRING     n/a                 "%xyz%"
                {
                if (!fIgnoreCase)
                    {
                    Object o = list.get(1);
                    if (o != ANY)
                        {
                        MatchStep matchstep = (MatchStep) o;
                        if (matchstep.isLiteral())
                            {
                            if (matchstep.getLength() == 1)
                                {
                                m_nPlan  = CONTAINS_CHAR;
                                m_chPart = matchstep.getString().charAt(0);
                                }
                            else
                                {
                                m_nPlan = CONTAINS_STRING;
                                m_sPart = matchstep.getString();
                                }
                            return;
                            }
                        }
                    }
                }
                break;
            }

        // build iterative plan
        // # steps  description
        // -------  --------------------------------------------------------
        //    1     match with '_'
        //    2     starts with or ends with match with '_'
        //    3     starts and ends with matches, or contains match with '_'
        //    4+    alternating % and matches, potentially starting with
        //          and/or ending with matches, each could have '_'
        m_nPlan = ITERATIVE_EVAL;
        switch (list.size())
            {
            case 0:
                throw azzert();

            case 1:
                m_stepFront = (MatchStep) list.get(0);
                m_fTrailingTextAllowed = false;
                break;

            case 2:
                {
                Object step1 = list.get(0);
                Object step2 = list.get(1);

                // should not have two "ANYs" in a row, but one must be ANY
                azzert(step1 == ANY ^ step2 == ANY);

                if (step1 == ANY)
                    {
                    m_stepBack = (MatchStep) step2;
                    m_fTrailingTextAllowed = false;
                    }
                else
                    {
                    m_stepFront = (MatchStep) step1;
                    m_fTrailingTextAllowed = true;
                    }
                }
                break;

            default:
                {
                int cMatchSteps   = list.size();

                // figure out where the "middle" is; the "middle" is defined
                // as those steps that occur after one or more '%' matches
                // and before one or more '%' matches
                int ofStartMiddle = 1;               // offset in list of first middle step
                int ofEndMiddle   = cMatchSteps - 2; // offset in list of last middle step

                Object oFirst = list.get(0);
                if (oFirst != ANY)
                    {
                    m_stepFront = (MatchStep) oFirst;
                    ++ofStartMiddle;
                    }

                Object oLast = list.get(cMatchSteps - 1);
                boolean fLastStepIsAny = (oLast == ANY);
                if (!fLastStepIsAny)
                    {
                    m_stepBack = (MatchStep) oLast;
                    --ofEndMiddle;
                    }
                m_fTrailingTextAllowed = fLastStepIsAny;

                int         cMatches = (ofEndMiddle - ofStartMiddle) / 2 + 1;
                MatchStep[] aMatches = new MatchStep[cMatches];
                int         nMatch   = 0;
                for (int of = ofStartMiddle; of <= ofEndMiddle; of += 2)
                    {
                    aMatches[nMatch++] = (MatchStep) list.get(of);
                    }
                m_astepMiddle = aMatches;
                }
            }
        }

    /**
    * Check the passed String value to see if it matches the pattern that
    * this filter was constructed with.
    *
    * @param sValue  the String value to match against this filter's pattern
    *
    * @return true iff the passed String value is LIKE this filter's pattern
    */
    protected boolean isMatch(String sValue)
        {
        if (sValue == null)
            {
            // null is not like anything
            return false;
            }

        int cchValue = sValue.length();
        switch (m_nPlan)
            {
            case STARTS_WITH_CHAR:
                return cchValue >= 1 && sValue.charAt(0) == m_chPart;

            case STARTS_WITH_STRING:
                return sValue.startsWith(m_sPart);

            case STARTS_WITH_INSENS:
                {
                String sPrefix   = m_sPart;
                int    cchPrefix = sPrefix.length();
                if (cchPrefix > cchValue)
                    {
                    return false;
                    }
                return sValue.regionMatches(true, 0, sPrefix, 0, cchPrefix);
                }

            case ENDS_WITH_CHAR:
                return cchValue >= 1 && sValue.charAt(cchValue - 1) == m_chPart;

            case ENDS_WITH_STRING:
                return sValue.endsWith(m_sPart);

            case ENDS_WITH_INSENS:
                {
                String sSuffix   = m_sPart;
                int    cchSuffix = sSuffix.length();
                if (cchSuffix > cchValue)
                    {
                    return false;
                    }
                return sValue.regionMatches(true, cchValue - cchSuffix, sSuffix, 0, cchSuffix);
                }

            case CONTAINS_CHAR:
                return sValue.indexOf(m_chPart) >= 0;

            case CONTAINS_STRING:
                return sValue.indexOf(m_sPart) >= 0;

            case ALWAYS_TRUE:
                return true;

            case ALWAYS_FALSE:
                return false;

            case EXACT_MATCH:
                return m_sPart.equals(sValue);

            case INSENS_MATCH:
                return m_sPart.equalsIgnoreCase(sValue);
            }

        // get the character data and iteratively process the LIKE
        char[] ach     = sValue.toCharArray();
        int    cch     = ach.length;
        int    ofBegin = 0;
        int    ofEnd   = cch;

        // start by checking the front
        MatchStep matchstep = m_stepFront;
        if (matchstep != null)
            {
            int cchStep = matchstep.getLength();
            if (cchStep > cch || matchstep.indexOf(ach, ofBegin, cchStep) < 0)
                {
                return false;
                }
            else
                {
                ofBegin = cchStep;
                }
            }

        // next check the back
        matchstep = m_stepBack;
        if (matchstep != null)
            {
            int cchStep = matchstep.getLength();
            int ofStep  = cch - cchStep;
            if (ofStep < ofBegin || matchstep.indexOf(ach, ofStep, ofEnd) < 0)
                {
                return false;
                }
            else
                {
                ofEnd = ofStep;
                }
            }

        // check the middle
        MatchStep[] amatchstep = m_astepMiddle;
        if (amatchstep != null)
            {
            for (int i = 0, c = amatchstep.length; i < c; ++i)
                {
                matchstep = amatchstep[i];
                int of = matchstep.indexOf(ach, ofBegin, ofEnd);
                if (of < 0)
                    {
                    return false;
                    }
                else
                    {
                    ofBegin = of + matchstep.getLength();
                    }
                }
            }

        // this is the "is there anything left" check, which solves an
        // ambiguity in the "iterative step" design that did not correctly
        // differentiate between "%a_%" and "%a_", for example
        if (m_stepBack == null && !m_fTrailingTextAllowed)
            {
            if (ofBegin != cch)
                {
                return false;
                }
            }

        return true;
        }


    // ----- inner class: MatchStep -----------------------------------------

    /**
    * Handles one matching step for a literal or a character-by-character
    * (literal and/or '_' matching).
    */
    private static class MatchStep
            implements Serializable
        {
        // ----- constructors -------------------------------------------

        /**
        * Construct a MatchStep object.
        *
        * @param sb      the StringBuilder of characters to match in this step
        * @param bitset  corresponding to each character, true if any
        *                character is allowed ('_')
        */
        public MatchStep(StringBuilder sb, BitSet bitset, boolean fIgnoreCase)
            {
            String    sMatch   = sb.toString();
            char[]    achMatch = sMatch.toCharArray();
            char[]    achLower = null;
            boolean[] afAny    = null;

            int     cchSkipFront = 0;       // count of leading wildcards
            int     cchSkipBack  = 0;       // count of trailing wildcards
            boolean fMiddleWilds = false;   // true iff any wildcards occur
                                            // in the middle of non-wildcards
            if (bitset != null)
                {
                int cch = achMatch.length;
                afAny = new boolean[cch];   // true for each char that is a wildcard
                boolean fFront = true;      // false iff a non-wildcard is encountered
                int     cWilds = 0;         // total number of wildcards
                int     cCont  = 0;         // current count of continuous wildcards
                for (int i = 0; i < cch; ++i)
                    {
                    if (bitset.get(i))      // indicates a wildcard
                        {
                        afAny[i] = true;
                        if (fFront)
                            {
                            ++cchSkipFront;
                            }
                        ++cWilds;
                        ++cCont;
                        }
                    else
                        {
                        fFront = false;
                        cCont  = 0;
                        }
                    }
                if (cCont > 0 && cCont < cWilds)
                    {
                    cchSkipBack = cCont;    // trailing continuous wildcards
                    }
                fMiddleWilds = (cWilds > (cchSkipFront + cchSkipBack));
                }

            if (fIgnoreCase)
                {
                // create both "upper" and "lower" case characters for the
                // literal characters that need to be matched
                int cch = achMatch.length;
                achLower = new char[cch];
                for (int of = 0; of < cch; ++of)
                    {
                    char ch = achMatch[of];
                    if (afAny == null || !afAny[of])
                        {
                        ch = Character.toUpperCase(ch);
                        achMatch[of] = ch;
                        ch = Character.toLowerCase(ch);
                        }
                    achLower[of] = ch;
                    }
                }

            m_sMatch       = sMatch;
            m_achMatch     = achMatch;
            m_achLower     = achLower;
            m_afAny        = afAny;
            m_cchSkipFront = cchSkipFront;
            m_cchSkipBack  = cchSkipBack;
            m_fMiddleWilds = fMiddleWilds;
            m_fIgnoreCase  = fIgnoreCase;
            }

        // ----- accessors ----------------------------------------------

        /**
        * @return the match pattern as a String
        */
        public String getString()
            {
            return m_sMatch;
            }

        /**
        * @return the length of the match pattern
        */
        public int getLength()
            {
            return m_achMatch.length;
            }

        /**
        * @return true if there are no wildcards ('_') in the match pattern
        */
        public boolean isLiteral()
            {
            return m_afAny == null;
            }

        // ----- Object methods -----------------------------------------

        /**
        * @return a human-readable description for debugging purposes
        */
        public String toString()
            {
            return "MatchStep(" + m_sMatch + ", " + (m_afAny == null ? "exact" : "wild") + ')';
            }

        // ----- matching methods ---------------------------------------

        /**
        * Find the first index of this match step in the passed character
        * array starting at the passed offset and within the specified
        * number of characters.
        *
        * @param ach       the array of characters within which to find a
        *                  match
        * @param ofBegin   the starting offset in character array<tt>ach</tt>
        *                  to start looking for a match
        * @param ofEnd     the first offset in the character array
        *                  <tt>ach</tt> which is beyond the region that this
        *                  operation is allowed to search through to find a
        *                  match
        *
        * @return the first index at which the match is made, or -1 if the
        *         match cannot be made in the designated range of offsets
        */
        public int indexOf(char[] ach, int ofBegin, int ofEnd)
            {
            char[] achMatch = m_achMatch;
            int    cchMatch = achMatch.length;
            int    cch      = ofEnd - ofBegin;
            if (cchMatch > cch)
                {
                // doesn't fit: can't match
                return -1;
                }

            int cchSkipFront = m_cchSkipFront;
            if (cchSkipFront > 0)
                {
                if (cchSkipFront == cchMatch)
                    {
                    // just wildcards; found it if it fits
                    return ofBegin;
                    }

                // do not bother to match leading wildcards
                ofBegin += cchSkipFront;
                ofEnd   += cchSkipFront;
                }

            ofEnd    -= cchMatch;       // determine last offset that allows it to fit
            cchMatch -= m_cchSkipBack;  // don't bother matching trailing wilds

            boolean   fMiddleWilds = m_fMiddleWilds;
            boolean[] afAny        = m_afAny;

            if (m_fIgnoreCase)
                {
                // processed in an equivalent way to String.equalsIngoreCase()
                char[] achLower     = m_achLower;
                char   chFirstUpper = achMatch[cchSkipFront];
                char   chFirstLower = achLower[cchSkipFront];
                NextChar: for ( ; ofBegin <= ofEnd; ++ofBegin)
                    {
                    char ch = ach[ofBegin];
                    if (ch == chFirstUpper || ch == chFirstLower)
                        {
                        if (fMiddleWilds)
                            {
                            for (int ofMatch = cchSkipFront + 1, ofCur = ofBegin + 1;
                                    ofMatch < cchMatch; ++ofMatch, ++ofCur)
                                {
                                if (!afAny[ofMatch])
                                    {
                                    ch = ach[ofCur];
                                    if (ch != achMatch[ofMatch] && ch != achLower[ofMatch])
                                        {
                                        continue NextChar;
                                        }
                                    }
                                }
                            }
                        else
                            {
                            for (int ofMatch = cchSkipFront + 1, ofCur = ofBegin + 1;
                                    ofMatch < cchMatch; ++ofMatch, ++ofCur)
                                {
                                ch = ach[ofCur];
                                if (ch != achMatch[ofMatch] && ch != achLower[ofMatch])
                                    {
                                    continue NextChar;
                                    }
                                }
                            }

                        // found it; adjust for the leading wilds that we skipped matching
                        return ofBegin - cchSkipFront;
                        }
                    }
                }
            else
                {
                // scan for a match
                char chFirst = achMatch[cchSkipFront];
                NextChar: for ( ; ofBegin <= ofEnd; ++ofBegin)
                    {
                    if (ach[ofBegin] == chFirst)
                        {
                        if (fMiddleWilds)
                            {
                            for (int ofMatch = cchSkipFront + 1, ofCur = ofBegin + 1;
                                    ofMatch < cchMatch; ++ofMatch, ++ofCur)
                                {
                                if (!afAny[ofMatch] && achMatch[ofMatch] != ach[ofCur])
                                    {
                                    continue NextChar;
                                    }
                                }
                            }
                        else
                            {
                            for (int ofMatch = cchSkipFront + 1, ofCur = ofBegin + 1;
                                    ofMatch < cchMatch; ++ofMatch, ++ofCur)
                                {
                                if (achMatch[ofMatch] != ach[ofCur])
                                    {
                                    continue NextChar;
                                    }
                                }
                            }

                        // found it; adjust for the leading wilds that we skipped matching
                        return ofBegin - cchSkipFront;
                        }
                    }
                }

            return -1;
            }

        // ----- data members -------------------------------------------

        /**
        * The match pattern, as a String.
        */
        private String m_sMatch;

        /**
        * The match pattern, as an array of char values. If the filter is
        * case insensitive, then this is the uppercase form of the char
        * values.
        */
        private char[] m_achMatch;

        /**
        * The match pattern for a case insensitive like filter, as an array
        * of lowercase char values. For case sensitive filters, this is null.
        */
        private char[] m_achLower;

        /**
        * For each character, true if the character is a wildcard ('_'), or
        * null if there are no wildcards.
        */
        private boolean[] m_afAny;

        /**
        * Number of leading wildcards.
        */
        private int m_cchSkipFront;

        /**
        * Number of trailing wildcards.
        */
        private int m_cchSkipBack;

        /**
        * True if there are any wildcards in the middle.
        */
        private boolean m_fMiddleWilds;

        /**
         * True if the case should be ignored.
         */
        private boolean m_fIgnoreCase;
        }


    // ----- constants ------------------------------------------------------

    /**
    * Non-optimized plan with support for trailing data.
    */
    private static final int ITERATIVE_EVAL       = 0;
    /**
    * Optimized plan: The pattern is anything that starts with a specific
    * character ("x%").
    */
    private static final int STARTS_WITH_CHAR     = 1;
    /**
    * Optimized plan: The pattern is anything that starts with a specific
    * string ("xyz%").
    */
    private static final int STARTS_WITH_STRING   = 2;
    /**
    * Optimized plan: The pattern is anything that starts with a specific
    * (but case-insensitive) string ("xyz%").
    */
    private static final int STARTS_WITH_INSENS   = 3;
    /**
    * Optimized plan: The pattern is anything that ends with a specific
    * character ("%x").
    */
    private static final int ENDS_WITH_CHAR       = 4;
    /**
    * Optimized plan: The pattern is anything that ends with a specific
    * string ("%xyz").
    */
    private static final int ENDS_WITH_STRING     = 5;
    /**
    * Optimized plan: The pattern is anything that ends with a specific (but
    * case-insensitive) string ("%xyz").
    */
    private static final int ENDS_WITH_INSENS     = 6;
    /**
    * Optimized plan: The pattern is anything that contains a specific
    * character ("%x%").
    */
    private static final int CONTAINS_CHAR        = 7;
    /**
    * Optimized plan: The pattern is anything that contains a specific string
    * ("%xyz%").
    */
    private static final int CONTAINS_STRING      = 8;
    /**
    * Optimized plan: Everyting matches ("%").
    */
    private static final int ALWAYS_TRUE          = 9;
    /**
    * Optimized plan: Nothing matches (null).
    */
    private static final int ALWAYS_FALSE         = 10;
    /**
    * Optimized plan: Exact match ("xyz").
    */
    private static final int EXACT_MATCH          = 11;
    /**
    * Optimized plan: Exact case-insensitive match ("xyz").
    */
    private static final int INSENS_MATCH         = 12;

    private static final String[] PLAN_NAMES = new String[]
        {
        "iterative evaluation",
        "starts-with-character",
        "starts-with-string",
        "starts-with-string (case-insensitive)",
        "ends-with-character",
        "ends-with-string",
        "ends-with-string (case-insensitive)",
        "contains-character",
        "contains-string",
        "always-true",
        "always-false",
        "exact-match",
        "exact-match (case-insensitive)",
        };

    /**
    * A special object that represents a "match any" ('%') portion of a
    * pattern while building a processing plan.
    */
    private static final Object ANY = new Object();


    // ----- data members ---------------------------------------------------

    /**
    * The escape character for escaping '_' and '%' in the pattern. The value
    * zero is reserved to mean "no escape".
    */
    @JsonbProperty("escapeChar")
    private char    m_chEscape;

    /**
    * The option to ignore case sensitivity. True means that the filter will
    * match using the same logic that is used by the
    * {@link String#equalsIgnoreCase} method.
    */
    @JsonbProperty("ignoreCase")
    private boolean m_fIgnoreCase;

    /**
    * Optimization plan number. Zero means default iterative evalution is
    * necessary.
    */
    private transient int     m_nPlan;

    /**
    * Used by single-character matching optimization plans.
    */
    private transient char    m_chPart;

    /**
    * Used by string-character matching optimization plans.
    */
    private transient String  m_sPart;

    /**
    * The "front" matching step used by the iterative processing; null if
    * the pattern starts with '%'.
    */
    private transient MatchStep m_stepFront;

    /**
    * The "back" matching step used by the iterative processing; null if
    * the pattern ends with '%'.
    */
    private transient MatchStep m_stepBack;

    /**
    * For iterative plans with a null "back" matching step, is trailing
    * data permitted.
    */
    private transient boolean m_fTrailingTextAllowed;

    /**
    * The array of "middle" matching steps used by the iterative processing;
    * may be null if none.
    */
    private transient MatchStep[] m_astepMiddle;
    }
