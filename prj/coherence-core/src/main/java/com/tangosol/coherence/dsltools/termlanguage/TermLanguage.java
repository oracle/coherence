/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.dsltools.termlanguage;


import com.tangosol.coherence.dsltools.base.LiteralBaseToken;

import com.tangosol.coherence.dsltools.precedence.ParenOPToken;
import com.tangosol.coherence.dsltools.precedence.PathOPToken;
import com.tangosol.coherence.dsltools.precedence.PunctuationOPToken;
import com.tangosol.coherence.dsltools.precedence.ListOpToken;
import com.tangosol.coherence.dsltools.precedence.LiteralOPToken;
import com.tangosol.coherence.dsltools.precedence.TokenTable;


/**
* TermLanguage is a simple language for building Terms.  It is a superset
* of JSON.  A Term can be a, Atom which is a literal which can be a
* Number, String, or Symbol or the constants true, false, or null. This
* results in an AtomicTerm. A term can also be a "functor" applied to a list
* of arguments that are themselves Terms. This results in a NodeTerm.
* Nested list Terms are syntaticaly supported by enclosing a list of Terms
* between "[" and "]". This list is translated to a NodeTerm with a functor
* of ".list.". The characters "{" and"}" create NodeTerms that are similar
* to lists but imply no ordering hence we translate to a NodeTerm with a
* functor of ".bag.". If curlies are used with a functor then we create a
* special NodeTerm whose distunguised single element is the bag of elements.
*  This list is translated to a NodeTerm with a functor of ".list.". The ":"
* character is used as a shorthand for denoting attributes where a:b is
* translated to the NodeTerm ".attr.(a(b)).Finally, ";" is used to build
* sequences. A run of Terms separated by ";" results in a special NodeTerm
* with functor ".sequence."
*
* Much of our inspiration came from the E-Language's
* @see <a href="http://www.erights.org/data/terml/terml-spec.html">TermL</a>
* and
* @see <a href="http://www.wolfram.com">Mathematica</a>
* 
* examples:
*
* "a" -&gt; AtomicTerm("a", String)
*  2 -&gt; AtomicTerm("2",Integer)
*  a -&gt; AtomicTerm("a",Symbol)
*  f(x) -&gt; NodeTerm("f", AtomicTerm("a", Symbol))
*
* the rest of the example translations will use literal Term Language.
* [1,2,3] -&gt; .list.(1,2,3)
* {1,3,3} -&gt; .bag.(1,2,3)
* foo(a:"b" z:[1,2,[3]]) -&gt; foo(.attr.(a(b), .attr.(.list.(1,2,.list.(3)))
* obj{a:1, b:2} -&gt; obj(.bag.(.attr.(a(1)), .attr.(b(2))
* if( f(a), a, else: do(a);do(b);compute(c)) -&gt;
*     if(f(a), a, .attr.(else(.sequence.(do(a),do(b),compute(c))))
*
* @author djl  2009.08.31
*/
public class TermLanguage
    {
    /**
    * Return an initialized TokenTable for the Term Language.
    *
    * @return a TokenTable for the Term Language
    */
     public static synchronized TokenTable tokenTable()
        {
        if (s_tokens == null)
            {
            s_tokens = new TokenTable();
            s_tokens.addToken("true",
                new LiteralOPToken(LiteralBaseToken.createBoolean("true")));
            s_tokens.addToken("false",
                new LiteralOPToken(LiteralBaseToken.createBoolean("false")));
            s_tokens.addToken("null",
                new LiteralOPToken(LiteralBaseToken.createNull("null")));
            s_tokens.addToken(new ParenOPToken("(", 80, null, ".list."));
            s_tokens.addToken(new ListOpToken("[", 80,".list."));
            s_tokens.addToken(new CurlyToken("{", 80));
            s_tokens.addToken(new ColonToken(":", 80));
            s_tokens.addToken(new PathOPToken(";", 80, ".sequence."));
            s_tokens.addToken(new PunctuationOPToken(","));
            }
        return s_tokens;
        }


    // ----- static data members --------------------------------------------

    /**
    * The TokenTable.
    */
    private static TokenTable s_tokens;
    }
