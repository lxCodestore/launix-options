package org.ml.options;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.xml.sax.SAXException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;

/**
 * The central class for option processing. Sets are identified by their name,
 * but there is also an anonymous default set, which is very convenient if an
 * application requires only one set.
 * <p>
 * The default values used in this class are:
 *
 * <table border=1>
 * <caption>Default caption</caption>
 * <tr><td colspan=4><b>Default Values</b>
 * <tr><td> <b>ID</b> <td> <b>Parameter</b> <td><b>Default</b>
 * <td><b>Individual Setting</b>
 * <tr><td> 1 <td> Prefix
 * <td>
 * <code>Prefix.SLASH</code> (Windows) <br>
 * <code>Prefix.DASH</code> (all others)
 * <td> No
 * <tr><td> 2 <td> Alternate Prefix
 * <td>
 * <code>Prefix.DOUBLEDASH</code>
 * <td> No
 * <tr><td> 3 <td> Separator for value options
 * <td>
 * <code>Separator.BLANK</code>
 * <td> No
 * <tr><td> 4 <td> Separator for detail options
 * <td>
 * <code>Separator.EQUALS</code>
 * <td> No
 * <tr><td> 5 <td> Min. Data
 * <td>
 * <code>0</code>
 * <td> Option set level
 * <tr><td> 6 <td> Max. Data
 * <td>
 * <code>0</code>
 * <td> Option set level
 * <tr><td> 7 <td> Multiplicity
 * <td>
 * <code>Multiplicity.ZERO_OR_ONCE</code>
 * <td> Option level
 * </table>
 * <p>
 * All of these values can be changed using one of the <code>setDefault()</code>
 * methods. However, for 1 - 4 this can only be done
 * <i>before</i> any actual set or option has been created (otherwise an
 * <code>UnsupportedOperationException</code> is thrown). 5 - 7 can be called
 * anytime, but they affect only sets and options which are created
 * <i>afterwards</i>.
 */
public class Options {

    private final static String CLASS = "Options";
    /**
     * The name used internally for the default set
     */
    private final static String DEFAULT_SET = "DEFAULT_OPTION_SET";

    // ==========================================================================================
    // Helper enums
    // ==========================================================================================

    /**
     * An enum encapsulating the possible separators between value options and
     * their actual values.
     */
    public enum Separator {

        /**
         * Separate option and value by ":"
         */
        COLON(':'),
        /**
         * Separate option and value by "="
         */
        EQUALS('='),
        /**
         * Separate option and value by blank space
         */
        BLANK(' ');      // Or, more precisely, whitespace (as allowed by the CLI)

        private char c;

        Separator(char c) {
            this.c = c;
        }

        /**
         * Return the actual separator character
         * <p>
         *
         * @return The actual separator character
         */
        char getName() {
            return c;
        }
    }

    // ==========================================================================================

    /**
     * An enum encapsulating the possible prefixes identifying options (and
     * separating them from command line data items)
     */
    public enum Prefix {

        /**
         * Options start with a "-" (typically on Unix platforms)
         */
        DASH("-"),
        /**
         * Options start with a "--" (like GNU-style options on Unix platforms)
         */
        DOUBLEDASH("--"),
        /**
         * Options start with a "/" (typically on Windows platforms)
         */
        SLASH("/");
        private String c;

        Prefix(String c) {
            this.c = c;
        }

        /**
         * Return the actual prefix character
         * <p>
         *
         * @return The actual prefix character
         */
        String getName() {
            return c;
        }
    }

    // ==========================================================================================

    /**
     * An enum encapsulating the possible multiplicities for options
     */
    public enum Multiplicity {

        /**
         * Option needs to occur exactly once
         */
        ONCE(true),
        /**
         * Option needs to occur at least once
         */
        ONCE_OR_MORE(true),
        /**
         * Option needs to occur either once or not at all
         */
        ZERO_OR_ONCE(false),
        /**
         * Option can occur any number of times
         */
        ZERO_OR_MORE(false);
        private boolean required = false;

        Multiplicity(boolean required) {
            this.required = required;
        }

        boolean isRequired() {
            return required;
        }
    }

    // ==========================================================================================
    // Instance members
    // ==========================================================================================
    private TreeMap<String, OptionSet> optionSets = new TreeMap<>();
    private String[] arguments;
    private boolean ignoreUnmatched = false;
    private StringBuilder checkErrors = new StringBuilder();
    //.... Defaults
    private Prefix defaultPrefix = getDefaultPrefix();
    private Prefix defaultAltPrefix = Prefix.DOUBLEDASH;
    private Separator defaultValueSeparator = Separator.BLANK;
    private Separator defaultDetailSeparator = Separator.EQUALS;
    private Multiplicity defaultMultiplicity = Multiplicity.ZERO_OR_ONCE;
    private int defaultMinData = 0;
    private int defaultMaxData = 0;

    /**
     * Constructor
     * <p>
     *
     * @param args The command line arguments to check
     */
    public Options(String[] args) {
        if (args == null) {
            throw new IllegalArgumentException(CLASS + ": args may not be null");
        }
        arguments = new String[args.length];
        int i = 0;
        for (String s : args) {
            arguments[i++] = s;
        }
    }

