#!/bin/sh
# Copyright 2006 BEA Systems, Inc.
# make sure new versions of bash >= 3.2.9-11 on Cygwin ignore carriage returns
set -o | grep igncr >/dev/null && set -o igncr # comment required

# This assumes this lives in the client in dev/main/tools/wls/infra/test
cd `dirname $0`/../../../..
tmpdir=`pwd`
DEV_ROOT=${tmpdir:?}
export DEV_ROOT

# this is the branch
echo "$DEV_ROOT" | grep -e /release/ -e /coherence-ce/ > /dev/null
if [ $? -eq 0 ]; then
   SRC="`echo $DEV_ROOT | awk -F/ '{print $(NF-1) FS $NF}'`"
else
   SRC=`basename $DEV_ROOT`
fi
export SRC

unset tmpdir

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

HOST=`uname -n | tr A-Z a-z`
export HOST
# add common location of p4 binaries
#if [ -d /usr/local/bin ]; then
    #PATH="$PATH":/usr/local/bin
    #export PATH
#fi

# there is a "rogue" PWD env var that causes problems on Cygwin (corsair)
unset PWD

if [ -f $DEV_ROOT/.rbt.envs ]; then
    . $DEV_ROOT/.rbt.envs
    #export PATH="$PATH:$DEV_ROOT/tools/wls/infra"
fi

cd $DEV_ROOT/tools/wls
# this is required because we want to use stuff like RQSITE in queued commands
. ./infra/infraenv.sh
. ./infra/build/rbt.sh

rdequeuer "$@"
