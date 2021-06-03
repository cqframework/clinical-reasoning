package org.opencds.cqf.cql.evaluator.fhir.builders.dstu3;

import java.util.UUID;

public class RandomIdBuilder
{
    public static String build(String format)
    {
        if (format == null)
        {
            return UUID.randomUUID().toString();
        }
        switch (format.toLowerCase())
        {
            case "uuid": return UUID.randomUUID().toString();
            default: return UUID.randomUUID().toString();
        }
    }
}
