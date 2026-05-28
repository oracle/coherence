#!/usr/bin/perl -w -W

# todo
# - remove sort of machine names
#
# other rbt todo

use warnings;
use strict;

my %machs = ();		# list of machines with characteristics
my %jobs = ();		# list of all jobs
my %jobusers = ();	# list of the user name associated with each remotetest jobs
my %users = ();         # count of jobs each user has running

my %excluded = ();
my %nightly_excluded = ();
my %rq_excluded = ();
my %rq_machs = ();	# list of available rq machines
my %nonrq_machs = ();	# list of available non-rq machines
my %jenkins_machs = (); # list of idle jenkins machines
my %job_type = ();	# list of job with job type, if remotetest then set to remotetest_job, if others, non for now

my $jenkins_jobs = 0;   # number of pseudo jenkins machines I have created
my $jenkins_linux  = (defined $ARGV[0]) ? $ARGV[0] : 0;
my $jenkins_win    = (defined $ARGV[1]) ? $ARGV[1] : 0;
my $jenkins_macosx = (defined $ARGV[2]) ? $ARGV[2] : 0;

#print STDERR "linux = " . $jenkins_linux . ", windows = " . $jenkins_win . ", macosx = " . $jenkins_macosx . "\n";

my $SRC = $ENV{"SRC"};

sub read_btstatrc {
  my $rcfile = $ENV{"DEV_ROOT"} . "/tools/wls/infra/build/statuspage/btstat" . $ENV{"NEW_SRC"} . "rc";
  if (-f $rcfile) {
    my @tmp_excluded;
    my @tmp_nightly_excluded;
    my @tmp_rq_excluded;
    open(RCFILE, $rcfile);
    while (<RCFILE>) {
      chomp;
      s/\r$//;
      s/#.*$//;
      s/^[ \t]*//;
      if (/^dynamicexclude/) {
	s/^dynamicexclude[ \t]*//;
	push(@tmp_excluded, split(/[ \t]/));
#	print STDERR $_ . "\n";
      }
      if (/^nightlyexclude/) {
	s/^nightlyexclude[ \t]*//;
	push(@tmp_nightly_excluded, split(/[ \t]/));
#	print STDERR $_ . "\n";
      }
      if (/^nightlyonly/) {
	s/^nightlyonly[ \t]*//;
	push(@tmp_rq_excluded, split(/[ \t]/));
#	print STDERR $_ . "\n";
      }

    }
    close(RCFILE);
    @excluded{@tmp_excluded} = ();
    @nightly_excluded{@tmp_nightly_excluded} = ();
    @rq_excluded{@tmp_rq_excluded} = ();
  }
}

sub excluded {
  return (exists($excluded{$_[0]}) ||
	  ($machs{$_[0]} =~ /\|RBT_EXCLUDED=true\|/));
}

sub nightly_excluded {
  return (exists($nightly_excluded{$_[0]}) ||
	  ($machs{$_[0]} =~ /\|RBT_NIGHTLY_EXCLUDED=true\|/));
}

sub get_speed {
    my @chars = split /\|/, $machs{$_[0]};
    # a general lower bounds to the amount of memory machines have, it should be lower than the smallest machine
    my $lower_mem = 5600;
    my $mem = $lower_mem;
    foreach (@chars) {
#	print STDERR "chars=$_\n";
	if (/^MEM=/) {
#	    print STDERR "mem= $_\n";
	    my ($name, $value) = split /=/;
	    undef $name;
	    $mem = $value;
	}
    }
    foreach (@chars) {
#	print STDERR "chars=$_\n";
	if (/^CPU=/) {
#	    print STDERR "cpu= $_\n";
	    my ($name, $value) = split /=/;
	    undef $name;
	    my @cpu = split /,/, $value;
	    # add in the number of cpu's to give machines with the same
	    # clock speed but more cpu's a slight advantage.
	    # also give advantage to machines that have a multiple of the lower amount of memory
	    # (I pulled this algorithm out of thin air)
	    return ( $cpu[0] + @cpu ) + ( ($mem / $lower_mem) * 200);
	}
    }
    return 0;
}

sub slower {
    my $speed1 = get_speed($_[0]);
    my $speed2 = get_speed($_[1]);
#    print STDERR "speed1 = $speed1, speed2 = $speed2\n";
    return ($speed1 < $speed2);
}

