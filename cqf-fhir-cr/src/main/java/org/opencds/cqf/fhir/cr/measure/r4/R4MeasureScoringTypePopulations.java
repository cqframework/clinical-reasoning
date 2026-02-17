package org.opencds.cqf.fhir.cr.measure.r4;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType;
import org.opencds.cqf.fhir.cr.measure.common.MeasureScoring;

public class R4MeasureScoringTypePopulations {

    private R4MeasureScoringTypePopulations() {}

    public enum PROPORTION_ALLOWED {
        INITIALPOPULATION(MeasurePopulationType.INITIALPOPULATION),
        DENOMINATOR(MeasurePopulationType.DENOMINATOR),
        DENOMINATOREXCLUSION(MeasurePopulationType.DENOMINATOREXCLUSION),
        DENOMINATOREXCEPTION(MeasurePopulationType.DENOMINATOREXCEPTION),
        NUMERATOREXCLUSION(MeasurePopulationType.NUMERATOREXCLUSION),
        NUMERATOR(MeasurePopulationType.NUMERATOR),
        DATEOFCOMPLIANCE(MeasurePopulationType.DATEOFCOMPLIANCE);

        private final MeasurePopulationType measurePopulationType;

        PROPORTION_ALLOWED(MeasurePopulationType measurePopulationType) {
            this.measurePopulationType = measurePopulationType;
        }

        public MeasurePopulationType getPopulationType() {
            return measurePopulationType;
        }

        public static Set<MeasurePopulationType> getPopulations() {
            EnumSet<R4MeasureScoringTypePopulations.PROPORTION_ALLOWED> data = EnumSet.allOf(PROPORTION_ALLOWED.class);
            return data.stream().map(PROPORTION_ALLOWED::getPopulationType).collect(Collectors.toSet());
        }

        public static void validateMember(List<MeasurePopulationType> populations) {
            var populationSet = getPopulations();
            for (MeasurePopulationType popType : populations) {
                if (!populationSet.contains(popType)) {
                    throw new UnsupportedOperationException(
                            "MeasurePopulationType: %s, is not a member of allowed 'proportion' populations."
                                    .formatted(popType.toCode()));
                }
            }
        }
    }

    public enum PROPORTION_REQUIRED {
        INITIALPOPULATION(MeasurePopulationType.INITIALPOPULATION),
        DENOMINATOR(MeasurePopulationType.DENOMINATOR),
        NUMERATOR(MeasurePopulationType.NUMERATOR);
        private final MeasurePopulationType measurePopulationType;

        PROPORTION_REQUIRED(MeasurePopulationType measurePopulationType) {
            this.measurePopulationType = measurePopulationType;
        }

        public MeasurePopulationType getPopulationType() {
            return measurePopulationType;
        }

        public static Set<MeasurePopulationType> getPopulations() {
            EnumSet<R4MeasureScoringTypePopulations.PROPORTION_REQUIRED> data =
                    EnumSet.allOf(PROPORTION_REQUIRED.class);
            return data.stream().map(PROPORTION_REQUIRED::getPopulationType).collect(Collectors.toSet());
        }

        public static void validateRequired(List<MeasurePopulationType> populations) {
            for (MeasurePopulationType requiredPop : getPopulations()) {
                if (!populations.contains(requiredPop)) {
                    throw new UnsupportedOperationException(
                            "'proportion' measure is missing required population: %s.".formatted(requiredPop.toCode()));
                }
            }
        }
    }

    public enum RATIO_ALLOWED {
        INITIALPOPULATION(MeasurePopulationType.INITIALPOPULATION),
        DENOMINATOR(MeasurePopulationType.DENOMINATOR),
        DENOMINATOREXCLUSION(MeasurePopulationType.DENOMINATOREXCLUSION),
        NUMERATOREXCLUSION(MeasurePopulationType.NUMERATOREXCLUSION),
        NUMERATOR(MeasurePopulationType.NUMERATOR),
        MEASUREOBSERVATION(MeasurePopulationType.MEASUREOBSERVATION),
        DATEOFCOMPLIANCE(MeasurePopulationType.DATEOFCOMPLIANCE);

        private final MeasurePopulationType measurePopulationType;

