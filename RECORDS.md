# JCLO with Java Records: Design Guide

## Overview

JCLO now fully supports Java records through a **two-phase initialization pattern**. This document explains the design philosophy, implementation details, and best practices for using records with JCLO.

## The Problem: Records Are Immutable

Java records (introduced in Java 16, finalized in Java 17) are immutable data classes:

```java
record Point(int x, int y) {}
```

Key characteristics:
- All fields are `private final`
- No field setters exist
- Instances cannot be modified after creation
- Thread-safe by design

JCLO, however, uses **reflection to mutate fields** during parsing:

```java
Field field = Point.class.getDeclaredField("x");
field.setAccessible(true);
field.set(pointInstance, 42);  // ❌ Fails for records - field is final!
```

This creates a fundamental incompatibility: JCLO requires mutability, records demand immutability.

## The Solution: Two-Phase Initialization

The solution is elegantly simple: **don't parse directly into a record**. Instead:

**Phase 1**: Parse into a **mutable class** (JCLO works here)
**Phase 2**: Convert to an **immutable record** (safe and clean)

```
┌──────────────────────────────────┐
│ Mutable Class (from Phase 1)      │
│ - String name = "app"            │
│ - int count = 5                  │
│ - boolean verbose = true         │
└──────────────────────────────────┘
           ↓ (Phase 2)
┌──────────────────────────────────┐
│ Immutable Record (final config)   │
│ record Config(String name,       │
│   int count, boolean verbose) {} │
└──────────────────────────────────┘
```

## Implementation Patterns

### Pattern 1: Basic Record Conversion

The simplest pattern for straightforward cases:

```java
// Mutable class for JCLO to populate
class AppArgsMutable {
    String name;
    int count = 1;
    boolean debug;
    String[] urls;
}

// Immutable record for your application
record AppArgs(String name, int count, boolean debug, String[] urls) {
    // Factory method converts mutable → immutable
    static AppArgs fromMutable(AppArgsMutable m) {
        return new AppArgs(m.name, m.count, m.debug, m.urls);
    }
}

// Usage
public static void main(String[] args) {
    // Phase 1: JCLO parses into mutable instance
    AppArgsMutable temp = new AppArgsMutable();
    new JCLO(temp).parse(args);

    // Phase 2: Convert to immutable record
    AppArgs config = AppArgs.fromMutable(temp);

    // Rest of app uses immutable config
    System.out.println("App: " + config.name());
    System.out.println("Count: " + config.count());
}
```

**Advantages:**
- ✅ Simple and straightforward
- ✅ Single conversion step
- ✅ Minimal boilerplate

**When to use:**
- Simple configuration with few fields
- No validation needed
- Direct field-to-field mapping

### Pattern 2: Record with Validation

Use the record's compact constructor to validate parsed values:

```java
class DatabaseArgsMutable {
    String host = "localhost";
    int port = 5432;
    int timeout = 30;
}

record DatabaseConfig(String host, int port, int timeout) {
    // Compact constructor validates immediately after construction
    public DatabaseConfig {
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("host cannot be null or blank");
        }
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("port must be 1-65535");
        }
        if (timeout < 1 || timeout > 300) {
            throw new IllegalArgumentException("timeout must be 1-300 seconds");
        }
    }

    static DatabaseConfig fromMutable(DatabaseArgsMutable m) {
        return new DatabaseConfig(m.host, m.port, m.timeout);
        // Compact constructor runs here, validation happens
    }
}

// Usage
DatabaseArgsMutable temp = new DatabaseArgsMutable();
new JCLO(temp).parse(commandLineArgs);

try {
    DatabaseConfig config = DatabaseConfig.fromMutable(temp);
    // Only reached if validation passed
    System.out.println("Connected to " + config.host() + ":" + config.port());
} catch (IllegalArgumentException e) {
    System.err.println("Invalid configuration: " + e.getMessage());
    System.exit(1);
}
```

**Advantages:**
- ✅ Validation happens automatically
- ✅ Impossible to create invalid config
- ✅ Clear error messages
- ✅ Fail-fast design

