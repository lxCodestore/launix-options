package org.ml.options;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jdom2.Element;

/**
 * A constraint for options taking a value. It allows to constrain the values
 * acceptable for such an option to e. g. a list of strings.
 */
public class ValueConstraint implements XMLConstraint {

    private static final String CLASS = "ValueConstraint";

    /**
     * An enum with the supported subtypes for this constraint type
     */
    public enum Type {

        /**
         * A type defining a set of acceptable string values
         */
        STRING_ARRAY,
        /**
         * A type defining a set of acceptable int values
         */
        INT_ARRAY,
        /**
         * A type defining a range of acceptable int values
         */
        INT_RANGE
    }
    private String[] s_values = null;
    private int[] i_values = null;
    private int imin = 0;
    private int imax = 0;
    private boolean caseSensitive = true;
    private Type type = null;
    private OptionData optionData = null;

    /**
     * The public no-org constructor. This is a prereq for all constraints since
     * it is used for initialization based on XML data.
     */
    public ValueConstraint() {
    }

    /**
     * This method is used to initialize this constraint based on data read from
     * an XML configuration file. The method is invoked internally during setup
     * with the instance of {@link Constrainable} to which the constraint
     * applies and a list of JDOM elements, which contain the details about the
     * constraint itself.
     * 
     * This method initializes the constraint and attaches it to the list of
     * constraints of the {@link Constrainable} instance.
     * 
     * The parameters expected in the XML
     * <code>&lt;param&gt;</code> tags for this constraint are
     * 
     * <table border=1>
     * <caption>Default caption</caption>
     * <tr> <td> <b >Name</b> <td> <b>Value</b> <td>
     * <b>Status</b>
     * <tr> <td> type <td> Same as the
     * <code>type</code> parameter in {@link #add(OptionData, Type, String)}
     * <td> Required
     * <tr> <td> spec <td> Same as the
     * <code>spec</code> parameter in {@link #add(OptionData, Type, String)}
     * <td> Required
     * </table>
     * 
     * @param constrainable The {@link Constrainable} instance to which this
     * constraint applies
     * @param list A list of JDOM elements to be used to initialize the
     * constraint. Specifically, these are tags of the form
     * <p>
     * <code>&lt;param name="..." value="..." /&gt;</code>
     * <p>
     * containing key/value pairs with information.
     */
    @Override
    public void init(Constrainable constrainable, List<Element> list) {

        if (list == null) {
            throw new IllegalArgumentException(CLASS + ": list may not be null");
        }
        if (constrainable == null) {
            throw new IllegalArgumentException(CLASS + ": constrainable may not be null");
        }
        if (!supports(constrainable)) {
            throw new IllegalArgumentException(CLASS + ": Constrainable must be instance of OptionData");
        }

        //.... Extract all parameters

        Map<String, String> params = new HashMap<>();
        for (Element param : list) {
            params.put(param.getAttributeValue("name").trim(), param.getAttributeValue("value").trim());
        }

        //.... Checks

        if (!params.containsKey("type")) {
            throw new IllegalArgumentException(CLASS + ": missing <param> element with attribute named 'type'");
        }
        if (!params.containsKey("spec")) {
            throw new IllegalArgumentException(CLASS + ": missing <param> element with attribute named 'spec'");
        }

        //.... Add the constraint

        add((OptionData) constrainable, Type.valueOf(params.get("type")), params.get("spec"));

    }

    /**
     * Add a constraint of {@link Type}
     * <code>STRING_ARRAY</code> for the given option
     * <p>
     *
     * @param optionData
     * @param values A string array with the acceptable values for the option
     * @param caseSensitive Whether the string comparisons are to be made case
     * sensitive or not
     */
    public static void add(OptionData optionData, String[] values, boolean caseSensitive) {
        if (optionData == null) {
            throw new IllegalArgumentException(CLASS + ": optionData may not be null");
        }
        optionData.addConstraint(new ValueConstraint(optionData, values, caseSensitive));
    }

    /**
     * Add a constraint of {@link Type}
     * <code>INT_ARRAY</code> for the given option
     * <p>
     *
     * @param optionData
     * @param values An integer array with the acceptable values for the option
     */
    public static void add(OptionData optionData, int[] values) {
        if (optionData == null) {
            throw new IllegalArgumentException(CLASS + ": optionData may not be null");
        }
        optionData.addConstraint(new ValueConstraint(optionData, values));
    }

