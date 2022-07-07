package org.ml.options;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple implementation of the {@link HelpPrinter} interface. This can serve
 * as a basis for more complex formatting requirements.
 * <p>
 * The following approach is used here for the command line syntax:
 *
 * <table border=1>
 * <caption>Default caption</caption>
 * <tr><td colspan=3><b>Option Output Format</b>
 * <tr><td><b> Component </b><td><b> Example</b> <td>
 * <b> Remark </b>
 * <tr><td>
 * <code>OptionData.Type.SIMPLE</code> option
 * <td>
 * <code>-a </code>
 * <td> &nbsp;
 * <tr><td>
 * <code>OptionData.Type.VALUE</code> option
 * <td>
 * <code>-log &lt;logfile&gt; </code>
 * <td> The text <code>logfile</code> can be changed using
 * <code>OptionData.setValueText()</code>
 * <tr><td>
 * <code>OptionData.Type.DETAIL</code> option
 * <td>
 * <code>-D&lt;detail&gt;=&lt;value&gt; </code>
 * <td> The text <code>value</code> can be changed using
 * <code>OptionData.setValueText()</code>, the text <code>detail</code> can be
 * changed using <code>OptionData.setDetailText()</code>
 * <tr><td> Option names with alternate keys
 * <td>
 * <code> (-a|--Access)</code>
 * <td> &nbsp;
 * <tr><td>
 * <code>Options.Multiplicity.ZERO_OR_ONCE</code>
 * <td>
 * <code> [-a] </code>
 * <td> &nbsp;
 * <tr><td>
 * <code>Options.Multiplicity.ONCE_OR_MORE</code>
 * <td>
 * <code> -v=&lt;value1&gt; [ -v=&lt;value2&gt; [...]] </code>
 * <td> The text <code>value</code> can be changed using
 * <code>OptionData.setValueText()</code>
 * <tr><td>
 * <code>Options.Multiplicity.ZERO_OR_MORE</code>
 * <td>
 * <code> [-v=&lt;value1&gt; [ -v=&lt;value2&gt; [...]]] </code>
 * <td> The text <code>value</code> can be changed using
 * <code>OptionData.setValueText()</code>
 * <tr><td> Exclusive constraints
 * <td>
 * <code> { &lt;option1&gt; | &lt;option2&gt; | &lt;option3&gt; } </code>
 * <td>
 * <code>&lt;optionN&gt;</code> is a placeholder for the general option syntax
 * described above which is grouped here using the curly brackets and the pipe
 * symbol
 * </table>
 */
public class DefaultHelpPrinter implements HelpPrinter {

    private final static String CLASS = "DefaultHelpPrinter";

    /**
     * Return the help text describing the different options and data arguments
     * <p>
     *
     * @param set The {@link OptionSet} to format the output for
     *            <p>
     * @return A string with the help text for this option set
     */
    @Override
    public String getHelpText(OptionSet set) {

        if (set == null) {
            throw new IllegalArgumentException(CLASS + ": set may not be null");
        }

        //.... Collect option and data item names
        String st;
        StringBuilder sb;
        List<String> out = new ArrayList<>();
        int maxLen = 0;

        for (OptionData option : set.getOptionData()) {
            st = option.getSyntax();
            if (st.length() > maxLen) {
                maxLen = st.length();
            }
            out.add(st);
        }

        int limit = set.getMaxData();
        if (set.hasUnlimitedData()) {
            limit = set.getMinData() + 1;
        }

        for (int i = 0; i < limit; i++) {
            st = dataSyntax(i, set.getDataText(i), set.getMinData(), set.getMaxData());
            if (st.length() > maxLen) {
                maxLen = st.length();
            }
            out.add(st);
        }

        //.... Assemble final output
        StringBuilder s = new StringBuilder(100);
        for (int i = 0; i < maxLen + 3; i++) {
            s.append(' ');
        }
        String blank = s.toString();

        sb = new StringBuilder(300);

        //.... Help texts for options
        int i = 0;
        String k;
        String[] texts;
        boolean first;

        for (OptionData option : set.getOptionData()) {

            k = out.get(i++) + blank;
            texts = option.getHelpText().split("\n");
            first = true;

            for (String text : texts) {
                if (first) {
                    sb.append(k, 0, maxLen);
                    sb.append(" : ");
                    first = false;
                } else {
                    sb.append(blank);
                }
                sb.append(text);
                sb.append('\n');
            }

        }

        //.... Help texts for data items
        for (int j = 0; j < limit; j++) {

            k = out.get(i++) + blank;
            texts = set.getHelpText(j).split("\n");
            first = true;

            for (String text : texts) {
                if (first) {
                    sb.append(k, 0, maxLen);
                    sb.append(" : ");
                    first = false;
                } else {
                    sb.append(blank);
                }
                sb.append(text);
                sb.append('\n');
            }

        }

        if (sb.length() > 0) {
            sb.deleteCharAt(sb.length() - 1);
        }   // Delete last \n

        return sb.toString();

    }

