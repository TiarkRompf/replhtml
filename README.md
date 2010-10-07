Scala REPL HTML Interface
=========================

This project provides a Scala REPL with a GUI inspired by Mathematica worksheets. Commands can be edited and deleted, and new commands can be inserted anywhere. Whole worksheets can be re-evaluated with one click. Sessions can be saved and restored.

The frontend is just an HTML page that communicates with a backend servlet via web sockets. The backend servlet does the actual command execution. The servlet is not multi-user save; the program is meant to be run on a single computer.

**CAVEAT**: Right now, this is all highly experimental. And it has been thorougly tested only in Safari.

How to Run it
-------------

After downloading, use SBT to build

      sbt update
      sbt compile

and then run the REPL servlet within an embedded Jetty server:

      sbt run

Then open the html file that contains the frontend in your browser:

      src/main/webapp/index.html



Accessing Application Classes
-----------------------------

The available classes are defined by two system properties, `replhtml.class.path` and `replhtml.extra.class.path`. By default, the former contains the Scala library and compiler jars while the latter can be freely used to load additional code.

For example, the following command:

      sbt 'set replhtml.extra.class.path /Users/myself/projects/MyGreatApp/classes/' run

will start the REPL with your application's classes loaded from the specified path, much like `scala -cp` would.

With the backend running, point your browser to

      src/main/webapp/index.html

and enjoy!


Things to Try
-------------

Use the - and + buttons to remove and insert commands. Hit tab and shift-tab to navigate up and down. Turn on completion (this will display available members but not actually complete your typing). Save and restore sessions.

There is some limited support for interacting with the HTML environment. Text output that begins with `<js>` will be interpreted as JavaScript code. The following code adds a button to the HTML page that displays an alert box when clicked:

      def js(code: String) = println("<js>"+code)
      js("$('#container').append($('<button>Click me!</button>').click(function() { alert('hey!') }))")

The `:power` mode is enabled by default. Use `repl` to access the interpreter object, or `servlet` to access the servlet.
