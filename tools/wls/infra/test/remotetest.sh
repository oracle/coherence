#!/bin/sh
# Copyright 2006 BEA Systems, Inc.
#
# 1) This file is the quasi-static wrapper for remotetestrun.sh.
# 2) This file gets called from the ant target remotetest__run now in
#    src/build.xml.
# 3) This file uses remotetestutils.sh and other *.sh files.
#
#       Wednesday, May 31, 2006 3:39:12 PM   --jcnapier
#
# make sure new versions of bash >= 3.2.9-11 on Cygwin ignore carriage returns
set -o | grep igncr >/dev/null && set -o igncr # comment required
: ${RQSITE:?} ${SRC:?} ${DEV_ROOT:?}

cd $DEV_ROOT/tools/wls

# try to find a temp env var that has a drive letter because on Windows and
# MKS if we just use /tmp it could be on different drive letters if we cd
# around.  There are many parts of the code below that use tmp files and cd
# around so make sure TMPDIR is convienently always defined.
if [ -z "$TMPDIR" ]; then
    TMPDIR=${TMP:-${TEMP:-/tmp}}
    export TMPDIR
fi
if [ ! -d ${TMPDIR:?} ]; then
    mkdir ${TMPDIR:?}
fi

# make sure HOME is setup correctly to fix this error I've seen occasionally
# cd: %HOMEDRIVE%%HOMEPATH%: The system cannot find the file specified. 
if [ "$HOME" = "%HOMEDRIVE%%HOMEPATH%" ]; then
    HOME="$HOMEDRIVE$HOMEPATH"
    export HOME
fi

if [ "$HOME" = "" ]; then
    HOME=`dirname "$APPDATA"`
    export HOME
fi

# make sure most everything is here
P4DIRS="
//dev/$SRC/%%1
//dev/$SRC/bin/%%1
//dev/$SRC/tools/wls/...
"
# using tmp1 because I've seen the grep's hang... maybe if it isn't a pipe?
tmp1=${TMPDIR:?}/p4.tmp1_$$
p4 revert $P4DIRS > $tmp1 2>&1
egrep -v ' not opened on this client|, reverted' $tmp1
p4 sync $P4DIRS > $tmp1 2>&1
egrep -v ' - (refreshing|updating|added as|deleted as) | up-to-date' $tmp1
(
    p4 diff -se $P4DIRS
    p4 diff -sd $P4DIRS
) | p4 -x- sync -f

# unset all env vars that might be passed in to remotetest.sh to make sure they aren’t set in a rdequeuer restart, as
# we met lots of unexpected 'spin' remote jobs running on those failing boxes
unset JOBID REMOTE_TEST REMOTE_P4USER JV JV4TEST JVURL SUBMIT SYNCTO INTEGFROM SPIN PUBLISH MAILTO RTARGETS REVERT 
unset METADATA METATIME REMOTETESTENVSYNCTO WLTEST CODECOVERAGE MANIFESTVERSION PARALLEL TESTPROPS TESTDIR TESTFILE
unset RUNINFARM DEVINSTALL DEVINSTALLDATA
unset ADE INSTYPE STAGE_NAME

# this should be done *before* devenv because devenv messes with $*/$@
# getcommandline.sh doesn't need devenv
. infra/test/getcommandline.sh
getCommandLine REMOTE_TEST REMOTE_P4USER

PARALLEL="`echo $PARALLEL| tr A-Z a-z`"

# save the JV setting, re-set it just before remotetestrun.sh.  If JV is a
# bad value we don't want it breaking the devenv -dl and marking this as
# a bad machine
RBT_JV="$JV"
unset JV

# save OS setting for spin job
if [ "$SPIN" = "true" ]; then
    RBT_OS=`cat infra/test/queued/$RBT_QUEUED_ID | head -n 1 | sed s/.*OS=// | awk -F, '{ print $1 }'`
	export RBT_OS
fi

# from command line
# ID comes from job file name via redequeuer
: ${REMOTE_TEST:?test to run} ${REMOTE_P4USER:?p4 user} ${RBT_QUEUED_ID:?job id}

# job is runnable now
# ------------------------------------------------------------------------

# stop the virus scanner via ant task, get ~20% speedup
# Alternate:  config scanner to ignore build dirs
echo $0 | grep new.sh >/dev/null
if [ $? -eq 0 ]; then
    NEW=new
else
    NEW=
fi
. infra/test/remotetestutils$NEW.sh
stopvscan

