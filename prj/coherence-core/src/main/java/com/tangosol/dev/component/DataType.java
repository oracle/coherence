/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.dev.component;


import java.io.Serializable;

import java.util.Map;
import java.util.Hashtable;
import java.util.Vector;

import com.tangosol.dev.assembler.ClassFile;
import com.tangosol.dev.assembler.ClassConstant;

import com.tangosol.java.type.Type;
import com.tangosol.java.type.ArrayType;
import com.tangosol.java.type.ClassType;
import com.tangosol.java.type.PrimitiveType;
import com.tangosol.java.type.ReferenceType;

import com.tangosol.util.Base;
import com.tangosol.util.ClassHelper;


/**
* A DataType is an immutable object that describes any intrinsic, class,
* interface, or component type.  The manner of storing a data type is based
* on the JVM specification, with additional support for components:
*
*   <field_type>     ::= <base_type>|<object_type>|<component_type>|<array_type>
*   <base_type>      ::= B|C|D|F|I|J|S|Z
*   <object_type>    ::= L<fullclassname>;
*   <component_type> ::= R<fullcomponentname>;
*   <array_type>     ::= [<optional_size><field_type>
*
* The following option is not supported:
*
*   <optional_size>  ::= [0-9]*
*
* The literal characters are interpreted as follows:
*
* Literal                Type     Description
* ---------------------  -------  -------------------------------------------
* V                      void     no return type (for return types only)
* Z                      boolean  true or false
* B                      byte     signed byte
* C                      char     character
* S                      short    signed short
* I                      int      signed integer
* J                      long     signed long integer
* F                      float    single-precision IEEE float
* D                      double   double-precision IEEE float
* L<fullclassname>;               an object of the given class
* [<field_type>                   array
*
* R<fullcomponentname>;           an object of the given component type
* N                               null (used by compiler)
* U                               unknown (used by compiler)
*
* Instances of DataType are unique, and therefore can be compared using the
* Java == operator.
*
* @version 1.00, 11/13/97
* @author  Cameron Purdy
*/
public class DataType
        extends Base
        implements Constants
    {
    // ----- construction ---------------------------------------------------

    /**
    * Construct a data type object based on the passed string.
    *
    * @param type  the Type object for this DataType
    */
    private DataType(Type type)
        {
        m_type = type;
        }


    // ----- factory methods ------------------------------------------------

    /**
    * Return the data type for the specified class.
    *
    * @param clz  the Java class object, which may represent an array or
    *             intrinsic type
    *
    * @return the data type for the class
    */
    public static DataType getClassType(Class clz)
        {
        azzert(clz != null);

        if (clz.isArray())
            {
            return getClassType(clz.getComponentType()).getArrayType();
            }

        if (clz.isPrimitive())
            {
            if (clz == java.lang.Boolean.TYPE)
                {
                return BOOLEAN;
                }
            else if (clz == java.lang.Character.TYPE)
                {
                return CHAR;
                }
            else if (clz == java.lang.Byte.TYPE)
                {
                return BYTE;
                }
            else if (clz == java.lang.Short.TYPE)
                {
                return SHORT;
                }
            else if (clz == java.lang.Integer.TYPE)
                {
                return INT;
                }
            else if (clz == java.lang.Long.TYPE)
                {
                return LONG;
                }
            else if (clz == java.lang.Float.TYPE)
                {
                return FLOAT;
                }
            else if (clz == java.lang.Double.TYPE)
                {
                return DOUBLE;
                }
            else if (clz == java.lang.Void.TYPE)
                {
                return VOID;
                }
            else
                {
                throw new IllegalArgumentException(CLASS + ".getClassType:  " +
                    "Primitive type (" + clz.getName() + ") is not known!");
                }
            }

        return getClassType(clz.getName());
        }

    /**
    * Return the data type for the specified class.
    *
    * @param sName the name of the class
    *
    * @return the data type for the class
    */
    public static DataType getClassType(String sName)
        {
        azzert(sName != null);
        azzert(ClassHelper.isQualifiedNameLegal(sName), "Illegal class name: \"" + sName + '\"');
        return getType('L' + sName + ';');
        }

    /**
    * Return the data type for the specified component.
    *
    * @param sName the name of the component
    *
    * @return the data type for the component
    */
    public static DataType getComponentType(String sName)
        {
        azzert(sName != null);
        azzert(Component.isQualifiedNameLegal(sName), "Illegal component name: \"" + sName + '\"');
        return getType('R' + sName + ';');
        }

    /**
    * Return the data type for the array containing the specified data type.
    *
    * @param dtElement the data type of the element
    *
    * @return the array data type whose elements are of the specified data
    *         type
    */
    public static DataType getArrayType(DataType dtElement)
        {
        azzert(dtElement != null);
        return dtElement.getArrayType();
        }

    /**
    * Determine the DataType for a JVM signature.
    *
    * @param sType the JVM specification for the data type
    *
    * @return the DataType instance corresponding to the JVM type signature
    */
    public static DataType getJVMType(String sType)
        {
        switch (sType.length())
            {
            case 0:
                throw new IllegalArgumentException(CLASS + ".getJVMType:  " +
                    "JVM Type Signature required!");

            case 1:
                switch (sType.charAt(0))
                    {
                    case 'V':
                        return VOID;
                    case 'Z':
                        return BOOLEAN;
                    case 'B':
                        return BYTE;
                    case 'C':
                        return CHAR;
                    case 'S':
                        return SHORT;
                    case 'I':
                        return INT;
                    case 'J':
                        return LONG;
                    case 'F':
                        return FLOAT;
                    case 'D':
                        return DOUBLE;
                    default:
                        throw new IllegalArgumentException(CLASS + ".getJVMType:  " +
                            "Illegal JVM Type Signature:  " + sType);
                    }

            default:
                switch (sType.charAt(0))
                    {
                    case '[':
                        return getJVMType(sType.substring(1)).getArrayType();

                    case 'L':
                        // get the Java class name
                        sType = sType.substring(1, sType.length() - 1);
                        if (sType.startsWith(ClassFile.Relocator.PACKAGE))
                            {
                            // it is a component or a synthetic class
                            String sName = getComponentName(sType);
                            if (Component.isQualifiedNameLegal(sName))
                                {
                                return getComponentType(sName);
                                }
                            }

                        // it is a class (external or synthetic)
                        return getClassType(sType.replace('/', '.'));

                    case 'R':
                        sType = sType.substring(1, sType.length() - 1);
                        return getComponentType(getComponentName(sType));

                    default:
                        throw new IllegalArgumentException(CLASS + ".getJVMType:  " +
                            "Illegal JVM Type Signature:  " + sType);
                    }
            }
        }

    /**
    * Return the DataType object corresponding to the specified Type object.
    *
    * @param type  the Type object
    *
    * @return the corresponding DataType object
    */
    public static DataType getDataType(Type type)
        {
        DataType dt;

        Map    map   = sm_tblTypes;
        String sType = toTypeString(type);
        synchronized (map)
            {
            dt = (DataType) map.get(sType);

            if (dt == null)
                {
                dt = new DataType(type);
                map.put(sType, dt);
                }
            }

        return dt;
        }

    /**
    * Return the requested data type, creating it if necessary.
    *
    * @param sType the internal specification for the data type
    *
    * @return the requested data type
    */
    public static DataType getType(String sType)
        {
        DataType dt;

        Map map = sm_tblTypes;
        synchronized (map)
            {
            dt = (DataType) map.get(sType);

            if (dt == null)
                {
                // only two differences between internal specification and Type
                // signature:
                //  1)  Type signature does not understand ComponentType
                //  2)  Type signature uses '/' instead of '.' for classes,
                //      but the ClassType constructor takes care of that
                Type type;
                switch (sType.charAt(0))
                    {
                    case '[':
                        {
                        int cDepth = 1;
                        for (int of = 1; sType.charAt(of) == '['; ++of)
                            {
                            ++cDepth;
                            }

                        type = getType(sType.substring(cDepth)).getType();
                        for (int i = 0; i < cDepth; ++i)
                            {
                            type = type.getArrayType();
                            }
                        }
                        break;

                    case 'R':
                        // pass a dot-delimited component name to create a ComponentType
                        type = new ComponentType(sType.substring(1, sType.length() - 1));
                        break;

                    case 'U':
                        type = Type.UNKNOWN;
                        break;

                    case 'N':
                        type = Type.NULL;
                        break;

                    default:
                        type = Type.parseSignature(sType);
                        break;
                    }

                dt = new DataType(type);
                map.put(sType, dt);
                }
            }

        return dt;
        }


    // ----- type categories ------------------------------------------------

    /**
    * Test for a primitive Java data type.
    *
    * @return true if the data type is a primitive Java type
    */
    public boolean isPrimitive()
        {
        return getType() instanceof PrimitiveType;
        }

    /**
    * Test for a primitive numeric Java type.
    *
    * @return true if the type is a Java numeric type
    */
    public boolean isNumeric()
        {
        Type type = getType();
        return type instanceof PrimitiveType && ((PrimitiveType) type).isNumeric();
        }

    /**
    * Test for a primitive integral Java type.
    *
    * @return true if the type is a Java integral type
    */
    public boolean isIntegral()
        {
        Type type = getType();
        return type instanceof PrimitiveType && ((PrimitiveType) type).isIntegral();
        }

    /**
    * Test for a primitive floating point Java type.
    *
    * @return true if the type is either float or double
    */
    public boolean isFloatingPoint()
        {
        Type type = getType();
        return type instanceof PrimitiveType && ((PrimitiveType) type).isFloatingPoint();
        }

    /**
    * Test for a reference Java data type.
    *
    * @return true if the data type is a reference type
    */
    public boolean isReference()
        {
        return getType() instanceof ReferenceType;
        }

    /**
    * Test for a simple (built-in) Java data type or String.
    *
    * @return true if the data type is a simple-plus type
    */
    public boolean isExtendedSimple()
        {
        return this == STRING || isPrimitive();
        }

    /**
    * Test for a Java class data type.
    *
    * @return true if the data type is a Java class type
    */
    public boolean isClass()
        {
        Type type = getType();
        return type instanceof ClassType && !(type instanceof ComponentType);
        }

    /**
    * Test for a Component Definition data type.
    *
    * @return true if the data type is a component type
    */
    public boolean isComponent()
        {
        return getType() instanceof ComponentType;
        }

    /**
    * Test for an array data type.
    *
    * @return true if the data type is an array type
    */
    public boolean isArray()
        {
        return getType() instanceof ArrayType;
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Get the Type object for this DataType.
    *
    * @return the Type object for this DataType object
    */
    public Type getType()
        {
        return m_type;
        }

    /**
    * Provide a representation of the DataType object that can be persisted.
    * The returned value can be reinstantiated using the getType method.
    *
    * @return the internal format of the DataType object
    */
    public String getTypeString()
        {
        return toTypeString(getType());
        }

    /**
    * Return the data type for the array containing this data type.
    *
    * @return the array data type whose elements are of this data type
    */
    public DataType getArrayType()
        {
        azzert(this != NULL && this != VOID);

        // array of unknown is unknown (only specialized tasks such as
        // compilation uses the "unknown" type)
        if (this == UNKNOWN)
            {
            return this;
            }

        return getType('[' + getTypeString());
        }

    /**
    * Get the name of the Java class represented by the data type.
    *
    * @return qualified Java class name
    */
    public String getClassName()
        {
        Type type = getType();
        azzert(type instanceof ClassType, "Invalid type: " + type);
        return ((ClassType) type).getClassName();
        }

    /**
    * Get the name of the component represented by the data type.
    *
    * @return the qualified Component Definition name
    */
    public String getComponentName()
        {
        azzert(isComponent());
        return ((ComponentType) getType()).getComponentName();
        }

    /**
    * Get the data type of the elements of an array.
    *
    * @return the element data type
    */
    public DataType getElementType()
        {
        if (this == UNKNOWN)
            {
            return this;
            }

        azzert(isArray());
        return getDataType(((ArrayType) getType()).getElementType());
        }

    /**
    * Get the base data type of the elements of an array.
    *
    * @return the base element data type
    */
    public DataType getBaseElementType()
        {
        DataType dt = getElementType();

        while (dt.isArray())
            {
            dt = dt.getElementType();
            }

        return dt;
        }

    /**
    * Return the type signature built into Java byte code.
    *
    * @return the JVM field signature
    */
    public String getJVMSignature()
        {
        return getType().getSignature();
        }

    /**
    * Get the class constant for the data type.  Sometimes the class constant
    * is the same as the JVM signature and sometimes it is the same as the
    * class name (in internal format).
    *
    * @return the class constant for the data type
    */
    public ClassConstant getClassConstant()
        {
        if (isPrimitive() || isArray())
            {
            return new ClassConstant(getJVMSignature());
            }

        // assume the null type implies java/lang/Object
        if (this == NULL)
            {
            return OBJECT.getClassConstant();
            }

        // must be a class or a component
        return new ClassConstant(getClassName());
        }


    // ----- parsing helpers ------------------------------------------------

    /**
    * Parse the behavior signature into discrete return and parameter data
    * types.
    *
    * @param sSig the behavior signature
    *
    * @return an array of DataType instances, where [0] is the return
    *         type and [1]..[c] are the parameter types.
    */
    public static DataType[] parseSignature(String sSig)
        {
        // find start of parameter signatures
        char[] ach = sSig.toCharArray();
        int    of  = 0;
        while (ach[of++] != '(')
            {
            }

        // reserve the first element for the return value
        Vector vect = new Vector();
        vect.add(null);

        // parse parameter signatures
        while (ach[of] != ')')
            {
            int cch = parseTypeLength(ach, of);
            vect.addElement(getJVMType(new String(ach, of, cch)));
            of += cch;
            }

        // return value starts after the parameter-stop character
        // and runs to the end of the method signature
        ++of;
        String sRet = new String(ach, of, ach.length - of);
        if (sRet.length() > 0)
            {
            vect.set(0, getJVMType(sRet));
            }

        DataType[] adt = new DataType[vect.size()];
        vect.copyInto(adt);

        return adt;
        }

    private static int parseTypeLength(char[] ach, int of)
        {
        switch (ach[of])
            {
            case 'V':
            case 'Z':
            case 'B':
            case 'C':
            case 'S':
            case 'I':
            case 'J':
            case 'F':
            case 'D':
                return 1;

            case '[':
                {
                int cch = 1;
                while (isDecimal(ach[++of]))
                    {
                    ++cch;
                    }
                return cch + parseTypeLength(ach, of);
                }

            case 'L':
            case 'R':
                {
                int cch = 2;
                while (ach[++of] != ';')
                    {
                    ++cch;
                    }
                return cch;
                }

            default:
                throw new IllegalArgumentException(CLASS + ".parseTypeLength:  " +
                    "JVM Type Signature cannot start with '" + ach[of] + '\'');
            }
        }

    /**
    * Provide the internal DataType String representation for the specified
    * Type.
    *
    * @param type  a Type object
    *
    * @return the DataType type string for the specified Type object
    */
    public static String toTypeString(Type type)
        {
        if (type instanceof ArrayType)
            {
            return '[' + toTypeString(((ArrayType) type).getElementType());
            }
        else if (type instanceof ComponentType)
            {
            return 'R' + ((ComponentType) type).getComponentName() + ';';
            }
        else
            {
            return type.getSignature().replace('/', '.');
            }
        }


    // ----- Component Definition helpers -----------------------------------

    /**
    * Return the relocatable class name for the specified component.
    *
    * @param cd  Component Definition
    *
    * @return the relocatable class name for the component ('.'-delimited)
    */
    public static String getComponentClassName(Component cd)
        {
        return ComponentType.getComponentClassName(cd);
        }

    /**
    * Return the relocatable class name for the specified component name.
    *
    * @param sName  qualified Component Definition name
    *
    * @return the relocatable class name for the component ('.'-delimited)
    */
    public static String getComponentClassName(String sName)
        {
        return ComponentType.getComponentClassName(sName);
        }

    /**
    * Return the package name for the specified component.
    *
    * @param cd  Component Definition
    *
    * @return the package name for the component prefixed with
    *         the "relocatable" package name ("."-delimited)
    */
    public static String getComponentPackage(Component cd)
        {
        return ComponentType.getComponentPackage(cd);
        }

    /**
    * Return the package name for the specified component name.
    *
    * According to section 7.1 of the Java Language Specification,
    * Second Edition, it is illegal for a package to contain a class
    * or interface type and a subpackage with the same name.
    * To inforce this rule, all global Component Definitions have names
    * that start with an upper case letter and the corresponding packages
    * just have this first letter lower-cased.
    *
    * @param sName  qualified Component Definition name
    *
    * @return the package name for the component ("."-delimited)
    *
    * @see com.tangosol.dev.component.Component#isSimpleNameLegal
    */
    public static String getComponentPackage(String sName)
        {
        return ComponentType.getComponentPackage(sName);
        }

    /**
    * Parses a component class name and calculates the component name,
    * where the class name may or may not start with the relocation prefix.
    *
    * @param sClassName the component class name (could be '/' or '.' delimited)
    *
    * @return the component name
    *
    * Note: for synthetic component classes the calculated name will not be
    * a valid component name.
    *
    * @see com.tangosol.dev.component.ComponentType#getComponentPackage(Component)
    */
    public static String getComponentName(String sClassName)
        {
        return ComponentType.getComponentName(sClassName);
        }


    // ----- Object methods -------------------------------------------------

    /**
    * Provide a human-readable representation of the DataType object.  For
    * all data types except components, the resulting string is the type as
    * it would appear in Java source code.
    *
    * @return a description of the DataType object
    */
    public String toString()
        {
        return getType().toString();
        }


    // ----- static members -------------------------------------------------

    /**
    * The name of this class.
    */
    private static final String CLASS = "DataType";

    /**
    * Create a hash table to store all data types.
    */
    private static Hashtable sm_tblTypes = new Hashtable();


    // ----- simple data types ----------------------------------------------

    /**
    * Unkown type (for compilers only).
    */
    public static final DataType UNKNOWN = getType("U");

    /**
    * Java null (for compilers only).
    */
    public static final DataType NULL    = getType("N");

    /**
    * Java void (return values only).
    */
    public static final DataType VOID    = getType("V");

    /**
    * Java boolean.
    */
    public static final DataType BOOLEAN = getType("Z");

    /**
    * Java byte.
    */
    public static final DataType BYTE    = getType("B");

    /**
    * Java char.
    */
    public static final DataType CHAR    = getType("C");

    /**
    * Java short.
    */
    public static final DataType SHORT   = getType("S");

    /**
    * Java int.
    */
    public static final DataType INT     = getType("I");

    /**
    * Java long.
    */
    public static final DataType LONG    = getType("J");

    /**
    * Java float.
    */
    public static final DataType FLOAT   = getType("F");

    /**
    * Java double.
    */
    public static final DataType DOUBLE  = getType("D");


    // ----- common data types ----------------------------------------------

    /**
    * Java reference type root.
    */
    public static final DataType OBJECT       = getClassType(Object.class);

    /**
    * Java semi-intrinsic string.
    */
    public static final DataType STRING       = getClassType(String.class);

    /**
    * Byte array.
    */
    public static final DataType BINARY       = getArrayType(BYTE);

    /**
    * Root Component (i.e. all Components).
    */
    public static final DataType COMPLEX      = DataType.getComponentType(Component.getRootName());

    /**
    * The Serializable interface.
    */
    public static final DataType SERIALIZABLE = getClassType(Serializable.class);

    /**
    * The Cloneable interface.
    */
    public static final DataType CLONEABLE    = getClassType(Cloneable.class);

    /**
    * Throwable class.
    */
    public static final DataType THROWABLE    = getClassType(Throwable.class);

    /**
    * Error class.
    */
    public static final DataType ERROR        = getClassType(Error.class);

    /**
    * Exception class.
    */
    public static final DataType EXCEPTION    = getClassType(Exception.class);

    /**
    * Runtime exception class.
    */
    public static final DataType RUNTIME      = getClassType(RuntimeException.class);


    // ----- data members ---------------------------------------------------

    /**
    * The Type object for this DataType.
    */
    private Type m_type;
    }
