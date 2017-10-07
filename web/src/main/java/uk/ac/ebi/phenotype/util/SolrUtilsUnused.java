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
package uk.ac.ebi.phenotype.util;

/**
 * TK: This class is unused. It's old name SolrUtils conflicted with another
 * class defined in data-model-solr, hence renamed here.
 * 
 */
public class SolrUtilsUnused {

	/**
	 * Method to handle spaces within queries for solr requests via solrj
	 *
	 * @param id
	 */
	public static String processQuery(String id) {

		String processedId = id;

		// Quote the ID if it hasn't been already
		if (processedId.contains(":") && !processedId.contains("\\")) {
			processedId = "\"" + processedId + "\"";
		}

		// put quotes around any query that contains spaces
		if (processedId.contains(" ")) {
			processedId = "\"" + processedId + "\"";
		}

		return processedId;
	}
}