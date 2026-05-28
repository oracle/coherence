#!/usr/bin/perl

# -------------------------------------------------------------------------
# Functions:
#
# 1.  (tdata) Extract test definitions from the *props.env file; produce the
# TESTDATA string (for consumption by remotetestrun.sh);
#
#     or 
#
# 2.  (syncto, infomsg, contact)  Extract one property's value for one test.
#
# 3.  (cdata) Extract a clean target for the given test from the *props.env file.
#
#     or
#
# 4.  (doc) Process the *props.env file to produce the doc page called
# remote.html.  Note: the tdata function and props.env file are used
# in both of the above.
#
#     or 
# 
# 5.  (doc-cc) Create an extended doc page that matches test
# definitions from the remote queue with their equals in
# Cruisecontrol.
#
# -------------------------------------------------------------------------
# Examples:
#
# Example of transforming build & test data for one remote test:
#
#     Read the following from the *props.env file:
#
# # remote.ide
# remote.ide.syncto=`$p4ro counter last_clean_test_platform_$SRC`
# remote.ide.build=minprod_wls
# remote.ide.infomsg="last_clean_test_platform_$SRC is change $SYNCTO"
# remote.ide.test="UNKNOWN remoteDRT_wlp_eclipse remoteDRT_workshop_ide"
# remote.ide.contact=xxx@xxx.com

#     Write this TESTDATA string:
#
# minprod_wls UNKNOWN wlp/tools/eclipse/test:drt UNKNOWN :workshop:min-drt
#
# -------------------------------------------------------------------------
# Todo and Gotchas:
#

# 1.  This code will almost certainly break a "mixed" test, i.e. one
# that contains both a compound test (test that contains other tests)
# and a singleton test (one that does not contain other tests, only a
# colon-delimited test definition).

 
use File::Basename qw/ basename /;
use Env;

$test_doc_file = "remotetestprops.txt";
$cc_p4_props_file = "cc_p4_props.tmp";
$cc_p4_props_pretty_file = "cc_p4_props_pretty.tmp";

$pname = basename $0;
$usage = "usage:  $pname  <tdata|tdataraw|recursive_tdataraw|cdata|stages|syncto|infomsg|contact|list>  <remote test name>  <path to RQ test props file>  [debug]\nor
usage:  $pname  <doc|doc-cc>  <path to RQ test props file>  [debug]";

if (($ARGV[0] =~ /^(-h|-\?|-help|--help)$/) || ($ ARGV[0] eq "")) {
    print "$usage\n";
    exit(0);
}

$opr = $ARGV[0];

if ($opr !~ /^(tdata|tdataraw|recursive_tdataraw|cdata|stages|syncto|infomsg|contact|doc|doc-cc|list)$/) {
    print STDERR "Error 00:  invalid operation \"$opr\"\n";
    print STDERR "$usage\n";
    exit(1);
}

if ( $opr eq "doc" || $opr eq "doc-cc" || $opr eq "list" ) {
    if ($#ARGV == 1 || $#ARGV == 2) {
        
        $rq_test_props_file = $ARGV[1];
        $debug = $ARGV[2];    # pass in anything to turn on debugging

        # The following is a hack to make the tdata routine serve the
        # doc routine set this to any test.
        $top_test = "remote.full";
    }
    else {
        print STDERR "Error 01: bad args\n";
        print STDERR "$usage\n";
        exit(1);
    }
}
elsif ($#ARGV == 2 || $#ARGV == 3) {

    $top_test = $ARGV[1];
    $rq_test_props_file = $ARGV[2];
    $debug = $ARGV[3];    # pass in anything to turn on debugging
}
else {
    print STDERR "Error 03: bad args\n";
    print STDERR "$usage\n";
    exit(1);
}

# -------------------------------------------------------------------------
sub DPR {

    # debug print

    $debug && print "$pname debug:  ", @_, "\n";
}

# -------------------------------------------------------------------------
sub syncto {

    # retrieve the syncto string

    @tests = keys (%{ $HoF{syncto} });
    " @tests " =~ / $_[0] / || die "Error 04, unknown test:  $_[0], exiting" ;
    $out = "\"$HoF{syncto}{$_[0]}\"";
}

# -------------------------------------------------------------------------
sub cdata {

    # retrieve the clean target
    @tests = keys (%{ $HoF{clean} });
    " @tests " =~ / $_[0] / || die "Error 04, unknown test:  $_[0], exiting" ;
    $out = "$HoF{clean}{$_[0]}";

}

# -------------------------------------------------------------------------
sub stages {

    # retrieve the included test stages
    @tests = keys (%{ $HoF{stages} });
    " @tests " =~ / $_[0] / || die "Error 04, unknown test:  $_[0], @tests exiting" ;
    $out = "$HoF{stages}{$_[0]}";

}

