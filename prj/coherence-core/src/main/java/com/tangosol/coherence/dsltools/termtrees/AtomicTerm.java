/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dsltools.termtrees;


/**
* AtomicTerms is the  class used to represent literal Terms such as String
* and Numbers. The functor() method for AtomicTerms return a type name for
* the stored literals.
*
* @author djl  2009.08.31
*/
public class AtomicTerm
        extends Term
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a new AtomicTerm with the given parameters.
    *
    * @param sValue  the String representation of the literal
    * @param nType   the type code for the given literal
    */
     public AtomicTerm(String sValue, int nType)
        {
        m_sValue = sValue;
        m_nTypeCode = nType;
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Obtain the typecode for the node.
    *
    * @return the typecode
    */
    public int getTypeCode()
        {
        return m_nTypeCode;
        }

    /**
    * Obtain the string value for the node.
    *
    * @return the string value
    */
    public String getValue()
        {
        return m_sValue;
        }

    // ----- Term API -------------------------------------------------------

    /**
    * {@inheritDoc}
    */
    public String getFunctor()
        {
        switch (m_nTypeCode)
            {
            case STRINGLITERAL:  return "String";
            case SHORTLITERAL:   return "Short";
            case INTEGERLITERAL: return "Integer";
            case FLOATLITERAL:   return "Float";
            case LONGLITERAL:    return "Long";
            case DOUBLELITERAL:  return "Double";
            case BOOLEANLITERAL: return "Boolean";
            case NULLLITERAL:    return "Null";
            case SYMBOLLITERAL:  return "Symbol";
            default: return null;
            }
        }

    /**
    * {@inheritDoc}
    */
     public String fullFormString()
        {
        return toString();
        }

    /**
    * {@inheritDoc}
    */
    public boolean isNumber()
        {
        return m_nTypeCode >= INTEGERLITERAL && m_nTypeCode <= DOUBLELITERAL;
        }

    /**
    * {@inheritDoc}
    */
    public Term withChild(Term t)
        {
        return new NodeTerm(".list.",this,t);
       }

    /**
    * {@inheritDoc}
    */
    public  Term[] children()
        {
        return new Term[0];
        }

    /**
    * {@inheritDoc}
    */
    public Term termAt(int index)
        {
        if (index == 0)
            {
            return new AtomicTerm(getFunctor(), STRINGLITERAL);
            }
        return null;
        }

    /**
    * {@inheritDoc}
    */
    public Term findChild(String fn)
        {
        return null;
        }

    /**
    * {@inheritDoc}
    */
    public boolean termEqual(Term t)
        {
        if (t == null)
            {
            return false;
            }
        if (t.isAtom())
            {
            AtomicTerm ta = (AtomicTerm) t;
            if (m_nTypeCode == ta.m_nTypeCode)
                {
                if (m_sValue == ta.m_sValue)
                    {
                    return true;
                    }
                else
                    {
                    if (m_sValue == null  || ta.m_sValue == null)
                        {
                        return false;
                        }
                    else
                        {
                        return m_sValue.equals(ta.m_sValue);
                        }
                    }
                }
             }
        return false;
        }


    // ----- AtomicTerm API -------------------------------------------------

    /**
    * Obtain the Object representation of the node.  This will be one of
    * the java Types String, Integer, Long, Float, Double, or Boolean.
    *
    * @return the Object
    */
    public Object getObject()
        {
        String s = m_sValue;
        switch (m_nTypeCode)
            {
            case STRINGLITERAL:  return s;
            case SHORTLITERAL:   return Short.valueOf(s);
            case INTEGERLITERAL: return Integer.valueOf(s);
            case FLOATLITERAL:   return Float.valueOf(s);
            case LONGLITERAL:    return Long.valueOf(s);
            case DOUBLELITERAL:
                if ("nan".equalsIgnoreCase(s))
                    {
                    return Double.NaN;
                    }
                else if ("infinity".equalsIgnoreCase(s))
                    {
                    return Double.POSITIVE_INFINITY;
                    }
                return Double.valueOf(s);
            case BOOLEANLITERAL: return Boolean.valueOf(m_sValue);
            case NULLLITERAL:    return null;
            case SYMBOLLITERAL:  return s;
            default: return null;
            }
    }

    /**
    * Obtain the Number representation of the node.
    *
    * @return the Comparable
    */
    public Number getNumber()
         {
         String s = m_sValue;
         switch (m_nTypeCode)
             {
             case SHORTLITERAL:   return Short.valueOf(s);
             case INTEGERLITERAL: return Integer.valueOf(s);
             case FLOATLITERAL:   return Float.valueOf(s);
             case LONGLITERAL:    return Long.valueOf(s);
             case DOUBLELITERAL:  return Double.valueOf(s);
             default: return null;
             }
     }

    /**
    * Make negative the given number that supposedly came from this node.
    *
    * @param num a Number that was created by this node
    *
    * @return a negated
    */
    public Number negativeNumber(Number num)
        {
        switch (m_nTypeCode)
            {
            case SHORTLITERAL:   return Short.valueOf((short) -num.shortValue());
            case INTEGERLITERAL: return Integer.valueOf(0 - num.intValue());
            case FLOATLITERAL:   return Float.valueOf((float) (0.0 - num.floatValue()));
            case LONGLITERAL:    return Long.valueOf(0l - num.longValue());
            case DOUBLELITERAL:  return Double.valueOf(0.0d - num.doubleValue());
            default: return null;
            }
        }

    /**
    * Make negavite the representation of this node.
    *
    */
    public void negate()
        {
        if (m_sValue.startsWith("-"))
            {
            m_sValue = m_sValue.substring(1);
            }
        else
            {
            m_sValue = "-" + m_sValue;
            }
        }

    /**
     * Test whether the value is of a valid number format.
     *
     * @return the results of testing for numeric format validity
     */
     public boolean isValidNumber()
          {
          String s = m_sValue;
          try
              {
              switch (m_nTypeCode)
                  {
                  case SHORTLITERAL:
                      Short.valueOf(s);
                      break;

                  case INTEGERLITERAL:
                      Integer.valueOf(s);
                      break;

                  case FLOATLITERAL:
                      Float.valueOf(s);
                      break;

                  case LONGLITERAL:
                      Long.valueOf(s);
                      break;

                  case DOUBLELITERAL:
                      Double.valueOf(s);
                      break;

                  default: return false;
                  }
              }
          catch (NumberFormatException ex)
              {
              return false;
              }
      return true;
      }


    // ----- TermWalker methods ---------------------------------------------

    public void accept(TermWalker walker)
        {
        walker.acceptAtom(getFunctor(), this);
        }


    // ----- Object methods -------------------------------------------------

    /**
    * Return a human-readable description for this Node.
    *
    * @return a String description of the Node
    */
     public String toString()
        {
        int nt = m_nTypeCode;
        StringBuffer str = new StringBuffer();
        if (nt == STRINGLITERAL)
            {
            str.append('"');
            }
        str.append(getValue());
        if (nt == FLOATLITERAL)
            {
            str.append('f');
            }
        else if (nt == LONGLITERAL)
            {
            str.append('l');
            }
        else if (nt == SHORTLITERAL)
            {
             str.append('s');
            }
        else if (nt == STRINGLITERAL)
                {
                str.append('"');
                }
        return str.toString();
       }


    // ----- static literal creation healper's  -----------------------------

     /**
     * Create new AtomicTerm representing a String with given value
     *
     * @param value  the text of the literal
     *
     * @return a AtomicTerm for a String
     */
     public static AtomicTerm createString(String value)
         {
         return new AtomicTerm(value,STRINGLITERAL);
         }

     /**
     * Create new AtomicTerm representing a Short with given value
     *
     * @param value  the text of the literal
     *
     * @return a AtomicTerm for a Integer
     */
     public static AtomicTerm createShort(String value)
         {
         return new AtomicTerm(value, SHORTLITERAL);
         }

     /**
     * Create new AtomicTerm representing a Integer with given value
     *
     * @param value  the text of the literal
     *
     * @return a AtomicTerm for a Integer
     */
     public static AtomicTerm createInteger(String value)
         {
         return new AtomicTerm(value, INTEGERLITERAL);
         }

     /**
     * Create new AtomicTerm representing a Long with given value
     *
     * @param value  the text of the literal
     *
     * @return a AtomicTerm for a Long
     */
     public static AtomicTerm createLong(String value)
         {
         return new AtomicTerm(value, LONGLITERAL);
         }

     /**
     * Create new AtomicTerm representing a float with given value
     *
     * @param value  the text of the literal
     *
     * @return a AtomicTerm for a Float
     */
     public static AtomicTerm createFloat(String value)
         {
         return new AtomicTerm(value, FLOATLITERAL);
         }

     /**
     * Create new AtomicTerm representing a Double with given value
     *
     * @param value  the text of the literal
     *
     * @return a AtomicTerm for a Double
     */
     public static AtomicTerm createDouble(String value)
         {
         return new AtomicTerm(value, DOUBLELITERAL);
         }

     /**
     * Create new AtomicTerm representing a Boolean with given value
     *
     * @param value  the text of the literal
     *
     * @return a AtomicTerm for a boolean
     */
     public static AtomicTerm createBoolean(String value)
         {
         return new AtomicTerm(value, BOOLEANLITERAL);
         }

     /**
     * Create new AtomicTerm representing a null with given value
     *
     * @param value  the text of the literal
     *
     * @return a AtomicTerm for a null
     */
     public static AtomicTerm createNull(String value)
         {
         return new AtomicTerm(value, NULLLITERAL);
         }
    /**
    * Create new AtomicTerm representing a null.
    *
    * @return a AtomicTerm for a null
    */
    public static AtomicTerm createNull()
        {
        return new AtomicTerm("null", NULLLITERAL);
        }

    /**
    * Create new AtomicTerm representing a Symbol with given value
    *
    * @param value  the text of the literal
    *
    * @return a AtomicTerm for a Symbol
    */
    public static AtomicTerm createSymbol(String value)
        {
        return new AtomicTerm(value, SYMBOLLITERAL);
        }
    // ----- constants ------------------------------------------------------

    /**
    * The numberic code for a string literal
    */
    public static final int STRINGLITERAL = 1;

    /**
    * The numberic code for a integer literal
    */
    public static final int INTEGERLITERAL = 2;

    /**
    * The numberic code for a long literal
    */
    public static final int LONGLITERAL = 3;

    /**
    * The numberic code for a float literal
    */
    public static final int FLOATLITERAL = 4;

    /**
    * The numberic code for a double literal
    */
    public static final int DOUBLELITERAL = 5;

    /**
    * The numberic code for a boolean literal
    */
    public static final int BOOLEANLITERAL = 6;

    /**
    * The numberic code for a boolean literal
    */
    public static final int NULLLITERAL = 7;

    /**
    * The numberic code for a symbol literal
    */
    public static final int SHORTLITERAL = 8;

    /**
    * The numberic code for a symbol literal
    */
    public static final int SYMBOLLITERAL = 9;

    // ----- data members ---------------------------------------------------

    /**
    * The string value of this node.
    */
    String m_sValue;

    /**
    * The type code for this node.
    */
    int m_nTypeCode;
    }
