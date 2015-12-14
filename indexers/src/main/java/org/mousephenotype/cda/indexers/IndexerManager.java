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

package org.mousephenotype.cda.indexers;

import joptsimple.HelpFormatter;
import joptsimple.OptionDescriptor;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.apache.commons.lang3.StringUtils;
import org.mousephenotype.cda.indexers.exceptions.*;
import org.mousephenotype.cda.utilities.CommonUtils;
import org.mousephenotype.cda.utilities.RunStatus;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;

import java.io.File;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.mousephenotype.cda.indexers.AbstractIndexer.CONTEXT_ARG;

/**
 * This class encapsulates the code and data necessary to represent an index
 * manager that manages the creation and deployment of all of the indexes
 * required for phenotype archive; a job previously delegated to Jenkins that
 * was susceptible to frequent failure.
 *
 * @author mrelac
 */
public class IndexerManager {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(IndexerManager.class);
    protected CommonUtils commonUtils = new CommonUtils();

    // core names.
    //      These are built only for a new data release.
    public static final String OBSERVATION_CORE = "experiment";                 // For historic reasons, the core's actual name is 'experiment'.
    public static final String GENOTYPE_PHENOTYPE_CORE = "genotype-phenotype";
    public static final String STATSTICAL_RESULT_CORE = "statistical-result";

    //      These are built daily.
    public static final String PREQC_CORE = "preqc";
    public static final String ALLELE_CORE = "allele";
    public static final String IMAGES_CORE = "images";
    public static final String IMPC_IMAGES_CORE = "impc_images";
    public static final String MP_CORE = "mp";
    public static final String MA_CORE = "ma";
    public static final String PIPELINE_CORE = "pipeline";
    public static final String GENE_CORE = "gene";
    public static final String DISEASE_CORE = "disease";
    public static final String AUTOSUGGEST_CORE = "autosuggest";

    // main return values.
    public static final int STATUS_OK                  = 0;
    public static final int STATUS_WARN                = 1;
    public static final int STATUS_NO_DEPS             = 2;
    public static final int STATUS_NO_ARGUMENT         = 3;
    public static final int STATUS_UNRECOGNIZED_OPTION = 4;
    public static final int STATUS_INVALID_CORE_NAME   = 5;
    public static final int STATUS_VALIDATION_ERROR    = 6;

    public static String getStatusCodeName(int statusCode) {
        switch (statusCode) {
            case STATUS_OK:                     return "STATUS_OK";
            case STATUS_WARN:                   return "STATUS_WARN";
            case STATUS_NO_DEPS:                return "STATUS_NO_DEPS";
            case STATUS_NO_ARGUMENT:            return "STATUS_NO_ARGUMENT";
            case STATUS_UNRECOGNIZED_OPTION:    return "STATUS_UNRECOGNIZED_OPTION";
            case STATUS_INVALID_CORE_NAME:      return "STATUS_INVALID_CORE_NAME";
            case STATUS_VALIDATION_ERROR:       return "STATUS_VALIDATION_ERROR";
            default:                            return "Unknown status code " + statusCode;
        }
    }

    // These are the args that can be passed to the indexer manager.
    private Boolean all;
    private List<String> cores;
    private Boolean daily;
    private Boolean nodeps;

    // These are the args passed to the individual indexers. They should be all the same and should be the same context argument passed to the indexerManager.
    private String[] indexerArgs;

    public static final String[] allCoresArray = new String[] {      // In dependency order.
          // In dependency order. These are built only for a new data release.
          OBSERVATION_CORE
        , GENOTYPE_PHENOTYPE_CORE
        , STATSTICAL_RESULT_CORE

          // These are built daily.
        , PREQC_CORE
        , ALLELE_CORE
        , IMAGES_CORE
        , IMPC_IMAGES_CORE
        , MP_CORE
        , MA_CORE
        , PIPELINE_CORE
        , GENE_CORE
        , DISEASE_CORE
        , AUTOSUGGEST_CORE
    };
    private final List<String> allCoresList = Arrays.asList(allCoresArray);

    public static final String[] allDailyCoresArray = new String[] {
          // In dependency order. These are built daily.
          PREQC_CORE
        , ALLELE_CORE
        , IMAGES_CORE
        , IMPC_IMAGES_CORE
        , MP_CORE
        , MA_CORE
        , PIPELINE_CORE
        , GENE_CORE
        , DISEASE_CORE
        , AUTOSUGGEST_CORE
    };