sub jobmatch {
    my @chars = split /,/, $_[0];
    shift(@chars);
    my $mach = $_[1];
#    print STDERR "chars=$chars[0]\n";
#    print STDERR "mach=$mach\n";
    foreach (@chars) {
#	print STDERR "defvar=$_\n";
	if (/HOST=.*/) {
	    (my $foobar, my $host) = split /=/;
	    undef $foobar;
	    if ($host eq $mach) {
#		print STDERR "--- $_ got it!\n";
	    } else {
		return 0;
	    }
	} elsif (/OS=Win/i) {
	    if ($machs{$mach} =~ /\|OS=(W2K|W2K3|W2K7|W2K8|XP|VISTA|Windows|W2K12)\|/i) {
#		print STDERR "--- $_ got it!\n";
	    } else {
		return 0;
	    }
	} elsif (/OS=Unix/i) {
	    if ($machs{$mach} =~ /\|OS=(Linux|Sol|HP)\|/i) {
#		print STDERR "--- $_ got it!\n";
	    } else {
		return 0;
	    }
	} elsif (/OS=.*/) {
	    if ($machs{$mach} =~ /\|$_\|/i) {
#		print STDERR "--- $_ got it!\n";
	    } else {
		return 0;
	    }
	} elsif (/.*=false$/) {
	    # if the characteristic is FOO=false then match FOO=false else
	    # the non-match of FOO=true
	    if ($machs{$mach} =~ /\|$_\|/) {
#		print STDERR "--- $_ got it!\n";
	    } else {
		(my $name) = split /=/;
		if ($machs{$mach} =~ /\|$name=true\|/) {
		    return 0;
		} else {
#		    print STDERR "--- $_ got it!\n";
		}
	    }
	} elsif ($machs{$mach} =~ /\|$_\|/) {
#	    print STDERR "--- $_ got it!\n";
	} else {
	    return 0;
	}
    }
    return 1;
}

sub rqmach_better {
	# this part is newly added to deal with remotetest job to get machine from specified machine list
	my $find_type = $_[0];
	my $find_mach = "";
	my @find_machlist = () ;
	if ($find_type eq "rq_find") {
	    @find_machlist = (sort keys %rq_machs);
	} else {
		@find_machlist = (sort keys %nonrq_machs);
	}
	my $jobchars = $_[1];
    foreach my $mach (@find_machlist) {
	  if (jobmatch($jobchars, $mach)) {
	    if (! $find_mach) {
	    # if we don't have a match yet then start with the first machine to match
	        $find_mach = $mach;
	    } else {
	      if (slower($find_mach, $mach)) {
	        $find_mach = $mach;
	      }
	    }
	  }
	}
	return $find_mach;
}

sub find_machine {
    my $chars = $_[0];
	my $jobname = $_[1];
    my $bestmatch = "";
	# below if/else divide find_machine to two part logic,if part is new logic for remotetest job to get machine,
	# else part is non remotetest job which use old logic to get machine
	if ( $job_type{$jobname} eq "remotetest_job") {
	    # call new bestmatch logic rqmach_better for remotetest_job
	    # nightly_excluded is set to true, do not find machine in nonrq_machs
	    # already find a rq machine for remotetest job, no need to check nonrq machines
	    $bestmatch = rqmach_better ('rq_find', $chars);
	    if (! ($chars =~ /RBT_NIGHTLY_EXCLUDED=true/ || $bestmatch) ) {
	        $bestmatch = rqmach_better ('nonrq', $chars);
	    }
	} else {
    foreach my $mach (sort keys %machs) {
	if (jobmatch($chars, $mach)) {
#	    print STDERR "$job matches $mach\n";
	    if (! $bestmatch) {
#		print STDERR "first bestmatch $mach\n";
		# if we don't have a match yet then start with the first machine to match
		$bestmatch = $mach;
	    } else {
		if (nightly_excluded($bestmatch) < nightly_excluded($mach)) {
#		    print STDERR "bestmatch $bestmatch to $mach (nightly excluded)\n";
		    # if this is a WLS nightly then nightly_excluded is always false(1)
		    # if this is a remote test then nightly_excluded is true(0) or false(1)
		    # and we want to favor true(0), nightly_excluded
		    $bestmatch = $mach;
		} elsif (nightly_excluded($bestmatch) != nightly_excluded($mach)) {
		    next;
		} elsif (slower($bestmatch, $mach)) {
#		    print STDERR "bestmatch $bestmatch to $mach (faster)\n";
		    # all things being equal use the faster machine
		    $bestmatch = $mach;
		}
#		if ((nightly_excluded($bestmatch) == nightly_excluded($mach)) &&
#		    slower($bestmatch, $mach)) {
#		    $bestmatch = $mach;
#		} elsif (! nightly_excluded($bestmatch)) {
#		    # favor machines that are RBT_NIGHTLY_EXCLUDED
#		    if (nightly_excluded($mach)) {
#			$bestmatch = $mach;
#		    } else {
#			if (slower($bestmatch, $mach)) {
#			    $bestmatch = $mach;
#			}
#		    }
#		}
	    }
	}
    }
	}
    return $bestmatch;
}

