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

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.Group;
import org.apache.solr.common.SolrDocumentList;
import org.mousephenotype.cda.enumerations.ObservationType;
import org.mousephenotype.cda.solr.generic.util.PhenotypeCallSummarySolr;
import org.mousephenotype.cda.solr.generic.util.PhenotypeFacetResult;
import org.mousephenotype.cda.solr.repositories.image.ImagesSolrDao;
import org.mousephenotype.cda.solr.service.*;
import org.mousephenotype.cda.solr.service.dto.ImpressBaseDTO;
import org.mousephenotype.cda.solr.service.dto.ImpressDTO;
import org.mousephenotype.cda.solr.service.dto.MpDTO;
import org.mousephenotype.cda.solr.service.dto.ParameterDTO;
import org.mousephenotype.cda.solr.web.dto.DataTableRow;
import org.mousephenotype.cda.solr.web.dto.PhenotypeCallSummaryDTO;
import org.mousephenotype.cda.solr.web.dto.PhenotypePageTableRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uk.ac.ebi.phenotype.error.GenomicFeatureNotFoundException;
import uk.ac.ebi.phenotype.error.OntologyTermNotFoundException;
import uk.ac.ebi.phenotype.generic.util.RegisterInterestDrupalSolr;
import uk.ac.ebi.phenotype.web.util.FileExportUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.*;

@Controller
public class PhenotypesController {

    private final Logger log = LoggerFactory.getLogger(PhenotypesController.class);
    private static final int numberOfImagesToDisplay = 5;

    @Autowired
    private PhenotypeCallSummarySolr phenotypeSummaryHelper;
    
    @Autowired
    private ImagesSolrDao imagesSummaryHelper;

    @Autowired
    SolrIndex solrIndex;
    
    @Autowired
    StatisticalResultService srService;
    @Autowired
	@Qualifier("postqcService")
    PostQcService gpService;

    @Autowired
    MpService mpService;

    @Autowired
    ObservationService os;

    @Autowired
    PreQcService preqcService;

    @Autowired
    ImpressService impressService;

    @Autowired
    ImageService imageService;

    @Value("drupalBaseUrl")
    private String drupalBaseUrl;

