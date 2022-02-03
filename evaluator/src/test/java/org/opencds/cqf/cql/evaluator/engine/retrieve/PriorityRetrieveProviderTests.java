
package org.opencds.cqf.cql.evaluator.engine.retrieve;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import java.util.Collections;
import java.util.List;

import com.google.common.collect.Lists;

import org.opencds.cqf.cql.engine.retrieve.RetrieveProvider;
import org.opencds.cqf.cql.engine.runtime.Code;
import org.opencds.cqf.cql.engine.runtime.Interval;
import org.testng.annotations.Test;


public class PriorityRetrieveProviderTests {

    @Test(expectedExceptions = NullPointerException.class)
    public void test_nullConstructorParameterThrowsException() {
        new PriorityRetrieveProvider(null);
    }

    @Test
    public void test_noProviders_returnsEmptySet() {
        RetrieveProvider retrieve = new PriorityRetrieveProvider(Collections.emptyList());
        Iterable<Object> result= retrieve.retrieve(null, null, null, null, null, null, null, null, null, null, null, null);
        assertNotNull(result);
        List<Object> resultList = Lists.newArrayList(result);
        assertEquals(resultList.size(), 0);
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void test_badProvider_throwsException() {

        RetrieveProvider badProvider = new RetrieveProvider(){
            @Override
            public Iterable<Object> retrieve(String context, String contextPath, Object contextValue, String dataType,
                    String templateId, String codePath, Iterable<Code> codes, String valueSet, String datePath,
                    String dateLowPath, String dateHighPath, Interval dateRange) {

                // This is an invalid results. Providers should return an empty set.
                return null;
            }
        };
        RetrieveProvider retrieve = new PriorityRetrieveProvider(Collections.singletonList(badProvider));
        retrieve.retrieve(null, null, null, null, null, null, null, null, null, null, null, null);
    }

    @Test
    public void test_retrieve_returnsFirstNonEmpty() {
        RetrieveProvider providerOne = new RetrieveProvider(){
            @Override
            public Iterable<Object> retrieve(String context, String contextPath, Object contextValue, String dataType,
                    String templateId, String codePath, Iterable<Code> codes, String valueSet, String datePath,
                    String dateLowPath, String dateHighPath, Interval dateRange) {
                return Collections.emptySet();
            }
        };

        RetrieveProvider providerTwo = new RetrieveProvider(){
            @Override
            public Iterable<Object> retrieve(String context, String contextPath, Object contextValue, String dataType,
                    String templateId, String codePath, Iterable<Code> codes, String valueSet, String datePath,
                    String dateLowPath, String dateHighPath, Interval dateRange) {
                return Lists.newArrayList(1, 2, 3);
            }
        };

        RetrieveProvider providerThree = new RetrieveProvider(){
            @Override
            public Iterable<Object> retrieve(String context, String contextPath, Object contextValue, String dataType,
                    String templateId, String codePath, Iterable<Code> codes, String valueSet, String datePath,
                    String dateLowPath, String dateHighPath, Interval dateRange) {
                return Lists.newArrayList(5, 4, 3, 2, 1);
            }
        };

        RetrieveProvider retrieve = new PriorityRetrieveProvider(Lists.newArrayList(providerOne, providerTwo, providerThree));
        Iterable<Object> results = retrieve.retrieve(null, null, null, null, null, null, null, null, null, null, null, null);
        assertNotNull(results);
        List<Object> resultList = Lists.newArrayList(results);
        assertEquals(resultList.size(), 3);
    }
}