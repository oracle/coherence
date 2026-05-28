# machine table
# loaded from the available directory

{
  gsub(/\r/, "");
  if ($1 == "SRC") {
    branch_count++;
    branches[$2] = branch_count;
  } else {
    data[$1] = $2;
  }
}

END {

  settings = "";

  if ("OS"  in data)			{ settings = settings "    shortos = '" data["OS"] "',\n"; }
  if ("CPU" in data)			{ settings = settings "    cpu = '" data["CPU"] "',\n"; }
  if ("SITE" in data)			{ settings = settings "    site = '" data["SITE"] "',\n"; }
  if ("RBT_EXCLUDED" in data && data["RBT_EXCLUDED"] == "true" )
    					{ settings = settings "    rbt_excluded = '" data["RBT_EXCLUDED"] "',\n"; }
  else					{ settings = settings "    rbt_excluded = NULL,\n"; }
  if ("PLATFORM" in data && data["PLATFORM"] == "ok" )
    					{ settings = settings "    platform = '" data["PLATFORM"] "',\n"; }
  else					{ settings = settings "    platform = NULL,\n"; }
  if ("RBT_NIGHTLY_EXCLUDED" in data && data["RBT_NIGHTLY_EXCLUDED"] == "true")
					{ settings = settings "    rbt_nightly_excluded = '" data["RBT_NIGHTLY_EXCLUDED"] "',\n"; }
  else					{ settings = settings "    rbt_nightly_excluded = NULL,\n"; }
  if ("RBT_RQ_EXCLUDED" in data && data["RBT_RQ_EXCLUDED"] == "true" )
    					{ settings = settings "    rbt_rq_excluded = '" data["RBT_RQ_EXCLUDED"] "',\n"; }
  else					{ settings = settings "    rbt_rq_excluded = NULL,\n"; }

  print "insert ignore into machine set";
  print settings;
  print "    machine = '" MACHINE "';";

  sub(/,\n$/, "", settings);

  print "update machine set";
  print settings;
  print "    where machine = '" MACHINE "';";

  if (branch_count > 0) {
    print "create temporary table if not exists mstatus_tmp (";
    print "    machine varchar(20) not null,";
    print "    branch varchar(40) not null,";
    print "    updated datetime,";
    print "    changed datetime,";
    print "    status enum('busy', 'available') not null);";
    for (branch in branches) {
      print "insert into mstatus_tmp (machine, branch, updated, changed, status) ";
      print "    select machine, '" branch "', updated, changed, status from mstatus";
      print "        where machine = '" MACHINE "' and status = 'available';";
    }
    print "replace into mstatus (machine, branch, updated, changed, status) ";
    print "    select machine, branch, updated, changed, status from mstatus_tmp;";
  }
}
