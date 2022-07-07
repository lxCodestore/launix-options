package org.ml.options;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class holds the information for a <i>set</i> of options. A set can hold
 * any number of <code>OptionData</code> instances which are checked together to
 * determine success or failure.
 * <p>
 * The approach to use this class looks like this:
 *
 * <ol>
 * <li> The user uses any of the <code>Options.addSet()</code> methods (e. g.
 * {@link Options#addSet(String)}) to create any number of sets required (or
 * just relies on the default set, if only one set is required)
 * <li> The user adds all required option definitions to each set
 * <li> Using any of the <code>Options.check()</code> methods, each set can be
 * checked whether the options that were specified on the command line satisfy
 * its requirements
 * <li> If the check was successful for a given set, several data items are
 * available from this class:
 * <ul>
 * <li> All options defined for the set (through which e. g. values, details,
 * and multiplicity are available)
 * <li> All data items found (these are the items on the command line which do
 * not start with the prefix, i. e. non-option arguments)
 * <li> All unmatched arguments on the command line (these are the items on the
 * command line which start with the prefix, but do not match to one of the
 * options). Programs can elect to ignore these, or react with an error
 * </ul>
 * </ol>
 */
public class OptionSet implements Constrainable {

    private final static String CLASS = "OptionSet";
    private static Pattern keyPattern = Pattern.compile("\\w+");
    private ArrayList<OptionData> options = new ArrayList<>();
    private HashMap<String, OptionData> keys = new HashMap<>();
    private HashSet<String> altKeys = new HashSet<>();
    private ArrayList<String> unmatched = new ArrayList<>();
    private ArrayList<String> data = new ArrayList<>();
    private String name;
    private String[] dataText;
    private String[] helpText;
    private int minData = 0;
    private int maxData = 0;
    private Options.Prefix prefix;
    private Options.Prefix altPrefix;
    private Options.Multiplicity defaultMultiplicity;
    private Options.Separator valueSeparator;
    private Options.Separator detailSeparator;
    private boolean isDefault = false;
    private boolean unlimitedData = false;
    private int limit = 0;
    private List<Constraint> constraints;
    /**
     * A constant indicating an unlimited number of supported data items
     */
    public static final int INF = -1;

    /**
     * A copying constructor. This is for setup purposes only and does not copy
     * any result data.
     */
    OptionSet(String name, OptionSet os) {

        this(name, os.getPrefix(), os.getAltPrefix(), os.getValueSeparator(), os.getDetailSeparator(),
                os.getDefaultMultiplicity(), os.getMinData(), os.getMaxData(), false);

        this.limit = os.getLimit();

        for (int i = 0; i < limit; i++) {
            helpText[i] = os.getHelpText(i);
            dataText[i] = os.getDataText(i);
        }

        for (String s : os.getAltKeys()) {
            altKeys.add(s);
        }

        OptionData nod;
        for (OptionData od : os.getOptionData()) {
            nod = new OptionData(od);
            options.add(nod);
            keys.put(od.getKey(), nod);
        }

    }

    /**
     * @return
     */
    Options.Multiplicity getDefaultMultiplicity() {
        return defaultMultiplicity;
    }

    /**
     * @return
     */
    HashMap<String, OptionData> getKeys() {
        return keys;
    }

    /**
     * @return
     */
    HashSet<String> getAltKeys() {
        return altKeys;
    }

    /**
     * @param name
     * @param prefix
     * @param altPrefix
     * @param valueSeparator
     * @param detailSeparator
     * @param defaultMultiplicity
     * @param minData
     * @param maxData
     * @param isDefault
     */
    OptionSet(String name,
              Options.Prefix prefix,
              Options.Prefix altPrefix,
              Options.Separator valueSeparator,
              Options.Separator detailSeparator,
              Options.Multiplicity defaultMultiplicity,
              int minData,
              int maxData,
              boolean isDefault) {

        if (name == null) {
            throw new IllegalArgumentException(CLASS + ": name may not be null");
        }
        if (minData < 0) {
            throw new IllegalArgumentException(CLASS + ": minData must be >= 0");
        }
        if (maxData < minData) {
            throw new IllegalArgumentException(CLASS + ": maxData must be >= minData");
        }

        this.prefix = prefix;
        this.altPrefix = altPrefix;
        this.defaultMultiplicity = defaultMultiplicity;
        this.valueSeparator = valueSeparator;
        this.detailSeparator = detailSeparator;
        this.name = name;
        this.minData = minData;
        this.maxData = maxData;

//.... Unless we support an unlimited number of data items, we can define texts for up to maxData
//     data items. Otherwise, we only can use up to (minData + 1) definitions: minData for the 
//     first required ones, and 1 more which goes into the [] brackets
        limit = maxData;
        if (maxData == Integer.MAX_VALUE) {
            unlimitedData = true;
            limit = minData + 1;
        }

        dataText = new String[limit];
        for (int i = 0; i < limit; i++) // Set the default for the data text
        {
            dataText[i] = "data";
        }
        helpText = new String[limit];
        for (int i = 0; i < limit; i++) // Set the default for the help text
        {
            helpText[i] = "";
        }

        this.isDefault = isDefault;                         // Whether this is the default set

    }

    /**
     * Indicate whether this set has no upper limit for the number of allowed
     * data items
     * <p>
     *
     * @return A boolean indicating whether this set has no upper limit for the
     * number of allowed data items
     */
    public boolean hasUnlimitedData() {
        return unlimitedData;
    }

    /**
     * Indicate whether this set is the default set or not
     * <p>
     *
     * @return A boolean indicating whether this set is the default set or not
     */
    public boolean isDefault() {
        return isDefault;
    }

    /**
     * Helper method to identify OptionSet instances that have ONLY optional options (flags and data items)
     *
     * @return
     */
    public boolean isPurelyOptional() {
        if (minData > 0) {
            return false;
        }
        boolean mandatory = false;
        for (OptionData optionData : options) {
            mandatory = mandatory | optionData.isMandatory();
        }
        return !mandatory;
    }

    /**
     * Add a constraint for this option set
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
            throw new IllegalArgumentException(CLASS + ": the given constraint can not be applied to option sets");
        }

        if (constraints == null) {
            constraints = new ArrayList<>();
        }

        constraints.add(constraint);
    }

    /**
     * Get the constraints defined for this option set
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
     * Set the data text for a data item on the command line. This is exploited
     * e. g. in {@link HelpPrinter} instances.
     * <p>
     *
     * @param index The index for this data item on the command line. Must be
     *              within the allowed range of <code>0 ... maxData - 1</code> for this set.
     *              If this set supports an unlimited number of data items, the allowed range
     *              is <code>0 ... minData</code>.
     * @param text  The text to use for this data item in the command line syntax
     *              <p>
     * @return This set to allow for invocation chaining
     */
    public OptionSet setDataText(int index, String text) {
        if (text == null) {
            throw new IllegalArgumentException(CLASS + ": text may not be null");
        }
        if (index < 0 || index >= limit) {
            throw new IllegalArgumentException(CLASS + ": invalid value for index");
        }
        this.dataText[index] = text.trim();
        return this;
    }

    /**
     * Set the help text for a data item on the command line. This is exploited
     * e. g. in {@link HelpPrinter} instances.
     * <p>
     *
     * @param index The index for this data item on the command line. Must be
     *              within the allowed range of <code>0 ... maxData - 1</code> for this set.
     *              If this set supports an unlimited number of data items, the allowed range
     *              is <code>0 ... minData</code>.
     * @param text  The help text to use to describe the purpose of the data item
     *              <p>
     * @return This set to allow for invocation chaining
     */
    public OptionSet setHelpText(int index, String text) {
        if (text == null) {
            throw new IllegalArgumentException(CLASS + ": text may not be null");
        }
        if (index < 0 || index >= limit) {
            throw new IllegalArgumentException(CLASS + ": invalid value for index");
        }
        this.helpText[index] = text.trim();
        return this;
    }

    /**
     * Get the data text for a data item on the command line. This is only
     * useful if such a data text is used.
     * <p>
     *
     * @param index The index for this data item on the command line. Must be
     *              within the allowed range of <code>0 ... maxData - 1</code> for this set.
     *              If this set supports an unlimited number of data items, the allowed range
     *              is <code>0 ... minData</code>.
     *              <p>
     * @return The text used for this data item in the command line syntax
     */
    public String getDataText(int index) {
        if (index < 0 || index >= limit) {
            throw new IllegalArgumentException(CLASS + ": invalid value for index");
        }
        return dataText[index];
    }

    /**
     * Get the help text for a data item on the command line. This is only
     * useful if such a help text is used.
     * <p>
     *
     * @param index The index for this data item on the command line. Must be
     *              within the allowed range of <code>0 ... maxData - 1</code> for this set.
     *              If this set supports an unlimited number of data items, the allowed range
     *              is <code>0 ... minData</code>.
     *              <p>
     * @return The help text used to describe the purpose of the data item
     */
    public String getHelpText(int index) {
        if (index < 0 || index >= limit) {
            throw new IllegalArgumentException(CLASS + ": invalid value for index");
        }
        return helpText[index];
    }

    /**
     * Get the primary {@link Options.Prefix} for this set. This is primarily
     * intended for use by {@link HelpPrinter} instances to format their output.
     * <p>
     *
     * @return The {@link Options.Prefix} instance used as primary prefix for
     * this set
     */
    Options.Prefix getPrefix() {
        return prefix;
    }

    /**
     * Get the alternate {@link Options.Prefix} for this set. This is primarily
     * intended for use by {@link HelpPrinter} instances to format their output.
     * <p>
     *
     * @return The {@link Options.Prefix} instance used as alternate prefix for
     * this set
     */
    Options.Prefix getAltPrefix() {
        return altPrefix;
    }

    /**
     * Get a list of all the options defined for this set
     * <p>
     *
     * @return A list of {@link OptionData} instances defined for this set
     */
    public List<OptionData> getOptionData() {
        return options;
    }

    /**
     * Get the data for a specific option, identified by its key name (which is
     * unique)
     * <p>
     *
     * @param key The key for the option
     *            <p>
     * @return The {@link OptionData} instance
     */
    public OptionData getOption(String key) {
        if (key == null) {
            throw new IllegalArgumentException(CLASS + ": key may not be null");
        }
        if (!keys.containsKey(key)) {
            throw new IllegalArgumentException(CLASS + ": unknown key: " + key);
        }
        return keys.get(key);
    }

    /**
     * @param key
     * @return
     */
    public OptionData getOption(Enum key) {
        if (key == null) {
            throw new NullPointerException("key may not be null");
        }
        return getOption(key.toString());
    }

    /**
     * Check whether a specific option is set, i. e. whether it was specified at
     * least once on the command line.
     * <p>
     *
     * @param key The key for the option
     *            <p>
     * @return <code>true</code> or <code>false</code>, depending on the outcome
     * of the check
     */
    public boolean isSet(String key) {
        if (key == null) {
            throw new IllegalArgumentException(CLASS + ": key may not be null");
        }
        if (!keys.containsKey(key)) {
            throw new IllegalArgumentException(CLASS + ": unknown key: " + key);
        }
        return keys.get(key).isSet();
    }

    /**
     * @param key
     * @return
     */
    public boolean isSet(Enum key) {
        if (key == null) {
            throw new NullPointerException("key may not be null");
        }
        return isSet(key.toString());
    }

    /**
     * Return the name of the set
     * <p>
     *
     * @return The name of the set
     */
    public String getName() {
        return name;
    }

    /**
     * Getter method for <code>minData</code> property
     * <p>
     *
     * @return The value for the <code>minData</code> property
     */
    public int getMinData() {
        return minData;
    }

    /**
     * Getter method for <code>maxData</code> property
     * <p>
     *
     * @return The value for the <code>maxData</code> property
     */
    public int getMaxData() {
        if (hasUnlimitedData()) {
            return INF;
        } else {
            return maxData;
        }
    }

    /**
     * Getter method for <code>valueSeparator</code> property
     * <p>
     *
     * @return The value for the <code>valueSeparator</code> property
     */
    Options.Separator getValueSeparator() {
        return valueSeparator;
    }

    /**
     * Getter method for <code>detailSeparator</code> property
     * <p>
     *
     * @return The value for the <code>detailSeparator</code> property
     */
    Options.Separator getDetailSeparator() {
        return detailSeparator;
    }

    /**
     * Helper method required for option set cloning
     */
    int getLimit() {
        return limit;
    }

    /**
     * Indicate whether this set accepts data (which means that
     * <code>maxData</code> is 1 or larger).
     * <p>
     *
     * @return A boolean indicating whether this set accepts data
     */
    public boolean acceptsData() {
        return (minData + maxData) != 0;
    }

    /**
     * Return the data items found (these are the items on the command line
     * which do not start with the prefix, i. e. non-option arguments)
     * <p>
     *
     * @return A list of strings with all data items found
     */
    public List<String> getData() {
        return data;
    }

    /**
     * Return the number of data items found (these are the items on the command
     * line which do not start with the prefix, i. e. non-option arguments)
     * <p>
     *
     * @return The number of all data items found
     */
    public int getDataCount() {
        return data.size();
    }

    /**
     * Return a specific data item.
     * <p>
     *
     * @param index
     * @return The requested data item
     */
    public String getData(int index) {
        if (index < 0 || index >= getDataCount()) {
            if (getDataCount() == 0) {
                throw new IllegalArgumentException(CLASS + ": No data items are available");
            } else {
                int n = getDataCount() - 1;
                throw new IllegalArgumentException(CLASS + ": Invalid index value - must be between 0 and " + n);
            }
        }
        return data.get(index);
    }

    /**
     * Return all unmatched items found (these are the items on the command line
     * which start with the prefix, but do not match to one of the options)
     * <p>
     *
     * @return A list of strings with all unmatched items found
     */
    public List<String> getUnmatched() {
        return unmatched;
    }

    /**
     * Return the number of unmatched items found (these are the items on the
     * command line which start with the prefix, but do not match to one of the
     * options)
     * <p>
     *
     * @return The number of all unmatched items found
     */
    public int getUnmatchedCount() {
        return unmatched.size();
    }

    /**
     * Return a specific unmatched item.
     * <p>
     *
     * @param index
     * @return The requested unmatched item
     */
    public String getUnmatched(int index) {
        if (index < 0 || index >= getUnmatchedCount()) {
            if (getUnmatchedCount() == 0) {
                throw new IllegalArgumentException(CLASS + ": No unmatched items are available");
            } else {
                int n = getUnmatchedCount() - 1;
                throw new IllegalArgumentException(CLASS + ": Invalid index value - must be between 0 and " + n);
            }
        }
        return unmatched.get(index);
    }

// ==========================================================================================
// Add a non-value option
// ==========================================================================================

    /**
     * Add the given option to the set.
     * <p>
     *
     * @param type The type of the option
     * @param key  The name of the option
     *             <p>
     * @return The newly created option (to support invocation chaining)
     */
    public OptionData addOption(OptionData.Type type, String key) {
        return addOption(type,
                key,
                null,
                type.detail() ? detailSeparator : valueSeparator,
                defaultMultiplicity);
    }

    /**
     * @param type
     * @param key
     * @return
     */
    public OptionData addOption(OptionData.Type type, Enum key) {
        if (key == null) {
            throw new NullPointerException("key may not be null");
        }
        return addOption(type, key.toString());
    }

    /**
     * Add the given option to the set.
     * <p>
     *
     * @param type         The type of the option
     * @param key          The name of the option
     * @param multiplicity The multiplicity of the option
     *                     <p>
     * @return The newly created option (to support invocation chaining)
     */
    public OptionData addOption(OptionData.Type type, String key, Options.Multiplicity multiplicity) {
        return addOption(type,
                key,
                null,
                type.detail() ? detailSeparator : valueSeparator,
                multiplicity);
    }

    /**
     * @param type
     * @param key
     * @param multiplicity
     * @return
     */
    public OptionData addOption(OptionData.Type type, Enum key, Options.Multiplicity multiplicity) {
        if (key == null) {
            throw new NullPointerException("key may not be null");
        }
        return addOption(type, key.toString(), multiplicity);
    }

    /**
     * Add the given option to the set.
     * <p>
     *
     * @param type   The type of the option
     * @param key    The name of the option
     * @param altKey The alternate name of the option
     *               <p>
     * @return The newly created option (to support invocation chaining)
     */
    public OptionData addOption(OptionData.Type type, String key, String altKey) {
        return addOption(type,
                key,
                altKey,
                type.detail() ? detailSeparator : valueSeparator,
                defaultMultiplicity);
    }

    /**
     * @param type
     * @param key
     * @param altKey
     * @return
     */
    public OptionData addOption(OptionData.Type type, Enum key, Enum altKey) {
        if (key == null) {
            throw new NullPointerException("key may not be null");
        }
        if (altKey == null) {
            throw new NullPointerException("altKey may not be null");
        }
        return addOption(type, key.toString(), altKey.toString());
    }

    /**
     * Add the given option to the set.
     * <p>
     *
     * @param type         The type of the option
     * @param key          The name of the option
     * @param altKey       The alternate name of the option
     * @param multiplicity The multiplicity of the option
     *                     <p>
     * @return The newly created option (to support invocation chaining)
     */
    public OptionData addOption(OptionData.Type type, String key, String altKey, Options.Multiplicity multiplicity) {
        return addOption(type,
                key,
                altKey,
                type.detail() ? detailSeparator : valueSeparator,
                multiplicity);
    }

    /**
     * 
     * @param type
     * @param key
     * @param altKey
     * @param multiplicity
     * @return
     */
    public OptionData addOption(OptionData.Type type, Enum key, Enum altKey, Options.Multiplicity multiplicity) {
        if (key == null) {
            throw new NullPointerException("key may not be null");
        }
        if (altKey == null) {
            throw new NullPointerException("altKey may not be null");
        }
        return addOption(type, key.toString(), altKey.toString(), multiplicity);
    }

    /**
     * The master method to add an option. Since there are combinations which
     * are not acceptable (like a NONE separator and a true value), this method
     * is not public. Internally, we only supply acceptable combinations.
     */
    OptionData addOption(OptionData.Type type,
                         String key,
                         String altKey,
                         Options.Separator separator,
                         Options.Multiplicity multiplicity) {

        if (type == null) {
            throw new IllegalArgumentException(CLASS + ": type may not be null");
        }
        if (key == null) {
            throw new IllegalArgumentException(CLASS + ": key may not be null");
        }
        if (multiplicity == null) {
            throw new IllegalArgumentException(CLASS + ": multiplicity may not be null");
        }
        if (keys.containsKey(key)) {
            throw new IllegalArgumentException(CLASS + ": the key " + key + " has already been defined for this OptionSet");
        }
        if (altKey != null && altKeys.contains(altKey)) {
            throw new IllegalArgumentException(CLASS + ": the alternate key " + altKey + " has already been defined for this OptionSet");
        }

        //.... Check keys for valid names (especially no whitespace)
        Matcher m = keyPattern.matcher(key);
        if (!m.matches()) {
            throw new IllegalArgumentException(CLASS + ": invalid key: may only contain [a-zA-Z_0-9]");
        }
        if (altKey != null) {
            m = keyPattern.matcher(altKey);
            if (!m.matches()) {
                throw new IllegalArgumentException(CLASS + ": invalid alternate key: may only contain [a-zA-Z_0-9]");
            }
        }

        OptionData od = new OptionData(type, prefix, altPrefix, key, altKey, separator, multiplicity);
        options.add(od);
        keys.put(key, od);
        if (altKey != null) {
            altKeys.add(altKey);
        }

        return od;

    }

    /**
     * @param type
     * @param key
     * @param altKey
     * @param separator
     * @param multiplicity
     * @return
     */
    OptionData addOption(OptionData.Type type,
                         Enum key,
                         Enum altKey,
                         Options.Separator separator,
                         Options.Multiplicity multiplicity) {
        if (key == null) {
            throw new NullPointerException("key may not be null");
        }
        if (altKey == null) {
            throw new NullPointerException("altKey may not be null");
        }
        return addOption(type, key.toString(), altKey.toString(), separator, multiplicity);
    }

    /**
     * A convenience method that prints all the results obtained for this option
     * set to <code>System.out</code>. This is quite handy to quickly check
     * whether a set definition yields the expected results for a given set of
     * command line arguments.
     */
    public void printResults() {
        for (OptionData od : getOptionData()) {
            System.out.println("Option: " + od.getSyntax() + " (found " + od.getResultCount() + " time(s))");
            for (int i = 0; i < od.getResultCount(); i++) {
                if (od.useDetail()) {
                    System.out.println("- Detail " + i + ": " + od.getResultDetail(i));
                }
                if (od.useValue()) {
                    System.out.println("- Value  " + i + ": " + od.getResultValue(i));
                }
            }
        }
        for (String s : getData()) {
            System.out.println("Data : " + s);
        }
        for (String s : getUnmatched()) {
            System.out.println("Unmatched : " + s);
        }
    }
}
