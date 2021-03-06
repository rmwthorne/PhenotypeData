<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<%@taglib prefix="t" tagdir="/WEB-INF/tags"%>

<script type="text/javascript">
	${chart.getChart()}
	$(document).ready(function() {/* removing as controls center and sex for chart at bottom of phenotype page now is hardly used, can bring back if requested
console.log('overview function ready');
		var c = '${centerFilters}';
		if (c != ""){
			c = c.replace("]", "").replace("[", "");
			var centers = c.split(', ');
			var sexes = ["male" , "female"];
			var checkedCenters = [];
			var checkedSexes = [];
			
			var html = '<div class="filters"> 	<div class="filter"> 	<div class="ftype">Center</div> 	<div class="foptions">	<ul>' ;
			
			for (var i = 0; i < centers.length; i ++){
				html += '<li> <input id="' + centers[i] + '" type="checkbox" class="checkbox" array="centers"> ' +  centers[i] + '</li>';
			}
			
			html += '</ul> </div> </div> <div class="filter"> <div class="ftype">Sex</div> <div class="foptions"> <ul>';
			
			for (var i = 0; i < sexes.length; i ++){
				html += '<li> <input id="' + sexes[i] + '" type="checkbox" class="checkbox" array="sexes"> ' + sexes[i] + '</li>';
			}

			html += '</ul> </div> </div> <div class="clear"></div> </div>';
			console.log('html overview='+html);
			$("#chartFilters").html(html);
			
			// add open-close functionality
			$('.ftype').on('click',function() {
				$(this).parent('.filter').toggleClass('open'); 
			});
			
			// attach onClick functionality to the filters
			$( '.checkbox').click(function() {
				  if ($(this).is(':checked')){
						// add to array
						if ($(this).attr('array') === 'centers'){
							checkedCenters.push($(this).attr('id'));
						}
						if ($(this).attr('array') === 'sexes'){
							checkedSexes.push($(this).attr('id'));
						}
					}
				  else{
						// remove from array
						if ($(this).attr('array') === 'centers'){
							var index = checkedCenters.indexOf($(this).attr('id'));
							checkedCenters.splice(index,1);
						}
						if ($(this).attr('array') === 'sexes'){
							var index = checkedSexes.indexOf($(this).attr('id'));
							checkedSexes.splice(index,1);
						}
					}
				  
				  // in any case call ajax to filter chart  
					$( '#spinner-overview-charts' ).show();
				  var parameter = $( '#single-chart-div' ).attr('parameter');
				 	var  mp = $( '#single-chart-div' ).attr('mp');
					console.log('parameter_id='+parameter);
					console.log("request uri="+document.URL);
					var chartUrl = document.URL.split("/phenotypes/")[0];
					chartUrl += "/overviewCharts/" + mp + "?parameter_id=" + parameter;
					if (checkedCenters.length != 0) chartUrl += "&center=" + checkedCenters.join();
					if (checkedSexes.length != 0) chartUrl += "&sex=" + checkedSexes.join();
					if (centers.length != 0) chartUrl += "&all_centers=" + centers.join();
					console.log('chartUrlOverview='+chartUrl);
					$.ajax({
					  url: chartUrl,
					  cache: false
					})
					.done(function( html ) {
						$( '#spinner-overview-charts' ).hide();
					   $( '#single-chart-div' ).html( html );
					});

				});
		} */
	});