sub load_machs {
    my $loaddir = $_[0];
    
    if (! -d "$loaddir/available") {
	return;
    }
    opendir(DIR, "$loaddir/available") or die "can not openddir available/: $!";
    while (defined(my $file = readdir(DIR))) {
      if (-f "$loaddir/available/$file" && -r "$loaddir/available/$file" &&
	  $file ne "." && $file ne ".." &&
	  ! ($file =~ /~$/) ) {
#	    print STDERR "$file\n";
	    my $values = "|";
	    my $age = time() - (stat("$loaddir/available/$file"))[9];
#	    print STDERR " age=$age\n";
	    # Expire any available files older than 20 minutes old.  They are
	    # recreated every 10 minutes by rdequeuer.  Note that this requires
	    # the time on all machines to be syncronized to within 10 minutes.
	    # If the machine time is old then the available file may appear
	    # to be old as soon as it is created.
	    if ($age > 1200) {
	        unlink "$loaddir/available/$file";
		print STDERR "delete old available file ($age seconds old) $loaddir/available/$file\n";
		next;
	    }
	    if (!open(MACHINE, "$loaddir/available/$file")) {
	      # Apr 2*, 2015 this was happening a lot, ignore bad permission files - alan.herrlich@oracle.com
	      if ($! == 2 || $! == 13) {
		print STDERR "skipping $loaddir/available/$file: $! (" . ($! + 0) . ")\n";
		next;
	      } else {
		die("can not open file ($loaddir/available/$file): $! (" . ($! + 0) . ")");
	      }
	    }
	    while (<MACHINE>) {
		chomp;
		s/\r$//;
#	    print STDERR "\t$_\n";
		$values .= $_ . "|";
	    }
#	    print STDERR "$values\n";
	    close(MACHINE);
	    if ($values =~ /\|RBT_EXCLUDED=true\|/) {
#	    print STDERR "skipping RBT_EXCLUDED $file\n";
	    } elsif (! ($values =~ /\|SRC=coherence\/$SRC\|/) && ($loaddir =~ /\/common$/)) {
		# don't do this if loading $SRC/available only from /common/available ?
#		print STDERR "skipping non SRC=$SRC $file\n";
	    } else {
		$machs{$file} = $values;
		if (exists($nightly_excluded{$file}) || $values =~ /\|RBT_NIGHTLY_EXCLUDED=true\|/) {
		    # divide available machs to two types, rq and nonrq, value 1 is meaningless at this time, psuedo jenkins# will use the jenkins label instead of 1
		    $rq_machs{$file} = 1;
		} else {
		    $nonrq_machs{$file} = 1;
		}
	    }
	}
    }
    closedir(DIR);
}

sub load_jenkins_machs {
  #print "count = " . $_[0] . ", chars = " . $_[1] . ", label = " . $_[2] . ", total = " . $jenkins_jobs . "\n";
  for (my $i=$jenkins_jobs; $i < ($jenkins_jobs + $_[0]); $i++) {
    my $name = "jenkins#" . sprintf("%03d", $i);
    # print STDERR "name = $name\n";
    $machs{$name} = $_[1];
    $rq_machs{$name} = $_[2];
  }
  $jenkins_jobs += $_[0];
}

read_btstatrc();

#foreach (sort keys %excluded) {
#  print STDERR "excluded: " . $_ . "\n";
#}
#foreach (sort keys %nightly_excluded) {
#  print STDERR "nightly_excluded: " . $_ . "\n";
#}

chdir($ENV{"RQSITE"} . "/" . $SRC);

load_machs($ENV{"RQSITE"} . "/$SRC");
load_machs($ENV{"RQSITE"} . "/common");
load_machs($ENV{"RQSITE"} . "/../common");

