#!/bin/sh
#
# make sure new versions of bash >= 3.2.9-11 on Cygwin ignore carriage returns
set -o | grep igncr >/dev/null && set -o igncr # comment required
# Method to do an 'ls' on a given directory and generate the list of files only
# for that directory
ls4p4() {

P4LS=`\ls -p $1 | grep -v "\/" | grep -v "@"`
for file in $P4LS; do
        echo $1/$file
done

}

# Calculate what files are in the P4 client but not in P4.  This looks
# at *all* files in the client and I suggest that you sync and "make
# clean" in all branches you have mapped before you run this.

[ ! -d /tmp ] && mkdir /tmp

# Set the SEARCH_DIR to default to "..." expected by p4
SEARCH_DIR="..."
SYNCF_ARG="true"

if [ $# -ne 0 ]; then
    # If there is an arg, check to see if it's the p4 arg for the files in the current
    # directory.  If it is, set the directory to search to %%1.  If not, exit with error msg.
    if [ "$1" = "%%1" ]; then
        SEARCH_DIR="%%1"
    elif [ "$1" = "nosyncf" ]; then
	SYNCF_ARG="false"
    else
        echo 1>&2 $0 takes no arguments!
        exit 1
    fi
fi

if [ "$CONNECTED" = "false" ]; then
    echo 1>&2 "Cannot run without p4 (CONNECTED=false)"
    exit 1
fi

case `uname` in
    SunOS) AWK=nawk;;
    *)     AWK=awk;;
esac

P4INFO=/tmp/`basename $0`.p4info_$$
CLIENTFILES=/tmp/`basename $0`.clientfiles_$$
CLIENTDIRS=/tmp/`basename $0`.clientdirs_$$
P4HAVE=/tmp/`basename $0`.p4have_$$
P4OPENED=/tmp/`basename $0`.p4opened_$$
RMDIRS=/tmp/rmdirs.dat
P4SYNC=/tmp/`basename $0`.p4sync_$$

# workaround for cygwin users
if [ -f "`which cygpath 2>/dev/null`" ]; then
	p4 -V | grep " P4/CYGWIN" >/dev/null
	if [ $? -ne 0 ]; then
	    # add /bin so that occludes the cygwin1.dll in env/bin/cygwin/
	    PATH="/usr/local/bin:/bin:$PATH"
	    export PATH
	    p4 -V | grep " P4/CYGWIN" >/dev/null
	    if [ $? -ne 0 ]; then
		echo "FATAL: You must use the Cygwin version of p4.exe with p4clean (a.k.a. not_in_p4.sh)" 1>&2
		echo "FATAL: Put Cygwin p4.exe in /usr/local/bin, usually c:/cygwin/usr/local/bin" 1>&2
		p4 info
		exit 2
	    fi
	fi
	p4 client -o | grep ^AltRoots: >/dev/null
	if [ $? -ne 0 ]; then
	    echo "FATAL: To use Cygwin and p4 you must have AltRoots: set in your p4 client!" 1>&2
	    echo "FATAL: For example, if your client Root: is c:/weblogic" 1>&2
	    echo "FATAL: one of your AltRoots: is usually /cygdrive/c/weblogic" 1>&2
	    p4 info
	    exit 2
	fi
        P4SYNC=`cygpath -m $P4SYNC`
        if [ ! x"$P4CONFIG" = "x" ] 
            then 
            P4CONFIG=`cygpath -m $P4CONFIG`
        fi
	CWLH="`cygpath ${DEV_ROOT:?}`"
else
	CWLH="${DEV_ROOT:?}"
fi

# this convoluted algorithm avoids \n type translation of echo in MKS
# For example, paths of the form \cc\wlw come out mangled in MKS because \c
# is a special sequence.
tmpfile=/tmp/test.tmp_$$
cat <<EOF | sed 's/\\/\\\\/g' > $tmpfile
$DEV_ROOT
EOF
WLH="`cat $tmpfile`"

rm -f $P4INFO $CLIENTFILES $CLIENTDIRS $RMDIRS $P4HAVE $P4OPENED $P4SYNC $tmpfile

# sometimes this env var confuses p4
unset PWD

set -e
# so I only have to do this once
# do this at the beginning and not in the background so if there is a basic
# p4 problem we get a nice simple failure
p4 info > $P4INFO
set +e

if [ ! -s $P4INFO ]; then
    echo ""
    echo ""
    echo ""
    echo "FATAL: p4 info command did not work, skipping p4clean!"
    echo "FATAL: p4 info command did not return any data, please check and rerun."
    echo ""
    echo ""
    echo ""
    exit 1
fi

