/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */


package com.tangosol.dev.assembler;


import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;


/**
* The AccessFlags class represents the unsigned short value containing
* various bit flags referred to as "access_flags" by the JVM specification.
* These flags include accessibility (public, private, and protected), and
* other attributes that apply to classes, fields, and methods, such as
* static, native, and interface.
*
* @version 0.10, 03/03/98, based on prototype dis-assembler
* @version 0.50, 05/07/98, modified for assembler
* @author  Cameron Purdy
*/
public class AccessFlags extends VMStructure implements Constants
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct an AccessFlags object.
    */
    protected AccessFlags()
        {
        }

    /**
    * Construct an AccessFlags object.
    *
    * @param nFlags  the initial flag values
    */
    protected AccessFlags(int nFlags)
        {
        m_nPrevFlags = m_nFlags = nFlags;
        }


    // ----- VMStructure operations -----------------------------------------

    /**
    * The disassembly process reads the structure from the passed input
    * stream and uses the constant pool to dereference any constant
    * references.
    *
    * @param stream  the stream implementing java.io.DataInput from which
    *                to read the assembled VM structure
    * @param pool    the constant pool for the class which contains any
    *                constants referenced by this VM structure
    */
    protected void disassemble(DataInput stream, ConstantPool pool)
            throws IOException
        {
        m_nFlags = stream.readUnsignedShort();
        }

    /**
    * The pre-assembly step collects the necessary entries for the constant
    * pool.  During this step, all constants used by this VM structure and
    * any sub-structures are registered with (but not yet bound by position
    * in) the constant pool.
    *
    * @param pool  the constant pool for the class which needs to be
    *              populated with the constants required to build this
    *              VM structure
    */
    protected void preassemble(ConstantPool pool)
        {
        // no references to constants
        }

    /**
    * The assembly process assembles and writes the structure to the passed
    * output stream, resolving any dependencies using the passed constant
    * pool.
    *
    * @param stream  the stream implementing java.io.DataOutput to which to
    *                write the assembled VM structure
    * @param pool    the constant pool for the class which by this point
    *                contains the entire set of constants required to build
    *                this VM structure
    */
    protected void assemble(DataOutput stream, ConstantPool pool)
            throws IOException
        {
        stream.writeShort(m_nFlags);
        }

    /**
    * Determine if the VM structure (or any contained VM structure) has been
    * modified.
    *
    * @return true if the VM structure has been modified
    */
    public boolean isModified()
        {
        return m_nFlags != m_nPrevFlags;
        }

    /**
    * Reset the modified state of the VM structure.
    */
    protected void resetModified()
        {
        m_nPrevFlags = m_nFlags;
        }


    // ----- Object operations ----------------------------------------------

    /**
    * Compare this object to another object for equality.
    *
    * @param obj  the other object to compare to this
    *
    * @return true if this object equals that object
    */
    public boolean equals(Object obj)
        {
        try
            {
            AccessFlags that = (AccessFlags) obj;
            return this            == that
                || this.getClass() == that.getClass()
                && this.m_nFlags   == that.m_nFlags;
            }
        catch (NullPointerException e)
            {
            // obj is null
            return false;
            }
        catch (ClassCastException e)
            {
            // obj is not of this class
            return false;
            }
        }

    /**
    * Provide a human-readable description of the object.
    *
    * @return a string describing the object
    */
    public String toString()
        {
        return toString(ACC_ALL);
        }

    /**
    * Provide a human-readable description of the object.
    *
    * @param nMask  specifies which ACC_ bit flags to display
    *
    * @return a string describing the object
    */
    public String toString(int nMask)
        {
        StringBuffer sb = new StringBuffer();
        int nAccess = m_nFlags & nMask;
        if ((nAccess & ACC_PUBLIC      ) != 0) {sb.append(" public");      }
        if ((nAccess & ACC_PRIVATE     ) != 0) {sb.append(" private");     }
        if ((nAccess & ACC_PROTECTED   ) != 0) {sb.append(" protected");   }
        if ((nAccess & ACC_STATIC      ) != 0) {sb.append(" static");      }
        if ((nAccess & ACC_FINAL       ) != 0) {sb.append(" final");       }
        if ((nAccess & ACC_SYNCHRONIZED) != 0) {sb.append(" synchronized");}
        if ((nAccess & ACC_VOLATILE    ) != 0) {sb.append(" volatile");    }
        if ((nAccess & ACC_TRANSIENT   ) != 0) {sb.append(" transient");   }
        if ((nAccess & ACC_BRIDGE      ) != 0) {sb.append(" (bridge)");    }
        if ((nAccess & ACC_VARARGS     ) != 0) {sb.append(" (varargs)");   }
        if ((nAccess & ACC_STRICT      ) != 0) {sb.append(" strictfp");    }
        if ((nAccess & ACC_NATIVE      ) != 0) {sb.append(" native");      }
        if ((nAccess & ACC_INTERFACE   ) != 0) {sb.append(" interface");   }
        if ((nAccess & ACC_ABSTRACT    ) != 0) {sb.append(" abstract");    }
        if ((nAccess & ACC_SYNTHETIC   ) != 0) {sb.append(" (synthetic)"); }
        if ((nAccess & ACC_ANNOTATION  ) != 0) {sb.append(" (annotation)");}
        if ((nAccess & ACC_ENUM        ) != 0) {sb.append(" (enum)");      }
        return (sb.length() == 0 ? "" : sb.toString().substring(1));
        }


    // ----- accessors ------------------------------------------------------

    // ----- access_flags

    /**
    * Get the value of the access_flags structure.
    *
    * @return the access_flags bit values
    */
    public int getFlags()
        {
        return m_nFlags;
        }

    /**
    * Set the value of the access_flags structure.
    *
    * @param nFlags  the access_flags bit values
    */
    public void setFlags(int nFlags)
        {
        m_nFlags = nFlags & ACC_ALL;
        }


    // ----- interface

    /**
    * Determine if the interface attribute is set.
    *
    * @return true if interface
    */
    public boolean isInterface()
        {
        return (m_nFlags & ACC_INTERFACE) != 0;
        }

    /**
    * Set the interface attribute.
    *
    * @param fInterface  true to set to interface, false to set to class
    */
    public void setInterface(boolean fInterface)
        {
        if (fInterface)
            {
            m_nFlags |= ACC_INTERFACE;
            }
        else
            {
            m_nFlags &= ~ACC_INTERFACE;
            }
        }


    // ----- access

    /**
    * Get the class/method/field accessibility value.
    *
    * @return one of ACC_PUBLIC, ACC_PROTECTED, ACC_PRIVATE, or ACC_PACKAGE
    */
    public int getAccess()
        {
        return m_nFlags & ACC_ACCESS_MASK;
        }

    /**
    * Set the class/method/field accessibility value.
    *
    * @param nAccess  should be one of ACC_PUBLIC, ACC_PROTECTED,
    *                 ACC_PRIVATE, or ACC_PACKAGE
    */
    public void setAccess(int nAccess)
        {
        m_nFlags = m_nFlags & ~ACC_ACCESS_MASK | nAccess & ACC_ACCESS_MASK;
        }

    /**
    * Determine if the accessibility is public.
    *
    * @return true if the accessibility is public
    */
    public boolean isPublic()
        {
        return (m_nFlags & ACC_PUBLIC) != 0;
        }

    /**
    * Set the accessibility to public.
    */
    public void setPublic()
        {
        setAccess(ACC_PUBLIC);
        }

    /**
    * Determine if the accessibility is protected.
    *
    * @return true if the accessibility is protected
    */
    public boolean isProtected()
        {
        return (m_nFlags & ACC_PROTECTED) != 0;
        }

    /**
    * Set the accessibility to protected.
    */
    public void setProtected()
        {
        setAccess(ACC_PROTECTED);
        }

    /**
    * Determine if the accessibility is package private.
    *
    * @return true if the accessibility is package private
    */
    public boolean isPackage()
        {
        return (m_nFlags & ACC_ACCESS_MASK) == 0;
        }

    /**
    * Set the accessibility to package private.
    */
    public void setPackage()
        {
        setAccess(ACC_PACKAGE);
        }

    /**
    * Determine if the accessibility is private.
    *
    * @return true if the accessibility is private
    */
    public boolean isPrivate()
        {
        return (m_nFlags & ACC_PRIVATE) != 0;
        }

    /**
    * Set the accessibility to private.
    */
    public void setPrivate()
        {
        setAccess(ACC_PRIVATE);
        }


    // ----- abstract

    /**
    * Determine if the abstract attribute is set.
    *
    * @return true if abstract
    */
    public boolean isAbstract()
        {
        return (m_nFlags & ACC_ABSTRACT) != 0;
        }

    /**
    * Set the abstract attribute.
    *
    * @param fAbstract  true to set to abstract, false to set to concrete
    */
    public void setAbstract(boolean fAbstract)
        {
        if (fAbstract)
            {
            m_nFlags |= ACC_ABSTRACT;
            }
        else
            {
            m_nFlags &= ~ACC_ABSTRACT;
            }
        }

    // ----- synthetic

    /**
    * Determine if the synthetic attribute is set.
    *
    * @return true if a synthetic type
    */
    public boolean isSynthetic()
        {
        return (m_nFlags & ACC_SYNTHETIC) != 0;
        }

    /**
    * Set the synthetic attribute.
    *
    * @param fSynthetic  true to set to synthetic, false to set to
    *                    non-synthetic class/interface type
    */
    public void setSynthetic(boolean fSynthetic)
        {
        if (fSynthetic)
            {
            m_nFlags |= ACC_SYNTHETIC;
            }
        else
            {
            m_nFlags &= ~ACC_SYNTHETIC;
            }
        }

    // ----- bridge

    /**
    * Determine if the bridge attribute is set.
    *
    * @return true if a bridge method
    */
    public boolean isBridge()
        {
        return (m_nFlags & ACC_BRIDGE) != 0;
        }

    /**
    * Set the bridge attribute.
    *
    * @param fBridge  true to set to bridge, false to set to
    *                 non-bridge method
    */
    public void setBridge(boolean fBridge)
        {
        if (fBridge)
            {
            m_nFlags |= ACC_BRIDGE;
            }
        else
            {
            m_nFlags &= ~ACC_BRIDGE;
            }
        }

    // ----- varargs

    /**
    * Determine if the varargs attribute is set.
    *
    * @return true if a varargs method
    */
    public boolean isVarArgs()
        {
        return (m_nFlags & ACC_VARARGS) != 0;
        }

    /**
    * Set the varargs attribute.
    *
    * @param fVarArgs  true to set to varargs method, false to 
    *                  set to non-varargs method
    */
    public void setVarArgs(boolean fVarArgs)
        {
        if (fVarArgs)
            {
            m_nFlags |= ACC_VARARGS;
            }
        else
            {
            m_nFlags &= ~ACC_VARARGS;
            }
        }

    // ----- strict

    /**
    * Determine if the strict attribute is set.
    *
    * @return true if a strict method
    */
    public boolean isStrict()
        {
        return (m_nFlags & ACC_STRICT) != 0;
        }

    /**
    * Set the strict attribute.
    *
    * @param fStrict  true to set to a strictfp method, false to 
    *                 set to non-strictfp method
    */
    public void setStrict(boolean fStrict)
        {
        if (fStrict)
            {
            m_nFlags |= ACC_STRICT;
            }
        else
            {
            m_nFlags &= ~ACC_STRICT;
            }
        }

        
    // ----- annotation

    /**
    * Determine if the annotation attribute is set.
    *
    * @return true if an annotation type
    */
    public boolean isAnnotation()
        {
        return (m_nFlags & ACC_ANNOTATION) != 0;
        }

    /**
    * Set the annotation attribute.
    *
    * @param fAnnotation  true to set to annotation type, false to set to
    *                     class/interface type
    */
    public void setAnnotation(boolean fAnnotation)
        {
        if (fAnnotation)
            {
            m_nFlags |= ACC_ANNOTATION;
            }
        else
            {
            m_nFlags &= ~ACC_ANNOTATION;
            }
        }

    // ----- enum

    /**
    * Determine if the enum attribute is set.
    *
    * @return true if an enum type
    */
    public boolean isEnum()
        {
        return (m_nFlags & ACC_ENUM) != 0;
        }

    /**
    * Set the enum attribute.
    *
    * @param fEnum  true to set to enum type, false to set to
    *               class/interface type
    */
    public void setEnum(boolean fEnum)
        {
        if (fEnum)
            {
            m_nFlags |= ACC_ENUM;
            }
        else
            {
            m_nFlags &= ~ACC_ENUM;
            }
        }
        
    // ----- static

    /**
    * Determine the static attribute value.
    *
    * @return true if static
    */
    public boolean isStatic()
        {
        return (m_nFlags & ACC_STATIC) != 0;
        }

    /**
    * Set the static attribute.
    *
    * @param fStatic  true to set to static, false to set to instance
    */
    public void setStatic(boolean fStatic)
        {
        if (fStatic)
            {
            m_nFlags |= ACC_STATIC;
            }
        else
            {
            m_nFlags &= ~ACC_STATIC;
            }
        }


    // ----- final

    /**
    * Determine the final attribute value.
    *
    * @return true if final
    */
    public boolean isFinal()
        {
        return (m_nFlags & ACC_FINAL) != 0;
        }

    /**
    * Set the final attribute.
    *
    * @param fFinal  true to set to final, false to set to derivable
    */
    public void setFinal(boolean fFinal)
        {
        if (fFinal)
            {
            m_nFlags |= ACC_FINAL;
            }
        else
            {
            m_nFlags &= ~ACC_FINAL;
            }
        }


    // ----- synchronized

    /**
    * Determine if the synchronized attribute is set.
    *
    * @return true if synchronized
    */
    public boolean isSynchronized()
        {
        return ((m_nFlags & ACC_SYNCHRONIZED) != 0);
        }

    /**
    * Set the synchronized attribute.
    *
    * @param fSync  true to set to synchronized, false otherwise
    */
    public void setSynchronized(boolean fSync)
        {
        if (fSync)
            {
            m_nFlags |= ACC_SYNCHRONIZED;
            }
        else
            {
            m_nFlags &= ~ACC_SYNCHRONIZED;
            }
        }


    // ----- native

    /**
    * Determine if the native attribute is set.
    *
    * @return true if native
    */
    public boolean isNative()
        {
        return (m_nFlags & ACC_NATIVE) != 0;
        }

    /**
    * Set the native attribute.
    *
    * @param fNative  true to set to native, false to set to Java
    */
    public void setNative(boolean fNative)
        {
        if (fNative)
            {
            m_nFlags |= ACC_NATIVE;
            }
        else
            {
            m_nFlags &= ~ACC_NATIVE;
            }
        }


    // ----- volatile

    /**
    * Determine if the volatile attribute is set.
    *
    * @return true if volatile
    */
    public boolean isVolatile()
        {
        return (m_nFlags & ACC_VOLATILE) != 0;
        }

    /**
    * Set the volatile attribute.
    *
    * @param fVolatile  true to set to volatile, false otherwise
    */
    public void setVolatile(boolean fVolatile)
        {
        if (fVolatile)
            {
            m_nFlags |= ACC_VOLATILE;
            }
        else
            {
            m_nFlags &= ~ACC_VOLATILE;
            }
        }


    // ----- transient

    /**
    * Determine if the transient attribute is set.
    *
    * @return true if transient
    */
    public boolean isTransient()
        {
        return (m_nFlags & ACC_TRANSIENT) != 0;
        }

    /**
    * Set the transient attribute.
    *
    * @param fTransient  true to set to transient, false to set to persistent
    */
    public void setTransient(boolean fTransient)
        {
        if (fTransient)
            {
            m_nFlags |= ACC_TRANSIENT;
            }
        else
            {
            m_nFlags &= ~ACC_TRANSIENT;
            }
        }

    // ----- validation

    /**
    * Return whether at least the provided bit mask of access flags is set.
    *
    * @param nMask  a bit-mask of the access flags that should be set
    *
    * @return whether all bits in the provided bit-mask are set
    */
    protected boolean isMaskSet(int nMask)
        {
        return (m_nFlags & nMask) != 0;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The name of this class.
    */
    private static final String CLASS = "AccessFlags";

    /**
    * The access_flags bit values.
    */
    private int m_nFlags;

    /**
    * Tracks whether the flags have been modified.
    */
    private int m_nPrevFlags;
    }
