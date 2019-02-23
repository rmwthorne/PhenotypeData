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
package uk.ac.ebi.phenotype.web.controller;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.mousephenotype.cda.solr.service.SearchGeneService;
import org.mousephenotype.cda.solr.service.SearchPhenotypeService;
import org.mousephenotype.cda.solr.service.dto.GeneDTO;
import org.mousephenotype.cda.solr.service.dto.MpDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class SearchController {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final SearchGeneService searchGeneService;
    private final SearchPhenotypeService searchPhenotypeService;

    @Autowired
    public SearchController(SearchGeneService searchGeneService, SearchPhenotypeService searchPhenotypeService) {
        this.searchGeneService = searchGeneService;
        this.searchPhenotypeService = searchPhenotypeService;
    }


    /**
     * redirect calls to the base url or a page named index.html to the search page
     *
     * @return
     */
    @RequestMapping(value={"/", "/index.html"})
    public String rootForward(HttpServletRequest request) {

        String scheme = (request.getAttribute("isProxied") == Boolean.TRUE ? "https" : request.getScheme());
        logger.info("rootForward(): isProxied = {}. scheme = {}.", request.getAttribute("isProxied"), scheme);
        String redirectUrl = scheme + ":" + request.getAttribute("mappedHostname") + request.getAttribute("baseUrl") + "/search";

        return "redirect:" + redirectUrl;
    }


    @RequestMapping("/search")
    public String search(@RequestParam(value = "term", required = false, defaultValue = "*") String term,
                         @RequestParam(value = "type", required = false, defaultValue = "gene") String type,
                         @RequestParam(value = "page", required = false, defaultValue = "1") String page,
                         @RequestParam(value = "rows", required = false, defaultValue = "10") String rows,
                         Model model) throws IOException, SolrServerException {

        Integer pageNumber = Integer.parseInt(page);
        Integer rowsPerPage = Integer.parseInt(rows);
        Integer start = (pageNumber-1) * rowsPerPage;

        if (type.equalsIgnoreCase("gene")) {
            model = searchGenes(term, start, rowsPerPage, model);
        } else {
            model = searchPhenotypes(term, start, rowsPerPage, model);
        }

        Long numberOfResults = Long.parseLong((String)model.asMap().get("numberOfResults"));
        Long numPages = (long) Math.ceil((double) numberOfResults / rowsPerPage);

        model.addAttribute("currentPage", pageNumber);
        model.addAttribute("rows", rowsPerPage);
        model.addAttribute("start", start);
        model.addAttribute("numPages", numPages);

        return "search";
    }


    private Model searchGenes(String term, Integer start, Integer rows, Model model) throws SolrServerException, IOException {

        QueryResponse response = searchGeneService.searchGenes(term, start, rows);
        final List<GeneDTO> genes = response.getBeans(GeneDTO.class);

        if (genes.size()<1) {
            QueryResponse suggestionsResponse = searchGeneService.searchSuggestions(term, 3);
            final List<String> suggestions = suggestionsResponse
                    .getBeans(GeneDTO.class)
                    .stream()
                    .map(GeneDTO::getMarkerSymbol)
                    .collect(Collectors.toList());
            model.addAttribute("geneSuggestions", suggestions);
        }

        model.addAttribute("genes", genes);
        model.addAttribute("numberOfResults", Long.toString(response.getResults().getNumFound()));

        return model;
    }

    private Model searchPhenotypes(String term, Integer start, Integer rows, Model model) throws SolrServerException, IOException {

        QueryResponse response = searchPhenotypeService.searchPhenotypes(term, start, rows);
        final List<SearchResultMpDTO> phenotypes = response.getBeans(SearchResultMpDTO.class);

        if (phenotypes.size() < 1) {
            QueryResponse suggestionsResponse = searchPhenotypeService.searchSuggestions(term, 3);
            System.out.println("suggestionsResponse: " + suggestionsResponse);
            final List<String> suggestions = suggestionsResponse
                    .getBeans(MpDTO.class)
                    .stream()
                    .map(MpDTO::getMpTerm)
                    .collect(Collectors.toList());
            model.addAttribute("phenotypeSuggestions", suggestions);
        }

        // Augment results with counts of genes with phenotype data for each phenotype returned
        final Map<String, Integer> genesByPhenotype = searchPhenotypeService.getGenesByPhenotype();
        for (SearchResultMpDTO result : phenotypes) {
            result.setGeneCount(genesByPhenotype.getOrDefault(result.getMpTerm(), 0));
        }

        model.addAttribute("phenotypes", phenotypes);
        model.addAttribute("numberOfResults", Long.toString(response.getResults().getNumFound()));

        return model;
    }


}
