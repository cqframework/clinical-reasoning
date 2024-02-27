package org.opencds.cqf.fhir.benchmark;

import ca.uhn.fhir.context.FhirContext;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import org.opencds.cqf.cql.engine.runtime.Code;
import org.opencds.cqf.cql.engine.terminology.ValueSetInfo;
import org.opencds.cqf.fhir.cql.engine.terminology.RepositoryTerminologyProvider;
import org.opencds.cqf.fhir.cr.measure.r4.Measure;
import org.opencds.cqf.fhir.utility.repository.ig.IgRepository;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

@State(Scope.Benchmark)
public class TerminologyProviders {
    private RepositoryTerminologyProvider terminologyProvider;

    private static final Code smallCode =
            new Code().withCode("F").withSystem("http://terminology.hl7.org/CodeSystem/v3-AdministrativeGender");

    private static final ValueSetInfo smallValueSet = new ValueSetInfo()
            .withId("http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1")
            .withVersion("20150331");

    private static final Code largeCode =
            new Code().withCode("199246").withSystem("http://www.nlm.nih.gov/research/umls/rxnorm");

    private static final ValueSetInfo largeValueSet = new ValueSetInfo()
            .withId("http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1190.58")
            .withVersion("20220304");

    @Setup(Level.Trial)
    public void setupTrial() throws Exception {
        var repository =
                new IgRepository(FhirContext.forR4Cached(), Paths.get(Measure.CLASS_PATH + "/CaseRepresentation101"));
        this.terminologyProvider = new RepositoryTerminologyProvider(repository);
    }

    @Benchmark
    @Fork(warmups = 1, value = 1)
    @Measurement(iterations = 10, timeUnit = TimeUnit.MILLISECONDS)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public void testSmall(Blackhole bh) throws Exception {
        bh.consume(this.terminologyProvider.in(smallCode, smallValueSet));
    }

    @Benchmark
    @Fork(warmups = 1, value = 1)
    @Measurement(iterations = 10, timeUnit = TimeUnit.MILLISECONDS)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public void testLarge(Blackhole bh) throws Exception {
        // NOTE: There's an issue with this valueSet in that it does not contain many duplicate codes.
        // A high cardinality of codes is likely to change performance
        // So that's a gap we should close at some point with our sample set.
        bh.consume(this.terminologyProvider.in(largeCode, largeValueSet));
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(TerminologyProviders.class.getSimpleName())
                .build();
        new Runner(opt).run();
    }
}
