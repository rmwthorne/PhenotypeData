package org.mousephenotype.cda.solr.service;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mousephenotype.cda.db.dao.PhenotypePipelineDAO;
import org.mousephenotype.cda.solr.TestConfigSolr;
import org.mousephenotype.cda.solr.repositories.GenotypePhenotypeRepository;
import org.mousephenotype.cda.solr.repositories.StatisticalResultRepository;
import org.mousephenotype.cda.solr.service.dto.GenotypePhenotypeDTO;
import org.mousephenotype.cda.solr.service.dto.StatisticalResultDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test the statistical result core
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes={TestConfigSolr.class})
@TestPropertySource(locations = {"file:${user.home}/configfiles/${profile:dev}/test.properties"})
@Transactional
public class StatisticalResultTest {

	public static final Integer NUM_TO_TEST = 50;

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	PhenotypePipelineDAO ppDAo;

	@Autowired
	StatisticalResultService statisticalResultService;

	@Autowired
	GenotypePhenotypeRepository genotypePhenotypeRepository;

	@Autowired
	StatisticalResultRepository statisticalResultRepository;


	@Test
	public void allReferenceRangeDocumentsHaveATopLevelMpTermId() throws IOException, SolrServerException {

		//q=-top_level_mp_term_id:*&fq=status:Success&fq=-procedure_stable_id:IMPC_VIA_001&fq=-procedure_stable_id:IMPC_FER*&fq=+statistical_method:"Reference%20Ranges%20Plus%20framework"&fq=mp_term_id_options:*

		SolrQuery query = new SolrQuery("-top_level_mp_term_id:* AND -female_top_level_mp_term_id:* AND -male_top_level_mp_term_id:*")
				.addFilterQuery("status:Success")
				.addFilterQuery("-procedure_stable_id:IMPC_VIA_001")
				.addFilterQuery("-procedure_stable_id:IMPC_FER*")
				.addFilterQuery("mp_term_id_options:*")
				.addFilterQuery("data_type:unidimensional-ReferenceRange")

				// These legacy parameters have no abnormal term defined in impress, so when there is no
				// specific term, we cannot find a fallback term.  Ignore these
				.addFilterQuery("-parameter_stable_id:ESLIM_006_001_004")
				.addFilterQuery("-parameter_stable_id:ESLIM_006_001_015")
				.addFilterQuery("-parameter_stable_id:ESLIM_006_001_016")
				.addFilterQuery("-parameter_stable_id:ESLIM_006_001_024")
				.addFilterQuery("-parameter_stable_id:ESLIM_006_001_025")
				.addFilterQuery("-parameter_stable_id:ESLIM_006_001_027")
				.addFilterQuery("-parameter_stable_id:ESLIM_006_001_028")
				.addFilterQuery("-parameter_stable_id:ESLIM_006_001_031")

				.setRows(1);

		QueryResponse response = statisticalResultService.getSolrServer().query(query);
		logger.debug("query in allDocumentsHaveTopLevelmp=" + query);

		long numberFound = response.getResults().getNumFound();
		logger.debug("number of documents in SR core without a top level mp=" + numberFound);
		if(numberFound > 0) {
			logger.warn(response.getBeans(StatisticalResultDTO.class).get(0).toString());

		}

		assertFalse("Found " + numberFound + " documents in SR core without a top-level mp", numberFound > 0);

	}

	@Test
	public void verifyAllGpEntriesAppearInSrCore(){


		List<GenotypePhenotypeDTO> list = genotypePhenotypeRepository.findByProcedureStableId("IMPC_ABR_001");
		list.stream().limit(NUM_TO_TEST).forEach(x -> logger.info("Marker {}, Phenotype {}, Sex {}", x.getMarkerAccessionId(), x.getMpTermName(), x.getSex()) );
		assertTrue(list.size()>0);

		// Check random assortment of parameters
		List<String> parametersToCheck = Arrays.asList(
				"IMPC_DXA_006_001",
				"IMPC_CSD_027_001",
				"IMPC_ECG_004_001",
				"IMPC_ECG_006_001",
				"IMPC_OFD_021_001",
				"IMPC_HEM_001_001",
				"IMPC_ECG_002_001");

		for (String parameter : parametersToCheck) {

			list = new ArrayList<>(genotypePhenotypeRepository.findByParameterStableId(parameter));

			// Randomize the list so we check a different NUM_TO_TEST for each test run
			Collections.shuffle(list);

			list.stream().limit(NUM_TO_TEST).forEach(x -> logger.info("Marker {}, Phenotype {}, Sex {}", x.getMarkerAccessionId(), x.getMpTermName(), x.getSex()));
			assertTrue(list.size() > 0);

			list.stream().limit(NUM_TO_TEST).forEach(x -> {
				List<StatisticalResultDTO> list1 = statisticalResultRepository.findByMarkerAccessionIdAndParameterStableIdAndProcedureStableIdAndPhenotypeSexInAndMpTermIdOptionsIn(x.getMarkerAccessionId(), x.getParameterStableId(), x.getProcedureStableId(), x.getSex(), x.getMpTermId());
				if (list1.size() == 0) {
					logger.warn("Genotype phenotype result for {}, which has no statistical-results entry", StringUtils.join(Arrays.asList(x.getMarkerAccessionId(), x.getParameterStableId(), x.getProcedureStableId(), x.getSex(), x.getMpTermId()), ", "));
				}
			});
		}
	}

	@Test
	public void testGetDistinctPipelineAlleleCenterListByGeneAccession() {

		statisticalResultRepository.findByParameterStableId(null);

	}
}