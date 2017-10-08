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
