#!/usr/bin/env perl

#-------------------------------------------------------------------------------
# REVISION HISTORY
# DATE             MODIFIER           DESCRIPTION
# ------------     ---------------    ------------------------------------------
# 2008-03-24       Liu Bo             create (CR321045)
#-------------------------------------------------------------------------------

#-------------------------------------------------------------------------------
# MAIN (POD) DOCUMENTATION
#-------------------------------------------------------------------------------
=pod

=head1 NAME

p4branched.pl

=head1 DESCRIPTION

get the option from cmd, filter all branched files from p4opened.files and get
where the file branched from infomation then write into p4branched.files, or 
apply all those files listed in p4branched.files in p4 to be branched from
accordingly. 

this script run on both developer local machine and remote machine, it was
called by build.xml at local to parse out all those branched files from
p4opened.files, and write the branch from information into p4branched.files.
at remote machine, it was called at apply_zip part to open those branched files
according to p4branched.files

=head1 SYNOPSIS

p4branched.pl [ B<-p4opened> <p4opened.files> ]
              [ B<-p4branched> <p4branched.files> ]
              [ B<-h> ]

=item -p4opened <p4opened.files>

the full access path of p4opened.files

=item -p4branched <p4branched.files>

the full access path of p4branched.files

=item -h

Print help to terminal

=head1 ASSUMPTIONS

none

=head1 PORTABILITY

none

=head1 BUGS AND LIMITATIONS

none

=head1 REFERENCES

none

=cut


###################
# modules
###################
use strict;
use English;
#use GetOpt::Long;

###################
# global variables
###################
my %opts;

####################
# Main block
####################

main();

####################
# subroutines
####################
sub main()
{
    getOpts();
    if ( $opts{p4opened} && $opts{p4branched} )
    {
        getBranchFrom($opts{"p4opened"}, $opts{"p4branched"});
    }
    elsif ( (!$opts{"p4opened"}) && $opts{"p4branched"} )
    {
        apply_branched($opts{"p4branched"});
    }
    else
    {
        printUsage();
        exit 1;
    }

}