    /**
     * Phenotype controller loads information required for displaying the
     * phenotype page or, in the case of an error, redirects to the error page
     *
     * @param phenotypeId the Mammalian phenotype id of the phenotype to
     * display
     * @param model the data model
     * @param request the <code>HttpServletRequest</code> request instance
     * @param attributes redirect attributes
     * @return the name of the view to render, or redirect to search page on
     * error
     * @throws OntologyTermNotFoundException
     * @throws URISyntaxException
     * @throws IOException
     * @throws SolrServerException, IOException
     * @throws SQLException
     *
     */
    @RequestMapping(value = "/phenotypes/{phenotypeId}", method = RequestMethod.GET)
    public String loadMpPage(   @PathVariable String phenotypeId,  Model model, HttpServletRequest request, RedirectAttributes attributes)
    throws OntologyTermNotFoundException, IOException, URISyntaxException, SolrServerException, SQLException {
    	
    	// Check whether the MP term exists
    	MpDTO mpTerm = mpService.getPhenotype(phenotypeId);
    	
     	model.addAttribute("hasData", mpTerm  == null ? false : true);
        
        // register interest state
 		RegisterInterestDrupalSolr registerInterest = new RegisterInterestDrupalSolr(drupalBaseUrl, request);
 		Map<String, String> regInt = registerInterest.registerInterestState(phenotypeId, request, registerInterest);
 		model.addAttribute("registerInterestButtonString", regInt.get("registerInterestButtonString"));
 		model.addAttribute("registerButtonAnchor", regInt.get("registerButtonAnchor"));
 		model.addAttribute("registerButtonId", regInt.get("registerButtonId"));
        
        // Query the images for this phenotype
        SolrDocumentList images = imagesSummaryHelper.getDocsForMpTerm(phenotypeId, 0, numberOfImagesToDisplay).getResults();

        model.addAttribute("images", images);

        // IMPC image display at the bottom of the page
        List<Group> groups = imageService.getPhenotypeAssociatedImages(null, phenotypeId, null, true, 1);
        Map<String, String> paramToNumber=new HashMap<>();
        for(Group group:groups){
            if(!paramToNumber.containsKey(group.getGroupValue())){
                paramToNumber.put(group.getGroupValue(), Long.toString(group.getResult().getNumFound()));
            }
        }
        model.addAttribute("paramToNumber", paramToNumber);
        model.addAttribute("impcImageGroups",groups);

        model.addAttribute("isLive", new Boolean((String) request.getAttribute("liveSite")));
        
        model.addAttribute("phenotype", mpTerm);


        List<ImpressDTO> procedures = new ArrayList<ImpressDTO>(impressService.getProceduresByMpTerm(phenotypeId));
	    Collections.sort(procedures, ImpressDTO.getComparatorByProcedureNameImpcFirst());
	    model.addAttribute("procedures", procedures);

	    model.addAttribute("parametersAssociated", getParameters(phenotypeId));

	    model.addAttribute("genePercentage", ControllerUtils.getPercentages(phenotypeId, srService, gpService));

        // Stuff for parent-child display
        model.addAttribute("hasChildren", mpService.getChildren(phenotypeId).size() > 0 ? true : false);
        model.addAttribute("hasParents", mpService.getParents(phenotypeId).size() > 0 ? true : false);

        // Associations table and filters
        PhenotypeFacetResult phenoResult = phenotypeSummaryHelper.getPhenotypeCallByMPAccessionAndFilter(phenotypeId,  null, null, null);
        PhenotypeFacetResult preQcResult = phenotypeSummaryHelper.getPreQcPhenotypeCallByMPAccessionAndFilter(phenotypeId,  null, null, null);
        model.addAttribute("phenoFacets", getPhenotypeFacets(phenoResult, preQcResult));
        model.addAttribute("errorMessage", getErrorMessage(phenoResult, preQcResult));   
        model.addAttribute("phenotypes", getPhenotypeRows(phenoResult, preQcResult, request.getAttribute("baseUrl").toString()));
        
        return "phenotypes";
        
    }
    
    
    private  Map<String, Map<String, Integer>> getPhenotypeFacets(PhenotypeFacetResult  phenoResult, PhenotypeFacetResult  preQcResult){
    	
        Map<String, Map<String, Integer>> phenoFacets = phenoResult.getFacetResults();
        Map<String, Map<String, Integer>> preQcFacets = preQcResult.getFacetResults();

		for (String key : preQcFacets.keySet()){
			if (preQcFacets.get(key).keySet().size() > 0){
				for (String key2: preQcFacets.get(key).keySet()){
					phenoFacets.get(key).put(key2, preQcFacets.get(key).get(key2)); 
				}
			}
		}
		
        return sortPhenFacets(phenoFacets);
    }
    
    
    private String getErrorMessage(PhenotypeFacetResult... phenoResult){
    	
    	Set<String> errorCodes = new HashSet<>();
    	for (PhenotypeFacetResult result: phenoResult){
    		errorCodes.addAll(result.getErrorCodes());
    	}
 		String errorMessage = null;
 		if (errorCodes != null && errorCodes.size() > 0){
 			errorMessage = "There was a problem retrieving some of the phenotype calls. Some rows migth be missing from the table below. Error code(s) " +
 			StringUtils.join(errorCodes, ", ") + ".";
 		}
 		
 		return errorMessage;
    }
    

    private Map<String, Map<String, Integer>> sortPhenFacets(Map<String, Map<String, Integer>> phenFacets) {
    	
        Map<String, Map<String, Integer>> sortPhenFacets = phenFacets;
        for (String key : phenFacets.keySet()) {
            sortPhenFacets.put(key, new TreeMap<String, Integer>(phenFacets.get(key)));
        }
        return sortPhenFacets;
    }
    

