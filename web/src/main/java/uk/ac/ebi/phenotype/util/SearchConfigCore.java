/** *****************************************************************************
 * Copyright 2017 QMUL - Queen Mary University of London
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
 ****************************************************************************** */
package uk.ac.ebi.phenotype.util;

import java.util.List;
import org.apache.commons.lang.StringUtils;

/**
 * Functionality for creating solr search queries for a particular solr core.
 *
 * 
 * TO DO: more detailed docs on each function. Perhaps eliminate some of them. 
 * 
 */
public abstract class SearchConfigCore {
    
    public String defType() {
        return "edismax";
    }

    public abstract String qf();

    public abstract String fqStr();

    /**
     * Create a composite filter query string using the SearchConfig fq and an
     * additional string
     *
     * @param customFilterStr
     *
     * @return
     */
    public String fqStr(String customFilterStr) {
        if (StringUtils.isEmpty(customFilterStr)) {
            return fqStr();
        } else {
            return fqStr() + " +" + customFilterStr;
        }
    }

    public abstract String bqStr(String q);

    public abstract List<String> fieldList();

    public String fieldListSolrStr() {
        return "&fl=" + StringUtils.join(fieldList(), ",");
    }

    public abstract List<String> facetFields();

    public String facetFieldsSolrStr() {
        String solrStr = "";
        for (String facetField : facetFields()) {
            solrStr += "&facet.field=" + facetField;
        }
        return "&facet=on&facet.limit=-1&facet.sort=" + facetSort() + solrStr;
    }

    public abstract String facetSort();

    public abstract List<String> gridHeaders();

    public String gridHeadersStr() {
        return StringUtils.join(gridHeaders(), ",");
    }

    public abstract String breadcrumLabel();

    public abstract String sortingStr();

    public abstract String solrUrl();        
    
    /**
     * Create a complete query solr string. This applies the default settings
     * from the SearchConfig class and the user's query and custom filter
     *
     * @param query
     *
     * primary search string
     *
     * @param customFq
     *
     * a custom solr filter
     *
     * @param rows
     *
     * number of rows to return
     *
     * @return
     *
     * a solr query string
     */
    public String getQuerySolrStr(String query, String customFq, int rows) {
        return "q=" + query
                + "&fq=" + fqStr(customFq)
                + "&qf=" + qf()
                + "&defType=" + defType()
                + "&rows=" + rows
                + "&wt=json";
    }

    /**
     * Similar to getQuerySolrStr, but contains solr core url.
     * 
     * @param query
     * @param customFq
     * @param rows
     * @return 
     */
    public String getQuerySolrUrl(String query, String customFq, int rows) {        
        return solrUrl() + "/select?" + getQuerySolrStr(query, customFq, rows);
    }

    /**
     * Similar to getQuerySolrStr, but here forces result to have zero rows.
     * This is suitable for only obtaining an idea of the size of the result.
     *
     * @param query
     * @param customFq
     * @return
     */
    public String getCountQuerySolrStr(String query, String customFq) {
        return getQuerySolrStr(query, customFq, 0);
    }
    
    /**
     * Similar to getCountQuerySolrStr, but contains solr core url
     * 
     * @param query
     * @param customFq
     * @return 
     */
    public String getCountQuerySolrUrl(String query, String customFq) {
        return getQuerySolrUrl(query, customFq, 0);
    }
    
}