package edu.msudenver.cs.JCLO;

import edu.msudenver.cs.jclo.JCLO;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Arrays;

/**
 * Test cases demonstrating JCLO with Java Records (Java 16+).
 *
 * This test suite shows the recommended pattern for using JCLO with immutable
 * records: create a mutable class for JCLO to parse into, then convert to
 * an immutable record via a factory method.
 */
public class TestJCLORecords {

    // ===== Pattern 1: Simple Mutable Class → Record =====

    class SimpleArgsMutable {
        String name;
        int count = 1;
        boolean verbose;
        String[] additional;
    }

    record SimpleArgs(String name, int count, boolean verbose, String[] additional) {
        static SimpleArgs fromMutable(SimpleArgsMutable m) {
            return new SimpleArgs(m.name, m.count, m.verbose, m.additional);
        }
    }

    @Test
    public void recordSimpleConversion() {
        SimpleArgsMutable temp = new SimpleArgsMutable();
        JCLO jclo = new JCLO(temp);
        jclo.parse(new String[]{"--name=myapp", "--count=5", "--verbose", "http://example.com"});

        SimpleArgs args = SimpleArgs.fromMutable(temp);
        assertEquals("myapp", args.name());
        assertEquals(5, args.count());
        assertTrue(args.verbose());
        assertNotNull(args.additional());
        assertEquals(1, args.additional().length);
        assertEquals("http://example.com", args.additional()[0]);
    }

    @Test
    public void recordPreservesDefaults() {
        SimpleArgsMutable temp = new SimpleArgsMutable();
        JCLO jclo = new JCLO(temp);
        jclo.parse(new String[]{"--name=myapp"});

        SimpleArgs args = SimpleArgs.fromMutable(temp);
        assertEquals("myapp", args.name());
        assertEquals(1, args.count()); // Default from mutable class
        assertFalse(args.verbose());
    }

    // ===== Pattern 2: Record with Validation =====

    class ValidatedArgsMutable {
        String host = "localhost";
        int port = 8080;
    }

    record ValidatedArgs(String host, int port) {
        public ValidatedArgs {
            if (host == null || host.isBlank()) {
                throw new IllegalArgumentException("host cannot be null or blank");
            }
            if (port < 1 || port > 65535) {
                throw new IllegalArgumentException("port must be 1-65535");
            }
        }

        static ValidatedArgs fromMutable(ValidatedArgsMutable m) {
            return new ValidatedArgs(m.host, m.port);
        }
    }

    @Test
    public void recordValidationAcceptsValidValues() {
        ValidatedArgsMutable temp = new ValidatedArgsMutable();
        JCLO jclo = new JCLO(temp);
        jclo.parse(new String[]{"--host=example.com", "--port=3000"});

        ValidatedArgs args = ValidatedArgs.fromMutable(temp);
        assertEquals("example.com", args.host());
        assertEquals(3000, args.port());
    }

    @Test
    public void recordValidationRejectsInvalidPort() {
        ValidatedArgsMutable temp = new ValidatedArgsMutable();
        JCLO jclo = new JCLO(temp);
        jclo.parse(new String[]{"--port=99999"});

        assertThrows(IllegalArgumentException.class, () -> {
            ValidatedArgs.fromMutable(temp); // Should throw
        });
    }

    @Test
    public void recordValidationRejectsBlankHost() {
        ValidatedArgsMutable temp = new ValidatedArgsMutable();
        temp.host = "";
        assertThrows(IllegalArgumentException.class, () -> {
            ValidatedArgs.fromMutable(temp); // Should throw
        });
    }

    // ===== Pattern 3: Record with Collections =====

    class DatabaseArgsMutable {
        String[] hosts = {};
        int[] ports = {};
        String user;
        String password;
        int timeout = 30;
    }

    record DatabaseConfig(String[] hosts, int[] ports, String user, String password, int timeout) {
        static DatabaseConfig fromMutable(DatabaseArgsMutable m) {
            return new DatabaseConfig(m.hosts, m.ports, m.user, m.password, m.timeout);
        }
    }

    @Test
    public void recordWithArrayFields() {
        DatabaseArgsMutable temp = new DatabaseArgsMutable();
        JCLO jclo = new JCLO(temp);
        jclo.parse(new String[]{
            "--hosts=host1", "--hosts=host2", "--hosts=host3",
            "--ports=5432", "--ports=5433", "--ports=5434",
            "--user=admin", "--password=secret"
        });

        DatabaseConfig config = DatabaseConfig.fromMutable(temp);
        assertArrayEquals(new String[]{"host1", "host2", "host3"}, config.hosts());
        assertArrayEquals(new int[]{5432, 5433, 5434}, config.ports());
        assertEquals("admin", config.user());
        assertEquals("secret", config.password());
        assertEquals(30, config.timeout()); // Default
    }