    /**
     * @throws IOException
     * @throws URISyntaxException
     * @throws SolrServerException, IOException
     */
    private List<DataTableRow> getPhenotypeRows(PhenotypeFacetResult phenoResult, PhenotypeFacetResult preQcResult, String baseUrl)
    throws IOException, URISyntaxException, SolrServerException {


    	List<PhenotypeCallSummaryDTO> phenotypeList;
        phenotypeList = phenoResult.getPhenotypeCallSummaries();
        phenotypeList.addAll(preQcResult.getPhenotypeCallSummaries());

        // This is a map because we need to support lookups
        Map<Integer, DataTableRow> phenotypes = new HashMap<Integer, DataTableRow>();

        for (PhenotypeCallSummaryDTO pcs : phenotypeList) {

            // On the phenotype pages we only display stats graphs as evidence, the MPATH links can't be linked from phen pages
            DataTableRow pr = new PhenotypePageTableRow(pcs, baseUrl, drupalBaseUrl, false);

	        // Collapse rows on sex
            if (phenotypes.containsKey(pr.hashCode())) {

                pr = phenotypes.get(pr.hashCode());
                // Use a tree set to maintain an alphabetical order (Female, Male)
                TreeSet<String> sexes = new TreeSet<String>();
                for (String s : pr.getSexes()) {
                    sexes.add(s);
                }
                sexes.add(pcs.getSex().toString());

                pr.setSexes(new ArrayList<String>(sexes));
            }

            if (pr.getParameter() != null && pr.getProcedure() != null) {
                phenotypes.put(pr.hashCode(), pr);
            }
        }

        List<DataTableRow> list = new ArrayList<DataTableRow>(phenotypes.values());
        Collections.sort(list);

        return list;

    }