curr_dir=`grep '^Current directory: ' < $P4INFO | sed -e 's|\\\\|/|g' -e 's/^Current directory: //p' | uniq`

dosed1=0
dosed2=0
if [ "${SEARCH_DIR}" = "..." ]; then
    find  "$curr_dir" -type f -o -type l > $CLIENTFILES &
    clientfiles_pid=$!
else
    ls4p4 "$curr_dir"         > $CLIENTFILES &
    clientfiles_pid=$!
fi

if [ "${SEARCH_DIR}" = "..." ]; then
    # "which cmd.exe" means Windows MKS or Cygwin
    if [ "`cmd.exe /c date /t 2>/dev/null`" != "" ]; then
        cmd /c subst 2>/dev/null > /tmp/subst$$
        if [ -s /tmp/subst$$ ]
        then
          # substitution in play - don't use dir
	  find  "$curr_dir" -type d > $CLIENTDIRS &
          clientdirs_pid=$!
        else
	  # use dir on Windows because it handles very long path's
	  cmd /c dir /a:d/s/b > $CLIENTDIRS &
          clientdirs_pid=$!
          dosed2=1
        fi
        rm -f /tmp/subst$$
    else
	find  "$curr_dir" -type d > $CLIENTDIRS &
        clientdirs_pid=$!
    fi
else
    cat /dev/null > $CLIENTDIRS &
    clientdirs_pid=$!
fi

p4 have ${SEARCH_DIR} > $P4HAVE &
p4have_pid=$!

# pass open files through p4 where so we have client path
p4 opened ${SEARCH_DIR} | awk '-F#' '{print $1}' | p4 -x- where > $P4OPENED &
p4opened_pid=$!

wait $clientfiles_pid
clientfiles_status=$?
if [ $clientfiles_status -ne 0 -a $clientfiles_status -ne 127 ]; then
    echo "ERROR: find on p4 client files failed ($clientfiles_status)!" 1>&2
    exit 1
else
    if [ ! -s $CLIENTFILES ]; then
	echo "ERROR: find on p4 client files failed ($clientfiles_status)!" 1>&2
	exit 1
    fi
fi
if [ "$dosed1" = 1 ]
then
  sed -e 's|\\|/|g' < $CLIENTFILES  > ${CLIENTFILES}n
  mv ${CLIENTFILES}n $CLIENTFILES
fi
wait $clientdirs_pid
clientdirs_status=$?
if [ $clientdirs_status -ne 0 -a $clientdirs_status -ne 127 ]; then
    echo "ERROR: find on p4 client dirs failed ($clientdirs_status)!" 1>&2
    exit 1
else
    if [ ! -f $CLIENTDIRS ]; then
	echo "ERROR: find on p4 client dirs failed ($clientdirs_status)!" 1>&2
	exit 1
    fi
fi
if [ "$dosed1" = 1 ]
then
  sed -e 's|\\|/|g' < $CLIENTDIRS  > ${CLIENTDIRS}n
  mv ${CLIENTDIRS}n $CLIENTDIRS
fi
wait $p4have_pid
p4have_status=$?
if [ $p4have_status -ne 0 -a $p4have_status -ne 127 ]; then
    echo "ERROR: p4 have failed ($p4have_status)!" 1>&2
    exit 1
fi
wait $p4opened_pid
p4opened_status=$?
if [ $p4opened_status -ne 0 -a $p4opened_status -ne 127 ]; then
    echo "ERROR: p4 opened failed ($p4opened_status)!" 1>&2
    exit 1
fi

# the .awk script depends on the order of this information.  It uses some
# "p4 info" information to process the "p4 have" and "p4 opened" data and
# it must have the "p4 opened" after the "p4 have" so that it can delete
# the "p4 opened" files from the "p4 have" list.
# The ; after find is important, don't completely bail if the find fails,
# worst case it will think some p4 files are not in the client.
(
  echo "-- P4INFO --"	&& \
  cat $P4INFO		&& \
  echo "-- CLIENTFILES --" && \
  cat $CLIENTFILES	&&
  echo "-- CLIENTDIRS --" && \
  cat $CLIENTDIRS	&&
  echo "-- P4HAVE --"	&& \
  cat $P4HAVE		&& \
  echo "-- P4OPENED --"	&& \
  cat $P4OPENED ) |\
    $AWK -v WLH="$WLH" -v CWLH="$CWLH" -v p4config=$P4CONFIG -v sync_files="$P4SYNC" -v syncf=$SYNCF_ARG -v rmdirs=$RMDIRS -f `dirname $0`/`basename $0 sh`awk
saved_status=$?

rm -f $CLIENTFILES $CLIENTDIRS $P4INFO $P4HAVE $P4OPENED "$P4SYNC"
exit $saved_status
