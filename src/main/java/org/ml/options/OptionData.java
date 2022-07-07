package org.ml.options;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * This class holds all the data for an option. This includes the prefix, the
 * key, the separators (for value and detail options), the multiplicity, and all
 * the other settings describing the option. The class is designed to be only a
 * data container from a user perspective, i. e. the user has access to any data
 * determined by the {@link Options#check()} methods, but not access to any of
 * the other methods which are used internally for the operation of the actual
 * check.
 */
public class OptionData implements Constrainable {

    private final static String CLASS = "OptionData";
    private Options.Prefix prefix;
    private Options.Prefix altPrefix;
    private String key;
    private String altKey;
    private String helpText = "";
    private String valueText = "value";
    private String detailText = "detail";
    private boolean detail = false;
    private Options.Separator separator;
    private boolean value = false;
    private boolean exclusive = false;
    private Options.Multiplicity multiplicity;
    private Pattern pattern;
    private int counter = 0;
    private List<String> values;
    private List<String> details;
    private List<Constraint> constraints;
    private Type type;

    /**
     * An enum describing the different available types of options
     */
    public enum Type {

        /**
         * An option which acts as a switch (i. e. no value or detail argument
         * is taken)
         */
        SIMPLE(false, false),
        /**
         * An option which expects a value to be specified along with it
         */
        VALUE(true, false),
        /**
         * An option which expects both a value and details further describing
         * the value to be specified along with it
         */
        DETAIL(true, true);
        boolean value = false;
        boolean detail = false;

        Type(boolean value, boolean detail) {
            this.value = value;
            this.detail = detail;
        }

        boolean value() {
            return value;
        }

        boolean detail() {
            return detail;
        }
    }

    /**
     * A copying constructor. This is for setup purposes only, i. e. result data
     * is NOT copied.
     */
    OptionData(OptionData od) {
        this(od.getType(), od.getPrefix(), od.getAltPrefix(), od.getKey(), od.getAltKey(),
                od.getSeparator(), od.getMultiplicity());
        this.helpText = od.getHelpText();
        this.valueText = od.getValueText();
        this.detailText = od.getDetailText();
    }

    /**
     * The constructor
     */
    OptionData(Type type,
               Options.Prefix prefix,
               Options.Prefix altPrefix,
               String key,
               String altKey,
               Options.Separator separator,
               Options.Multiplicity multiplicity) {

        if (type == null) {
            throw new IllegalArgumentException(CLASS + ": type may not be null");
        }
        if (prefix == null) {
            throw new IllegalArgumentException(CLASS + ": prefix may not be null");
        }
        if (altPrefix == null) {
            throw new IllegalArgumentException(CLASS + ": altPrefix may not be null");
        }
        if (key == null) {
            throw new IllegalArgumentException(CLASS + ": key may not be null");
        }
        if (multiplicity == null) {
            throw new IllegalArgumentException(CLASS + ": multiplicity may not be null");
        }

        //.... The data describing the option
        this.type = type;
        this.prefix = prefix;
        this.altPrefix = altPrefix;
        this.key = key;
        this.altKey = altKey;
        this.separator = separator;
        this.multiplicity = multiplicity;

        value = type.value();
        detail = type.detail();

        //.... Create the pattern to match this option
        String keyPattern;
        if (altKey == null) {
            keyPattern = prefix.getName() + key;
        } else {
            keyPattern = "(" + prefix.getName() + key + "|" + altPrefix.getName() + altKey + ")";
        }

        if (value) {
            if (separator.equals(Options.Separator.BLANK)) {
                if (detail) {
                    pattern = Pattern.compile(keyPattern + "((\\w|\\.)+)$");
                } else {
                    pattern = Pattern.compile(keyPattern + "$");
                }
            } else {
                if (detail) {
                    pattern = Pattern.compile(keyPattern + "((\\w|\\.)+)" + separator.getName() + "(.+)$");
                } else {
                    pattern = Pattern.compile(keyPattern + separator.getName() + "(.+)$");
                }
            }
        } else {
            pattern = Pattern.compile(keyPattern + "$");
        }

        //.... Structures to hold result data
        if (value) {
            values = new ArrayList<>();
            if (detail) {
                details = new ArrayList<>();
            }
        }

    }

    // ==========================================================================================
    // Inquiry methods
    // ==========================================================================================

    /**
     * Check whether this option has a defined alternate key value
     * <p>
     *
     * @return A boolean indicating whether this option has a defined alternate
     * key value
     */
    boolean hasAlternateKey() {
        return altKey == null ? false : true;
    }

    /**
     * Getter method for <code>prefix</code> property
     * <p>
     *
     * @return The value for the <code>prefix</code> property
     */
    Options.Prefix getPrefix() {
        return prefix;
    }

    /**
     * Getter method for <code>altPrefix</code> property
     * <p>
     *
     * @return The value for the <code>altPprefix</code> property
     */
    Options.Prefix getAltPrefix() {
        return altPrefix;
    }

    /**
     * Getter method for <code>type</code> property
     * <p>
     *
     * @return The value for the <code>type</code> property
     */
    Type getType() {
        return type;
    }

    /**
     * Getter method for <code>key</code> property
     * <p>
     *
     * @return The value for the <code>key</code> property
     */
    String getKey() {
        return key;
    }

    /**
     * Getter method for <code>altKey</code> property
     * <p>
     *
     * @return The value for the <code>altKey</code> property
     */
    String getAltKey() {
        return altKey;
    }

    /**
     * Getter method for <code>detail</code> property
     * <p>
     *
     * @return The value for the <code>detail</code> property
     */
    boolean useDetail() {
        return detail;
    }

    /**
     * Getter method for <code>separator</code> property
     * <p>
     *
     * @return The value for the <code>separator</code> property
     */
    Options.Separator getSeparator() {
        return separator;
    }

    /**
     * Getter method for <code>value</code> property
     * <p>
     *
     * @return The value for the <code>value</code> property
     */
    boolean useValue() {
        return value;
    }

    /**
     * Getter method for <code>multiplicity</code> property
     * <p>
     *
     * @return The value for the <code>multiplicity</code> property
     */
    Options.Multiplicity getMultiplicity() {
        return multiplicity;
    }

    /**
     * Setter method for <code>multiplicity</code> property
     * <p>
     *
     * @param multiplicity The value for the <code>multiplicity</code> property
     */
    void setMultiplicity(Options.Multiplicity multiplicity) {
        if (multiplicity == null) {
            throw new IllegalArgumentException(CLASS + ": multiplicity may not be null");
        }
        this.multiplicity = multiplicity;
    }

    /**
     * Getter method for <code>pattern</code> property
     * <p>
     *
     * @return The value for the <code>pattern</code> property
     */
    Pattern getPattern() {
        return pattern;
    }

    /**
     * @return
     */
    boolean isMandatory() {
        switch (multiplicity) {
            case ONCE:
            case ONCE_OR_MORE:
                return true;
            case ZERO_OR_MORE:
            case ZERO_OR_ONCE:
                return false;
            default:
                return false;
        }
    }

    // ==========================================================================================
    // Result management
    // ==========================================================================================

    /**
     * Check whether this option has been found on the command line
     * <p>
     *
     * @return A boolean indicating whether this option has been found on the
     * command line
     */
    public boolean isSet() {
        return getResultCount() > 0 ? true : false;
    }

    /**
     * Get the number of results found for this option, which is number of times
     * the key matched
     * <p>
     *
     * @return The number of results
     */
    public int getResultCount() {
        if (value) {
            return values.size();
        } else {
            return counter;
        }
    }

    /**
     * Get the value with the given index. The index can range between 0 and
     * {@link #getResultCount()} <code> - 1</code>. However, only for value
     * options, a non- <code>null</code> value will be returned. Non-value
     * options always return <code>null</code>.
     * <p>
     *
     * @param index The index for the desired value
     *              <p>
     * @return The option value with the given index
     */
    public String getResultValue(int index) {
        if (!value) {
            return null;
        }
        if (index < 0 || index >= getResultCount()) {
            throw new IllegalArgumentException(CLASS + ": illegal value for index");
        }
        return values.get(index);
    }

    /**
     * Return a list of all result values
     * <p>
     *
     * @return A list with all result values
     */
    public List<String> getResultValues() {
        List<String> list = new ArrayList<>();
        for (int index = 0; index < getResultCount(); index++) {
            list.add(getResultValue(index));
        }
        return list;
    }

    /**
     * Get the detail with the given index. The index can range between 0 and
     * {@link #getResultCount()} <code> - 1</code>. However, only for value
     * options which take details, a non- <code>null</code> detail will be
     * returned. Non-value options and value options which do not take details
     * always return <code>null</code>.
     * <p>
     *
     * @param index The index for the desired value
     *              <p>
     * @return The option detail with the given index
     */
    public String getResultDetail(int index) {
        if (!detail) {
            return null;
        }
        if (index < 0 || index >= getResultCount()) {
            throw new IllegalArgumentException(CLASS + ": illegal value for index");
        }
        return details.get(index);
    }

    /**
     * Return a list of all option details
     * <p>
     *
     * @return A list with all option details
     */
    public List<String> getResultDetails() {
        List<String> list = new ArrayList<>();
        for (int index = 0; index < getResultCount(); index++) {
            list.add(getResultDetail(index));
        }
        return list;
    }

    /**
     * Store the data for a match found
     */
    void addResult(String valueData, String detailData) {
        if (value) {
            if (valueData == null) {
                throw new IllegalArgumentException(CLASS + ": valueData may not be null");
            }
            values.add(valueData);
            if (detail) {
                if (detailData == null) {
                    throw new IllegalArgumentException(CLASS + ": detailData may not be null");
                }
                details.add(detailData);
            }
        }
        counter++;
    }

    // ==========================================================================================
    // Description management
    // ==========================================================================================

    /**
     * Set the text to be used for the &lt;value&gt; argument of a value option.
     * This is used in the {@link HelpPrinter} output.
     * <p>
     *
     * @param text The text used for the &lt;value&gt; argument of a value
     *             option
     *             <p>
     * @return The option instance itself to allow incovation chaining
     */
    public OptionData setValueText(String text) {
        if (text == null) {
            throw new IllegalArgumentException(CLASS + ": text may not be null");
        }
        this.valueText = text.trim();
        return this;
    }

    /**
     * Set the text to be used for the &lt;detail&gt; argument of a value
     * option. This is used in the {@link HelpPrinter} output.
     * <p>
     *
     * @param text The text used for the &lt;detail&gt; argument of a value
     *             option
     *             <p>
     * @return The option instance itself to allow incovation chaining
     */
    public OptionData setDetailText(String text) {
        if (text == null) {
            throw new IllegalArgumentException(CLASS + ": text may not be null");
        }
        this.detailText = text.trim();
        return this;
    }

    /**
     * Set the text describing the purpose of the option. This is used in the
     * {@link HelpPrinter} output.
     * <p>
     *
     * @param text The text describing the purpose of the option
     *             <p>
     * @return The option instance itself to allow incovation chaining
     */
    public OptionData setHelpText(String text) {
        if (text == null) {
            throw new IllegalArgumentException(CLASS + ": text may not be null");
        }
        this.helpText = text.trim();
        return this;
    }

    /**
     * Return the text describing the purpose of the option
     * <p>
     *
     * @return The text describing the purpose of the option (or an empty
     * string, if that text has not been set)
     */
    public String getHelpText() {
        return helpText;
    }

    /**
     * Return the text to be used for the &lt;value&gt; argument of a value
     * option
     * <p>
     *
     * @return The text to be used for the &lt;value&gt; argument of a value
     * option (or a default value if this text has not been set, or if this is
     * not a value option at all)
     */
    public String getValueText() {
        return valueText;
    }

    /**
     * Return the text to be used for the &lt;detail&gt; argument of a detail
     * option
     * <p>
     *
     * @return The text to be used for the &lt;detail&gt; argument of a detail
     * option (or a default value if this text has not been set, or if this is
     * not a detail option at all)
     */
    public String getDetailText() {
        return detailText;
    }

    /**
     * Add a constraint for this option
     * <p>
     *
     * @param constraint The {@link Constraint} to add
     */
    @Override
    public void addConstraint(Constraint constraint) {
        if (constraint == null) {
            throw new IllegalArgumentException(CLASS + ": constraint may not be null");
        }

        if (!constraint.supports(this)) {
            throw new IllegalArgumentException(CLASS + ": the given constraint can not be applied to options");
        }

        if (constraints == null) {
            constraints = new ArrayList<>();
        }

        constraints.add(constraint);
    }

    /**
     *
     */
    boolean isExclusive() {
        return exclusive;
    }

    /**
     *
     */
    void setExclusive(boolean exclusive) {
        this.exclusive = exclusive;
    }

    /**
     * Get the constraints defined for this option
     * <p>
     *
     * @return The defined constraints for this option (or <code>null</code> if
     * no constraints have been defined)
     */
    @Override
    public List<Constraint> getConstraints() {
        return constraints;
    }

    /**
     * Get the command line syntax for this option. This method accounts for all
     * characteristics of the option such as separators, multiplicity, alternate
     * keys and the like.
     * <p>
     *
     * @return A string with the command line syntax
     */
    public String getSyntax() {

        StringBuilder sb = new StringBuilder(20);

        boolean mult = (multiplicity == Options.Multiplicity.ZERO_OR_MORE)
                || (multiplicity == Options.Multiplicity.ONCE_OR_MORE) ? true : false;
        boolean opt = (multiplicity == Options.Multiplicity.ONCE_OR_MORE)
                || (multiplicity == Options.Multiplicity.ONCE) ? false : true;

        if (opt) {
            sb.append('[');
        }     // Option can also be omitted
        printFullOption(sb, mult);
        if (opt) {
            sb.append(']');
        }     // Option can also be omitted

        return sb.toString();

    }

    /**
     * Helper method: print the full text for an option, accounting for
     * multiplicity
     */
    void printFullOption(StringBuilder sb, boolean mult) {
        printOption(sb);
        if (mult) {                  // Option can occur more than once
            printTexts(sb, 1);
            sb.append(" [");
            printOption(sb);
            printTexts(sb, 2);
            sb.append(" [...]]");
        } else {
            printTexts(sb, 0);
        }
    }

    /**
     * Helper method: add the descriptive texts for value and detail options
     */
    void printTexts(StringBuilder sb, int i) {
        if (detail) {
            sb.append('<');
            sb.append(detailText);
            if (i > 0) {
                sb.append(i);
            }
            sb.append('>');
        }
        if (value) {
            sb.append(separator.getName());
            sb.append('<');
            sb.append(valueText);
            if (i > 0) {
                sb.append(i);
            }
            sb.append('>');
        }
    }

    /**
     * Helper method: prints the key and - if present - the alternate key for an
     * option, adds () if necessary
     */
    void printOption(StringBuilder sb) {
        if (altKey != null) {
            sb.append('(');
        }
        sb.append(prefix.getName());
        sb.append(key);
        if (altKey != null) {
            sb.append('|');
            sb.append(altPrefix.getName());
            sb.append(altKey);
            sb.append(')');
        }
    }

    /**
     * This is the overloaded {@link Object#toString()} method.
     * <p>
     *
     * @return A string representing the instance
     */
    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();

        sb.append("Prefix      : ");
        sb.append(prefix);
        sb.append('\n');
        sb.append("AltPrefix   : ");
        sb.append(altPrefix);
        sb.append('\n');
        sb.append("Key         : ");
        sb.append(key);
        sb.append('\n');
        if (hasAlternateKey()) {
            sb.append("AltKey      : ");
            sb.append(altKey);
            sb.append('\n');
        }
        sb.append("Detail      : ");
        sb.append(detail);
        sb.append('\n');
        sb.append("Separator   : ");
        sb.append(separator);
        sb.append('\n');
        sb.append("Value       : ");
        sb.append(value);
        sb.append('\n');
        sb.append("Multiplicity: ");
        sb.append(multiplicity);
        sb.append('\n');
        sb.append("Pattern     : ");
        sb.append(pattern);
        sb.append('\n');
        sb.append("HelpText    : ");
        sb.append(helpText);
        sb.append('\n');
        sb.append("ValueText   : ");
        sb.append(valueText);
        sb.append('\n');
        sb.append("DetailText  : ");
        sb.append(detailText);
        sb.append('\n');

        if (constraints != null) {
            for (Constraint constraint : constraints) {
                sb.append("Constraint  : ");
                sb.append(constraint.toString());
                sb.append('\n');
            }
        }

        sb.append("Results #   : ");
        sb.append(counter);
        sb.append('\n');

        if (value) {
            if (detail) {
                for (int i = 0; i < values.size(); i++) {
                    sb.append(details.get(i));
                    sb.append(" / ");
                    sb.append(values.get(i));
                    sb.append('\n');
                }
            } else {
                for (int i = 0; i < values.size(); i++) {
                    sb.append(values.get(i));
                    sb.append('\n');
                }
            }
        }

        return sb.toString();

    }
}