    /**
     * This constructor uses the XML file provided by the reader to set up
     * option sets and options.
     * <p>
     *
     * @param args   The command line arguments to check
     * @param reader The reader instance providing the XML file
     * @throws JDOMException If something went wrong in the JDOM library
     */
    public Options(String[] args, Reader reader) throws JDOMException {
        this(args);
        if (reader == null) {
            throw new IllegalArgumentException(CLASS + ": reader may not be null");
        }

        //.... Copy the XML content into a string. This is done since we need to read the data 
        //     twice (once for validation, once for evaluation), and not all readers support the 
        //     reset() method.
        StringBuilder sb = new StringBuilder(1000);
        String line;
        BufferedReader r = new BufferedReader(reader);

        try {
            while ((line = r.readLine()) != null) {
                sb.append(line);
                sb.append('\n');
            }
        } catch (IOException ex) {
            throw new XMLParsingException(CLASS + ": Error while reading XML file!\n" + ex.getMessage());
        }
        line = sb.toString();

        //.... Try to validate the XML document against the schema
        SchemaValidator validator = new SchemaValidator();

        try {
            if (!validator.validate(new BufferedReader(new StringReader(line)))) {
                throw new XMLParsingException(CLASS + ": Error in XML file validation against schema!\n" + validator.getError());
            }
        } catch (IOException | SAXException ex) {
            throw new XMLParsingException(CLASS + ": Error in XML file validation against schema!\n" + ex.getMessage());
        }

        //.... Retrieve the data and create the option sets and options
        try {

            SAXBuilder builder = new SAXBuilder();
            Document doc = builder.build(new BufferedReader(new StringReader(line)));

            //... Process the <options> tag
            Element root = doc.getRootElement();
            String value;
            String[] values;

            if (root.getAttribute("defData") != null) {
                value = root.getAttributeValue("defData");
                if (value.indexOf(':') < 0) {
                    setDefault(Integer.parseInt(value));
                } else {
                    values = value.split(":");
                    if (values[1].equals("INF")) {
                        setDefault(Integer.parseInt(values[0]), Integer.MAX_VALUE);
                    } else {
                        setDefault(Integer.parseInt(values[0]), Integer.parseInt(values[1]));
                    }
                }
            }

            if (root.getAttribute("defMult") != null) {
                setDefault(Multiplicity.valueOf(root.getAttributeValue("defMult")));
            }

            if (root.getAttribute("defSep") != null) {
                value = root.getAttributeValue("defSep");
                if (value.indexOf(':') < 0) {
                    setDefault(Separator.valueOf(value));
                } else {
                    values = value.split(":");
                    setDefault(Separator.valueOf(values[0]), Separator.valueOf(values[1]));
                }
            }

            if (root.getAttribute("defPrefix") != null) {
                value = root.getAttributeValue("defPrefix");
                if (value.indexOf(':') < 0) {
                    setDefault(Prefix.valueOf(value));
                } else {
                    values = value.split(":");
                    setDefault(Prefix.valueOf(values[0]), Prefix.valueOf(values[1]));
                }
            }

            //... Process the <set> tag(s)
            OptionSet set;
            boolean found = false;

            for (Element element : root.getChildren("set")) {

                if (element.getAttribute("data") != null) {          // Create the set
                    value = element.getAttributeValue("data");
                    if (value.indexOf(':') < 0) {
                        set = addSet(element.getAttributeValue("name"),
                                Integer.parseInt(value));
                    } else {
                        values = value.split(":");
                        if (values[1].equals("INF")) {                        // Allow unlimited number of data items
                            set = addSet(element.getAttributeValue("name"),
                                    Integer.parseInt(values[0]), Integer.MAX_VALUE);
                        } else {
                            set = addSet(element.getAttributeValue("name"),
                                    Integer.parseInt(values[0]), Integer.parseInt(values[1]));
                        }
                    }
                } else {
                    set = addSet(element.getAttributeValue("name"));
                }

                processSet(set, element);
                found = true;

            }

            //... Process the <defaultSet> tag (if present)
            if (root.getChild("defaultSet") != null) {
                processSet(getSet(), root.getChild("defaultSet"));
                found = true;
            }

            if (!found) {
                throw new XMLParsingException(CLASS + ": At least one option set needs to be defined");
            }

            //.... Process the <option> tags (for addOptionAllSets())
            for (Element element : root.getChildren("option")) {
                for (String name : optionSets.keySet()) {
                    addOptions(optionSets.get(name), element);
                }
            }

        } catch (java.io.IOException ex) {
            throw new XMLParsingException(CLASS + ": Error during XML file content validation!\n" + ex.getMessage());
        }

    }

