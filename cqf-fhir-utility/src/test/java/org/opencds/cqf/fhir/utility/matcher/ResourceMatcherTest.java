package org.opencds.cqf.fhir.utility.matcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.RuntimeSearchParam;
import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.rest.param.CompositeParam;
import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.ParamPrefixEnum;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.param.UriParam;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.apache.commons.lang3.NotImplementedException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.Enumerations.PublicationStatus;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Observation.ObservationStatus;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Period;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Note that most of the tests herein could be in one method with
 * a giant parameters method.
 * But it's been broken up (largely) by parameter type for simpler
 * debugging and fixing
 */
class ResourceMatcherTest {

    private static final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
    private static final SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    // TODO - we should combine the various resource matchers into 1
    // and/or use a factory to produce them (we never mix fhir versions anyways)
    private ResourceMatcherR4 resourceMatcher;

    @BeforeEach
    void before() {
        resourceMatcher = new ResourceMatcherR4();
    }

    // NB: the list of parameters are always OR'd
    // internal compositeparams are always AND'd
    static List<Arguments> coverageParameters() {
        var args = new ArrayList<Arguments>();

        // 1 encounter with date range entirely within search bounds
        {
            var encounter = new Encounter();
            encounter
                    .addLocation()
                    .setPeriod(new Period()
                            .setStart(createDate("2000-01-01 00:00:00"))
                            .setEnd(createDate("2000-12-31 23:59:59")));

            var start = createDate("2000-02-01");
            var end = createDate("2000-11-11");
            var params = new ArrayList<IQueryParameterType>();
            // start <= v <= end || start <= v <= end
            params.add(new CompositeParam<>(
                    new DateParam(ParamPrefixEnum.LESSTHAN_OR_EQUALS, end),
                    new DateParam(ParamPrefixEnum.GREATERTHAN_OR_EQUALS, start)));

            args.add(Arguments.of("location-period", params, encounter, true));
        }
        // 2 encounter with date range overlapping and after
        {
            var encounter = new Encounter();
            encounter
                    .addLocation()
                    .setPeriod(new Period()
                            .setStart(createDate("2000-01-01 00:00:00"))
                            .setEnd(createDate("2000-12-31 23:59:59")));

            var start = createDate("2000-11-11");
            var end = createDate("2001-11-11");
            var params = new ArrayList<IQueryParameterType>();
            params.add(new CompositeParam<>(
                    new DateParam(ParamPrefixEnum.LESSTHAN_OR_EQUALS, end),
                    new DateParam(ParamPrefixEnum.GREATERTHAN_OR_EQUALS, start)));

            args.add(Arguments.of("location-period", params, encounter, true));
        }
        // 3 encounter with date range overlapping and before
        {
            var encounter = new Encounter();
            encounter
                    .addLocation()
                    .setPeriod(new Period()
                            .setStart(createDate("2000-01-01 00:00:00"))
                            .setEnd(createDate("2000-12-31 23:59:59")));

            var start = createDate("1999-11-11");
            var end = createDate("2000-11-11");
            var params = new ArrayList<IQueryParameterType>();
            params.add(new CompositeParam<>(
                    new DateParam(ParamPrefixEnum.LESSTHAN_OR_EQUALS, end),
                    new DateParam(ParamPrefixEnum.GREATERTHAN_OR_EQUALS, start)));

            args.add(Arguments.of("location-period", params, encounter, true));
        }
        // 4 encounter with date range entirely overlapping (and extending on both sides)
        {
            var encounter = new Encounter();
            encounter
                    .addLocation()
                    .setPeriod(new Period()
                            .setStart(createDate("2000-01-01 00:00:00"))
                            .setEnd(createDate("2000-12-31 23:59:59")));

            var start = createDate("1999-11-11");
            var end = createDate("2001-01-01");
            var params = new ArrayList<IQueryParameterType>();
            params.add(new CompositeParam<>(
                    new DateParam(ParamPrefixEnum.LESSTHAN_OR_EQUALS, end),
                    new DateParam(ParamPrefixEnum.GREATERTHAN_OR_EQUALS, start)));

            args.add(Arguments.of("location-period", params, encounter, true));
        }
        // 5 encounter with date range entirely before
        {
            var encounter = new Encounter();
            encounter
                    .addLocation()
                    .setPeriod(new Period()
                            .setStart(createDate("2000-01-01 00:00:00"))
                            .setEnd(createDate("2000-12-31 23:59:59")));

            var start = createDate("1999-01-01");
            var end = createDate("1999-11-11");
            var params = new ArrayList<IQueryParameterType>();
            params.add(new CompositeParam<>(
                    new DateParam(ParamPrefixEnum.LESSTHAN_OR_EQUALS, end),
                    new DateParam(ParamPrefixEnum.GREATERTHAN_OR_EQUALS, start)));

            args.add(Arguments.of("location-period", params, encounter, false));
        }
        // 6 encounter with date range entirely after
        {
            var encounter = new Encounter();
            encounter
                    .addLocation()
                    .setPeriod(new Period()
                            .setStart(createDate("2000-01-01 00:00:00"))
                            .setEnd(createDate("2000-12-31 23:59:59")));

            var start = createDate("2001-01-01");
            var end = createDate("2001-11-11");
            var params = new ArrayList<IQueryParameterType>();
            params.add(new CompositeParam<>(
                    new DateParam(ParamPrefixEnum.GREATERTHAN_OR_EQUALS, start),
                    new DateParam(ParamPrefixEnum.LESSTHAN_OR_EQUALS, end)));

            args.add(Arguments.of("location-period", params, encounter, false));
        }
        // 7 encounter with date range exactly matching
        {
            var start = createDate("2000-01-01 00:00:00");
            var end = createDate("2000-12-31 23:59:59");
            var encounter = new Encounter();
            encounter.addLocation().setPeriod(new Period().setStart(start).setEnd(end));

            var params = new ArrayList<IQueryParameterType>();
            params.add(new CompositeParam<>(
                    new DateParam(ParamPrefixEnum.GREATERTHAN_OR_EQUALS, start),
                    new DateParam(ParamPrefixEnum.LESSTHAN_OR_EQUALS, end)));

            args.add(Arguments.of("location-period", params, encounter, true));
        }
        // 8 encounter with date range starting at endpoint (not in range)
        {
            var start = createDate(dateTimeFormatter, "2000-01-01 00:00:00");
            var end = createDate(dateTimeFormatter, "2000-12-31 23:59:59");
            var encounter = new Encounter();
            encounter
                    .addLocation()
                    .setPeriod(new Period().setStart(end).setEnd(createDate(dateTimeFormatter, "2001-02-02 00:00:00")));

            var params = new ArrayList<IQueryParameterType>();
            params.add(new CompositeParam<>(
                    new DateParam(ParamPrefixEnum.GREATERTHAN_OR_EQUALS, start),
                    new DateParam(ParamPrefixEnum.LESSTHAN, end)));

            args.add(Arguments.of("location-period", params, encounter, false));
        }
        // 9 encounter with date range ending at startpoint (not in range)
        {
            var start = createDate("2000-01-01 00:00:00");
            var end = createDate("2000-12-31 23:59:59");
            var encounter = new Encounter();
            encounter
                    .addLocation()
                    .setPeriod(new Period().setStart(createDate("1999-02-02")).setEnd(start));

            var params = new ArrayList<IQueryParameterType>();
            params.add(new CompositeParam<>(
                    new DateParam(ParamPrefixEnum.GREATERTHAN, start),
                    new DateParam(ParamPrefixEnum.LESSTHAN_OR_EQUALS, end)));

            args.add(Arguments.of("location-period", params, encounter, false));
        }
        // 10 same as case 1 (encounter with date range entirely within search bounds)
        //      but with the composite params flipped for robust-ness
        {
            var encounter = new Encounter();
            encounter
                    .addLocation()
                    .setPeriod(new Period()
                            .setStart(createDate("2000-01-01 00:00:00"))
                            .setEnd(createDate("2000-12-31 23:59:59")));

            var start = createDate("2000-02-01");
            var end = createDate("2000-11-11");
            var params = new ArrayList<IQueryParameterType>();
            // start <= v <= end || start <= v <= end
            params.add(new CompositeParam<>(
                    new DateParam(ParamPrefixEnum.GREATERTHAN_OR_EQUALS, start),
                    new DateParam(ParamPrefixEnum.LESSTHAN_OR_EQUALS, end)));

            args.add(Arguments.of("location-period", params, encounter, true));
        }
        // 11 Observation with valueDateTime after range
        {
            var obs = new Observation();
            obs.setValue(new DateTimeType().setValue(createDate(dateTimeFormatter, "2001-03-14 02:59:00")));
            obs.setStatus(ObservationStatus.CORRECTED);

            var start = createDate("2000-02-01");
            var end = createDate("2000-11-11");
            var params = new ArrayList<IQueryParameterType>();
            params.add(new CompositeParam<>(
                    new DateParam(ParamPrefixEnum.GREATERTHAN_OR_EQUALS, start),
                    new DateParam(ParamPrefixEnum.LESSTHAN_OR_EQUALS, end)));

            args.add(Arguments.of("value-date", params, obs, false));
        }
        // 12 Observation with valueDateTime before range
        {
            var obs = new Observation();
            obs.setValue(new DateTimeType().setValue(createDate(dateTimeFormatter, "1999-03-14 02:59:00")));
            obs.setStatus(ObservationStatus.CORRECTED);

            var start = createDate("2000-02-01");
            var end = createDate("2000-11-11");
            var params = new ArrayList<IQueryParameterType>();
            params.add(new CompositeParam<>(
                    new DateParam(ParamPrefixEnum.GREATERTHAN_OR_EQUALS, start),
                    new DateParam(ParamPrefixEnum.LESSTHAN_OR_EQUALS, end)));

            args.add(Arguments.of("value-date", params, obs, false));
        }
        // 13 Observation with valueDateTime within range
        {
            var obs = new Observation();
            obs.setValue(new DateTimeType().setValue(createDate(dateTimeFormatter, "2000-03-14 02:59:00")));
            obs.setStatus(ObservationStatus.CORRECTED);

            var start = createDate("2000-02-01");
            var end = createDate("2000-11-11");
            var params = new ArrayList<IQueryParameterType>();
            params.add(new CompositeParam<>(
                    new DateParam(ParamPrefixEnum.GREATERTHAN_OR_EQUALS, start),
                    new DateParam(ParamPrefixEnum.LESSTHAN_OR_EQUALS, end)));

            args.add(Arguments.of("value-date", params, obs, true));
        }
        // 14 Observation with valueDateTime exactly on startpoint of range
        {
            var start = createDate("2000-02-01");
            var end = createDate("2000-11-11");
            var obs = new Observation();
            obs.setValue(new DateTimeType().setValue(start));
            obs.setStatus(ObservationStatus.CORRECTED);

            var params = new ArrayList<IQueryParameterType>();
            params.add(new CompositeParam<>(
                    new DateParam(ParamPrefixEnum.GREATERTHAN_OR_EQUALS, start),
                    new DateParam(ParamPrefixEnum.LESSTHAN_OR_EQUALS, end)));

            args.add(Arguments.of("value-date", params, obs, true));
        }
        // 15 Observation with valueDateTime exactly on starting point of range (with more specificity)
        {
            var start = createDate("2000-01-01 00:00:00");
            var end = createDate("2000-12-31 23:59:59");
            var obs = new Observation();
            obs.setValue(new DateTimeType().setValue(end));
            obs.setStatus(ObservationStatus.CORRECTED);

            var params = new ArrayList<IQueryParameterType>();
            params.add(new CompositeParam<>(
                    new DateParam(ParamPrefixEnum.GREATERTHAN_OR_EQUALS, start),
                    new DateParam(ParamPrefixEnum.LESSTHAN_OR_EQUALS, end)));

            args.add(Arguments.of("value-date", params, obs, true));
        }
        // 16 Observation with valueDateTime that doesn't equal an exact match
        {
            var date = createDate(dateTimeFormatter, "2000-03-14 01:59:00");
            var obs = new Observation();
            obs.setValue(new DateTimeType().setValue(date));
            obs.setStatus(ObservationStatus.CORRECTED);

            var params = new ArrayList<IQueryParameterType>();
            params.add(new DateParam(ParamPrefixEnum.NOT_EQUAL, date));

            args.add(Arguments.of("value-date", params, obs, false));
        }
        // 17 Observation with valueDateTime that doesn't match
        {
            var date = createDate(dateTimeFormatter, "2000-03-14 01:59:00");
            var obs = new Observation();
            obs.setValue(new DateTimeType().setValue(date));
            obs.setStatus(ObservationStatus.CORRECTED);

            var params = new ArrayList<IQueryParameterType>();
            params.add(new DateParam(ParamPrefixEnum.NOT_EQUAL, createDate(dateTimeFormatter, "2000-01-01 00:00:01")));

            args.add(Arguments.of("value-date", params, obs, true));
        }

        return args;
    }

