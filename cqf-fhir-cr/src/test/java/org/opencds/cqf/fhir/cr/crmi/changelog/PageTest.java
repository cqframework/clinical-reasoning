package org.opencds.cqf.fhir.cr.crmi.changelog;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import org.junit.jupiter.api.Test;

class PageTest {

    private static PageBase newPageBase() {
        return new PageBase("title", "id", "1.0.0", "name", "http://example.org/X", "Library");
    }

    /**
     * Regression: pages created for resources that exist on only one side of a manifest diff
     * (inserted-only or deleted-only) legitimately have a null oldData or newData. Prior to
     * the null-guard, any REPLACE / INSERT / DELETE op routed against the missing side would
     * NPE inside {@code PageBase.addOperation} and abort {@code $create-changelog}.
     */
    @Test
    void addReplaceOperation_withNullOldData_recordsOnNewDataOnly() {
        var newData = newPageBase();
        var page = new Page<>("http://example.org/X|2.0.0", null, newData);

        assertDoesNotThrow(() -> page.addOperation(ChangeLog.REPLACE, "url", "newUrl", "oldUrl"));

        // newData side recorded the operation; oldData side was null so nothing to record.
        assertEquals(ChangeLog.REPLACE, newData.getUrl().getOperation().getType());
    }

    @Test
    void addReplaceOperation_withNullNewData_recordsOnOldDataOnly() {
        var oldData = newPageBase();
        var page = new Page<>("http://example.org/X|1.0.0", oldData, null);

        assertDoesNotThrow(() -> page.addOperation(ChangeLog.REPLACE, "url", "newUrl", "oldUrl"));

        assertEquals(ChangeLog.REPLACE, oldData.getUrl().getOperation().getType());
    }

    @Test
    void addInsertOperation_withNullNewData_isNoOp() {
        var oldData = newPageBase();
        var page = new Page<>("http://example.org/X|1.0.0", oldData, null);

        assertDoesNotThrow(() -> page.addOperation(ChangeLog.INSERT, "version", "9.9.9", null));

        // oldData isn't touched by an insert; nothing should have changed on either side.
        assertNull(oldData.getVersion().getOperation());
    }

    @Test
    void addDeleteOperation_withNullOldData_isNoOp() {
        var newData = newPageBase();
        var page = new Page<>("http://example.org/X|2.0.0", null, newData);

        assertDoesNotThrow(() -> page.addOperation(ChangeLog.DELETE, "version", null, "1.0.0"));

        // newData isn't touched by a delete; nothing should have changed on either side.
        assertNull(newData.getVersion().getOperation());
    }

    @Test
    void addOperation_unknownTypeStillThrows() {
        var page = new Page<>("http://example.org/X", newPageBase(), newPageBase());

        assertThrows(UnprocessableEntityException.class, () -> page.addOperation("not-a-real-type", "url", "a", "b"));
    }
}
