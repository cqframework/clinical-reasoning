package org.opencds.cqf.cql.evaluator.engine.util;

import static java.util.Objects.requireNonNull;

import java.util.EnumSet;
import java.util.List;

import org.cqframework.cql.cql2elm.CqlTranslatorOptions;
import org.cqframework.cql.elm.execution.CqlToElmBase;
import org.cqframework.cql.elm.execution.CqlToElmInfo;
import org.cqframework.cql.elm.execution.Library;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides functions for extracting and parsing CQL Translator Options from a Library
 */
public class TranslatorOptionsUtil {

    private TranslatorOptionsUtil() {}

    protected static final Logger logger = LoggerFactory.getLogger(TranslatorOptionsUtil.class);

    /**
     * Gets the translator options used to generate an elm Library.
     *
     * Returns null if the translator options could not be determined. (for example, the Library was
     * translated without annotations)
     *
     * @param library The library to extracts the options from.
     * @return The set of options used to translate the library.
     */
    public static EnumSet<CqlTranslatorOptions.Options> getTranslatorOptions(Library library) {
        requireNonNull(library, "library can not be null");
        if (library.getAnnotation() == null || library.getAnnotation().isEmpty()) {
            return null;
        }

        String translatorOptions = getTranslatorOptions(library.getAnnotation());
        return parseTranslatorOptions(translatorOptions);
    }

    private static String getTranslatorOptions(List<CqlToElmBase> annotations) {
        for (CqlToElmBase o : annotations) {
            // Library mapped through the Library mapper
            // The Library mapper currently uses the translator types instead of the engine
            // types because that's all there are.
            if (o instanceof CqlToElmInfo) {
                CqlToElmInfo c = (CqlToElmInfo) o;
                return c.getTranslatorOptions();
            }
        }

        return null;
    }

    /**
     * Parses a string representing CQL Translator Options into an EnumSet. The string is expected
     * to be a comma delimited list of values from the CqlTranslator.Options enumeration. For
     * example "EnableListPromotion, EnableListDemotion".
     *
     * @param translatorOptions the string to parse
     * @return the set of options
     */
    public static EnumSet<CqlTranslatorOptions.Options> parseTranslatorOptions(
            String translatorOptions) {
        if (translatorOptions == null) {
            return null;
        }

        EnumSet<CqlTranslatorOptions.Options> optionSet =
                EnumSet.noneOf(CqlTranslatorOptions.Options.class);

        if (translatorOptions.trim().isEmpty()) {
            return optionSet;
        }

        String[] options = translatorOptions.trim().split(",");

        for (String option : options) {
            optionSet.add(CqlTranslatorOptions.Options.valueOf(option));
        }

        return optionSet;
    }
}
