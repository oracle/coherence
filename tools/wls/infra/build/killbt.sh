#!/bin/sh
# make sure new versions of bash >= 3.2.9-11 on Cygwin ignore carriage returns
set -o | grep igncr >/dev/null && set -o igncr # comment required

:
# $Id$

# uid I'm interested in.  Use ps -u myuid rather than ps -u LOGNAME because
# that works on all systems *and* NT boxes where you are logged into a
# domain (Ex. East Coast DCS_ENG\bt).
# id gives output uid=999(bt) gid=999(users), first cut by = then by (
myuid=`id > t.dat ; cut < t.dat -d "=" -f 2 | cut -d "(" -f 1`
rm -f t.dat

# commands to try to kill (Note: mixture of UNIX and NT commands)
# the .awk checks for exact matches and the CMD at the end of the line and,
# if the CMD contains a / anywhere on the command line.  See the .awk for
# a further explaination.
CMDS="-sh|sh|-ksh|ksh|-bash|bash|-tcsh|tcsh|/bin/sh|/bin/ksh|/bin/bash|/bin/tcsh|cmd|CMD|rsh|make|expr|more|rsync|jdk13|jdk14|javadoc|apt|appletviewer|javap|java|javaw|jview|javac|jrcmd|sj|/jre/|/java/|sleep|btstatlo|btstatru|uninstall|cut|IEXPLORE|apache|httpd|tail|mkdir|beaexec|/installertmp/|p4|xargs|FileSystemFetch|FileSystemFetch.exe|CAPITestsLoadStress|capitestsloadstress|tnameserv|derby|IEDriverServer|sourceanalyzer"

# add BEA_HOME and RELEASE to the cmds to kill (if they are set)
if [ "$BEA_HOME" != "" ] ; then
    CMDS="$CMDS|$BEA_HOME"
fi
if [ "$RELEASE" != "" ] ; then
    CMDS="$CMDS|$RELEASE"
fi

# if -n this is a noop
if [ "$1" = "-n" ]; then
    NOOP="x"
    shift
fi

# if -u then  kill the mysterious cygwin "*** unknown ***" processes
# in *addition* to the regular kill logic
if [ "$1" = "-u" ]; then
    UNKNOWN="x"
    shift
else
    UNKNOWN=""
fi

# only kill a pid and it's children
if [ "$1" = "-p" ]; then
    shift
    KILLPID="$1"
    shift
else
    KILLPID=""
fi

awk_script="`dirname $0`/`basename $0 .sh`.awk"

KILL="kill"
if [ "`uname -o 2>&1`" = "Cygwin" ] ; then
  IN_CYGWIN=true
  PS="`dirname $0`/winps.sh"
  KILL="/usr/bin/kill -f"
  # for Cygwin add the shortname/dos versions of BEA_HOME/RELEASE to
  # the commands to kill
  if [ "$BEA_HOME" != "" ] ; then
      tmp=`cygpath -d "$BEA_HOME" | sed 's/\\\\/\\\\\\\\/g'`
      CMDS="$CMDS|$tmp"
      tmp=`cygpath -w "$BEA_HOME" | sed 's/\\\\/\\\\\\\\/g'`
      CMDS="$CMDS|$tmp"
      unset tmp
  fi
  if [ "$RELEASE" != "" ] ; then
      tmp=`cygpath -d "$RELEASE" | sed 's/\\\\/\\\\\\\\/g'`
      CMDS="$CMDS|$tmp"
      tmp=`cygpath -w "$RELEASE" | sed 's/\\\\/\\\\\\\\/g'`
      CMDS="$CMDS|$tmp"
      unset tmp
  fi
fi

if [ "`uname`" = "Windows_NT" ] ; then
  # make sure mks bin dir is first so it doesn't pick up the ps.exe in tlt-antutils/bin
  if [ -n "$ROOTDIR" ]; then
      PATH="$ROOTDIR/mksnt:$PATH"
      export PATH
  fi
  # for MKS add BEA_HOME/RELEASE with the /'s flipped to the commands to kill
  if [ "$BEA_HOME" != "" -a -d "$BEA_HOME" ] ; then
      tmp=`echo "$BEA_HOME" | sed 's:/:\\\\\\\\:g'`
      CMDS="$CMDS|$tmp"
      tmp=`cd "$BEA_HOME" ; command.com /c cd | sed 's/\\\\/\\\\\\\\/g'`
      CMDS="$CMDS|$tmp"
      unset tmp
  fi
  if [ "$RELEASE" != "" -a -d "$RELEASE" ] ; then
      tmp=`echo "$RELEASE" | sed 's:/:\\\\\\\\:g'`
      CMDS="$CMDS|$tmp"
      tmp=`cd "$RELEASE" ; command.com /c cd | sed 's/\\\\/\\\\\\\\/g'`
      CMDS="$CMDS|$tmp"
      unset tmp
  fi
fi