    /**
     * Add a constraint of {@link Type}
     * <code>INT_RANGE</code> for the given option
     * <p>
     *
     * @param optionData
     * @param imin The minimum acceptable integer value
     * @param imax The maximum acceptable integer value (must be greater than or
     * equal to <code>imin</code>)
     */
    public static void add(OptionData optionData, int imin, int imax) {
        if (optionData == null) {
            throw new IllegalArgumentException(CLASS + ": optionData may not be null");
        }
        optionData.addConstraint(new ValueConstraint(optionData, imin, imax));
    }

    /**
     * Add a constraint of the given {@link Type} with the specified details
     * <p>
     *
     * @param optionData
     * @param type The type for this constraint
     * @param spec A string specifying the details for this constraint:
     * 
     * <table border=1>
     * <caption>Default caption</caption>
     * <tr> <td> <b>Type</b> </td><td> <b>Format for
     * specification</b></td></tr>
     * <tr> <td> STRING_ARRAY </td><td> All values separated by vertical bar (e. g.
     * Foo|Bah|Yeah). If the first string is preceded by '+', the checks are run
     * case insensitive (default is to run them case sensitive)</td></tr>
     * <tr> <td> INT_ARRAY </td><td> All values separated by vertical bar (e. g.
     * 1|2|7)</td></tr>
     * <tr> <td> INT_RANGE </td><td> MIN:MAX (e. g. 7:12)</td></tr>
     * </table>
     */
    public static void add(OptionData optionData, Type type, String spec) {
        if (optionData == null) {
            throw new IllegalArgumentException(CLASS + ": optionData may not be null");
        }
        optionData.addConstraint(new ValueConstraint(optionData, type, spec));
    }

    /**
     * Constructor for {@link Type}
     * <code>STRING_ARRAY</code>
     * <p>
     *
     * @param values A string array with the acceptable values for the option
     * @param caseSensitive Whether the string comparisons are to be made case
     * sensitive or not
     */
    ValueConstraint(OptionData optionData, String[] values, boolean caseSensitive) {
        if (values == null) {
            throw new IllegalArgumentException(CLASS + ": values may not be null");
        }
        if (values.length == 0) {
            throw new IllegalArgumentException(CLASS + ": values must contain at least one element");
        }
        s_values = values;
        type = Type.STRING_ARRAY;
        this.caseSensitive = caseSensitive;
        this.optionData = optionData;
    }

    /**
     * Constructor for {@link Type}
     * <code>INT_ARRAY</code>
     * <p>
     *
     * @param values An integer array with the acceptable values for the option
     */
    ValueConstraint(OptionData optionData, int[] values) {
        if (values == null) {
            throw new IllegalArgumentException(CLASS + ": values may not be null");
        }
        if (values.length == 0) {
            throw new IllegalArgumentException(CLASS + ": values must contain at least one element");
        }
        i_values = values;
        type = Type.INT_ARRAY;
        this.optionData = optionData;
    }

    /**
     * Constructor for {@link Type}
     * <code>INT_RANGE</code>
     * <p>
     *
     * @param imin The minimum acceptable integer value
     * @param imax The maximum acceptable integer value (must be greater than or
     * equal to <code>imin</code>)
     */
    ValueConstraint(OptionData optionData, int imin, int imax) {
        if (imax < imin) {
            throw new IllegalArgumentException(CLASS + ": imax must greater than or equal to imin");
        }
        this.imin = imin;
        this.imax = imax;
        type = Type.INT_RANGE;
        this.optionData = optionData;
    }