    public static final int RETRY_COUNT = 2;                                    // If any core fails, retry building it up to this many times.
    public static final int RETRY_SLEEP_IN_MS = 60000;                          // If any core fails, sleep this long before reattempting to build the core.
    public static final String STAGING_SUFFIX = "_staging";                     // This snippet is appended to core names meant to be staging core names.

    ;

    @Autowired
    ObservationIndexer observationIndexer;

    @Autowired
    GenotypePhenotypeIndexer genotypePhenotypeIndexer;

    @Autowired
    StatisticalResultIndexer statisticalResultIndexer;

    @Autowired
    PreqcIndexer preqcIndexer;

    @Autowired
    AlleleIndexer alleleIndexer;

    @Autowired
    SangerImagesIndexer imagesIndexer;

    @Autowired
    ImpcImagesIndexer impcImagesIndexer;

    @Autowired
    MPIndexer mpIndexer;

    @Autowired
    MAIndexer maIndexer;

    @Autowired
    PipelineIndexer pipelineIndexer;

    @Autowired
    GeneIndexer geneIndexer;

    @Autowired
    DiseaseIndexer diseaseIndexer;

    @Autowired
    AutosuggestIndexer autosuggestIndexer;

    private IndexerItem[] indexerItems;

    public String[] args;

    public static final String ALL_ARG = "all";
    public static final String CORES_ARG = "cores";
    public static final String DAILY_ARG = "daily";
    public static final String NO_DEPS_ARG = "nodeps";

    public class IndexerItem {
        public final String name;
        public final AbstractIndexer indexer;

        public IndexerItem(String name, AbstractIndexer indexer) {
            this.name = name;
            this.indexer = indexer;
        }
    }

    private enum RunResult {
        OK,
        WARNING,
        FAIL;

        private String getName(){
       		return this.toString();
       	}
    }

    protected ApplicationContext applicationContext;


    // GETTERS


    public Boolean getAll() {
        return all;
    }

    public List<String> getCores() {
        return cores;
    }

    public Boolean getDaily() {
        return daily;
    }

    public Boolean getNodeps() {
        return nodeps;
    }


    // PUBLIC/PROTECTED METHODS


    public void initialise(String[] args) throws IndexerException {
        logger.debug("IndexerManager called with args = " + StringUtils.join(args, ", "));

        try {
            OptionSet options = parseCommandLine(args);
            if (options != null) {
                this.args = args;
                applicationContext = loadApplicationContext((String)options.valuesOf(CONTEXT_ARG).get(0));
                applicationContext.getAutowireCapableBeanFactory().autowireBeanProperties(this, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, true);
                initialiseHibernateSession(applicationContext);
                loadIndexers();
            } else {
                throw new IndexerException("Failed to parse command-line options.");
            }

            // Print the starting jvm memory configuration.
            printJvmMemoryConfiguration();
        } catch (Exception e) {
            if (e.getLocalizedMessage() != null) {
                logger.error(e.getLocalizedMessage());
            }
            throw new IndexerException(e);
        }
    }

