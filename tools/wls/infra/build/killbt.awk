# killbt.awk
# Inputs (variables):
#    CMDS="cmd1|cmd2|cmd3|..."  all the commands you want to delete
#    PPID=1234  the pid (with all its parents) that you want to protect
#    stdin, the output of ps -opid,ppid,comm -u LOGNAME
# Output:
#    stderr, various debugging messages
#    stdout, the pids to kill, one per line
#

# on the header line of ps, break out the CMDS to be killed
FNR == 1 {
  # take the passed in CMDS variable and split it based on |
  split(CMDS, tmp, /\|/)
  # put it in an associative array so I can use "if (a in cmds)"
  for (i in tmp) {
    # make the comparision case insenstive by lowercasing everything
    # this is especially important in matching Windows pathnames,
    # DOS "~" shortnames can be various cases compared to the ps output
    tmp[i] = tolower(tmp[i]);
    cmds[tmp[i]] = i;
  }
  # delete tmp array
  for (i in tmp) del tmp[i];

  # format used to send messages to stderr
  FMT = "%-10s %6d %6d %s\n";

  readingcygwin = 0;

  # skip the header line of ps output
  next;
}

# at the very end of the output on Cygwin there will be a table of pid,
# ppid, winpid, cmds.  This information, if read in, will be added to the
# previously read in information to try to "fixup" the process tree on
# cygwin.  See the END for more details.
/^CYGWINTOWINPIDS$/ { readingcygwin = 1; next; }

readingcygwin == 1 {
  pid = $1;
  ppid = $2;
  winpid = $3;
  $1 = "";
  $2 = "";
  $3 = "";
  cmd = substr($0, 3);
  cpid_ppid[pid] = ppid;
  cpid_cmd[pid] = cmd;
  cpid_winpid[pid] = winpid;
  winpid_cpid[winpid] = pid;
}

{
  # parse output of ps
  pid = $1;
  ppid = $2;
  $1 = "";
  $2 = "";
  cmd = substr($0, 3);
  # make the comparision case insenstive by lowercasing everything
  # this is especially important in matching Windows pathnames,
  # DOS "~" shortnames can be various cases compared to the ps output
  cmd = tolower(cmd);

  # This records of all the pids and their parents that will be used along
  # with the passed in PPID to protect the PPID and it's parents.
  pid_ppid[pid] = ppid;
  pid_cmd[pid] = cmd;
  
  # also record the children pids, this is used to kill all the children
  # of a pid"-p pid
  if (ppid in pid_children) {
    pid_children[ppid] = pid_children[ppid] " " pid;
  } else {
    pid_children[ppid] = pid;
  }
}

function select_cmds() {
  # ok, so now I've recorded all the pids I do *not* want to kill, now
  # look through all the pids, skipping the protected pids and recording
  # the ones (in kill_pids) that match the commands I want to kill (from CMD).
  # Also record the pids that don't fit into any category in ignored_pids
  for (i in pid_ppid) {
    if (i in protected_pids) {
      continue;
    }
    if (pid_ppid[i] == PPID) {
      ignored_pids[i] = -1;
      continue;
    }
    # first check for the exact command name in the cmds array
    if (pid_cmd[i] in cmds) {
      kill_pids[i] = -1;
    } else {
      # if the first check fails then...
      for (j in cmds) {
	# try the regular expression "/cmd$" because sometimes on UNIX
	# the full path
	if (pid_cmd[i] ~ "/" j "$") {
	  kill_pids[i] = -1;
	  break;
	}
	# if cmd contains a / then try looking for the "cmd" anywhere in
	# the ps -ocomm output (ps command arguments have a max which
	# truncates the end of the command.  /foo/bar/java/130/bin/java
	# will look like /foo/var/java/130/bin/ja.  Specifying / in the
	# search cmd's allows us to match on strings like "/java/"
	# or "jre/bin".  On the other hand looking for "sh" *anywhere*
	# on a command line would pickup too much)
	if (j ~ "/" || j ~ "\\\\") {
	  if (pid_cmd[i] ~ j || index(pid_cmd[i], j) != 0) {
	    kill_pids[i] = -1;
	    break;
	  }
	}
      }
      if (! (i in kill_pids)) {
	ignored_pids[i] = -1;
      }
    }
  }
}

# note: "a" is not a parameter but a "local" variable, this is needed
# because select_children is recursive
function select_children(KILLPID, a) {
  if (KILLPID == "") {
    return;
  }
  kill_pids[KILLPID] = -1;
  split(pid_children[KILLPID], a, / /);
  for (i in a) {
    select_children(a[i]);
  }
}

END {
  # *if* cygwin data exists then use it to augment the pid data.
  for (i in cpid_ppid) {
    # if the cpid doesn't exist in the pid array then insert it
    if (! (i in pid_ppid)) {
      pid_ppid[i] = cpid_ppid[i];
      pid_cmd[i] = cpid_cmd[i];
    }
  }

  i = PPID;
  # initially there are no protected_pids, walk up the pid tree starting
  # at the PPID saving all the pids to protect (PPID and all it's parents).
  while (i != "") {
      # break if the pid is already in the protected_pids *and* if we
      # already added it to the protected pids more than twice.  We
      # allow adding more than once because I think there sometimes
      # overlap between the cygwin and Windows pids and parts of the
      # tree of protected pids can be missed if we stop adding pids
      # and parents of pids the first time we hit a protected pid.   
      if ((i in protected_pids) && (protected_pids[i] > 2)) {
	break;
      }
      protected_pids[i]++;
      # if running cygwin and the pid has a corresponding cygwin pid then
      # make sure the cygwin pid is also protected
      if (i in winpid_cpid) {
	protected_pids[winpid_cpid[i]]++;
      }
      # if running cygwin and the pid has a corresponding windows pid then
      # make sure the windows pid is also protected
      if (i in cpid_winpid) {
	protected_pids[cpid_winpid[i]]++;
      }
      i = pid_ppid[i];
  }
  
  # select pids to kill based on the commands or the pid and children of a pid
  if (KILLPID == "") {
    select_cmds();
  } else {
    print "KILLPID=" KILLPID | "cat 1>&2";
    select_children(KILLPID);
  }

  if (0 in pid_ppid || "0" in pid_ppid || 0 in kill_pids || "0" in kill_pids) {
    protected_pids[0] = -1;
    protected_pids["0"] = -1;
  }
  print "PPID=" PPID | "cat 1>&2";
  for (i in ignored_pids) {
    printf FMT, "Ignoring", i, pid_ppid[i], pid_cmd[i] | "cat 1>&2";
  }
  for (i in protected_pids) {
    printf FMT, "Protecting", i, pid_ppid[i], pid_cmd[i] | "cat 1>&2";
  }
  for (i in kill_pids) {
    # sanity check
    if (i in protected_pids) {
      printf FMT, "WARNING: found kill pid in protected pids, skipping", i, pid_ppid[i], pid_cmd[i] | "cat 1>&2";
    } else {
      print i;
      printf FMT, "Killing", i, pid_ppid[i], pid_cmd[i] | "cat 1>&2";
    }
  }
}
