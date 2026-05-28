#!/bin/sh
#
# ///dev/main/tools/wls/infra/build/restart.sh
#
# used by nightly_all.sh to restart the nightly testing.  This script
# tries to off load as much processing as possible from nightly_all.sh
# so that we don't have to rbt into the machines multiple times.  Also
# this script *may* decide that something is running, like weekly,
# load or remote testing, that shouldn't be interrupted right now.
#
# I expect this script to be run *outside* of a devenv environment and
# tests might still be running while this script is running.  The
# usage pattern is to p4 print this file into dev/$SRC/wls and then
# run it.
#
# rbt machine "p4 print -q infra/build/restart.sh > restart.sh ; sh ./restart.sh > infra/build/restart.log 2>&1"
#

echo "`date`: beginning restart.sh"

if [ "$0" != "" ]; then
    cd `dirname $0`
fi

# can be started in dev/$SRC/wls, dev/$SRC/wls/infra,
# dev/$SRC/wls/infra/test, or dev/$SRC/wls/infra/build
thispath=`pwd`
thisdir=`basename "$thispath"`
[ "$thisdir" = "test" -o "$thisdir" = "build" ] && cd ../../../..
[ "$thisdir" = "infra" ] && cd ../../..
[ "$thisdir" = "wls" ] && cd ../..

# there is a "rogue" PWD env var that causes problems on Cygwin (corsair)
unset PWD

# FIXME: herrlich@bea.com Oct 7, 2002
# I had to the t.dat thing to get it to work on W2K/fitter
USERNAME=`id > t.dat ; cut < t.dat -d '(' -f 2 | cut -d ')' -f 1`
USERNAME=`basename $USERNAME`
export USERNAME
rm -f t.dat
if [ "$USERNAME" = "SYSTEM" ]; then
    echo "ERROR: running under SYSTEM account" 1>&2
    echo "ERROR: Windows Task Scheduler service misconfigured" 1>&2
    echo "ERROR: Scheduled Tasks -> Advanced -> AT Service Account (change to bt)" 1>&2
    exit 1
fi

# if mydevenv is there use it
if [ -f .rbt.envs ]; then
    . ./.rbt.envs
fi
if [ -f bin/cfglocal.sh ]; then
   if [ "`uname`" = "Windows_NT" ]; then
       bin/cfgwindows
   else
      . ./bin/cfglocal.sh
   fi
else
    # if no devenv.sh (!), revert and sync enough to use it
    if [ ! -f bin/cfglocal.sh ]; then
	p4 revert bin/%%1 
        p4 sync -f bin/%%1
    fi
    if [ "`uname`" = "Windows_NT" ]; then
       bin/cfgwindows
    else
       . ./bin/cfglocal.sh
    fi
fi

# if these are not defined we are in a really messed up env
: ${DEV_ROOT:?} ${SRC:?}
cd ${DEV_ROOT}/tools/wls

# don't do anything if check_stop.sh says we can't stop
sh ./infra/test/check_stop.sh show | grep STOPTEST=NO
if [ $? -eq 0 ]; then
    echo INFO: STOPTEST=NO not restarting
    exit
fi

[ -f infra/infraenv.sh ]  || p4 sync -f infra/infraenv.sh
. ./infra/infraenv.sh
[ -f infra/build/rbt.sh ] || p4 sync -f infra/build/rbt.sh
. ./infra/build/rbt.sh

# FIXME: herrlich@bea.com Dec 6, 2005
# turn on debugging, I'm seeing interrupted remote tests
# Ex job.9.20051129162756.578, and I want to understand why
set -x

# if this is a remote test only box and we are busy doing remote testing
# right now then don't kill it but enqueue a high priority (higher than
# nightly testing or remote testing) to restart this box when the current
# remote test has completed.
if rbt_nightly_excluded; then
    # use rqstatus -a to get the status from *all* branches, this assumes
    # the other branches are part of the common remote queue
    rqstatus -a | grep "busy remote" >/dev/null
    if [ $? -eq 0 ]; then
	rqstatus -i machine restart waiting for remote test job to complete
	renqueue -l -p 0 sh ./infra/test/at_restart.sh
	echo "`date`: completed restart.sh"
	exit
    fi
fi

# do this to show the pid's for debugging of queue problems
grep . /tmp/rdequeuer-$SRC.*

set +x

[ -f infra/build/killbt.sh ]  || p4 sync -f infra/build/killbt.sh
[ -f infra/build/killbt.awk ] || p4 sync -f infra/build/killbt.awk
sh ./infra/build/killbt.sh

p4 revert //...

# make sure this works if critical files get p4 sync #none
p4 sync infra/... ../../bin/...
(
    p4 diff -se infra/...  
    p4 diff -sd infra/... 
) | p4 -x- sync -f

. ./infra/infraenv.sh
. ./infra/build/rbt.sh
rqstatus -i machine restarting
rqstatus busy

rdequeuer -c

# After the job has completed then go and do a "p4clean" so that it
# won't have to be done at the beginning of the next job
renqueue -l ". infra/build/rbt.sh ; rbt_p4clean"

if rbt_nightly_excluded; then
    sh -c "./infra/test/at_rdequeuer.sh -r"
else
    sh ./infra/test/at_testrel_scheduler.sh
fi

echo "`date`: completed restart.sh"
