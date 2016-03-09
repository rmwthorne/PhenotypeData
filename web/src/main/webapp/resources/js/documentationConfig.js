
/**
 * Copyright © 2011-2014 EMBL - European Bioinformatics Institute
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License.  
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * searchAndFacetConfig: definition of variables for the search and facet 
 * see searchAndFacet directory
 * 
 * Author: Chao-Kung Chen
 * 
 */

if(typeof(window.MDOC) === 'undefined') {
    window.MDOC = {};
}

MDOC.search = {
		'facetPanel'        : '<div class="briefDocCap">Browse IMPC data with facets</div>'
							+ '<ul><li>Click on a facet/subfacet to open or hide it.</li>'
							+ '    <li>Ways to display facet result:'
							+ '        <ul>'
							+ '            <li>Click on <b>checkbox</b> on the left.</li>'
							+'         </ul></li>'
							+'     <li>Click on the <b>info button</b> for detailed description.</li>'
							+ '</ul>', 
			
		'facetPanelDocUrl'   				: baseUrl + '/documentation/doc-search',
};

var docuBase = baseUrl + '/documentation/doc-explore';
MDOC.gene = {
		'detailsSection'         			: '<p>Details about the gene including: Gene name, accession IDs, location, links and a genome browser.</p><p>Click the help icon for more detail.</p>',
		'detailsSectionDocUrl'   			: docuBase + '#detailsSection',
		'phenoAssocSection'              	: '<p>Mammalian Phenotype (MP) associations made to this gene.</p><p>Click the help icon for more detail.</p>',
		'phenoAssocSectionDocUrl'        	: docuBase + '#phenoAssocSection',
		'heatmapSection'           			: '<p>Analysis of the IMPC data displayed in a heatmap.</p><p>Click the help icon for more detail.</p>',
		'heatmapSectionDocUrl'     			: docuBase + '#heatmapSection',
		'expressionSection'       		    : '<p>Expression of IMPC images associated to this gene.</p><p>Click the help icon for more detail.</p>',
		'expressionSectionDocUrl' 		    : docuBase + '#expressionSection',
		'phenoAssocImgSection'      		: '<p>Image data used by the phenotyping centers to score the presence or absence of an abnormal phenotyp</p><p>Click the help icon for more detail.</p>',
		'phenoAssocImgSectionDocUrl'		: docuBase + '#phenoAssocImgSection',
		'diseaseSection'           		    : '<p>Human disease models found to be associated with mouse phenotypes.</p><p>Click the help icon for more detail.</p>',
		'diseaseSectionDocUrl'     		    : docuBase + '#diseaseSection',
		'orderSection'          			: '<p>Ordering information for alleles and ES cells of this gene produced from the IKMC project.  When available to order a link to the correspondiong repository will be included.</p><p>Click the help icon for more detail.</p>',
		'orderSectionDocUrl'    			: docuBase + '#order',
};
MDOC.phenotypes = {
		'generalPanel'         				: "<p> Phenotype details panel.<p> <p>Click the help icon for more detail.</p>",
		'generalPanelDocUrl'   				: baseUrl + '/documentation/phenotype-help',
		'relatedMpPanel'       				: "<p>Allele associated with current phenotype. You can filter the table using the dropdown checkbox filters over the table, sort by one column and export the data. <p>Click the help icon for more detail.</p>",
		'relatedMpPanelDocUrl' 				: baseUrl + '/documentation/phenotype-help#associations',
		'phenotypeStatsPanel'  				: "<p> Find out more about how we obtain the stats and associations presented in this panel. <p>",
		'phenotypeStatsPanelDocUrl'			: baseUrl + '/documentation/phenotype-help#phenotype-stats-panel'
};
MDOC.images = {
		'generalPanel'         				: "<p>All images associated with current phenotype.</p> <p>Click the help icon for more detail.</p>",
		'generalPanelDocUrl'   				: baseUrl + '/documentation/image-help',
};

MDOC.stats = {
		'generalPanel'         				: '<p>Details about the graphs.</p> <p>Click the help icon for more detail.</p>',
		'generalPanelDocUrl'   				: baseUrl + '/documentation/graph-help'
};

MDOC.alleles = {
		'generalPanel'         				: '<p>Details about the graphs.</p> <p>Click the help icon for more detail.</p>',
		'generalPanelDocUrl'   				: baseUrl + '/documentation/graph-help'
};

MDOC.phenome = {
		'phenomePanel'         				: '<p>Details about the phenotype calls by center.</p> <p>Click the help icon for more detail.</p>',
		'phenomePanelDocUrl'   				: baseUrl + '/documentation/phenome-help'
};

MDOC.parallel = {
		'parallelPanel'         			: '<p>Select one or more procedures to be displayed in a parallel coordinates chart. You can select ranges to filter the data for multiple parameters. The values displayed are the genotype effect for each strain.</p> <p>Click the help icon for more details.</p>',
		'parallelPanelDocUrl'   			: baseUrl + '/documentation/parallel-help'
};


