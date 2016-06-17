/*******************************************************************************
 * Copyright © 2015 EMBL - European Bioinformatics Institute
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the
 * License.
 ******************************************************************************/

package org.mousephenotype.cda.loads.cdaloader.configs;

import org.mousephenotype.cda.db.pojo.GenomicFeature;
import org.mousephenotype.cda.db.pojo.OntologyTerm;
import org.mousephenotype.cda.db.pojo.SequenceRegion;
import org.mousephenotype.cda.enumerations.DbIdType;
import org.mousephenotype.cda.loads.cdaloader.exceptions.CdaLoaderException;
import org.mousephenotype.cda.loads.cdaloader.steps.*;
import org.mousephenotype.cda.loads.cdaloader.support.SqlLoaderUtils;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import javax.annotation.PostConstruct;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by mrelac on 03/05/16.
 */
@Configuration
public class ConfigBeans {

    private Map<String, OntologyTerm>   featureTypes    = new HashMap<>();
    private Map<String, GenomicFeature> genomicFeatures = new HashMap<>();
    private Map<String, SequenceRegion> sequenceRegions = new HashMap<>();

    @NotNull
    @Value("${cdaload.workspace}")
    protected String cdaWorkspace;

    @NotNull
    @Value("${owlpath}")
    protected String owlpath;

    @Autowired
    private StepBuilderFactory stepBuilderFactory;


    public class DownloadFilename {
        public final DownloadFileEnum downloadFileEnum;
        public final String sourceUrl;
        public final String targetFilename;
        public final int dbId;

        public DownloadFilename(DownloadFileEnum downloadFileEnum, String sourceUrl, String targetFilename, int dbId) {
            this.downloadFileEnum = downloadFileEnum;
            this.sourceUrl = sourceUrl;
            this.targetFilename = targetFilename;
            this.dbId = dbId;
        }
    }

    public class DownloadOntologyFilename extends DownloadFilename {
        public final String prefix;

        public DownloadOntologyFilename(DownloadFileEnum downloadFileEnum, String sourceUrl, String targetFilename, int dbId, String prefix) {
            super(downloadFileEnum, sourceUrl, targetFilename, dbId);
            this.prefix = prefix;
        }
    }


    public DownloadFilename[] filenames;
    private enum DownloadFileEnum {
          report
        , ES_CellLine
        , EUCOMM_Allele
        , HMD_HumanPhenotype
        , KOMP_Allele
        , MGI_Gene
        , MGI_Gene_Model_Coord
        , MGI_GenePheno
        , MGI_GTGUP
        , MGI_PhenoGenoMP
        , MGI_PhenotypicAllele
        , MGI_QTLAllele
        , MGI_Strain
        , MRK_List1
        , MRK_List2
        , MRK_Reference
        , MRK_Sequence
        , MRK_SwissProt
        , NorCOMM_Allele
        , eco
        , efo
        , emap
        , emapa
        , ma
        , MmusDv
        , mp
        , mpath
        , pato

    }


