#!/bin/sh
# Copyright 2006 BEA Systems, Inc.
# make sure new versions of bash >= 3.2.9-11 on Cygwin ignore carriage returns
set -o | grep igncr >/dev/null && set -o igncr # comment required

# assert there is one parameter
: ${1:?}

# if $1 is foo.sh then dirname will be "." and the default script location
# will default to infra/test otherwise it will default to the full or
# relative path given as the at.sh script argument
DIR="`dirname $1`"
if [ "$DIR" = "." ]; then
    cd "`dirname $0`"
else
    cd "$DIR"
fi

SCRIPT="`basename $1`"
shift

if [ "`uname -o 2>&1`" = "Cygwin" ] ; then
    IN_CYGWIN=true
    # FIXME: Roger 2013-12-16
    # I have to unset env var TZ in cygwin to enable at job in cygwin env, since we use active perl
    # the env var TZ has effect on perl to get correct at job time. I used to fix this by unset TZ in .bashrc,
    # but that doesn't work in recent cygwin, when rq process kick another at job, the new at job in new sh/bash
    # will not load .bashrc (i presume), so i make it in our at.sh
    unset TZ
fi
if [ "`uname`" = "Windows_NT" -o "$IN_CYGWIN" = "true" ]; then
    # wait until we aren't within 10 seconds of the minute boundry because
    # the timing is too close or at 23:59 because the at /next
    # calculation gets too hard (stupid Windows needs 'at now')
    while [ `date +%S` -gt 50 -o "`date +%H:%M`" = "23:59" ]; do
	sleep 5
    done
    unset PERL5LIB
    in_one_minute=`perl -e 'use POSIX;print strftime "%H:%M", localtime(time() + 60)'`

    # use this in case MKS w/ at is installed (only > MKS 7.0?)
    AT=${SystemRoot:-${SYSTEMROOT:-$systemroot}}/System32/at
    
    if [ "$IN_CYGWIN" = "true" ] ; then
	DRIVE=`pwd | cygpath -w -f- | awk -F: '{print $1}'`      
	SH="`which bash | cygpath -w -f-` -l"
    else
	DRIVE=`pwd | awk -F: '{print $1}'`
	SH="sh"
    fi

    # the at job will fail if the log file still exists and is open by
    # some other process.  Try to remove the log file and then if it still
    # exists some process still has it open.  Search for script.log,
    # script.1.log, script.2.log, etc until you file a file that you can
    # delete and doesn't exist.  Let the env var LOG set to it
    # backup the log and keep it for seven days.
    if [ -d ${DRIVE}:/backup ]; then
	find ${DRIVE}:/backup -mtime +7 -exec rm {} \;
    else
	mkdir ${DRIVE}:/backup
    fi
    LOG=${DRIVE}:/$SCRIPT.log
    stamp=`date +%m%d%H%M`
    [ -f "$LOG" ] && cp "$LOG" ${DRIVE}:/backup/${SCRIPT}.${stamp}.log
    rm -f "$LOG"
    if [ -f "$LOG" ]; then
	i=1
	while [ $i -lt 100 ]; do
	    LOG=${DRIVE}:/$SCRIPT.$i.log
	    [ -f "$LOG" ] && cp "$LOG" ${DRIVE}:/backup/${SCRIPT}.$i.${stamp}.log
	    rm -f "$LOG"
	    if [ ! -f "$LOG" ]; then
		break
	    else
		LOG=NUL:
	    fi
	    i=`expr $i + 1`
	done
    fi

    # Windows 2008 (ver 6.2) doesn't like at but works ok with schtasks.  When I use schtasks
    # on older versions of Windows I seem to get AT Service Account errors where it prompts
    # for the password.
    cmd /c ver | sed -n "s/.*Version \([0-9]*\.[0-9]*\).*/\1/gp" | sed 's/\./ /g' > t.dat
    read major minor < t.dat
    rm -f t.dat
    if [ "$major" -eq 6 -a "$minor" -ge 1 ] || [ "$major" -gt 6 ]; then
        TN="$SCRIPT$RANDOM"
	echo Y | schtasks /create /sc once /st ${in_one_minute:?} /it /tn "$TN" /tr "cmd /c $SH `pwd`/$SCRIPT $* >$LOG 2>&1" > schtasks.log 2>&1
	if [ `wc -l < schtasks.log` -gt 1 ]; then
	    echo "FATAL: problem running schtasks command!"
	    cat schtasks.log
	    rm schtasks.log
	    exit 1
	fi
	schtasks /query /tn "$TN" 2>&1 | tee schtasks.log
	if (! grep -e Ready -e Running schtasks.log 2>&1 >/dev/null); then
	    echo "FATAL: did not see \"Ready\" or \"Running\" in task listing, deleting task!"
	    echo y | schtasks /delete /tn "$TN" /f
	fi
	rm -f schtasks.log
    else
	$AT ${in_one_minute:?} /i cmd /c "$SH `pwd`/$SCRIPT $* >$LOG 2>&1" > at.log 2>&1
	if [ `wc -l < at.log` -gt 1 ]; then
	    echo "FATAL: problem running at command!"
	    cat at.log
	    rm at.log
	    exit 1
	fi
	job_num=`awk '{print $NF}' < at.log`
	$AT $job_num 2>&1 | tee at.log
	if (! grep Today at.log 2>&1 >/dev/null); then
	    echo "FATAL: did not see \"Today\" in job listing, deleting job!"
	    $AT $job_num /delete
	fi
	rm -f at.log
    fi
else
    # use the correct stdout/stderr redirection for the login shell
    # "at" will use
    if [ -d ./backup ]; then
	find ./backup -mtime +7 -exec rm {} \;
    else
	mkdir ./backup
    fi
    stamp=`date +%m%d%H%M`
    cp ./$SCRIPT.log ./backup/$SCRIPT.$stamp.log
    if [ "$SHELL" = "/bin/csh" -o "$SHELL" = "/bin/tcsh" ]; then
	at now <<EOF
sh ./$SCRIPT $* >& ./$SCRIPT.log
EOF
    else
	at now <<EOF
sh ./$SCRIPT $* > ./$SCRIPT.log 2>&1
EOF
    fi
fi