    @ExceptionHandler(OntologyTermNotFoundException.class)
    public ModelAndView handleOntologyTermNotFoundException(OntologyTermNotFoundException exception) {
        ModelAndView mv = new ModelAndView("identifierError");
        mv.addObject("errorMessage", exception.getMessage());
        mv.addObject("acc", exception.getAcc());
        mv.addObject("type", "mammalian phenotype");
        mv.addObject("exampleURI", "/phenotypes/MP:0000585");
        return mv;
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView handleGenericException(Exception exception) {
        exception.printStackTrace();
        ModelAndView mv = new ModelAndView("identifierError");
        mv.addObject("errorMessage", exception.getMessage());
        mv.addObject("acc", "");
        mv.addObject("type", "mammalian phenotype");
        mv.addObject("exampleURI", "/phenotypes/MP:0000585");
        return mv;
    }

    @RequestMapping("/geneVariantsWithPhenotypeTable/{acc}") // Keep params in synch with export()
    public String geneVariantsWithPhenotypeTable(
            @PathVariable String acc,
			@RequestParam(required = false, value = "procedure_name")  List<String> procedureName,
			@RequestParam(required = false, value = "marker_symbol")  List<String> markerSymbol,
			@RequestParam(required = false, value = "mp_term_name")  List<String> mpTermName,			
            Model model,
            HttpServletRequest request,
            RedirectAttributes attributes) 
    throws KeyManagementException, NoSuchAlgorithmException, URISyntaxException, GenomicFeatureNotFoundException, IOException, SolrServerException {
        
        // Associations table and filters
        PhenotypeFacetResult phenoResult = phenotypeSummaryHelper.getPhenotypeCallByMPAccessionAndFilter(acc,  procedureName, markerSymbol, mpTermName);
        PhenotypeFacetResult preQcResult = phenotypeSummaryHelper.getPreQcPhenotypeCallByMPAccessionAndFilter(acc,  procedureName, markerSymbol, mpTermName);       
        model.addAttribute("phenoFacets", getPhenotypeFacets(phenoResult, preQcResult));
        model.addAttribute("errorMessage", getErrorMessage(phenoResult, preQcResult));   
        model.addAttribute("phenotypes", phenotypeSummaryHelper.getPhenotypeRows(phenoResult, preQcResult, request.getAttribute("baseUrl").toString()));
        return "geneVariantsWithPhenotypeTable";
    }

    
    /**
     * @author ilinca
     * @since 2016/05/05
     * @throws IOException
     * @throws URISyntaxException
     * @throws SolrServerException, IOException
     */
    @RequestMapping("/phenotypes/export/{acc}") // Keep params in synch with geneVariantsWithPhenotypeTable()
    public void export(
            @PathVariable String acc,
			@RequestParam(required = true, value = "fileType") String fileType,
			@RequestParam(required = true, value = "fileName") String fileName,
			@RequestParam(required = false, value = "procedure_name")  List<String> procedureName,
			@RequestParam(required = false, value = "marker_symbol")  List<String> markerSymbol,
			@RequestParam(required = false, value = "mp_term_name")  List<String> mpTermName,			
            Model model,
            HttpServletRequest request,
			HttpServletResponse response,
            RedirectAttributes attributes) 
    throws IOException, URISyntaxException, SolrServerException {
            
        PhenotypeFacetResult phenoResult = phenotypeSummaryHelper.getPhenotypeCallByMPAccessionAndFilter(acc, procedureName, markerSymbol, mpTermName);
        List<PhenotypeCallSummaryDTO> phenotypeList = phenoResult.getPhenotypeCallSummaries();
        List<PhenotypePageTableRow> phenotypes = new ArrayList<PhenotypePageTableRow>();
        String url =  request.getAttribute("mappedHostname").toString() + request.getAttribute("baseUrl").toString();
        
        for (PhenotypeCallSummaryDTO pcs : phenotypeList) {
            List<String> sex = new ArrayList<String>();
            sex.add(pcs.getSex().toString());
            // On the phenotype pages we only display stats graphs as evidence, the MPATH links can't be linked from phen pages
            PhenotypePageTableRow pr = new PhenotypePageTableRow(pcs, url, drupalBaseUrl, false);
            phenotypes.add(pr);
        } 
        
        List<String> dataRows = new ArrayList<>();
		dataRows.add(PhenotypePageTableRow.getTabbedHeader());
		for (PhenotypePageTableRow row : phenotypes) {
			dataRows.add(row.toTabbedString());
		}

		FileExportUtils.writeOutputFile(response, dataRows, fileType, fileName);
    }
    
    @RequestMapping("/mpTree/{mpId}")
    public String getChildParentTree(
            @PathVariable String mpId,
            Model model,
            HttpServletRequest request,
            RedirectAttributes attributes) 
    throws KeyManagementException, NoSuchAlgorithmException, URISyntaxException, GenomicFeatureNotFoundException, IOException, SolrServerException {
        
    	model.addAttribute("ontId", mpId);
        return "pageTree";
    }    
    
    
    /**
     * @author ilinca
     * @since 2016/03/22
     * @param mpId
     * @param type
     * @param model
     * @return
     * @throws SolrServerException, IOException
     * @throws IOException
     * @throws URISyntaxException
     */
    @RequestMapping(value="/mpTree/json/{mpId}", method=RequestMethod.GET)	
    public @ResponseBody String getParentChildren( @PathVariable String mpId, @RequestParam(value = "type", required = true) String type, Model model) 
    throws SolrServerException, IOException , URISyntaxException {
    	
    	if (type.equals("parents")){
    	
	    	JSONObject data = new JSONObject();
	    	data.element("id", mpId);
	    	JSONArray nodes = new JSONArray();
	    
	    	for (OntologyBean term : mpService.getParents(mpId)){
	    		nodes.add(term.toJson());
	    	}

	    	data.element("children", nodes);
			return data.toString();
			
    	} else if (type.equals("children")){
    		
    		JSONObject data = new JSONObject();
        	data.element("id", mpId);
        	JSONArray nodes = new JSONArray();

        	for (OntologyBean term : mpService.getChildren(mpId)){
	    		nodes.add(term.toJson());
	    	}
        	
        	data.element("children", nodes);
    		return data.toString();
    	}
    	return "";
    }
        
    
    public JSONObject getJsonObj(String name, String type){
    	return new JSONObject().element("name", name).element(type, true);
    }
    
    


    /**
     *
     * @param mpId
     * @return List of parameters that led to an association to the given
     * phenotype term or any of it's children
     * @throws SolrServerException, IOException
     */
    public List<ParameterDTO> getParameters(String mpId) 
    throws SolrServerException, IOException {
    
    	List<String> parameters = srService.getParametersForPhenotype(mpId);
    	List<ParameterDTO> res =  new ArrayList<>();
    	
    	for (String parameterStableId : parameters){
    		ParameterDTO param = impressService.getParameterByStableId(parameterStableId);
    		if (param.getObservationType().equals(ObservationType.unidimensional)){
    			res.add(param);
    		} 
    	}
        Collections.sort(res, ImpressBaseDTO.getComparatorByNameImpcFirst());

        return res;
    }


}
