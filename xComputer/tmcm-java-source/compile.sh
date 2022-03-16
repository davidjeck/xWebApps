#!/bin/sh

# This UNIX shell script will compile all the source files
# for the "Most Complex Machine" applets, and it will make
# a jar file containing all the compiled class files.
# The JDK utilities javac and jar must be on the PATH for
# this to work. This file must be run in the directory
# that contains the tmcm directory.

# NOTE THAT MANY WARNING MESAGES ARE PRODUCED DURING THE
# COMPILATION.  HOWEVER, THERE SHOULD BE NO ERRORS.

# After compilation, you can run ALL the applets as a standalone
# application with the command:    java tmcm.Apps
# This command must be given in the directory that contains
# the tmcm directory.  This uses the compiled class files.
# To use the jar file, you need to know the location of
# the file (classes.zip) that contains the standard Java classes. 
# On my system, this is  /usr/lib/java/lib/classes.zip   So, I
# can run the applets with the command:
#    java -classpath tmcm.jar:/usr/lib/java/lib/classes.zip tmcm.Apps

# You can also run individual applets as applications.  Substitute
# the name "tmcm.DataRepsFrame", "tmcm.xComputerFrame", etc., for
# "tmcm.Apps".  However, note that these frames have the following
# problem:  when you close the frame, the Java interpreter will not
# end.  You will need to stop it by typing a Control-C in your
# xterm (or whatever you use for a command-line interface).

echo
echo "Compiling files.  Expect lots of warnings..."
echo

javac tmcm/xComputer/*.java
javac tmcm/xLogicCircuits/*.java
javac tmcm/xModels/*.java
javac tmcm/xSortLab/*.java
javac tmcm/xTuringMachine/*.java
javac tmcm/xTurtle/*.java
javac tmcm/*.java

echo
echo "Making jar file..."
echo

jar cf0 tmcm.jar  tmcm/*.class\
        tmcm/xComputer/*.class\
        tmcm/xLogicCircuits/*.class\
        tmcm/xModels/*.class\
        tmcm/xSortLab/*.class\
        tmcm/xTuringMachine/*.class\
        tmcm/xTurtle/*.class
        
# Uncomment the following lines if you want to delete the
# compiled class files after the .jar file has been made.

# rm tmcm/*.class\
#    tmcm/xComputer/*.class\
#    tmcm/xLogicCircuits/*.class\
#    tmcm/xModels/*.class\
#    tmcm/xSortLab/*.class\
#    tmcm/xTuringMachine/*.class\
#    tmcm/xTurtle/*.class\

