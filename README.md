# JCLO
A Java Command Line Option package

[![CI](https://github.com/drsjb80/JCLO/actions/workflows/ci.yml/badge.svg)](https://github.com/drsjb80/JCLO/actions/workflows/ci.yml)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![Java 21](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)

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
            new JCLO(ea, args);

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
            JCLO jclo = new JCLO (main);
            jclo.setPrefix("JCLO");
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

One can also create aliased arguments via the setAliases method. It takes an
array of arrays, where each subarray is alias, arg.  For example:

    JCLO jclo = new JCLO(object);
    String aliases[][] = {{"alias", "a"}};
    jclo.setAliases(aliases);
    jclo.parse(args);

states that one can use --alias for --a.

## Using JCLO with Java Records (Java 16+)

JCLO supports Java records through a **two-phase approach**: parse into a mutable class, then
convert to an immutable record. This works because JCLO uses reflection to mutate fields, which
records don't allow.

### The Two-Phase Pattern

**Phase 1**: Mutable class for JCLO to populate
```
args → JCLO → MutableClass
```

**Phase 2**: Factory method converts to immutable record
```
MutableClass → fromMutable() → Record
```

Your application then uses the immutable record, ensuring configuration cannot be modified at runtime.

### Pattern 1: Basic Usage

    // Mutable class: JCLO works with this
    class AppArgsMutable {
        String name;
        int count = 1;
        boolean verbose;
    }

    // Immutable record: your final config
    record AppConfig(String name, int count, boolean verbose) {
        static AppConfig fromMutable(AppArgsMutable m) {
            return new AppConfig(m.name, m.count, m.verbose);
        }
    }

    public class MyApp {
        public static void main(String[] args) {
            AppArgsMutable temp = new AppArgsMutable();
            new JCLO(temp).parse(args);
            
            AppConfig config = AppConfig.fromMutable(temp);
            System.out.println("Name: " + config.name());
        }
    }

### Pattern 2: Validation

Use a compact constructor to validate immutable data:

    record Config(String host, int port) {
        public Config {  // Compact constructor
            if (host == null || host.isBlank()) {
                throw new IllegalArgumentException("host cannot be null");
            }
            if (port < 1 || port > 65535) {
                throw new IllegalArgumentException("Invalid port: " + port);
            }
        }

        static Config fromMutable(ConfigMutable m) {
            return new Config(m.host, m.port);  // Constructor validates
        }
    }

### Pattern 3: Default Values

Set defaults in the mutable class; they'll carry through to the record:

    class DatabaseArgsMutable {
        String host = "localhost";
        int port = 5432;
        String user;
        String password;
    }

    record DatabaseConfig(String host, int port, String user, String password) {
        static DatabaseConfig fromMutable(DatabaseArgsMutable m) {
            return new DatabaseConfig(m.host, m.port, m.user, m.password);
        }
    }

### Pattern 4: Pattern Matching

Records enable Java's pattern matching (Java 16+):

    AppConfig config = parseArgs(args);
    switch (config) {
        case AppConfig(var name, var count, true) -> 
            System.out.println("Verbose: " + name);
        case AppConfig(var name, var count, false) ->
            System.out.println("Quiet: " + name);
    }

### Benefits of Records

- **Immutability**: Config cannot be modified after creation
- **Thread-safe**: Immutable objects are safe across threads
- **Less boilerplate**: Auto-generated `equals()`, `hashCode()`, `toString()`
- **Pattern matching**: More expressive control flow

### Migrating Existing Code

If you have mutable JCLO classes you want to convert:

1. Rename your mutable class to `NameMutable`
2. Create a new record with the same fields
3. Add a `static fromMutable()` factory method
4. Update your main method to do the two-phase parse
5. Gradually replace mutable class usage with the record

This keeps backward compatibility while moving to immutable configs.

