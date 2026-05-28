# . check_stop.sh [show]
# exports STOPTEST=YES/NO based on STOPFILE info YES means ok to 
# stop current test.
# if "show" is given, will echo results.
# This code has to live seperate from testrel_scheduler because
# Nightly_all.sh needs determine if tests are runnable while
# testrel_scheduler is running.  On W2K you cannot run a script 
# while it is running. ergo, seperate script.


env_setup() {
  # env_setup:   Run devenv and infraenv.
  # Mainly interested in grabbing the variable KITSITE
  infraCheck=`dirname $0 |grep infra/test`
  cd `dirname $0`
  cd ../../../../..
  . ./.rq.envs
  if [ "`uname`" = "Windows_NT" ]; then
      bin/cfgwindows
  else
      . ./bin/cfglocal.sh
  fi
  . ./tools/common/wls/infra/infraenv.sh
  cd tools/common/wls/infra/test
}

check_stop_time_stamp() {
  STOPTEST="YES"
  if [ -s STOPFILE ]
  then
    today=`date '+%j'`
    creationDay=`cat STOPFILE|grep creationDay |cut -f2 -d"="`
    stopLength=`cat STOPFILE|grep stopLength |cut -f2 -d"="`   
    resumeDate=`expr $creationDay + $stopLength`
    # Even if we meet the critera for the stop file we will not honor
    # it if there are no java processes running. (There is a risk of 
    # getting caught between tests, but it is low)
    javaRunning=`ps -ef |grep java |grep -v grep`
    if [ "${resumeDate}" -gt 365 ]
    then      
      resumeDate=`expr ${resumeDate} - 365`; # Going to get nailed on leap year
    fi
    dayDiff=`expr ${resumeDate} - ${today}`
    # If my dayDiff is greater than stopLength then more than likely
    # we are crossing over from one year to another so Dont set STOPTEST
    # equal to NO
    if [ "${dayDiff}" -le "${stopLength}" -a "${dayDiff}" -gt 0 ]
    then
      if [ -n "${javaRunning}" ]
      then
        echo Test Stop Directive is NO.
        STOPTEST="NO"
      else
        echo Stop directive overridden because there are no java processes running
      fi        
    fi
  fi
  export STOPTEST
 }

## MAIN PROGRAM ## 
check_stop_time_stamp
if [ "$1" = "show" ]
  then
    env_setup
    check_stop_time_stamp
    echo STOPTEST=$STOPTEST
fi