    //.... Helper method to add the options to a set
    private void addOptions(OptionSet set, Element element) {

        String type = element.getAttributeValue("type");
        String key = element.getAttributeValue("key");
        String altKey = element.getAttributeValue("altKey");
        String mult = element.getAttributeValue("mult");
        String value;
        String detail;
        String help;
        OptionData.Type otype;

        //.... Create the option
        if (type.equals(OptionData.Type.SIMPLE.name())) {
            otype = OptionData.Type.SIMPLE;
            if (altKey == null) {
                if (mult == null) {
                    set.addOption(otype, key);
                } else {
                    set.addOption(otype, key, Multiplicity.valueOf(mult));
                }
            } else {
                if (mult == null) {
                    set.addOption(otype, key, altKey);
                } else {
                    set.addOption(otype, key, altKey, Multiplicity.valueOf(mult));
                }
            }
        } else {
            if (type.equals(OptionData.Type.VALUE.name())) {
                otype = OptionData.Type.VALUE;
            } else {
                otype = OptionData.Type.DETAIL;
            }
            if (altKey == null) {
                if (mult == null) {
                    set.addOption(otype, key);
                } else {
                    set.addOption(otype, key, Multiplicity.valueOf(mult));
                }
            } else {
                if (mult == null) {
                    set.addOption(otype, key, altKey);
                } else {
                    set.addOption(otype, key, altKey, Multiplicity.valueOf(mult));
                }
            }
        }

        //.... Add texts, if necessary
        if (element.getChild("text") != null) {   // Texts for this set

            Element textElement = element.getChild("text");

            value = textElement.getChildText("value");
            detail = textElement.getChildText("detail");
            help = textElement.getChildText("help");

            if (value != null) {
                if (detail != null) {
                    set.getOption(key).setValueText(value).setDetailText(detail).setHelpText(help);
                } else {
                    set.getOption(key).setValueText(value).setHelpText(help);
                }
            } else {
                set.getOption(key).setHelpText(help);
            }

        }

        //.... Add constraints, if necessary. Only instances of XMLConstraint are possible here, of course
        if (element.getChild("constraints") != null) {   // Constraints for this option

            try {

                Element constraints = element.getChild("constraints");
                XMLConstraint constr;
                for (Element constraint : constraints.getChildren("constraint")) {
                    constr = (XMLConstraint) Class.forName(constraint.getAttributeValue("class").trim()).newInstance();
                    constr.init(set.getOption(key), constraint.getChild("params").getChildren("param"));
                }

            } catch (InstantiationException | ClassNotFoundException | IllegalAccessException ex) {
                throw new XMLParsingException(CLASS + ": Could not create constraint instance", ex);
            }

        }

    }

    //.... Helper method to add the texts to a set
    private void addTexts(OptionSet set, Element element) {
        int index = Integer.parseInt(element.getAttributeValue("index"));
        String data = element.getChildText("data");
        String help = element.getChildText("help");

        if (help != null) {
            set.setDataText(index, data).setHelpText(index, help);
        } else {
            set.setDataText(index, data);
        }
    }

    /**
     * Helper method used when adding a set
     */
    private void processSet(OptionSet set, Element element) {

        for (Element subElement : element.getChildren("option")) {
            addOptions(set, subElement);
        }

        if (element.getChildren("text") != null) {
            for (Element subElement : element.getChildren("text")) {
                addTexts(set, subElement);
            }
        }

        if (element.getChild("constraints") != null) {   // Constraints for this set

            try {

                Element constraints = element.getChild("constraints");
                XMLConstraint constr;
                for (Element constraint : constraints.getChildren("constraint")) {
                    constr = (XMLConstraint) Class.forName(constraint.getAttributeValue("class").trim()).newInstance();
                    constr.init(set, constraint.getChild("params").getChildren("param"));
                }

            } catch (InstantiationException | ClassNotFoundException | IllegalAccessException ex) {
                throw new XMLParsingException(CLASS + ": Could not create constraint instance", ex);
            }

        }
    }

    // ==========================================================================================
    // Defaults handling
    // ==========================================================================================

    /**
     * Define the default to use for the separator for value options. Note that
     * this method can only be invoked <i>before</i> any option set has been
     * created.
     * <p>
     *
     * @param defaultValueSeparator The default separator to use for all value
     *                              options
     *                              <p>
     * @return This instance to allow for invocation chaining
     */
    public Options setDefault(Separator defaultValueSeparator) {
        if (defaultValueSeparator == null) {
            throw new IllegalArgumentException(CLASS + ": defaultValueSeparator may not be null");
        }
        if (optionSets.size() > 0) {
            throw new UnsupportedOperationException(CLASS + ": method can not be invoked, OptionSets have already been defined");
        }
        this.defaultValueSeparator = defaultValueSeparator;
        return this;
    }

    /**
     * Define the defaults to use for the separators for value and detail
     * options. Note that this method can only be invoked <i>before</i> any
     * option set has been created.
     * <p>
     *
     * @param defaultValueSeparator  The default separator to use for all value
     *                               options
     * @param defaultDetailSeparator The default separator to use for all detail
     *                               options
     *                               <p>
     * @return This instance to allow for invocation chaining
     */
    public Options setDefault(Separator defaultValueSeparator, Separator defaultDetailSeparator) {

        if (defaultValueSeparator == null) {
            throw new IllegalArgumentException(CLASS + ": defaultValueSeparator may not be null");
        }
        if (defaultDetailSeparator == null) {
            throw new IllegalArgumentException(CLASS + ": defaultDetailSeparator may not be null");
        }
        if (optionSets.size() > 0) {
            throw new UnsupportedOperationException(CLASS + ": method can not be invoked, OptionSets have already been defined");
        }

        this.defaultValueSeparator = defaultValueSeparator;
        this.defaultDetailSeparator = defaultDetailSeparator;

        return this;

    }

    /**
     * Define the default to use for the option prefix. Note that this method
     * can only be invoked <i>before</i> any option set has been created.
     * <p>
     *
     * @param defaultPrefix The prefix to use for all options
     *                      <p>
     * @return This instance to allow for invocation chaining
     */
    public Options setDefault(Prefix defaultPrefix) {
        if (defaultPrefix == null) {
            throw new IllegalArgumentException(CLASS + ": defaultPrefix may not be null");
        }
        if (optionSets.size() > 0) {
            throw new UnsupportedOperationException(CLASS + ": method can not be invoked, OptionSets have already been defined");
        }
        this.defaultPrefix = defaultPrefix;
        return this;
    }

