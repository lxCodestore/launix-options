package org.ml.options;

import java.util.List;
import org.jdom2.Element;

/**
 * Constraints implementing this interface have - beyond the {@link Constraint}
 * interface - the capability to be created through XML configuration files. In
 * this case, a public no-arg constructor is also required.
 */
public interface XMLConstraint extends Constraint {

    /**
     * This method is used to initialize a constraint based on data read from an
     * XML configuration file. The method is invoked internally during setup
     * with the instance of {@link Constrainable} to which the constraint
     * applies and a list of JDOM elements, which contain the details about the
     * constraint itself.
     * <p>
     * This method initializes the constraint and attaches it to the list of
     * constraints of the {@link Constrainable} instance.
     * <p>
     *
     * @param constrainable The {@link Constrainable} instance to which this
     * constraint applies
     * @param list A list of JDOM elements to be used to initialize the
     * constraint. Specifically, these are tags of the form
     * <p>
     * <code>&lt;param name="..." value="..." /&gt;</code>
     * <p>
     * containing key/value pairs with information. The expected pairs are
     * specific to each implementation.
     */
    void init(Constrainable constrainable, List<Element> list);
}