    @ParameterizedTest
    @MethodSource("coverageParameters")
    void matches_dateParamCoverage_worksAsExpected(
            String spName, List<IQueryParameterType> params, IBaseResource resource, boolean expectedMatch) {
        // test
        var matches = resourceMatcher.matches(spName, params, resource);

        // verify
        assertEquals(expectedMatch, matches);
    }

    static List<Arguments> stringLikeMatchParameters() {
        var args = new ArrayList<Arguments>();

        // 1 Measure with exactly matching uri
        {
            var measure = new Measure();
            measure.setUrl("http://example.com");

            var params = new ArrayList<IQueryParameterType>();
            params.add(new UriParam().setValue("http://example.com"));

            args.add(Arguments.of("url", params, measure, true));
        }
        // 2 Measure with a non-matching uri
        {
            var measure = new Measure();
            measure.setUrl("http://example.net");

            var params = new ArrayList<IQueryParameterType>();
            params.add(new UriParam().setValue("http://example.com"));

            args.add(Arguments.of("url", params, measure, false));
        }
        // 3 Measure with matching name
        {
            var measure = new Measure();
            measure.setName("measure-1");

            var params = new ArrayList<IQueryParameterType>();
            params.add(new StringParam("measure-1"));

            args.add(Arguments.of("name", params, measure, true));
        }
        // 4 Measure with matching name
        {
            var measure = new Measure();
            measure.setName("measure-1");

            var params = new ArrayList<IQueryParameterType>();
            params.add(new StringParam("measure-2"));

            args.add(Arguments.of("name", params, measure, false));
        }
        // 5 Organization with single matching endpoint reference only
        {
            var organization = new Organization();
            organization.addEndpoint().setReference("Endpoint/123");

            var params = new ArrayList<IQueryParameterType>();
            params.add(new ReferenceParam("Endpoint/123"));

            args.add(Arguments.of("endpoint", params, organization, true));
        }
        // 6 Organization with an attached matching endpoint
        {
            var ep = new Endpoint();
            ep.setId("Endpoint/123");
            var organization = new Organization();
            organization.addEndpoint().setResource(ep);

            var params = new ArrayList<IQueryParameterType>();
            params.add(new ReferenceParam("Endpoint/123"));

            args.add(Arguments.of("endpoint", params, organization, true));
        }
        // 7 Organization with one of many endpoints matching
        {
            var organization = new Organization();
            for (int i = 0; i < 4; i++) {
                organization.addEndpoint().setReference("Endpoint/12" + i);
            }

            var params = new ArrayList<IQueryParameterType>();
            params.add(new ReferenceParam("Endpoint/123"));

            args.add(Arguments.of("endpoint", params, organization, true));
        }
        // 8 Organization without a matching endpoint
        {
            var organization = new Organization();
            organization.addEndpoint().setReference("Endpoint/234");

            var params = new ArrayList<IQueryParameterType>();
            params.add(new ReferenceParam("Endpoint/123"));

            args.add(Arguments.of("endpoint", params, organization, false));
        }
        return args;
    }

