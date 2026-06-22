package edu.msudenver.cs.jclo;

import java.lang.reflect.Field;
import java.lang.reflect.Array;
import java.lang.reflect.Modifier;
import java.util.*;
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
 * CONSTRUCTORS:
 * =============
 * Two constructors are available:
 *
 * JCLO(Object object):
 *   Binds to the object without parsing. Call parse(String[]) explicitly.
 *
 * JCLO(Object object, String[] args):
 *   Binds to the object AND immediately parses the arguments in one step.
 *
 * CONFIGURATION:
 * ==============
 * Before calling parse(), optionally configure prefix and aliases:
 *   jclo.setPrefix("PREFIX");      // Prepend prefix to all field names
 *   jclo.setAliases(aliases);      // Set command-line aliases
 *
 * USAGE EXAMPLES:
 * ===============
 * One-shot parsing:
 *   MyArgs obj = new MyArgs();
 *   new JCLO(obj, args);
 *
 * With prefix:
 *   MyArgs obj = new MyArgs();
 *   JCLO jclo = new JCLO(obj);
 *   jclo.setPrefix("PREFIX");
 *   jclo.parse(args);
 *
 * JAVA RECORDS SUPPORT:
 * =====================
 * JCLO supports Java records (immutable data classes) via a two-phase approach:
 *
 * 1. Create a mutable class with the same fields as your record
 * 2. Parse into the mutable class using JCLO
 * 3. Convert the mutable instance to your immutable record
 *
 * Example:
 *   class AppArgsMutable { String name; int count; }  // Mutable for JCLO
 *   record AppArgs(String name, int count) {}          // Immutable record
 *
 *   AppArgsMutable temp = new AppArgsMutable();
 *   new JCLO(temp, args);
 *   AppArgs result = new AppArgs(temp.name, temp.count);
 *
 * Or use a factory method for cleaner conversion:
 *   static AppArgs fromMutable(AppArgsMutable m) {
 *       return new AppArgs(m.name, m.count);
 *   }
 *
 * @author Steve Beaty (beatys@mscd.edu) @version    $Id:
 *         JCLO.java,v 1.5 2007/11/01 16:43:12 beaty Exp beaty $
 */

public class JCLO {
    private Field[] fields;
    private Object object;
    private boolean doubleDashes;
    private boolean hasEquals;
    private String prefix = "";
    private String[][] aliases;

    private void init(final Object object) {
        this.object = object;
        this.fields = object.getClass().getDeclaredFields();
        for (Field field : this.fields) {
            field.setAccessible(true);
        }
    }

    /**
     * A constructor that takes the Object that contains the variables
     * acceptable on a command line.  Call parse (String) to do the actual
     * parsing.
     *
     * @param object where the variables/arguments are
     */
    public JCLO(final Object object) {
        init(object);
    }

    /**
     * A constructor that takes the Object and immediately parses the
     * command-line arguments.
     *
     * @param object where the variables/arguments are
     * @param args the command-line arguments to parse
     */
    public JCLO(final Object object, final String[] args) {
        init(object);
        parse(args);
    }

    /**
     * Set a prefix for all command-line variables. Call before parse().
     *
     * @param prefix the String CLO's start with, if any
     */
    public void setPrefix(final String prefix) {
        this.prefix = prefix;
    }

    /**
     * Set aliases for command-line variables. Call before parse().
     *
     * @param aliases if there are CLO aliases
     */
    public void setAliases(final String[][] aliases) {
        this.aliases = aliases;
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
        String dd = (doubleDashes ? "=" : " ");

        if (type.getName().equals("boolean") || type.getName().equals("java.lang.Boolean")) {
            return (doubleDashes ? "[=boolean]" : "");
        } else if (type.isArray()) {
            return dd + getArrayType(type) + "...";
        } else if (type.isEnum()) {
            return dd + Arrays.toString(type.getEnumConstants());
        } else {
            return dd +
                type.getName().replaceFirst("java.lang.", "");
        }
    }

    /**
     * Create and usage message for the acceptable command line variables.
     *
     * @return a String that specifies acceptable options
     */
    public String usage() {
        List<String> list = new ArrayList<>();

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

            list.add((doubleDashes ? "--" : "-") + key +
                    getUsageType(type) + "\n");
        }

        Collections.sort(list);

        String r = "";
        for (String l : list)
            r += l;

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
        switch (type) {
            case "boolean": case "java.lang.Boolean": return Boolean.valueOf(val);
            case "byte": return Byte.valueOf(val);
            case "short": return Short.valueOf(val);
            case "int": return Integer.valueOf(val);
            case "float": return Float.valueOf(val);
            case "double": return Double.valueOf(val);
            case "long": return Long.valueOf(val);
            case "string": return val;
            case "char": return val.charAt(0);
        }

        throw new IllegalArgumentException("Unknown type: " + type);
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
                throw (new IllegalArgumentException ("No such option: \"" + key + "\""));
            }

            Class type = field.getType();
            String name = type.getName();

            if (type.isArray())
                name = type.getComponentType().getName();

            String value;

            if (name.equals("boolean") || name.equals("java.lang.Boolean"))
                value = getBooleanValue(args[i]);
            else if (doubleDashes || hasEquals)
                value = getEqualsValue(args[i]);
            else
                value = args[++i];

            // horrible hack as i can't figure out how to call a generic class's toValue,
            // so we'll pretend the are primitive types and call makeObject.
            if (! type.isPrimitive()) {
                name = name.replaceFirst("java.lang.", "").toLowerCase();
            }

            Object o = type.isEnum() ? Enum.valueOf(type, value) : makeObject(name, value);

            if (type.isArray())
                o = addToArray(field, o);

            setObject(field, o);
        }
    }
}
