# Copyright 2006 BEA Systems, Inc.
##########################################################################
#
# Simple script to hold all of the log file names and set a value for all 
# of them.
#
##########################################################################
# make sure new versions of bash >= 3.2.9-11 on Cygwin ignore carriage returns
set -o | grep igncr >/dev/null && set -o igncr # comment required

get_timestamp() {
    # note timestamp is UTC/GMT which helps make it sane with
    # multi-timezone test runs all publishing to the same file server
    TIMESTAMP=`date -u +%Y%m%d-%H%M%S`
    export TIMESTAMP
}

create_logdir() {
    : ${1:?} ${TESTKEY:?} ${TESTTORUN:-${TESTSCRIPT:?}} ${TIMESTAMP:?need.to.call.get_timestamp}
    tmp="$1/qa${TESTTORUN:-$TESTSCRIPT}/$TESTKEY/$TIMESTAMP"
    # only mkdir if it it doesn't start with http: and it doesn't exist
    echo "$tmp" | grep "^http:" >/dev/null 2>&1
    http=$?
    if [ $http -ne 0 -a ! -d "$tmp" ]; then
  mkdir -p "$tmp"
    fi
    echo "INFO: create_logdir $tmp" 1>&2
    echo "$tmp"
    # this should actually be 1 *more* than number you want to keep
    NUMKEEP=12
    # sort reverse and delete all dirs beyond NUMKEEP
    [ $http -ne 0 ] && (
  cd "$tmp/.." && \ls -r | tail -n +$NUMKEEP | xargs rm -rf
    )
    unset tmp http
}

remote_name() {
    : ${1:?filename} ${TESTKEY:?}
    echo "$1" | sed "s/^$TESTKEY/test/"
}

# Names of output files that we expect.
  LOG=${TESTKEY}.log
  OUT=${TESTKEY}.out
  OERRS=${TESTKEY}.oerrs
  BOUT=${TESTKEY}.bout
  SLOG=${TESTKEY}.slog
  DEBUG=${TESTKEY}.debug
  DEBUGA=${TESTKEY}.debug-all
  TDUMP=${TESTKEY}.td
  TESTZIP=${TESTKEY}.zip
  CORE=${TESTKEY}.core
  PIDS=${TESTKEY}.pids
  PNG=${TESTKEY}.png 
  BMP=${TESTKEY}.bmp
  JTE=${TESTKEY}.jte
  PERFRM=${TESTKEY}.perfrm
  ERRS=${TESTKEY}.errs
  BERRS=${TESTKEY}.berrs
  SERRS=${TESTKEY}.serrs
  TSTATS=${TESTKEY}.tstats
  # note: "a=b; export a" is a lot easier to do p4 resolves for.
  # Those intra-line merges are killing me - Alan
  DIFFHTML=${TESTKEY}_diff.html; export DIFFHTML
  TRENDHTML=${TESTKEY}_trend.html; export TRENDHTML
  PATTHTML=${TESTKEY}_pattern.html; export PATTHTML
  LAHTML=${TESTKEY}_logAnalyzerReport.html; export LAHTML
  HTML=${TESTKEY}.html
  CSS=${TESTKEY}.xml.css
  GTLF=${TESTKEY}.xml
  GTLFOLD=${TESTKEY}.gtlf.xml
  GTLFREPORT=${TESTKEY}.gtlf.xml.report
  TRC=${TESTKEY}.trc

  # TestLogic specific logs
  TESTLOGIC_INTERNAL_LOG=.${TESTKEY}-internal.log; export TESTLOGIC_INTERNAL_LOG
  TESTLOGIC_ERR_FILE=${TESTKEY}.err; export TESTLOGIC_ERR_FILE

  export LOG OUT OERRS BOUT PIDS ERRS SERRS BERRS TSTATS HTML CSS SLOG DEBUG DEBUGA TDUMP TESTZIP CORE PERFRM GTLF GTLFOLD GTLFREPORT TRC

# Note: wildcarded files listed here also have to be added to btstatus.sh
listoflogs="$TSTATS $LOG $OUT $OERRS $BOUT $ERRS $SERRS $BERRS $HTML $DIFFHTML $PATTHTML $PNG $BMP $LAHTML $TRENDHTML $CSS $SLOG $DEBUG $DEBUGA $TDUMP $TESTZIP $CORE $PERFRM ${TESTKEY}_*.zip ${TESTKEY}_*.out ${TESTKEY}_*.png ${TESTKEY}_*.bmp ${TESTKEY}_*.jte ${TESTKEY}_*.log ${TESTKEY}_*.debug ${TESTKEY}_*.td ${TESTKEY}_*.xml ${TESTKEY}_*.err ${TESTKEY}_*.dump ${TESTKEY}_*.mdmp ${TESTKEY}.cout $GTLF $GTLFOLD $GTLFREPORT $TRC $TESTLOGIC_INTERNAL_LOG $TESTLOGIC_ERR_FILE"
export listoflogs

# this list is only used by the status page to create the drop down list
listofexts="out bout log slog html debug debug-all td dump zip core errs perfm png bmp gtlf.xml gtlf.xml.report xml trc err"
export listofexts
