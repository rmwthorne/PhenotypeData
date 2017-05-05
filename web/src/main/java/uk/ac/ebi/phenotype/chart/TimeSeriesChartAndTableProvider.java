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
package uk.ac.ebi.phenotype.chart;

import org.apache.commons.lang.WordUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mousephenotype.cda.db.pojo.DiscreteTimePoint;
import org.mousephenotype.cda.db.pojo.Parameter;
import org.mousephenotype.cda.enumerations.SexType;
import org.mousephenotype.cda.enumerations.ZygosityType;
import org.mousephenotype.cda.solr.service.ImpressService;
import org.mousephenotype.cda.solr.service.dto.ExperimentDTO;
import org.mousephenotype.cda.solr.service.dto.ObservationDTO;
import org.mousephenotype.cda.solr.service.dto.ParameterDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;




@Service
public class TimeSeriesChartAndTableProvider {

	
	
	private final Logger logger = LoggerFactory.getLogger(this.getClass().getCanonicalName());

	public ChartData doTimeSeriesOverviewData(
			Map<String, List<DiscreteTimePoint>> lines, Parameter p) {

		if (lines == null) {
			return new ChartData();
		}

		String title = p.getName();
		// create CharData
		ChartData chartsNTablesForParameter = creatDiscretePointTimeSeriesChartOverview(
				"1", title, lines, p.checkParameterUnit(1),
				p.checkParameterUnit(2), 1, "org", p);
		return chartsNTablesForParameter;
	}



	public ChartData doTimeSeriesData(ExperimentDTO experiment,	ParameterDTO parameter, String experimentNumber)
	throws IOException,	URISyntaxException {

		ChartData chartNTableForParameter = null;
		Map<String, List<DiscreteTimePoint>> lines = new TreeMap<String, List<DiscreteTimePoint>>();

		for (SexType sex : experiment.getSexes()) {

			List<DiscreteTimePoint> controlDataPoints = new ArrayList<>();

			for (ObservationDTO control : experiment.getControls()) {
				String docGender = control.getSex();

				if (SexType.valueOf(docGender).equals(sex)) {
					Float dataPoint = control.getDataPoint();
					logger.debug("data value=" + dataPoint);
					Float discreteTimePoint = control.getDiscretePoint();
					controlDataPoints.add(new DiscreteTimePoint(discreteTimePoint, dataPoint));
				}
			}

			logger.debug("finished putting control to data points");
			TimeSeriesStats stats = new TimeSeriesStats();
			String label = ChartUtils.getLabel(null, sex);
			List<DiscreteTimePoint> controlMeans = stats.getMeanDataPoints(controlDataPoints);
			lines.put(label, controlMeans);

			for (ZygosityType zType : experiment.getZygosities()) {

				List<DiscreteTimePoint> mutantData = new ArrayList<>();
				Set<ObservationDTO> expObservationsSet = Collections.emptySet();

				if (zType.equals(ZygosityType.heterozygote)
						|| zType.equals(ZygosityType.hemizygote)) {
					expObservationsSet = experiment.getHeterozygoteMutants();
				}
				if (zType.equals(ZygosityType.homozygote)) {
					expObservationsSet = experiment.getHomozygoteMutants();
				}

				for (ObservationDTO expDto : expObservationsSet) {
					// get the attributes of this data point
					String docGender = expDto.getSex();
					if (SexType.valueOf(docGender).equals(sex)) {
						Float dataPoint = expDto.getDataPoint();
						logger.debug("data value=" + dataPoint);
						Float discreateTimePoint = expDto.getDiscretePoint();
						mutantData.add(new DiscreteTimePoint(discreateTimePoint, new Float(dataPoint)));
					}
				}

				logger.debug("doing mutant data");
				List<DiscreteTimePoint> mutantMeans = stats.getMeanDataPoints(mutantData);
				label = ChartUtils.getLabel(zType, sex);
				lines.put(label, mutantMeans);
			}
		}

		String title = "Mean " + parameter.getName();
		if (lines.size() > 1) {
			// if lines are greater than one i.e. more than just control create charts and tables
			System.out.println("Body weight curve?"+ parameter.getName()+" xUnit="+ parameter.getUnitX()+" yUnit="+parameter.getUnitY());
			String xAxisLabel =parameter.getUnitX();
			String yAxisLabel=parameter.getUnitY();
			if(parameter.getStableId().equals("IMPC_BWT_008_001")){
				xAxisLabel= "Age - rounded to nearest week";
				yAxisLabel= "Mass (g)";
			}
			System.out.println("xAxis label="+xAxisLabel);
			int decimalPlaces = ChartUtils.getDecimalPlaces(experiment);
			chartNTableForParameter = creatDiscretePointTimeSeriesChart(
					experimentNumber, title, lines, xAxisLabel,
					yAxisLabel, decimalPlaces,
					experiment.getOrganisation(), parameter);
			chartNTableForParameter.setExperiment(experiment);
		}

		chartNTableForParameter.setLines(lines);

		return chartNTableForParameter;

	}


