/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dsltools.precedence;

import com.tangosol.coherence.dsltools.termtrees.Term;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A PeekOPToken is a token that contains other {@link OPToken}
 * instances. It will defer calls to the {@link #nud(OPParser)} and
 * {@link #led(OPParser, Term)} methods to these other tokens. The exact
 * instance of the OPToken deferred to will be determined by
 * looking ahead at the next token in the token stream and calling the
 * contained OPToken with that id.
 *
 * @author jk 2014.02.12
 * @since Coherence 12.2.1
 */
public class PeekOPToken
        extends OPToken
    {
    // -----  constructors --------------------------------------------------

    /**
     * Construct a new PeekOPToken with the given parameters.
     *
     * @param sId    string representation of the token
     * @param tokens the tokens to defer to
     */
    public PeekOPToken(String sId, OPToken... tokens)
        {
        this(sId, -1, null, null, tokens);
        }

    /**
     * Construct a new PeekOPToken with the given parameters.
     *
     * @param sId           string representation of the token
     * @param nBindingPower the precedence binding power of this token
     * @param tokens        the tokens to defer to
     */
    public PeekOPToken(String sId, int nBindingPower, OPToken... tokens)
        {
        this(sId, nBindingPower, null, null, tokens);
        }

    /**
     * Construct a new PeekOPToken with the given parameters.
     *
     * @param sId      string representation of the token
     * @param sASTName the type code for this token
     * @param tokens   the tokens to defer to
     */
    public PeekOPToken(String sId, String sASTName, OPToken... tokens)
        {
        this(sId, -1, sASTName, null, tokens);
        }

    /**
     * Construct a new PeekOPToken with the given parameters.
     *
     * @param sId           string representation of the token
     * @param nBindingPower the precedence binding power of this token
     * @param sASTName      the type code for this token
     * @param tokens        the tokens to defer to
     */
    public PeekOPToken(String sId, int nBindingPower, String sASTName, OPToken... tokens)
        {
        this(sId, nBindingPower, sASTName, null, tokens);
        }

    /**
     * Construct a new PeekOPToken with the given parameters.
     *
     * @param sId           string representation of the token
     * @param nBindingPower the precedence binding power of this token
     * @param sLedASTName   the name for this tokens AST
     * @param sNudASTName   the name for this tokens AST
     * @param aTokens       the tokens to defer to
     */
    public PeekOPToken(String sId, int nBindingPower, String sLedASTName, String sNudASTName, OPToken...aTokens)
        {
        super(sId, nBindingPower, sLedASTName, sNudASTName);
        Map mapTokens = f_mapTokens = new LinkedHashMap<>();

        for (OPToken token : aTokens)
            {
            mapTokens.put(token.getId(), token);
            }
        }

    // ----- PeekOPToken API ------------------------------------------------

    /**
     * Add the specified {@link OPToken} to the {@link Map} of
     * tokens that will be called depending on the next token parsed
     * from the token stream.
     *
     * @param token  the {@link OPToken} to call if the next token in the
     *               stream matches the specified {@link OPToken}s identifier
     */
    public void addOPToken(OPToken token)
        {
        addOPToken(token.getId(), token);
        }

    /**
     * Add the specified {@link OPToken} to the {@link Map} of
     * tokens that will be called depending on the next token parsed
     * from the token stream.
     *
     * @param id     the identifier to match with the next token in the
     *               stream
     * @param token  the {@link OPToken} to call if the next token in the
     *               stream matches the specified identifier
     */
    public void addOPToken(String id, OPToken token)
        {
        f_mapTokens.put(id, token);
        }

    /**
     * Obtain the {@link OPToken} that is mapped to
     * the specified identifier.
     *
     * @param sId  the identifier to obtain the mapped {@link OPToken} for
     *
     * @return the {@link OPToken} mapped to the specified identifier
     */
    public OPToken getOPToken(String sId)
        {
        return f_mapTokens.get(sId);
        }

    /**
     * The default nud method that will be called if there is no
     * {@link OPToken} mapped for the token parsed from the token stream.
     * <p>
     * This method may be overridden in sub-classes that require different
     * default processing to the default {@link OPToken#nud(OPParser)} method.
     *
     * @param parser  the current token stream's parser
     *
     * @return the result of calling the default nud method
     */
    protected Term defaultNud(OPParser parser)
        {
        return super.nud(parser);
        }

    /**
     * The default led method that will be called if there is no
     * {@link OPToken} mapped for the token parsed from the token
     * stream.
     *
     * This method may be overridden in sub-classes that require different
     * default processing to the default {@link OPToken#led(OPParser, Term)}
     * method.
     *
     * @param parser    the current token stream's parser
     * @param leftNode  the left node of the current AST
     *
     * @return the result of calling the default led method
     */
    protected Term defaultLed(OPParser parser, Term leftNode)
        {
        return super.led(parser, leftNode);
        }

    // ----- Operator Precedence API ----------------------------------------

    /**
     * {@inheritDoc}
     */
    public Term nud(OPParser parser)
        {
        OPToken token = findMatchingOPToken(parser);

        return token != null
               ? token.nud(parser)
               : defaultNud(parser);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public Term led(OPParser parser, Term leftNode)
        {
        OPToken token = findMatchingOPToken(parser);

        return token != null
               ? token.led(parser, leftNode)
               : defaultLed(parser, leftNode);
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Return the {@link OPToken} mapped to the next token in the
     * {@link OPParser}'s token stream.
     *
     * @param parser  the OPParser providing the token stream
     *
     * @return the OPToken matching the next token in the stream or
     *         null if there is no {@link OPToken} mapped to the next
     *         token in the stream.
     */
    protected OPToken findMatchingOPToken(OPParser parser)
        {
        for (Map.Entry<String,OPToken> entry : f_mapTokens.entrySet())
            {
            OPScanner scanner = parser.getScanner();

            if (scanner.advanceWhenMatching(entry.getKey()))
                {
                return entry.getValue();
                }
            }

        return null;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The {@link Map} of {@link OPToken} instances that will be called
     * depending on the value of the next token in the stream.
     */
    protected final Map<String, OPToken> f_mapTokens;
    }
