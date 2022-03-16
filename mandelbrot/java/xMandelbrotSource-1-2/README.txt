
This directory, xMandelbrotSource, contains the Java source code for David Eck's
Mandelbrot Viewer program.  More information about the program can be found on
the web at http://math.hws.edu/eck/js/mandelbrot/java/MB-java.html

    --David J. Eck
      Department of Mathematics and Computer Science
      Hobart and William Smith Colleges
      300 Pulteney Street
      Geneva, NY  14456
      eck@hws.edu
      
----------------------------------------------------------------------------

February 2016.  As support for Java applets disappears from the Web, I have
published a JavaScript version of the Mandelbrot Viewer that can still be
used on the Web.  It does not have all the features of the Java version,
but is still quite usable.  The Java version is still available for use
as a stand-alone application.  The JavaScript version is on-line at:

       http://math.hws.edu/eck/js/mandelbrot/MB.html

Version 1.2, June 2011, adds a few menu items ("Zoom In By / Zoom Out By",
a 400x300 image size, and a Dark Spectrum default palette).  It also makes
high precision MandelbrotTasks interruptable, making the interface more
responsive when canceling a high precision calculation.

Version 1.1, May 2011, corrects some errors that affected very large images
only.  It also improved memory management to make it possible to compute
larger images.  And it made a minor change to the network config dialog,
to allow network config changes to be saved when user clicks the close
box of the dialog.

----------------------------------------------------------------------------

The Java source code is in the directory named edu.  You can use the source in
an integrated development such as Eclipse or Netbeans by adding a copy of this
directory to your project.  You can also use it on the command line.  Two utility
scripts --- one for Windows (build.bat) and one for Linux/Mac (build.sh) --- are
included in this directory.  When executed on the command line, these scripts 
create three executable jar files:  xMandlebrot.jar,  MandelbrotCL.jar, and 
MBNetServe.jar.  To compile the programs, you need to have a Java JDK installed
on your computer.  (The Java runtime, which lets you run Java programs but not
compile them, is not sufficient.)

xMandelbrot.jar is the main Mandelbrot Viewer program.  You should be able to
double-click this file to run the viewer program.  (On Linux, you might have to
right-click the file icon and tell the system to open it using java or a command such
as "java -jar".)  You can also run it on the command line with the command:
java -jar xMandelbrot.jar

