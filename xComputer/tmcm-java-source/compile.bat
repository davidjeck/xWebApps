@echo off

REM   This .bat script for DOS/Windos will compile all the source
REM   files for the "Most Complex Machine" applets, and it will make
REM   a jar file containing all the compiled class files.
REM   The JDK utilities javac and jar must be on the PATH for
REM   this to work. This file must be run in the directory
REM   that contains the tmcm directory.  Just give the command:  compile.bat

REM   NOTE THAT MANY WARNING MESAGES ARE PRODUCED DURING THE
REM   COMPILATION.  HOWEVER, THERE SHOULD BE NO ERRORS.

REM   After compilation, you can run ALL the applets as a standalone
REM   application with the command:    java tmcm.Apps
REM   This command must be given in the directory that contains
REM   the tmcm directory.  This uses the compiled class files.
REM   To use the jar file, you need to know the location of
REM   the file (classes.zip) that contains the standard Java classes. 
REM   On my system, this is  C:\jdk1.1.6\lib\classes.zip   So, I
REM   can run the applets with the command:
REM      java -classpath tmcm.jar;C:\jdk1.1.6\lib\classes.zip tmcm.Apps

REM   If you have Microsoft Internet Explorer with Java, you should
REM   also be able to use Microsoft's jview command to run the applets.
REM   The jview command is easier to use with jar files, since you don't
REM   have to specify the location of the standard Java classes.
REM   To run the applets using the jar file, use the command  
REM         jview -cp tmcm.jar tmcm.Apps


REM   You can also run individual applets as applications.  Substitute
REM   the name "tmcm.DataRepsFrame", "tmcm.xComputerFrame", etc., for
REM   "tmcm.Apps".  However, note that these frames have the following
REM   problem:  when you close the frame, the Java interpreter will not
REM   end.  You will need to stop it by typing a Control-C in your
REM   DOS command window.

echo .
echo . Compiling files.  Expect lots of warnings...
echo .

javac tmcm\xComputer\*.java
javac tmcm\xLogicCircuits\*.java
javac tmcm\xModels\*.java
javac tmcm\xSortLab\*.java
javac tmcm\xTuringMachine\*.java
javac tmcm\xTurtle\*.java
javac tmcm\*.java

echo .
echo . Making jar file...
echo .

jar cf0 tmcm.jar tmcm\*.class tmcm\xComputer\*.class tmcm\xLogicCircuits\*.class tmcm\xModels\*.class tmcm\xSortLab\*.class tmcm\xTuringMachine\*.class tmcm\xTurtle\*.class

                
REM   Remove the following line if you want to delete the
REM   compiled class files after the .jar file has been made.

exit


echo .
echo . Deleting class files...
echo .

del tmcm\*.class
del tmcm\xComputer\*.class
del tmcm\xLogicCircuits\*.class
del tmcm\xModels\*.class 
del tmcm\xSortLab\*.class 
del tmcm\xTuringMachine\*.class 
del tmcm\xTurtle\*.class


