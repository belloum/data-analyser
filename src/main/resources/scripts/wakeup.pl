#!/usr/bin/perl
# Implements the following auto:
# "presence in bedroom becomes true precedes presence in kitchen becomes true
#    within T minutes
# during wakeup time"
# Occ(Dur(Btw(Bed=>1, Kitch=>1) <= T, Wakeup), Wakeup)

use Getopt::Std;
our %opts = ();

getopts('T:b:e:1:2:f', \%opts) or die "available options:
  -T min: timeout for bed-to-kitchen (default 10 min)
  -b hh:mm[:ss]: begin wakeup time
  -e hh:mm[:ss]: end wakeup time
  -1 pattern: matches the name of the bedroom sensor
  -2 pattern: matches the name of the kitchen sensor
  -f: print input file name on each line";

my $T = defined($opts{'T'})? $opts{'T'}: 10; # timeout
my %lastval = (); # last value of each sensor
#my %lastts = (); # last timestamp of each sensor value
my $lastday; # calendar day of the last log line
my $alarmts; # timestamp of the last alarm
my $bedts;
my $begints = &timestamp($opts{'b'} || "05:00");
my $endts = &timestamp($opts{'e'} || "10:00");
my $bedroom = $opts{'1'} || "MotionD_B";
my $kitchen = $opts{'2'} || "MotionD_K";

while(<>) {
  if(my ($date, undef, $val, $loc, $s) =
  #/\{"date":"([^"]*)"(,"id":"[^"]*")?,"value":([\d.]*),"device":\{"location":"([^"]*)","id":"([^"]*)"(,"type":"[^"]*")?\},"timestamp":\d*\}/
  /\{"date":"([^"]*)"(,"(?:id|state)":"[^"]*")?,"value":([\d.]*),"device":\{"location":"([^"]*)","id":"([^"]*)"(,"type":"[^"]*")?\},"timestamp":\d*\}/
  ) {
    my $ts = &timestamp($date);
    my ($day) = &datetime($date) =~ /^(\d\d\d\d-\d\d-\d\d ...)/;
    if(defined($lastday) && $day ne $lastday) { # at the end of the day
      if(!defined($alarmts)) {
        print "$ARGV; " if $opts{'f'};
        print "$lastday; 00:00:00; 0; wakeup; 0\n";
      }
      $alarmts = undef;
    }
    if(defined($bedts) && &period($bedts, $ts) >= $T * 60) {
      $bedts = undef;
    }
    if(&atnight($ts)) {
      if($s =~ /^$bedroom$/o && $val == 1) {
          $bedts = $ts;
      } elsif($s =~ /^$kitchen$/o && $val == 1 && defined($bedts)
          && !defined($alarmts)) {
          print "$ARGV; " if $opts{'f'};
          print "@{[&datetime($date)]}; $ts; wakeup; 1;" .
            " bedroom at @{[&time($bedts)]}; $bedts\n";
          $bedts = undef;
          $alarmts = $ts;
      }
    }
    $lastval{$s} = $val;
    $lastday = $day;
  }
}
if(!defined($alarmts)) {
  print "$ARGV; " if $opts{'f'};
  print "$lastday; 00:00:00; 0; wakeup; 0\n";
}

sub timestamp {
  my ($date) = @_;
  my ($hh, $mm, undef, $ss) =
    ($date =~ /(\d\d?):(\d\d?)(:(\d\d?))?/);
  my $ts = 60 * (60 * $hh + $mm) + $ss;
  return $ts;
}

sub time {
  use integer;
  my ($ts) = @_;
  my $hh = $ts / (60 * 60);
  $ts = $ts % (60 * 60);
  my $mm = $ts / 60;
  $ts = $ts % 60;
  my $ss = $ts;
  return sprintf("%02d:%02d:%02d", $hh, $mm, $ss);
}

sub period {
  my ($t1, $t2) = @_;
  if($t1 <= $t2) { return $t2 - $t1; }
  else { return (24 * 60 * 60) - $t1 + $t2; }
}

sub atnight {
  my ($t) = @_;
  return &period($begints, $t) <= &period($begints, $endts);
}

sub datetime {
  my ($date) = @_;
  my %mon2mm = ('Jan' => 1, 'Feb' => 2, 'Mar' => 3, 'Apr' => 4,
    'May' => 5, 'Jun' => 6, 'Jul' => 7, 'Aug' => 8,
    'Sep' => 9, 'Oct' => 10, 'Nov' => 11, 'Dec' => 12);
  ($day, $mon, $nn, $time, $zone, $year) =
    $date =~ /(...) (...) (\d\d) (\d\d:\d\d:\d\d) (\S+) (\d\d\d\d)/;
  my $mm = sprintf("%02d", $mon2mm{$mon});
  return "$year-$mm-$nn $day; $time";
}