    @ParameterizedTest
    @MethodSource("stringLikeMatchParameters")
    void matches_stringParamsCoverage_works(
            String spName, List<IQueryParameterType> params, IBaseResource resource, boolean expectedMatch) {
        // test
        var matches = resourceMatcher.matches(spName, params, resource);

        // verify
        assertEquals(expectedMatch, matches);
    }

    static List<Arguments> tokenMatchingParameters() {
        var args = new ArrayList<Arguments>();

        // 1 Measure with status matching
        {
            var measure = new Measure();
            measure.setStatus(PublicationStatus.ACTIVE);

            var params = new ArrayList<IQueryParameterType>();
            params.add(new TokenParam().setValue("active"));

            args.add(Arguments.of("status", params, measure, true));
        }
        // 2 Measure id matching
        {
            var measure = new Measure();
            measure.setId("Measure/abc");

            var params = new ArrayList<IQueryParameterType>();
            params.add(new TokenParam("abc"));

            args.add(Arguments.of("_id", params, measure, true));
        }
        // 3 Measure matching on version
        {
            var measure = new Measure();
            measure.setVersion("1.2.3");

            var params = new ArrayList<IQueryParameterType>();
            params.add(new TokenParam("1.2.3"));

            args.add(Arguments.of("version", params, measure, true));
        }
        // 4 Measure no match
        {
            var measure = new Measure();
            measure.setStatus(PublicationStatus.ACTIVE);

            var params = new ArrayList<IQueryParameterType>();
            params.add(new TokenParam().setValue("INACTIVE"));

            args.add(Arguments.of("status", params, measure, false));
        }
        // 5 Measure with identifier matching system and value
        {
            var measure = new Measure();
            measure.addIdentifier().setSystem("http://system.com").setValue("value1");

            var params = new ArrayList<IQueryParameterType>();
            params.add(new TokenParam().setSystem("http://system.com").setValue("value1"));

            args.add(Arguments.of("identifier", params, measure, true));
        }
        // 6 Measure with identifier matching on system only
        {
            var measure = new Measure();
            measure.addIdentifier().setSystem("http://system.com");

            var params = new ArrayList<IQueryParameterType>();
            params.add(new TokenParam().setSystem("http://system.com").setValue("value1"));

            args.add(Arguments.of("identifier", params, measure, true));
        }
        // 7 Measure with identifier matching on value only
        {
            var measure = new Measure();
            measure.addIdentifier().setValue("value1");

            var params = new ArrayList<IQueryParameterType>();
            params.add(new TokenParam().setSystem("http://system.com").setValue("value1"));

            args.add(Arguments.of("identifier", params, measure, true));
        }
        // TODO - should this even be a valid test or should it return false?
        // 8 Measure matching on empty
        {
            var measure = new Measure();
            measure.addIdentifier().setSystem("http://system.com").setValue("value1");

            var params = new ArrayList<IQueryParameterType>();
            params.add(new TokenParam());

            args.add(Arguments.of("identifier", params, measure, true));
        }

        return args;
    }

