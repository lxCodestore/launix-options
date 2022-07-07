package org.ml.options;

import java.util.List;

/**
 * The interface for objects which can be constrained, i. e. {@link Constraint}s
 * can be attached to such objects.
 */
public interface Constrainable {

    /**
     * Add a constraint to this instance.
     * <p>
     *
     * @param constraint The {@link Constraint} to add to the list of
     * constraints for this instance
     */
    void addConstraint(Constraint constraint);

    /**
     * Access all known constraints
     * <p>
     *
     * @return A list of {@link Constraint}s for this instance
     */
    List<Constraint> getConstraints();
}