	/**
	 * Creates a single chart and adds it to the chart array that is then added
	 * to the model by the main method
	 *
	 * @param title
	 * @param lines
	 * @param xUnitsLabel
	 * @param yUnitsLabel
	 * @param organisation
	 * @param parameter
	 *
	 * @return
	 */
	private ChartData creatDiscretePointTimeSeriesChart(String expNumber,
			String title, Map<String, List<DiscreteTimePoint>> lines,
			String xUnitsLabel, String yUnitsLabel, int decimalPlaces, String organisation, ParameterDTO parameter) {

		JSONArray series = new JSONArray();
		String seriesString = "";
		Set<Float> categoriesSet = new HashSet<Float>();
		String mColor = ChartColors.getMutantColor(ChartColors.alphaTranslucid70).replaceAll("'", "");
		String wtColor = ChartColors.getWTColor(ChartColors.alphaTranslucid70).replaceAll("'", "");

		try {
			for (String key : lines.keySet()) {// key is control hom or het
				String color = mColor;
				JSONObject object = new JSONObject();
				JSONArray data = new JSONArray();
				object.put("name", key);

				if (key.contains("WT")){
					color = wtColor;
				}

				JSONObject errorBarsObject = null;

				try {
					errorBarsObject = new JSONObject();// "{ name: 'Confidence', type: 'errorbar', color: 'black', data: [ [7.5, 8.5], [2.8, 4], [1.5, 2.5], [3, 4.1], [6.5, 7.5], [3.3, 4.1], [4.8, 5.1], [2.2, 3.0], [5.1, 8] ] } ");
					errorBarsObject.put("name", "Standard Deviation");
					errorBarsObject.put("type", "errorbar");
					errorBarsObject.put("color", color);

				} catch (JSONException e) {
					e.printStackTrace();
				}

				JSONArray errorsDataJson = new JSONArray();
				for (DiscreteTimePoint pt : lines.get(key)) {

					JSONArray pair = new JSONArray();
					pair.put(pt.getDiscreteTime());
					pair.put(pt.getData());
					categoriesSet.add(pt.getDiscreteTime());
					data.put(pair);
					// set the error bars
					JSONArray errorBarsJ = new JSONArray();
					errorBarsJ.put(pt.getDiscreteTime());
					errorBarsJ.put(pt.getErrorPair().get(0));
					errorBarsJ.put(pt.getErrorPair().get(1));
					errorsDataJson.put(errorBarsJ);

					errorBarsObject.put("data", errorsDataJson);

				}
				object.put("data", data);
				object.put("color", color);
				// add a placeholder string so we can add a tooltip method
				// specifically for data that is not error bars later on in this
				// code
				String placeholderString = "placeholder";
				object.put(placeholderString, placeholderString);
				series.put(object);
				series.put(errorBarsObject);
			}

		} catch (JSONException e) {
			e.printStackTrace();
		}

		// need to add error bars to series data as well!
		// sort the categories by time as means are all sorted already

		List<Float> cats = new ArrayList<Float>();
		for (Float cat : categoriesSet) {
			cats.add(cat);
		}
		Collections.sort(cats);
		String noDecimalsString = "";
		if (xUnitsLabel.equals("number")) {
			// set the xAxis to be numbers with no decimals
			noDecimalsString = "allowDecimals:false,";
		}

		String decimalFormatString = ":." + decimalPlaces + "f";
		String headerFormatString = "headerFormat: '<span style=\"font-size: 12px\">"
				+ WordUtils.capitalize(xUnitsLabel)
				+ " {point.key}</span><br/>',";
		String pointToolTip = "tooltip: { "
				+ headerFormatString
				+ "pointFormat: '<span style=\"font-weight: bold; color: {series.color}\">{series.name}</span>:<b>{point.y"
				+ decimalFormatString + "}" + yUnitsLabel + "</b> '}";
		String escapedPlaceholder = "\"placeholder\":\"placeholder\"";
		seriesString = series.toString().replace(escapedPlaceholder, pointToolTip);
		String errorBarsToolTip = "tooltip: { pointFormat: 'SD: {point.low"
				+ decimalFormatString + "}-{point.high" + decimalFormatString
				+ "}<br/>' }";
		String escapedErrorString = "\"errorbar\"";
		seriesString = seriesString.replace(escapedErrorString,
				escapedErrorString + "," + errorBarsToolTip);
		String axisFontSize = "15";


		String javascript = "$(document).ready(function() { chart = new Highcharts.Chart({ "
		//		+" colors:" + colors + ", "
				+" chart: {  zoomType: 'x', renderTo: 'timechart"
				+ expNumber
				+ "', type: 'line' }, title: { text: '"
				+ ""				+ "', x: -20  }, credits: { enabled: false },  subtitle: { text: '"
				+ ""
				+ "', x: -20 }, xAxis: { "
				+ noDecimalsString
				+ " labels: { style:{ fontSize:"
				+ axisFontSize
				+ " }},   title: {   text: '"
				+ xUnitsLabel
			//	+ "Age - rounded to nearest week"
				+ "'   }  }, yAxis: { labels: { style:{ fontSize:"
				+ axisFontSize
				+ " }}, title: { text: ' "
				+ yUnitsLabel
				+ "' }, plotLines: [{ value: 0, width: 1, color: '#808080' }] }, "
				+ "tooltip: {shared: true},"
				+ "series: "
				+ seriesString
				+ " }); });  ";
		ChartData chartAndTable = new ChartData();
		chartAndTable.setTitle(title);
		String parameterLink = "";
		if (parameter != null) {
			parameterLink = "<a href=\""+ImpressService.getParameterUrl(parameter.getStableKey()).toString()+"\">"+parameter.getStableId()+"</a>";
		}
		chartAndTable.setSubTitle(parameterLink);
		chartAndTable.setChart(javascript);
		chartAndTable.setOrganisation(organisation);
		chartAndTable.setId(expNumber);
		return chartAndTable;
	}

