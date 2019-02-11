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

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

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

@Controller
public class SearchController {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	SearchGeneService searchGeneService;

	@Autowired
	SearchPhenotypeService searchPhenotypeService;


	/**
	 * redirect calls to the base url or /search path to the search page with the version 2 URL path
	 *
	 * @return
	 */
	@RequestMapping("/index.html")
	public String rootForward(HttpServletRequest request) {

		String scheme = (request.getAttribute("isProxied") == Boolean.TRUE ? "https" : request.getScheme());
		logger.info("rootForward(): isProxied = {}. scheme = {}.", request.getAttribute("isProxied"), scheme);
		String redirectUrl = scheme + ":" + request.getAttribute("mappedHostname") + request.getAttribute("baseUrl") + "/search";

		return "redirect:" + redirectUrl;
	}


	/**
	 * search page
	 *
	 */

	/**
	 * redirect calls to the base url or /search path to the search page with the version 2 URL path
	 *
	 * @return
	 */
//	@RequestMapping("/search/")  // appended slash
//	public String searchForward(HttpServletRequest request) {
//
//		String scheme = (request.getAttribute("isProxied") == Boolean.TRUE ? "https" : request.getScheme());
//		logger.info("searchForward(): isProxied = {}. scheme = {}.", request.getAttribute("isProxied"), scheme);
//		String redirectUrl = scheme + ":" + request.getAttribute("mappedHostname") + request.getAttribute("baseUrl") + "/search/gene?kw=*";
//
//		return "redirect:" + redirectUrl;
//	}

	@RequestMapping("/search")
	public String search(@RequestParam(value = "term", required = false, defaultValue = "*") String term,
			@RequestParam(value = "type", required = false, defaultValue = "gene") String type,
			@RequestParam(value = "start", required = false, defaultValue = "0") String start,
			@RequestParam(value = "rows", required = false, defaultValue = "10") String rows,
			HttpServletRequest request,
			Model model) throws IOException, URISyntaxException, SolrServerException {
		
		System.out.println("calling type="+ type+" search method kw="+ term+"  start="+start+" rows="+rows);
		int startLong = Integer.parseInt(start);
		int rowLong = Integer.parseInt(rows);
		if(type.equalsIgnoreCase("gene")) {
			model=searchGenes(term,  startLong, rowLong, model);
		}else {
			model=searchPhenotypes(term,  startLong, rowLong, model);
		}
		return "search";
	}


	private Model searchGenes(String term, Integer start, Integer rows, Model model) throws SolrServerException, IOException {
		if(term.contains(":")) {
			term=term.replace(":", "\\:");
		}
		QueryResponse response = searchGeneService.searchGenes(term, start, rows);
		final List<GeneDTO> genes = response.getBeans(GeneDTO.class);
		model.addAttribute("genes", genes);
		model=addPagination(start, rows, response,model);
		return model;
	}
	
	private Model searchPhenotypes(String term, Integer start, Integer rows, Model model) throws SolrServerException, IOException {
		QueryResponse response = searchPhenotypeService.searchPhenotypes(term, start, rows);
		final List<MpDTO> phenotypes = response.getBeans(MpDTO.class);
		model.addAttribute("phenotypes", phenotypes);
		model=addPagination(start, rows, response, model);
		return model;
	}
	
	private Model addPagination(Integer start, Integer rows, QueryResponse response, Model model) {
		Long numberOfResults=response.getResults().getNumFound();
		model.addAttribute("numberOfResults",Long.toString(response.getResults().getNumFound()));
		model.addAttribute("start",start);
		model.addAttribute("rows",rows);
		long pagesDouble=Math.round((double)numberOfResults/(double)rows);
		
		System.out.println("numberOfResults="+numberOfResults+"pagesLong="+pagesDouble+" pages="+pagesDouble);
		model.addAttribute("pages", String.valueOf(pagesDouble));
		return model;
	}

	



	
	

}