    @PostConstruct
    public void initialise() {

        filenames = new DownloadFilename[] {
              // imsr
              new DownloadFilename(DownloadFileEnum.report, "http://www.findmice.org/report.txt?query=&states=Any&_states=1&types=Any&_types=1&repositories=Any&_repositories=1&_mutations=on&results=500000&startIndex=0&sort=score&dir=", cdaWorkspace + "/report.txt", DbIdType.IMSR.intValue())

              // mgi reports
                , new DownloadFilename(DownloadFileEnum.ES_CellLine, "ftp://ftp.informatics.jax.org/pub/reports/ES_CellLine.rpt", cdaWorkspace + "/ES_CellLine.rpt", DbIdType.MGI.intValue())
                , new DownloadFilename(DownloadFileEnum.EUCOMM_Allele, "ftp://ftp.informatics.jax.org/pub/reports/EUCOMM_Allele.rpt", cdaWorkspace + "/EUCOMM_Allele.rpt", DbIdType.MGI.intValue())
                , new DownloadFilename(DownloadFileEnum.HMD_HumanPhenotype, "ftp://ftp.informatics.jax.org/pub/reports/HMD_HumanPhenotype.rpt", cdaWorkspace + "/HMD_HumanPhenotype.rpt", DbIdType.MGI.intValue())
                , new DownloadFilename(DownloadFileEnum.KOMP_Allele, "ftp://ftp.informatics.jax.org/pub/reports/KOMP_Allele.rpt", cdaWorkspace + "/KOMP_Allele.rpt", DbIdType.MGI.intValue())
                , new DownloadFilename(DownloadFileEnum.MGI_Gene, "ftp://ftp.informatics.jax.org/pub/reports/MGI_Gene.rpt", cdaWorkspace + "/MGI_Gene.rpt", DbIdType.MGI.intValue())
                , new DownloadFilename(DownloadFileEnum.MGI_Gene_Model_Coord, "ftp://ftp.informatics.jax.org/pub/reports/MGI_Gene_Model_Coord.rpt", cdaWorkspace + "/MGI_Gene_Model_Coord.rpt", DbIdType.MGI.intValue())
                , new DownloadFilename(DownloadFileEnum.MGI_GenePheno, "ftp://ftp.informatics.jax.org/pub/reports/MGI_GenePheno.rpt", cdaWorkspace + "/MGI_GenePheno.rpt", DbIdType.MGI.intValue())
                , new DownloadFilename(DownloadFileEnum.MGI_GTGUP, "ftp://ftp.informatics.jax.org/pub/reports/MGI_GTGUP.gff", cdaWorkspace + "/MGI_GTGUP.gff", DbIdType.MGI.intValue())
                , new DownloadFilename(DownloadFileEnum.MGI_PhenoGenoMP, "ftp://ftp.informatics.jax.org/pub/reports/MGI_PhenoGenoMP.rpt", cdaWorkspace + "/MGI_PhenoGenoMP.rpt", DbIdType.MGI.intValue())
                , new DownloadFilename(DownloadFileEnum.MGI_PhenotypicAllele, "ftp://ftp.informatics.jax.org/pub/reports/MGI_PhenotypicAllele.rpt", cdaWorkspace + "/MGI_PhenotypicAllele.rpt", DbIdType.MGI.intValue())
                , new DownloadFilename(DownloadFileEnum.MGI_QTLAllele, "ftp://ftp.informatics.jax.org/pub/reports/MGI_QTLAllele.rpt", cdaWorkspace + "/MGI_QTLAllele.rpt", DbIdType.MGI.intValue())
                , new DownloadFilename(DownloadFileEnum.MGI_Strain, "ftp://ftp.informatics.jax.org/pub/reports/MGI_Strain.rpt", cdaWorkspace + "/MGI_Strain.rpt", DbIdType.MGI.intValue())
                , new DownloadFilename(DownloadFileEnum.MRK_List1, "ftp://ftp.informatics.jax.org/pub/reports/MRK_List1.rpt", cdaWorkspace + "/MRK_List1.rpt", DbIdType.MGI.intValue())
                , new DownloadFilename(DownloadFileEnum.MRK_List2, "ftp://ftp.informatics.jax.org/pub/reports/MRK_List2.rpt", cdaWorkspace + "/MRK_List2.rpt", DbIdType.MGI.intValue())
                , new DownloadFilename(DownloadFileEnum.MRK_Reference, "ftp://ftp.informatics.jax.org/pub/reports/MRK_Reference.rpt", cdaWorkspace + "/MRK_Reference.rpt", DbIdType.MGI.intValue())
                , new DownloadFilename(DownloadFileEnum.MRK_Sequence, "ftp://ftp.informatics.jax.org/pub/reports/MRK_Sequence.rpt", cdaWorkspace + "/MRK_Sequence.rpt", DbIdType.MGI.intValue())
                , new DownloadFilename(DownloadFileEnum.MRK_SwissProt, "ftp://ftp.informatics.jax.org/pub/reports/MRK_SwissProt.rpt", cdaWorkspace + "/MRK_SwissProt.rpt", DbIdType.MGI.intValue())
                , new DownloadFilename(DownloadFileEnum.NorCOMM_Allele, "ftp://ftp.informatics.jax.org/pub/reports/NorCOMM_Allele.rpt", cdaWorkspace + "/NorCOMM_Allele.rpt", DbIdType.MGI.intValue())

            // OWL ontologies
//            , new DownloadOntologyFilename(DownloadFileEnum.eco, "https://raw.githubusercontent.com/evidenceontology/evidenceontology/master/eco.owl", cdaWorkspace + "/eco.owl", DbIdType.ECO.intValue(), DbIdType.ECO.getName())
//            , new DownloadOntologyFilename(DownloadFileEnum.efo, "http://www.ebi.ac.uk/efo/efo.owl", cdaWorkspace + "/efo.owl", DbIdType.EFO.intValue(), DbIdType.EFO.getName())
//            , new DownloadOntologyFilename(DownloadFileEnum.emap, "http://purl.obolibrary.org/obo/emap.owl", cdaWorkspace + "/emap.owl", DbIdType.EMAP.intValue(), DbIdType.EMAP.getName())
//            , new DownloadOntologyFilename(DownloadFileEnum.emapa, "http://www.berkeleybop.org/ontologies/emapa.owl", cdaWorkspace + "/emapa.owl", DbIdType.EMAPA.intValue(), DbIdType.EMAPA.getName())
//            , new DownloadOntologyFilename(DownloadFileEnum.ma, "http://purl.obolibrary.org/obo/ma.owl", cdaWorkspace + "/ma.owl", DbIdType.MA.intValue(), DbIdType.MA.getName())
            , new DownloadOntologyFilename(DownloadFileEnum.MmusDv, "http://www.berkeleybop.org/ontologies/MmusDv.owl", cdaWorkspace + "/MmusDv.owl", DbIdType.MmusDv.intValue(), DbIdType.MmusDv.getName())
//            , new DownloadOntologyFilename(DownloadFileEnum.mp, "ftp://ftp.informatics.jax.org/pub/reports/mp.owl", cdaWorkspace + "/mp.owl", DbIdType.MP.intValue(), DbIdType.MP.getName())
//            , new DownloadOntologyFilename(DownloadFileEnum.mpath, "http://purl.obolibrary.org/obo/mpath.owl", cdaWorkspace + "/mpath.owl", DbIdType.MPATH.intValue(), DbIdType.MPATH.getName())
//            , new DownloadOntologyFilename(DownloadFileEnum.pato, "https://raw.githubusercontent.com/pato-ontology/pato/master/pato.owl", cdaWorkspace + "/pato.owl", DbIdType.PATO.intValue(), DbIdType.PATO.getName())
        };

        for (DownloadFilename downloadFilename : filenames) {
            downloadFilenameMap.put(downloadFilename.downloadFileEnum, downloadFilename);
        }
    }

