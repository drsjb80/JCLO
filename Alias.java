import edu.msudenver.cs.jclo.JCLO;

public class Alias
{
    private static int a;

    public static void main (String args[])
    {
        String alias[][] = {{"alias", "a"}};
        JCLO jclo = new JCLO (new Alias(), alias);

        String test[] = {"--alias=5"};
        jclo.parse (test);

        System.out.println ("a = " + a);
    }
}
