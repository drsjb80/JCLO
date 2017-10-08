package edu.msudenver.cs.jclo;

import java.lang.reflect.Field;
import java.lang.reflect.Array;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.io.FileInputStream;

/**
 * This class is used to parse command-line arguments based on a the
 * variables with an object.  Each variable in the class specifies a
 * command-line argument that can be accepted; the name of the variable
 * becomes the name of the command-line argument.  As Java does not allow
 * dashes '-' in variables, use two underscores '__' if you want a dash in
 * an argument.  Also, if you want to have a numeric argument (e.g.: '-1'),
 * start the variable name with a single underscore.  JCLO uses reflection
 * to determine the type of each variable in the passed object and sets the
 * values in the object passed to it via parsing the command line.  If you
 * include a String array named "additional", all non-dashed arguments will
 * be placed in it.
 *
 * @author Steve Beaty (beatys@mscd.edu) @version    $Id:
 *         JCLO.java,v 1.5 2007/11/01 16:43:12 beaty Exp beaty $
 */

public class JCLO {
    private final Field[] fields;
    private final Object object;
    private boolean doubleDashes;
    private boolean hasEquals;
    private String prefix = "";
    private final String[][] aliases;

    /**
     * A constructor that takes the Object that contains the variables
     * acceptable on a command line.  Call parse (String) to do the actual
     * parsing.
     *
     * @param object where the variables/arguments are
     */
    public JCLO(Object object) {
        this(null, object, null);
    }

    /**
     * A constructor that takes the Object that contains the variables
     * acceptable on a command line.  Call parse (String) to do the actual
     * parsing.
     *
     * @param object  where the variables/arguments are
     * @param aliases if there are CLO aliases
     */
    public JCLO(Object object, String[][] aliases) {
        this(null, object, aliases);
    }

    /**
     * A constructor that takes the Object that contains the variables
     * acceptable on a command line.  Call parse (String) to do the actual
     * parsing.
     *
     * @param object where the variables/arguments are
     * @param prefix if all CLO variables start with a prefix
     */
    public JCLO(String prefix, Object object) {
        this(prefix, object, null);
    }

    /**
     * A constructor that takes an Object, a prefix, and a boolean that
     * specifies whether to accept single or double dashes; call parse
     * (String) to do the actual parsing.
     *
     * @param object  where the variables/arguments are
     * @param prefix  the String CLO's start with, if any
     * @param aliases if there are CLO aliases
     */
    public JCLO(String prefix, Object object, String[][] aliases) {
        this.object = object;
        this.prefix = prefix;
        this.aliases = aliases;

        fields = object.getClass().getDeclaredFields();

        for (Field field : fields) {
            field.setAccessible(true);
        }
    }

    /**
     * Just a simple method to put the try/catch in one place.
     *
     * @param f the Field to get the value of
     * @return the Object with the value
     */
    private Object getObject(Field f) {
        try {
            return (f.get(object));
        } catch (java.lang.IllegalAccessException iae) {
            iae.printStackTrace();
            System.exit(1);
        }

        return (null);
    }

    private Field getField(String key) {
        if (prefix != null)
            key = prefix + key;

        for (Field field : fields) {
            String name = field.getName();

            if (name.equals(key))
                return (field);
        }

        return (null);
    }

    /**
     * Get the current value of the variable in the object
     *
     * @param key the variable name
     * @return an Object with the value

    private Object getValue(String key) {
        Field f = getField(key);

        if (f == null) {
            System.out.println("Field not found: " + key);
            return (null);
        }

        return (getObject(f));
    }
    */

    /**
     * Just a simple method to put the try/catch in one place.
     *
     * @param f the Field to set the value of
     * @param o the Object with the value
     */
    private void setObject(Field f, Object o) {
        try {
            f.set(object, o);
        } catch (java.lang.IllegalAccessException iae) {
            iae.printStackTrace();
        }
    }

    private String getArrayType(Class type) {
        return (type.getComponentType().toString().replaceFirst
                ("class.*\\.", ""));
    }

    /**
     * An external representation of the object
     *
     * @return a formatted version of this object
     */
    @Override
    public String toString() {
        String r = "";
        boolean first = true;

        for (Field field : fields) {
            String key = field.getName();
            Class type = field.getType();
            Object object = getObject(field);

            if (!first) r += "\n";
            first = false;

            if (type.isArray()) {
                r += getArrayType(type) + "[]" + ": " + key + " = ";
                r += java.util.Arrays.toString((Object[]) object);
            } else {
                r += type.toString().replaceFirst("class java.lang.", "") +
                        ": " + key + " = " + object;
            }
        }

        return (r);
    }

    private String getUsageType(Class type) {
        // System.out.println(type.getName());
        if (type.getName().equals("boolean")) {
            return (doubleDashes ? "[=boolean]" : "");
        } else if (type.isArray()) {
            return ((doubleDashes ? "=" : " ") + getArrayType(type) + "...");
        } else {
            return ((doubleDashes ? "=" : " ") +
                    type.getName().replaceFirst("java.lang.", ""));
        }
    }