    /**
     * Define the defaults to use for the option prefixes. Note that this method
     * can only be invoked <i>before</i> any option set has been created.
     * <p>
     *
     * @param defaultPrefix    The prefix to use for all options
     * @param defaultAltPrefix The prefix to use for all alternate keys for
     *                         options
     *                         <p>
     * @return This instance to allow for invocation chaining
     */
    public Options setDefault(Prefix defaultPrefix, Prefix defaultAltPrefix) {

        if (defaultPrefix == null) {
            throw new IllegalArgumentException(CLASS + ": defaultPrefix may not be null");
        }
        if (defaultAltPrefix == null) {
            throw new IllegalArgumentException(CLASS + ": defaultAltPrefix may not be null");
        }
        if (defaultPrefix.equals(defaultAltPrefix)) {
            throw new IllegalArgumentException(CLASS + ": The prefixes must be different");
        }
        if (optionSets.size() > 0) {
            throw new UnsupportedOperationException(CLASS + ": method can not be invoked, OptionSets have already been defined");
        }

        this.defaultPrefix = defaultPrefix;
        this.defaultAltPrefix = defaultAltPrefix;

        return this;

    }

    /**
     * Define the default to use for the multiplicity for options. This applies
     * only to option sets and options within these sets which are created
     * <i>after</i> this call.
     * <p>
     *
     * @param defaultMultiplicity The default multiplicity to use for all
     *                            options
     *                            <p>
     * @return This instance to allow for invocation chaining
     */
    public Options setDefault(Multiplicity defaultMultiplicity) {
        if (defaultMultiplicity == null) {
            throw new IllegalArgumentException(CLASS + ": defaultMultiplicity may not be null");
        }
        this.defaultMultiplicity = defaultMultiplicity;
        return this;
    }

    /**
     * Define the defaults to use for the number of data items for a set. This
     * applies only to option sets which are created <i>after</i> this call.
     * <p>
     *
     * @param defaultData The default minimum and maximum number of data items
     *                    <p>
     * @return This instance to allow for invocation chaining
     */
    public Options setDefault(int defaultData) {
        if (defaultData < 0) {
            throw new IllegalArgumentException(CLASS + ": defaultData must be >= 0");
        }
        this.defaultMinData = defaultData;
        this.defaultMaxData = defaultData;
        return this;
    }

    /**
     * Define the defaults to use for the number of data items for a set. This
     * applies only to option sets which are created <i>after</i> this call.
     * <p>
     *
     * @param defaultMinData The default minimum number of data items
     * @param defaultMaxData The default maximum number of data items
     *                       <p>
     * @return This instance to allow for invocation chaining
     */
    public Options setDefault(int defaultMinData, int defaultMaxData) {

        if (defaultMinData < 0) {
            throw new IllegalArgumentException(CLASS + ": defaultMinData must be >= 0");
        }

        int limit = defaultMaxData;
        if (defaultMaxData == OptionSet.INF) {
            limit = Integer.MAX_VALUE;
        }

        if (limit < defaultMinData) {
            throw new IllegalArgumentException(CLASS + ": defaultMaxData must be >= defaultMinData");
        }
        this.defaultMinData = defaultMinData;
        this.defaultMaxData = limit;
        return this;

    }

    // ==========================================================================================
    // The actual API
    // ==========================================================================================

    /**
     * Return the (first) matching set. This invocation does not ignore
     * unmatched options and requires that data items are the last ones on the
     * command line. It is equivalent to calling
     * <code>getMatchingSet(false, true)</code>.
     * <p>
     *
     * @return The first set which matches (i. e. the <code>check()</code>
     * method returns <code>true</code>) - or <code>null</code>, if no set
     * matches.
     */
    public OptionSet getMatchingSet() {
        return getMatchingSet(false, true);
    }

    /**
     * Return the (first) matching set.
     * <p>
     *
     * @param ignoreUnmatched A boolean to select whether unmatched options can
     *                        be ignored in the checks or not
     * @param requireDataLast A boolean to indicate whether the data items have
     *                        to be the last ones on the command line or not
     *                        <p>
     * @return The first set which matches (i. e. the <code>check()</code>
     * method returns <code>true</code>) - or <code>null</code>, if no set
     * matches.
     */
    public OptionSet getMatchingSet(boolean ignoreUnmatched, boolean requireDataLast) {

        // If we have no set at this stage, we need to create the default set since
        // chances are the user just wants to check for data (no options), and thus
        // getSet() has not been invoked by the user at this stage.
        if (optionSets.isEmpty()) {
            getSet();
        }

        // Run the checks for all known sets
        for (String name : optionSets.keySet()) {
            if (check(name, ignoreUnmatched, requireDataLast)) {
                return optionSets.get(name);
            }
        }

        return null;

    }

