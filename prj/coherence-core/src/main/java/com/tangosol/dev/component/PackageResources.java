/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.dev.component;


import com.tangosol.util.Resources;


/**
* Registered prefixes:
*
*   ATTR        Attribute
*   RESOLVE     Trait resolve processing
*   EXTRACT     Trait extract processing
*
* @version 1.00, 11/17/97
* @author  Cameron Purdy
*/
public class PackageResources
        extends    Resources
        implements Constants
    {
    public Object[][] getContents()
        {
        return resources;
        }

    private static final Object[][] resources =
        {
        {ATTR_READONLY,             "The \"{0}\" attribute is not modifiable."},
        {ATTR_ILLEGAL,              "An attempt was made to set the \"{0}\" attribute to an illegal value."},
        {ATTR_NO_ADD,               "The \"{0}\" sub-trait is not addable."},
        {ATTR_NO_REMOVE,            "The \"{0}\" sub-trait is not removable."},
        {ATTR_NO_UNREMOVE,          "The \"{0}\" sub-trait is not un-removable."},

        {RESOLVE_UIDCHANGE,         "While resolving \"{0}\", it was determined that the base has been replaced. ({1})"},
        {RESOLVE_FORCERESOLVE,      "\"{0}\" did not naturally resolve and therefore was forcefully resolved. ({1})"},
        {RESOLVE_FORCEEXTRACT,      "\"{0}\" required a forced extraction before being resolved. ({1})"},
        {RESOLVE_PROPERTYDISCARDED, "During resolution, it was necessary to discard the \"{0}\" property due to a name conflict. ({1})"},
        {RESOLVE_BEHAVIORDISCARDED, "During resolution, it was necessary to discard the \"{0}\" behavior due to a name conflict. ({1})"},
        {RESOLVE_CHILDDISCARDED,    "During resolution, it was necessary to discard the \"{0}\" child component due to a name conflict. ({1})"},
        {RESOLVE_PROPERTYORPHANED,  "During resolution, it was necessary to discard the \"{0}\" property because it was orphaned. ({1})"},
        {RESOLVE_BEHAVIORORPHANED,  "During resolution, it was necessary to discard the \"{0}\" behavior because it was orphaned. ({1})"},
        {RESOLVE_CHILDORPHANED,     "During resolution, it was necessary to discard the \"{0}\" child component because it was orphaned. ({1})"},
        {RESOLVE_GLOBALFINAL,       "During resolution, it was necessary to discard the \"{0}\" child component because it attempts to derive from or modify the final component \"{1}\".  ({2})"},
        {RESOLVE_NOCATEGORY,        "During resolution, it was necessary to discard the \"{0}\" child component because it derives from \"{1}\" which does not match a declared aggregation category.  ({2})"},
        {RESOLVE_BEHAVIORRESERVED,  "During resolution, it was necessary to discard the \"{0}\" behavior because it is a reserved behavior.  ({1})"},
        {RESOLVE_PROPERTYRESERVED,  "During resolution, it was necessary to discard the \"{0}\" property because it is a reserved property.  ({1})"},
        {RESOLVE_BEHAVIORFINAL,     "During resolution, it was necessary to discard the derivation information for the \"{0}\" behavior because it attempts to derive from or modify a final behavior.  ({1})"},
        {RESOLVE_PROPERTYSTATIC,    "During resolution, it was necessary to discard the derivation information for the \"{0}\" property because it attempts to derive from a static property.  ({1})"},
        {RESOLVE_LOCALFINAL,        "During resolution, it was necessary to discard the derivation information for the \"{0}\" child component because it attempts to derive from or modify a final component.  ({1})"},
        {RESOLVE_LOADINTEGRATES,    "During resolution, an error occurred loading the integration map \"{0}\" required for the component \"{1}\"."},
        {RESOLVE_LOADINTERFACE,     "During resolution, an error occurred loading the interface \"{0}\" required to resolve the \"{1}\" attribute of the component \"{2}\"."},
        {RESOLVE_EXPANDINTERFACE,   "During resolution, an error occurred applying the interface \"{0}\" required to resolve the \"{1}\" attribute of the component \"{2}\"."},
        {RESOLVE_BEHAVIORNAMECHANGE,"During resolution, it was determined that the name of the behavior \"{0}\" had been changed to \"{1}\" at its declaration level. ({2})"},
        {RESOLVE_PARAMETERDISCARDED,"During resolution, it was determined that the \"{0}\" parameter for the behavior \"{1}\" had been discarded at the behavior's declaration level. ({2})"},
        {RESOLVE_EXCEPTIONDISCARDED,"During resolution, it was determined that the \"{0}\" exception for the behavior \"{1}\" had been discarded at the behavior's declaration level. ({2})"},
        {RESOLVE_PARAMTYPECHANGE,   "During resolution, it was determined that the data type of parameter {0} for the behavior \"{1}\" had been modified from \"{2}\" to \"{3}\" at the behavior's declaration level. ({4})"},
        {RESOLVE_PARAMNAMECHANGE,   "During resolution, it was determined that the name of parameter {0} for the behavior \"{1}\" had been modified from \"{2}\" to \"{3}\" at the behavior's declaration level. ({4})"},
        {RESOLVE_PARAMDIRCHANGE,    "During resolution, it was determined that the direction of parameter {0} for the behavior \"{1}\" had been modified from \"{2}\" to \"{3}\" at the behavior's declaration level. ({4})"},
        {RESOLVE_RETTYPECHANGE,     "During resolution, it was determined that the return value for the behavior \"{0}\" had been modified from \"{1}\" to \"{2}\" at the behavior's declaration level. ({3})"},
        {RESOLVE_EXCEPTTYPECHANGE,  "During resolution, it was determined that the exception for the behavior \"{0}\" had been modified from \"{1}\" to \"{2}\" at the behavior's declaration level. ({3})"},
        {RESOLVE_PROPNAMECHANGE,    "During resolution, it was determined that the property \"{0}\" had been renamed to \"{1}\" at its declaration level. ({2})"},
        {RESOLVE_PROPTYPECHANGE,    "During resolution, it was determined that the data type for the property \"{0}\" had been modified from \"{1}\" to \"{2}\" at the property's declaration level. ({3})"},
        {RESOLVE_PROPVALUEORPHANED, "During resolution, it was necessary to discard the \"{0}\" complex property value because it was orphaned. ({1})"},

        {EXTRACT_UIDCHANGE,         "While extracting the derivation or modification for \"{0}\", it was determined that the base has been replaced. ({1})"},
        {EXTRACT_PROPERTYDISCARDED, "During extraction, it was necessary to discard the \"{0}\" property due to a name conflict. ({1})"},
        {EXTRACT_BEHAVIORDISCARDED, "During extraction, it was necessary to discard the \"{0}\" behavior due to a name conflict. ({1})"},
        {EXTRACT_CHILDDISCARDED,    "During extraction, it was necessary to discard the \"{0}\" child component due to a name conflict. ({1})"},
        {EXTRACT_PROPERTYORPHANED,  "During extraction, it was necessary to discard the \"{0}\" property because it was orphaned. ({1})"},
        {EXTRACT_BEHAVIORORPHANED,  "During extraction, it was necessary to discard the \"{0}\" behavior because it was orphaned. ({1})"},
        {EXTRACT_CHILDORPHANED,     "During extraction, it was necessary to discard the \"{0}\" child component because it was orphaned. ({1})"},
        {EXTRACT_GLOBALFINAL,       "During extraction, it was necessary to discard the \"{0}\" child component because it attempts to derive from or modify the final component \"{1}\".  ({2})"},
        {EXTRACT_NOCATEGORY,        "During extraction, it was necessary to discard the \"{0}\" child component because it derives from \"{1}\" which does not match a declared aggregation category.  ({2})"},
        {EXTRACT_BEHAVIORNAMECHANGE,"During extraction, it was determined that the name of the behavior \"{0}\" had been changed to \"{1}\" at its declaration level. ({2})"},
        {EXTRACT_PARAMETERDISCARDED,"During extraction, it was determined that the \"{0}\" parameter for the behavior \"{1}\" had been discarded at the behavior's declaration level. ({2})"},
        {EXTRACT_EXCEPTIONDISCARDED,"During extraction, it was determined that the \"{0}\" exception for the behavior \"{1}\" had been discarded at the behavior's declaration level. ({2})"},
        {EXTRACT_PARAMTYPECHANGE,   "During extraction, it was determined that the data type of parameter {0} for the behavior \"{1}\" had been modified from \"{2}\" to \"{3}\" at the behavior's declaration level. ({4})"},
        {EXTRACT_PARAMNAMECHANGE,   "During extraction, it was determined that the name of parameter {0} for the behavior \"{1}\" had been modified from \"{2}\" to \"{3}\" at the behavior's declaration level. ({4})"},
        {EXTRACT_PARAMDIRCHANGE,    "During extraction, it was determined that the direction of parameter {0} for the behavior \"{1}\" had been modified from \"{2}\" to \"{3}\" at the behavior's declaration level. ({4})"},
        {EXTRACT_RETTYPECHANGE,     "During extraction, it was determined that the return value for the behavior \"{0}\" has been modified from \"{1}\" to \"{2}\" at the behavior's declaration level. ({3})"},
        {EXTRACT_EXCEPTTYPECHANGE,  "During extraction, it was determined that the exception for the behavior \"{0}\" had been modified from \"{1}\" to \"{2}\" at the behavior's declaration level. ({3})"},
        {EXTRACT_PROPNAMECHANGE,    "During extraction, it was determined that the property \"{0}\" had been renamed to \"{1}\" at its declaration level. ({2})"},
        {EXTRACT_PROPTYPECHANGE,    "During extraction, it was determined that the data type for the property \"{0}\" had been modified from \"{1}\" to \"{2}\" at the property's declaration level. ({3})"},
        {EXTRACT_PROPVALUEORPHANED, "During extraction, it was necessary to discard the \"{0}\" complex property value because it was orphaned. ({1})"},

        {MAP_ILLEGALINTEGRATES,     "While checking if the integration map was expandable for the component \"{0}\", it was determined that the parameters were invalid.  Both the JCS and the map are required parameters, and the JCS must be the signature specified by the integration map."},
        {MAP_MISSINGMETHOD,         "It is not possible to expand the integration map \"{0}\" for the component \"{1}\" because the method \"{2}\" is not present in the signature for\"{3}\"."},
        {MAP_INVALIDMETHOD,         "It is not possible to expand the integration map \"{0}\" for the component \"{1}\" because the method \"{2}\" in the signature for \"{3}\" is not an accessible Java method."},
        {MAP_RESERVEDBEHAVIOR,      "It is not possible to expand the integration map \"{0}\" for the component \"{1}\" because the behavior \"{2}\" is reserved."},
        {MAP_SCOPEMISMATCH,         "While checking if the integration map \"{0}\" was expandable for the component \"{1}\", a scope mismatch was found between the behavior \"{2}\" and the integrated method \"{3}\"."},
        {MAP_RETURNMISMATCH,        "While checking if the integration map \"{0}\" was expandable for the component \"{1}\", a return data type mismatch was found between the behavior \"{2}\" (which returns \"{3}\") and the integrated method \"{4}\" (which returns \"{5}\")."},
        {MAP_DIRECTIONMISMATCH,     "While checking if the integration map \"{0}\" was expandable for the component \"{1}\", a parameter direction mismatch was found between the behavior \"{2}\" and the integrated method \"{3}\"."},
        {MAP_MISSINGFIELD,          "It is not possible to expand the integration map \"{0}\" for the component \"{1}\" because the field \"{2}\" is not present in the signature for\"{3}\"."},
        {MAP_INVALIDFIELD,          "It is not possible to expand the integration map \"{0}\" for the component \"{1}\" because the field \"{2}\" in the signature for \"{3}\" is not an accessible Java field or constant."},
        {MAP_PROPNOTCREATABLE,      "It is not possible to expand the integration map \"{0}\" for the component \"{1}\" because the property \"{2}\" of data type \"{3}\" cannot be created."},
        {MAP_PROPFIELDMISMATCH,     "While checking if the integration map \"{0}\" was expandable for the component \"{1}\", a mismatch was found in the \"{2}\" attribute between the property \"{3}\" and the integrated field \"{4}\"."},
        {MAP_PROPVALUE,             "While checking if the integration map \"{0}\" was expandable for the component \"{1}\", the property \"{2}\" was found to have a value; (integrated properties must have no value)."},

        {IFACE_RESERVEDBEHAVIOR,    "It is not possible to expand the \"{0}\" interface \"{1}\" for the component \"{2}\" because the behavior \"{3}\" is reserved."},
        {IFACE_SCOPEMISMATCH,       "While checking if the \"{0}\" interface \"{1}\" was expandable for the component \"{2}\", the behavior \"{3}\", which is part of the interface, was found to be static."},
        {IFACE_RETURNMISMATCH,      "While checking if the \"{0}\" interface \"{1}\" was expandable for the component \"{2}\", a return data type mismatch was found for the behavior \"{3}\" (which returns \"{4}\", but the interface method returns \"{5}\")."},
        {IFACE_DIRECTIONMISMATCH,   "While checking if the \"{0}\" interface \"{1}\" was expandable for the component \"{2}\", a parameter direction mismatch was found with the behavior \"{3}\"."},
        {IFACE_BEHAVIORACCESS,      "While checking if the \"{0}\" interface \"{1}\" was expandable for the component \"{2}\", the behavior \"{3}\", which is part of the interface, was found to be missing public accessibility."},
        };
    }
