#!/usr/bin/perl -w

use Getopt::Std;
use Net::SMTP;

getopts("f:H:S:", \my %opt) or usage() and exit;
$from = "wls-bt_ww\@oracle.com";
$from = $opt{f} if $opt{f};
$server = "internal-mail-router.oracle.com";
$server = $opt{H} if $opt{H};
$subject = $opt{S} if $opt{S};

#$smtp = Net::SMTP->new($server, Debug => 1);

$smtp = Net::SMTP->new($server);

$smtp->mail($from);
$smtp->to(split(/,/, $ARGV[0]));

$smtp->data();
$smtp->datasend("From: " . $from . "\n") if $from;
$smtp->datasend("Subject: " . $subject . "\n") if $subject;

while (<STDIN>) {
  chomp;
  $smtp->datasend($_ . "\n");
}
$smtp->dataend();

$smtp->quit;