    /**
     * Constructor for any {@link Type}
     * <p>
     *
     * @param type The type for this constraint
     * @param spec A string specifying the details for this constraint:
     * <p>
     * <table border=1 cellpadding=6>
     * <tr bgcolor=#dddddd> <td> <b >Type</b> <td> <b>Format for
     * specification</b>
     * <tr> <td> STRING_ARRAY <td> All values separated by vertical bar (e. g.
     * Foo|Bah|Yeah). If the first string is preceded by '+', the checks are run
     * case insensitive (default is to run them case sensitive)
     * <tr> <td> INT_ARRAY <td> All values separated by vertical bar (e. g.
     * 1|2|7)
     * <tr> <td> INT_RANGE <td> MIN:MAX (e. g. 7:12)
     * </table>
     */
    ValueConstraint(OptionData optionData, Type type, String spec) {

        if (type == null) {
            throw new IllegalArgumentException(CLASS + ": type may not be null");
        }
        if (spec == null) {
            throw new IllegalArgumentException(CLASS + ": spec may not be null");
        }

        this.type = type;
        this.optionData = optionData;

        switch (type) {

            case STRING_ARRAY:

                s_values = spec.split("\\|");
                if (s_values[0].startsWith("+")) {
                    caseSensitive = false;
                    s_values[0] = s_values[0].substring(1);
                }
                break;

            case INT_ARRAY:

                s_values = spec.split("\\|");
                i_values = new int[s_values.length];
                try {
                    int i = 0;
                    for (String s : s_values) {
                        i_values[i++] = Integer.parseInt(s);
                    }
                } catch (NumberFormatException ex) {
                    throw new IllegalArgumentException(CLASS + ": Invalid specification for type " + type + ": " + spec);
                }
                break;

            case INT_RANGE:

                s_values = spec.split(":");
                if (s_values.length != 2) {
                    throw new IllegalArgumentException(CLASS + ": Invalid specification for type " + type + ": " + spec);
                }

                try {
                    imin = Integer.parseInt(s_values[0]);
                    imax = Integer.parseInt(s_values[1]);
                } catch (NumberFormatException ex) {
                    throw new IllegalArgumentException(CLASS + ": Invalid specification for type " + type + ": " + spec);
                }
                break;

        }

    }

    /**
     * The actual check routine
     * <p>
     *
     * @return A boolean indicating whether the constraint is satisfied or not
     */
    @Override
    public boolean isSatisfied() {

        String test;

        for (int i = 0; i < optionData.getResultCount(); i++) {

            test = optionData.getResultValue(i);

            switch (type) {

                case STRING_ARRAY:

                    if (caseSensitive) {
                        for (String s : s_values) {
                            if (s.equals(test)) {
                                return true;
                            }
                        }
                    } else {
                        for (String s : s_values) {
                            if (s.equalsIgnoreCase(test)) {
                                return true;
                            }
                        }
                    }

                    break;

                case INT_ARRAY:

                    int t;
                    try {
                        t = Integer.parseInt(test);
                    } catch (NumberFormatException ex) {
                        return false;
                    }

                    for (int ii : i_values) {
                        if (ii == t) {
                            return true;
                        }
                    }

                    break;

                case INT_RANGE:

                    try {
                        t = Integer.parseInt(test);
                    } catch (NumberFormatException ex) {
                        return false;
                    }

                    if ((t >= imin) && (t <= imax)) {
                        return true;
                    }

                    break;

            }

        }

        return false;

    }

    /**
     * Indicates whether a constraint supports a given type of
     * {@link Constrainable}
     * <p>
     *
     * @param constrainable
     * @return A boolean to indicate whether this {@link Constrainable} is
     * supported. This constraint only supports {@link OptionData}
     * constrainables
     */
    @Override
    public boolean supports(Constrainable constrainable) {
        if (constrainable == null) {
            throw new IllegalArgumentException(CLASS + ": constrainable may not be null");
        }
        return constrainable instanceof OptionData;
    }

    /**
     * Return the type for this constraint
     * <p>
     *
     * @return The type for this constraint
     */
    Type getType() {
        return type;
    }

    /**
     * This is the overloaded {@link Object#toString()} method
     * <p>
     *
     * @return A string representing the instance
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append(type.name());
        sb.append(": ");

        switch (type) {
            case STRING_ARRAY:
                if (!caseSensitive) {
                    sb.append('+');
                }
                for (String s : s_values) {
                    sb.append(s);
                    sb.append('|');
                }
                sb.deleteCharAt(sb.length() - 1);
                break;
            case INT_ARRAY:
                for (int i : i_values) {
                    sb.append(i);
                    sb.append('|');
                }
                sb.deleteCharAt(sb.length() - 1);
                break;
            case INT_RANGE:
                sb.append(imin);
                sb.append(':');
                sb.append(imax);
                break;
        }

        return sb.toString();

    }
}