sub getBranchFrom()
{
    my $p4opened=shift;
    my $p4branched=shift;
    #convert to unix formatted path 
    $p4opened =~ s/\\/\//g;
    $p4branched =~ s/\\/\//g;
    #remove leading and tail \" or \'
    $p4opened =~ s/^[\"|\']//;
    $p4opened =~ s/[\"|\']$//;
    $p4branched =~ s/^[\"|\']//;
    $p4branched =~ s/[\"|\']$//;

    #my $p4root=$ENV{WEBLOGICHOME};
    my $p4root=getP4root();
    #if ( ! -d $p4root )
    #{
    #    print "P4 client root $p4root not exist!\n";
    #    exit 1;
    #}
    #p4 opened
    #//depot/dev/src1000mp1/env/site.dat#1 - branch change 1098324 (text)
    open(OPENEDFH, "$p4opened") or die "fail to open $p4opened\n" ;
    # do this *before* any chdir's
    open(BRANCHEDFH, ">$p4branched") or die "fail to open $p4branched\n";
    my @branches=();
    while ( <OPENEDFH> )
    {
        #cd to the file path to do the p4 resolved to avoid
        #cygwin p4 can't parse such as c:/dir/file like path
        if ( $_ =~ /(.+)#\d+\s+-\s+(branch)\s+.*$/ )
        {
            my $branchfile=$1;
            $branchfile=~/\/\/dev(\/.*)/;
            my $localfile=$p4root . $1;
            $localfile=~/(.*)\/([^\/]+)$/;
            my $dirname=$1;
            my $filename=$2;
	    #print STDERR "RESOLVED p4r $p4root\n";
	    #print STDERR "RESOLVED bf  $branchfile\n";
	    #print STDERR "RESOLVED lf  $localfile\n";
	    #print STDERR "RESOLVED DIR $dirname\n";
            chdir($dirname);
            #p4 resolved
            #e:\weblogic\dev\src1000mp1\env\site.dat - branch from //depot/dev/src/env/site.dat#1
            my $p4resolved;
	    #print STDERR "RESOLVED IN  $filename\n";
            $p4resolved=`sh -c "p4 resolved $filename"`;
	    #print STDERR "RESOLVED OUT $p4resolved\n";
            chomp $p4resolved;
            if ( $p4resolved =~ /(.*)\s+-\s+branch\s+from\s+(.*)$/ )
            {
                my $from=$2;
                #print BRANCHEDFH "$branchfile - branch from $from\n";
                my $line="$branchfile - branch from $from";
                push(@branches, $line);
            }
            
        }
    }
    close OPENEDFH;

    foreach my $branch (@branches)
    {
        #print STDERR "BRANCHEDFH($p4branched) $branch\n";
        print BRANCHEDFH "$branch\n";
    }
    close BRANCHEDFH;

}

sub apply_branched()
{
    my $p4branched=shift;
    $p4branched =~ s/\\/\//g;
    my $p4root=getP4root();

    open(BRANCHEDFH, "$p4branched") or die "fail to open $p4branched\n";
    while ( <BRANCHEDFH> )
    {
        if ( $_ =~ /(.*)\s+-\s+branch\s+from\s+(.*)$/ )
        {
            my $sourcefile=$1;
            my $fromfile=$2;
            my $output=`p4 integrate -it "$fromfile" "$sourcefile"`;
            #if ( $output !~ /$sourcefile#\d+\s+-\s+branch(\/sync)*\s+from\s+$fromfile/ )
            if ( $? ne 0 )
            {
                print $output;
                exit 1;
            }
            print $output;
        }
    }
    close BRANCHEDFH;
}

sub getP4root()
{
    my $p4root;
    
    my @p4info=`sh -c "p4 info"`;
    foreach my $line (@p4info)
    {
        chomp $line;
        if ( $line =~ /^Client root:\s+(.*)$/ )
        {
            $p4root=$1;
            last;
        }
    }
    return $p4root;

    my $currentdir=`pwd`;
    chomp $currentdir;
    #print STDERR "RESOLVED cd  $currentdir\n";
    if ( $currentdir=~/^(.*)\/dev(\/.*)/ )
    {
        $p4root=$1;
    }
    my $os;
    open (unameExec, "uname -s 2>&1|");
    chomp ($os = <unameExec>);
    close (unameExec);
  
    if ($os =~/^cygwin.*/i)
    {
        $p4root=`cygpath -m $p4root`;
        chomp $p4root;
    }

    $p4root=~s/\s*$//;
    $p4root=~s/\r*$//;
    $p4root=~s/\\/\//g;
    $p4root =~ s/\/$//;
    return $p4root;
}

sub getOpts()
{
# some cygwin perl version didn't install getOpt::Long module, so comment blow lines for now 
#    GetOptions(\%opts,
#               'p4opened=s',     # specify the path of p4opened.files
#               'p4branched=s',    # specify the path of p4branched.txt
#               'h',
#              );
#
#    if (%opts eq 0 || $opts{h})
#    {
#        PrintUsage();
#        exit;
#    }

    if (scalar(@ARGV) == 0 )
    {
        printUsage();
        exit 1;
    }

    my $arg;
    while (scalar(@ARGV) != 0 )
    {
        $arg = shift(@ARGV);
        if ( $arg eq "-p4opened" )
        {
            $opts{"p4opened"}=shift(@ARGV);
        }
        elsif ( $arg eq "-p4branched")
        {
            $opts{"p4branched"} = shift(@ARGV);
        }
        elsif ( $arg eq "-h" || $arg eq "-help")
        {
            printUsage();
            exit;
        }
        else
        {
            print "Unknown option: $arg\n";
            printUsage();
            exit 1;
        }
    }

}

sub printUsage()
{
    print STDERR "\nUsage:\n";
    print STDERR "    -p4opened       : specify the full access path of p4opened.files\n";
    print STDERR "    -p4branched     : (Mandatory) specify the full access path of p4branched.files\n";
    print STDERR " If both option specified, get branched files from p4opened.files and write into p4branched.files\n";
    print STDERR " If only -p4branched specified, apply the branched files in p4 according to p4branched.files\n";
}
