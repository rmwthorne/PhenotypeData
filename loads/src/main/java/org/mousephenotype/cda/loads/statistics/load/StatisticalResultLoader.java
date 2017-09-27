package org.mousephenotype.cda.loads.statistics.load;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.mousephenotype.cda.db.pojo.OntologyTerm;
import org.mousephenotype.cda.enumerations.BatchClassification;
import org.mousephenotype.cda.enumerations.ControlStrategy;
import org.mousephenotype.cda.enumerations.SexType;
import org.mousephenotype.cda.enumerations.ZygosityType;
import org.mousephenotype.cda.solr.service.BasicService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.util.Assert;

import javax.inject.Inject;
import javax.inject.Named;
import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

@SpringBootApplication
@Import(value = {StatisticalResultLoaderConfig.class})
public class StatisticalResultLoader extends BasicService implements CommandLineRunner {

    final private Logger logger = LoggerFactory.getLogger(getClass());

    final private DataSource komp2DataSource;
    final private MpTermService mpTermService;


    Map<String, NameIdDTO> organisationMap = new HashMap<>();
    Map<String, NameIdDTO> pipelineMap = new HashMap<>();
    Map<String, NameIdDTO> procedureMap = new HashMap<>();
    Map<String, NameIdDTO> parameterMap = new HashMap<>();

    Map<String, Integer> datasourceMap = new HashMap<>();
    Map<String, Integer> projectMap = new HashMap<>();
    Map<String, String> colonyAlleleMap = new HashMap<>();
    Map<String, Map<ZygosityType, Integer>> bioModelMap = new HashMap<>();
    Map<Integer, String> bioModelStrainMap = new HashMap<>();
    Map<String, Integer> controlBioModelMap = new HashMap<>();



    void populateControlBioModelMap() throws SQLException {
        Map<String, Integer> map = controlBioModelMap;

        String query = "SELECT  * FROM biological_model bm " +
                "INNER JOIN biological_model_strain bmstrain ON bmstrain.biological_model_id=bm.id " +
                "INNER JOIN strain ON strain.acc=bmstrain.strain_acc " +
                "WHERE allelic_composition = '' " ;

        try (Connection connection = komp2DataSource.getConnection(); PreparedStatement p = connection.prepareStatement(query)) {
            ResultSet r = p.executeQuery();
            while (r.next()) {

                String background = r.getString("name");
                Integer modelId = r.getInt("biological_model_id");

                map.put(background, modelId);
            }
        }

        logger.info(" Mapped {} biological model entries", map.size());
    }

    void populateBioModelMap() throws SQLException {
        Map<String, Map<ZygosityType, Integer>> map = bioModelMap;

        String query = "SELECT DISTINCT colony_id, ls.zygosity, bm.id as biological_model_id, strain.name " +
                "FROM live_sample ls " +
                "INNER JOIN biological_sample bs ON ls.id=bs.id " +
                "INNER JOIN biological_model_sample bms ON bms.biological_sample_id=ls.id " +
                "INNER JOIN biological_model_strain bmstrain ON bms.biological_model_id=bms.biological_model_id " +
                "INNER JOIN strain ON strain.acc=bmstrain.strain_acc " +
                "INNER JOIN biological_model bm ON (bm.id=bms.biological_model_id AND bm.zygosity=ls.zygosity) " ;

        try (Connection connection = komp2DataSource.getConnection(); PreparedStatement p = connection.prepareStatement(query)) {
            ResultSet r = p.executeQuery();
            while (r.next()) {

                String colonyId = r.getString("colony_id");
                ZygosityType zyg = ZygosityType.valueOf(r.getString("zygosity"));
                Integer modelId = r.getInt("biological_model_id");
                String strain = r.getString("name");

                bioModelStrainMap.put(modelId, strain);

                map.putIfAbsent(colonyId, new HashMap<>());
                map.get(colonyId).put(zyg, modelId);
            }
        }

        logger.info(" Mapped {} biological model entries", map.size());
    }

    void populateColonyAlleleMap() throws SQLException {
        Map map = colonyAlleleMap;

        String query = "SELECT DISTINCT colony_id, allele_acc " +
                "FROM live_sample ls " +
                "INNER JOIN biological_model_sample bms ON bms.biological_sample_id=ls.id " +
                "INNER JOIN biological_model_allele bma ON bma.biological_model_id=bms.biological_model_id " +
                "INNER JOIN allele a ON a.acc=bma.allele_acc " ;

        try (Connection connection = komp2DataSource.getConnection(); PreparedStatement p = connection.prepareStatement(query)) {
            ResultSet r = p.executeQuery();
            while (r.next()) {
                map.put(r.getString("colony_id"), r.getString("allele_acc"));
            }
        }

        logger.info(" Mapped {} datasource entries", map.size());
    }