    @ParameterizedTest
    @MethodSource("tokenMatchingParameters")
    void matches_tokenParamsCoverage_worksAsExpected(
            String spName, List<IQueryParameterType> params, IBaseResource resource, boolean expectedMatch) {
        // test
        var matches = resourceMatcher.matches(spName, params, resource);

        // verify
        assertEquals(expectedMatch, matches);
    }

    private static Date createDate(String dateStr) {
        try {
            return formatter.parse(dateStr);
        } catch (ParseException ex) {
            fail(ex);
            return null;
        }
    }

    private static Date createDate(SimpleDateFormat frmtr, String dateStr) {
        try {
            return frmtr.parse(dateStr);
        } catch (ParseException ex) {
            fail(ex);
            return null;
        }
    }

    // -- DSTU3 matcher tests --

    @Test
    void dstu3GetEngineAndContext() {
        var matcher = new ResourceMatcherDSTU3();
        assertNotNull(matcher.getEngine());
        assertEquals(FhirContext.forDstu3Cached(), matcher.getContext());
    }

    @Test
    void dstu3GetDateRangeFromPeriod() {
        var matcher = new ResourceMatcherDSTU3();
        var period = new org.hl7.fhir.dstu3.model.Period();
        period.setStart(new Date());
        period.setEnd(new Date());
        assertNotNull(matcher.getDateRange(period));
    }