        RATIO_ALLOWED(MeasurePopulationType measurePopulationType) {
            this.measurePopulationType = measurePopulationType;
        }

        public MeasurePopulationType getPopulationType() {
            return measurePopulationType;
        }

        public static Set<MeasurePopulationType> getPopulations() {
            EnumSet<R4MeasureScoringTypePopulations.RATIO_ALLOWED> data = EnumSet.allOf(RATIO_ALLOWED.class);
            return data.stream().map(RATIO_ALLOWED::getPopulationType).collect(Collectors.toSet());
        }

        public static void validateMember(List<MeasurePopulationType> populations) {
            var populationSet = getPopulations();
            for (MeasurePopulationType popType : populations) {
                if (!populationSet.contains(popType)) {
                    throw new UnsupportedOperationException(
                            "MeasurePopulationType: %s, is not a member of allowed 'ratio' populations."
                                    .formatted(popType.toCode()));
                }
            }
        }
    }

    public enum RATIO_REQUIRED {
        INITIALPOPULATION(MeasurePopulationType.INITIALPOPULATION),
        DENOMINATOR(MeasurePopulationType.DENOMINATOR),
        NUMERATOR(MeasurePopulationType.NUMERATOR);
        private final MeasurePopulationType measurePopulationType;

        RATIO_REQUIRED(MeasurePopulationType measurePopulationType) {
            this.measurePopulationType = measurePopulationType;
        }

        public MeasurePopulationType getPopulationType() {
            return measurePopulationType;
        }

        public static Set<MeasurePopulationType> getPopulations() {
            EnumSet<R4MeasureScoringTypePopulations.RATIO_REQUIRED> data = EnumSet.allOf(RATIO_REQUIRED.class);
            return data.stream().map(RATIO_REQUIRED::getPopulationType).collect(Collectors.toSet());
        }

        public static void validateRequired(List<MeasurePopulationType> populations) {
            for (MeasurePopulationType requiredPop : getPopulations()) {
                if (!populations.contains(requiredPop)) {
                    throw new UnsupportedOperationException(
                            "'ratio' measure is missing required population: %s.".formatted(requiredPop.toCode()));
                }
            }
        }
    }

    public enum CONTINUOUS_VARIABLE_ALLOWED {
        INITIALPOPULATION(MeasurePopulationType.INITIALPOPULATION),
        MEASUREPOPULATION(MeasurePopulationType.MEASUREPOPULATION),
        MEASUREPOPULATIONEXCLUSION(MeasurePopulationType.MEASUREPOPULATIONEXCLUSION),
        MEASUREOBSERVATION(MeasurePopulationType.MEASUREOBSERVATION);
        private final MeasurePopulationType measurePopulationType;

        CONTINUOUS_VARIABLE_ALLOWED(MeasurePopulationType measurePopulationType) {
            this.measurePopulationType = measurePopulationType;
        }

        public MeasurePopulationType getPopulationType() {
            return measurePopulationType;
        }

        public static Set<MeasurePopulationType> getPopulations() {
            EnumSet<R4MeasureScoringTypePopulations.CONTINUOUS_VARIABLE_ALLOWED> data =
                    EnumSet.allOf(CONTINUOUS_VARIABLE_ALLOWED.class);
            return data.stream()
                    .map(CONTINUOUS_VARIABLE_ALLOWED::getPopulationType)
                    .collect(Collectors.toSet());
        }

        public static void validateMember(List<MeasurePopulationType> populations) {
            var populationSet = getPopulations();
            for (MeasurePopulationType popType : populations) {
                if (!populationSet.contains(popType)) {
                    throw new UnsupportedOperationException(
                            "MeasurePopulationType: %s, is not a member of allowed 'continuous-variable' populations."
                                    .formatted(popType.toCode()));
                }
            }
        }
    }
    // TODO: should require Measure Observation per spec
    public enum CONTINUOUS_VARIABLE_REQUIRED {
        INITIALPOPULATION(MeasurePopulationType.INITIALPOPULATION),
        MEASUREPOPULATION(MeasurePopulationType.MEASUREPOPULATION);
        private final MeasurePopulationType measurePopulationType;

        CONTINUOUS_VARIABLE_REQUIRED(MeasurePopulationType measurePopulationType) {
            this.measurePopulationType = measurePopulationType;
        }

