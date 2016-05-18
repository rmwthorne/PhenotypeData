<%@ page language="java" contentType="text/html; charset=US-ASCII"
    pageEncoding="US-ASCII"%>
    <%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<%@taglib prefix="t" tagdir="/WEB-INF/tags"%>
<t:genericpage>
<jsp:attribute name="breadcrumb">&nbsp;&raquo;<a href='${baseUrl}/genes/${gene.mgiAccessionId}'>${gene.markerSymbol}</a>&nbsp;&raquo; Image Comparator</jsp:attribute>


 <jsp:attribute name="title">Image Picker</jsp:attribute>
<jsp:attribute name="header">
 <!--  <link rel="stylesheet" type="text/css" href="css/bootstrap.css">
  <link rel="stylesheet" type="text/css" href="css/bootstrap-responsive.css">
  <link rel="stylesheet" type="text/css" href="examples.css">
  <link rel="stylesheet" type="text/css" href="image-picker/image-picker.css"> -->
  
  <link href="${baseUrl}/css/default.css" rel="stylesheet" type="text/css" />
 <link href="${baseUrl}/css/comparator/comparator.css" rel="stylesheet" type="text/css" />
<script src="//ajax.googleapis.com/ajax/libs/jquery/1.10.2/jquery.min.js"></script>
<script src="//ajax.googleapis.com/ajax/libs/jqueryui/1.10.3/jquery-ui.min.js"></script>
<script type='text/javascript' src="${baseUrl}/js/comparator/comparator.js?v=${version}"></script>
 
 
</jsp:attribute>
<jsp:body>

<!-- <div class="region region-content">
	<div class="block">
    	<div class="content">
        	<div class="node"> -->
            <form>
           
	            <div id="comparator" class="section">
	            	<div id="filters">Filter Images: 
	            	<select>
            			<option>Males</option>
            			<option>Females</option>
            		</select>
             		<select>
            			<option>Het</option>
            			<option>Hom</option>
            		</select>
            		</div>
	            	<div id="control_box" class=box>
		            	<c:choose>
			            	<c:when test="${not empty controls}">
			            	
			            		<iframe id="control_frame"
												src="http://www.ebi.ac.uk/mi/media/omero/webgateway/img_detail/${controls[0].omero_id }"></iframe>
							</c:when>
							<c:otherwise>
								No Image for Controls Selected
							</c:otherwise>
						</c:choose>
	            
	            		<div id="control_annotation" class="annotation">
	            			control annotations go here
	            		</div>
	            		<div class="picker">
	            			<c:forEach var="img" items="${controls}">
	            			<div class="clickbox">
	            				<img id="${img.omero_id}" src="${fn:replace(img.jpeg_url, 'image','thumbnail')}/70" class="clickable_image_control" title="${img.sex}">
	            			</div>
	            			</c:forEach>
	            		</div>
	            	
	            	</div>
	            
	            	
	            	
	            	<div id="mutant_box" class=box>
	            	<c:choose>
			            	<c:when test="${not empty mutants}">
								<iframe id="mutant_frame"
										src="http://www.ebi.ac.uk/mi/media/omero/webgateway/img_detail/${mutants[0].omero_id }"></iframe>
							</c:when>
							<c:otherwise>
								No Image for Mutants Selected
							</c:otherwise>
					</c:choose>
	            		<div id="mutant_annotation" class="annotation">
	            		mutant annotations go here
	            		</div>
	            		<div class="picker">
	            			<c:forEach var="img" items="${mutants}">
	            			<div class="clickbox">
	            				<img id="${img.omero_id}" src="${fn:replace(img.jpeg_url, 'image','thumbnail')}/70" class="clickable_image_mutant" title="${img.sex}">
	            			</div>
	            			</c:forEach> 
	            		</div>
	            	
	            	</div>
	            </div>
            </form>
            <!-- </div>




		
			</div>
 		</div>
	</div> -->

</jsp:body>
</t:genericpage>