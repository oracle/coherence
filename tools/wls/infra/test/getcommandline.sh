# Copyright 2006 BEA Systems, Inc.
#
# getCommandLine:  Command line parsing function.
# Usage:
#   getCommandLine "$@" [POSARG ...]
#
# Command line arguments containing '=' are evaluated and exported.
# Command line arguments lacking '=' are assumed to be positional
#  arguments, candidates for being assigned to the POSARG variables
#  and exported.
# Assignment-type arguments override positional arguments and environment
#  variables.
# Positional arguments override environment variables.
# CONFIG is a special variable, in the environment or on the command
# line.  It's the name of a file that contains variable settings to
# be applied and exported.
# SEE Bottom of file for a detailed writeup. 

# Save this shell's command line for now
CommandLine="$*"
getCommandLine() {
  # Save the variable names passed to this function
  posargnames="$*"

  if [ -n "$CommandLine" ]
  then
    # Parse the command line
    set "$CommandLine"
    for arg in $*
    do
      case "$arg" in
      *=*) eqargs="$eqargs $arg" ;;
      *) args="$args $arg" ;;
      esac
    done
  
    # Positional command line arguments override environment
    [ -n "$args" ] && set $args
    for arg in $posargnames
    do
      export $arg
      if [ $# -ge 1 ]
      then
        eval $arg=$1
        shift
      fi
    done
  
    # name=value command line arguments override positional command line args
    for arg in $eqargs
    do
      eval $arg
      export `expr $arg : "\([^=]*\)=.*"`
    done
  fi

  saved_pwd="`pwd`"
  parseConfig
  cd $saved_pwd
  unset CommandLine arg args eqargs posargnames
}

translateConfig() {
  cd $WEBLOGICHOME/dev/$SRC/wls/infra/build/config
  egrep -v '^#|^[        ]*$' ${1:?} | while read i; do
    case "$i" in
      *=*)
        echo $i
        echo export "$i" |awk -F= '{print $1}'
        ;;
      include*|INCLUDE*)
	echo "# BEGIN $i"
	translateConfig `echo $i | awk '{print $2}'`
	echo "# END $i"
	;;
      *)
       echo 1>&2 getCommandLine non-name value pair $i found in $CONFIG
       exit 1
    esac
  done
}

parseConfig() {

  if [ ! -n "$CONFIG" ]; then
      return
  fi

  cd $WEBLOGICHOME/dev/$SRC/wls/infra/build/config

  # If we didn't find it, complain and exit the calling script.
  if [ ! -f "$CONFIG" ]; then
    echo 1>&2 $0 getCommandLine cannot open $CONFIG!
    exit 1
  fi

  mkdir -p /tmp
  tmp_file=/tmp/`basename $0`.tmp_$$

  # Remove any existing tmp file
  if [ -f $tmp_file ]; then
    rm -f $tmp_file
  fi
  # Parse the CONFIG file.
  # Skip lines beginning with a #
  # Skip line beginning with a space or a tab
  # Skip blank lines
  # recurse for INCLUDE's
  translateConfig "$CONFIG" > $tmp_file
  STATUS=$?
  if [ $STATUS -eq 0 -a -f $tmp_file ]; then
    . $tmp_file
  fi
  rm -f $tmp_file
  if [ $STATUS -ne 0 ]; then
    exit $STATUS
  fi
}

#############################################
# Writeup:  Zsolt Szabo 11/2/2000
# getCommandLine is a Shell-Variable assignment tool
# which has 3 purposes; assignment, override, and
# configuration file assignment
#
# BACKGROUND
# In a normal shell script I.E. Blah.sh, the shell
# would be called in the following manner
# Blah.sh arg1 arg2 arg3 etc..  These arguments
# are considered to be positional variables. Within
# the program "Blah.sh"  you might expect to see the 
# following assignments
# variable1 = $1 ($1 corresponds with arg1)
# variable2 = $2 ($2 corresponds with arg2)
# etc.
# 
# USAGES:
# 1) Assignment
#  This can be accomplished in an easier fashion by 
#  using getCommandLine in the following manner at the
#  front of your code:
#    . <path>/getcommandline.sh
#    getCommandLine variable1 variable2 variable3 variable4
#  When the your main code is called I.E. 
#  "Blah.sh arg1 arg2 arg3"
#  getCommandLine will perform assign the following:
#  variable1= arg1
#  variable2= arg2
#  variable3= arg3
#  variable4= `'  -Notice variable 4 was created, but didn't have 
#                  an argument which could be assigned to it.
#  If more arguments are given than identified with getCommandLine,
#  they are not lost, they would just need to be accessed in the 
#  traditional manner I.E. extraArg=$6.  All variables are exported
#  when assigned (I.E. export variable1 = arg1)
#
# 2) Override
#  Keeping the same two lines identified in 1)Assignment. Use the 
#  sample program in the following manner.
#  Blah.sh arg1 NEWVARIABLE=NewValue arg2 arg3.
#  First, variables are assigned (like before) as if 
#  the "=" argument did not exist (Blah.sh arg1 arg2 arg3)
#  Next, the variable NEWVARIABLE is assigned the value NewValue
#  (Just like the way it's presented in the command line option).
#  If that variable existed before in the shell's environment or
#  was given an assignment prior to calling getCommandLine, it 
#  will be overridden with NewValue. To confuse you even more,
#  you could do something like;
#  (Blah.sh arg1 arg2 arg3 variable1=newval).  This would first
#  assign variable1=arg1, but immediately overwrite it with
#  variable1=newval.
#
# 3) Configuration file assignment
#  If you create a file with variable assignments, and you have 
#  an exported shell variable CONFIG which points to this file,
#  these variables will be assigned as defined by the file.
#  Configuration files have the final say.  They will overwrite
#  all other assignments.
###############################################

