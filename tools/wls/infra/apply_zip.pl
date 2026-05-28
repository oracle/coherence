#!/usr/bin/perl -w

# todo
# - check to make sure zip has all the files
# - make sure the files paths make sense (protect against "homemade" .zip's)
# - make sure "jar" command exists

use File::Spec;

if (scalar(@ARGV) != 1) {
  die("Usage: apply_zip file.zip\n");
}

if (! -f $ARGV[0]) {
  die($ARGV[0] . " does not exist!\n");
}

if ( `p4 info` =~ /Client unknown\./) {
  die("Unknown p4 client!\n");
}

open P4, "p4 client -o |" or die("trying to read p4 client Root:");

while(<P4>) {
  if (/^Root:/) {
    chomp;
    s/^Root:[ \t]*//;
    $root = $_;
  }
}

close P4;

#print STDERR "root = " . $root . "\n";

my $abs_jar = File::Spec->rel2abs($ARGV[0]);

#print "abs path of " . $ARGV[0] . " is $abs_jar\n";

my $chdir_loc = $root . "/dev/" . $ENV{"SRC"};

chdir $chdir_loc or die("chdir to $chdir_loc failed");

system("jar -xf \"$abs_jar\" p4opened.files") == 0 or die("retrieving p4opened.files using jar failed"); 

open FILES, "<p4opened.files" or die("trying to open p4opened.files");

@adds = ();
@adds_t = ();
@deletes = ();
@edits = ();
@edits_t = ();

while(<FILES>) {
  chomp;
  ($file, $revision, $type) = /^(.*)#(\d+) - .* \((.*)\)/;
  
  $types{$file} = $type;
  @types{$file . "#" . $revision} = $type;
  @types{$file . "#none"} = $type;
#  print STDERR "TYPES: $file#$revision - $type\n";

  if (/#[0-9]* - (add|branch|move\/add) /) {
    s/#.*/#none/;
    push @adds, $_;
    # this will be sorted to do similar file types in one p4 add -t
    push @adds_t, $types{"$_"} . "|" . $_;
  } elsif (/#[0-9]* - (delete|move\/delete) /) {
    s/ - .*//;
    push @deletes, $_;
  } elsif (/#[0-9]* - (edit|integrate) /) {
    s/ - .*//;
    push @edits, $_;
    # this will be sorted to do similar file types in one p4 edit -t
    push @edits_t, $types{"$_"} . "|" . $_;
  } elsif (/^\.\.\. - file\(s\) not opened on this client/) {
    next;
  } else {
    die("something (" . $_ . " I don't understand in p4opened.files\n");
  }
}

close FILES;

print STDERR "\nINFO: Making sure none of the files are already open...\n\n";

open P4, "| p4 -x- opened > p4opened.tmp" or die("testing to make sure no files were open");

foreach $file (@adds, @deletes, @edits) {
  # use another so the file value in the array is left unmodified
  $simplefile = $file;
  $simplefile =~ s/#.*//;
#  print STDERR "opened file - $simplefile\n";
  print P4 "$simplefile\n";
}

close P4;

open P4, "p4opened.tmp";

while(<P4>) {
    print STDERR $_;
}

close P4;

if (-s "p4opened.tmp") {
  unlink "p4opened.tmp";
  die("Files in the zip are opened in the client");
} else {
  unlink "p4opened.tmp";
}

print STDERR "\nINFO: Syncing files to correct revisions...\n\n";

open P4, "| p4 -x- sync" or die("trying to sync files to correct revision");

foreach $file (@adds, @deletes, @edits) {
#  print STDERR "sync line - $file\n";
  print P4 "$file\n" or die("giving filenames to p4 sync");
}

close P4;

# do deletes before the add checks in case we are doing case sensitive fixes
# (Ex. meta-inf/... to META-INF/...)
if (scalar(@deletes) > 0) {

  print STDERR "\nINFO: Deleting deleted files...\n\n";

  open P4, "| p4 -x- delete" or die("trying to p4 delete deleted files");

  foreach $file (@deletes) {
    $simplefile = $file;
    $simplefile =~ s/#.*//;
#    print STDERR "delete line - $file/$simplefile\n";
    print P4 "$simplefile\n" or die("giving filenames to p4 delete");
  }
  
  close P4 or die("error doing p4 delete ($!, $?)");
}

if (scalar(@adds) > 0) {

  print STDERR "\nINFO: Checking that new (p4 add'ed) files do not already exist...\n\n";

  open P4, "| p4 -x- where > p4where.tmp" or die("trying to p4 where");

  foreach $file (@adds) {
    $simplefile = $file;
    $simplefile =~ s/#.*//;
#    print STDERR "where line - $file/$simplefile\n";
    print P4 "$simplefile\n" or die("giving filenames to p4 where");
  }
  
  close P4 or die("error doing p4 where ($!, $?)");

  open P4, "p4where.tmp";

  my $overwrite = "false";

  while(<P4>) {
    chomp;
    @where = split / /;
    $count = scalar(@where);
    $file = join(' ', @where[(int($count / 2) + 1) .. ($count - 1)]);
#    print STDERR "where: $file\n";
    if (-f $file) {
      print STDERR "FATAL: Local file and zip collision: $file\n";
      $overwrite = "true";
    }
#    print STDERR $_;
  }
  
  close P4 or die("closing p4where.tmp");
  
  unlink "p4where.tmp";

  if ($overwrite eq "true") {
    die("Files p4 add'ed in the zip already exist locally");
  }
}

if (scalar(@edits) > 0) {

  print STDERR "\nINFO: Editing files...\n\n";

  # sort the list of type|file so that all the similar types are together,
  # keep track of the type of the current p4 edit -t and create a new
  # p4 edit -t command each type the type changes
  $currenttype = "";

  foreach (sort @edits_t) {
    ($type, $file) = split /\|/;
    $file =~ s/#.*//;
    if ($currenttype ne $type) {
      open P4, "| p4 -x- edit -t $type" or die("trying to p4 edit -t $type");
#      print STDERR "INFO: start new p4.exe -t $type\n";
      $currenttype = $type;
    }
#    print STDERR "INFO: edit/type line - $type - $file\n";
    print P4 "$file\n" or die("giving filenames to p4 edit");
  }

  close P4 or die("error doing p4 edit -t $currenttype ($!, $?)");

}

print STDERR "\nINFO: Extracting zip into client...\n\n";

system("jar -xf \"$abs_jar\"") == 0 or die("retrieving p4 opened files from zip failed"); 

unlink "p4opened.files", "p4changes.txt";

if (scalar(@adds) > 0) {

  print STDERR "\nINFO: Doing p4 add's...\n\n";

  # sort the list of type|file so that all the similar types are together,
  # keep track of the type of the current p4 add -t and create a new
  # p4 add -t command each type the type changes
  $currenttype = "";

  # FIXME: July 8, 2009 alan.herrlich@oracle.com
  # add appropriate unix2dos/flip -d (windows), unix2dos (unix) to convert
  # files zipped on windows or unix but unzipped on the other platform.

  foreach (sort @adds_t) {
    ($type, $file) = split /\|/;
    $file =~ s/#.*//;
    if ($currenttype ne $type) {
      open P4, "| p4 -x- add -t $type" or die("trying to p4 add -t $type");
#      print STDERR "INFO: start new p4 add -t $type\n";
      $currenttype = $type;
    }
#    print STDERR "add/type line - $type - $file\n";
    print P4 "$file\n" or die("giving filenames to p4 add");
  }

  close P4 or die("error doing p4 add -t $currenttype ($!, $?)");

}

print STDERR "\nINFO: Done.\n\n";

exit;

