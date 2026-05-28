BEGIN {
    month["Jan"] = "01";
    month["Feb"] = "02";
    month["Mar"] = "03";
    month["Apr"] = "04";
    month["May"] = "05";
    month["Jun"] = "06";
    month["Jul"] = "07";
    month["Aug"] = "08";
    month["Sep"] = "09";
    month["Oct"] = "10";
    month["Nov"] = "11";
    month["Dec"] = "12";

    user = "bt";

    # break it out queued/ID or status/ID
    num = split(id, a, /\//);
    id = a[num];

    if (a[1] == "queued") {
      STATUS = "queued";
    }

    if (id ~ /^job\.[0-9]\.[^\.]*\.[^\.]*$/) {
      split(id, a, /\./);
      timestamp = a[3];
    } else if (id ~ /^job\.[0-9]\.[^\.]*$/) {
      split(id, a, /\./);
      timestamp = a[3];
    } else if (id ~ /^job\.[^\.]*\.[^\.]*$/) {
      split(id, a, /\./);
      timestamp = a[2];
    } else if (id ~ /^job\.[^\.]*$/) {
      split(id, a, /\./);
      timestamp = a[2];
    }
    if (timestamp != "") { 
      if (length(timestamp) == 14) {
	queued = \
	  substr(timestamp,  1, 4) "-" \
	  substr(timestamp,  5, 2) "-" \
	  substr(timestamp,  7, 2) " " \
	  substr(timestamp,  9, 2) ":" \
	  substr(timestamp, 11, 2) ":" \
	  substr(timestamp, 13, 2) " UTC";
      } else if (length(timestamp) == 12) {
	queued = \
	  substr(timestamp,  1, 4) "-" \
	  substr(timestamp,  5, 2) "-" \
	  substr(timestamp,  7, 2) " " \
	  substr(timestamp,  9, 2) ":" \
	  substr(timestamp, 11, 2) ":00 UTC";
      }
    }
    if (queued == "") {
      print "INFO: " id " has no timestamp ", timestamp, length(timestamp) | "cat 1>&2";
    }
}

function zdate(old) {
  cmd = "TZ=PST8PDT date --date=\"" old "\" \"+%F %T\"";
  cmd | getline new;
  return new;
}

FNR == 1 && $0 ~ /^#,/ {
  sub(/^#/, "");
  chars = $0;
  next;
}

FNR <= 2 && $0 !~ /^#,/ && $0 ~ /\/remotetest/ {
  if ($1 == "sh") {
    target = $3;
    user = $4;
  } else {
    target = $2;
    user = $3;
  }
  sub(/^"/, "", user);
  sub(/"$/, "", user);
  for (i = 4; i <= NF; i++) {
    split($i, a, /=/);
    if (a[1] == "JV")		jv = a[2];
    if (a[1] == "SUBMIT")	submit = a[2];
    if (a[1] == "SYNCTO")	syncto = a[2] + 0;
    if (a[1] == "INTEGFROM")	integfrom = a[2];
    if (a[1] == "SPIN")		spin = a[2];
    if (a[1] == "YIM")		yim = a[2];
    if (a[1] == "RTARGETS")	rtargets = a[2];
  }
  next;
}

FNR <= 2 && $0 !~ /^#,/ && $0 ~ /\/testrel.sh / {
  for (i = 4; i <= NF; i++) {
    split($i, a, /=/);
    if (a[1] == "CONFIG")	target = a[2];
  }
  next;
}

FNR <= 2 && $0 !~ /^#,/ && $0 ~ /\/abl2.sh / {
  target = "ABL2 " $4;
  next;
}

FNR <= 2 && $0 !~ /^#,/ && $0 ~ /\/ceinfraplugin.sh / {
  tmp = $0;
  sub(/^.*ceinfraplugin.sh /, "", tmp);
  sub(/ >.*$/, "", tmp);
  target = tmp;
  next;
}

FNR == 1 && $0 !~ /^#,/ && $0 ~ /\/not_in_p4.sh / {
  target = "p4clean";
  next;
}

FNR <= 2 && $0 !~ /^#,/ && $0 ~ /restart.sh/ {
  target = "machine restart";
  next;
}

$0 ~ /: starting\r?$/ {
  sub(/:/, "", $6);
  sub(/:/, "", $7);
  sub(/:/, "", $8);
  # handle old MKS versions where date -u doesn't include timezone UTC or GMT
  if ($7 == int($7)) {
    started = $7 "-" month[$3] "-" $4 " " $5 " " $6;
    machine = $8;
  } else {
    started = $6 "-" month[$3] "-" $4 " " $5;
    machine = $7;
  }
  completed = "";

  failure = "";
  next;
}

# just in case the "starting" line gets lost (started == "") use the first
# INFO line at the starting time.  This should be the exact same as the
# above starting section without the final "next" statement.
FNR > 2 && started == "" && $0 ~ /: INFO: / {
  sub(/:/, "", $6);
  sub(/:/, "", $7);
  sub(/:/, "", $8);
  # handle old MKS versions where date -u doesn't include timezone UTC or GMT
  if ($7 == int($7)) {
    started = $7 "-" month[$3] "-" $4 " " $5 " " $6;
    machine = $8;
  } else {
    started = $6 "-" month[$3] "-" $4 " " $5;
    machine = $7;
  }
  completed = "";

  failure = "";
}

failure == "" && $0 ~ /: failed$/ {
  failure = "unknown";
}

