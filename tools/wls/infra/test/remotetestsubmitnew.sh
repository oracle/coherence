#!/bin/sh
#
# The ~static file remotetest.sh calls this more-specialized file.
#
: ${SRC:?} ${JOBID:?} ${SYNCTO:?}
cd `dirname $0`/../../../../
DEV_ROOT=`pwd`

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
mkdir -p ${TMPDIR:?}/$JOBID

echo $0 | grep new.sh >/dev/null
if [ $? -eq 0 ]; then
    NEW=new
else
    NEW=
fi
. $DEV_ROOT/tools/wls/infra/test/remotetestutils$NEW.sh

# JV="" isn't the same as unset for devenv.pl, in order to get the
# devenv default java version you need to unset JV.  So there is going to be
# *no* way in the remote tests to set JV="", you either have to put
# JV=jrockit or JV=server and JV="" or no JV setting is going to be unset JV
if [ "$JV" = "" ]; then
    unset JV
fi

# need this to get devenv to take parameters non-interactively
#params="-dl"
#cd bin
. ./bin/cfglocal.sh
#cd ../tools
#unset params

. tools/wls/infra/infraenv.sh
. tools/wls/infra/build/rbt.sh
# the zip is regenerated on the remote machine for modules builds
REMOTE_ZIP="$RQSITE/$SRC/remotetests/autosubmit/$JOBID.zip"
if [ ! -f "$REMOTE_ZIP" ]; then
    REMOTE_ZIP="$RQSITE/$SRC/remotetests/zips/$JOBID.zip"
fi

#Liu Bo, 2008/04/02
#comment out this part because files with multibyte filename in P4 system
#will cause other such as CC build fail, won't support submittion for such
#files for now
#JOBFILE="$RQSITE/$SRC/status/$JOBID"
#ENCODING=`head -2 $JOBFILE |awk '-FENCODING=' '/.*/{print $2}' |awk '-F ' '/.*/{print $1}' |sed '/^$/d'`
#export ENCODING=$ENCODING

safe_revert

apply_zip "$REMOTE_ZIP"
# remove file apply_zip creates
rm -f p4description.txt

auto_submit_real

safe_revert