    @Test
    void dstu3GetDateRangeFromTimingThrows() {
        var matcher = new ResourceMatcherDSTU3();
        assertThrows(NotImplementedException.class, () -> matcher.getDateRange(new org.hl7.fhir.dstu3.model.Timing()));
    }

    @Test
    void dstu3GetDateRangeFromUnsupportedThrows() {
        var matcher = new ResourceMatcherDSTU3();
        assertThrows(
                UnsupportedOperationException.class,
                () -> matcher.getDateRange(new org.hl7.fhir.dstu3.model.Address()));
    }

    @Test
    void dstu3GetCodesFromCoding() {
        var matcher = new ResourceMatcherDSTU3();
        var coding = new org.hl7.fhir.dstu3.model.Coding("http://sys", "code1", null);
        assertEquals(1, matcher.getCodes(coding).size());
    }

    @Test
    void dstu3GetCodesFromCodeType() {
        var matcher = new ResourceMatcherDSTU3();
        assertEquals(
                1,
                matcher.getCodes(new org.hl7.fhir.dstu3.model.CodeType("abc")).size());
    }

    @Test
    void dstu3GetCodesFromCodeableConcept() {
        var matcher = new ResourceMatcherDSTU3();
        var cc = new org.hl7.fhir.dstu3.model.CodeableConcept();
        cc.addCoding().setSystem("sys").setCode("c1");
        cc.addCoding().setSystem("sys").setCode("c2");
        assertEquals(2, matcher.getCodes(cc).size());
    }