$0 ~ /FATAL: / {
  if (failure == "") {
    failure = "unknown";
  }
  if ($0 ~ / FATAL: p4clean / && $0 ~ /failed/) {
    failure = "build";
  }
  if ($0 ~ / FATAL: build / && $0 ~ / failed, /) {
    failure = "build";
  }
  if ($0 ~ / FATAL: modules build / && $0 ~ / failed/) {
    failure = "build";
  }
  if ($0 ~ / p4 / || $0 ~ / additional sync/ || $0 ~ /FATAL: invalid P4USER/) {
    failure = "p4";
  }
  if ($0 ~ / been killed/) {
    failure = "killed";
    sub(/:/, "", $6);
    sub(/:/, "", $7);
    # handle old MKS versions where date -u doesn't include timezone UTC or GMT
    if ($7 == int($7)) {
      completed = $7 "-" month[$3] "-" $4 " " $5 " " $6;
    } else {
      completed = $6 "-" month[$3] "-" $4 " " $5;
    }
    if (started == "") started = completed;
  }
  if ($0 ~ / FATAL: auto-submit / && $0 ~ /failed/) {
    failure = "submit";
  }
  next;
}

$0 ~ /WARNING: test / {
  failure = "test";
  next;
}

$0 ~ /info: job being returned / {
  failure = "";
  machine = "";
  started = "";
  completed = "";
  chng = "";
  chngarg = "";
  results = "";
  description = "";
  next;
}

$0 ~ /: (done|failed)\r?$/ {
  sub(/:/, "", $6);
  sub(/:/, "", $7);
  sub(/:/, "", $8);
  # handle old MKS versions where date -u doesn't include timezone UTC or GMT
  if ($7 == int($7)) {
    completed = $7 "-" month[$3] "-" $4 " " $5 " " $6;
    machine = $8;
  } else {
    completed = $6 "-" month[$3] "-" $4 " " $5;
    machine = $7;
  }
  next;
}

$0 ~ /\.\.\.@[0-9][0-9]/ {
  gsub(/\r/, "");
  for (i = 1; i <= NF; i++) {
    if ($i ~ /@/) {
      split($i, a, /@/);
      chng = a[2];
      next;
    }
  }
  next;
}

$0 ~ /: using (submitted|pending) change\(s\) [0-9]/ {
  gsub(/\r/, "");
  chngarg = $NF;
}

$0 ~ /Results directory/ {
  gsub(/\r/, "");
  num = split($0, a, /"/);
  # search for the ""'d href that has /remotetests/ in it
  for (i = 1; i <= num && a[i] !~ /\/remotetests\//; i++);
  full_results = a[i];
  sub(/^.*\/rqueue\/coherence/, RQSITE, full_results);
  if (full_results == a[i]) {
      sub(/^.*\/centralrq\/coherence/, RQSITE, full_results);
  }

  # strip off terminating / from path, if any
  sub(/\/$/, "", full_results);
  # grab the last part of the path as the results dir YYYYMMDD-MMHHSS
  num = split(full_results, b, /\//);
  results = b[num];
  next;
}

FNR <= 2 && $0 !~ /^#,/ {
  target = substr($0, 1, 50);
  next;
}

END {
  if (results != "") {
    # attempt to read p4description.txt
    desc_file = full_results "/p4description.txt";
    while ((status = getline desc < desc_file) == 1) {
      if (description != "") {
	description = description "\n" desc;
      } else {
	description = desc;
      }
    }
    # remove carriage returns from files created on MKS
    gsub(/\r/, "", description);
    # quote \'s so they don't ruin the MySQL syntax
    gsub(/\\/, "\\\\&", description);
    # quote ''s so they don't ruin the MySQL syntax
    gsub(/'/, "\\\\&", description);
  }

  print "replace delayed into rjob set id = '" id "', branch = '" BRANCH "',";
  if (user != "")	{ print "    user = '"		user "',"; }
  if (target != "")	{ print "    target = '"	target "',"; }
  if (queued != "")	{ print "    queued = '"	zdate(queued) "',"; }
  if (started != "")	{ print "    started = '"	zdate(started) "',"; }
  if (completed != "")	{ print "    completed = '"	zdate(completed) "',"; }
  if (submit != "")	{ print "    submit = '"	submit "',"; }
  if (jv != "")		{ print "    jv = '"		jv "',"; }
  if (syncto != "" &&
      syncto != 0)	{ print "    syncto = '"	syncto "',"; }	
  if (integfrom != "")	{ print "    integfrom = '"	integfrom "',"; }
  if (spin != "" )	{ print "    spin = '"		spin "',"; }
  if (yim != "")	{ print "    yim = '"		yim "',"; }
  if (rtargets != "")	{ print "    rtargets = '"      rtargets "',"; }
  if (chng != "")	{ print "    chng = '"		chng "',"; }
  if (chngarg != "")	{ print "    chngarg = '"	chngarg "',"; }
  if (results != "")	{ print "    results = '"	results "',"; }
  if (description != ""){ print "    description = '"	description "',"; }
  if (machine != "")	{ print "    machine = '"	machine "',"; }

  if (started == "" || STATUS == "queued") {
    print "    status = 'queued',";
  } else {
    if (completed == "") {
      print "    status = 'running',";
    } else {
      print "    status = 'done',";
    }
  }
  if (failure != "")	{ print "    failure = '" failure "',"; }
  print "    chars = '" chars "'";
  print ";";

}