    /**
     * Add an option set.
     * <p>
     *
     * @param name    The name for the set. This must be a unique identifier
     * @param minData The minimum number of data items for this set
     * @param maxData The maximum number of data items for this set (if set * *
     *                to <code>OptionSet.INF</code>, this effectively corresponds to an
     *                unlimited number)
     *                <p>
     * @return The new <code>OptionSet</code> instance created. This is useful
     * to allow chaining of <code>addOption()</code> calls right after this
     * method
     */
    public OptionSet addSet(String name, int minData, int maxData) {
        if (name == null) {
            throw new IllegalArgumentException(CLASS + ": name may not be null");
        }
        if (optionSets.containsKey(name)) {
            throw new IllegalArgumentException(CLASS + ": a set with the name " + name + " has already been defined");
        }

        int limit = maxData;
        if (maxData == OptionSet.INF) {
            limit = Integer.MAX_VALUE;
        }

        OptionSet os = new OptionSet(name,
                defaultPrefix,
                defaultAltPrefix,
                defaultValueSeparator,
                defaultDetailSeparator,
                defaultMultiplicity,
                minData,
                limit,
                name.equals(DEFAULT_SET));
        optionSets.put(name, os);

        return os;

    }

    /**
     *
     * @param name
     * @param minData
     * @param maxData
     * @return
     */
    public OptionSet addSet(Enum name, int minData, int maxData) {
        if (name == null) {
            throw new NullPointerException("name may not be null");
        }
        return addSet(name, minData, maxData);
    }

    /**
     * Add an option set.
     * <p>
     *
     * @param name The name for the set. This must be a unique identifier
     * @param data The minimum and maximum number of data items for this set
     *             <p>
     * @return The new <code>OptionSet</code> instance created. This is useful
     * to allow chaining of <code>addOption()</code> calls right after this
     * method
     */
    public OptionSet addSet(String name, int data) {
        return addSet(name, data, data);
    }

    /**
     *
     * @param name
     * @param data
     * @return
     */
    public OptionSet addSet(Enum name, int data) {
        if (name == null) {
            throw new NullPointerException("name may not be null");
        }
        return addSet(name.toString(), data);
    }

    /**
     * Add an option set. The defaults for the number of data items are used.
     * <p>
     *
     * @param name The name for the set. This must be a unique identifier
     *             <p>
     * @return The new <code>OptionSet</code> instance created. This is useful
     * to allow chaining of <code>addOption()</code> calls right after this
     * method
     */
    public OptionSet addSet(String name) {
        return addSet(name, defaultMinData, defaultMaxData);
    }

    /**
     *
     * @param name
     * @return
     */
    public OptionSet addSet(Enum name) {
        if (name == null) {
            throw new NullPointerException("name may not be null");
        }
        return addSet(name.toString());
    }

    /**
     * Add an option set by cloning an existing set. Note that is designed for
     * setup purposes only, i. e. no check result data is copied either for the
     * set or any options. This method can be very handy if an application
     * requires two (or more) sets which have a lot of options in common and
     * differ only in a few of them. In this case, one would first create a set
     * with the common options, then clone any number of additionally required
     * sets, and add the non-common options to each of these sets.
     * <p>
     * Note that it is not possible to change the number of data items required
     * for the new set.
     * <p>
     *
     * @param name The name for the new set. This must be a unique identifier
     * @param set  The set to clone the new set from
     *             <p>
     * @return The new <code>OptionSet</code> instance created. This is useful
     * to allow chaining of <code>addOption()</code> calls right after this
     * method
     */
    public OptionSet addSet(String name, OptionSet set) {
        if (name == null) {
            throw new IllegalArgumentException(CLASS + ": name may not be null");
        }
        if (set == null) {
            throw new IllegalArgumentException(CLASS + ": set may not be null");
        }
        if (optionSets.containsKey(name)) {
            throw new IllegalArgumentException(CLASS + ": a set with the name " + name + " has already been defined");
        }

        OptionSet os = new OptionSet(name, set);
        optionSets.put(name, os);
        return os;
    }

    /**
     *
     * @param name
     * @param set
     * @return
     */
    public OptionSet addSet(Enum name, OptionSet set) {
        if (name == null) {
            throw new NullPointerException("name may not be null");
        }
        return addSet(name.toString(), set);
    }

    /**
     * Return an option set - or <code>null</code>, if no set with the given
     * name exists
     * <p>
     *
     * @param name The name for the set to retrieve
     *             <p>
     * @return The set to retrieve (or <code>null</code>, if no set with the
     * given name exists)
     */
    public OptionSet getSet(String name) {
        return optionSets.get(name);
    }

    /**
     * Print a help description for this instance using a
     * {@link DefaultHelpPrinter}. This method provides a basic service in the
     * sense that it loops over all known option sets and prints the command
     * line for each set. If <code>printTexts</code> is <code>true</code>, also
     * descriptive texts are printed for all options and the data arguments.
     * <p>
     * Note that default values are used for all the components of the helper
     * text, which can be overridden by various methods available in the
     * {@link OptionSet} and {@link OptionData} classes.
     * <p>
     *
     * @param leadingText The text to precede the command line for each option
     *                    set (see {@link HelpPrinter#getCommandLine(OptionSet, String, boolean)})
     * @param lineBreak   A boolean indicating whether the command lines for the
     *                    option sets should be printed with line breaks or not (see
     *                    {@link HelpPrinter#getCommandLine(OptionSet, String, boolean)})
     * @param printTexts  A boolean indicating whether the full help information
     *                    should be printer (command lines and description texts) or just the
     *                    command lines
     */
    public void printHelp(String leadingText, boolean lineBreak, boolean printTexts) {
        printHelp(new DefaultHelpPrinter(), leadingText, lineBreak, printTexts);
    }

