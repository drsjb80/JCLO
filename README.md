# JCLO
A Java Command Line Option package

This package takes an object, uses reflection to find varibles in that
object, and creates a command line parser that uses those variable names
and types to give values to those variables. This dramtically reduces the
effort required to deal with command line options. It parses either the
double dash (--this=that) or single dash (-this that) styles. If there is a
String array variable named "additional", any options after the dashed
options are placed in it. It throws exceptions for nonexistant options and
for number format errors. It can be downloaded from:
https://github.com/drsjb80/JCLO

## Examples
Here is a simple example using a class devoted to command line options.

    import edu.msudenver.cs.jclo.JCLO;

    class ExampleArgs
    {
        int a;
        boolean b;
        float c;
        String d;
        String[] additional;
    }

    public class Example
    {
        public static void main (String args[])
        {
            ExampleArgs ea = new ExampleArgs();
            JCLO jclo = new JCLO (ea);
            jclo.parse (args);

            System.out.println ("a = " + ea.a);
            System.out.println ("b = " + ea.b);
            System.out.println ("c = " + ea.c);
            System.out.println ("d = " + ea.d);
            System.out.println ("additional = " + 
                java.util.Arrays.toString (ea.additional));
        }
    }

Here are several example runs: one with good options, one with a
nonexistant option, and one with a number format error.

    $ java Example --a=5 --b --c=3.141592 --d=this that theother
    a = 5
    b = true
    c = 3.141592
    d = this
    additional = [that, theother]

    $ java Example --e=5
    Exception in thread "main" java.lang.IllegalArgumentException: No such
    option: "e"

    $ java Example --c=true
    Exception in thread "main" java.lang.NumberFormatException: For input
    string: "true"

Here is one that only uses variables prefixed with "JCLO" and that accepts
single dash options.

    import edu.msudenver.cs.jclo.JCLO;

    public class Main
    {
        private int JCLOa;
        private boolean JCLOb;
        private int c;
        private boolean d;

        public String toString()
        {
            return ("JCLOa = " + JCLOa + " JCLOb = " + JCLOb +
            " c = " + c + " d = " + d);
        }

        public static void main (String args[])
        {
            Main main = new Main();
            System.out.println ("before: " + main);
            JCLO jclo = new JCLO (main, "JCLO");
            jclo.parse (args);
            System.out.println (jclo.usage());
            System.out.println ("after: " + main);
        }
    }

An example run.

    $ java Main -a 5 -b
    before: JCLOa = 0 JCLOb = false c = 0 d = false
    Command line options:
        -b
        -a int
    after: JCLOa = 5 JCLOb = true c = 0 d = false

One can also create aliased arguments via the constructor. It can take an
array of arrays, where each subarray is alias, arg.  For example:

    String aliases[][] = {{"alias", "a"}};

states that one can use --alias for --a.