	private ChartData creatDiscretePointTimeSeriesChartOverview(String expNumber,
			String title, Map<String, List<DiscreteTimePoint>> lines,
			String xUnitsLabel, String yUnitsLabel, int decimalPlaces, String organisation, Parameter parameter) {

		JSONArray series = new JSONArray();
		String seriesString = "";
		Set<Float> categoriesSet = new HashSet<Float>();
		
		try {
			int i = 0;
			for (String key : lines.keySet()) {// key is line name or "Control"

				JSONObject object = new JSONObject();
				JSONArray data = new JSONArray();
				object.put("name", key);

				String colorString;
				if (key.equalsIgnoreCase("Control")){
					colorString = ChartColors.getDefaultControlColor(ChartColors.alphaTranslucid70);
				}
				else {
					colorString = ChartColors.getRgbaString(SexType.male, i, ChartColors.alphaTranslucid70);
				}
				object.put("color", colorString);

				JSONObject errorBarsObject = null;
				try {
					errorBarsObject = new JSONObject();
					errorBarsObject.put("name", "Standard Deviation");
					errorBarsObject.put("type", "errorbar");
					errorBarsObject.put("color", colorString);

				} catch (JSONException e) {
					e.printStackTrace();
				}

				JSONArray errorsDataJson = new JSONArray();
				for (DiscreteTimePoint pt : lines.get(key)) {
					JSONArray pair = new JSONArray();
					pair.put(pt.getDiscreteTime());
					pair.put(pt.getData());
					categoriesSet.add(pt.getDiscreteTime());
					data.put(pair);
					// set the error bars
					JSONArray errorBarsJ = new JSONArray();
					errorBarsJ.put(pt.getDiscreteTime());
					errorBarsJ.put(pt.getErrorPair().get(0));
					errorBarsJ.put(pt.getErrorPair().get(1));
					errorsDataJson.put(errorBarsJ);

					errorBarsObject.put("data", errorsDataJson);

				}
				object.put("data", data);
				String placeholderString = "placeholder";
				object.put(placeholderString, placeholderString);
				series.put(object);
				series.put(errorBarsObject);
				i++;
			}

		} catch (JSONException e) {
			e.printStackTrace();
		}

		// need to add error bars to series data as well!
		// sort the categories by time as means are all sorted already

		List<Float> cats = new ArrayList<Float>();
		for (Float cat : categoriesSet) {
			cats.add(cat);
		}
		Collections.sort(cats);
		String noDecimalsString = "";
		if (xUnitsLabel.equals("number")) {
			// set the xAxis to be numbers with no decimals
			noDecimalsString = "allowDecimals:false,";
		}

		String decimalFormatString = ":." + decimalPlaces + "f";
		String headerFormatString = "headerFormat: '<span style=\"font-size: 12px\">"
				+ WordUtils.capitalize(xUnitsLabel)
				+ " {point.key}</span><br/>',";
		String pointToolTip = "tooltip: { "
				+ headerFormatString
				+ "pointFormat: '<span style=\"font-weight: bold; color: {series.color}\">{series.name}</span>:<b>{point.y"
				+ decimalFormatString + "}" + yUnitsLabel + "</b> '}";
		String escapedPlaceholder = "\"placeholder\":\"placeholder\"";
		seriesString = series.toString().replace(escapedPlaceholder,
				pointToolTip);

		String errorBarsToolTip = "tooltip: { pointFormat: 'SD: {point.low"
				+ decimalFormatString + "}-{point.high" + decimalFormatString
				+ "}<br/>' }";
		String escapedErrorString = "\"errorbar\"";
		seriesString = seriesString.replace(escapedErrorString,
				escapedErrorString + "," + errorBarsToolTip);
		String axisFontSize = "15";

		List<String> colors=ChartColors.getFemaleMaleColorsRgba(ChartColors.alphaTranslucid70);
//		JSONArray colorArray = new JSONArray(colors);

		String javascript = "$(document).ready(function() { chart = new Highcharts.Chart({ "
//				+" colors:"+colorArray
				+" chart: {  zoomType: 'x', renderTo: 'single-chart-div', type: 'line', marginRight: 130, marginBottom: 50 }, title: { text: '"
				+ WordUtils.capitalize(title)
				+ "', x: -20  }, credits: { enabled: false },  subtitle: { text: '"
				+ parameter.getStableId()
				+ "', x: -20 }, xAxis: { "
				+ noDecimalsString
				+ " labels: { style:{ fontSize:"
				+ axisFontSize
				+ " }},   title: {   text: '"
				+ xUnitsLabel
				+ "'   }  }, yAxis: { labels: { style:{ fontSize:"
				+ axisFontSize
				+ " }}, title: { text: ' "
				+ yUnitsLabel
				+ "' }, plotLines: [{ value: 0, width: 1, color: '#808080' }] },  legend: { layout: 'vertical', align: 'right', verticalAlign: 'top', x: -10, y: 100, borderWidth: 0 }, "
				+ "tooltip: {shared: true},"
				+ "series: "
				+ seriesString
				+ " }); });  ";
		ChartData chartAndTable = new ChartData();
		chartAndTable.setChart(javascript);
		chartAndTable.setOrganisation(organisation);
		chartAndTable.setId(parameter.getStableId());
		return chartAndTable;
	}

}