    /**
     * Print a help description for this instance using the provided
     * {@link HelpPrinter}. This method provides a basic service in the sense
     * that it loops over all known option sets and prints the command line for
     * each set. If <code>printTexts</code> is <code>true</code>, also
     * descriptive texts are printed for all options and the data arguments.
     * <p>
     * Note that default values are used for all the components of the helper
     * text, which can be overridden by various methods available in the
     * {@link OptionSet} and {@link OptionData} classes.
     * <p>
     *
     * @param helpPrinter The {@link HelpPrinter} to use to format the output
     * @param leadingText The text to precede the command line for each option
     *                    set (see {@link HelpPrinter#getCommandLine(OptionSet, String, boolean)})
     * @param lineBreak   A boolean indicating whether the command lines for the
     *                    option sets should be printed with line breaks or not (see
     *                    {@link HelpPrinter#getCommandLine(OptionSet, String, boolean)})
     * @param printTexts  A boolean indicating whether the full help information
     *                    should be printer (command lines and description texts) or just the
     *                    command lines
     */
    public void printHelp(HelpPrinter helpPrinter, String leadingText, boolean lineBreak, boolean printTexts) {

        if (helpPrinter == null) {
            throw new IllegalArgumentException(CLASS + ": helpPrinter may not be null");
        }
        if (leadingText == null) {
            throw new IllegalArgumentException(CLASS + ": leadingText may not be null");
        }

        OptionSet set;

        //.... No sets are defined, we only work with the default set
        if (getSetNames().isEmpty()) {
            set = getSet();
            System.out.println(helpPrinter.getCommandLine(set, leadingText, lineBreak));
            if (printTexts) {
                System.out.print('\n');
                System.out.println(helpPrinter.getHelpText(set));
            }

            //.... Loop over all defined sets
        } else {
            Set<String> sets = getSetNames();
            for (String name : sets) {
                set = getSet(name);
                System.out.println(helpPrinter.getCommandLine(set, leadingText, lineBreak));
                if (printTexts) {
                    System.out.print('\n');
                    System.out.println(helpPrinter.getHelpText(set));
                    if (sets.size() > 1) {
                        System.out.print('\n');
                    }
                }
            }
        }

    }

    /**
     * Get a set view on the known option set names. This is not public since it
     * includes the default set name as well, which we don't want to expose.
     * <p>
     *
     * @return A set containing the names of the option sets, * * * * or
     * <code>null</code> if no such sets are defined
     * <p>
     */
    Set<String> getSetNames() {
        return optionSets.keySet();
    }

    /**
     * This returns the (anonymous) default set
     * <p>
     *
     * @return The default set
     */
    public OptionSet getSet() {
        if (getSet(DEFAULT_SET) == null) {
            addSet(DEFAULT_SET, defaultMinData, defaultMaxData);
        }
        return getSet(DEFAULT_SET);
    }

