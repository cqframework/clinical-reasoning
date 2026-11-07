package org.opencds.cqf.fhir.cr.plandefinition.apply;

import static org.opencds.cqf.fhir.utility.SearchHelper.searchRepositoryByCanonical;

import ca.uhn.fhir.repository.IRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.function.Function;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemComponent;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemType;
import org.opencds.cqf.fhir.cr.questionnaire.generate.GenerateProcessor;
import org.opencds.cqf.fhir.utility.adapter.IDataRequirementAdapter;

/** CRL presentation protocol 1: collect independent Boolean profiles before population. */
public final class PresentationChoiceGroupProcessor {
    public static final int PROTOCOL = 1;
    public static final String URL = "http://clinicalreasoninglanguage.org/fhir/StructureDefinition/presentation-choice-group";
    private static final String ITEM_CONTROL = "http://hl7.org/fhir/StructureDefinition/questionnaire-itemControl";
    private PresentationChoiceGroupProcessor() {}

    public static void process(ApplyRequest request, IDataRequirementAdapter input, IRepository repository, GenerateProcessor generator) {
        if (!input.hasProfile() || input.getExtensionsByUrl(URL).size() != 1) fail("Choice group input requires profiles and exactly one descriptor");
        if (!(request.getQuestionnaire() instanceof Questionnaire questionnaire)) throw new IllegalArgumentException("CRL choice groups require FHIR R4 Questionnaire");
        Extension descriptor = input.getExtensionByUrl(URL);
        var profiles = descriptor.getExtension().stream().filter(e -> e.getUrl().equals("member")).map(e -> field(e, "profile", true)).toList();
        if (input.getProfile().stream().anyMatch(p -> !profiles.contains(p.getValueAsString()))) fail("Choice group descriptor does not contain its input profile");
        collect(questionnaire, descriptor, profile -> {
            var generateRequest = request.toGenerateRequest(searchRepositoryByCanonical(repository, new org.hl7.fhir.r4.model.CanonicalType(profile)));
            generateRequest.setNextProfileAdapter();
            var generated = generator.generateItem(generateRequest);
            if (generated == null || !(generated.getLeft().get() instanceof QuestionnaireItemComponent item)) throw new IllegalArgumentException("CRL choice group profile generated no R4 question: " + profile);
            request.addLaunchContextExtensions(generated.getRight());
            return item;
        });
    }

    // Questionnaire is the shared registry, including across delegated ApplyRequest copies.
    static void collect(Questionnaire q, Extension descriptor, Function<String, QuestionnaireItemComponent> generate) {
        if (!field(descriptor, "protocol", true).equals("1")) fail("Unsupported presentation choice group protocol");
        var id = field(descriptor, "id", true);
        field(descriptor, "name", true);
        var text = field(descriptor, "text", true);
        try { if (Integer.parseInt(field(descriptor, "columns", true)) < 1) fail("Choice group columns must be positive"); }
        catch (NumberFormatException e) { fail("Choice group columns must be a positive integer"); }
        var control = field(descriptor, "control", true);
        if (!control.equals("radio") && !control.equals("checkbox")) fail("Unknown choice group control");
        var members = descriptor.getExtension().stream().filter(e -> e.getUrl().equals("member")).toList();
        if (members.isEmpty()) fail("Empty choice group");
        var memberProfiles = new HashSet<String>();
        for (var member : members) {
            if (!memberProfiles.add(field(member, "profile", true))) fail("Duplicate choice group member profile");
            field(member, "text", true);
        }
        var wrapperId = linkId(id);
        QuestionnaireItemComponent wrapper = null;
        for (var item : flatten(q.getItem())) {
            var marker = item.getExtensionByUrl(URL);
            if (marker != null) {
                if (field(marker, "id", true).equals(id)) {
                    if (wrapper != null || !marker.equalsDeep(descriptor) || !q.getItem().contains(item) || item.getType() != QuestionnaireItemType.GROUP || item.getRepeats() || item.hasDefinition() || item.hasEnableWhen() || item.getReadOnly() || !item.getLinkId().equals(wrapperId)) fail("Incompatible existing choice group");
                    wrapper = item;
                } else if (marker.getExtension().stream().filter(e -> e.getUrl().equals("member")).anyMatch(e -> memberProfiles.contains(field(e, "profile", true)))) fail("Question profile belongs to another choice group");
            }
            if (item.getLinkId().equals(wrapperId) && marker == null) fail("Choice group linkId collision");
        }
        var ordered = new ArrayList<QuestionnaireItemComponent>();
        for (var member : members) {
            var profile = field(member, "profile", true);
            var matches = profileRoots(q.getItem(), profile);
            if (matches.size() > 1) fail("Duplicate question profile roots: " + profile);
            var item = matches.isEmpty() ? null : matches.get(0);
            if (item != null && !q.getItem().contains(item) && (wrapper == null || !wrapper.getItem().contains(item))) fail("Cannot move choice group member from an existing parent: " + profile);
            if (item == null) {
                if (wrapper != null) fail("Existing choice group is missing a member");
                item = generate.apply(profile);
                if (item == null || !profile.equals(profileOf(item))) fail("Generated question has wrong profile");
                // Attach sequentially so generation sees all prior descendant IDs.
                q.addItem(item);
            }
            validateMember(item);
            item.setText(field(member, "text", true));
            var description = field(member, "description", false);
            if (description != null && !item.hasExtension(URL + "-description")) item.addExtension(new Extension(URL + "-description", new org.hl7.fhir.r4.model.MarkdownType(description)));
            ordered.add(item);
        }
        if (wrapper == null) {
            wrapper = new QuestionnaireItemComponent().setLinkId(wrapperId).setType(QuestionnaireItemType.GROUP).setText(text).setRepeats(false);
            wrapper.addExtension(descriptor.copy());
            wrapper.addExtension(new Extension(ITEM_CONTROL, new CodeableConcept().addCoding(new org.hl7.fhir.r4.model.Coding("http://hl7.org/fhir/questionnaire-item-control", "list", null))));
            var description = field(descriptor, "description", false);
            if (description != null) wrapper.addItem(new QuestionnaireItemComponent().setLinkId(wrapperId + "-help").setType(QuestionnaireItemType.DISPLAY).setText(description));
            q.addItem(wrapper);
        } else if (wrapper.getItem().stream().filter(i -> i.getType() != QuestionnaireItemType.DISPLAY).count() != ordered.size()) fail("Existing choice group contains unexpected members");
        for (var item : ordered) {
            q.getItem().remove(item);
            if (!wrapper.getItem().contains(item)) wrapper.addItem(item);
        }
        var ids = new HashSet<String>();
        for (var item : flatten(q.getItem())) if (!item.hasLinkId() || !ids.add(item.getLinkId())) fail("Duplicate Questionnaire linkId");
    }