# -------------------------------------------------------------------------
sub infomsg {

    # retrieve the infomsg string

    @tests = keys (%{ $HoF{infomsg} });
    " @tests " =~ / $_[0] / || die "Error 05, unknown test:  $_[0], exiting" ;
    $out = "\"$HoF{infomsg}{$_[0]}\"";
}

# -------------------------------------------------------------------------
sub contact {

    # retrieve the contact string

    @tests = keys (%{ $HoF{contact} });
    " @tests " =~ / $_[0] / || die "Error 05, unknown test:  $_[0], exiting" ;
    $out = "\"$HoF{contact}{$_[0]}\"";
}
# -------------------------------------------------------------------------
sub cleanup {

    # Clean up possible duplicate delimiters to avoid potential parsing
    # problems when remotestrun.sh operates on the output of this script
    # (aka TESTDATA).
        
    # todo someday:  figure out why retrieving anything from the hash
    # here adds a ^M to the end of the retrieved value on our RQ machines
    # with perl cygwin 5.8.6 but perl 5.6.0 on my mks laptop does not.
    # This happens only if/when you try to join the hash value to
    # another string (by any means:  '.', 'join', or just " ").
    $out =~ s/^ //;
    $out =~ s/ : / :/g;
    $out =~ s/::/:/g;
    $out =~ s/ +/ /g;
    $out =~ s/(UNKNOWN )+/UNKNOWN /g;

    # The docs for Perl 5.8.x (i.e. latest) say '\n' is the
    # all-platform endline symbol, but using only \n alone did NOT
    # work.  Maybe one or two of the following are redundant, but they
    # presumably covers all the cases.
    $out =~ s/\015//g;
    $out =~ s/\012//g;
    $out =~ s/\r//g;
    $out =~ s/\n//g;

}

$dbg_td_ctr = 0;

# -------------------------------------------------------------------------
sub tdata {

    # make the TESTDATA string
    # 
    # i.e. expand all targets to make a string like this:
    #
    #   buildtarget[,buildtarget]:testdir:testtarget buildtarget[,buildtarget]:testdir:testtarget
    #
    # or, abbreviated:
    #
    #   TESTDATA="bt1:td1:tt1 bt2a,bt2b:td2:tt2"
    #
    # and if there is only one build target for multiple test targets:
    #
    #   TESTDATA="bt1,bt2:td1:tt1 :td2:tt2 :td3:tt3 :td4:tt4 ..."
    #
    # and "UNKNOWN" can appear anywhere, space-delimited
    #
    #   TESTDATA="bt1,bt2:td1:tt1 UNKNOWN :td2:tt2 :td3:tt3 UNKNOWN :td4:tt4 ..."
    # 
    # The above amounts to a depth-first traversal, "sinking" to the leaf nodes of a tree.

    DPR "tdata:  $dbg_td_ctr start, \@_ = [@_]\n"; $dbg_td_ctr++;

    if ( ! @_ ) {
        #
        # terminate a sink
        #
        ! $top_test && print STDERR "Error 06:  $pname:  <remote test> is null\n";

        ! $HoF{build}{$top_test} && die "Error 07:  $pname:  <remote test>.build not found for remote test:  \"$top_test\"\n";

        # add the build target(s)
        
        # remove possible leading space to prevent perl from
        # translating it to a null field
        $out =~ s/^ //;
        my @tmp = (split /\s+/, $out);

        if ($tmp[0] =~ /UNKNOWN/) {
            $out = " $out";
        }

        $out = "$HoF{build}{$top_test}$out";
        return 0;
    }
    elsif ($_[0] =~ /:/) {
        #
        # Yes, first arg is a leaf node, i.e. a "dir:test" pair,
        # so add it to the output string.
        #
        $out = "$out :$_[0]";
        shift @_;
    }
    elsif ($_[0] =~ /UNKNOWN/) {
        # todo:  check on the one instance (so far) of two UNKNOWN's adjacent
        $out = "$out UNKNOWN ";
        shift @_;
        }
    elsif ($HoF{test}{$_[0]}) {
        #
        # No, first arg is not a leaf node so replace it with its expansion
        #
        # split on whitepace (not just one space)
        my @in_one = split /\s+/, $HoF{test}{$_[0]};

        # Assemble the output column called "Contains These Tests".
        # Don't print the first test name because it shows in the
        # far left column ("Ant Target").
        if ( $ck_list_started ) {
            if ( $HoF{test}{$_[0]} !~ /:/ ) {
                # this is a compound test, not singleton
                $ck_list .= "* <b>$_[0]</b> ";
            } else {
                # this is a singleton test (leaf node)
                $ck_list .= "$_[0] <br>";
        }
        } else {
            $ck_list_started = "yes";
        }
        # remove the remote test name from the to-be-processed list
        shift @_;

        # Add the results of expansion to the left side of the current
        # list to be processed.
        unshift @_, @in_one;
    }
    else {
        print STDERR "\nError 08, unknown test name, cannot expand it.\n";
        print STDERR "Exiting $pname\n";
        exit(1);
    }
    
    DPR "tdata:  $dbg_td_ctr end,  out =     [$out]\n"; $dbg_td_ctr++;

    tdata (@_);
}