The program should be pretty easy to use, and there are a few "Help" items in its
menus and in its Palette Editor with information about the less obvious features.
More documentation can be found on the web site (http://math.hws.edu/xJava/MB).
One non-obvious feature is the ability to use distributed computation over a network.
This feature is documented below, along with information about MBNetServe.jar, which
is used for network operation.

MandelbrotCL.jar is a simple command-line utility for making Mandelbrot images.  This
program can read Mandelbrot "settings" files, which are created by xMandelbrot.jar, and
can create the images that are specified by those files.  I use this utility, for example,
to create small "thumbnail" images from a group of settings files.  The syntax for using
this command is:

           java -jar MandelbrotCL.jar  [options]  filenames...
           
where "filenames..." represents one or more settings file names and the "[options]"
can include any or all (or none) of the following:

   -size WWWxHHH --- where WWW and HHH are positive integers, specifies the size
                     of the image.  If no size is specified, 800x600 is used.  

   -format XXX -- use XXX as the format for the image.  PNG is the default.  JPEG is 
                  also definitely supported.  Other formats might be supported as well.
                  
   -onepass --- turn subpixel sampling off.  If this option is omitted, subpixel sampling
                is used.  (This is an option in the Control menu of xMandelbrot.jar;
                subpixel sampling can produce smoother, more attractive images in many
                cases.)
                
   -net XXX --- add one or more network workers.  The format for XXX is a list of one 
                or more hosts, separated by commas.  Each host can be specifed as a host 
                name or IP address optionally followed by a colon and a port number.  The 
                port number is only necessary if different from the default, 17071.
                No spaces are allowed in the list of computers.  A copy of MBNetServe.jar 
                should already be running on each of the  specified computers.
                
For example, to make small JPEG images for two files named mbdata1.xml and mbdata2.xml:

        java -jar MandelbrotCL.jar -size 160x120 -format jpeg mbdata1.xml mbdata2.xml
        
By the way, if you are working with very large images, you might need to tell the java
virtual machine to use more memory than it ordinarily would.  You can do this with the
"-Xmx" option to the java command.  For example:

       java -jar -Xmx2000m MandelbrotCL.jar -size 3300x2550 settings.xml
       
The program has been used for images as large as 10800-by-7200 pixels.
       
------------------------------------------------------------------------------------------

About networking:

It can take a long time to compute some Mandelbrot images.  This is especially true
for high precision computation, which is used when you zoom in so far that the standard
Java real number implementation does not have enough significant digits to represent
the numbers involved.  The networking option is for people who want to speed up these
long computations and who have access to several networked computers.

To distribute Mandelbrot computations over a network, you must run MBNetServe.jar on
each computer EXCEPT the one where you will run xMandlebrot.jar (or MandelbrotCL.jar).
Once that is done, you can use the "Configure Multiprocessing..." command in the
"Control" menu of xMandelbrot to configure that program to use the network. (For
MandelbrotCL, use the "-net" option.)  You will need to know something about
networking in order to use this option.  Most important, you will need to know the
host names or IP addresses of the computers where the server is running.

To run MBNetServe.jar on the computers that you want to use as computation servers, 
use the following command in the directory that contains the program:

                java -jar MBNetServe.jar [options]
                
where the "[options]" can include:

      -processcount XXX --- use XXX processes instead of the default number.  Enter 0 for 
                            XXX to use one process for each available processor.  The 
                            default is to use one less than this (if the number of processors 
                            is greater than one).
                            
      -timeout XXX --- exit after XXX minutes of inactivity.  The default is 30 minutes.  
                       Use 0 for XXX to mean that there is no timeout.  You can also stop
                       the server by pressing Control-C in the window where the program
                       is running.
                       
      -once --- accept one connection, and exit when that connection is closed.  The default 
                is to open a new listener after the connection is closed and wait for another 
                connection.
                
      -quiet --- suppress all output.
      
      -port XXX --- listen on port number XXX instead of on the default port (17071).
                    (Ordinarily, you will NOT need this option; just use the default.)
      
For example:

                java -jar MBNetServe.jar -quiet -processcount 0

To use the network in xMandelbrot, choose the "Configure Multiprocessing..." command
from the "Control" menu.  In the dialog box, check the option labeled "Enable Networking".
Then, use the "Add Newtork Host" button to add each computer where MBNetServe is running.
When you click this button, a small dialog will open where you can type in the host name
of a computer that is running the server.  (You can also enter an IP address, instead of
a host name.)  After adding the hosts, click the "Apply Config Now" button -- the network
is not activated until you press this button.  You should see "Connecting" next to each
host name in the list.  In a few seconds, this should change to "Connected."  If it changes
to "Connection Failed", or if it continues to say "Connected" for some time, then there is
something wrong -- check that the server is running and that the host names or IP addresses
are correct.  Even if some listed connections are not working, the program will still
use those that are connected.  (Note that the list of hosts that you have entered will
be saved for the next time you run the program, but networking will not automatically be
turned on when the program runs; you will have to enable it by hand each time you run
the program.)

Note: If your server computers are Linux or Mac OS computers that are configured to
accept ssh connetions, you can start up the server program remotely, using the ssh
command, instead of logging on to each individual computer to start the program.
If the computer's name is, for example, cslab1.hws.edu, you could use a command
such as:

              ssh -f cslab1.hws.edu java -jar MBNetServe.jar -quiet -processcount 0
              
The "-f" option means that the command will be run in the background (after asking
for your password, if required) so that you can continue to use the same command-line
window to give other commands.  This assumes that you have an account on cslab1.hws.edu
with the same user name as on the computer where you give this command.  It also assumes
that MBNetServe.jar is in your home directory on cslab1.hws.edu.  MBNetServe will shut
itself down after 30 minutes of inactivity, so you don't have to worry about shutting
it down.  However, if you do want to shut down the server remotely, you can do so
by using a special "-shutdown" option on the MBNetServe program.  For example, if you
are finished with the server that was started with the above command, you can use:

            java -jar MBNetServe.jar -shutdown cslab1.hws.edu
            
to shut down the server.  (This command will NOT work if the server is still being
used by xMandelbrot or MandelbrotCL.)




