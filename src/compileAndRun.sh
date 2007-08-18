#!/bin/sh
CLEANCOMMAND="rm -Rf uk/org/dataforce/*/*.class ; rm -Rf uk/org/dataforce/*/*/*.class ; rm -Rf uk/org/dataforce/*/*/*/*.class ; rm -Rf com/dmdirc/parser/*.class ; rm -Rf com/dmdirc/parser/*/*.class ; rm -Rf com/dmdirc/parser/*/*/*.class"
COMPILECOMMAND="javac -Xlint:all uk/org/dataforce/dfbnc/DFBnc.java"
RUNCOMMAND="java uk.org.dataforce.dfbnc.DFBnc"

$CLEANCOMMAND
$COMPILECOMMAND && $RUNCOMMAND "$@"