# because we can't guarrentee that all the processes were previously killed
# and processes will interfer with the deletes below
infra/build/killbt.sh -u

if [ "`cmd.exe /c date /t 2>/dev/null`" != "" ]; then
    (
	cd $DEV_ROOT
	echo y | cmd /c cacls build /t /p everyone:f >/dev/null
    )
    PATH="$PATH:$DEV_ROOT/tools/wls/infra"
    export PATH
fi

cd $DEV_ROOT/tools/wls

# ------------------------------------------------------------------------
# . $DEV_ROOT/bin/cfglocal.sh  # not work on windows, so replaced by .rbt.envs
# ------------------------------------------------------------------------
. infra/infraenv.sh
. infra/build/rbt.sh
. infra/test/logfilenames.sh
. infra/test/remotetestutils$NEW.sh

# this will delete all the build directories
rbt_clean

# make sure we can access a few java commands, if we can't then move the job
# back to the central queue, send email and pause this box
for i in java javac jar; do
    if [ "`which $i 2>/dev/null`" = "" ]; then
	env | sort -iu
	BadMachine "devenv -dl failed (no java, javac or jar command)"
    fi
done

# make sure we don't have a bad version of jar (doesn't implement jar -u)
jar -u 2>&1 | grep -i "mode unimplemented" >/dev/null
if [ $? -eq 0 ]; then
    BadMachine "GNU gcc-java jar detected on machine (jar -u not supported)"
fi

# make sure the machine has rsync, new cygwin requirement from src_mod
# that not all machines have yet
#if [ "`which rsync 2>/dev/null`" = "" ]; then
#    BadMachine "rsync not found"
#fi

# Check port range
if [ "$OS" = "Linux" ]; then
   min_port=`cat /proc/sys/net/ipv4/ip_local_port_range | awk '{print $1}'`
   if [ "$min_port" -lt 32768 ]; then
      BadMachine "Linux minimum port is less than 32768 (modify /etc/sysctl.conf and run \"sysctl -p\")"
   fi
fi

if [ "$OS" = "Linux" ]; then
    ulimit=`ulimit -n`
    if [ -n "$ulimit" -a "$ulimit" -lt 4096 ]; then
        BadMachine "Linux ulimit -n is less than 4096 (modify /ets/security/limits.conf?)"
    fi
    unset ulimit
fi
# ------------------------------------------------------------------------
# check for pop-up windows that block gui tests
if [ "`cmd.exe /c date /t 2>/dev/null`" != "" -a -x $DEV_ROOT/tools/wls/infra/test/cmdow/cmdow.exe ]; then
    cmdowtmp=${TMPDIR:?}/cmdow.tmp_$$
    $DEV_ROOT/tools/wls/infra/test/cmdow/cmdow.exe > $cmdowtmp
    
    grep -i ' wuauclt ' $cmdowtmp >/dev/null
    [ $? -eq 0 ] && BadMachine "Windows Update has windows open"

    grep -i ' Symantec Email Proxy ' $cmdowtmp >/dev/null
    [ $? -eq 0 ] && BadMachine "Symantec Email Proxy has windows open"

    grep -i ' Service Control Manager ' $cmdowtmp >/dev/null
    [ $? -eq 0 ] && BadMachine "Service Control Manager has windows open"

    grep -i ' Eventlog Service ' $cmdowtmp >/dev/null
    [ $? -eq 0 ] && BadMachine "Eventlog Service has windows open"

    grep -i ' Data Execution Prevention ' $cmdowtmp >/dev/null
    [ $? -eq 0 ] && BadMachine "Data Execution Prevention has windows open"

    grep -i ' Windows - Virtual Memory Minimum Too ' $cmdowtmp >/dev/null
    [ $? -eq 0 ] && BadMachine "Windows - Virtual Memory Minimum Too Low has windows open"

    grep -i ' - Application Error' $cmdowtmp >/dev/null
    [ $? -eq 0 ] && BadMachine " - Application Error has windows open"

    grep -i ' csrss ' $cmdowtmp | grep -i 'Norton AntiVirus Corporate Edition' >/dev/null
    [ $? -eq 0 ] && BadMachine "AntiVirus has windows open"

    rm -f $cmdowtmp

    dotnettmp=${TMPDIR:?}/dotnet.tmp_$$
    \ls ${SystemRoot:-${SYSTEMROOT:-$systemroot}}/Microsoft.NET/Framework > $dotnettmp
    grep -i '^v2\.0' $dotnettmp >/dev/null
    [ $? -ne 0 ] && BadMachine "Microsoft .net V2.0 is not installed"

    grep -i '^v3\.0' $dotnettmp >/dev/null
    [ $? -ne 0 ] && BadMachine "Microsoft .net V3.0 is not installed"
    rm -f $dotnettmp

    if [ ! -f "${ProgramFiles:-${PROGRAMFILES:-$programfiles}}/HTML Help Workshop/hhc.exe" ]; then
        # add a hardcoded check for C:\Program Files
        if [ ! -f "C:/Program Files/HTML Help Workshop/hhc.exe" -a ! -f "C:/Program Files (x86)/HTML Help Workshop/hhc.exe" ]; then
            BadMachine "HTML Help compiler is not installed"
        fi
    fi

    #check if igncr is on for cygwin
    uname -s |grep "CYGWIN" 
    if [ $? -eq 0 ]; then
        (echo $SHELLOPTS |grep igncr || set -o |grep igncr |grep on)
        if [ $? -ne 0 ]; then
        	BadMachine "igncr is off or not set"
        fi
	# make sure all the required cygwin packages are installed
	for i in \
	    cpio \
	    cygrunsrv \
	    file \
	    openssh \
	    rsync \
	    unzip \
	    vim \
	    zip \
	    ; do
	    cygcheck -c $i | grep OK >/dev/null
	    if [ $? -ne 0 ]; then
		BadMachine "Cygwin package $i is not installed"
	    fi
	done
    fi

    # job id is required by kill_console_rdp
    JOBID="$RBT_QUEUED_ID"
    export JOBID

    # --------------------------------------------------------------------
    # test auto-login
    # if the console session is not logged in (i.e. STATE = Conn) then
    # the auto-login to "bt" didn't work
    #qwinsta | grep console | grep Conn >/dev/null
    #if [ $? -eq 0 ]; then
    #	BadMachine "Console not logged in (auto-login to bt did not work?)"
    #fi

