# Copyright 2006 BEA Systems, Inc.
# Should be dotted by all infrastructure scripts.  Will take some of the 
# hairyness out of devenv.  Devenv must be run first, however.
# make sure new versions of bash >= 3.2.9-11 on Cygwin ignore carriage returns
set -o | grep igncr >/dev/null && set -o igncr # comment required here

updateNightlyClient() {
  CLIENT=$P4CLIENT
  #delete all current client lines containing $SRC
  p4 client -o | egrep -v "//depot/dev/$SRC/" > .p4client.tmp
  #now append to the client the "min QA tools build" client lines
  sed "s|<CLIENT>|$CLIENT|g
     s|<SRC>|$SRC|g" $DEV_ROOT/tools/wls/infra/build/minnightlyclient.template >> .p4client.tmp
  cat .p4client.tmp | p4 client -i 
  rm .p4client.tmp
}

LOGNAME=${LOGNAME:-$USERNAME}
# use []'s rather than test because of MKS W2K??? herrlich@bea.com May 5, 2001
# otherwise it you can get an infinite loop in test.sh (???)
[ "$OS" = "" ] && OS=`uname -s`
# Setting of OSABR.  We use this in our log files, config files, etc.
if [ "${OS}" = "Windows_NT" ]
then
  case `uname -rv` in
	"4 10") OSABR=W98;;
	"4 90") OSABR=WME;;
	"4 00") OSABR=NT;; # this is also Windows 95
	"5 00") OSABR=W2K;;
	"5 01") OSABR=XP;;
	"5 02") OSABR=W2K3;;
        "6 00") OSABR=VISTA;;
        "6 01") OSABR=W2K7;;
        "6 02") OSABR=W2K8;;
        "6 03") OSABR=W2K12;;
	*)
	    case `cmd /c ver | sed -n "s/.*Version \([0-9]*\.[0-9]*\).*/\1/gp"` in
		"4.10") OSABR=W98;;
		"4.90") OSABR=WME;;
		"4.00") OSABR=NT;;
		"5.00") OSABR=W2K;;
		"5.01") OSABR=XP;;
		"5.2")  OSABR=W2K3;;
		"6.0")	OSABR=VISTA;;
		"6.1")	OSABR=W2K7;;
		"6.2")	OSABR=W2K8;;
		"6.3")	OSABR=W2K12;;
		*)
		    echo WARNING: unknown Windows version 1>&2
		    OSABR=Windows
		    ;;
	    esac
  esac
else
  case $OS in
    Solaris|SunOS)		OSABR="Sol"	;;
    HPUX|HP-UX)			OSABR="HP"	;;
    Linux)			OSABR="Linux"	;;
    Darwin)			OSABR="Darwin"	;;
    SuSE)			OSABR="s390"     ;;
    AIX)			OSABR="AIX"	;;
    OSF1)			OSABR="OSF"	;;
    IRIX)			OSABR="IRIX"	;;
    *)				OSABR="???"	
    echo "Error, OS name not found in case statement"
    exit 1 ;;
  esac
fi
export OSABR

# Setting of HOST: Important mostly when it comes to cluster-testing. 
HOST=${HOST:-`hostname`}
export HOST

if [ "${OS}" = "Windows_NT" ]; then
  root="${SystemRoot:-${SYSTEMROOT:-${systemroot}}}"
  root="`echo $root | sed 's:\\\\:/:g'`"
  MD5SUM="md5"
else
  MD5SUM="md5sum"
fi
if [ "${OSABR}" = "VISTA" ]; then
   MD5SUM="md5"
fi
export MD5SUM
if [ "${OS}" = "Windows_NT" ]
then
  RQSITE=//centralrq.subnet3ad2phx.devweblogicphx.oraclevcn.com/rqueue/coherence
  # for some reason (newer windows, MKS 10+??) net use works in cmd /c but not from the mks shell
  cmd /c net use \\\\centralrq.subnet3ad2phx.devweblogicphx.oraclevcn.com 4sure /user:centralrq.subnet3ad2phx.devweblogicphx.oraclevcn.com\\wls >/dev/null
  $DEV_ROOT/tools/wls/infra/delete_shares.sh
elif [ "${OS}" = "Darwin" ]
then
  # as far as I can tell dumbarton WEBSITE location does not exist anymore
  RQSITE=$HOME/.mounts/rqueue/coherence
  mkdir -p $RQSITE
  mount -t smbfs //wls:4sure@centralrq.subnet3ad2phx.devweblogicphx.oraclevcn.com/rqueue $RQSITE
else
  if [ -d /mounts/centralrq_data ]; then
      RQSITE=/mounts/centralrq_data/coherence
  else
      RQSITE=/net/centralrq-data.subnet3ad2phx.devweblogicphx.oraclevcn.com/centralrq_data/coherence
  fi
fi
if [ ! -z "$RQSITE_CTL" ]; then
    RQSITE=$RQSITE_CTL
fi
# This is usually defined in the .cfg at sites remote to San Francisco that
# have their own cache of kits.  It is only used by the scripts to get kits.
case "$SITE" in
    burlington)	        KITSITE=bup4p:/wls/results ;;	
    liberty-corner)	KITSITE=lcp4p:/wls/results ;;
    reno)               KITSITE=renocakes:/wls/results ;;
    *)			KITSITE=renocakes:/wls/results ;;
esac
if [ ! -z "$KITSITE_CTL" ]; then
    KITSITE=$KITSITE_CTL
fi
if [ ! -z "$KITSITE_ctl" ]; then
    KITSITE=$KITSITE_ctl
fi
export KITSITE RQSITE

# MAILPROG is the program per OS used for sending mail.  We use it to send 
# the monkey mail, etc.
from_name="${LOGNAME}"
if [ "$from_name" = "bt" -o \
    "$from_name" = "wls" -o \
    "$from_name" = "wlsbt" -o \
    "$from_name" = "stjpg" -o \
    "$from_name" = "aime" -o \
    "$from_name" = "aime1" -o \
    "$from_name" = "aime2" -o \
    "$from_name" = "aime3" -o \
    "$from_name" = "aime4" -o \
    "$from_name" = "aime5" -o \
    "$from_name" = "aime6" ]; then
    from_name="wls-bt_ww"
fi
case $OS in
  HP-UX|HPUX)  MAILPROG="/usr/bin/mailx -r ${from_name}@oracle.com -s" ;;
  Windows_NT|Linux|Solaris|SunOS|Darwin) MAILPROG="$DEV_ROOT/tools/wls/infra/mailprog.pl -f ${from_name}@oracle.com -Hinternal-mail-router.oracle.com -S" ;;
  SuSE)	MAILPROG="/bin/mail -s" ;;
  AIX)  MAILPROG="/bin/mailx -s" ;;
  OSF1)  MAILPROG="/usr/bin/mailx -s " ;;
  IRIX)  MAILPROG="/bin/mailx -r${from_name}@oracle.com -s" ;;
  *)  MAILPROG="???"	
  echo "Error, OS name not found in case statement"
  exit 1 ;;
esac
unset from_name
export MAILPROG

# WEBWRITE is whether or not to write to the shared drive.  Only true if 
# username is bt or release.
if [ "$LOGNAME" = "bt" -o "$LOGNAME" = "release" ]
then
  WEBWRITE=true
else
  WEBWRITE=false
fi
export WEBWRITE
