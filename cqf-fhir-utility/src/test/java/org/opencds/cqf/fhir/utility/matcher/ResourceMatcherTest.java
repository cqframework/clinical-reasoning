package org.opencds.cqf.fhir.utility.matcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.rest.param.CompositeParam;
import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.ParamPrefixEnum;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Observation.ObservationStatus;
import org.hl7.fhir.r4.model.Period;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class ResourceMatcherTest {

    private static final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
    private static final SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    // TODO - we should combine the various resource matchers into 1
    // and/or use a factory to produce them (we never mix fhir versions anyways)
    private ResourceMatcherR4 resourceMatcher;

    @BeforeEach
    public void before() {
        resourceMatcher = new ResourceMatcherR4();
    }

    // NB: the list of parameters are always OR'd
    // internal compositeparams are always AND'd
    static List<Arguments> coverageParameters() {
        List<Arguments> args = new ArrayList<>();

        // 1 encounter with date range entirely within search bounds
        {
            Encounter encounter = new Encounter();
            encounter
                .addLocation()
                .setPeriod(new Period()
                    .setStart(createDate("2000-01-01 00:00:00"))
                    .setEnd(createDate("2000-12-31 23:59:59")));

            Date start = createDate("2000-02-01");
            Date end = createDate("2000-11-11");
            List<IQueryParameterType> params = new ArrayList<>();
            // start <= v <= end || start <= v <= end
            params.add(new CompositeParam<>(
                new DateParam(ParamPrefixEnum.LESSTHAN_OR_EQUALS, end),
                new DateParam(ParamPrefixEnum.GREATERTHAN_OR_EQUALS, start)));

            args.add(Arguments.of("location-period", params, encounter, true));
        }
        // 2 encounter with date range overlapping and after
        {
            Encounter encounter = new Encounter();
            encounter
                .addLocation()
                .setPeriod(new Period()
                    .setStart(createDate("2000-01-01 00:00:00"))
                    .setEnd(createDate("2000-12-31 23:59:59")));

            Date start = createDate("2000-11-11");
            Date end = createDate("2001-11-11");
            List<IQueryParameterType> params = new ArrayList<>();
            params.add(new CompositeParam<>(
                new DateParam(ParamPrefixEnum.LESSTHAN_OR_EQUALS, end),
                new DateParam(ParamPrefixEnum.GREATERTHAN_OR_EQUALS, start)));

            args.add(Arguments.of("location-period", params, encounter, true));
        }
        // 3 encounter with date range overlapping and before
        {
            Encounter encounter = new Encounter();
            encounter
                .addLocation()
                .setPeriod(new Period()
                    .setStart(createDate("2000-01-01 00:00:00"))
                    .setEnd(createDate("2000-12-31 23:59:59")));

            Date start = createDate("1999-11-11");
            Date end = createDate("2000-11-11");
            List<IQueryParameterType> params = new ArrayList<>();
            params.add(new CompositeParam<>(
                new DateParam(ParamPrefixEnum.LESSTHAN_OR_EQUALS, end),
                new DateParam(ParamPrefixEnum.GREATERTHAN_OR_EQUALS, start)));

            args.add(Arguments.of("location-period", params, encounter, true));
        }
        // 4 encounter with date range entirely overlapping (and extending on both sides)
        {
            Encounter encounter = new Encounter();
            encounter
                .addLocation()
                .setPeriod(new Period()
                    .setStart(createDate("2000-01-01 00:00:00"))
                    .setEnd(createDate("2000-12-31 23:59:59")));

            Date start = createDate("1999-11-11");
            Date end = createDate("2001-01-01");
            List<IQueryParameterType> params = new ArrayList<>();
            params.add(new CompositeParam<>(
                new DateParam(ParamPrefixEnum.LESSTHAN_OR_EQUALS, end),
                new DateParam(ParamPrefixEnum.GREATERTHAN_OR_EQUALS, start)));

            args.add(Arguments.of("location-period", params, encounter, true));
        }
        // 5 encounter with date range entirely before
        {
            Encounter encounter = new Encounter();
            encounter
                .addLocation()
                .setPeriod(new Period()
                    .setStart(createDate("2000-01-01 00:00:00"))
                    .setEnd(createDate("2000-12-31 23:59:59")));

            Date start = createDate("1999-01-01");
            Date end = createDate("1999-11-11");
            List<IQueryParameterType> params = new ArrayList<>();
            params.add(new CompositeParam<>(
                new DateParam(ParamPrefixEnum.LESSTHAN_OR_EQUALS, end),
                new DateParam(ParamPrefixEnum.GREATERTHAN_OR_EQUALS, start)));

            args.add(Arguments.of("location-period", params, encounter, false));
        }
        // 6 encounter with date range entirely after
        {
            Encounter encounter = new Encounter();
            encounter
                .addLocation()
                .setPeriod(new Period()
                    .setStart(createDate("2000-01-01 00:00:00"))
                    .setEnd(createDate("2000-12-31 23:59:59")));

            Date start = createDate("2001-01-01");
            Date end = createDate("2001-11-11");
            List<IQueryParameterType> params = new ArrayList<>();
            params.add(new CompositeParam<>(
                new DateParam(ParamPrefixEnum.GREATERTHAN_OR_EQUALS, start),
                new DateParam(ParamPrefixEnum.LESSTHAN_OR_EQUALS, end)));

            args.add(Arguments.of("location-period", params, encounter, false));
        }
        // 7 encounter with date range exactly matching
        {
            Date start = createDate("2000-01-01 00:00:00");
            Date end = createDate("2000-12-31 23:59:59");
            Encounter encounter = new Encounter();
            encounter
                .addLocation()
                .setPeriod(new Period()
                    .setStart(start)
                    .setEnd(end));

            List<IQueryParameterType> params = new ArrayList<>();
            params.add(new CompositeParam<>(
                new DateParam(ParamPrefixEnum.GREATERTHAN_OR_EQUALS, start),
                new DateParam(ParamPrefixEnum.LESSTHAN_OR_EQUALS, end)));

            args.add(Arguments.of("location-period", params, encounter, true));
        }
        // 8 encounter with date range starting at endpoint (not in range)
        {
            Date start = createDate(dateTimeFormatter, "2000-01-01 00:00:00");
            Date end = createDate(dateTimeFormatter, "2000-12-31 23:59:59");
            Encounter encounter = new Encounter();
            encounter
                .addLocation()
                .setPeriod(new Period()
                    .setStart(end)
                    .setEnd(createDate(dateTimeFormatter, "2001-02-02 00:00:00")));

            List<IQueryParameterType> params = new ArrayList<>();
            params.add(new CompositeParam<>(
                new DateParam(ParamPrefixEnum.GREATERTHAN_OR_EQUALS, start),
                new DateParam(ParamPrefixEnum.LESSTHAN, end)));

            args.add(Arguments.of("location-period", params, encounter, false));
        }
        // 9 encounter with date range ending at startpoint (not in range)
        {
            Date start = createDate("2000-01-01 00:00:00");
            Date end = createDate("2000-12-31 23:59:59");
            Encounter encounter = new Encounter();
            encounter
                .addLocation()
                .setPeriod(new Period()
                    .setStart(createDate("1999-02-02"))
                    .setEnd(start));

            List<IQueryParameterType> params = new ArrayList<>();
            params.add(new CompositeParam<>(
                new DateParam(ParamPrefixEnum.GREATERTHAN, start),
                new DateParam(ParamPrefixEnum.LESSTHAN_OR_EQUALS, end)));

            args.add(Arguments.of("location-period", params, encounter, false));
        }
        // 10 same as case 1 (encounter with date range entirely within search bounds)
        //      but with the composite params flipped for robust-ness
        {
            Encounter encounter = new Encounter();
            encounter
                .addLocation()
                .setPeriod(new Period()
                    .setStart(createDate("2000-01-01 00:00:00"))
                    .setEnd(createDate("2000-12-31 23:59:59")));

            Date start = createDate("2000-02-01");
            Date end = createDate("2000-11-11");
            List<IQueryParameterType> params = new ArrayList<>();
            // start <= v <= end || start <= v <= end
            params.add(new CompositeParam<>(
                new DateParam(ParamPrefixEnum.GREATERTHAN_OR_EQUALS, start),
                new DateParam(ParamPrefixEnum.LESSTHAN_OR_EQUALS, end)));

            args.add(Arguments.of("location-period", params, encounter, true));
        }
        // 11 Observation with valueDateTime after range
        {
            Observation obs = new Observation();
            obs.setValue(
                new DateTimeType().setValue(createDate(dateTimeFormatter, "2001-03-14 02:59:00")));
            obs.setStatus(ObservationStatus.CORRECTED);

            Date start = createDate("2000-02-01");
            Date end = createDate("2000-11-11");
            List<IQueryParameterType> params = new ArrayList<>();
            params.add(new CompositeParam<>(
                new DateParam(ParamPrefixEnum.GREATERTHAN_OR_EQUALS, start),
                new DateParam(ParamPrefixEnum.LESSTHAN_OR_EQUALS, end)
            ));

            args.add(Arguments.of("value-date", params, obs, false));
        }
        // 12 Observation with valueDateTime before range
        {
            Observation obs = new Observation();
            obs.setValue(
                new DateTimeType().setValue(createDate(dateTimeFormatter, "1999-03-14 02:59:00")));
            obs.setStatus(ObservationStatus.CORRECTED);

            Date start = createDate("2000-02-01");
            Date end = createDate("2000-11-11");
            List<IQueryParameterType> params = new ArrayList<>();
            params.add(new CompositeParam<>(
                new DateParam(ParamPrefixEnum.GREATERTHAN_OR_EQUALS, start),
                new DateParam(ParamPrefixEnum.LESSTHAN_OR_EQUALS, end)
            ));

            args.add(Arguments.of("value-date", params, obs, false));
        }
        // 13 Observation with valueDateTime within range
        {
            Observation obs = new Observation();
            obs.setValue(
                new DateTimeType().setValue(createDate(dateTimeFormatter, "2000-03-14 02:59:00")));
            obs.setStatus(ObservationStatus.CORRECTED);

            Date start = createDate("2000-02-01");
            Date end = createDate("2000-11-11");
            List<IQueryParameterType> params = new ArrayList<>();
            params.add(new CompositeParam<>(
                new DateParam(ParamPrefixEnum.GREATERTHAN_OR_EQUALS, start),
                new DateParam(ParamPrefixEnum.LESSTHAN_OR_EQUALS, end)
            ));

            args.add(Arguments.of("value-date", params, obs, true));
        }
        // 14 Observation with valueDateTime exactly on startpoint of range
        {
            Date start = createDate("2000-02-01");
            Date end = createDate("2000-11-11");
            Observation obs = new Observation();
            obs.setValue(
                new DateTimeType().setValue(start));
            obs.setStatus(ObservationStatus.CORRECTED);


            List<IQueryParameterType> params = new ArrayList<>();
            params.add(new CompositeParam<>(
                new DateParam(ParamPrefixEnum.GREATERTHAN_OR_EQUALS, start),
                new DateParam(ParamPrefixEnum.LESSTHAN_OR_EQUALS, end)
            ));

            args.add(Arguments.of("value-date", params, obs, true));
        }
        // 15 Obseravion with valueDateTime exactly on starting point of range (with more specificity)
        {
            Date start = createDate("2000-01-01 00:00:00");
            Date end = createDate("2000-12-31 23:59:59");
            Observation obs = new Observation();
            obs.setValue(
                new DateTimeType().setValue(end));
            obs.setStatus(ObservationStatus.CORRECTED);


            List<IQueryParameterType> params = new ArrayList<>();
            params.add(new CompositeParam<>(
                new DateParam(ParamPrefixEnum.GREATERTHAN_OR_EQUALS, start),
                new DateParam(ParamPrefixEnum.LESSTHAN_OR_EQUALS, end)
            ));

            args.add(Arguments.of("value-date", params, obs, true));
        }

        return args;
    }

    @ParameterizedTest
    @MethodSource("coverageParameters")
    public void matches_dateParamCoverage_worksAsExpected(
            String spName, List<IQueryParameterType> params, IBaseResource resource, boolean expectedMatch) {
        // test
        boolean matches = resourceMatcher.matches(spName, params, resource);

        // verify
        assertEquals(expectedMatch, matches);
    }

//    @Test
//    public void matches_unsupportedCoverage_fails() {
//        assertThrows(UnsupportedOperationException.class, () -> {
//           resourceMatcher.matches()
//        });
//    }

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
}