    /**
     * Return a string with the command line syntax for the given option set
     * <p>
     *
     * @param set         The {@link OptionSet} to format the output for
     * @param leadingText The text to precede the command line
     * @param lineBreak   A boolean indicating whether the command line for the
     *                    option set should be printed with line breaks after each option or not
     *                    <p>
     * @return A string with the command line syntax for this option set
     */
    @Override
    public String getCommandLine(OptionSet set, String leadingText, boolean lineBreak) {

        if (set == null) {
            throw new IllegalArgumentException(CLASS + ": set may not be null");
        }

        int limit = set.getMaxData();
        if (set.hasUnlimitedData()) {
            limit = set.getMinData() + 1;
        }

        StringBuilder sb = new StringBuilder(200);
        StringBuilder s = new StringBuilder();
        String[] d;

        if (leadingText != null) {
            sb.append(leadingText.trim());
            sb.append(' ');
            d = leadingText.trim().split("\\n");                    // We may have \n in the leading text string
            for (int i = 0; i <= d[d.length - 1].length(); i++) {
                s.append(' ');
            }
        }

        //.... Options (not part of an ExclusiveConstraint)
        boolean first = true;
        for (OptionData option : set.getOptionData()) {
            if (!option.isExclusive()) {
                if (lineBreak && !first) {
                    sb.append(s);
                } else {
                    first = false;
                }
                sb.append(option.getSyntax());
                if (lineBreak) {
                    sb.append('\n');
                } else {
                    sb.append(' ');
                }
            }
        }

        //.... ExclusiveConstraint options
        if (set.getConstraints() != null) {

            ExclusiveConstraint ec;

            for (Constraint constraint : set.getConstraints()) {

                if (constraint instanceof ExclusiveConstraint) {

                    ec = (ExclusiveConstraint) constraint;

                    if (lineBreak && !first) {
                        sb.append(s);
                    } else {
                        first = false;
                    }

                    sb.append('{');                                 // This identifies this type of constraint
                    for (OptionData option : ec.getOptionData()) {
                        sb.append(option.getSyntax());
                        sb.append('|');                               // Separator in between options
                    }
                    sb.deleteCharAt(sb.length() - 1);               // Remove last | character
                    sb.append('}');                                 // End of constraint output

                    if (lineBreak) {
                        sb.append('\n');
                    } else {
                        sb.append(' ');
                    }

                }

            }
        }

        //.... Data
        if (set.acceptsData()) {
            if (lineBreak && set.getOptionData().size() > 0) {
                sb.append(s);
            }
            for (int i = 0; i < limit; i++) {
                sb.append(dataSyntax(i, set.getDataText(i), set.getMinData(), set.getMaxData()));
                sb.append(' ');
            }
        }

        sb.deleteCharAt(sb.length() - 1);

        return sb.toString();

    }

    /**
     * Helper
     */
    private String dataSyntax(int index, String text, int minData, int maxData) {
        if (index < minData) {
            return "<" + text + ">";
        }
        if (maxData == OptionSet.INF) {
            return "[<" + text + "> [...]]";
        }
        return "[<" + text + ">]";
    }
}