    Map<DownloadFileEnum, DownloadFilename> downloadFilenameMap = new HashMap<>();


    @Bean(name = "databaseInitialiser")
    public DatabaseInitialiser databaseInitialiser() {
        return new DatabaseInitialiser();
    }

    @Bean(name = "downloader")
    public List<Downloader> downloader() {
        List<Downloader> downloaderList = new ArrayList<>();

        for (DownloadFilename download : filenames) {
            downloaderList.add(new Downloader(download.sourceUrl, download.targetFilename));
        }

        return downloaderList;
    }

    @Bean(name = "sqlLoaderUtils")
    public SqlLoaderUtils sqlLoaderUtils() {
        return new SqlLoaderUtils();
    }


    // LOADERS, PROCESSORS, AND WRITERS


    @Bean(name = "alleleProcessorPhenotypic")
    public AlleleProcessorPhenotypic alleleProcessorPhenotypic() {
        return new AlleleProcessorPhenotypic(genomicFeatures, featureTypes, sequenceRegions);
    }

    @Bean(name = "alleleProcessorQtl")
    public AlleleProcessorQtl alleleProcessorQtl() {
        return new AlleleProcessorQtl(genomicFeatures, featureTypes, sequenceRegions);
    }

    @Bean(name = "alleleLoader")
    public AlleleLoader alleleLoader() throws CdaLoaderException {
        Map<AlleleLoader.FilenameKeys, String> filenameKeys = new HashMap<>();
        filenameKeys.put(AlleleLoader.FilenameKeys.EUCOMM, downloadFilenameMap.get(DownloadFileEnum.EUCOMM_Allele).targetFilename);
        filenameKeys.put(AlleleLoader.FilenameKeys.GENOPHENO, downloadFilenameMap.get(DownloadFileEnum.MGI_GenePheno).targetFilename);
        filenameKeys.put(AlleleLoader.FilenameKeys.KOMP, downloadFilenameMap.get(DownloadFileEnum.KOMP_Allele).targetFilename);
        filenameKeys.put(AlleleLoader.FilenameKeys.NORCOM, downloadFilenameMap.get(DownloadFileEnum.NorCOMM_Allele).targetFilename);
        filenameKeys.put(AlleleLoader.FilenameKeys.PHENOTYPIC, downloadFilenameMap.get(DownloadFileEnum.MGI_PhenotypicAllele).targetFilename);
        filenameKeys.put(AlleleLoader.FilenameKeys.QTL, downloadFilenameMap.get(DownloadFileEnum.MGI_QTLAllele).targetFilename);

        return new AlleleLoader(filenameKeys);
    }


