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
package org.mousephenotype.cda.solr.web.dto;

import org.mousephenotype.cda.db.pojo.DatasourceEntityId;
import org.mousephenotype.cda.db.pojo.OntologyTerm;
import org.mousephenotype.cda.enumerations.ZygosityType;
import org.mousephenotype.cda.solr.service.dto.ImageDTO;
import org.mousephenotype.cda.solr.service.dto.ImpressBaseDTO;
import org.mousephenotype.cda.solr.service.dto.MarkerBean;
import org.mousephenotype.cda.solr.web.dto.EvidenceLink.IconType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class AnatomyPageTableRow extends DataTableRow{


	String expression;
    List<OntologyTerm> anatomy;
    String anatomyLinks;
    int numberOfImages = 0;

    public AnatomyPageTableRow() {
        super();
    }


    public AnatomyPageTableRow(ImageDTO image, String maId, String baseUrl, String expressionValue) {

    	super();
        List<String> sex = new ArrayList<String>();
        sex.add(image.getSex().toString());
        MarkerBean gene = new MarkerBean();
        gene.setSymbol(image.getGeneSymbol());
        gene.setAccessionId(image.getGeneAccession());
        MarkerBean allele = new MarkerBean();
        allele.setSymbol(image.getAlleleSymbol());
        allele.setSymbol(image.getAlleleAccession());
        this.setGene(gene);
        this.setAllele(allele);
        this.setSexes(sex);
        this.setDataSourceName(image.getDataSourceName());
        this.setZygosity(image.getZygosity() != null ? ZygosityType.valueOf(image.getZygosity()) : ZygosityType.not_applicable);
        ImpressBaseDTO proc = new ImpressBaseDTO();
        proc.setName(image.getProcedureName());
        proc.setStableId(image.getProcedureStableId());
        ImpressBaseDTO param = new ImpressBaseDTO();
        param.setName(image.getParameterName());
        param.setStableId(image.getParameterStableId());
        this.setProcedure(proc);
        this.setParameter(param);
        this.setPhenotypingCenter(image.getPhenotypingCenter());

        List<OntologyTerm> anatomyTerms = new ArrayList<>();
        for (int i = 0; i < image.getMaId().size(); i++){
        	if (image.getExpression(image.getMaId().get(i)).equalsIgnoreCase(expressionValue)){
	        	OntologyTerm anatomy = new OntologyTerm();
	        	DatasourceEntityId maIdDei = new DatasourceEntityId(image.getMaId().get(i), -1);
	        	anatomy.setId(maIdDei);
	        	anatomy.setName(image.getMaTerm().get(i));
	        	anatomyTerms.add(anatomy);
        	}
        }

        this.setExpression(expressionValue);
        this.setAnatomy(anatomyTerms);

	    // Collect the parallel lists of IDs and Term names into combined parallel list of all three sets (term, intermediates, top levels)
	    List<String> maIds = Stream.of(image.getMaId(), image.getIntermediateMaId(), image.getSelectedTopLevelMaId())
		    .filter(Objects::nonNull)
		    .flatMap(Collection::stream)
		    .collect(Collectors.toList());
	    List<String> maTerms = Stream.of(image.getMaTerm(), image.getIntermediateMaTerm(), image.getSelectedTopLevelMaTerm())
		    .filter(Objects::nonNull)
		    .flatMap(Collection::stream)
		    .collect(Collectors.toList());

        this.setEvidenceLink(buildImageUrl(baseUrl, maId, maTerms.get(maIds.indexOf(maId))));
        this.setAnatomyLinks(getAnatomyWithLinks(baseUrl));
        this.numberOfImages ++;
    }


    public String getAnatomyWithLinks(String baseUrl){
    	
    	String links = "";
    	
    	if (anatomy != null && anatomy.size() > 0){
	    	links = "<a href=\"" + baseUrl + "/anatomy/";
	    	for (int i = 0; i < anatomy.size(); i++){
	    		links += anatomy.get(i).getId().getAccession() + "\">" + anatomy.get(i).getName() + "</a>";
	    		if (i != anatomy.size()-1 ){
	    			links += ", <a href=\"" + baseUrl + "/anatomy/";
	    		}
	    	}
    	}

    	return links;
    }


    public EvidenceLink buildImageUrl(String baseUrl, String maId, String maTerm){

    	String url = baseUrl + "/impcImages/images?q=*:*&defType=edismax&wt=json&fq=(";
        url += ImageDTO.MA_ID + ":\"";
        url += maId + "\" OR " + ImageDTO.SELECTED_TOP_LEVEL_MA_ID + ":\"" + maId + "\"";
        url += " OR " + ImageDTO.INTERMEDIATE_MA_ID + ":\"" + maId + "\"";

    	url += ") ";

    	if (getGene().getSymbol()!= null){
    		url += " AND " + ImageDTO.GENE_SYMBOL + ":" + this.getGene().getSymbol();
    	} else {
    		url += " AND " + ImageDTO.BIOLOGICAL_SAMPLE_GROUP + ":control";
    	}
    	if (getParameter() != null){
    		url += " AND " + ImageDTO.PARAMETER_NAME + ":\"" + getParameter().getName() + "\"";
    	}
    	url += "&title=gene " + this.getGene().getSymbol() + " in " + maTerm + "";

    	EvidenceLink link = new EvidenceLink();
    	link.setUrl(url);
    	link.setDisplay(true);
    	link.setIconType(IconType.IMAGE);
    	link.setAlt("Images");

    	return link;
    }

    public void addImage(){
    	this.numberOfImages ++;
    }

	@Override
	public int compareTo(DataTableRow o) {

		return 0;
	}

	public String getExpression() {

		return expression;
	}

	public void setExpression(String expression) {

		this.expression = expression;
	}

	public String getKey(){
		return getAllele().getSymbol() + getZygosity().name() + getParameter().getName() + getExpression();
	}

	public boolean equals(AnatomyPageTableRow obj) {
	    return this.getKey().equalsIgnoreCase(obj.getKey());
	}

	public int getNumberOfImages() {

		return numberOfImages;
	}

	public void setNumberOfImages(int numberOfImages) {

		this.numberOfImages = numberOfImages;
	}


	public void addSex(String sex){

		if (!sexes.contains(sex)){
			sexes.add(sex);
		}
	}

	public List<OntologyTerm> getAnatomy() {

		return anatomy;
	}


	public void setAnatomy(List<OntologyTerm> anatomy) {

		this.anatomy = anatomy;
	}


	public String getAnatomyLinks() {

		return anatomyLinks;
	}


	public void setAnatomyLinks(String anatomyLinks) {

		this.anatomyLinks = anatomyLinks;
	}

}