sub recursive_tdataraw {
# based on tdata function, but we stop recursing before we get to the ":tests"
#
    DPR "recursive_tdataraw:  $dbg_td_ctr start, \@_ = [@_]\n"; $dbg_td_ctr++;
 
    if ( ! @_ ) {
        #
        # terminate a sink
        #
        ! $top_test && print STDERR "Error 06:  $pname:  <remote test> is null\n";

        ! $HoF{build}{$top_test} && die "Error 07:  $pname:  <remote test>.build not found for remote test:  \"$top_test\"\n";

        # remove possible leading space to prevent perl from
        # translating it to a null field
        $out =~ s/^ //;
        $out = "$out";
        my @tdata_raw = split ' ', $out;
        # stick the data into a hash so that it automatically uniq's the data
        my %data1;  #holds the "sub" jobs
        my %data2;  #holds other jobs
        my $i = 0;
        foreach (@tdata_raw) {
          #strip out UNKNOWN one more time
          s/UNKNOWN//g;
          s/^ //g;
            if ( /\.sub1/ ) {
               #stick the *.sub1 jobs in front of the array.  These jobs are the ones we need to target to the build machine
               # if multiple sub1 jobs are here, they will be later joined by a ',' and turned into a remote.multiple job on 
               # the dev build machine
               $data1{$_} = $i++ ;
             } else {
           # .sub* jobs that are not sub1 can end up here, and will be parallel.
               $data2{$_} = $i++ ;
             }
           }
       
        my $out1 = join(",", sort(keys(%{data1})));
        my $out2 = join(" ", sort(keys(%{data2})));
        #final output should show the *sub1 jobs first, then the rest.  Trying to target *sub1 jobs to the dev build machine
        $out = $out1 . " " . $out2;
        return 0;
    }
    elsif ( ($HoF{test}{$_[0]} =~ /:/) || ($_[0] =~ /\.sub/) ) {
        #
        # Yes, value of first arg is a leaf node, i.e. a "dir:test" pair,
        # so add it to the output string.
        # 
        # also stop here if the current item is a *.sub test.
        #
        $out = "$out $_[0]";
        shift @_;
    }
    elsif ($HoF{test}{$_[0]} ) {
        #
        # No, first arg is not a leaf node so replace it with its expansion
        #
        #
        # split on whitepace (not just one space)
        my @in_one = split /\s+/, $HoF{test}{$_[0]};

        # remove the remote test name from the to-be-processed list
        shift @_;

        # Add the results of expansion to the left side of the current
        # list to be processed.
        unshift @_, @in_one;
    } elsif ($_[0] =~ /UNKNOWN/) {
       shift @_;  
    } else {
        print STDERR "\nError 08, unknown test name (" . $_[0] . "), cannot expand it.\n";
        print STDERR "Exiting $pname\n";
        exit(1);
    }
   
    DPR "recursive_tdataraw:  $dbg_td_ctr end,  out =     [$out]\n"; $dbg_td_ctr++;

    recursive_tdataraw (@_); 
}


# -------------------------------------------------------------------------
sub doc_cc_hdrs {

    # Add headers for the additonal CC columns in the doc page remote.html

    $CC_hdr1 = "<th width=250>CC <br><br>Test Name</td></th>";
    $CC_hdr2 = "<th width=150>CC <br><br>Build Target</th>";
    $CC_hdr3 = "<th width=200>CC <br><br>Test Directory</th>";
    $CC_hdr4 = "<th width=150>CC <br><br>Test Target</th>";
    $CC_hdr5 = "<th width=150>CC <br><br>Host</th>";
}

# -------------------------------------------------------------------------
sub print_page_header {

    # html table header
    print <<ENDTEXT_01;
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
<head>
<title>Remote Testing Ant Targets</title>

<style type="text/css">

td {
    font-family: Verdana,Geneva,Arial,Helvetica,sans-serif;
    color:black;
    font-size:  8pt;
    font-weight: normal;
}
.right {
    margin-right:0.20in; 
    margin-bottom:0in;   
    text-align:right;    
    float:right;
}
.cctname {
  /* background:#fffafa; snow */
  background:#fff8dc;
}

</style>
</head>
ENDTEXT_01

}

