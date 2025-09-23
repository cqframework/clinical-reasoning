/*-
 * #%L
 * HAPI FHIR Storage api
 * %%
 * Copyright (C) 2014 - 2025 Smile CDR, Inc.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package org.opencds.cqf.fhir.cr.hapi.config;

import ca.uhn.fhir.jpa.repository.SearchConverter;
import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.rest.param.TokenOrListParam;
import ca.uhn.fhir.rest.param.TokenParam;
import com.google.common.collect.Multimap;
import java.util.List;
import java.util.Map;

// LUKETODO:  do we really need this?
// LUKETODO:  javadoc
// LUKETODO:  unit test?
public class ClinicalIntelligenceSearchConverter extends SearchConverter {

    // LUKETODO:  javadoc
    @Override
    public void convertToSearchParameterMap(Multimap<String, List<IQueryParameterType>> theSearchMap) {
        if (theSearchMap == null) {
            return;
        }
        for (Map.Entry<String, List<IQueryParameterType>> entry : theSearchMap.entries()) {
            // if list of parameters is the value
            if (entry.getValue().size() > 1 && !isOrList(entry.getValue()) && !isAndList(entry.getValue())) {
                // is value a TokenParam
                addTokenToSearchIfNeeded(entry);

                // parameter type is single value list
            } else {
                for (IQueryParameterType value : entry.getValue()) {
                    setParameterTypeValue(entry.getKey(), value);
                }
            }
        }
    }

    private void addTokenToSearchIfNeeded(Map.Entry<String, List<IQueryParameterType>> theEntry) {
        if (isTokenParam(theEntry.getValue().get(0))) {
            String tokenKey = theEntry.getKey();
            TokenOrListParam tokenList = new TokenOrListParam();
            for (IQueryParameterType rec : theEntry.getValue()) {
                tokenList.add((TokenParam) rec);
            }
            mySearchParameterMap.add(tokenKey, tokenList);
        }
    }
}
