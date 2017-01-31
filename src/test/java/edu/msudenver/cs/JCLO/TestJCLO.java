import edu.msudenver.cs.jclo.JCLO;

import org.junit.Assert;
import org.junit.Test;

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
        JCLOArgs jcloargs = new JCLOArgs();
        JCLO jclo = new JCLO (jcloargs);

        jclo.parse (new String[]{"--debug"});
        Assert.assertEquals (jcloargs.debug, true);

        jclo.parse (new String[]{"--debug=FALSE"});
        Assert.assertEquals (jcloargs.debug, false);
    }
}
