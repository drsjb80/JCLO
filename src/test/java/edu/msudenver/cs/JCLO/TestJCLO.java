import edu.msudenver.cs.jclo.JCLO;

import org.junit.Assert;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

class JCLOArgs
{
    int font__size;     // __ -> -
    String font__name;
    String font__style;
    boolean debug;
    boolean d;
    boolean _1;         // _ -> 
    boolean help;
    String Djava_$util_$logging_$config_$file;  // _$ -> .

    String additional[];
}

class JCLOnly
{
    int font__size;
    String font__name;
    String font__style;
    boolean JCLOdebug;
    boolean JCLO_1;

    String JCLOadditional[];
}

class Equivalent
{
    int one, two;
    String equivalent[][] = {{"o", "one"}, {"t", "two" }};
}

enum Levels {SEVERE, ERROR, WARNING, INFO, DEBUG, TRACE}

class TestLevels {
    Levels level;
}

public class TestJCLO
{
    JCLOArgs jcloargs = new JCLOArgs();
    JCLO jclo = new JCLO (jcloargs);

    @Test
    public void single_booleans()
    {
        Map<String, Boolean> hm = new HashMap<String, Boolean>();
        hm.put ("", false);
        hm.put ("--debug", true);
        hm.put ("--debug=FALSE", false);
	    hm.put ("--debug=true", true);
	    hm.put ("--debug=yes", true);
	    hm.put ("--debug=YES", true);
	    hm.put ("--debug=false", false);
	    hm.put ("--debug=no", false);
	    hm.put ("--debug=NO", false);
	    hm.put ("--1", true);

        for (Object o : hm.entrySet()) {
            Map.Entry me = (Map.Entry) o;
            String foo[] = new String[1];
            foo[0] = (String) me.getKey();

            jclo.parse(foo);

            Assert.assertEquals(jcloargs.debug, me.getValue());
        }
    }

    @Test
    public void test_strings()
    {
	    jclo.parse (new String[]{"--font-size=10", "--font-style=BOLD",
            "--font-name=foo", "--debug"});
        Assert.assertEquals (jcloargs.font__size, 10);
        Assert.assertEquals (jcloargs.font__style, "BOLD");
        Assert.assertEquals (jcloargs.font__name, "foo");
        Assert.assertTrue (jcloargs.debug);
    }

    @Test
    public void test_additionals()
    {
	    jclo.parse (new String[]{"--debug", "one", "two", "three"});
        Assert.assertTrue (jcloargs.debug);
        Assert.assertTrue (jcloargs.additional[0].equals("one"));
        Assert.assertTrue (jcloargs.additional[1].equals("two"));
        Assert.assertTrue (jcloargs.additional[2].equals("three"));

	    jclo.parse (new String[]{"zero", "one", "two"});
        Assert.assertTrue (jcloargs.additional[0].equals("zero"));
        Assert.assertTrue (jcloargs.additional[1].equals("one"));
        Assert.assertTrue (jcloargs.additional[2].equals("two"));
    }

    @Test
    public void test_dotted()
    {
	    jclo.parse (new String[]
            {"-Djava.util.logging.config.file=MethodFilter.props"});
        Assert.assertEquals(jcloargs.Djava_$util_$logging_$config_$file,
            "MethodFilter.props");
    }

    @Test
    public void multiple_args()
    {
	    jclo.parse (new String[]{"-debug", "true"});
        Assert.assertTrue(jcloargs.debug);
	    jclo.parse (new String[]{"-font-size", "10"});
        Assert.assertEquals (jcloargs.font__size, 10);
    }

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void exeception()
    {
        exception.expect(IllegalArgumentException.class);
	    jclo.parse (new String[]{"--none=none"});
    }

    @Test
    public void test_prefix()
    {
        JCLOnly jclonly = new JCLOnly();
        JCLO jclo = new JCLO ("JCLO", jclonly);

	    jclo.parse (new String[]{"-debug", "true"});
        Assert.assertTrue(jclonly.JCLOdebug);

	    jclo.parse (new String[]{"zero", "one", "two"});
        String additionals[] = jclonly.JCLOadditional;
        Assert.assertTrue(additionals[0].equals("zero"));
        Assert.assertTrue(additionals[1].equals("one"));
        Assert.assertTrue(additionals[2].equals("two"));

	    jclo.parse (new String[]{"-1"});
        Assert.assertTrue(jclonly.JCLO_1);
    }

    @Test
    public void test_help()
    {
        Assert.assertEquals (jclo.usage(), 
            "-1\n" +
            "-Djava.util.logging.config.file String\n"+
            "-d\n"+
            "-debug\n"+
            "-font-name String\n"+
            "-font-size int\n"+
            "-font-style String\n"+
            "-help\n");
    }

    @Test
    public void test_enum()
    {
        TestLevels l = new TestLevels();
        l.level = Levels.SEVERE;
        JCLO jclo = new JCLO(l);
        jclo.parse(new String[]{"-level", "SEVERE"});
        System.out.println (l.level);
        System.out.println (jclo.usage());
    }

    /*
    @Test
    public void test_equivalent()
    {
        Equivalent equiv = new Equivalent();
        JCLO jclo = new JCLO (equiv);

	    jclo.parse (new String[]{"-o", "10"});
        Assert.assertEquals(equiv.one, 10);
    }
    */
}