    void populateDatasourceMap() throws SQLException {
        Map map = datasourceMap;

        String query = "SELECT * FROM external_db";

        try (Connection connection = komp2DataSource.getConnection(); PreparedStatement p = connection.prepareStatement(query)) {
            ResultSet r = p.executeQuery();
            while (r.next()) {
                map.put(r.getString("short_name"), r.getInt("id"));
            }
        }

        logger.info(" Mapped {} datasource entries", map.size());
    }

    void populateProjectMap() throws SQLException {
        Map map = projectMap;

        String query = "SELECT * FROM project";

        try (Connection connection = komp2DataSource.getConnection(); PreparedStatement p = connection.prepareStatement(query)) {
            ResultSet r = p.executeQuery();
            while (r.next()) {
                map.put(r.getString("name"), r.getInt("id"));
            }
        }

        logger.info(" Mapped {} project entries", map.size());
    }



    void populateOrganisationMap() throws SQLException {
        Map map = organisationMap;

        // Populate the organisation map with this query
        String query = "SELECT * FROM organisation";

        try (Connection connection = komp2DataSource.getConnection(); PreparedStatement p = connection.prepareStatement(query)) {
            ResultSet r = p.executeQuery();
            while (r.next()) {
                map.put(r.getString("name"), new NameIdDTO(r.getInt("id"), r.getString("name")));
            }
        }

        logger.info(" Mapped {} organisation entries", map.size());
    }

    void populatePipelineMap() throws SQLException {
        Map map = pipelineMap;

        String query = "SELECT * FROM phenotype_pipeline";

        try (Connection connection = komp2DataSource.getConnection(); PreparedStatement p = connection.prepareStatement(query)) {
            ResultSet r = p.executeQuery();
            while (r.next()) {
                map.put(r.getString("stable_id"), new NameIdDTO(r.getInt("id"), r.getString("name"), r.getString("stable_id")));
            }
        }

        logger.info(" Mapped {} pipeline entries", map.size());
    }

    /**
     * Lookup procedure by procedure group (split by "_" chop the last element)
     * @throws SQLException
     */
    void populateProcedureMap() throws SQLException {
        Map map = procedureMap;

        // Order by ID en
        String query = "SELECT * FROM phenotype_procedure ORDER BY id";

        try (Connection connection = komp2DataSource.getConnection(); PreparedStatement p = connection.prepareStatement(query)) {
            ResultSet r = p.executeQuery();
            while (r.next()) {
                String procGroup = r.getString("stable_id");
                procGroup = StringUtils.join(ArrayUtils.subarray(procGroup.split("_"), 0, 2), "_");
                map.put(procGroup, new NameIdDTO(r.getInt("id"), r.getString("name"), r.getString("stable_id")));
            }
        }

        logger.info(" Mapped {} procedure entries", map.size());
    }

    void populateParameterMap() throws SQLException {
        Map map = parameterMap;

        String query = "SELECT * FROM phenotype_parameter";

        try (Connection connection = komp2DataSource.getConnection(); PreparedStatement p = connection.prepareStatement(query)) {
            ResultSet r = p.executeQuery();
            while (r.next()) {
                map.put(r.getString("stable_id"), new NameIdDTO(r.getInt("id"), r.getString("name"), r.getString("stable_id")));
            }
        }

        logger.info(" Mapped {} parameter entries", map.size());
    }



    private String fileLocation;

    @Inject
    public StatisticalResultLoader(@Named("komp2DataSource") DataSource komp2DataSource, MpTermService mpTermService) {
        Assert.notNull(komp2DataSource, "Komp2 datasource cannot be null");
        Assert.notNull(mpTermService, "mpTermService cannot be null");
        this.komp2DataSource = komp2DataSource;
        this.mpTermService = mpTermService;
    }



