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

import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Provides support for solr search queries for core "impc_images"
 *
 */
@Service
public class SearchConfigImpcImage extends SearchConfigCore {

    @Override
    public String qf() {
        return "imgQf";
    }

    @Override
    public String fqStr() {
        return "*:*";
    }

    @Override
    public String bqStr(String q) {
        return "&bq=procedure_name:(" + q + ")^500"
                + " gene_symbol:(" + q + ")^500";
    }

    @Override
    public List<String> fieldList() {
        return Arrays.asList("omero_id",
                "procedure_name",
                "gene_symbol",
                "gene_accession_id",
                "anatomy_term",
                "anatomy_id",
                "jpeg_url",
                "thumbnail_url",
                "download_url",
                "parameter_association_name",
                "parameter_association_value");        
    }
   
    @Override
    public List<String> facetFields() {
        return Arrays.asList("procedure_name", "parameter_association_name_procedure_name",
                "anatomy_id_term",
                "anatomy_term_synonym_anatomy_id_term",
                "selected_top_level_anatomy_term",
                "selected_top_level_anatomy_id_anatomy_id_term",
                "selected_top_level_anatomy_term_anatomy_id_term",
                "selected_top_level_anatomy_term_synonym_anatomy_id_term",
                "intermediate_anatomy_id_anatomy_id_term",
                "intermediate_anatomy_term_anatomy_id_term",
                "intermediate_anatomy_term_synonym_anatomy_id_term",
                "symbol_gene",
                "marker_synonym_symbol_gene",
                "stage");        
    }    

    @Override
    public String facetSort() {
        return "index";
    }

    @Override
    public List<String> gridHeaders() {
        return Arrays.asList("Name", "Images");
    }

    @Override
    public String breadcrumLabel() {
        return "IMPC Images";
    }

    @Override
    public String sortingStr() {
        return "";
    }

}