# -------------------------------------------------------------------------
sub print_user_header {

    # html table header
    print <<ENDTEXT_02;

<body>

<h1>Remote Testing Ant Targets</h1>

<p> These targets create a remote job that will zip up all the p4 opened
files on your local machine, sync to the head, sync/resolve
your changes into the label, build what it needs and runs the requested test(s). Targets named with wls(eg, remote.standard.wls) will create a WLS RQ job to test the coherence bits build by your change after test in coherence RQ passed. You are sent email when the job starts and completes. </p>
<p>Then use the command "tools/wls/infra/enqueue" to enqueue a remote test. There are several options to enqueue that will modify its behavior.</p>
<p> An auto-submit job can be used to submit the changes if the build and test succeed. The -a option is used . Here are a few rules related to auto-submit jobs.</p>

<ul>
<li>
you must use the -c "your changelist"option to mention the local change list. Create a change using "p4 change" and specify the created change number to this option. 
<li>
you must include a comment in the change (add it when you run p4 change). You can also use the -d "your description" option but it's preferable to include it in the change definition (for example, integ'ing the change to another code line will use the description in the change). 
<li>
you must revert the change on your local client after the remote submit completes. 
</ul>

<p> NOTE: If you think a drt failure in the remote queue is machine
related, or something to do with the remote queue email the <a
href=mailto:infra-rq_cn_grp\@oracle.com> Platform Infrastructure Team</a> </p>

<p>See the end of this document for the details of what labels, build
and tests are used.  To have additional tests added send email to <a
href=mailto:infra-rq_cn_grp\@oracle.com> Platform Infrastructure Team</a>
supplying the details listed for the other remote tests.</p>

<p>The URL for machine and job queue...
<a href="remote.jsp">Remote Status</a></p>

<p>Shows the same thing as "remote.status" in html format.</p>

<p>Ant properties that modify what the remote targets do.  These
properties can be mixed and matched in most every combination.</p>

<pre>
    tools/wls/infra/enqueue       	     	<i># just zip up open files in your default change list</i>
    tools/wls/infra/enqueue -c none          	<i># ignore all opened files, the remote job will run with no changes</i>
    tools/wls/infra/enqueue -c 123456        	<i># just zip up open files in pending change 123456 on your client</i>

    tools/wls/infra/enqueue -s 123456        	<i># override sync'ing to the head, sync to this change number</i>
    tools/wls/infra/enqueue -s head          	<i># override sync'ing to the clean label, sync to the head</i>
    tools/wls/infra/enqueue -a               	<i># if the test(s) are successful submit the changes</i>
                              		     	<i># requires -c option</i>
    tools/wls/infra/enqueue -d "my big change"
                               		     	<i># Displayed on the remote quote status, you can use it
                                  	       	# to distinguish between multiple similar remote jobs.  If this is a
                              		 	# -a job then it is used for the p4 submit Description: and the
                               			# scripts will also grab the description from -c pending change
                               			# numbers if you supplied one.</i>

    tools/wls/infra/enqueue -m "user\@oracle.com"   <i> # send  email to mailto list (comma seperated list) when 
                                      		        # job starts or completes.  You must fully qualify the email 
                                      			# address, ie it must contain oracle.com. 
                                      			# The submitter will always get emailed.</i>
    tools/wls/infra/enqueue -p         		<i># always publish test logs, not just on test failures</i>
    tools/wls/infra/enqueue -f "functional test" remote.function     <i> # remote.function run user specified functional test with -f option.
                                                                     # functional test name is the subfolder name under prj/test/functional
    tools/wls/infra/enqueue -f "functional test" remote.distribution <i> # remote.distribution run user specified functional test with -f option.
                                                                     # functional test name is the subfolder name under prj/test/distribution
    tools/wls/infra/enqueue -S                  <i> # enable -Dtest.security.enabled=true in maven test command
    tools/wls/infra/enqueue -o Win              <i> # assign job to run on Windows machine, now we support Windows and Linux platform, default is Linux 
    tools/wls/infra/enqueue -w host_name               <i> # assign job to run specified host
    tools/wls/infra/enqueue -i branch_name -c 12345    <i> # integ change 12345 from specified branch to your current branch, 
                                                # only can integ between main branch and branches later than 12.2.1
</pre>
<p>Examples:</p>

<pre>
    tools/wls/infra/enqueue remote.full         <i># mvn clean integration-test</i>
    tools/wls/infra/enqueue -p remote.full  	<i># by default the remote queue only publishes files when a test fails, setting -p will publish all logs regardless of the drt status</i>

    tools/wls/infra/enqueue -s 123400 -c none remote.full
    tools/wls/infra/enqueue -s 123450 -c none remote.full
    tools/wls/infra/enqueue -s 123500 -c none remote.full
                               <i># Use remote tests to do a binary search for a test.full
			       # failure.  Set -c none to ignore any p4 opened
			       # files in the client.</i>
   tools/wls/infra/enqueue -f jvisualvm remote.function  <i># run jvisualvm functional test
   tools/wls/infra/enqueue -f examples remote.distribution <i># run examples functional test
   tools/wls/infra/enqueue -f examples -o Win remote.distribution <i># run examples functional test on Windows machine
   tools/wls/infra/enqueue -w slc05aud               <i> # assign job to run on slc05aud
   tools/wls/infra/enqueue -i main -c 12345    <i># integ submitted change 12345 from main branch to your current branch
