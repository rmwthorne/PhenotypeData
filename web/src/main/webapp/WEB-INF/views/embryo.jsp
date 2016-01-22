<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@taglib prefix="t" tagdir="/WEB-INF/tags" %>

<t:genericpage>

    <jsp:attribute name="title">IMPC Embryo Landing Page</jsp:attribute>
	<jsp:attribute name="bodyTag">
		<body class="gene-node no-sidebars small-header">
	</jsp:attribute>          



	<jsp:attribute name="header">
		<script type='text/javascript' src='${baseUrl}/js/charts/highcharts.js?v=${version}'></script>
        <script type='text/javascript' src='${baseUrl}/js/charts/highcharts-more.js?v=${version}'></script>
        <script type='text/javascript' src='${baseUrl}/js/charts/exporting.js?v=${version}'></script>
        <script type='text/javascript' src='${baseUrl}/js/slider.js?v=${version}'></script> 
        
        <style type="text/css">
        	#slider {
			  position: relative;
			  overflow: hidden;
			  margin: 20px auto 0 auto;
			  border-radius: 4px;
			}
			
			#slider ul {
			  position: relative;
			  margin: 0;
			  padding: 0;
			  list-style: none;
			}
			
			#slider ul li {
			  position: relative;
			  display: block;
			  float: left;
			  margin: 0;
			  padding: 0;
			  width: 800px;
			  height: 500px;
			  background: #DDD;
			}
						 
			 .slider img{
			 	max-width:100%;
			 	max-height:80%;
			 	margin-left: auto;
    			margin-right: auto;
    			display: block;
			 }
			 
			 .slider p{
			 	z-index: 1000;
			 	padding-left:10%;
			 	padding-right:10%;
			 }
			 
			.sliderControl ul{
				overflow-x:auto;
				display: block;
			}
			
			.sliderControl ul li {
				display:inline;
				width:100px;
				float:left;
			}
			
			.sliderControl img{
				max-height:50px;
			}
			
			.sliderControl .caption{
				display:block;
			}
			
			.control_prev, .control_next {
			  position: absolute;
			  top: 40%;
			  z-index: 999;
			  display: block;
			  padding: 4% 3%;
			  width: auto;
			  height: auto;
			  background: #2a2a2a;
			  color: #fff;
			  text-decoration: none;
			  font-weight: 600;
			  font-size: 18px;
			  opacity: 0.8;
			  cursor: pointer;
			}
			
			.control_prev:hover, div.control_next:hover {
			  opacity: 1;
			  -webkit-transition: all 0.2s ease;
			}
			
			.control_prev {
			  border-radius: 0 2px 2px 0;
			  opacity: 0.5;
			}
			
			.control_next {
			  right: 0;
			  opacity: 0.5;
			  border-radius: 2px 0 0 2px;
			}
			
			.nav-dots .nav-dot {
				top: -5px;
				width: 11px;
				height: 11px;
				margin: 0 4px;
				position: relative;
				border-radius: 100%;
				display: inline-block;
				background-color: rgba(0, 0, 0, 0.6);
			}
			
			.nav-dots .nav-dot:hover {
				cursor: pointer;
				background-color: rgba(0, 0, 0, 0.8);
			}
						
        </style>
        
    </jsp:attribute>

    <jsp:body>
        <div class="region region-content">
            <div class="block">
                <div class="content">
                    <div class="node node-gene">
                        <h1 class="title" id="top">IMPC Embryo Data </h1>

                        <div class="section">
                            <div class="inner">
                            	<h2>IMPC Viability</h2>
                            	<div id="viabilityChart" class="half right">
				            		<script type="text/javascript">${viabilityChart}</script>
								</div>
								<div id="viabilityChart" class="half right">
				            		<table> 
				            		<thead>				            		
				            			<tr> <th class="headerSort"> Category </th> <th> # Genes </th> <th> Download</th>  </tr>
				            		</thead>
				            		<tbody>
				            		<c:forEach var="key" items="${viabilityTable.keySet()}">
					            		<tr>
					            			<td><h4 class="capitalize">${key}</h4></td>
					            			<td><h4>${viabilityTable.get(key)}</h4></td> 
					            			<td>
					            				<c:choose>
									            	<c:when test='${key.equalsIgnoreCase("All")}'>
									            		<a href="ftp://ftp.ebi.ac.uk/pub/databases/impc/latest/reports/viabilityReport.csv" style="text-decoration:none;"> <i class="fa fa-download" alt="Download"></i></a>
									            	</c:when>
									            	<c:otherwise>
														<a href="" style="text-decoration:none;"> <i class="fa fa-download" alt="Download"></i></a>
									            	</c:otherwise>
									           </c:choose>
					            			</td>					            					
					            		</tr>
									</c:forEach>
				            		</tbody></table>
								</div>
								<div class="clear"> </div>								
	                           
                                                        	
                            </div>
                        </div>

        				<div class="section">

                            <h2 class="title"> Vignettes </h2>
                            <div class="inner">
								<div id="sliderDiv">
									<div id="slider" class="slider">
									  <ul>
									    <li>  <img src="${baseUrl}/img/vignettes/Chtop.png" />
									    	<p class="caption"> CHTOP has been shown to recruit the histone-methylating methylosome to genomic regions containing 5-Hydroxymethylcytosine, thus affecting gene expression.  Chtop mutants showed complete preweaning lethality with no homozygous pups observed.  High resolution episcopic microscopy (HREM) imaging, revealed decreased number of vertebrae, abnormal joint morphology and edema </p></li>
									    <li><img src="${baseUrl}/img/vignettes/Rab34.png" /> 
									    	<p class="caption"> Paralog of Rab23, a paralog of Rab23, which is a key component of hedgehog signalling. Homozygous E15.5 mutant embryos have the following phenotypes, consistent with a role in hedgehog signalling. </p> </li>
									    <li><img src="${baseUrl}/img/vignettes/Gyg.png" /> 
									    	<p class="caption">Glycogenin 1 is involved in glycogen biosynthesis. Recently a novel human mutation Gyg was shown to be associated with skeletal myopathy.</p></li>
									     <li><img src="http://www.ebi.ac.uk/mi/media/omero/webgateway/render_image/140715/" />	</li>
									    <li><img src="http://www.ebi.ac.uk/mi/media/omero/webgateway/render_image/140711/" /> 
									    	<p class="caption"> This is interesting because see here and here and here and here </p> </li>
									     </ul>  
									  <div> 
										  <span class="control_next half left">></span>
										  <span class="control_prev half right"><</span>
									  </div>
									</div>
									<div class="clear"> </div>
									
									<ul id="navdots">
									<li class="nav-dots">
								      <label  class="nav-dot" ></label>
								      <label  class="nav-dot" ></label>
								      <label  class="nav-dot" ></label>
								      <label  class="nav-dot" ></label>
								      <label  class="nav-dot" ></label>
								      <label  class="nav-dot" ></label>
								    </li>
								    </ul>
									<div class="clear"> </div>
	                           	</div>
                            </div>

                        </div>

                        <div class="section">

                            <h2 class="title"> 3D Imaging </h2>
                            <div class="inner">
                            	<div>
                            		<c:forEach  var="gene" items="${genesWithEmbryoViewer}">
                            			<a class="btn" href="${drupalBaseUrl}/embryoviewer?mgi=${gene.mgiAccessionId}" style="margin: 10px">${gene.markerSymbol}</a>
                            		</c:forEach>
                            	</div>                              	
                            </div>

                        </div>
                        
                         <div class="section">
							<h2 class="title ">IMPC Embryonic Pipeline</h2>
                            <div class="inner">
	                        	<div><img src="${baseUrl}/img/embryo_impress.png"/></div>
                            </div>

                        </div>
                        
                      </div>
                    <!--end of node wrapper should be after all secions  -->
                </div>
            </div>
        </div>


	

      </jsp:body>

</t:genericpage>