    private boolean isValidInt(String str) {
        if (str == null) {
            return false;
        }
        int length = str.length();
        if (length == 0) {
            return false;
        }
        int i = 0;
        if (str.charAt(0) == '-') {
            if (length == 1) {
                return false;
            }
            i = 1;
        }
        for (; i < length; i++) {
            char c = str.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }

    private Double getDoubleField(String str) {
        return (NumberUtils.isNumber(str)) ? Double.parseDouble(str) : null;
    }

    private Integer getIntegerField(String str) {
        if (str == null) {
            return null;
        }
        int length = str.length();
        if (length == 0) {
            return null;
        }
        int i = 0;
        if (str.charAt(0) == '-') {
            if (length == 1) {
                return null;
            }
            i = 1;
        }
        for (; i < length; i++) {
            char c = str.charAt(i);
            if (c < '0' || c > '9') {
                return null;
            }
        }

        return Integer.parseInt(str);
    }

    private String getStringField(String str) {

        if (str.isEmpty() || str.equals("NA")) {
            return "";
        }

        return str;
    }


    // Will return true only when str is TRUE or true
    private Boolean getBooleanField(String str) {
        return Boolean.parseBoolean(str);
    }


    /**
     * Process a string from a results file into a LineStatisticalResult object
     *
     * field list
     0	metadata_group
     1	zygosity
     2	colony_id
     3	depvar
     4	status
     5	code
     6	count cm
     7	count cf
     8	count mm
     9	count mf
     10	mean cm
     11	mean cf
     12	mean mm
     13	mean mf
     14	control_strategy
     15	workflow
     16	weight available
     17	Method
     18	Dependent variable
     19	Batch included
     20	Residual variances homogeneity
     21	Genotype contribution
     22	Genotype estimate
     23	Genotype standard error
     24	Genotype p-Val
     25	Genotype percentage change
     26	Sex estimate
     27	Sex standard error
     28	Sex p-val
     29	Weight estimate
     30	Weight standard error
     31	Weight p-val
     32	Gp1 genotype
     33	Gp1 Residuals normality test
     34	Gp2 genotype
     35	Gp2 Residuals normality test
     36	Blups test
     37	Rotated residuals normality test
     38	Intercept estimate
     39	Intercept standard error
     40	Interaction included
     41	Interaction p-val
     42	Sex FvKO estimate
     43	Sex FvKO standard error
     44	Sex FvKO p-val
     45	Sex MvKO estimate
     46	Sex MvKO standard error
     47	Sex MvKO p-val
     48	Classification tag
     49	Additional information
     * @param data a line from a statistical results file
     * @throws IOException
     */
    LineStatisticalResult getResult(String data, String filename) {

        if (data.contains("metadata_group")) {
            // This is a header
            return null;
        }

        LineStatisticalResult result = new LineStatisticalResult();


        // Several attributes come from the filename
        filename =  filename.substring(0, filename.indexOf(".tsv"));
        List<String> fileMetaData = Arrays.asList(filename.trim().split("::"));

        if ( ! filename.contains("::")) {
            String dataString = filename
                    .replaceAll("M-G-P", "M:G:P")
                    .replaceAll("NULL-", "NULL:")
                    .replaceAll("IMPC-CURATE-", "IMPC:CURATE:")
                    ;
            fileMetaData = Arrays.stream(
                    dataString
                            .trim()
                            .split("-"))
                    .map(x -> x.replaceAll("M:G:P", "M-G-P")
                            .replaceAll("NULL:", "NULL-")
                            .replaceAll("IMPC:CURATE:", "IMPC-CURATE-"))
                    .collect(Collectors.toList());
        }

        String dataSource = fileMetaData.get(0);
        String project    = fileMetaData.get(1);
        String center     = fileMetaData.get(2).replaceAll("_", " ");
        String pipeline   = fileMetaData.get(3);
        String procedure  = fileMetaData.get(4);
        String strain     = fileMetaData.get(5);

        // Strain in the filename does not include a ":"
        if (strain.contains("MGI")) {
            strain = strain.replaceAll("MGI", "MGI:");

        }

        result.setDataSource(dataSource);
        result.setProject(project);
        result.setCenter(center);
        result.setPipeline(pipeline);
        result.setProcedure(procedure);
        result.setStrain(strain);


        String [] fields = data.replace(System.getProperty("line.separator"), "").split("\t", -1);

        result.setMetadataGroup( getStringField(fields[0]) );
        result.setZygosity( getStringField(fields[1]) );
        result.setColonyId( getStringField(fields[2]) );

        // May need to change the output from R
        String depVar = getStringField(fields[3]).replaceAll("_MAPPED", "");
        if (depVar.contains(".")) {
            depVar = depVar.replaceAll("\\.", "-");
        }
        result.setDependentVariable( depVar );

        StatusCode status = StatusCode.valueOf(fields[4]);
        result.setStatus( status.name() );

        result.setCode( getStringField(fields[5]) );
        result.setCountControlMale( getIntegerField(fields[6]) );
        result.setCountControlFemale( getIntegerField(fields[7]) );
        result.setCountMutantMale( getIntegerField(fields[8]) );
        result.setCountMutantFemale( getIntegerField(fields[9]) );

        result.setMaleControlMean ( getDoubleField(fields[10]) );
        result.setFemaleControlMean ( getDoubleField(fields[11]) );
        result.setMaleMutantMean ( getDoubleField(fields[12]) );
        result.setFemaleMutantMean ( getDoubleField(fields[13]) );

        result.setControlSelection( getStringField(fields[14]) );
        result.setWorkflow( getStringField(fields[15]) );
        result.setWeightAvailable( getStringField(fields[16]) );

        try {
            result.setStatisticalMethod(getStringField(fields[17]));
        } catch (ArrayIndexOutOfBoundsException e) {
            result.setStatisticalMethod("-");
        }

        // fields[18] is a duplicate of fields[3]


        if (fields.length < 40) {
            return result;
        }

        switch(status) {
            case TESTED:
                // Result was processed successfully by PhenStat, load the result object

                // Vector output results from PhenStat start at field 19
                int i = 19;

                result.setBatchIncluded( getBooleanField(fields[i++]) );
                result.setResidualVariancesHomogeneity( getBooleanField(fields[i++]) );
                result.setGenotypeContribution( getDoubleField(fields[i++]) );
                result.setGenotypeEstimate( getStringField(fields[i++]) );
                result.setGenotypeStandardError( getDoubleField(fields[i++]) );
                result.setGenotypePVal( getStringField(fields[i++]) );
                result.setGenotypePercentageChange( getStringField(fields[i++]) );
                result.setSexEstimate( getDoubleField(fields[i++]) );
                result.setSexStandardError( getDoubleField(fields[i++]) );
                result.setSexPVal( getDoubleField(fields[i++]) );
                result.setWeightEstimate( getDoubleField(fields[i++]) );
                result.setWeightStandardError( getDoubleField(fields[i++]) );
                result.setWeightPVal( getDoubleField(fields[i++]) );
                result.setGroup1Genotype( getStringField(fields[i++]) );
                result.setGroup1ResidualsNormalityTest( getDoubleField(fields[i++]) );
                result.setGroup2Genotype( getStringField(fields[i++]) );
                result.setGroup2ResidualsNormalityTest( getDoubleField(fields[i++]) );
                result.setBlupsTest( getDoubleField(fields[i++]) );
                result.setRotatedResidualsNormalityTest( getDoubleField(fields[i++]) );
                result.setInterceptEstimate( getDoubleField(fields[i++]) );
                result.setInterceptStandardError( getDoubleField(fields[i++]) );
                result.setInteractionIncluded( getBooleanField(fields[i++]) );
                result.setInteractionPVal( getDoubleField(fields[i++]) );
                result.setSexFvKOEstimate( getDoubleField(fields[i++]) );
                result.setSexFvKOStandardError( getDoubleField(fields[i++]) );
                result.setSexFvKOPVal( getDoubleField(fields[i++]) );
                result.setSexMvKOEstimate( getDoubleField(fields[i++]) );
                result.setSexMvKOStandardError( getDoubleField(fields[i++]) );
                result.setSexMvKOPVal( getDoubleField(fields[i++]) );
                result.setClassificationTag( getStringField(fields[i++]) );
                result.setAdditionalInformation( getStringField(fields[i++]) );

                logger.debug("Last iteration left index i at: ", i);

                break;

            default:
            case FAILED:
                // Result failed to be processed by PhenStat

                result.setBatchIncluded( null );
                result.setResidualVariancesHomogeneity( null );
                result.setGenotypeContribution( null );
                result.setGenotypeEstimate( null );
                result.setGenotypeStandardError( null );
                result.setGenotypePVal( null );
                result.setGenotypePercentageChange( null );
                result.setSexEstimate( null );
                result.setSexStandardError( null );
                result.setSexPVal( null );
                result.setWeightEstimate( null );
                result.setWeightStandardError( null );
                result.setWeightPVal( null );
                result.setGroup1Genotype( null );
                result.setGroup1ResidualsNormalityTest( null );
                result.setGroup2Genotype( null );
                result.setGroup2ResidualsNormalityTest( null );
                result.setBlupsTest( null );
                result.setRotatedResidualsNormalityTest( null );
                result.setInterceptEstimate( null );
                result.setInterceptStandardError( null );
                result.setInteractionIncluded( null );
                result.setInteractionPVal( null );
                result.setSexFvKOEstimate( null );
                result.setSexFvKOStandardError( null );
                result.setSexFvKOPVal( null );
                result.setSexMvKOEstimate( null );
                result.setSexMvKOStandardError( null );
                result.setSexMvKOPVal( null );
                result.setClassificationTag( null );
                result.setAdditionalInformation( null );
                break;
        }

        return result;
    }


    private enum StatusCode {
            TESTED,
            FAILED;
    }

    private class NameIdDTO {

        int dbId;
        String name;
        String stableId;

        public NameIdDTO(int dbId, String name) {
            this.dbId = dbId;
            this.name = name;
        }

        public NameIdDTO(int dbId, String name, String stableId) {
            this.dbId = dbId;
            this.name = name;
            this.stableId = stableId;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getDbId() {
            return dbId;
        }

        public void setDbId(int dbId) {
            this.dbId = dbId;
        }

        public String getStableId() {
            return stableId;
        }

        public void setStableId(String stableId) {
            this.stableId = stableId;
        }
    }


    /**
     * Generate the data set to process
     *
     * @param data    The data result from a stats analysis
     * @return result object partially populated with basic information
     */
    public LightweightResult getBaseResult(LineStatisticalResult data) {

        if (data == null) {
            return null;
        }

        NameIdDTO center = organisationMap.get(data.getCenter());
        NameIdDTO pipeline = pipelineMap.get(data.getPipeline());
        NameIdDTO procedure = procedureMap.get(data.getProcedure()); // Procedure group e.g. IMPC_CAL
        NameIdDTO parameter = parameterMap.get(data.getDependentVariable());

        // result contains a "statistical result" that has the
        // ability to produce a PreparedStatement ready for database insertion

        LightweightResult result;
        StatisticalResult statsResult;

        if (data.getStatisticalMethod().contains("Fisher Exact Test framework")) {

            Double effectSize = getDoubleField(data.getGenotypeEstimate());
            Double pValue = getDoubleField(data.getGenotypePVal());

            // Categorical result
            result = new LightweightCategoricalResult();
            ((LightweightCategoricalResult)result).setCategoryA("normal");
            ((LightweightCategoricalResult) result).setpValue( pValue );
            ((LightweightCategoricalResult) result).setEffectSize( effectSize );

            StatisticalResultCategorical temp = new StatisticalResultCategorical();
            temp.setpValue( pValue );
            temp.setEffectSize( effectSize );
            statsResult = temp;

        } else if (data.getStatisticalMethod().contains("Mixed Model framework")) {

            Double effectSize = getDoubleField(data.getGenotypeEstimate());
            Double pValue = getDoubleField(data.getGenotypePVal());

            // Unidimensional result
            result = new LightweightUnidimensionalResult();
            ((LightweightUnidimensionalResult)result).setFemaleControlMean(data.getFemaleControlMean());
            ((LightweightUnidimensionalResult)result).setFemaleExperimentalMean(data.getFemaleMutantMean());
            ((LightweightUnidimensionalResult)result).setMaleControlMean(data.getMaleControlMean());
            ((LightweightUnidimensionalResult)result).setMaleExperimentalMean(data.getMaleMutantMean());


            StatisticalResultMixedModel temp = new StatisticalResultMixedModel();
            temp.setBatchSignificance(data.getBatchIncluded());
            temp.setVarianceSignificance(data.getResidualVariancesHomogeneity());
            temp.setNullTestSignificance(data.getGenotypeContribution());
            temp.setGenotypeParameterEstimate( effectSize );
            temp.setGenotypeStandardErrorEstimate(data.getGenotypeStandardError());
            temp.setGenotypeEffectPValue( pValue );
            temp.setGenotypePercentageChange(data.getGenotypePercentageChange());
            temp.setGenderParameterEstimate(data.getSexEstimate());
            temp.setGenderStandardErrorEstimate(data.getSexStandardError());
            temp.setGenderEffectPValue(data.getSexPVal());
            temp.setWeightParameterEstimate(data.getWeightEstimate());
            temp.setWeightStandardErrorEstimate(data.getWeightStandardError());
            temp.setWeightEffectPValue(data.getWeightPVal());
            temp.setGp1Genotype(data.getGroup1Genotype());
            temp.setGp1ResidualsNormalityTest(data.getGroup1ResidualsNormalityTest());
            temp.setGp2Genotype(data.getGroup2Genotype());
            temp.setGp2ResidualsNormalityTest(data.getGroup2ResidualsNormalityTest());
            temp.setBlupsTest(data.getBlupsTest());
            temp.setRotatedResidualsNormalityTest(data.getRotatedResidualsNormalityTest());
            temp.setInterceptEstimate(data.getInterceptEstimate());
            temp.setInterceptEstimateStandardError(data.getInterceptStandardError());
            temp.setInteractionSignificance(data.getInteractionIncluded());
            temp.setInteractionEffectPValue(data.getInteractionPVal());
            temp.setGenderFemaleKoEstimate(data.getSexFvKOEstimate());
            temp.setGenderFemaleKoStandardErrorEstimate(data.getSexFvKOStandardError());
            temp.setGenderFemaleKoPValue(data.getSexFvKOPVal());
            temp.setGenderMaleKoEstimate(data.getSexMvKOEstimate());
            temp.setGenderMaleKoStandardErrorEstimate(data.getSexMvKOStandardError());
            temp.setGenderMaleKoPValue(data.getSexMvKOPVal());
            temp.setClassificationTag(data.getClassificationTag());

            statsResult = temp;
        } else if (data.getStatisticalMethod().contains("Reference Ranges Plus framework")) {

            // Reference Range result
            result = new LightweightUnidimensionalResult();
            ((LightweightUnidimensionalResult)result).setFemaleControlMean(data.getFemaleControlMean());
            ((LightweightUnidimensionalResult)result).setFemaleExperimentalMean(data.getFemaleMutantMean());
            ((LightweightUnidimensionalResult)result).setMaleControlMean(data.getMaleControlMean());
            ((LightweightUnidimensionalResult)result).setMaleExperimentalMean(data.getMaleMutantMean());

            StatisticalResultReferenceRangePlus temp = new StatisticalResultReferenceRangePlus();
            temp.setBatchSignificance(data.getBatchIncluded());
            temp.setVarianceSignificance(data.getResidualVariancesHomogeneity());
            temp.setNullTestSignificance(data.getGenotypeContribution());
            temp.setGenotypeParameterEstimate(data.getGenotypeEstimate());
            temp.setGenotypeStandardErrorEstimate(data.getGenotypeStandardError());
            temp.setGenotypeEffectPValue(data.getGenotypePVal());
            temp.setGenotypePercentageChange(data.getGenotypePercentageChange());
            temp.setGenderParameterEstimate(data.getSexEstimate() != null ? data.getSexEstimate().toString() : null);
            temp.setGenderStandardErrorEstimate(data.getSexStandardError());
            temp.setGenderEffectPValue(data.getSexPVal() != null ? data.getSexPVal().toString() : null);
            temp.setWeightParameterEstimate(data.getWeightEstimate());
            temp.setWeightStandardErrorEstimate(data.getWeightStandardError());
            temp.setWeightEffectPValue(data.getWeightPVal());
            temp.setGp1Genotype(data.getGroup1Genotype());
            temp.setGp1ResidualsNormalityTest(data.getGroup1ResidualsNormalityTest());
            temp.setGp2Genotype(data.getGroup2Genotype());
            temp.setGp2ResidualsNormalityTest(data.getGroup2ResidualsNormalityTest());
            temp.setBlupsTest(data.getBlupsTest());
            temp.setRotatedResidualsNormalityTest(data.getRotatedResidualsNormalityTest());
            temp.setInterceptEstimate(data.getInterceptEstimate());
            temp.setInterceptEstimateStandardError(data.getInterceptStandardError());
            temp.setInteractionSignificance(data.getInteractionIncluded());
            temp.setInteractionEffectPValue(data.getInteractionPVal());
            temp.setGenderFemaleKoEstimate(data.getSexFvKOEstimate() != null ? data.getSexFvKOEstimate().toString() : null);
            temp.setGenderFemaleKoStandardErrorEstimate(data.getSexFvKOStandardError());
            temp.setGenderFemaleKoPValue(data.getSexFvKOPVal() != null ? data.getSexFvKOPVal().toString() : null);
            temp.setGenderMaleKoEstimate(data.getSexMvKOEstimate() != null ? data.getSexMvKOEstimate().toString() : null);
            temp.setGenderMaleKoStandardErrorEstimate(data.getSexMvKOStandardError());
            temp.setGenderMaleKoPValue(data.getSexMvKOPVal() != null ? data.getSexMvKOPVal().toString() : null);
            temp.setClassificationTag(data.getClassificationTag());

            statsResult = temp;

        } else {
            // Unknown method or failed to process.
            if (StringUtils.isNotEmpty(data.getStatisticalMethod())) {
                logger.debug("Unknown statistical method '" + data.getStatisticalMethod() + "'");
            }

            result = new LightweightUnidimensionalResult();
            StatisticalResultFailed temp = new StatisticalResultFailed();
            temp.setStatisticalMethod("Not processed");
            statsResult = temp;
        }

        result.setStatisticalResult(statsResult);


        ControlStrategy strategy = ControlStrategy.NA;
        try {
            strategy = ControlStrategy.valueOf(data.getControlSelection());
        } catch (IllegalArgumentException e) {
            // It's ok, stats failed to process so control strat stays as NA
        }

        result.setMetadataGroup(data.getMetadataGroup());
        result.setStatisticalMethod(data.getStatisticalMethod().isEmpty() ? "-" : data.getStatisticalMethod());

        result.setDataSourceId(datasourceMap.get(data.getDataSourceName()));
        result.setProjectId(projectMap.get(data.getProjectName()));

        result.setOrganisationId(center.getDbId());
        result.setOrganisationName(center.getName());

        result.setPipelineId(pipeline.getDbId());
        result.setPipelineStableId(pipeline.getStableId());

        result.setProcedureId(procedure.getDbId());
        result.setProcedureGroup(data.getProcedure());

        result.setParameterId(parameter.getDbId());
        result.setParameterStableId(parameter.getStableId());
        result.setDependentVariable(parameter.getStableId());

        result.setColonyId(data.getColonyId());
        result.setStrain(data.getStrain());
        result.setZygosity(data.getZygosity());

        result.setZygosity(data.getZygosity());

        result.setControlSelectionMethod(strategy);

        Integer bioModelId = bioModelMap.get(data.getColonyId()).get(ZygosityType.valueOf(data.getZygosity()));
        result.setExperimentalId(bioModelId);

        Integer controlBioModelId = controlBioModelMap.get(bioModelStrainMap.get(bioModelId));
        result.setControlId(controlBioModelId);


        // Lookup from colony ID
        result.setAlleleAccessionId(colonyAlleleMap.get(data.getColonyId()));

        result.setMaleControlCount(data.getCountControlMale());
        result.setMaleMutantCount(data.getCountMutantMale());
        result.setFemaleControlCount(data.getCountControlFemale());
        result.setFemaleMutantCount(data.getCountMutantFemale());

        Set<String> sexes = new HashSet<>();
        if (data.getCountMutantMale()>3) {
            sexes.add("male");
        }
        if (data.getCountMutantFemale()>3) {
            sexes.add("female");
        }

        // Set the sex(es) of the result set
        result.setSex(SexType.both.getName());
        if (sexes.size() == 1) {
            result.setSex(new ArrayList<>(sexes).get(0));
        }

        // Set workflow if it has been provided
        if (Arrays.stream(BatchClassification.values())
                .map(Enum::toString)
                .collect(Collectors.toList())
                .contains(data.getWorkflow())) {

            BatchClassification batches = BatchClassification.valueOf(data.getWorkflow());
            result.setWorkflow(batches);
        } else  {
            result.setWorkflow(null);
        }

        result.setWeightAvailable(data.getWeightAvailable() != null && data.getWeightAvailable().equals("TRUE"));

        result.setCalculationTimeNanos(0L);

        result.setStatus(data.getStatus() + " - " + data.getCode());
        setMpTerm(result);

        return result;
    }

    /**
     * Set the appropriate MP term(s) on the result object
     * <p>
     * Could set differnet terms for male and female
     *
     * @param result the result object to populate
     * @throws SQLException
     */
    public void setMpTerm(LightweightResult result) {

        ResultDTO resultWrapper = new ResultDTO(result);

        try (Connection connection = komp2DataSource.getConnection()) {

            String parameterId = result.getParameterStableId();

            OntologyTerm mpTerm = mpTermService.getMPTerm(parameterId, resultWrapper, null, connection, 1.0f);

            if (mpTerm != null) {
                result.setMpAcc(mpTerm.getId().getAccession());

            } else {

                OntologyTerm femaleMpTerm = null;
                OntologyTerm maleMpTerm = null;

                if (resultWrapper.getFemaleEffectSize() != null) {
                    femaleMpTerm = mpTermService.getMPTerm(parameterId, resultWrapper, SexType.female, connection, 1.0f);
                }
                if (resultWrapper.getMaleEffectSize() != null) {
                    maleMpTerm = mpTermService.getMPTerm(parameterId, resultWrapper, SexType.male, connection, 1.0f);
                }

                if (femaleMpTerm != null && result instanceof LightweightUnidimensionalResult) {
                    ((LightweightUnidimensionalResult) result).setFemaleMpAcc(femaleMpTerm.getId().getAccession());
                }
                if (maleMpTerm != null && result instanceof LightweightUnidimensionalResult) {
                    ((LightweightUnidimensionalResult) result).setMaleMpAcc(maleMpTerm.getId().getAccession());
                }

                if (femaleMpTerm != null && maleMpTerm != null && maleMpTerm.equals(femaleMpTerm)) {

                    // Both female and male terms are defined and the same
                    result.setMpAcc(femaleMpTerm.getId().getAccession());

                } else {

                    // Sexual dimorphism, get the abnormal term for this parameter
                    mpTerm = mpTermService.getAbnormalMPTerm(parameterId, resultWrapper, connection, 1.0f);

                    if (mpTerm != null) {
                        result.setMpAcc(mpTerm.getId().getAccession());
                    }

                }
            }
        } catch (SQLException e) {
            logger.warn("Cannot retreive MP term for result", result);
        }
    }

    private void processFile(String loc) throws IOException {

        try {
            Map<String, Integer> counts = new HashMap<>();

            for (String line : Files.readAllLines(Paths.get(loc))) {

                LightweightResult result = getBaseResult(getResult(line, Paths.get(loc).getFileName().toString()));

                if (result == null) {
                    // Skipping record
                    continue;
                }

                try (Connection connection = komp2DataSource.getConnection()) {

                    PreparedStatement p = result.getSaveResultStatement(connection);
                    p.executeUpdate();

                    counts.put(result.getStatisticalMethod(), counts.getOrDefault(result.getStatisticalMethod(), 0) + 1);

                } catch (SQLException e) {
                    e.printStackTrace();
                }

            }

            for(String method : counts.keySet()) {
                logger.info("  ADDED " + counts.get(method) + " statistical results for method: " + method);
            }

        } catch (Exception e) {
          logger.error("Could not process file: " + loc, e);
        }


    }

    @Override
    public void run(String... strings) throws Exception {

        logger.info("Starting statistical result loader");

        // Populate lookups
        populateOrganisationMap();
        populatePipelineMap();
        populateProcedureMap();
        populateParameterMap();
        populateDatasourceMap();
        populateProjectMap();
        populateColonyAlleleMap();
        populateBioModelMap();
        populateControlBioModelMap();


        // parameter to indicate the location of the result file(s)
        OptionParser parser = new OptionParser();
        parser.accepts("location").withRequiredArg().ofType(String.class).isRequired();
        OptionSet options = parser.parse(strings);
        if ( ! options.hasArgument("location") ) {
            logger.error("location argument missing");
            return;
        }
        fileLocation = (String) options.valuesOf("location").get(0);

        // If the location is a single file, parse it
        boolean regularFile = Files.isRegularFile(Paths.get(fileLocation));
        boolean directory = Files.isDirectory(Paths.get(fileLocation));

        if (regularFile) {

            // process the file
            processFile(fileLocation);

        } else if (directory) {

            // process all regular files in the directory that end in "result" and have "tsv" in the filename
            Files
                .walk(Paths.get(fileLocation))
                .filter(p -> p.toString().endsWith("result"))
                .filter(p -> p.toString().contains(".tsv"))
                .filter(p -> Files.isRegularFile(p.toAbsolutePath()))
                .parallel()
                .forEach(p -> {
                    logger.info("Processing file: " + p.toAbsolutePath().toString());
                    try {
                        processFile(p.toAbsolutePath().toString());
                    } catch (IOException e) {
                        logger.warn("IO error proccessing file: " + p.toAbsolutePath().toString());
                    }
                });

        } else {
            logger.warn("File " + fileLocation + " is not a regular file or a directory");
        }


    }

    public static void main(String[] args) throws Exception {
        SpringApplication.run(StatisticalResultLoader.class, args);
    }



}