fi

# the zip stays on dumbarton
# the zip includes:
# 1) p4opened files
# 2) p4changes.txt
#    a) pending chgnum
#    b) <blank line>
#    c) -Ddescription property form user's cmd
# 3) all opened files with pat rel to dev/src
# also see apply_zip for unpacking a remote job's changes into an existing workspace

mailto="`p4 user -o $REMOTE_P4USER | awk '/^Email:/{print $NF}'`"

echo $RBT_QUEUED_ID | grep '\-[0-9][0-9]*$' >/dev/null
if [ "$?" -eq 0 ]; then
    # if this is a parallel RQ run then remove the -1, -2, -3, etc from the end
    # of the job id to find the zip
    shortid=`echo $RBT_QUEUED_ID | sed 's/-[0-9][0-9]*$//'`
    ziploc="$SRC/remotetests/zips/$shortid.zip"
else
    ziploc="$SRC/remotetests/zips/$RBT_QUEUED_ID.zip"
fi

# "RQSITE" gets set in infraenv.sh as UNC path for windows
# the form of the name is: //home.us.oracle.com/centralrq
if [ -f $RQSITE/$ziploc ]; then
    REMOTE_CHANGES=""
    # download, collect mail info
    if [ -z "$ENCODING" ]; then
        jar -xf $RQSITE/$ziploc p4changes.txt
    else
        ant -f $DEV_ROOT/build.xml ant.unzip -Dzippedfile="$1" -Dunzipfilelist="p4changes.txt" -Dencoding="$ENCODING"
    fi
    if [ -f p4changes.txt ]; then
	REMOTE_CHANGES=`head -1 p4changes.txt`
    fi
    if [ "$REMOTE_CHANGES" = "\${change}" -o "$REMOTE_CHANGES" = "" ]; then
	REMOTE_CHANGES=""
    fi
fi

# ------------------------------------------------------------------------
get_timestamp
# dumbarton: full path=RQSITE/LOGDIR
# url is only for email
LOGDIR="${SRC:?}/remotetests/$RBT_QUEUED_ID/${TIMESTAMP:?}"
export LOGDIR
LOGURL="http://home.us.oracle.com/centralrq/$LOGDIR"
export LOGURL

