package org.ml.options;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jdom2.Element;

/**
 * A constraint combining one or more options such that just one of them can
 * occur. This type of constraint can only be added to an option set as it
 * combines one or more options.
 * <p>
 * Constraints of this kind are also accounted for in the
 * {@link DefaultHelpPrinter} to format the output provided.
 */
public class ExclusiveConstraint implements XMLConstraint {

    private static final String CLASS = "ExclusiveConstraint";
    private List<OptionData> optionData = new ArrayList<>();
    private Options.Multiplicity multiplicity = null;

    /**
     * The public no-org constructor. This is a prereq for all constraints since
     * it is used for initialization based on XML data.
     */
    public ExclusiveConstraint() {
    }

    /**
     * This method is used to initialize this constraint based on data read from
     * an XML configuration file. The method is invoked internally during setup
     * with the instance of {@link Constrainable} to which the constraint
     * applies and a list of JDOM elements, which contain the details about the
     * constraint itself.
     * <p>
     * This method initializes the constraint and attaches it to the list of
     * constraints of the {@link Constrainable} instance.
     * <p>
     * The parameters expected in the XML <code>&lt;param&gt;</code> tags for
     * this constraint are
     *
     * <table border=1>
     * <caption>Default caption</caption>
     * <tr> <td> <b >Name</b> </td><td> <b>Value</b> </td><td>Status</td>
     * </tr>
     * <tr> <td> keys </td><td> Same as the <code>keys</code> parameter in
     * {@link #add(OptionSet, Options.Multiplicity, String[])} </td><td>
     * Required</td></tr>
     * <tr> <td> mult </td><td> Same as the <code>multiplicity</code> parameter
     * in {@link #add(OptionSet, Options.Multiplicity, String[])} </td><td>
     * Optional</td></tr>
     * </table>
     * <p>
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
            throw new IllegalArgumentException(CLASS + ": Constrainable must be instance of OptionSet");
        }

        //.... Extract all parameters
        Map<String, String> params = new HashMap<>();
        for (Element param : list) {
            params.put(param.getAttributeValue("name").trim(), param.getAttributeValue("value").trim());
        }

        //.... Checks
        if (!params.containsKey("keys")) {
            throw new IllegalArgumentException(CLASS + ": missing <param> element with attribute named 'keys'");
        }

        Options.Multiplicity mult = Options.Multiplicity.valueOf(params.get("mult"));

        //.... Add the constraint
        if (params.containsKey("mult")) {
            add((OptionSet) constrainable, mult, params.get("keys").split("\\|"));
        } else {
            add((OptionSet) constrainable, params.get("keys").split("\\|"));
        }

    }

    /**
     * Add a constraint to the given option set
     * <p>
     *
     * @param optionSet The {@link OptionSet} to add this constraint to
     * @param multiplicity The {@link Options.Multiplicity} to use for all
     * options tied together by these constraints. A <code>Multiplicity</code>
     * defined previously for any option contained in this constraint is
     * overridden.
     * @param keys The keys of the options to tie together by this constraint.
     * At least two keys must be given here, and the corresponding options must
     * already be defined in the set.
     */
    public static void add(OptionSet optionSet, Options.Multiplicity multiplicity, String... keys) {
        if (optionSet == null) {
            throw new IllegalArgumentException(CLASS + ": optionSet may not be null");
        }
        if (multiplicity == null) {
            throw new IllegalArgumentException(CLASS + ": multiplicity may not be null");
        }
        if (keys.length < 2) {
            throw new IllegalArgumentException(CLASS + ": at least two keys must be provided");
        }
        optionSet.addConstraint(new ExclusiveConstraint(optionSet, multiplicity, keys));
    }

    /**
     * Add a constraint to the given option set using the default multiplicity
     * defined for this set
     * <p>
     *
     * @param optionSet The {@link OptionSet} to add this constraint to
     * @param keys The keys of the options to tie together by this constraint.
     * At least two keys must be given here, and the corresponding options must
     * already be defined in the set.
     */
    public static void add(OptionSet optionSet, String... keys) {
        if (optionSet == null) {
            throw new IllegalArgumentException(CLASS + ": optionSet may not be null");
        }
        if (keys.length < 2) {
            throw new IllegalArgumentException(CLASS + ": at least two keys must be provided");
        }
        optionSet.addConstraint(new ExclusiveConstraint(optionSet, optionSet.getDefaultMultiplicity(), keys));
    }

    /**
     * Constructor
     */
    ExclusiveConstraint(OptionSet optionSet, Options.Multiplicity multiplicity, String[] keys) {
        OptionData od;
        for (String key : keys) {
            od = optionSet.getOption(key);
            if (od.isExclusive()) {
                throw new IllegalArgumentException(CLASS + ": option '" + key + "' is already part of an " + CLASS);
            }
            optionData.add(od);
            od.setExclusive(true);
            od.setMultiplicity(multiplicity);
            this.multiplicity = multiplicity;
        }
    }

    /**
     *
     */
    Options.Multiplicity getMultiplicity() {
        return multiplicity;
    }

    /**
     *
     */
    List<OptionData> getOptionData() {
        return optionData;
    }

    /**
     * Indicates whether a constraint supports a given type of
     * {@link Constrainable}
     * <p>
     *
     * @param constrainable The constraint to check
     * @return A boolean to indicate whether this {@link Constrainable} is
     * supported. This constraint only supports {@link OptionSet} constrainables
     */
    @Override
    public boolean supports(Constrainable constrainable) {
        if (constrainable == null) {
            throw new IllegalArgumentException(CLASS + ": constrainable may not be null");
        }
        return constrainable instanceof OptionSet;
    }

    /**
     * The actual check routine
     * <p>
     *
     * @return A boolean indicating whether the constraint is satisfied or not
     */
    @Override
    public boolean isSatisfied() {

        //.... Check whether only one of the grouped options appears
        boolean found = false;
        OptionData odata = null;
        for (OptionData od : optionData) {
            if (od.getResultCount() > 0) {
                if (found) {
                    return false;
                }               // We found the second one - failure
                found = true;                          // We found the first one
                odata = od;
            }
        }

        if (!found) {
            return false;
        }       // No occurence found - constraint not satisfied

        //.... Check multiplicity for the one option found
        switch (multiplicity) {
            case ONCE:
                if (odata.getResultCount() != 1) {
                    return false;
                }
                break;
            case ONCE_OR_MORE:
                if (odata.getResultCount() == 0) {
                    return false;
                }
                break;
            case ZERO_OR_ONCE:
                if (odata.getResultCount() > 1) {
                    return false;
                }
                break;
        }

        return true;

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
        for (OptionData od : optionData) {
            sb.append(od.getKey());
            sb.append("|");
        }
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }
}
