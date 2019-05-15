package uk.ac.ebi.phenotype.web.dao;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.mousephenotype.cda.solr.service.dto.ExperimentDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.PagedResources;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;



/**
 * Service should be able to connect to a direct file Dao or a rest Dao
 * @author jwarren
 *
 */

@Service
public class StatisticsService {
	@Autowired
	StatsClient statsClient;
	
	//need to import the jar somehow or have the stats repo as a module in the PA???
	
	
	public ResponseEntity<PagedResources<Statistics>> getUniqueStatsResult(String geneAccession, String alleleAccession, String parameterStableId,
			 String pipelineStableId,  String zygosity,  String phenotypingCenter,  String metaDataGroup){
		
		ResponseEntity<PagedResources<Statistics>> stats=statsClient.getUniqueStatsResult(geneAccession, alleleAccession, parameterStableId,
		 pipelineStableId,  zygosity,  phenotypingCenter,  metaDataGroup);
		return stats;
	
		}
	

	
	public ExperimentDTO getSpecificExperimentDTOFromRest(String parameterStableId, String pipelineStableId, String geneAccession, List<String> genderList, List<String> zyList, String phenotypingCenter, String strain, String metaDataGroup, String alleleAccession, String ebiMappedSolrUrl)
	{
		String zygosity=null;
	
//		if(zyList.isEmpty()||zyList==null) {
//			zygosity=null;
//		}else {
//			
//		}
		ResponseEntity<PagedResources<Statistics>> response = this.getUniqueStatsResult(geneAccession, alleleAccession, parameterStableId, pipelineStableId, "homozygote", phenotypingCenter, metaDataGroup);
		Collection<Statistics> stats = response.getBody().getContent();
		assert(stats.size()==1);
		ExperimentDTO exp = StatisticsServiceUtilities.convertToExperiment(parameterStableId, stats);
		
		System.out.println("experiment from file="+exp);
		return exp;
}
	
	
//	public ExperimentDTO getSpecificExperimentDTOFromRepository(String parameterStableId, String pipelineStableId, String geneAccession, List<String> genderList, List<String> zyList, String phenotypingCenter, String strain, String metaDataGroup, String alleleAccession, String ebiMappedSolrUrl)
//	{
//		String zygosity=null;
//	
////		if(zyList.isEmpty()||zyList==null) {
////			zygosity=null;
////		}else {
////			
////		}
//		List<Statistics> stats = statsRepository.findByGeneAccessionAndAlleleAccessionAndParameterStableIdAndPipelineStableIdAndZygosityAndPhenotypingCenterAndMetaDataGroup(geneAccession, alleleAccession, parameterStableId, pipelineStableId, "homozygote", phenotypingCenter, metaDataGroup);
//		assert(stats.size()>0);
//		ExperimentDTO exp = StatisticsServiceUtilities.convertToExperiment(parameterStableId, stats);
//		
//		System.out.println("experiment from file="+exp);
//		return exp;
//}

	
	
	 /**
     * @return the dateOfExperiment
     */
    public Date getDateOfExperiment(Date dateOfExperiment) {
	    //        return dateOfExperiment;
    	if(dateOfExperiment==null){
    		return null;
    	}
	    ZonedDateTime zdt = ZonedDateTime.ofInstant(dateOfExperiment.toInstant(), ZoneId.of("UTC"));
	    if(TimeZone.getDefault().inDaylightTime(dateOfExperiment)) {
		    zdt = dateOfExperiment.toInstant().atZone(ZoneId.of(TimeZone.getDefault().getID()));
	    }
	    return Date.from(zdt.toLocalDateTime().toInstant(ZoneOffset.ofHours(0)));
    }

	
	public ResponseEntity<PagedResources<Statistics>> getStatsDataForGeneAccesssion(String geneAccession) {
		return statsClient.getStatsDataForGeneAccession(geneAccession);
		
	}
	

	public ResponseEntity<PagedResources<Statistics>> getStatsDataForGeneSymbol(String geneSybmol) {
		return statsClient.getStatsDataForGeneSymbol(geneSybmol);
		
	}

	
	
}