# ------------------------------------------------------------------------
#                                  functions
# ------------------------------------------------------------------------
create_index() {
    (
        # "all_files.html" in results published
	title="Test logs for $TIMESTAMP"

	cat <<EOF
<html>
<head>
<title>$title</title>
</head>
<body>
<h1>$title</h1>
<p>Note: empty files are not shown</p>
<h2><a href="http://home.us.oracle.com/internal/coherence/$SRC/zipresults.jsp?id=$RBT_QUEUED_ID&timestamp=$TIMESTAMP">ZIP of all test logs</a></h2>
EOF

        # in utils
	ftp_list "$LOGDIR" | sed 's|^\./||' | sort | while read i; do
	    echo "<a href=\"$i\">$i</a><br>"
	done

	cat <<EOF
</html>
EOF
    ) > ${TMPDIR:?}/all_files.html
    # xfer(dest, src)
    ftp_xfer_file "$LOGDIR" ${TMPDIR:?}/all_files.html
    rm -f ${TMPDIR:?}/all_files.html
}

# ------------------------------------------------------------------------
#                              end functions
# ------------------------------------------------------------------------

# reset mailto if MAILTO set
if [ "$MAILTO" != "" ]; then
	mailto="$MAILTO,$mailto"
fi
export mailto

# initial email headers
SUBJECT="Remote $REMOTE_TEST on $SRC starting"
[ -z "$REMOTE_CHANGES" ] || SUBJECT="$SUBJECT, changes $REMOTE_CHANGES"
[ -z "$SYNCTO" ]         || SUBJECT="$SUBJECT, syncing @$SYNCTO"
SUBJECT="$SUBJECT, $RBT_QUEUED_ID"

shortid=`echo $RBT_QUEUED_ID | sed 's/-[0-9][0-9]*$//'`

# send email when not SPINning and if this the first job of a parallel run
if [ "$SPIN" != "true" -a "$RBT_QUEUED_ID" = "$shortid" ]; then
    (
        echo "To: $mailto"
        echo "Subject: $SUBJECT"
        echo ""
        echo "Job Status http://home.us.oracle.com/internal/coherence/$SRC/job.jsp?id=$RBT_QUEUED_ID"
        echo "Queue/Machine Status http://home.us.oracle.com/internal/coherence/$SRC/remote.jsp"
        echo "Queue/Machine Documentation http://home.us.oracle.com/internal/coherence/$SRC/remote.html"
        echo ""
        echo "Results directory $LOGURL (Results Zip http://home.us.oracle.com/internal/coherence/$SRC/zipresults.jsp?id=$RBT_QUEUED_ID?timestamp=$TIMESTAMP)"
        echo "P4 Opened Files http://home.us.oracle.com/centralrq/$ziploc (P4 Diff $LOGURL/p4diff.log)"
        echo ""
    ) | awk -f $DEV_ROOT/tools/wls/infra/build/htmlize.awk | $MAILPROG"$SUBJECT" $mailto
fi

rm -rf ${TMPDIR:?}/$RBT_QUEUED_ID
mkdir -p ${TMPDIR:?}/$RBT_QUEUED_ID

echo "INFO: pwd=`pwd`" 1>&2
logfile=${TMPDIR:?}/$RBT_QUEUED_ID/test.log

# tie the LOGDIR and the jsp together
# The meta tag is a hack that does a redirect
cat <<EOF > ${TMPDIR:?}/job.html
<html>
<body>
<meta http-equiv="refresh" content="0; url=http://home.us.oracle.com/internal/coherence/$SRC/job.jsp?id=$RBT_QUEUED_ID">
</body>
</html>
EOF

# ------------------------------------------------------------------------
# xfer(dest, src) results to home.us.oracle.com (provides link back to the jsp)
ftp_xfer_file "$LOGDIR" ${TMPDIR:?}/job.html
rm ${TMPDIR:?}/job.html

# ------------------------------------------------------------------------
# devenv uses its JV
# user gets his JV
if [ -n "$RBT_JV" ]; then
    JV="$RBT_JV"
    export JV
else
    unset JV
fi
unset RBT_JV

# ------------------------------------------------------------------------
echo "INFO: `date`: starting remotetestrun.sh"
`dirname $0`/remotetestrun$NEW.sh "$REMOTE_TEST" "$RQSITE/$ziploc" "$RBT_QUEUED_ID" "$LOGDIR" >$logfile 2>&1
saved_status=$?
echo "INFO: `date`: completed remotetestrun.sh ($saved_status)"

# ------------------------------------------------------------------------
# sync to a relatively known good change number
#p4 sync \
#    //depot/dev/$SRC/wls/%%1@${SRC}rel-test \
#    //depot/dev/$SRC/env/...@${SRC}rel-test \
#    //depot/dev/$SRC/tools/wls/infra/...@${SRC}rel-test

cd $DEV_ROOT

# xfer(dest, src) the run's stdout to site as file test.log
ftp_xfer_file "$LOGDIR" $logfile

