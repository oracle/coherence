# mstatus table
#
# machine
# branch
# target
# id (job) maybe null
# status (busy, available)
# updated (time of last update)
# changed (time of last status change, i.e. time idle or busy)

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
}

function zdate(old) {
  cmd = "TZ=PST8PDT date --date=\"" old "\" \"+%F %T\"";
  cmd | getline new;
  return new;
}

function set_status(new_status) {
  gsub(/\r/, "");
  sub(/:/, "", $7);
  sub(/:/, "", $6);
  if ($7 == int($7)) {
    updated = $7 "-" month[$3] "-" $4 " " $5 " " $6;
  } else {
    updated = $6 "-" month[$3] "-" $4 " " $5;
  }
  if (status != new_status) {
    if ($7 == int($7)) {
      changed = $7 "-" month[$3] "-" $4 " " $5 " " $6;
    } else {
      changed = $6 "-" month[$3] "-" $4 " " $5;
    }
    status = new_status;
  }
}

function busy() {
  set_status("busy");
}

function avail() {
  target = "";
  id = "";
  set_status("available");
}

/^\# .*: available/ {
  avail();
}

/^\# .*: busy\r?$/ {
  busy();
}

/^\# .*: job\..*: starting/ {
  split($0, a, /: /);
  id = a[2];
  busy();
}

/^\# .*: job\..*: done/ {
  avail();
}

/^\# .*: INFO: testrel starting / {
  gsub(/\r/, "");
  if (target == "") {
    target = $NF;
  } else {
    id = "";
    target = $NF;
  }
  busy();
}

/^\# .*: INFO: testrel done / {
  avail();
}

/^\# .*: INFO: infraplugin starting / {
  gsub(/\r/, "");
  if (target == "") {
    target = $NF;
  } else {
    id = "";
    target = $NF;
  }
  busy();
}

/^\# .*: INFO: infraplugin done / {
  avail();
}

END {
  print "replace into mstatus set machine = '" MACHINE "',";
  if (target != "")		{ print "    target = '"	target "',"; }
  if (id != "")			{ print "    id = '"		id "',"; }
  if (status != "")		{ print "    status = '"	status "',"; }
  if (updated != "")		{ print "    updated = '"	zdate(updated) "',"; }
  if (changed != "")		{ print "    changed = '"	zdate(changed) "',"; }
  print "    branch = '" BRANCH "';";
  print "delete from mstatus where machine = '" MACHINE "' and branch != '" BRANCH "';";
}
