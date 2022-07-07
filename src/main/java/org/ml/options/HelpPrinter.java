package org.ml.options;

/**
 * This interface is supposed to be implemented by all classes providing help printing 
 * capabilities.
 */
public interface HelpPrinter {

    /**
     * Return a string with the command line syntax for this option set
     * <p>
     * @param set         The {@link OptionSet} to format the output for 
     * @param leadingText The text to precede the command line
     * @param lineBreak   A boolean indicating whether the command line for the option set should
     *                    be printed with line breaks after each option or not
     * <p>
     * @return A string with the command line syntax for this option set
     */
    String getCommandLine(OptionSet set, String leadingText, boolean lineBreak);

    /**
     * Return the help text describing the different options and data arguments
     * <p>
     * @param set The {@link OptionSet} to format the output for 
     * <p>
     * @return A string with the help text for this option set
     */
    String getHelpText(OptionSet set);
}

