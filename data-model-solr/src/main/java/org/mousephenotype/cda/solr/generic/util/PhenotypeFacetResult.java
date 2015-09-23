/*******************************************************************************
 * Copyright 2015 EMBL - European Bioinformatics Institute
 *
 * Licensed under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the
 * License.
 *******************************************************************************/
package org.mousephenotype.cda.solr.generic.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mousephenotype.cda.solr.web.dto.PhenotypeCallSummaryDTO;

public class PhenotypeFacetResult {
	
	List<PhenotypeCallSummaryDTO> phenotypeCallSummaries = new ArrayList<PhenotypeCallSummaryDTO>();
	Map<String, Map<String, Integer>> facetResults = new HashMap<String, Map<String, Integer>>();
	
	public List<PhenotypeCallSummaryDTO> getPhenotypeCallSummaries() {
		return phenotypeCallSummaries;
	}
	
	public void setPhenotypeCallSummaries(	List<PhenotypeCallSummaryDTO> phenotypeCallSummaries) {
		this.phenotypeCallSummaries = phenotypeCallSummaries;
	}
	
	public Map<String, Map<String, Integer>> getFacetResults() {
		return facetResults;
	}
	
	public void setFacetResults(Map<String, Map<String, Integer>> facetResults) {
		this.facetResults = facetResults;
	}
}