    // ===== Pattern 4: Immutability Verification =====

    @Test
    public void recordIsImmutable() {
        SimpleArgsMutable temp = new SimpleArgsMutable();
        JCLO jclo = new JCLO(temp);
        jclo.parse(new String[]{"--name=app", "--count=3"});

        SimpleArgs args = SimpleArgs.fromMutable(temp);

        // Records should not allow field assignment
        // This test documents that records are immutable
        assertEquals("app", args.name());
        assertEquals(3, args.count());

        // Modifying the mutable doesn't affect the record
        temp.name = "modified";
        assertEquals("app", args.name()); // Still original
    }

    // ===== Pattern 5: Record with Complex Defaults =====

    class ComplexArgsMutable {
        String logLevel = "INFO";
        int retries = 3;
        boolean caching = true;
        String[] excludedPaths = new String[0];
    }

    record ComplexConfig(String logLevel, int retries, boolean caching, String[] excludedPaths) {
        static ComplexConfig fromMutable(ComplexArgsMutable m) {
            return new ComplexConfig(m.logLevel, m.retries, m.caching, m.excludedPaths);
        }
    }

    @Test
    public void recordWithComplexDefaults() {
        ComplexArgsMutable temp = new ComplexArgsMutable();
        JCLO jclo = new JCLO(temp);
        jclo.parse(new String[]{"--logLevel=DEBUG", "--caching=false"});

        ComplexConfig config = ComplexConfig.fromMutable(temp);
        assertEquals("DEBUG", config.logLevel());
        assertEquals(3, config.retries()); // Default
        assertFalse(config.caching());
        assertArrayEquals(new String[0], config.excludedPaths()); // Default
    }

    // ===== Pattern 6: Enum Support in Records =====

    enum Environment {DEVELOPMENT, STAGING, PRODUCTION}

    class EnumArgsMutable {
        Environment env = Environment.DEVELOPMENT;
        String region;
    }

    record EnumConfig(Environment env, String region) {
        static EnumConfig fromMutable(EnumArgsMutable m) {
            return new EnumConfig(m.env, m.region);
        }
    }

    @Test
    public void recordWithEnumField() {
        EnumArgsMutable temp = new EnumArgsMutable();
        JCLO jclo = new JCLO(temp);
        jclo.parse(new String[]{"--env=PRODUCTION", "--region=us-west"});

        EnumConfig config = EnumConfig.fromMutable(temp);
        assertEquals(Environment.PRODUCTION, config.env());
        assertEquals("us-west", config.region());
    }

    // ===== Pattern 7: Record Composition =====

    class DatabaseMutable {
        String host = "localhost";
        int port = 5432;
    }

    record Database(String host, int port) {
        static Database fromMutable(DatabaseMutable m) {
            return new Database(m.host, m.port);
        }
    }

    class ServerArgsMutable {
        String name;
        int serverPort = 8080;
        String dbHost = "localhost";
        int dbPort = 5432;
    }

    record ServerConfig(String name, int serverPort, Database database) {
        static ServerConfig fromMutable(ServerArgsMutable m) {
            return new ServerConfig(
                m.name,
                m.serverPort,
                new Database(m.dbHost, m.dbPort)
            );
        }
    }

    @Test
    public void recordComposition() {
        ServerArgsMutable temp = new ServerArgsMutable();
        JCLO jclo = new JCLO(temp);
        jclo.parse(new String[]{
            "--name=myserver",
            "--serverPort=9000",
            "--dbHost=db.example.com",
            "--dbPort=3306"
        });

        ServerConfig config = ServerConfig.fromMutable(temp);
        assertEquals("myserver", config.name());
        assertEquals(9000, config.serverPort());
        assertEquals("db.example.com", config.database().host());
        assertEquals(3306, config.database().port());
    }

    // ===== Pattern 8: ToString and Equals from Records =====

    @Test
    public void recordAutoGeneratedMethods() {
        SimpleArgsMutable temp1 = new SimpleArgsMutable();
        JCLO jclo1 = new JCLO(temp1);
        jclo1.parse(new String[]{"--name=app", "--count=5", "--verbose"});
        SimpleArgs args1 = SimpleArgs.fromMutable(temp1);

        SimpleArgsMutable temp2 = new SimpleArgsMutable();
        JCLO jclo2 = new JCLO(temp2);
        jclo2.parse(new String[]{"--name=app", "--count=5", "--verbose"});
        SimpleArgs args2 = SimpleArgs.fromMutable(temp2);

        // Records auto-generate equals() (with same field values)
        assertEquals(args1, args2);

        // Records auto-generate toString()
        String str = args1.toString();
        assertTrue(str.contains("app"));
        assertTrue(str.contains("5"));
        assertTrue(str.contains("true"));
    }
}