    protected void initialiseHibernateSession(ApplicationContext applicationContext) {
        // allow hibernate session to stay open the whole execution
        PlatformTransactionManager transactionManager = (PlatformTransactionManager) applicationContext.getBean("transactionManager");
        DefaultTransactionAttribute transactionAttribute = new DefaultTransactionAttribute(TransactionDefinition.PROPAGATION_REQUIRED);
        transactionAttribute.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);
        transactionManager.getTransaction(transactionAttribute);
    }

    public void run() throws IndexerException {
        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        ExecutionStatsList executionStatsList = new ExecutionStatsList();
        logger.debug("IndexerManager: nodeps = " + nodeps);
        System.out.println("Building these cores in this order:	" + StringUtils.join(cores));

        for (IndexerItem indexerItem : indexerItems) {
            long start = System.currentTimeMillis();
            RunStatus runStatus = new RunStatus();
            RunResult runResult = RunResult.OK;

            logger.info("[START] {} at {}", indexerItem.name.toUpperCase(), dateFormatter.format(new Date()));
            try {
                indexerItem.indexer.initialise(indexerArgs);
                runStatus = indexerItem.indexer.run();
                if (runStatus.hasErrors()) {
                    for (String errorMessage : runStatus.getErrorMessages()) {
                        logger.error(errorMessage);
                        runResult = RunResult.FAIL;
                    }
                } else {
                    if (runStatus.hasWarnings()) {
                        for (String warningMessage : runStatus.getWarningMessages()) {
                            logger.warn(warningMessage);
                            runResult = RunResult.WARNING;
                        }
                    }

                    runStatus = indexerItem.indexer.validateBuild();
                    if (runStatus.hasErrors()) {
                        for (String errorMessage : runStatus.getErrorMessages()) {
                            logger.error(errorMessage);
                            runResult = RunResult.FAIL;
                        }
                    } else {
                        if (runStatus.hasWarnings()) {
                            for (String warningMessage : runStatus.getWarningMessages()) {
                                logger.warn(warningMessage);
                                runResult = RunResult.WARNING;
                            }
                        }
                    }
                }

            } catch (IndexerException ie) {
                runStatus.addError(ie.getLocalizedMessage());
                logExceptions(ie);
                runResult = RunResult.FAIL;
            }

            logger.info("[END]   {} at {}", indexerItem.name.toUpperCase(), dateFormatter.format(new Date()));
            executionStatsList.add(new ExecutionStatsRow(indexerItem.name, runResult, start, new Date().getTime()));
            printJvmMemoryConfiguration();
        }

        System.out.println(executionStatsList.toString());
    }

    protected ApplicationContext loadApplicationContext(String context) {
        ApplicationContext appContext;

        // Try context as a file resource.
        File file = new File(context);
        if (file.exists()) {
            // Try context as a file resource
            appContext = new FileSystemXmlApplicationContext("file:" + context);
        } else {
            // Try context as a class path resource
            appContext = new ClassPathXmlApplicationContext(context);
        }

        if (appContext == null) {
            logger.error("Unable to load context '" + context  + "' from file or classpath. Exiting...");
        }

        return appContext;
    }

    public void validateParameters(OptionSet options, List<String> coresRequested) throws IndexerException {
        // Exactly one of: ALL_ARG, DAILY_ARG, or CORES_ARG must be specified.
        if ( ! (options.has(ALL_ARG) || (options.has(DAILY_ARG) || options.has(CORES_ARG)))) {
            throw new IndexerException(new MissingRequiredArgumentException("Expected either --all, --daily, or --cores=aaa"));
        }

        // if DAILY_ARG specified, no other args are allowed.
        if (options.has(ALL_ARG)) {
            if ((options.has(DAILY_ARG)) || (options.has(CORES_ARG)) || (options.has(NO_DEPS_ARG))) {
                throw new IndexerException(new ValidationException("Expected exactly one of: --all, --daily, or --cores=aaa"));
            }
        }
        // if DAILY_ARG specified, no other args are allowed.
        if (options.has(DAILY_ARG)) {
            if ((options.has(ALL_ARG)) || (options.has(CORES_ARG)) || (options.has(NO_DEPS_ARG))) {
                throw new IndexerException(new ValidationException("Expected exactly one of: --all, --daily, or --cores=aaa"));
            }
        }
        // if CORES_ARG specified, neither ALL_ARG nor DAILY_ARG is permitted.
        if (options.has(CORES_ARG)) {
            if ((options.has(ALL_ARG)) || (options.has(DAILY_ARG))) {
                throw new IndexerException(new ValidationException("Expected exactly one of: --all, --daily, or --cores=aaa"));
            }
        }

        // NO_DEPS_ARG may only be specified with CORES_ARG.
        if (options.has(NO_DEPS_ARG)) {
            if ((options.has(ALL_ARG)) || (options.has(DAILY_ARG))) {
                throw new IndexerException(new ValidationException("--nodeps may only be specified with --cores"));
            }
        }

        // Verify that each core name in coresRequested exists. Throw an exception if any does not.
        for (String core : coresRequested) {
            if ( ! allCoresList.contains(core)) {
                throw new IndexerException(new InvalidCoreNameException("Invalid core name '" + core + "'"));
            }
        }
    }


    // PRIVATE METHODS


    private void loadIndexers() {
        List<IndexerItem> indexerItemList = new ArrayList();

        for (String core : cores) {
            switch (core) {
                case OBSERVATION_CORE:          indexerItemList.add(new IndexerItem(OBSERVATION_CORE, observationIndexer));                 break;
                case GENOTYPE_PHENOTYPE_CORE:   indexerItemList.add(new IndexerItem(GENOTYPE_PHENOTYPE_CORE, genotypePhenotypeIndexer));    break;
                case STATSTICAL_RESULT_CORE:    indexerItemList.add(new IndexerItem(STATSTICAL_RESULT_CORE, statisticalResultIndexer));     break;

                case PREQC_CORE:                indexerItemList.add(new IndexerItem(PREQC_CORE, preqcIndexer));                             break;
                case ALLELE_CORE:               indexerItemList.add(new IndexerItem(ALLELE_CORE, alleleIndexer));                           break;
                case IMAGES_CORE:               indexerItemList.add(new IndexerItem(IMAGES_CORE, imagesIndexer));                           break;
                case IMPC_IMAGES_CORE:          indexerItemList.add(new IndexerItem(IMPC_IMAGES_CORE, impcImagesIndexer));                  break;
                case MP_CORE:                   indexerItemList.add(new IndexerItem(MP_CORE, mpIndexer));                                   break;
                case MA_CORE:                   indexerItemList.add(new IndexerItem(MA_CORE, maIndexer));                                   break;
                case PIPELINE_CORE:             indexerItemList.add(new IndexerItem(PIPELINE_CORE, pipelineIndexer));                       break;
                case GENE_CORE:                 indexerItemList.add(new IndexerItem(GENE_CORE, geneIndexer));                               break;
                case DISEASE_CORE:              indexerItemList.add(new IndexerItem(DISEASE_CORE, diseaseIndexer));                         break;
                case AUTOSUGGEST_CORE:          indexerItemList.add(new IndexerItem(AUTOSUGGEST_CORE, autosuggestIndexer));                 break;
            }
        }

        indexerItems = indexerItemList.toArray(new IndexerItem[0]);
    }

    // PRIVATE METHODS

    /*
     * Rules:
     * 1. context is always required.
     * 2. cores is optional.
     * 2a. If cores is missing or empty:
     *     - build all cores starting at the preqc core (don't build OBSERVATION_CORE, GENOTYPE_PHENOTYPE_CORE, or STATSTICAL_RESULT_CORE).
     *     - specifying nodeps throws IllegalArgumentException.
     * 2b. If 1 core:
     *     - core name must be valid.
     *     - if nodeps is specified:
     *       - build only the specified core. Don't build downstream cores.
     *     - else
     *       - build requested core and all downstream cores.
     * 2c. If more than 1 core:
     *     - if nodeps is specified, it is ignored.
     *     - core names must be valid. No downstream cores are built.
     *
     * Core Build Truth Table (assume a valid '--context=' parameter is always supplied - not shown in table below to save space):
     *    |---------------------------------------------------------------------------|
     *    |          command line           |  nodeps value   |     cores built       |
     *    |---------------------------------------------------------------------------|
     *    | <empty>                         | false           | preqc to autosuggest  |
     *    | --cores                         | false           | preqc to autosuggest  |
     *    | --nodeps                        | NoDepsException | <none>                |
     *    | --cores --nodeps                | NoDepsException | <none>                |
     *    | --cores=mp                      | false           | mp to autosuggest     |
     *    | --cores=mp --nodeps             | true            | mp                    |
     *    | --cores=mp,observation          | true            | mp,observation        |
     *    | --cores-mp,observation --nodeps | true            | mp,observation        |
     *    |---------------------------------------------------------------------------|

     * @param args command-line arguments
     * @return<code>OptionSet</code> of parsed parameters
     * @throws IndexerException
     */

    /*
     * Rules:
     * 1. context is always required.
     * 2. cores is always required.
     * 2b. If 1 core:
     *     - core name must be valid.
     *     - if nodeps is specified:
     *       - build only the specified core. Don't build downstream cores.
     *     - else
     *       - build requested core and all downstream cores.
     * 2c. If more than 1 core:
     *     - if nodeps is specified, it is ignored.
     *     - core names must be valid. No downstream cores are built.
     * 3. If 'all' is specified, build all of the cores: experiment to autosuggest.
     *      Specifying --nodeps throws IllegalArgumentException.
     * 4. If 'daily' is specified, build the daily cores: preqc to autosuggest.
     *      Specifying --nodeps throws IllegalArgumentException.
     *
     * Core Build Truth Table (assume a valid '--context=' parameter is always supplied - not shown in table below to save space):
     *    |-----------------------------------------------------------------------------------|
     *    |          command line  | Action                                  |  nodeps value  |
     *    |-----------------------------------------------------------------------------------|
     *    | <empty>                | Throw MissingRequiredArgumentException  |      N/A       |
     *    | --cores                | Throw MissingRequiredArgumentException  |      N/A       |
     *    | --nodeps               | Throw MissingRequiredArgumentException  |      N/A       |
     *    | --cores --nodeps       | Throw MissingRequiredArgumentException  |      N/A       |
     *    | --cores= --nodeps      | Throw MissingRequiredArgumentException  |      N/A       |
     *    | --cores=junk           | Throw InvalidCoreNameException          |      N/A       |
     *    | --cores=mp             | build mp to autosuggest cores           |      false     |
     *    | --cores=mp --nodeps    | build mp core only                      |      true      |
     *    | --cores=mp,ma          | build mp and ma cores                   |      true      |
     *    | --cores-mp,ma --nodeps | build mp and ma cores                   |      true      |
     *    | --all                  | build experiment to autosuggest cores   |      false     |
     *    | --all --cores=ma       | Return STATUS_VALIDATION_ERROR          |      N/A       |
     *    | --all --nodeps         | Return STATUS_VALIDATION_ERROR          |      N/A       |
     *    | --daily                | build preqc to autosuggest cores.       |      false     |
     *    | --daily --cores=ma     | Return STATUS_VALIDATION_ERROR          |      N/A       |
     *    | --daily --nodeps       | Return STATUS_VALIDATION_ERROR          |      N/A       |
     *    | --all --daily          | Return STATUS_VALIDATION_ERROR          |      N/A       |
     *    | --all --daily --nodeps | Return STATUS_VALIDATION_ERROR          |      N/A       |
     *    |-----------------------------------------------------------------------------------|

     * @param args command-line arguments
     * @return<code>OptionSet</code> of parsed parameters
     * @throws IndexerException
     */
    private OptionSet parseCommandLine(String[] args) throws IndexerException {
        OptionParser parser = new OptionParser();
        OptionSet options = null;

        // Spring context file parameter [required]
        parser.accepts(CONTEXT_ARG)
                .withRequiredArg()
                .required()
                .ofType(String.class)
                .describedAs("Spring context file, such as 'index-app-config.xml'");

        // cores [optional]
        parser.accepts(CORES_ARG)
                .withRequiredArg()
                .ofType(String.class)
                .describedAs("A list of cores, in build order.");

        parser.accepts(ALL_ARG);
        parser.accepts(DAILY_ARG);
        parser.accepts(NO_DEPS_ARG);

        try {
            // Parse the parameters.
            options = parser.parse(args);
            boolean coreMissingOrEmpty = false;
            List<String> coresRequested = new ArrayList();                      // This is a list of the unique core values specified in the 'cores=' argument.

            // Create a list of cores requested based on the value the 'cores=' argument.
            if (options.has(CORES_ARG)) {
                String rawCoresArgument = (String)options.valueOf((CORES_ARG));
                if ((rawCoresArgument == null) || (rawCoresArgument.trim().isEmpty())) {
                    // Cores list is empty.
                    coreMissingOrEmpty = true;
                } else if ( ! rawCoresArgument.contains(",")) {
                    coresRequested.add(rawCoresArgument);
                } else {
                    String[] coresArray = ((String)options.valueOf(CORES_ARG)).split(",");
                    coresRequested.addAll(Arrays.asList(coresArray));
                }
            } else {
                // Cores list is missing.
                coreMissingOrEmpty = true;
            }
            // The only case where nodeps is true is when:
            //    --cores is specified, AND
            //    (--nodeps is specified OR # cores requested > 1)
            if ((options.has(NO_DEPS_ARG)) && (options.has(NO_DEPS_ARG) || coresRequested.size() > 1)) {
                nodeps = true;
            } else {
                nodeps = false;
            }

            validateParameters(options, coresRequested);

            // Build the cores list as follows:
            //   If --all specified, set firstCore to experiment.
            //   Else if --daily specified, set firstCore to preqc.
            //   Else if --cores specified
            //       If nodeps or coresRequested.size > 1
            //           set firstCore to null.
            //           set cores to coresRequested[0].
            //       Else
            //           set firstCore to coresRequested[0].
            //
            //   If firstCore is not null
            //     search allCoresArray for the 0-relative firstCoreOffset of the value matching firstCore.
            //     add to cores every core name from firstCoreOffset to the last core in allCoresList.

            String firstCore = null;
            cores = new ArrayList();

            if (options.has(ALL_ARG)) {
                firstCore = OBSERVATION_CORE;
            } else if (options.has(DAILY_ARG)) {
                firstCore = PREQC_CORE;
            } else if (options.has(CORES_ARG)) {
                if ((nodeps) || coresRequested.size() > 1) {
                    cores.addAll(coresRequested);
                } else {
                    firstCore = coresRequested.get(0);
                }
            }

            if (firstCore != null) {
                int firstCoreOffset = 0;
                for (String core : allCoresArray) {
                    if (core.equals(firstCore)) {
                        break;
                    }
                    firstCoreOffset++;
                }
                for (int i = firstCoreOffset; i < allCoresArray.length; i++) {
                    cores.add(allCoresArray[i]);
                }
            }
        } catch (IndexerException ie) {
            if ((ie.getLocalizedMessage() != null) && ( ! ie.getLocalizedMessage().isEmpty())) {
                System.out.println(ie.getLocalizedMessage() + "\n");
            }
            try { parser.printHelpOn(System.out); } catch (Exception e) {}
            throw ie;
        } catch (Exception uoe) {
            Throwable t;
            if (uoe.getLocalizedMessage().contains("is not a recognized option")) {
                t = new UnrecognizedOptionException(uoe);
            } else if (uoe.getLocalizedMessage().contains(" requires an argument")) {
                t = new MissingRequiredArgumentException(uoe);
            } else if (uoe.getLocalizedMessage().contains("Missing required option(s)")) {
                t = new MissingRequiredArgumentException(uoe);
            } else {
                t = uoe;
            }

            try {
                if ((uoe.getLocalizedMessage() != null) && ( ! uoe.getLocalizedMessage().isEmpty())) {
                    System.out.println(uoe.getLocalizedMessage() + "\n");

                }

                parser.formatHelpWith( new IndexManagerHelpFormatter() );
                parser.printHelpOn(System.out);

            } catch (Exception e) {}
            throw new IndexerException(t);
        }
        indexerArgs = new String[] { "--context=" + (String)options.valueOf(CONTEXT_ARG) };
        logger.debug("indexer config file: '" + indexerArgs[0] + "'");

        return options;
    }

    public static void main(String[] args) throws IndexerException {
        int retVal = mainReturnsStatus(args);
        if (retVal != STATUS_OK) {
            throw new IndexerException("Build failed: " + getStatusCodeName(retVal));
        }
    }

    public static int mainReturnsStatus(String[] args) {
        try {
            IndexerManager manager = new IndexerManager();
            manager.initialise(args);
            manager.run();

        } catch (IndexerException ie) {
            logExceptions(ie);
            if (ie.getCause() instanceof NoDepsException) {
                return STATUS_NO_DEPS;
            } else if (ie.getCause() instanceof MissingRequiredArgumentException) {
                return STATUS_NO_ARGUMENT;
            } else if (ie.getCause() instanceof UnrecognizedOptionException) {
                return STATUS_UNRECOGNIZED_OPTION;
            } else if (ie.getCause() instanceof InvalidCoreNameException) {
                return STATUS_INVALID_CORE_NAME;
            } else if (ie.getCause() instanceof ValidationException) {
                return STATUS_VALIDATION_ERROR;
            } else if (ie.getCause() instanceof MissingRequiredArgumentException) {
                return STATUS_NO_ARGUMENT;
            }

            return -1;
        }

        return STATUS_OK;
    }

    private static void logExceptions(IndexerException ie) {
        // Print out the exceptions.
        if (ie.getLocalizedMessage() != null) {
            System.out.println("EXCEPTION: IndexerManager: " + ie.getLocalizedMessage());
        }
        int i = 0;
        Throwable t = ie.getCause();
        while (t != null) {
            StringBuilder errMsg = new StringBuilder("Level " + i + ": ");
            if (t.getLocalizedMessage() != null) {
                errMsg.append(t.getLocalizedMessage());
            } else {
                errMsg.append("<null>");
            }
            System.out.println("ERROR: IndexerManager: " + errMsg.toString());
            i++;
            t = t.getCause();
        }
    }

    /**
     * Print the jvm memory configuration.
     */
    private void printJvmMemoryConfiguration() {
        final int mb = 1024*1024;
        Runtime runtime = Runtime.getRuntime();
        DecimalFormat formatter = new DecimalFormat("#,###");
        logger.info("Used memory:  {}", (formatter.format(runtime.totalMemory() - runtime.freeMemory() / mb)));
        logger.info("Free memory : {}", formatter.format(runtime.freeMemory()));
        logger.info("Total memory: {}", formatter.format(runtime.totalMemory()));
        logger.info("Max memory:   {}\n", formatter.format(runtime.maxMemory()));
    }

    /**
     * Represents an execution status row. Used to display execution status in
     * a readable format. Example:
     *  "preqc started xxx. Finshed (OK) yyy. Elapsed time: hh:mm:ss".
     */
    private class ExecutionStatsRow {
        private String coreName;
        private RunResult runResult;
        private Long startTimeInMs;
        private Long endTimeInMs;

        public ExecutionStatsRow() {
            this("<undefined>", RunResult.FAIL, 0, 0);
        }

        public ExecutionStatsRow(String coreName, RunResult runResult, long startTimeInMs, long endTimeInMs) {
            this.coreName = coreName;
            this.runResult = runResult;
            this.startTimeInMs = startTimeInMs;
            this.endTimeInMs = endTimeInMs;
        }

        @Override
        public String toString() {
            SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            StringBuilder sb = new StringBuilder();
            Formatter formatter = new Formatter(sb);
            long millis = endTimeInMs - startTimeInMs;
            String elapsed = commonUtils.msToHms(millis);
            formatter.format("%20s started %s. Finished (%s) %s. Elapsed time: %s",
                             coreName, dateFormatter.format(startTimeInMs), runResult.name(),
                             dateFormatter.format(endTimeInMs), elapsed);

            return sb.toString();
        }
    }

    private class ExecutionStatsList {
        private final List<ExecutionStatsRow> rows = new ArrayList();

        public ExecutionStatsList() {

        }

        public ExecutionStatsList add(ExecutionStatsRow row) {
            rows.add(row);

            return this;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            if ((rows == null) || (rows.isEmpty())) {
                sb.append("<empty>");
            } else {
                for (ExecutionStatsRow row : rows) {
                    sb.append(row.toString());
                    sb.append("\n");
                }
                sb.append("\n");
                sb.append("Total build time: ");
                String elapsed = commonUtils.msToHms(rows.get(rows.size() - 1).endTimeInMs - rows.get(0).startTimeInMs);
                sb.append(elapsed);
            }

            return sb.toString();
        }
    }

    public class IndexManagerHelpFormatter implements HelpFormatter {
        private String errorMessage;

        @Override
        public String format( Map<String, ? extends OptionDescriptor> options ) {
            String buffer =
                    "Usage: IndexerManager --context=aaa\n" +
                    "     --all\n" +
                    "   | --daily\n" +
                    "   | --cores=aaa [--nodeps]\n" +
                    "   | --cores=aaa,bbb[,ccc [, ...]]\n" +
                    "   \n" +
                    "where aaa is the context file name (should be on the classpath)\n" +
                    "and aaa, bbb, and ccc are cores chosen from the list shown below." +
                    "\n" +
                    "if '--all' is specified, all cores from experiment to autosuggest are built.\n" +
                    "if '--daily' is specified, all cores from preqc to autosuggest are built.\n" +
                    "if ('--core=aaa' is specified, all cores from aaa to autosuggest are built.\n" +
                    "if ('--cores=aaa --nodeps' is specified, ony core 'aaa' is built.\n" +
                    "if ('--cores=aaa,bbb[,ccc [, ...]] is specified (i.e. 2 or more cores), only\n" +
                    "   the specified cores are built, and in the order specified.\n" +
                    "   NOTE: specifying --nodeps with multiple cores is superfluous and is ignored,\n" +
                    "         as nodeps is the default for this case.\n" +
                    "\n" +
                    "Core list (in priority build order):\n" +
                    "   experiment\n" +
                    "   genotype-phenotype\n" +
                    "   statistical-result\n" +
                    "   preqc\n" +
                    "   allele\n" +
                    "   images\n" +
                    "   impc_images\n" +
                    "   mp\n" +
                    "   ma\n" +
                    "   pipeline\n" +
                    "   gene\n" +
                    "   disease\n" +
                    "   autosuggest\n";

            return buffer;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }
    }
}