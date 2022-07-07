package org.ml.options;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.net.URL;
import javax.xml.transform.sax.SAXSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Validator for XML documents using XML schema. This is based on JDK 5.0 and
 * requires no outside library.
 */
public class SchemaValidator extends org.xml.sax.helpers.DefaultHandler {

    private final String CLASS = "SchemaValidator";
    private final String xsdFile = "config/options.xsd";
    private String error = null;

    /**
     * The actual validation method. If validation is not successful, the errors
     * found can be retrieved using the {@link #getError()} method.
     * <p>
     *
     * @param xmlReader The reader for the XML file to validate
     * <p>
     * @return <code>true</code> if the XML file could be validated against the
     * XML schema, else <code>false</code>
     * @throws IOException
     * @throws SAXException
     */
    public boolean validate(Reader xmlReader) throws IOException, SAXException {

        if (xmlReader == null) {
            throw new IllegalArgumentException(CLASS + ": xmlReader may not be null");
        }

        //.... Get the XML schema from the JAR and create a validator

        SchemaFactory factory = SchemaFactory.newInstance(javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI);
        ClassLoader loader = this.getClass().getClassLoader();
        URL url = loader.getResource(xsdFile);
        Schema schema = factory.newSchema(url);
        Validator validator = schema.newValidator();

        validator.setErrorHandler(this);

        //.... Try to validate the XML file given

        InputSource source = new InputSource(new BufferedReader(xmlReader));
        validator.validate(new SAXSource(source));

        return getError() == null;

    }

    //-----------------------------------------------------------------------------------------
    // Below are helper methods that are required to improve the error handling (specifically,
    // to make sure all thrown exceptions are reported, and row and column numbers are added
    // to the output for better debugging
    //-----------------------------------------------------------------------------------------
    /**
     * Retrieve the error message set by the
     * <code>org.xml.sax.ErrorHandler</code> methods. If no error has been
     * found,
     * <code>null</code> is returned.
     * <p>
     *
     * @return A string describing the error encountered
     */
    public String getError() {
        return error;
    }

    /**
     * A method required by the
     * <code>org.xml.sax.ErrorHandler</code> interface
     * <p>
     *
     * @param ex A parsing exception
     */
    @Override
    public void warning(SAXParseException ex)  {
        getError("Warning", ex);
    }

    /**
     * A method required by the
     * <code>org.xml.sax.ErrorHandler</code> interface
     * <p>
     *
     * @param ex A parsing exception
     */
    @Override
    public void error(SAXParseException ex)  {
        getError("Error", ex);
    }

    /**
     * A method required by the
     * <code>org.xml.sax.ErrorHandler</code> interface
     * <p>
     *
     * @param ex A parsing exception
     */
    @Override
    public void fatalError(SAXParseException ex)  {
        getError("Fatal Error", ex);
    }

    /**
     * A helper method for the formatting
     */
    private void getError(String type, SAXParseException ex) {

        StringBuilder out = new StringBuilder(200);

        out.append(type);

        if (ex == null) {
            out.append("!!!");
        } else {
            String systemId = ex.getSystemId();
            if (systemId != null) {
                int index = systemId.lastIndexOf('/');
                if (index != -1) {
                    systemId = systemId.substring(index + 1);
                }
                out.append(systemId);
            }
            out.append(": Row ");
            out.append(ex.getLineNumber());
            out.append(" /`Col ");
            out.append(ex.getColumnNumber());
            out.append(": ");
            out.append(ex.getMessage());
        }

        //.... There may be multiple exceptions thrown, we don't want to miss any information

        if (error == null) {
            error = out.toString();
        } else {
            error += "\n" + out.toString();
        }

    }
}
