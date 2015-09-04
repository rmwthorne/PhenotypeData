<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<%@taglib prefix="t" tagdir="/WEB-INF/tags"%>

<t:genericpage>
	<jsp:attribute name="title">International Mouse Phenotyping Consortium Documentation</jsp:attribute>
	<jsp:attribute name="breadcrumb">&nbsp;&raquo; <a href="${baseUrl}/documentation/index">Documentation</a></jsp:attribute>
	<jsp:attribute name="bodyTag"><body class="page-node searchpage one-sidebar sidebar-first small-header"></body></jsp:attribute>
	<jsp:attribute name="addToFooter"></jsp:attribute>
	<jsp:attribute name="header"></jsp:attribute>

	<jsp:body>
		
        <div id="wrapper">

            <div id="main">
                
                <!-- Sidebar First -->
                <jsp:include page="doc-menu.jsp"></jsp:include>

                <!-- Maincontent -->

                <div class="region region-content">              

                    <div class="block block-system">

                        <div id="top" class="content node">
                            
                            <h3>Explore the diverse entry points to mouse phenotype data.</h3>
							Currently, the IMPC portal supports 6 main data types on the search page:
							Genes, Phenotypes, Diseases, Anatomy, Impc Images, and legacy Images.
							These are the main facets of the IMPC facet search interface.<br>
							Each main data type has sub data types which act as subfacet filters to allow for data filtering.<p>
							
							<h4><a name="mainFaceting" href='#'>Main data type browsing</a></h4>
                            <div>Clicking on a main facet (eg, Genes, Phenotypes, Diseases) without ticking its subfacet filter(s) will display all records of that data type.
                            The screenshot below shows the total number of genes in the portal.<p>
                            <img src='img/main_data_type.png' /><p>
                            
                            <h6>Facet control and behaviors</h6>
                            <div>You are in control of whether a main facet/subfacet is open or closed.<p>
                            When a filter is checked (ie, ticking on the checkbox of a subfacet), the facet counts will change dynamically. The counts of non-matching facet filters will become zero and disabled (grayed out) and when you hover over them, a no-entry sign will appear indicating that that filter is now unclickable until it matches other filter combinations.
                            </div>
                            <img src='img/facet_behavior.png' /><p>
                            </div><br>
                            
							<h4><a name="subFaceting" href='#'>Cross data type browsing</a></h4>
                            <div>By checking a checkbox (or subfacet filter, eg. when 'Genes' main facet is expanded and 'Started' subfacet is ticked), the data of each main facet and its subfacets will be filtered and the counts of all 6 main data types
                            will be updated accordingly.<p><p>This is a powerful tool to find data in the portal.<p>
                            <h5>Some search examples:</h5>
                            <h6>(1) I want to find a list of genes whose mice phenotyping status has been marked as 'Started' and whose mice were produced (production center) and phenotyped (phenotyping center) at the WTSI.</h6>
                            First, click on the 'Genes' main facet to expand its subfacets (if not yet expanded). Then click the '<b>IMPC Phenotyping Status</b>' subfacet to expand its filters (if not yet expanded). Next check the '<b>Started</b>' subfacet filter. 
                            A '<b>phenotyping started</b>' filter will appear in the light gray filter summary box just under the big blue 'Filter your search' bar on top of the search facet interface.<p><img src='img/started_subfacet.png' />
                            <br>To further filter these set of genes for their production/phenotyping centers, click to expand the '<b>IMPC Mouse Production Center</b>' subfacet. Then check the 'WTSI' checkbox. 
                            Now a second filter '<b>mice produced at WTSI</b>' will be added to the filter summary box.<img src='img/prod_center_subfacet.png' /><p>Do the same for '<b>IMPC Mouse Phenotyping Center</b>' subfacet and check the 'WTSI' checkbox. 
                            You will see a third filter '<b>mice phenotyped at WTSI</b>' in the filter summary box.<img src='img/pheno_center_subfacet.png' /><br>
                            The resulting gene list on the right of the search page will be what you are looking for.<p><img src='img/result1.png' /><p>  
                            <h6>(2) What are the mouse genes from (1) above that have the skeleton phenotype and have OMIM phenotype annotations?</h6>
                            With the 3 filters from (1) still in place, click on the 'Phenotypes' main facet to expand its subfacets and check the '<b>skeleton</b>' subfacet filter.
                            Notice that the skeleton filter is added to the filter summary box.<p><img src='img/skeleton_subfacet.png' /><p>
                            Then click on the 'Disease' main facet and tick the '<b>OMIM</b>' checkbox from the expanded subfacets to add the 5th filter.
                            The resulting gene list on the right of the search page will be what you are looking for.<img src='img/result2.png' />
                            </div><br>
                            <h6>(3) I want to remove a filter.</h6>  
    						You can untick a checked checkbox from the subfacets or click on an individual filter in the summary filter box.<br>
    						To remove all filters in one go, just click on the 'Remove all facet filters' link at bottom of the summary filter box.<p>
    						<img src='img/remove_all_filters.png' />
    						<p>
                                  
                            <h4><a name="autosuggest_srch" href='#'>Auto-suggest Support for Keyword Search</a></h4>			
                            <div>Enter at least 3 letters to invoke a drop-down list of terms (prefixed by data type) related to your search keyword.
                            The top ten most relevant terms will be shown, in order of relevance.
                            You can select the desired term by using the UP/DOWN keys or by selecting the term with the mouse and pressing the ENTER key.
                            </div><br>			
                            <img src='img/autosuggest_search.png' /><p>      
                                                    
                            <h4><a name="quick_gene_srch" href='#'>Quick Gene Search</a></h4>			
                            <div>Enter a gene symbol, gene ID, name or human orthologue into the search box and press the ENTER key to get a list of relevant gene pages.
                                <p>Eg. search by gene symbol "mtf1":
                            </div><br>			
                            <img src='img/quick_gene_search.png' /><p>

                            <h4><a name="quick_pheno_srch" href='#'>Quick Phenotype Search</a></h4>
                            <div>Enter an abnormal phenotype or MP:ID and click on phenotype facet on the left panel to show relevant phenotype pages.
                                <p>Eg. search by phenotype "glucose":<br>
                            </div><br> 
                            <img src='img/quick_phenotype_search.png' /><p>

                            <h4><a name="quick_disease_srch" href='#'>Quick Disease Search</a></h4>
                            <div>Enter a (partial) disease name or ID (OMIM, Orphanet or DECIPHER), click on the disease facet and the the results grid will return relevant disease pages.
                                <p>Eg. Search for "cardiac" diseases:<br>
                            </div><br> 
                            <img src='img/disease_search.png' /><p>

                            <h4><a name="quick_anatomy_srch" href='#'>Quick Anatomy Search</a></h4>
                            <div>Enter an anatomical entity or MA:ID, click on the anatomy facet on the on the left panel, and the results grid will return any relevant anatomy pages.
                                <p>Eg. search by anatomy "eye":<br>
                            </div><br> 
                            <img src='img/quick_anatomy_search.png' /><p>
                        <!-- 
                            <h4><a name="quick_param_srch" href='#'>Quick Assay Search</a></h4>
                            <div>Enter an assay, parameter or IMPReSS ID, click on the procedure facet on the left panel, and the results grid will return relevant parameter pages.
                                <p>Eg. search by parameter "grip strength":<br>
                            </div><br> 
                            <img src='img/quick_param_search.png' /><p>	
                        -->

                            <h4><a name="quick_img_srch" href='#'>Quick Image Search</a></h4>
                            <div>Enter a gene, phenotype, assay, or anatomical entity, click on the Images (legacy) or IMPC images facet on the left panel and the results grid will return relevant image pages.
                                <p>By default, Annotation View will be displayed, where images are grouped by annotations.<p>Eg. search by anatomy "trunk":<br>				
                            </div><br> 
                           
							<img src='img/quick_img_search_annotView.png' /><p>
                            <p>To list annotations to an image, simply click on the "Show Image View" link in the top-right corner of the results grid. The label of the same link will then change to "Show Annotation View" so that you can toggle the views.</p><br>
                            
                                <img src='img/quick_img_search_imageView.png' /><p>

                            <h4><a name="export" href='#'>Data Export of Search Results</a></h4>
                            <div>Click on the export icon <p><img src='img/export.jpg' />
                            <br>in the top-right corner of the results grid to expand or hide it.
                                When expanded, it looks like this:<p> <img src='img/export_expanded.jpg' /><p><p>
                                    Click on either the TSV (tab separated) or the XLS (MS Excel) link for the desired report format.
                                <p>To download data for the currently displayed page only, choose the set of links under the label "Current paginated entries in table".
                                   To download the data for <i>all</i> pages, choose the set of links under the label "All entries in table".</p>
                                <br>A warning message dialog box will be displayed if the dataset is large and the download could take a long time.</div>

                           

                        </div><%-- end of content div--%>
                    </div>
                </div>
            </div>
        </div>
   
    </jsp:body>
  
</t:genericpage>
