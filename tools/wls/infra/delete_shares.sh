#!/bin/sh
# make sure new versions of bash >= 3.2.9-11 on Cygwin ignore carriage returns
set -o | grep igncr >/dev/null && set -o igncr # comment required here

#
# Delete writable shares on the current system.  Writable shares on Windows
# is a serious security hole and we've had to cleanup numerous virus laden
# systems because people working on the systems create them.  Read-only and
# the default admin shares will be left alone.
#
# This script does *not* depend on devenv.sh or infraenv.sh
# environment variables
#
# Add -f as an argument to delete shares *even* if not running as "bt"
# or "release"
#
# Usage:
#   sh ./delete_shares.sh
#   sh ./delete_shares.sh -f
#

if [ "`uname`" = "Windows_NT" ]; then
  if [ "$USERNAME" = "bt" -o "$USERNAME" = "release" -o "$1" = "-f" ]; then
    # FIXME: herrlich@bea.com Feb 13, 2003
    # names with spaces or very long names will confuse awk
    net view \\\\127.0.0.1 |\
      awk '
/^Shared resource/{next}
/^Share name/{next}
/^----------/{next}
/^The command completed successfully/{next}
/^There are no entries in the list/{next}
/^$/{next}
{print $1}' |\
	while read i; do
	  touch //127.0.0.1/$i/delete_shares.testing >/dev/null 2>&1
	  if [ $? -eq 0 ]; then
	    echo "INFO: deleting writable Windows share '$i'"
	    rm -f //127.0.0.1/$i/delete_shares.testing
	    echo y | net share /delete $i
	  fi
	done
  fi
fi