# this loads a bunch of "attractive" psuedo jenkins machines that will be enqueued to before regular RQ machines
# to let regular RQ machines have highest priority there is a line in rbt.sh you can comment out
# there is still code below that will use jenkins agents if no regular RQ machines can be found
# Note: that k8s can *not* handle ADE=true because it does not have farm commands.
load_jenkins_machs($jenkins_linux,  "|OS=Linux|CPU=9999999|RBT_NIGHTLY_EXCLUDED=true|", "(linux-ol8||linux-ol9)");
load_jenkins_machs($jenkins_win,    "|OS=Windows|CPU=9999999|RBT_RQ_EXCLUDED=false|RBT_NIGHTLY_EXCLUDED=true|", "((windows-rq||win-rq)&&!slow)");
load_jenkins_machs($jenkins_macosx, "|OS=macosx|CPU=9999999|RBT_NIGHTLY_EXCLUDED=true|", "(macosx-rq&&!slow)");

opendir(DIR, "queued") or die "can not openddir queued/: $!";
while (defined(my $file = readdir(DIR))) {
    if ($file ne "." && $file ne ".." && ! ($file =~ /~$/) && $file =~ /^job\./ && ! ($file =~ /\.owner$/)) {
#	print STDERR "$file\n";
	open(JOB, "queued/$file") || die("can not open file (queued/$file): $!");
	my $chars = <JOB>;
	if ($chars) {
	    chomp $chars;
	    $chars =~ s/\r$//;
	    $chars =~ s/^#//;
#	    print STDERR "\t$chars\n";
	    my $command = <JOB>;
	    if (!defined($command)) {
	      print "DEBUG: undefined command, queued/$file\n";
	    }
	    if (! ($command =~ /\/remotetest/)) {
		# if it is a WLS nightly (i.e. isn't a remotetest) then
		# prevent it from running on any machine
		# that is excluded from running nightly tests (; hits
		# multi-machine tests, appending to the end hits single
		# machine tests
		$jobusers{$file} = "bt";
		$chars =~ s/;/RBT_NIGHTLY_EXCLUDED=false,;/g;
		$chars .= 'RBT_NIGHTLY_EXCLUDED=false,';
		$job_type{$file} = 'nonrq_job';
#		print STDERR "chars=$chars\n";
	    } else {
	        my @jobargs = split / +/, $command;
		if ($jobargs[0] eq "sh") {
		  $jobusers{$file} = $jobargs[3];
		} else {
		  $jobusers{$file} = $jobargs[2];
		}
#		print STDERR "foo: " . $command . "\n" if ($jobusers{$file} eq "");
	    	$chars =~ s/;/RBT_RQ_EXCLUDED=false,;/g;
	    	$chars .= 'RBT_RQ_EXCLUDED=false,';
		$job_type{$file} = 'remotetest_job';
	    }
	    # make sure all the "users" have a entry in "users"
	    $users{$jobusers{$file}} = 0;
	    $jobs{$file} = $chars;
	}
	close(JOB);
    }
}
closedir(DIR);

opendir(DIR, "status") or die "can not openddir status/: $!";
while (defined(my $file = readdir(DIR))) {
    if ($file ne "." && $file ne ".." && ! ($file =~ /~$/) && $file =~ /^job\./ && $file =~ /\.owner$/) {
#        print STDERR "owner2: $file\n";
	open(JOBOWNER, "status/$file") || die("can not open file (status/$file): $!");
	my $user = <JOBOWNER>;
	if ($user) {
	    chomp $user;
	    $user =~ s/\r$//;
	    $user =~ s/^#//;
#	    print STDERR "\t$user\n";
	    if (defined($users{$user})) {
	      $users{$user} ++;
	    } else {
	      $users{$user} = 1;
	    }
	}
	close(JOBOWNER);
    }
}
closedir(DIR);

#foreach my $user (sort keys %users) {
#    print STDERR "$user\t$users{$user}\n";
##    print STDERR "$user\n";
#}

#foreach my $mach (sort keys %machs) {
#    print STDERR "$mach\t$machs{$mach}\n";
#    print STDERR "$mach\n";
#}

#print STDERR "checking...\n";

my @sorted_jobs = sort {
  # sort by user count of jobs running (lower is better) then...
  # whether or not it is a HOST= job (give higher priority) then...
  # the job id (which is the UTC enqueue time, lower is better)
  if ($users{$jobusers{$a}} == $users{$jobusers{$b}}) {
    if ($jobs{$a} =~ /,HOST=/) {
      if ($jobs{$b} =~ /,HOST=/) {
	return $a cmp $b;
      } else {
	return -1;
      }
    } elsif ($jobs{$b} =~ /,HOST=/) {
      return 1;
    } else {
      return $a cmp $b;
    }
  } else {
    return $users{$jobusers{$a}} <=> $users{$jobusers{$b}};
  } } keys %jobs;

