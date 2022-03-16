#!/bin/bash

# This script will create the three executable jar files xMandelbrot.jar,
# MandelbrotCL.jar, and MBNetServe.jar.  It should work on Linux and Mac OS,
# assuming that you have a Java SDK installed.
#
# To use it, open a Terminal (command line) window, cd to the directory that
# contains this script and the top-level source directory (edu), and type
# the command:  ./build.sh

echo "Compiling java files..."
javac `find . -name "*.java"`

echo "Creating jar files..."
jar cfm xMandelbrot.jar manifests/xMandelbrot.txt\
        edu/hws/eck/umb/resources/strings.properties\
        edu/hws/eck/umb/resources/examples/*\
        `find . -name "*.class"`

jar cfm MBNetServe.jar manifests/MBNetServe.txt\
        edu/hws/eck/umb/comp/MandelbrotTask.class\
        edu/hws/eck/umb/comp/MandelbrotNetworkTaskServer*.class
        
jar cfm MandelbrotCL.jar manifests/MandelbrotCL.txt\
        edu/hws/eck/umb/comp/*.class\
        edu/hws/eck/umb/MandelbrotSettings.class\
        edu/hws/eck/umb/palette/Palette.class\
        edu/hws/eck/umb/palette/PaletteMapping.class\
        edu/hws/eck/umb/palette/PaletteIO.class

echo "Deleting class files..."
rm `find . -name "*.class"`

        