</pre>

<h3>Details on how changes are sync/resolved</h3>

<p> First the files are sync'ed and opened to the exact file revision you
have opened in your p4 client.  The same file revisions shown in <i>p4
opened</i> command.  Next the entire client is sync'ed to the known clean
label (or -s "changelist you want to syncto") change number.  Finally three different resolve
commands are used to sync/resolve the changes.  Those commands are p4 resolve
-t -as (does binary one sided resolves), p4 resolve -am (merge most text
files), p4 resolve -dw -am (slow command but resolves various whitespace
diffs not already resolved by -am).  There is no way to to change these
sync/resolves except to sync/resolve on your own local p4 client before
submit. </p>

<a name=failureguide></a>
<h2>Test Failure Guide</h2>

<p>Your first indication of failure will be via email or from the
job.jsp status page.  If you do not immediately understand what is
going on your first stop should be the <b>Results Directory</b>.  This
is the <i>timestamped</i> directory (YYYYMMDD-HHMMSS). </p>

<p>If you had a build failure then look at build.log.  If you had a
test failure then check the ant output for that failed test.  The .log
is named based on the directory and ant target the remote test
infrastructure used to run the test.  If the ant output doesn't tell
you what's wrong then you'll have to find the detailed logs and output
of the test.  The tests aren't entirely consistent about where they
put their logs but the locations in the <b>Results Directory</b>
should be anologous to where they are if you run the test locally.
We try to capture enough so you can debug most
every problem but we can't capture everything because of disk space
limitations.  Sometimes you just have to look at all_files.html and
search for your test name in the list of files.  Remember the remote queue only publishes 
logs for a test when that test fails, this is done to speed up the cycle time 
of remote runs.  If you want the test logs published regardless of the test's status, pass -p on your remote run. </p>

<p> If you need a little more detail than job.jsp gives you see
test.log, this is kind of a verbose listing of the same basic
information. </p>

<p>If you think a test failure in the remote queue is machine related,
or something to do with the remote queue email the <a
href=mailto:infra-rq_cn_grp\@oracle.com> Platform Infrastructure Team</a></p>

ENDTEXT_02

}

# -------------------------------------------------------------------------

sub print_table_header {

    print <<ENDTEXT_03;

<table $table_width border=4 cellpadding=0 summary="Details of test runs">
    <caption><h2>Details of test runs</h2></caption>
    <tr>
        <th width=0 >Ant target</th>
        <th width=0 >Clean P4 Label/Counter</th>
        <th width=0 align=center>Dirs Sync\'d<br>under<br>//depot/dev/<br>&lt;branch&gt; </th>
        <th width=60 >Build Targets</th>
        <th width=0 >Test Directory</th>
        <th width=0 >Test Target</th>
        <th width=300 >Contains These Tests</th>
        <th width=350 >Contact Info <a name="contact"></a></th>
        $CC_hdr1
        $CC_hdr2
        $CC_hdr3
        $CC_hdr4
        $CC_hdr5
    </tr>
ENDTEXT_03

}

# -------------------------------------------------------------------------
sub print_table_trailer {

    # these misc notes show at the bottom of the table
    print "<tr><td colspan=8 ><br>

  <b>remote.status</b><br><br>&nbsp; &nbsp; &nbsp; Show the status of
  remote tests. <br><br>

</td></tr>";

}

