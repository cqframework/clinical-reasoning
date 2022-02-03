package org.opencds.cqf.cql.evaluator.engine.util;

import static java.util.Objects.requireNonNull;

import java.lang.reflect.Method;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;

import org.cqframework.cql.cql2elm.CqlTranslator;
import org.cqframework.cql.elm.execution.Library;
import org.hl7.cql_annotations.r1.CqlToElmInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

/**
 * This class provides functions for extracting and parsing CQL Translator Options from
 * a Library
 */
public class TranslatorOptionsUtil {

    private TranslatorOptionsUtil() {}

    protected static final Logger logger = LoggerFactory.getLogger(TranslatorOptionsUtil.class);

    /**
     * Gets the translator options used to generate an elm Library.
     * 
     * Returns null if the translator options could not be determined.
     * (for example, the Library was translated without annotations)
     * @param library The library to extracts the options from.
     * @return The set of options used to translate the library.
     */
    public static EnumSet<CqlTranslator.Options> getTranslatorOptions(Library library) {
        requireNonNull(library, "library can not be null");
        if (library.getAnnotation() == null || library.getAnnotation().isEmpty()) {
            return null;
        }

        String translatorOptions = getTranslatorOptions(library.getAnnotation());
        return parseTranslatorOptions(translatorOptions);
    }

    // TODO: This has some hackery to work around type serialization that's being tracked here:
    // https://github.com/DBCG/cql_engine/issues/436
    // Once the deserializers are fixed this should only need engine annotation types. 
    private static String getTranslatorOptions(List<Object> annotations){
        for (Object o : annotations) {
            // Library mapped through the  Library mapper
            // The Library mapper currently uses the translator types instead of the engine
            // types because that's all there are.
            if (o instanceof CqlToElmInfo) {
                CqlToElmInfo c = (CqlToElmInfo)o;
                return c.getTranslatorOptions();
            }

            // Library loaded from JSON
            if (o instanceof LinkedHashMap<?,?>) {
                try {
                    @SuppressWarnings("unchecked")
                    LinkedHashMap<String, String> lhm = (LinkedHashMap<String, String>)o;
                    String options = lhm.get("translatorOptions");
                    if (options != null) {
                        return options;
                    }
                }
                catch(Exception e) {
                    continue;
                }

            }

            // Library read from XML
            // ElementNsImpl is a private class internal to the JVM
            // that we aren't allowed to use, hence all the reflection
            if (o.getClass().getSimpleName().equals("ElementNSImpl")) {
                try {
                    Class<?> elementNsClass = o.getClass();
                    Method method = elementNsClass.getMethod("getAttributes");
                    org.w3c.dom.NamedNodeMap nodeMap =  (org.w3c.dom.NamedNodeMap)method.invoke(o);
                    if (nodeMap == null) {
                        continue;
                    }

                    Node attributeNode = nodeMap.getNamedItem("translatorOptions");
                    if (attributeNode == null) {
                        continue;
                    }

                    return  attributeNode.getNodeValue();
                }
                catch(Exception e) {
                    continue;
                }
            }
        }

        return null;
    }

    /**
     * Parses a string representing CQL Translator Options into an EnumSet. The string is expected
     * to be a comma delimited list of values from the CqlTranslator.Options enumeration.
     * For example "EnableListPromotion, EnableListDemotion".
     * @param translatorOptions the string to parse
     * @return the set of options
     */
    public static EnumSet<CqlTranslator.Options> parseTranslatorOptions(String translatorOptions) {
        if (translatorOptions == null) {
            return null;
        }

        EnumSet<CqlTranslator.Options> optionSet = EnumSet.noneOf(CqlTranslator.Options.class);
        String[] options = translatorOptions.trim().split(",");

        for (String option : options) {
            optionSet.add(CqlTranslator.Options.valueOf(option));
        }

        return optionSet;
    }
}