# check for p4 depot corruption?  Possible strings to look for in "$logfile"
# without # or quotes...
# "^Librarian "
# ",v: "
# "^Operation: '.*' failed\.$"

# does a find on output tree
create_index

shortid=`echo $RBT_QUEUED_ID | sed 's/-[0-9][0-9]*$//'`
syncfile=`mktemp syncfile.XXXXXXXX`

# if this is not being run parallel then all the related jobs have completed
# check return code of rjobsync. If the return status is non-zero, do not try to retry jobs. Just send email.
RELATED_JOBS="completed"
rjobsync_status=0
if [ "$RBT_QUEUED_ID" != "$shortid" -o "$PARALLEL" = "true" ]; then
    echo "RBT_QUEUED_ID=$RBT_QUEUED_ID,shortid=$shortid"
    echo "`date` Job sync..."
    tries=0
    while [ $tries -lt 5 -a ! -s $syncfile ]; do
      ( rjobsync $RBT_QUEUED_ID x ) > $syncfile
      rjobsync_status=$?
      if [ ! -s $syncfile ]; then
        sleep 60
      fi
      tries=`expr $tries + 1`
    done
    if [ $rjobsync_status -eq 0 -a -s $syncfile ]; then
        cat $syncfile
        echo "`date` checking job sync..."
        # queued failed success running unknown
        grep -e ' queued$' -e ' running$' -e ' unknown$' < $syncfile
        if [ $? -eq 0 ]; then
          echo "Some job is still active, do not send email."
          RELATED_JOBS="active"
        else
          echo "All (?) jobs completed, send email."
          RELATED_JOBS="completed"
        fi
    else
        rjobsync_status=1
        echo "rjobsync exited abnormally. The return code is $rjobsync_status" 
    fi
fi

if [ $saved_status -eq 0 ]; then
    STATUS="`test_status`"
else
    STATUS="failed"
fi

THIS_JOB_STATUS="$STATUS"

if [ $RELATED_JOBS = "completed" ]; then
    SUBMIT="`echo $SUBMIT | tr A-Z a-z`"
    if [ "$SUBMIT" = "true" -o "$SUBMIT" = "yes" -o "$SUBMIT" = "zip" ]; then
        if [ "$STATUS" = "success" ]; then
            grep -e "Related job \(.*\) failed or was killed" -e "Related job \(.*\) did not succeed" $logfile > /dev/null
            if [ $? -eq 0 ]; then
                rqstatus -j $RBT_QUEUED_ID "INFO: Related job(s) failed or was killed"
            fi
        fi
    fi
fi

# if this is a subjob of a parallel run then adjust REMOTE_TEST to the master job and the STATUS to a summary of all the statuses
if [ "$RBT_QUEUED_ID" != "$shortid" ]; then
    masterfile=$RQSITE/$SRC/status/$shortid
    if [ -s $masterfile ]; then
        REMOTE_TEST=`awk 'FNR == 2 {print $2;exit}' < $masterfile`
    fi
fi
anyfailures=false
anysuccesses=false
if [ "$STATUS" = "partial success" ]; then
    anyfailures=true
    anysuccesses=true
else
    if [ "$STATUS" = "failed" ]; then
      anyfailures=true
    else
      grep ' failed$' < $syncfile > /dev/null
      if [ $? -eq 0 ]; then
        anyfailures=true
      fi
    fi
    if [ "$STATUS" = "success" ]; then
      anysuccesses=true
    else
      grep ' success$' < $syncfile > /dev/null
      if [ $? -eq 0 ]; then
        anysuccesses=true
      fi
    fi
fi
if [ "$anyfailures" = "true" ]; then
    if [ "$anysuccesses" = "true" ]; then
      STATUS="partial success"
    else
      STATUS="failed"
    fi
else
    if [ "$anysuccesses" = "true" ]; then
      STATUS="success"
    else
      STATUS="failed" # unknown?
    fi
fi

# email and/or IM
grep "queuing for approval" $logfile > /dev/null
if [ $? -eq 0 ]; then
# auto-submit failed and the user is in china_wlseng, re-enqueue to approval queue.
    SUBJECT="Remote $REMOTE_TEST on $SRC re-enqueued to approval queue"
    if [ "$SUBMIT" = "requeue" ]; then
	approval_message="submit=requeue, the job has been re-enqueued to approval queue."
    else
	approval_message="auto-submit failed and p4 user $REMOTE_P4USER is in the china_wlseng group, the job has been re-enqueued to approval queue!"
    fi