        public MeasurePopulationType getPopulationType() {
            return measurePopulationType;
        }

        public static Set<MeasurePopulationType> getPopulations() {
            EnumSet<R4MeasureScoringTypePopulations.CONTINUOUS_VARIABLE_REQUIRED> data =
                    EnumSet.allOf(CONTINUOUS_VARIABLE_REQUIRED.class);
            return data.stream()
                    .map(CONTINUOUS_VARIABLE_REQUIRED::getPopulationType)
                    .collect(Collectors.toSet());
        }

        public static void validateRequired(List<MeasurePopulationType> populations) {
            for (MeasurePopulationType requiredPop : getPopulations()) {
                if (!populations.contains(requiredPop)) {
                    throw new UnsupportedOperationException(
                            "'continuous-variable' measure is missing required population: %s."
                                    .formatted(requiredPop.toCode()));
                }
            }
        }
    }

    public enum COHORT_ALLOWED {
        INITIALPOPULATION(MeasurePopulationType.INITIALPOPULATION);
        private final MeasurePopulationType measurePopulationType;

        COHORT_ALLOWED(MeasurePopulationType measurePopulationType) {
            this.measurePopulationType = measurePopulationType;
        }

        public MeasurePopulationType getPopulationType() {
            return measurePopulationType;
        }

        public static Set<MeasurePopulationType> getPopulations() {
            EnumSet<R4MeasureScoringTypePopulations.COHORT_ALLOWED> data = EnumSet.allOf(COHORT_ALLOWED.class);
            return data.stream().map(COHORT_ALLOWED::getPopulationType).collect(Collectors.toSet());
        }

        public static void validateMember(List<MeasurePopulationType> populations) {
            var populationSet = getPopulations();
            for (MeasurePopulationType popType : populations) {
                if (!populationSet.contains(popType)) {
                    throw new UnsupportedOperationException(
                            "MeasurePopulationType: %s, is not a member of allowed 'cohort' populations."
                                    .formatted(popType.toCode()));
                }
            }
        }
    }

    public enum COHORT_REQUIRED {
        INITIALPOPULATION(MeasurePopulationType.INITIALPOPULATION);
        private final MeasurePopulationType measurePopulationType;

        COHORT_REQUIRED(MeasurePopulationType measurePopulationType) {
            this.measurePopulationType = measurePopulationType;
        }

        public MeasurePopulationType getPopulationType() {
            return measurePopulationType;
        }

        public static Set<MeasurePopulationType> getPopulations() {
            EnumSet<R4MeasureScoringTypePopulations.COHORT_REQUIRED> data = EnumSet.allOf(COHORT_REQUIRED.class);
            return data.stream().map(COHORT_REQUIRED::getPopulationType).collect(Collectors.toSet());
        }

        public static void validateRequired(List<MeasurePopulationType> populations) {
            for (MeasurePopulationType requiredPop : getPopulations()) {
                if (!populations.contains(requiredPop)) {
                    throw new UnsupportedOperationException(
                            "'cohort' measure is missing required population: %s.".formatted(requiredPop.toCode()));
                }
            }
        }
    }

    /**
     *
     * @param populations
     * @param measureScoring
     */
    public static void validateScoringTypePopulations(
            List<MeasurePopulationType> populations, MeasureScoring measureScoring) {
        switch (measureScoring) {
            case RATIO:
                RATIO_ALLOWED.validateMember(populations);
                RATIO_REQUIRED.validateRequired(populations);
                break;
            case PROPORTION:
                PROPORTION_ALLOWED.validateMember(populations);
                PROPORTION_REQUIRED.validateRequired(populations);
                break;
            case COHORT:
                COHORT_ALLOWED.validateMember(populations);
                COHORT_REQUIRED.validateRequired(populations);
                break;
            case CONTINUOUSVARIABLE:
                CONTINUOUS_VARIABLE_ALLOWED.validateMember(populations);
                CONTINUOUS_VARIABLE_REQUIRED.validateRequired(populations);
                break;
            default:
                throw new UnsupportedOperationException(
                        "Measure scoring type: %s, is not an accepted value".formatted(measureScoring.toCode()));
        }
    }
}