foreach my $job (@sorted_jobs) {
    my $chars;
#    print STDERR "$job $users{$jobusers{$job}} $jobs{$job}\n" if ($jobusers{$job} ne "bt");
    $chars = $jobs{$job};
    my $bestmatch = "";
    my $match = "";
    if ($chars =~ /;/) {
#	print STDERR "multi-machine config detected ($chars)!\n";
	my @metachars = split /;/, $chars;
	my %metamachs = ();
	foreach my $meta (@metachars) {
#	    print STDERR "metachar=$meta\n";
	    my $match = find_machine ($meta,$job);
	    if ($match) {
		$metamachs{$match} = $machs{$match};
		delete $machs{$match};
		delete $rq_machs{$match};
		delete $nonrq_machs{$match};
		# create bestmatch incrementally so that the faster machines
		# returned by find_machine are listed first ($bestmatch could
		# also be created using a join of metamachs)
		$bestmatch .= $match . " ";
	    } else {
		# if find_machine failed to find a machine for one of the
		# job characteristics then add all the machines found so far
		# back into the machine array
		%machs = (%machs, %metamachs);
		%metamachs = ();
		$bestmatch = "";
		last;
	    }
#	    print STDERR "meta bestmatch=$match\n";
	}
	# get rid of trailing space
	chop $bestmatch;
    } else {
	$bestmatch = find_machine ($chars,$job);
    }
    # this puts RQ jobs on jenkins machines if no regular RQ machines are available
    # uncomment the false line and comment the bestmatch line to disable
    #if (false) {
    if ($bestmatch eq "") {
      #
      # A guide to the output of this...
      #  jenkins@machine		# run on jenkins agent "machine"
      #  jenkins=label_expression	# run on user supplied -Dlabel=label_expression
      #  jenkins=((win-rq||windows-rq)&&!slow) # no OS=Win boxes in the regular RQ use one in Jenkins
      #  jenkins=(($OS-rq&&!slow)	# no OS=??? boxes in the regular RQ use one in Jenkins
      # 
      # only use jenkins if there isn't a regular "available" machines and for OS=Linux jobs
      if ($jobs{$job} =~ /,HOST=([^,]*),/) {
	my @keys = keys %jenkins_machs;
	my $size = @keys;
	# fill up %jenkins_machs if it is empty
	if ($size eq 0) {
	  #print `env | sort -i`;
	  open (GET, ". \$DEV_ROOT/.rbt.envs ; . \$DEV_ROOT/tools/wls/infra/build/rbt.sh ; get_machines -jio |");
	  while (<GET>) {
	    chomp;
	    $jenkins_machs{$_} = 1;
	    #print STDERR "DEBUG1: >$_<\n";
	  }
	  close(GET);
	}
	# grab host from job characteristics
	my $host = $1;
	#print STDERR "DEBUG2: host:$host:\n";
	if (exists($jenkins_machs{$host})) {
	  #print STDERR "DEBUG3: $host HOST= found it!\n";
	  $bestmatch = "jenkins@" . $host ;
	} else {
	  # foreach my $mach (sort keys %jenkins_machs) {
	  #   print STDERR "DEBUG4: $mach\n";
	  # }
	}
      } elsif ($jobs{$job} =~ /,LABEL=([^,]*),/) {
	$bestmatch="jenkins=$1";
      } elsif ($jobs{$job} =~ /,OS=([^,]*),/) {
	my $os = lc $1;
	if ($os eq "win") {
	  # because the "standard" windows label seems to be windows but IMHO it should match the RQ one of "win"
	  $bestmatch="jenkins=((win-rq||windows-rq)&&!slow)";
	} else {
	  if ($os eq "linux") {
 	    $bestmatch="jenkins=(linux-ol8-rq&&!slow)";
 	  } else {
 	    $bestmatch="jenkins=($os" . "-rq&&!slow)";
 	  }
	}
      } 
    }
    if ($bestmatch) {
#	print STDERR "$job-$jobs{$job}-best match = $bestmatch\n";
        if ($bestmatch =~ /^jenkins#/) {
	  print "$job \t jenkins=" . $rq_machs{$bestmatch} . "\n";
	} else {
	  print "$job \t $bestmatch\n";
	}
# remove the machine and job from the array
	delete $machs{$bestmatch};
	delete $jobs{$job};
	delete $rq_machs{$bestmatch};
	delete $nonrq_machs{$bestmatch};
#	exit;
    }
}