    @Test
    void dstu3CustomParameters() {
        var matcher = new ResourceMatcherDSTU3();
        assertNotNull(matcher.getPathCache());
        assertNotNull(matcher.getCustomParameters());
        var sp = new RuntimeSearchParam(null, null, "test", null, null, null, null, null, null, null);
        matcher.addCustomParameter(sp);
        assertEquals(1, matcher.getCustomParameters().size());
    }

    // -- R5 matcher tests --

    @Test
    void r5GetEngineAndContext() {
        var matcher = new ResourceMatcherR5();
        assertNotNull(matcher.getEngine());
        assertEquals(FhirContext.forR5Cached(), matcher.getContext());
    }

    @Test
    void r5GetDateRangeFromPeriod() {
        var matcher = new ResourceMatcherR5();
        var period = new org.hl7.fhir.r5.model.Period();
        period.setStart(new Date());
        assertNotNull(matcher.getDateRange(period));
    }

    @Test
    void r5GetDateRangeFromTimingThrows() {
        var matcher = new ResourceMatcherR5();
        assertThrows(NotImplementedException.class, () -> matcher.getDateRange(new org.hl7.fhir.r5.model.Timing()));
    }

    @Test
    void r5GetCodesFromCoding() {
        var matcher = new ResourceMatcherR5();
        var r5Coding = new org.hl7.fhir.r5.model.Coding();
        r5Coding.setSystem("http://sys").setCode("c1");
        assertEquals(1, matcher.getCodes(r5Coding).size());
    }

    @Test
    void r5GetCodesFromCodeType() {
        var matcher = new ResourceMatcherR5();
        assertEquals(
                1, matcher.getCodes(new org.hl7.fhir.r5.model.CodeType("xyz")).size());
    }

    @Test
    void r5GetCodesFromCodeableConcept() {
        var matcher = new ResourceMatcherR5();
        var cc = new org.hl7.fhir.r5.model.CodeableConcept();
        cc.addCoding().setSystem("sys").setCode("c1");
        assertEquals(1, matcher.getCodes(cc).size());
    }

    @Test
    void r5CustomParameters() {
        var matcher = new ResourceMatcherR5();
        var sp = new RuntimeSearchParam(null, null, "test", null, null, null, null, null, null, null);
        matcher.addCustomParameter(sp);
        assertEquals(1, matcher.getCustomParameters().size());
    }
}
