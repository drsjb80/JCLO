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

## Using JCLO with Java Records (Java 16+)

JCLO now fully supports Java records, which are immutable data classes. Since JCLO uses
reflection to mutate fields, it cannot work directly with record instances (records are immutable).
Instead, use a **two-phase approach**: parse into a mutable class, then convert to your record.

### Pattern 1: Simple Record Conversion

Create a mutable class for JCLO to work with, then convert to your immutable record:

    import edu.msudenver.cs.jclo.JCLO;

    // Mutable class: JCLO will populate this
    class AppArgsMutable {
        String name;
        int count = 1;
        boolean verbose;
        String[] urls;
    }

    // Immutable record: your final config
    record AppConfig(String name, int count, boolean verbose, String[] urls) {
        static AppConfig fromMutable(AppArgsMutable m) {
            return new AppConfig(m.name, m.count, m.verbose, m.urls);
        }
    }

    public class MyApp {
        public static void main(String[] args) {
            AppArgsMutable temp = new AppArgsMutable();
            JCLO jclo = new JCLO(temp);
            jclo.parse(args);

            AppConfig config = AppConfig.fromMutable(temp);
            System.out.println("Name: " + config.name());
            System.out.println("Count: " + config.count());
            System.out.println("Verbose: " + config.verbose());
            System.out.println("URLs: " + java.util.Arrays.toString(config.urls()));
        }
    }

Example run:

    $ java MyApp --name=example --count=5 --verbose http://example.com http://other.com
    Name: example
    Count: 5
    Verbose: true
    URLs: [http://example.com, http://other.com]

### Pattern 2: Validation with Records

Since your record is immutable, use a compact or canonical constructor for validation:

    record Config(String host, int port) {
        // Compact constructor for validation
        public Config {
            if (host == null || host.isBlank()) {
                throw new IllegalArgumentException("host cannot be null or blank");
            }
            if (port < 1 || port > 65535) {
                throw new IllegalArgumentException("port must be 1-65535, got " + port);
            }
        }

        static Config fromMutable(ConfigMutable m) {
            return new Config(m.host, m.port);  // Triggers compact constructor
        }
    }

### Pattern 3: Default Values in Records

Records don't support field initialization like mutable classes. Instead, use a factory method
or the mutable class to set defaults:

    // Mutable class with defaults
    class DatabaseArgsMutable {
        String host = "localhost";
        int port = 5432;
        String user;
        String password;
        int timeout = 30;
    }

    record DatabaseConfig(String host, int port, String user, String password, int timeout) {
        static DatabaseConfig fromMutable(DatabaseArgsMutable m) {
            return new DatabaseConfig(m.host, m.port, m.user, m.password, m.timeout);
        }
    }

### Pattern 4: Why Records are Better (When Conversion is Worth It)

Benefits of using records after JCLO parsing:

1. **Immutability**: Configuration cannot be accidentally modified at runtime
2. **Thread Safety**: Immutable objects are inherently thread-safe
3. **Conciseness**: Records are much more concise than mutable classes
4. **Auto-generated methods**: Records generate `equals()`, `hashCode()`, and `toString()`
5. **Pattern Matching**: Work with records in Java's pattern matching (Java 16+, enhanced in 17+)

Example showing pattern matching:

    AppConfig config = parseArgs(args);
    switch (config) {
        case AppConfig(var name, var count, true, var urls) -> 
            System.out.println("Verbose mode: " + name + " with " + count + " URLs");
        case AppConfig(var name, var count, false, var urls) ->
            System.out.println("Quiet mode: " + name);
    }

### The Two-Phase Design

This two-phase approach combines the best of both worlds:

**Phase 1 (Mutable)**: JCLO parses command-line args into a mutable instance
```
args → JCLO → AppArgsMutable
```

**Phase 2 (Immutable)**: Factory method converts to record
```
AppArgsMutable → fromMutable() → AppConfig (record)
```

The rest of your application uses the immutable `AppConfig` record, ensuring that
configuration cannot be accidentally modified.

### Comparison: Mutable vs Record

**Mutable approach** (simpler if you don't need immutability):
```java
class AppArgs {
    String name;
    int count;
    boolean verbose;
}
AppArgs args = new AppArgs();
new JCLO(args).parse(commandLineArgs);
// Use: args.name, args.count, args.verbose
```

**Record approach** (better for thread-safe, immutable config):
```java
class AppArgsMutable {
    String name;
    int count;
    boolean verbose;
}
record AppArgs(String name, int count, boolean verbose) {
    static AppArgs fromMutable(AppArgsMutable m) {
        return new AppArgs(m.name, m.count, m.verbose);
    }
}
AppArgsMutable temp = new AppArgsMutable();
new JCLO(temp).parse(commandLineArgs);
AppArgs args = AppArgs.fromMutable(temp);
// Use: args.name(), args.count(), args.verbose()
```

The extra conversion step in Phase 2 is a small price for the benefits of immutability
and all the boilerplate code that records automatically generate.

### Migration Path: Mutable to Records

If you have existing code using mutable classes with JCLO:

1. Keep your mutable class as-is (rename to `NameMutable`)
2. Create a new record with the same fields
3. Add a `fromMutable()` static factory method
4. Update your main method to do the two-phase parse
5. Gradually replace references to the mutable class with the record

This approach ensures backward compatibility while leveraging records going forward.