    @Bean(name = "markerLoader")
    public MarkerLoader markerLoader() throws CdaLoaderException {
        Map<MarkerLoader.FilenameKeys, String> filenameKeys = new HashMap<>();
        filenameKeys.put(MarkerLoader.FilenameKeys.GENE_TYPES, downloadFilenameMap.get(DownloadFileEnum.MGI_GTGUP).targetFilename);
        filenameKeys.put(MarkerLoader.FilenameKeys.MARKER_LIST, downloadFilenameMap.get(DownloadFileEnum.MRK_List1).targetFilename);
        filenameKeys.put(MarkerLoader.FilenameKeys.XREFS, downloadFilenameMap.get(DownloadFileEnum.MGI_Gene).targetFilename);

        return new MarkerLoader(filenameKeys);
    }

    @Bean(name = "markerProcessorGeneTypes")
    public MarkerProcessorGeneTypes markerProcessorGeneTypes() {
        return new MarkerProcessorGeneTypes(genomicFeatures, featureTypes, sequenceRegions);
    }

    @Bean(name = "markerProcessorMarkerList")
    public MarkerProcessorMarkerList markerProcessorMarkerList() {
        return new MarkerProcessorMarkerList(genomicFeatures, featureTypes, sequenceRegions);
    }

    @Bean(name = "markerProcessorXrefs")
    public MarkerProcessorXrefs markerProcessorXrefs() {
        return new MarkerProcessorXrefs(genomicFeatures);
    }

    @Bean(name = "markerWriter")
    public MarkerWriter markerWriter() {
        return new MarkerWriter();
    }


    @Bean(name = "ontologyLoaderList")
    public List<OntologyLoader> ontologyLoader() throws CdaLoaderException {
        List<OntologyLoader> ontologyloaderList = new ArrayList<>();

        for (DownloadFilename filename : filenames) {
            if (filename instanceof DownloadOntologyFilename) {
                DownloadOntologyFilename downloadOntology = (DownloadOntologyFilename) filename;
                ontologyloaderList.add(new OntologyLoader(downloadOntology.targetFilename, downloadOntology.dbId, downloadOntology.prefix, stepBuilderFactory, ontologyWriter()));
            }
        }

        return ontologyloaderList;
    }

    @Bean(name = "ontologyWriter")
    public OntologyWriter ontologyWriter() {
        return new OntologyWriter();
    }


    @Bean(name = "strainLoader")
    public StrainLoader strainLoader() throws CdaLoaderException {
        Map<StrainLoader.FilenameKeys, String> filenameKeys = new HashMap<>();
        filenameKeys.put(StrainLoader.FilenameKeys.MGI, downloadFilenameMap.get(DownloadFileEnum.MGI_Strain).targetFilename);
        filenameKeys.put(StrainLoader.FilenameKeys.IMSR, downloadFilenameMap.get(DownloadFileEnum.report).targetFilename);

        return new StrainLoader(filenameKeys);
    }

    @Bean(name = "strainProcessorImsr")
    public StrainProcessorImsr strainProcessorImsr() {
        return new StrainProcessorImsr();
    }

    @Bean(name = "strainWriter")
    public StrainWriter strainWriter() {
       return new StrainWriter();
    }


    // NOTE: Using @Lazy here and in the @Autowire to postpone creation of this bean (so that @PostConstruct can be used)
    //       doesn't delay invocation of the @PostConstruct as we would like, so we shant use it.
    /**
     * ******** DO NOT DELETE. JUST UNCOMMENT IF YOU NEED THE SPRING BATCH TABLES REBUILT ********
     * Using this home-grown jobRepository correctly recreates the spring batch tables. Without it, you have to
     * recreate the tables manually after dropping them.
     */
//    @Bean(name = "jobRepository")
//    public JobRepository jobRepository() throws Exception {
//        MapJobRepositoryFactoryBean b = new MapJobRepositoryFactoryBean();
//
//        JobRepository jobRepository = b.getObject();
//
//        return jobRepository;
//    }
}