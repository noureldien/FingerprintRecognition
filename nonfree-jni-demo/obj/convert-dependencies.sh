#!/bin/sh
# AUTO-GENERATED FILE, DO NOT EDIT!
if [ -f $1.org ]; then
  sed -e 's!^D:/Android_dev/cygwin/lib!/usr/lib!ig;s! D:/Android_dev/cygwin/lib! /usr/lib!ig;s!^D:/Android_dev/cygwin/bin!/usr/bin!ig;s! D:/Android_dev/cygwin/bin! /usr/bin!ig;s!^D:/Android_dev/cygwin/!/!ig;s! D:/Android_dev/cygwin/! /!ig;s!^D:!/cygdrive/d!ig;s! D:! /cygdrive/d!ig;s!^C:!/cygdrive/c!ig;s! C:! /cygdrive/c!ig;' $1.org > $1 && rm -f $1.org
fi
