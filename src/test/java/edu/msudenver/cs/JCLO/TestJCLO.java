import edu.msudenver.cs.jclo.JCLO;

import org.junit.Assert;
import org.junit.Test;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

class JCLOArgs
{
    int font__size;
    String font__name;
    String font__style;
    boolean debug;
    boolean d;
    boolean _1;
    String Accept[];
    int ints[];
    boolean help;
    String Djava_$util_$logging_$config_$file;

    String additional[];
}

public class TestJCLO
{

    @Test
    public void double_dash()
    {
        HashMap<String, Boolean> hm = new HashMap<String, Boolean>();
        hm.put ("", false);
        hm.put ("--debug", true);
        hm.put ("--debug=FALSE", false);
	    hm.put ("--debug=true", true);
	    hm.put ("--debug=yes", true);
	    hm.put ("--debug=YES", true);
	    hm.put ("--debug=false", false);
	    hm.put ("--debug=no", false);
	    hm.put ("--debug=NO", false);
	    // hm.put ("--debug", "one", "two", "three");

        JCLOArgs jcloargs = new JCLOArgs();
        JCLO jclo = new JCLO (jcloargs);

        Iterator iterator = hm.entrySet().iterator();
        while(iterator.hasNext())
        {
            Map.Entry me = (Map.Entry) iterator.next();
            String foo[] = new String[1];
            foo[0] = (String) me.getKey();

            jclo.parse (foo);

            Assert.assertEquals (jcloargs.debug, me.getValue());
        }
    }
}