    private static void validateMember(QuestionnaireItemComponent root) {
        var count = 0;
        for (var item : flatten(List.of(root))) {
            if (item.getRepeats()) fail("Repeating choice group member");
            if (item.getType() == QuestionnaireItemType.BOOLEAN) count++;
            else if (item.getType() != QuestionnaireItemType.GROUP && item.getType() != QuestionnaireItemType.DISPLAY) fail("Choice group members must contain a single Boolean question");
        }
        if (count != 1) fail("Choice group members must contain a single Boolean question");
    }
    private static String profileOf(QuestionnaireItemComponent item) { return item.hasDefinition() ? item.getDefinition().split("#")[0] : null; }
    private static List<QuestionnaireItemComponent> profileRoots(List<QuestionnaireItemComponent> items, String profile) {
        var found = new ArrayList<QuestionnaireItemComponent>();
        for (var item : items) {
            if (profile.equals(profileOf(item))) found.add(item);
            else found.addAll(profileRoots(item.getItem(), profile));
        }
        return found;
    }
    private static List<QuestionnaireItemComponent> flatten(List<QuestionnaireItemComponent> items) {
        var found = new ArrayList<QuestionnaireItemComponent>();
        for (var item : items) { found.add(item); found.addAll(flatten(item.getItem())); }
        return found;
    }
    private static String field(Extension extension, String key, boolean required) {
        var fields = extension.getExtensionsByUrl(key);
        if (fields.size() > 1 || (required && fields.isEmpty())) fail("Missing or duplicate choice group field: " + key);
        if (fields.isEmpty()) return null;
        var expectedType = switch (key) {
            case "protocol", "columns" -> "integer";
            case "profile" -> "canonical";
            case "control" -> "code";
            case "description" -> "markdown";
            default -> "string";
        };
        if (!fields.get(0).hasValue() || !fields.get(0).getValue().fhirType().equals(expectedType)) fail("Wrong choice group field type: " + key);
        var value = fields.get(0).hasValue() ? fields.get(0).getValue().primitiveValue() : null;
        if (value == null || value.isBlank()) fail("Empty choice group field: " + key);
        return value;
    }
    private static String linkId(String id) {
        try { return "crl-choice-group-" + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(id.getBytes(StandardCharsets.UTF_8))); }
        catch (java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    private static void fail(String message) { throw new IllegalArgumentException(message); }
}
