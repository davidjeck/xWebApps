
@echo off

REM This Windows script will create the executable jar files xMandelbrot.jar,
REM MandelbrotCL.jar, and MBNetServe.jar.  For this script to work, the JDK
REM must be installed, and the javac and jar commands must be on the PATH
REM environment variable.  This script should be executed on the command line
REM in the same directory as the top-level source directory, named edu.
REM (Just cd to that directory and enter the command:  build

echo Compiling java files...

javac edu\hws\eck\umb\comp\*.java edu\hws\eck\umb\palette\*.java edu\hws\eck\umb\util\*.java edu\hws\eck\umb\*.java

echo Creating jar files...

jar cfm xMandelbrot.jar manifests\xMandelbrot.txt edu\hws\eck\umb\*.class edu\hws\eck\umb\comp\*.class edu\hws\eck\umb\util\*.class edu\hws\eck\umb\palette\*.class edu\hws\eck\umb\resources\strings.properties edu\hws\eck\umb\resources\examples\* 

jar cfm MBNetServe.jar manifests\MBNetServe.txt edu\hws\eck\umb\comp\MandelbrotTask.class edu\hws\eck\umb\comp\MandelbrotNetworkTaskServer*.class
        
jar cfm MandelbrotCL.jar manifests\MandelbrotCL.txt edu\hws\eck\umb\comp\*.class edu\hws\eck\umb\MandelbrotSettings.class edu\hws\eck\umb\palette\Palette.class edu\hws\eck\umb\palette\PaletteMapping.class edu\hws\eck\umb\palette\PaletteIO.class

echo Deleting class files...

del edu\hws\eck\umb\*.class edu\hws\eck\umb\comp\*.class edu\hws\eck\umb\util\*.class edu\hws\eck\umb\palette\*.class

echo Done.
