BEGIN {
  WLH = normalize_file(WLH);
  exit_status = 0;
}

# first the output of p4info
/^-- P4INFO --$/	{processing = "P4INFO"; next }

# then the output of p4 have ...
/^-- P4HAVE --$/	{processing = "P4HAVE"; next }

# then the output of p4 opened ...
/^-- P4OPENED --$/	{processing = "P4OPENED"; next }

# finally the output of find fullpath -type f (no relative paths!)
/^-- CLIENTFILES --$/	{processing = "FILES"; next }

# finally the output of find fullpath -type d (no relative paths!)
/^-- CLIENTDIRS --$/	{processing = "DIRS"; next }

# take files with lowercase drive letters and uppercase them
function normalize_file (the_file) {
  # remove carriage returns from filename, if any
  sub(/\r$/, "", the_file);
  # flip \'s to /'s
  gsub(/\\/, "/", the_file);
  # change multiple //'s to /'s
  gsub(/\/\/*/, "/", the_file);
  # uppercase any drive letter
  if (the_file ~ /^[a-z]\:/) {
    the_file = toupper(substr(the_file,1,1)) substr(the_file,2);
  }
  # convert Cygwin WEBLOGICHOME to "standard" WEBLOGICHOME
  if (CWLH != WLH) {
    sub("^" CWLH, WLH, the_file);
  }
#  print "INFO: file:" the_file, "clientroot:" client_root, "WLH:" WLH | "cat 1>&2";
  return the_file;
}

# "the_dir" should be a directory, *NOT* a file path
function add_dir(the_dir) {
  count = split(the_dir, parts, "/");
  if (count > 0) {
    a_dir = parts[1];
    delete leafdirs[a_dir];
  }
  for (i = 2; i < count; i++) {
    a_dir = a_dir "/" parts[i];
    delete leafdirs[a_dir];
  }
  a_dir = a_dir "/" parts[count];
  if (! (a_dir in leafdirs)) {
#    print "INFO: adding dir " a_dir | "cat 1>&2";
    leafdirs[a_dir] = 1;
  }
}

# "the_file" should be a filename, *NOT* just a directory name
function delete_dir(the_file) {
  count = split(the_file, parts, "/");
  if (count > 0) {
    a_dir = parts[1];
    if (a_dir in leafdirs) {
#      print "INFO: deleting dir " a_dir | "cat 1>&2";
      delete leafdirs[a_dir];
    }
  }
  for (i = 2; i < count; i++) {
    a_dir = a_dir "/" parts[i];
    if (a_dir in leafdirs) {
#      print "INFO: deleting dir " a_dir | "cat 1>&2";
      delete leafdirs[a_dir];
    }
  }
}

processing == "P4INFO" && /^Client root: / {
  sub(/^Client root: /, "");
  # this needs to be done because it is going to be used as a
  # regular expression
  gsub(/\\/, "\\\\\\");
  client_root = normalize_file($0);
  next;
}

processing == "P4INFO" && /^Client name: / {
  sub(/^Client name: /, "");
  client_name = $0;
  if (client_name ~ /^cc\.config\./) {
    print "ERROR: you can not p4clean a cc.config.* client (" client_name ")!" | "cat 1>&2";
    print "ERROR: This is almost always CruiseControl config problem." | "cat 1>&2";
    exit 1;
  }
  next;
}

processing == "P4INFO" && /^User name: / {
  username = $NF;
  if (username == "bt" ||
      username == "build" ||
      username == "jrbt" ||
      username == "release" ||
      username == "root" ||
      username == "sjbuild" ||
      username == "wlesqa") {
    automation_user = "true";
  } else {
    automation_user = "false";
  }
  next;
}

processing == "P4OPENED" {
  # this is the output of p4 opened passed through p4 where
  # lines are in the form
  # "//depot/dir/dir/file //client/dir/dir/file d:/dir/dir/file"
  # the files *might* have one or more spaces in them
  count = split($0, a, / /); # must use /'s rather than " " or multiple
                             # spaces will get compressed to one. need to
                             # handle files with multiple consecutive spaces

  # count must be divisable by three, it will be three if the files
  # have no spaces
  if (int(count/3) * 3 != count) {
    print "INTERNAL ERROR: P4OPENED output did not split into three pieces!";
    exit_status = 2;
    exit;
  }

  # so this is kinda weird, this puts back together files with spaces
  # if count = 3 just grab a[3]
  # if count = 6 grab a[5] " " a[6]
  # if count = 9 grab a[7] " " a[8] " " a[9]
  # etc.
  p4_file = "";
  for (i = count/3; i > 0; i--) {
    if (p4_file == "") {
      p4_file = a[count - i + 1];
    } else {
      p4_file = p4_file " " a[count-i+1];
    }
  }
  # free up the array used to split
  for (i in a) delete a[i];

  # flip all the \'s (for NT path's)
  gsub(/\\/, "/", p4_file);
  p4_file = normalize_file(p4_file);

  delete_dir(p4_file);

  # delete open files from both p4 list and client list, just totally
  # ignore these files (like they don't exist at all)
  if (p4_file in p4_files) {
    delete p4_files[p4_file];
    p4_count--;
  }
  if (p4_file in client_files) {
    delete client_files[p4_file];
    client_count--;
  }
  next;
}

processing == "P4HAVE" {
  # lines are in the form "//depot/dir/dir/file#123 - clientroot\dir\dir\file"
  # got to use the localpath rather than the depot syntax because who
  # knows what file translation goes on in the client view
  split($0, a, "#");
  p4_file = a[2];
  # delete the array I split into...
  for (i in a) delete a[i];

  # strip revision number of depot file to be just left with full local path
  sub_count = sub("^[0-9]+ - ", "", p4_file);

  # sanity check, this substitute *must* substitute
  if (sub_count != 1) {
    print;
    print "INTERNAL ERROR: Removing revision number regexp did not work!" | "cat 1>&2";
    exit_status = 2;
    exit;
  }

  # flip all the \'s (for NT path's)
  gsub(/\\/, "/", p4_file);
  p4_file = normalize_file(p4_file);
  delete_dir(p4_file);
  p4_files[p4_file] = 0;
  p4_count++;
  next;
}

processing == "FILES" {
  client_files[normalize_file($0)] = 0;
  client_count++;
  next;
}

processing == "DIRS" {
  add_dir(normalize_file($0));
  next;
}

END {
  if (p4_count <= 0) {
    print "ERROR: Are you sure you are in a p4 client?" | "cat 1>&2";
    exit 1;
  }

  if (client_count <= 0) {
    print "ERROR: Don't see any files in the client (try again)?" | "cat 1>&2";
    exit 1;
  }

#  print "INFO:" p4_count     " files in p4"     | "cat >&2";
#  print "INFO:" client_count " files in client" | "cat >&2";

  # ignore $P4CONFIG, if set.
  if (p4config != "") {
      use_p4config = p4config;
  } else {
      use_p4config = "DO_NOT_MATCH";
  }
# print "INFO: use_p4config: ", use_p4config | "cat>&2";

  not_in_p4_count = 0;

# OK, print out all the client files not in p4
  for (i in client_files) {
    if (! (i in p4_files)) {
# ignore all infra/test directories because more often
# than not there are interesting files that we want to keep there during
# monkey, release build or test runs
# FIXME: herrlich@bea.com Dec 31, 2001
# also ignoring .out files under qa/tests until I can figure out how to move
# it so tools.p4clean doesn't rm it
# Ignoring ymsg.class so IM gets sent on success of the build
      if (i ~ /\.rbt\.envs/ ||
	  i ~ /\/openssl\// ||
          i ~ /infra\/test\// ||
	  i ~ /infra\/build\/.*\.class$/ ||
	  i ~ /\/mydevenv\.sh$/ ||
	  i ~ /\/mydevenv\.cmd$/ ||
# be really careful about only ignoring dev/??/build.properties
# otherwise this leaves around some build.prperites in eclipse plugin dirs
# use /dev/?/build.properties so that I don't have to hardcode the branch or
# pass it in to this awk script
	  i ~ /\/dev\/[^\/]*\/build\.properties$/ ||
	  i ~ /\/buildenv\.properties$/ ||
	  i ~ /\/implicitenv\.properties$/ ||
	  i ~ /\/dev\/[^\/]*\/external\/jdk.*/ ||
	  i ~ /\/dev\/[^\/]*\/external\/jrockit.*/ ||
	  i ~ /\/dev\/[^\/]*\/build\/installers/ ||
	  i ~ /\/dev\/[^\/]*\/build\/modules/ ||
# the vmm client jars are copied to repository/modules
	  i ~ /\/dev\/[^\/]*\/repository\/modules\/com.oracle.vmm.client.*/ ||
	  i ~ /\/dev\/[^\/]*\/build\/internal/ ||
	  i ~ use_p4config ||
	  i ~ /\/qa\/tests\/[^\/]*\.out$/) {
	if ( syncf != "false" ) {
	    print "ignoring " i | "cat >&2";
	}
      } else if (automation_user == "false" &&
		 # these directories/files we only ignore for regular (non-automation) users
		 (i ~ /\/\.settings\// ||
		  i ~ /\/user-drt\.properties$/ ||
		  i ~ /\.classpath$/ ||
		  i ~ /\.project$/)) {
	if ( syncf != "false" ) {
	    print "ignoring " i | "cat >&2";
	}
      } else if (i ~ /\/build\/jdk.*/ ||
		 i ~ /\/build\/x86_64\/jdk.*/ ||
		 i ~ /\/build\/jrockit.*/ ||
		 i ~ /\/build\/x86_64\/jrockit.*/) {
	continue;
      } else {
	  # if syncf is false, then don't print out files that aren't in p4
	  if ( syncf != "false" ) {
	      # quote files with spaces and single quotes in them for xargs
	      if (i ~ /[ ']/) {
	          print "\"" i "\"";
	      } else {
	          print i;
	      }
	      not_in_p4_count++;
	  }
      }
    }
  }

  if (not_in_p4_count == 1) {
    print "INFO: found 1 non-p4 file in client." | "cat >&2";
  } else if (not_in_p4_count > 1) {
    print "INFO: found " not_in_p4_count " non-p4 files in client." | "cat >&2";
  }

  # close to flush out cat output before (potential) sync -f's below
  close("cat >&2");
  sync_count=0;

  # it's a warning is there are p4 files not in the client
  # do this after the files above so that the warning is printed at the end
  # and is more likely to be seen by users
  for (i in p4_files) {
    if (! (i in client_files)) {
      if ( syncf == "false" ) {
          print "ERROR:", i, "in p4 but not on client" | "cat 1>&2";
      } else {
          print "WARNING:", i, "in p4 but not on client, sync -f'ing it" | "cat >&2";
      }
      close("cat >&2");
      sync_count++ ;
      syncfile = i;
      # four special characters that can be in p4 filespecs that cause problems if passed directly to p4 sync
      # http://www.perforce.com/perforce/doc.current/manuals/cmdref/filespecs.html
      gsub("\\%", "%25", syncfile);
      gsub("\\@", "%40", syncfile);
      gsub("\\#", "%23", syncfile);
      gsub("\\*", "%2A", syncfile);
      print syncfile "#have" >> sync_files;
    }
  }
  close(sync_files);
  if (sync_count > 0) {
    if ( syncf == "false" ) {
       print "ERROR: client was missing p4 controlled files!!!" | "cat 1>&2";
       exit_status = 1;
       exit 1;
    } else {
       system("p4 -x " sync_files " sync -f 1>&2");
    }
  }
  for (i in leafdirs) {
    if (i ~ /[ ']/) {
      print "\"" i "\"" >> rmdirs;
    } else {
      print i >> rmdirs;
    }
  }
  close(rmdirs);
}