# -------------------------------------------------------------------------
sub doc {

    # produce one html file listing all remote tests

    DPR "doc:  1 start, \@_ = [@_]\n";

    print_page_header;
    print_user_header if $opr ne "doc-cc";
    print_table_header;

    # Expanded testdata looks like this:
    #
    #    "minprod_wls UNKNOWN :workshop:min-drt :wlp/tools/eclipse:drt"
    #
    # Raw test names look like this:
    #
    #    remoteDRT_workshop_runtime
    #    remoteDRT_P13N
    #    remoteDRT_Lithium
    #
    foreach $rqtst ( sort keys ( %{ $HoF{build} } ) )  {

        DPR "doc:  2 rqtst = [$rqtst]\n";

        print "    <tr>", "\n";
        #
        # Ant Target (test name)
        #
        print "        <td>$rqtst\n        <a name=\"$rqtst\"/></td>\n";
        #
        # Clean P4 Label/Counter
        #
        print "        <td>$HoF{syncto}{$rqtst}</td>\n";
        #
        # Dirs Sync'd
        #
        my $dir_val;
        if ( $HoD{dirs}{$rqtst} ) {
            $dir_val = $HoD{dirs}{$rqtst};
        } else {
            $dir_val = '<b>...</b>';
        }
        print "        <td align=center>$dir_val</td>\n";
        #
        # Build Target(s)
        #
        (my $tmpbuild = $HoF{build}{$rqtst}) =~ s/,/<br>/g;
        if ( $HoF{clean}{$rqtst} ) {
            $tmpbuild = "$HoF{clean}{$rqtst}" . "<br> $tmpbuild";
        }
        print "        <td>$tmpbuild</td>\n";

        #
        # Test Directory, Test Target
        #
        $ck_list_started = "";
        $ck_list = "";

        # get the test data for all tests contained in this test
        tdata $rqtst;

        $out  =~ s/\s*UNKNOWN\s*/ /g;

        DPR "\nout=$out\n";

        my @t_defs = split ' ', $out;
        $out = "";

        # throw away the build target (i.e. all chars up to the first colon)
        #
        # 1. build targets followed immediately by a colon
        $t_defs[0] =~ s/^[^:]+://;
        # 2. build targets followed by a space (from ' UNKNOWN ')
        $t_defs[0] !~ /:/ && shift @t_defs;

        DPR "\n\$t_defs[0]=$t_defs[0]\n";

        my @t_dirs = "";
        my @t_targets = "";

        $cc_box1 = $cc_box2 = $cc_box3 = $cc_box4 = $cc_box5 = "";
        foreach (@t_defs) {
            s/^://;
            ($t_dir, $t_target) = split ':' ;
            push @t_dirs, "<nobr>$t_dir</nobr><br>";
            push @t_targets, "$t_target<br>";

            # make additional columns showing which CC box runs which RQ test
            if ( $opr eq "doc-cc" ) {

                # Hash of Cruisecontrol Test parms
                my %HoCT = (
                    1 => {
                        "dir" => "drt.dir",
                        "drt" => "drt.name",
                        "run" => "run.drt.name",
                    },
                    2 => {
                        "dir" => "drt2.dir",
                        "drt" => "drt2.name",
                        "run" => "run.drt2.name",
                    },
                    3 => {
                        "dir" => "drt3.dir",
                        "drt" => "drt3.name",
                        "run" => "run.drt3.name",
                    },
                );

                # compare   dir:testname   of RQ with CC and print if equal
                for my $host ( sort keys %HoC ) {

                    foreach $cc_tdef ( keys %HoCT ) {

                        # print "\n\nt_dir = $t_dir  HoCT{}{} = $HoCT{$cc_tdef}{dir} \n\n ";

                        if ( $t_dir    eq "$HoC{$host}{$HoCT{$cc_tdef}{dir}}" &&
                             $t_target eq "$HoC{$host}{$HoCT{$cc_tdef}{drt}}" ) {

                            # print "\n\nt_dir = $t_dir  HoC = $HoC{$host}{$r1}\n\n ";
                            my $intro = "<br>" if $cc_box1 ne "";

                            # run.drt.name
                            $cc_box1 = "$cc_box1$intro" . "<b>$HoC{$host}{$HoCT{$cc_tdef}{run}}</b>";
                            # build target
                            $cc_box2 = "$cc_box2$intro" . "$HoC{$host}{'min.build'}";
                            # test dir
                            $cc_box3 = "$cc_box3$intro" . "$t_dir";
                            # test target
                            $cc_box4 = "$cc_box4$intro" . "$t_target";
                            # host
                            $cc_box5 = "$cc_box5$intro" . "$host";
                        }
                    }
                }
                $cc_box1 = $cc_box2 = $cc_box3 = $cc_box4 = $cc_box5 = "&nbsp;" if $cc_box1 eq "";
            }
        }

        # List the contents of tests that contain other tests
        my @tdata_raw = split ' ', $HoF{test}{$rqtst};
        foreach (@tdata_raw) {
            s/UNKNOWN//g;
            s/^ //g;
        }

        # Handle display of tests which contain both compound and
        # non-compound tests.  Only one of these exists as of 05/2007.
        foreach (@tdata_raw) {
            if ( $_ =~ /:/ ) {
                # replace tdata string to display the box borders consistently
                $_ = "&nbsp;";;
            } else {
                # allow display of the test name
                next;
            }
        }

        # Test Directory
        if ("@t_dirs" eq "") { @t_dirs = "&nbsp;" };
        if ("@t_targets" eq "") { @t_targets = "&nbsp;" };
        if ("$ck_list" eq "") { $ck_list = "&nbsp;" };
        print "        <td valign=top>@t_dirs</td>\n";
        # Test Target
        print "        <td valign=top>@t_targets</td>\n";
        # Contains These Tests
        print "        <td valign=top>$ck_list</td>\n";
        # Contact info
        #
        (my $contact = $HoF{contact}{$rqtst}) =~ s/;/<br>/g;
        if ("$contact" eq "") {$contact = "&nbsp;" };
        print "        <td>$contact</td>\n";

        #
        # CC Box
        if ( $opr eq "doc-cc" ) {
            print "        <td valign=top class=\"cctname\">$cc_box1</td>\n";
            print "        <td valign=top>$cc_box2</td>\n";
            print "        <td valign=top>$cc_box3</td>\n";
            print "        <td valign=top>$cc_box4</td>\n";
            print "        <td valign=top>$cc_box5</td>\n";
        }

        print "    </tr>\n";
    }

    print_table_trailer;
    print '</table>', "\n", '</body>', "\n", '</html>', "\n";
}