else
    SUBJECT="Remote $REMOTE_TEST on $SRC $STATUS"
fi
[ -z "$REMOTE_CHANGES" ] || SUBJECT="$SUBJECT, changes $REMOTE_CHANGES"
[ -z "$SYNCTO" ]         || SUBJECT="$SUBJECT, synced @$SYNCTO"
SUBJECT="$SUBJECT, $shortid"

# only email parallel runs when they complete
if [ "$RELATED_JOBS" = "completed" ]; then
    if [ \( "$SPIN" = "true" -a \( "$STATUS" = "failed" -o "$STATUS" = "partial success" \) \) -o "$SPIN" != "true" ]; then
(
    echo "To: $mailto"
    echo "Subject: $SUBJECT"
    echo ""
    echo "Job Status http://home.us.oracle.com/internal/coherence/$SRC/job.jsp?id=$RBT_QUEUED_ID"
    echo "Queue/Machine Status http://home.us.oracle.com/internal/coherence/$SRC/remote.jsp"
    echo "Queue/Machine Documentation http://home.us.oracle.com/internal/coherence/$SRC/remote.html"
    echo ""
    echo "Full stdout file $LOGURL/`basename $logfile`"
    echo "Results directory $LOGURL (Results Zip http://home.us.oracle.com/internal/coherence/$SRC/zipresults.jsp?id=$RBT_QUEUED_ID?timestamp=$TIMESTAMP)"
    echo "P4 opened files from your client http://home.us.oracle.com/centralrq/$ziploc (P4 Diff $LOGURL/p4diff.log)"
    echo ""
    if [ -n "$approval_message" ]; then
        echo "$approval_message"
        echo ""
    fi
    echo "Last few lines of stdout file..."
    echo ""
    tail -10 $logfile | uniq
) | awk -f $DEV_ROOT/tools/wls/infra/build/htmlize.awk | $MAILPROG"$SUBJECT" $mailto
    fi
fi

rm -rf ${TMPDIR:?}/$RBT_QUEUED_ID $syncfile

# ------------------------------------------------------------------------------
#                                for spin jobs
# If this is non-parallel job, just spin it; Spin after the last completed sub job in a parallel build.
# ------------------------------------------------------------------------------
set -x
# try spin here
# Not only the common job, now also allow the parallel build to spin
# for the parallel buld, the main job will spin after the last job of the related jobs completes, including retried jobs
if [ "$SPIN" = "true" -a "$RELATED_JOBS" = "completed" -a $rjobsync_status -eq 0 ]; then
    # if this is a subjob of a parallel run then adjust REMOTE_TEST and SYNCTO to the master job
    if [ "$RBT_QUEUED_ID" != "$shortid" ]; then
        masterfile=$RQSITE/$SRC/status/$shortid
        if [ -s $masterfile ]; then
            REMOTE_TEST=`awk 'FNR == 2 {print $2;exit}' < $masterfile`
            PARALLEL="true"
            SYNCTO_SAVE=`sed < $masterfile -n '2s/.* SYNCTO=\([^ ]*\) .*/\1/p'`
        fi
    else
        SYNCTO_SAVE=`sed < $DEV_ROOT/tools/wls/infra/test/queued/$RBT_QUEUED_ID -n '2s/.* SYNCTO=\([^ ]*\) .*/\1/p'`
    fi
    (
    cd $DEV_ROOT/prj
    P4USER=$REMOTE_P4USER
    export P4USER
    ant -Drq.nostatus=true -Drq.mailto="$MAILTO" -Drq.OS=$RBT_OS -Drq.syncto=$SYNCTO_SAVE -Drq.spin=$SPIN -Dparallel=$PARALLEL -Drq.description="$DESC" $REMOTE_TEST
    newjoburl=`sed < $tmpfile -n 's/.* \([^ ]*job.jsp[^ ]*\)/\1/p'`
    newjobid=`echo $newjoburl | cut -d= -f2`
    infomsg "re-enqueued, <a href=\"$newjoburl\">$newjobid</a>."    
    unset RBT_OS tmpfile newjoburl newjobid JOBID SYNCTO_SAVE
    )
fi

safe_revert

# After the job has completed then go and do a "p4clean" so that it
# won't have to be done at the beginning of the next job
# NOTE: this code is duplicated in both restart.sh and remotetest.sh,
# please keep them in sync
renqueue -l ". infra/build/rbt.sh ; rbt_p4clean $ENCODING"

exit $saved_status
