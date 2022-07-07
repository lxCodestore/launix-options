package org.ml.options;

/**
 * The interface for all constraints. Custom constraints need to implement this
 * interface.
 */
public interface Constraint {

    /**
     * Check whether a constraint is satisfied. This method can be invoked after
     * a set of command line arguments has been analyzed such that the results
     * are known for each option and option set.
     * <p>
     *
     * @return A boolean to indicate whether a constraint is satisfied or not
     */
    boolean isSatisfied();

    /**
     * Indicates whether a constraint supports a given type of
     * {@link Constrainable}
     * <p>
     *
     * @param constrainable The constraint to check for
     * @return A boolean to indicate whether this {@link Constrainable} is
     * supported
     */
    boolean supports(Constrainable constrainable);
}