**When to use:**
- Configuration with constraints
- Values that need validation
- Want to catch errors early

### Pattern 3: Record with Computed Fields

Use the factory method to compute/transform values:

```java
class CachingArgsMutable {
    int maxSize;
    String evictionPolicy = "LRU"; // String for CLI parsing
    long ttlSeconds = 3600;
}

enum EvictionPolicy {
    LRU("LRU"), LFU("LFU"), FIFO("FIFO");
    final String displayName;
    EvictionPolicy(String displayName) { this.displayName = displayName; }
}

record CachingConfig(
    int maxSize,
    EvictionPolicy policy,
    long ttlMillis  // Converted from seconds
) {
    static CachingConfig fromMutable(CachingArgsMutable m) {
        return new CachingConfig(
            m.maxSize,
            EvictionPolicy.valueOf(m.evictionPolicy),  // String → Enum
            m.ttlSeconds * 1000  // Seconds → Milliseconds
        );
    }
}

// CLI: --maxSize=10000 --evictionPolicy=LFU --ttlSeconds=7200
```

**Advantages:**
- ✅ Transformation logic in one place
- ✅ Record has values in final form
- ✅ No post-initialization conversions needed

**When to use:**
- Values need type conversion (String → Enum, seconds → milliseconds, etc.)
- Want to normalize values
- Different representations in CLI vs. application

### Pattern 4: Record Composition

Build complex configurations from multiple records:

```java
record Database(String host, int port) {
    static Database fromMutable(DatabaseMutable m) {
        return new Database(m.host, m.port);
    }
}

record Logging(String level, String format) {
    static Logging fromMutable(LoggingMutable m) {
        return new Logging(m.level, m.format);
    }
}

class ApplicationArgsMutable {
    String appName;
    String dbHost = "localhost";
    int dbPort = 5432;
    String logLevel = "INFO";
    String logFormat = "text";
}

record ApplicationConfig(
    String appName,
    Database database,
    Logging logging
) {
    static ApplicationConfig fromMutable(ApplicationArgsMutable m) {
        return new ApplicationConfig(
            m.appName,
            new Database(m.dbHost, m.dbPort),
            new Logging(m.logLevel, m.logFormat)
        );
    }
}

// Usage: Strongly typed configuration hierarchy
ApplicationConfig config = ApplicationConfig.fromMutable(parsedArgs);
config.database().host();  // Type-safe access
config.logging().level();  // Clear structure
```

**Advantages:**
- ✅ Hierarchical structure
- ✅ Type-safe nested config
- ✅ Reusable record types
- ✅ Clear separation of concerns

**When to use:**
- Large configurations with logical groupings
- Want to share record types across projects
- Need type-safe nested structures

### Pattern 5: Sealed Records for Variants

Use sealed records to represent configuration variants:

```java
sealed interface Transport permits TcpTransport, HttpTransport {}

record TcpTransport(String host, int port) implements Transport {}
record HttpTransport(String url) implements Transport {}

class TransportArgsMutable {
    String transportType = "TCP";  // "TCP" or "HTTP"
    String tcpHost = "localhost";
    int tcpPort = 9000;
    String httpUrl;
}

record TransportConfig(Transport transport) {
    static TransportConfig fromMutable(TransportArgsMutable m) {
        Transport t = switch(m.transportType) {
            case "TCP" -> new TcpTransport(m.tcpHost, m.tcpPort);
            case "HTTP" -> new HttpTransport(m.httpUrl);
            default -> throw new IllegalArgumentException(
                "Unknown transport: " + m.transportType);
        };
        return new TransportConfig(t);
    }
}

// Usage with pattern matching
public void connect(TransportConfig config) {
    switch (config.transport()) {
        case TcpTransport tcp -> connectTcp(tcp.host(), tcp.port());
        case HttpTransport http -> connectHttp(http.url());
    }
}
```

**Advantages:**
- ✅ Type-safe variants
- ✅ Pattern matching support
- ✅ Exhaustiveness checking at compile time
- ✅ No nullable discriminator fields