    /**
     * Create and usage message for the acceptable command line variables.
     *
     * @return a String that specifies acceptable options
     */
    public String usage() {
        List<String> a = new ArrayList<>();

        for (Field field : fields) {
            String key = field.getName();
            Class type = field.getType();

            if (key.equals("additional"))
                continue;

            if (prefix != null) {
                if (!key.startsWith(prefix))
                    continue;
                else
                    key = key.replaceFirst("^" + prefix, "");
            }

            if (Modifier.isFinal(field.getModifiers()))
                continue;

            key = key.replaceFirst("^_([0-9])", "$1");
            key = key.replaceAll("__", "-");
            key = key.replaceAll("_\\$", ".");

            a.add((doubleDashes ? "--" : "-") + key +
                    getUsageType(type) + "\n");
        }

        String r = "";

        Collections.sort(a);

        for (String anA : a)
            if (anA != null)
                r += anA;

        return (r);
    }

    private void parseAdditional(String args[], int i) {
        int number = args.length - i;
        String add[] = new String[number];

        for (int j = 0; j < number; j++, i++) {
            add[j] = args[i];
        }

        Field f = getField("additional");
        if (f != null) {
            setObject(f, add);
        } else {
            System.err.println("No varible 'additional' found");
        }
    }

    /**
     * Add to object o to the end of the array contained in field and
     * return the resulting array.
     *
     * @param field the field in the object
     * @param o     the new object to be placed at the end
     * @return a formatted version of this object
     */
    private Object addToArray(Field field, Object o) {
        Object ret;
        Object orig = getObject(field);
        Class componentType = field.getType().getComponentType();

        if (orig == null) {   // the array is empty
            ret = Array.newInstance(componentType, 1);
            Array.set(ret, 0, o);
        } else {
            int length = Array.getLength(orig);

            ret = Array.newInstance(componentType, length + 1);

            int j;
            for (j = 0; j < length; j++)
                Array.set(ret, j, Array.get(orig, j));

            Array.set(ret, j, o);
        }

        return (ret);
    }

    private String getKey(String arg) {
        if (hasEquals)
            arg = arg.replaceFirst("=.*", "");

        if (doubleDashes)
            arg = arg.substring(2);
        else
            arg = arg.substring(1);

        // variables can't start with a number and can't have a dash
        arg = arg.replaceAll("^([0-9])", "_$1");
        arg = arg.replaceAll("-", "__");
        arg = arg.replaceAll("\\.", "_\\$");

        if (aliases != null)
            for (String[] aliase : aliases)
                if (aliase[0].equals(arg))
                    arg = aliase[1];

        return (arg);
    }

    private String getBooleanValue(String arg) {
        if (hasEquals) {
            // remove the argument name
            arg = arg.replaceFirst("[^=]*=", "");

            if (arg.equalsIgnoreCase("true") || arg.equalsIgnoreCase("yes"))
                return ("true");
            else
                return ("false");
        } else {
            return ("true");
        }
    }

    /**
     * Make an Object of the correct type for the field, using a String
     * version of the value to create it.
     *
     * @param type a String representing the base (or String) type
     * @param val  the value
     * @return an Object of the correct type and value
     */
    private Object makeObject(String type, String val) {
        if (type.equals("boolean")) {
            return (Boolean.valueOf(val));
        }
        if (type.equals("byte")) {
            return (Byte.valueOf(val));
        }
        if (type.equals("short")) {
            return (Short.valueOf(val));
        }
        if (type.equals("int")) {
            return (Integer.valueOf(val));
        }
        if (type.equals("float")) {
            return (Float.valueOf(val));
        }
        if (type.equals("double")) {
            return (Double.valueOf(val));
        }
        if (type.equals("long")) {
            return (Long.valueOf(val));
        }
        if (type.equals("java.lang.String")) {
            return (val);
        }
        if (type.equals("char")) {
            return (val.charAt(0));
        }

        return (null);
    }

    private String getEqualsValue(String arg) {
        if (!arg.contains("=")) {
            throw (new IllegalArgumentException
                    ("'" + arg + "' requires '=VALUE'"));
        } else {
            return (arg.replaceFirst("[^=]*=", ""));
        }
    }

    /**
     * Parse a command line.
     *
     * @param args the arguments to be parsed
     */
    public void parse(String args[]) {
        for (int i = 0; i < args.length; i++) {
            if (!args[i].startsWith("-")) {
                parseAdditional(args, i);
                return;
            }

            doubleDashes = args[i].startsWith("--");
            hasEquals = args[i].contains("=");

            String key = getKey(args[i]);
            Field field = getField(key);

            if (field == null) {
                throw (new IllegalArgumentException
                        ("No such option: \"" + key + "\""));
            }

            Class type = field.getType();
            String name = type.getName();

            if (type.isArray())
                name = type.getComponentType().getName();

            String value;

            if (name.equals("boolean"))
                value = getBooleanValue(args[i]);
            else if (doubleDashes || hasEquals)
                value = getEqualsValue(args[i]);
            else
                value = args[++i];

            Object o = makeObject(name, value);

            if (o == null)
                continue;

            if (type.isArray())
                o = addToArray(field, o);

            setObject(field, o);
        }
    }

    public static void main(String args[]) throws java.io.IOException {
        Properties properties = new Properties();
        FileInputStream in = new FileInputStream("version.properties");
        properties.load(in);
        in.close();
        String version = properties.getProperty("version");

        System.out.println("Version: " + version);
    }
}
