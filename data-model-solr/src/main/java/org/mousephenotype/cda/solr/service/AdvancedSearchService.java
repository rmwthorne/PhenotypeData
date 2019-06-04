package org.mousephenotype.cda.solr.service;

import org.apache.commons.collections.CollectionUtils;
import org.apache.solr.client.solrj.SolrServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

public class AdvancedSearchService {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	private PostQcService postQcService;

	@Inject
	public AdvancedSearchService(PostQcService postQcService){
		this.postQcService = postQcService;
	}

	public AdvancedSearchService() {

	}
	
	//get the number of genes associated with phenotypes - the same way as we do for the phenotypes pages?
	 public List<String> getGenesForPhenotype(String phenotypeId) throws IOException, URISyntaxException, SolrServerException{
		List<String> geneSymbols = postQcService.getGenesForMpId(phenotypeId);
		return geneSymbols;
	 }
	 
	 public List<String> getGenesForPhenotypeAndPhenotype(String phenotypeId, String phenotypeId2) throws IOException, URISyntaxException, SolrServerException{
		 List<String> geneSymbols = postQcService.getGenesForMpId(phenotypeId);
		 logger.info("geneSymbols.size(): " + geneSymbols.size());
		 List<String> geneSymbols2 = postQcService.getGenesForMpId(phenotypeId2);
		 @SuppressWarnings("unchecked")
		List<String> list = (List<String>) CollectionUtils.intersection(geneSymbols, geneSymbols2);
		 logger.info("geneSymbols2.size(): " + geneSymbols2.size());
		return list;
	 }
	 
	 public List<String> getGenesForPhenotypeORPhenotype(String phenotypeId, String phenotypeId2) throws IOException, URISyntaxException, SolrServerException{
		 List<String> geneSymbols = postQcService.getGenesForMpId(phenotypeId);
		 logger.info("geneSymbols.size(): " + geneSymbols.size());
		 List<String> geneSymbols2 = postQcService.getGenesForMpId(phenotypeId2);
		 logger.info("geneSymbols2.size(): " + geneSymbols2.size());
		 @SuppressWarnings("unchecked")
		List<String> list=(List<String>) CollectionUtils.union(geneSymbols, geneSymbols2);
		 
		return list;
	 }
}