**When to use:**
- Configuration has variants
- Want compile-time safety
- Need different fields per variant

## Design Philosophy

### Why This Approach Works

1. **Separation of Concerns**
   - Mutable class: concerned with CLI parsing
   - Immutable record: concerned with application logic

2. **Phase Clarity**
   - Phase 1 (Mutation): "Accept user input, populate fields"
   - Phase 2 (Conversion): "Validate, transform, finalize"

3. **Immutability Benefits**
   - Thread-safe by design
   - Hashable (usable in sets/maps)
   - Easier to reason about
   - Clear ownership

4. **Single Responsibility**
   - Mutable class: JCLO integration
   - Record: domain modeling
   - Factory method: conversion logic

### Trade-offs

**Pros:**
- ✅ Full record immutability
- ✅ Clear two-phase flow
- ✅ Familiar pattern (like builders)
- ✅ Validation at construction time
- ✅ Works with existing JCLO code
- ✅ No changes to JCLO needed

**Cons:**
- ❌ Slightly more boilerplate than direct mutable class
- ❌ Two types instead of one (mutable + record)
- ❌ Extra factory method

The boilerplate cost is minimal and the benefits of immutability are substantial.

## Best Practices

### 1. Naming Convention

Use a clear naming pattern:

```java
class ConfigMutable {}      // For JCLO
record Config(...) {}       // Immutable version
// OR
class ConfigArgs {}         // For JCLO
record ConfigSpec(...) {}   // Immutable specification
```

### 2. Factory Method Location

Place the factory method in the record (not the mutable class):

```java
// ✅ Good: Factory is closer to where it's needed
record AppConfig(...) {
    static AppConfig fromMutable(AppConfigMutable m) { ... }
}

// ❌ Avoid: Factory far from record definition
class AppConfigMutable {
    AppConfig toRecord() { ... }
}
```

### 3. Default Values

Set defaults in the mutable class, not the record:

```java
// ✅ Good: Record doesn't duplicate defaults
class AppArgsMutable {
    int timeout = 30;  // Default here
}
record AppConfig(int timeout) {}

// ❌ Bad: Duplicate defaults
class AppArgsMutable {
    int timeout = 30;
}
record AppConfig(int timeout) {
    public AppConfig {
        if (timeout == 0) timeout = 30;  // Don't repeat!
    }
}
```

### 4. Validation Strategy

```java
// ✅ Good: Validate in compact constructor
record Config(int port) {
    public Config {
        if (port < 1 || port > 65535) throw new ...;
    }
}

// ❌ Avoid: Validation after construction
AppConfig config = AppConfig.fromMutable(temp);
if (!isValid(config)) throw new ...;
```

### 5. Complex Transformations

For non-trivial conversions, add helper methods:

```java
// ✅ Good: Helper method for clarity
record ServerConfig(...) {
    static ServerConfig fromMutable(ServerArgsMutable m) {
        return new ServerConfig(
            m.host,
            parseLogLevel(m.logLevelString),  // Clear intent
            parseThreadPool(m.threadPoolConfig),
            buildTimeoutPolicy(m.timeout, m.retries)
        );
    }

    private static LogLevel parseLogLevel(String s) { ... }
    private static ThreadPool parseThreadPool(String s) { ... }
    private static TimeoutPolicy buildTimeoutPolicy(int t, int r) { ... }
}
```

### 6. Testing

Test both phases:

```java
@Test
public void testJCLOParsing() {
    // Phase 1: JCLO works correctly
    AppArgsMutable temp = new AppArgsMutable();
    new JCLO(temp).parse("--name=app --count=5".split(" "));
    assertEquals("app", temp.name);
    assertEquals(5, temp.count);
}

@Test
public void testRecordConversion() {
    // Phase 2: Conversion is correct
    AppArgsMutable temp = new AppArgsMutable();
    temp.name = "app";
    temp.count = 5;
    AppConfig config = AppConfig.fromMutable(temp);
    assertEquals("app", config.name());
    assertEquals(5, config.count());
}

@Test
public void testFullPipeline() {
    // Both phases together
    AppArgsMutable temp = new AppArgsMutable();
    new JCLO(temp).parse("--name=app --count=5".split(" "));
    AppConfig config = AppConfig.fromMutable(temp);
    assertEquals("app", config.name());
    assertEquals(5, config.count());
}

@Test
public void testValidation() {
    // Validation in compact constructor
    AppArgsMutable temp = new AppArgsMutable();
    temp.port = 99999;  // Invalid
    assertThrows(IllegalArgumentException.class,
        () -> AppConfig.fromMutable(temp));
}
```

## Migration Path

If you have existing code using mutable classes:

### Step 1: Keep Mutable Class As-Is

```java
class OldAppArgs {
    String name;
    int count;
}
```

### Step 2: Create Record Version

```java
record NewAppArgs(String name, int count) {
    static NewAppArgs fromMutable(OldAppArgs m) {
        return new NewAppArgs(m.name, m.count);
    }
}
```

### Step 3: Update Main Method

```java
public static void main(String[] args) {
    OldAppArgs temp = new OldAppArgs();
    new JCLO(temp).parse(args);
    NewAppArgs config = NewAppArgs.fromMutable(temp);  // Convert
    doWork(config);
}
```

### Step 4: Gradually Migrate Code

Replace usages of `OldAppArgs` with `NewAppArgs` gradually:

```java
// Before
void doWork(OldAppArgs args) {
    System.out.println(args.name);
}

// After
void doWork(NewAppArgs args) {
    System.out.println(args.name());  // Method call, not field
}
```

## Comparison with Other Approaches

### Approach 1: Direct Record (NOT POSSIBLE)

```java
record AppArgs(String name, int count) {}
AppArgs args = new AppArgs(...);
new JCLO(args).parse(commandLineArgs);  // ❌ Fails - records are immutable
```

### Approach 2: Builder Pattern

```java
record AppArgs(String name, int count) {
    static Builder builder() { return new Builder(); }
    
    static class Builder {
        String name;
        int count;
        AppArgs build() { return new AppArgs(name, count); }
    }
}

AppArgs.Builder b = AppArgs.builder();
new JCLO(b).parse(args);  // Requires builder to have JCLO-compatible fields
AppArgs config = b.build();
```

**Why two-phase is better:**
- ✅ Clearer separation
- ✅ Simpler (no Builder class needed)
- ✅ Familiar pattern (like adapters)
- ✅ Works with existing JCLO code

### Approach 3: Mutable Forever

```java
class AppArgs {  // Always mutable
    String name;
    int count;
}

new JCLO(appArgs).parse(commandLineArgs);
// Rest of app assumes AppArgs is final, but it's not!
```

**Why records are better:**
- ✅ Compile-time enforcement of immutability
- ✅ Thread-safe by design
- ✅ Clearer intent
- ✅ Auto-generated methods

## FAQ

**Q: Why not modify JCLO to work with records?**
A: Records are immutable by design and specification. JCLO's reflection-based mutation is fundamentally incompatible. This two-phase approach is the idiomatic Java solution.

**Q: Can I avoid the mutable class?**
A: Not with JCLO. You need a mutable class for field mutation. This is by design.

**Q: Is the factory method necessary?**
A: It's a best practice, but you can inline it: `new AppConfig(temp.name, temp.count)`. The factory method is clearer when conversions or validations are needed.

**Q: Can I use records with existing mutable JCLO code?**
A: Yes! That's the whole point. Gradually migrate to records without changing JCLO.

**Q: Is this pattern specific to JCLO?**
A: No, this is a general pattern for combining mutable parsers with immutable records. It works with any parser/builder that requires field mutation.

## Summary

The **two-phase initialization pattern** provides:
- ✅ Full record immutability
- ✅ JCLO compatibility
- ✅ Validation and transformation
- ✅ Clean, readable code
- ✅ Familiar design pattern
- ✅ Gradual migration path

This is the idiomatic way to use JCLO with Java records.