# -------------------------------------------------------------------------
sub get_func_props {

    # read all test properties into a hash of hashes called HoF - hash of functionals

    my $name;
    my $value;

    if (-e $rq_test_props_file) {

        open(PROPS,"< $rq_test_props_file") || die "Error 09, unable to open \"$rq_test_props_file\" for reading.\n";
        while(<PROPS>)
        {
            # Operate on each line.

            # ignore comment lines and blank lines
            next if ( /^\#/ || /^$/ );

            # remove quotes
            s/\"//g;

            # remove newline
            chomp;

            # Build a hash of hashes containing the test properties.
            # The outer hash key ranges over types of parameters for each remote test.
            # The inner hash key ranges over remote test names.
            # i.e. $build{remote.ide}=minprod_wls
            # i.e. $test{remote.ide}=UNKNOWN remoteDRT_wlp_eclipse remoteDRT_workshop_ide
            # The values are strings.

            foreach $tstparm (qw/ syncto clean build infomsg test contact stages /) {

                if ( /\.$tstparm=/ ) {

                    $name = $value = $_;
                    $name =~ s/=[^=]*$//;   # keep left part
                    $value =~ s/^[^=]+=//;   # keep right part

                    # get the bare name of the remote test by removing
                    # suffix words ".test", ".build" etc.
                    ($rt_name = $name) =~ s/\.$tstparm//;

                    chomp $value;
                    chomp ($HoF{$tstparm}{$rt_name} = $value);
                }
            }
        }
        close PROPS;
        # Extend the test target "remote.multiple" by adding the
        # user-defined tests after whatever was already defined in
        # the property file for "remote.multiple.test".

        if ( $top_test eq "remote.multiple" ) {
            my $rtargets = $ENV{RTARGETS};

            $rtargets eq "" && die "Error 10, RTARGETS is undefined and target is remote.multiple";
            $rtargets =~ s/,/ /g;

            $HoF{"test"}{"remote.multiple"} = $HoF{"test"}{"remote.multiple"} . ' ' . "$rtargets";
        }
    } else {
        print "Test properties file \"$rq_test_props_file\" does not exist.\n";
        print "Exiting $pname\n";
        exit(1);
    }
}

# -------------------------------------------------------------------------
sub get_doc_props {

    # read all doc properties into a hash of hashes called HoD - Hash of Doc(strings)

    my $name;
    my $value;

    if (-e $test_doc_file) {

        open(PROPS,"< $test_doc_file") || die "Error 11, unable to open \"$test_doc_file\" for reading.\n";
        while(<PROPS>)
        {
            # Operate on each line.

            # ignore comment lines and blank lines
            next if ( /^\#/ || /^$/ );

            # remove quotes
            s/\"//g;

            # remove newline
            chomp;

            # Build a hash of hashes containing the test doc properties.
            # The outer hash key ranges over types of parameters for each remote test.
            # The inner hash key ranges over remote test names.
            # i.e. $build{remote.ide}=minprod_wls
            # i.e. $test{remote.ide}=UNKNOWN remoteDRT_wlp_eclipse remoteDRT_workshop_ide
            # The values are strings.

            foreach $tstparm (qw/ dirs/) {

                if ( /\.$tstparm\.doc=/ ) {

                    $name = $value = $_;
                    $name =~ s/=[^=]+$//;   # keep left part
                    $value =~ s/^[^=]+=//;   # keep right part

                    # get the bare name of the remote test by removing
                    # suffixes "build.doc", ".dirs.doc" etc.
                    ($rt_name = $name) =~ s/\.$tstparm\.doc//;

                    chomp ($HoD{$tstparm}{$rt_name} = $value);
                }
            }
        }
        close PROPS;
    }
    else {
        print "Test doc file \"$test_doc_file\" does not exist.\n";
        print "Exiting $pname\n";
        exit(1);
    }
}

# -------------------------------------------------------------------------
sub get_cc_from_p4 {

    system "ksh ./remotetestprops_get_cc_p4.ksh $cc_p4_props_file";

}

# -------------------------------------------------------------------------
sub get_props_from_cc {

    # Read this into a hash:
    #
    # wlwrerh309/config/build.properties:min.build=minprod_wlp
    # wlwrerh309/config/build.properties:drt.name=drt
    # wlwrerh309/config/build.properties:run.drt.name=portal_admintools_portal
    # wlwrerh309/config/build.properties:drt.dir=wlp/tools/admin
    #
    # And write this to a confirmation utility file:
    #
    # drt.name.run  - portal_admintools_portal
    # drt.name      - drt
    # drt.dir       - wlp/tools/admin
    # min.build     - minprod_wlp
    # host          - wlwrerh309
    # portal_admintools_portal minprod_wlp wlp/tools/admin drt

    if (-e $cc_p4_props_file) {

        open(PROPS,"< ${cc_p4_props_file}") || die "Error 12, unable to open file \"${cc_p4_props_file}\" for reading.\n";
        while(<PROPS>)
        {
            chomp;
            my ($host, $prop, $value) = split /[:=]/;
            $host =~ s/\/config\/build.properties//;

            # Hash of Cruisecontrol host properties
            #
            $HoC{$host}{$prop} = $value;

        }
        close PROPS;

        # print the hash to its own file file - for reference and debug
        open(CC_CONFIG,">$cc_p4_props_pretty_file") || die "Error 13, unable to open \"$cc_p4_props_pretty_file\" for reading.\n";


        # Use run.drt[23].name to extract the rest of the CC host
        # properties from the CC host hash.
        #print '-' x 78, "\n\n";
        for my $host ( sort keys %HoC ) {
            for my $prop ( sort keys %{ $HoC{$host} } ) { 
                if ( $prop =~ /(run.drt)([0-9]*)(.name)/ ) {

                    # The following "p1,p2" hack works around a syntax a
                    # problem (perl dies with direct evaluation of
                    # these as args, complains that 'drt' is a
                    # function)
                    my $p1 = "drt$2.name";
                    my $p2 = "drt$2.dir";
                    printf CC_CONFIG "%-13s - %s\n", "run.drt$2.name", $HoC{$host}{$prop};
                    printf CC_CONFIG "%-13s - %s\n", "min.build", $HoC{$host}{'min.build'};
                    printf CC_CONFIG "%-13s - %s\n", "drt$2.dir", $HoC{$host}{$p2};
                    printf CC_CONFIG "%-13s - %s\n", "drt$2.name", $HoC{$host}{$p1};
                    printf CC_CONFIG "%-13s - %s\n", "host", "$host";

                    printf CC_CONFIG "\n";
                    #print CC_CONFIG "$HoC{$host}{$prop} $HoC{$host}{'min.build'} $HoC{$host}{$p2} $HoC{$host}{$p1}\n\n";
                }
            }
        }
        #print '-' x 78, "\n";
        close CC_CONFIG;
    }
    else {
        print "Test properties file \"$cc_p4_props_file\" does not exist.\n";
        print "Exiting $pname\n";
        exit(1);
    }
}
#---------
sub list {

    #
    foreach $rqtst ( sort keys ( %{ $HoF{build} } ) )  {

        print "$rqtst,";
 
   }
}
# ----------------------------------------------------------------------
#                             main
# ----------------------------------------------------------------------

DPR "main:  ARGV = [@ARGV]", "\n";

get_func_props;

# go to town
if ($opr eq "tdata") {
    tdata $top_test;
}
elsif ($opr eq "tdataraw") {
    my $data = $HoF{test}{$top_test};
    $data =~ s/UNKNOWN *//g;
    print $data;
}
elsif ($opr eq "recursive_tdataraw") {
    recursive_tdataraw $top_test;
}
elsif ($opr eq "cdata") {
    cdata $top_test;
}
elsif ($opr eq "stages") {
    stages $top_test;
}
elsif ($opr eq "syncto") {
    syncto $top_test;
}
elsif ($opr eq "infomsg") {
    infomsg $top_test;
}
elsif ($opr eq "contact") {
    contact $top_test;
}
elsif ($opr eq "doc") {
    get_doc_props;
    $table_width='width=1700';
    doc $top_test;
}
elsif ($opr eq "list") {
    list $top_test;
}
elsif ($opr eq "doc-cc") {
    get_cc_from_p4;
    get_props_from_cc;
    get_doc_props;
    doc_cc_hdrs;
    $table_width='width=2400';
    doc $top_test;
}

if ( $opr ne "doc" && $opr ne "doc-cc" ) {
    if ( $out =~ /""/ ) {
        print STDERR "Error 14, the generated TESTDATA is null:  typo in remotetestprops.env ?\n";
        print STDERR "Exiting $pname\n";
        exit(1);
    }
    cleanup;
    print $out;
}

exit 0;