# if there are arguments then use them as the commands to kill
if [ $# -gt 0 ]; then
    CMDS=""
    while [ $# -gt 0 ]; do
	CMDS="$CMDS${CMDS:+|}$1"
	shift
    done
fi

# for linux CC, should protect tomcat4.1 process
isLinuxCC=0
if [ "`uname`" = "Linux" ]; then
  ps -ef | grep cruisecontrol | grep -v grep > /dev/null
  if [ $? -eq 0 ]; then
    isLinuxCC=1
	echo "run on linux CC, should protect tomcat process."
  fi
fi

pCCTomcat() {
  tomcatPID="`ps -ef | grep tomcat4.1 | grep -v grep | awk '{ print $2 }'`"
  tmp_tomcat=tmp_tomcat.$$
  cat $tmp_file2 | grep -v "^[ ]*$tomcatPID" > $tmp_tomcat
  cat $tmp_tomcat > $tmp_file2
  rm -f $tmp_tomcat
}

# stop all services with " BEA " in the name (i.e. nodemanager)
stop_BEA() {
    [ -z "$NOOP" ] && net start | sed -n "/ BEA /s/^ *//p" |\
	while read i; do net stop "$i"; done
}

kill_pids() {
    # So far I've tested on...
    # MKS 6.1, MKS 8.0, HP-UX, Solaris, Tru64 and Linux
    # kill unknown processes in *addition* to the normal kill logic
    if [ "$UNKNOWN" != "" ]; then
	if [ "$IN_CYGWIN" = "true" ]; then
	    echo "INFO: killing Cygwin zombie processes" 1>&2
	    # all zombie pids I've seen are 4 digit's and pids 4 and 8 are ok
	    # picking a middle ground of 3 or more digit pids to kill
	    pids=`ps -Wef | awk '/\*\*\* unknown \*\*\*/{if ($2 > 99) {print $2}}'`
	    if [ "$pids" != "" ]; then
		ps -Wef | grep unknown
		echo              $KILL $1 $pids
		[ -z "$NOOP" ] && $KILL $1 $pids
	    fi
	fi
    fi
    if [ -x "`which nawk 2>${tmp_error:-/dev/null} `" ]; then
	AWK=nawk
    else
	AWK=awk
    fi
    export AWK
    if [ "`uname`" = "Windows_NT" ]; then
	# calculate the mksdir from where "sh" lives, use this to find "sort"
	# because sometimes it finds sort in SystemRoot/System32
	sh_path="`which sh`"
	mksdir="`dirname "$sh_path"`/"
	if [ -z "$mksdir" ]; then
	    mksdir="$ROOTDIR/mksnt"
	fi
	stop_BEA
    elif [ "$IN_CYGWIN" = "true" ] ; then
	mksdir="/usr/bin/"
	stop_BEA
    else
	mksdir=""
    fi
    # UNIX95 is only really needed on HP-UX to turn on XPG/4 behavior so it
    # will recognize the -o parameters
    #
    # comm & args are done because on some platforms (HP-UX) "comm" only
    # gives a partial truncated command line ( "jav" ) while "args" gives a
    # full truncated command line ( "/usr/local/bla/bla/jav" ).  Both need
    # to be searched for CMDS.
    #
    # Must use -U to pickup real user ID, this seems to mostly be a problem on HP-UX
    #
    # sort -u to remove duplicate pids
    # I use a tmp_file because (awk;awk)|sort tries to kill the sort pid
    # use tmp_file2 so that the ps doesn't pickup pids of the pipe
    tmp_file1=tmp_file1.$$
    tmp_file2=tmp_file2.$$
    tmp_error=tmp_error.$$
    cat /dev/null > $tmp_file1
    if [ "$IN_CYGWIN" = "true" ] ; then
      $PS > $tmp_file2
    elif [ "`uname`" = "Windows_NT" ]  ; then
      # FIXME bmoyers 04/15/2015. ps -U uid hanging on windows.   Changing to the -U $USERNAME until it is resolved.
      ps -aopid,ppid,comm -U $USERNAME > $tmp_file2
    else
      UNIX95=x ps -aopid,ppid,comm -U $myuid > $tmp_file2
    fi
	[ "$isLinuxCC" = "1" ] && pCCTomcat
    $AWK -f $awk_script -v PPID=$$ -v KILLPID=$KILLPID -v CMDS="$CMDS" >> $tmp_file1 < $tmp_file2
    if [ "$IN_CYGWIN" = "true" ] ; then
      $PS -f > $tmp_file2
    elif [ "`uname`" = "Windows_NT" ]  ; then
      # FIXME bmoyers 04/15/2015. ps -U uid hanging on windows.   Changing to the -U $USERNAME until it is resolved.
      ps -aopid,ppid,comm -U $USERNAME > $tmp_file2
    else
      UNIX95=x ps -opid,ppid,args -U $myuid > $tmp_file2
    fi
	[ "$isLinuxCC" = "1" ] && pCCTomcat
    $AWK -f $awk_script -v PPID=$$ -v KILLPID=$KILLPID -v CMDS="$CMDS" >> $tmp_file1 < $tmp_file2
    # use to find right "sort" if MKS not in path, mksdir empty on UNIX
    pids="`"${mksdir}sort" -nu < $tmp_file1`"
    rm -f $tmp_file1 $tmp_file2 $tmp_error

    if [ "$pids" != "" ]; then
	echo              $KILL $1 $pids
	[ -z "$NOOP" ] && $KILL $1 $pids
    fi
}

if [ "$NOOP" != "" ]; then
    echo "INFO: -n used, not really killing anything"
    sleep 3
fi

kill_pids

sleep 3

kill_pids -9

# This part is added because we find a left java process on server 2008 after
# remoteDRT_kitdrt runs, the process seems to be related with Derby. This java process
# can not be controled by cygwin ps/kill/pv.exe, so we have to kill it via cmd taskkill.
# "java -jar adelabels/jdk7/db/lib/derbyrun.jar server shutdown -p 19082" can stop derby
# service but we still want to directly kill java in case other java process are left.
if [ "$IN_CYGWIN" = "true" ]; then
  echo "Checking left java processes ..."
  ps -Wu $myuid | grep -i java.exe | grep -v grep | grep "\\\\build" | awk '{print $1}' | while read javapid; do
    echo "Killing java process: $javapid"
    taskkill.exe /pid $javapid /f
  done
fi