    /**
     * Determine a default prefix depending on the OS platform. This uses the
     * <code>os.name</code> Java system property. For Windows,
     * <code>Prefix.SLASH</code> is used, else <code>Prefix.DASH</code>. This
     * will likely need to be adapted over time for other platforms and VMs,
     * depending on the string they return for that Java system property.
     * <p>
     *
     * @return The default prefix for the current platform
     */
    private static Prefix getDefaultPrefix() {
        String os = System.getProperty("os.name");
        if (os.startsWith("Windows")) {
            return Prefix.SLASH;
        } else {
            return Prefix.DASH;
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

        sb.append("defaultPrefix          = ");
        sb.append(defaultPrefix);
        sb.append('\n');
        sb.append("defaultAltPrefix       = ");
        sb.append(defaultAltPrefix);
        sb.append('\n');
        sb.append("defaultValueSeparator  = ");
        sb.append(defaultValueSeparator);
        sb.append('\n');
        sb.append("defaultDetailSeparator = ");
        sb.append(defaultDetailSeparator);
        sb.append('\n');
        sb.append("defaultMultiplicity    = ");
        sb.append(defaultMultiplicity);
        sb.append('\n');
        sb.append("defaultMinData         = ");
        sb.append(defaultMinData);
        sb.append('\n');
        sb.append("defaultMaxData         = ");
        sb.append(defaultMaxData);
        sb.append('\n');

        for (OptionSet set : optionSets.values()) {
            sb.append("Set: ");
            sb.append(set.getName());
            sb.append('\n');
            for (OptionData data : set.getOptionData()) {
                sb.append(data.toString());
                sb.append('\n');
            }
        }

        return sb.toString();

    }

    // ==========================================================================================
    // The checks 
    // ==========================================================================================

    /**
     * The error messages collected during the last option check (invocation of
     * any of the <code>check()</code> methods). This is useful to determine
     * what was wrong with the command line arguments provided
     * <p>
     *
     * @return A string with all collected error messages
     */
    public String getCheckErrors() {
        return checkErrors.toString();
    }

    /**
     * Run the checks for the default set with default parameters. This is
     * equivalent to calling <code>check(false, true)</code>. If the default set
     * has not yet been used at all, it is created here with the default
     * settings.
     * <p>
     *
     * @return A boolean indicating whether all checks were successful or not
     */
    public boolean check() {
        if (getSet(DEFAULT_SET) == null) {
            addSet(DEFAULT_SET, defaultMinData, defaultMaxData);
        }
        return check(DEFAULT_SET, false, true);
    }

    /**
     * Run the checks for the default set. If the default set has not yet been
     * used at all, it is created here with the default settings.
     * <p>
     *
     * @param ignoreUnmatched A boolean to select whether unmatched options can
     *                        be ignored in the checks or not
     * @param requireDataLast A boolean to indicate whether the data items have
     *                        to be the last ones on the command line or not
     *                        <p>
     * @return A boolean indicating whether all checks were successful or not
     */
    public boolean check(boolean ignoreUnmatched, boolean requireDataLast) {
        if (getSet(DEFAULT_SET) == null) {
            addSet(DEFAULT_SET, defaultMinData, defaultMaxData);
        }
        return check(DEFAULT_SET, ignoreUnmatched, requireDataLast);
    }

    /**
     * Run the checks for the given set with default parameters. This is
     * equivalent to calling <code>check(name, false, true)</code>.
     * <p>
     *
     * @param name The name for the set to check
     *             <p>
     * @return A boolean indicating whether all checks were successful or not
     */
    public boolean check(String name) {
        return check(name, false, true);
    }

    /**
     * Run the checks for the given set.
     * <p>
     *
     * @param name            The name for the set to check
     * @param ignoreUnmatched A boolean to select whether unmatched options can
     *                        be ignored in the checks or not
     * @param requireDataLast A boolean to indicate whether the data items have
     *                        to be the last ones on the command line or not
     *                        <p>
     * @return A boolean indicating whether all checks were successful or not
     */
    public boolean check(String name, boolean ignoreUnmatched, boolean requireDataLast) {

        if (name == null) {
            throw new IllegalArgumentException(CLASS + ": name may not be null");
        }
        if (optionSets.get(name) == null) {
            throw new IllegalArgumentException(CLASS + ": Unknown OptionSet: " + name);
        }

        checkErrors.append("Checking set ");
        checkErrors.append(name);
        checkErrors.append('\n');

        //.... Access the data for the set to use
        OptionSet set = optionSets.get(name);
        List<OptionData> options = set.getOptionData();
        List<String> data = set.getData();
        List<String> unmatched = set.getUnmatched();

        //.... Catch some trivial cases
        if (options.isEmpty()) {                             // No options have been defined at all
            if (arguments.length == 0) {
                if (set.getMinData() > 0) {
                    checkErrors.append("The set expects data, but no arguments have been given\n");
                    return false;
                } else {         // No options and no data expected, no arguments given - technically true, but useless
                    return true;
                }
            }
        } else if (set.isPurelyOptional() & arguments.length == 0) {  // No arguments provided and set only has optional options
            return true;

        } else if (arguments.length == 0) {     // Options have been defined, but no arguments given
            checkErrors.append("Options have been defined, but no arguments have been given; nothing to check\n");
            return false;
        }

        //.... Parse all the arguments given
        int ipos = 0;
        int offset;
        int start;
        Matcher m;
        String value;
        String detail;
        String next;
        String key;
        String pre = defaultPrefix.getName();
        String altPre = defaultAltPrefix.getName();
        boolean add;
        boolean[] matched = new boolean[arguments.length];

        for (int i = 0; i < matched.length; i++) // Initially, we assume there was no match at all
        {
            matched[i] = false;
        }

        while (true) {

            value = null;
            detail = null;
            offset = 0;
            start = 1;
            add = true;
            key = arguments[ipos];

            for (OptionData optionData : options) {          // For each argument, we need to check all defined options

                m = optionData.getPattern().matcher(key);

                if (m.lookingAt()) {

                    if (optionData.useValue()) {                          // The code section for value options

                        if (optionData.hasAlternateKey()) {
                            start = 2;
                        }

                        if (optionData.useDetail()) {
                            detail = m.group(start);
                            offset = 2;                                       // required for correct Matcher.group access below
                        }

                        if (optionData.getSeparator() == Separator.BLANK) { // In this case, the next argument must be the value
                            if (ipos + 1 == arguments.length) {               // The last argument, thus no value follows it: Error
                                checkErrors.append("At end of arguments - no value found following argument ");
                                checkErrors.append(key);
                                checkErrors.append('\n');
                                add = false;
                            } else {
                                next = arguments[ipos + 1];
                                if (next.startsWith(pre) || next.startsWith(altPre)) {   // The next item is not a value: Error
                                    checkErrors.append("No value found following argument ");
                                    checkErrors.append(key);
                                    checkErrors.append('\n');
                                    add = false;
                                } else {
                                    value = next;
                                    matched[ipos++] = true;                       // Mark the key and the value
                                    matched[ipos] = true;
                                }
                            }
                        } else {                                            // The value follows the separator in this case
                            value = m.group(start + offset);
                            matched[ipos] = true;
                        }

                    } else {                                              // Simple, non-value options
                        matched[ipos] = true;
                    }

                    if (add) {
                        optionData.addResult(value, detail);         // Store the result
                    }
                    break;                                                // No need to check more options, we have a match
                }
            }

            ipos++;                                                   // Advance to the next argument to check
            if (ipos >= arguments.length) {
                break;
            }                      // Terminating condition for the check loop

        }

        //.... Identify unmatched arguments and actual (non-option) data
        int first = -1;                                             // Required later for requireDataLast
        for (int i = 0; i < matched.length; i++) {                  // Assemble the list of unmatched options
            if (!matched[i]) {
                if (arguments[i].startsWith(pre) || arguments[i].startsWith(altPre)) {   // An unmatched option
                    unmatched.add(arguments[i]);
                    checkErrors.append("No matching option found for argument ");
                    checkErrors.append(arguments[i]);
                    checkErrors.append('\n');
                } else {                                                // This is actual data
                    if (first < 0) {
                        first = i;
                    }
                    data.add(arguments[i]);
                }
            }
        }

        //.... Checks to determine overall success, start with the multiplicity of options
        boolean err;

        for (OptionData optionData : options) {

            if (!optionData.isExclusive()) {              // Only check options which are not part of an ExclusiveConstraint

                key = optionData.getKey();
                err = false;                                // Local check result for one option

                switch (optionData.getMultiplicity()) {
                    case ONCE:
                        if (optionData.getResultCount() != 1) {
                            err = true;
                        }
                        break;
                    case ONCE_OR_MORE:
                        if (optionData.getResultCount() == 0) {
                            err = true;
                        }
                        break;
                    case ZERO_OR_ONCE:
                        if (optionData.getResultCount() > 1) {
                            err = true;
                        }
                        break;
                }

                if (err) {
                    checkErrors.append("Wrong number of occurences found for argument ");
                    checkErrors.append(pre);
                    checkErrors.append(key);
                    checkErrors.append('\n');
                    return false;
                }

            }

        }

        //.... Check defined constraints for all options
        for (OptionData optionData : options) {

            if (optionData.isSet() && (optionData.getConstraints() != null)) {
                for (Constraint constraint : optionData.getConstraints()) {
                    if (!constraint.isSatisfied()) {
                        checkErrors.append("Constraint ");
                        checkErrors.append(constraint.toString());
                        checkErrors.append(" violated for option '");
                        checkErrors.append(optionData.getKey());
                        checkErrors.append("'\n");
                        return false;
                    }
                }
            }

        }

        //.... Check defined constraints for the current set
        if (set.getConstraints() != null) {
            for (Constraint constraint : set.getConstraints()) {
                if (!constraint.isSatisfied()) {
                    checkErrors.append("Constraint ");
                    checkErrors.append(constraint.toString());
                    checkErrors.append(" violated for option set '");
                    checkErrors.append(set.getName());
                    checkErrors.append("'\n");
                    return false;
                }
            }
        }

        //.... Check range for data
        int limit = set.getMaxData();
        if (set.hasUnlimitedData()) {
            limit = Integer.MAX_VALUE;
        }

        if (data.size() < set.getMinData() || data.size() > limit) {
            checkErrors.append("Invalid number of data arguments: ");
            checkErrors.append(data.size());
            checkErrors.append(" (allowed range: ");
            checkErrors.append(set.getMinData());
            checkErrors.append(" ... ");
            checkErrors.append(set.getMaxData());
            checkErrors.append(")\n");
            return false;
        }

        //.... Check for location of the data in the list of command line arguments
        if (requireDataLast && data.size() > 0) {
            if (first + data.size() != arguments.length) {
                checkErrors.append("Invalid data specification: data arguments are not the last ones on the command line\n");
                return false;
            }
        }

        //.... Check for unmatched arguments
        // Don't accept unmatched arguments
        //.... If we made it to here, all checks were successful
        return ignoreUnmatched || unmatched.size() <= 0;


    }

    // ==========================================================================================
    // Add a value option for all sets
    // ==========================================================================================

    /**
     * Add the given option to <i>all</i> known sets.
     * <p>
     *
     * @param type The type of the option
     * @param key  The name of the option
     */
    public void addOptionAllSets(OptionData.Type type, String key) {
        for (String name : optionSets.keySet()) {
            optionSets.get(name).addOption(type,
                    key,
                    null,
                    type.detail() ? defaultDetailSeparator : defaultValueSeparator,
                    defaultMultiplicity);
        }
    }

    /**
     * Add the given option to <i>all</i> known sets.
     * <p>
     *
     * @param type         The type of the option
     * @param key          The name of the option
     * @param multiplicity The multiplicity of the option
     */
    public void addOptionAllSets(OptionData.Type type, String key, Multiplicity multiplicity) {
        for (String name : optionSets.keySet()) {
            optionSets.get(name).addOption(type,
                    key,
                    null,
                    type.detail() ? defaultDetailSeparator : defaultValueSeparator,
                    multiplicity);
        }
    }

    /**
     * Add the given option to <i>all</i> known sets.
     * <p>
     *
     * @param type   The type of the option
     * @param key    The name of the option
     * @param altKey The alternate name of the option
     */
    public void addOptionAllSets(OptionData.Type type, String key, String altKey) {
        for (String name : optionSets.keySet()) {
            optionSets.get(name).addOption(type,
                    key,
                    altKey,
                    type.detail() ? defaultDetailSeparator : defaultValueSeparator,
                    defaultMultiplicity);
        }
    }

    /**
     * Add the given option to <i>all</i> known sets.
     * <p>
     *
     * @param type         The type of the option
     * @param key          The name of the option
     * @param altKey       The alternate name of the option
     * @param multiplicity The multiplicity of the option
     */
    public void addOptionAllSets(OptionData.Type type, String key, String altKey, Multiplicity multiplicity) {
        for (String name : optionSets.keySet()) {
            optionSets.get(name).addOption(type,
                    key,
                    altKey,
                    type.detail() ? defaultDetailSeparator : defaultValueSeparator,
                    multiplicity);
        }
    }
}
