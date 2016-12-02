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

package org.mousephenotype.cda.loads.create.extract.dcc;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.mousephenotype.cda.loads.common.DccSqlUtils;
import org.mousephenotype.cda.loads.create.extract.dcc.config.ExtractDccConfigBeans;
import org.mousephenotype.cda.loads.exceptions.DataLoadException;
import org.mousephenotype.dcc.exportlibrary.datastructure.core.specimen.*;
import org.mousephenotype.dcc.exportlibrary.xmlserialization.exceptions.XMLloadingException;
import org.mousephenotype.dcc.utils.xml.XMLUtils;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.Banner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import javax.validation.constraints.NotNull;
import java.sql.SQLException;
import java.util.List;

/**
 * Created by mrelac on 09/02/2016.
 * <p/>
 * This class encapsulates the code and data necessary to load the specified target database with the source dcc
 * specimen files currently found at /usr/local/komp2/phenotype_data/impc. This class is meant to be an executable jar
 * whose arguments describe the profile containing the application.properties, the source file, and the database name.
 */
@Import( {ExtractDccConfigBeans.class })
public class ExtractDccSpecimens implements CommandLineRunner {

    private String datasourceShortName;
    private String filename;
    private final org.slf4j.Logger logger = LoggerFactory.getLogger(this.getClass());

    // Required by the Harwell DCC export utilities
    public static final  String CONTEXT_PATH = "org.mousephenotype.dcc.exportlibrary.datastructure.core.common:org.mousephenotype.dcc.exportlibrary.datastructure.core.procedure:org.mousephenotype.dcc.exportlibrary.datastructure.core.specimen:org.mousephenotype.dcc.exportlibrary.datastructure.tracker.submission:org.mousephenotype.dcc.exportlibrary.datastructure.tracker.validation";

    private String    dbname;

    @NotNull
    @Autowired
    private DataSource dcc;

    @NotNull
    @Autowired
    private DccSqlUtils dccSqlUtils;


    public static void main(String[] args) throws Exception {
        SpringApplication app = new SpringApplication(ExtractDccSpecimens.class);
        app.setBannerMode(Banner.Mode.OFF);
        app.setLogStartupInfo(false);
        app.run(args);
    }

    @Override
    public void run(String... args) throws Exception {

        initialize(args);
        run();
    }

    private void initialize(String[] args) throws DataLoadException {

        OptionParser parser = new OptionParser();

        // parameter to indicate specimen table creation
        parser.accepts("create");

        // parameter to indicate the data source short name (e.g. EuroPhenome, IMPC, 3I, etc)
        parser.accepts("datasourceShortName").withRequiredArg().ofType(String.class);

        // parameter to indicate the name of the file to process
        parser.accepts("filename").withRequiredArg().ofType(String.class);

        // parameter to indicate profile (subdirectory of configfiles containing application.properties)
        parser.accepts("profile").withRequiredArg().ofType(String.class);

        OptionSet options = parser.parse(args);

        if ( ! options.has("datasourceShortName")) {
            String message = "Missing required command-line paraemter 'datasourceShortName'";
            logger.error(message);
            throw new DataLoadException(message);
        }
        datasourceShortName = (String) options.valuesOf("datasourceShortName").get(0);
        filename = (String) options.valuesOf("filename").get(0);

        try {
            dbname = dcc.getConnection().getCatalog();
        } catch (SQLException e) {
            dbname = "Unknown";
        }

        if (options.has("create")) {
            logger.info("Dropping and creating dcc specimen tables for database {} - begin", dbname);
            org.springframework.core.io.Resource r = new ClassPathResource("scripts/dcc/createSpecimen.sql");
            ResourceDatabasePopulator            p = new ResourceDatabasePopulator(r);
            p.execute(dcc);
            logger.info("Dropping and creating dcc specimen tables for database {} - complete", dbname);
        }

        logger.debug("Loading specimen file {}", filename);
    }

    private void run() throws DataLoadException {
        int                  totalSpecimens        = 0;
        int                  totalSpecimenFailures = 0;
        List<CentreSpecimen> centerSpecimens;

        try {
            centerSpecimens = XMLUtils.unmarshal(ExtractDccSpecimens.CONTEXT_PATH, CentreSpecimenSet.class, filename).getCentre();
        } catch (Exception e) {
            throw new DataLoadException(e);
        }

        if (centerSpecimens.size() == 0) {
            logger.error("{} failed to unmarshall", filename);
            throw new DataLoadException(filename + " failed to unmarshall.", new XMLloadingException());
        }

        logger.debug("There are {} center specimen sets in specimen file {}", centerSpecimens.size(), filename);

        for (CentreSpecimen centerSpecimen : centerSpecimens) {
            logger.debug("Parsing specimens for center {}", centerSpecimen.getCentreID());

            for (Specimen specimen : centerSpecimen.getMouseOrEmbryo()) {
                try {
                    insertSpecimen(specimen, datasourceShortName, centerSpecimen);
                    totalSpecimens++;
                } catch (Exception e) {
                    logger.error("ERROR IMPORTING SPECIMEN. CENTER: {}. SPECIMEN: {}. SPECIMEN SKIPPED. ERROR:\n{}", centerSpecimen.getCentreID(), specimen, e.getLocalizedMessage());
                    totalSpecimenFailures++;
                }
            }
        }
    }

    @Transactional
    private void insertSpecimen(Specimen specimen, String datasourceShortName, CentreSpecimen centerSpecimen) throws DataLoadException {

        Long specimenPk;

        // center
        long centerPk = dccSqlUtils.getCenterPk(centerSpecimen.getCentreID().value(), specimen.getPipeline(), specimen.getProject());
        if (centerPk < 1) {
            centerPk = dccSqlUtils.insertCenter(centerSpecimen.getCentreID().value(), specimen.getPipeline(), specimen.getProject());
        }

        // statuscode
        if (specimen.getStatusCode() != null) {
            dccSqlUtils.selectOrInsertStatuscode(specimen.getStatusCode().getValue(), specimen.getStatusCode().getDate());
        }

        // Correct the europhenome specimen colony id
        if (datasourceShortName.equals("EuroPhenome")) {
            specimen.setColonyID(dccSqlUtils.correctEurophenomeColonyId(specimen.getColonyID()));
        }

        // specimen
        specimen = dccSqlUtils.insertSpecimen(specimen, datasourceShortName);
        specimenPk = specimen.getHjid();

        // embryo or mouse
        if (specimen instanceof Embryo) {
            dccSqlUtils.insertEmbryo((Embryo) specimen, specimenPk);
        } else  if (specimen instanceof Mouse) {
            dccSqlUtils.insertMouse((Mouse) specimen, specimenPk);
        } else {
            throw new DataLoadException("Unknown specimen type '" + specimen.getClass().getSimpleName());
        }

        // genotype
        for (Genotype genotype : specimen.getGenotype()) {
            dccSqlUtils.insertGenotype(genotype, specimenPk);
        }

        // parentalStrain
        for (ParentalStrain parentalStrain : specimen.getParentalStrain()) {
            dccSqlUtils.insertParentalStrain(parentalStrain, specimenPk);
        }

        // chromosomalAlteration
        if ( ! specimen.getChromosomalAlteration().isEmpty()) {
            throw new DataLoadException("chromosomalAlteration is not yet supported. Records found!");
        }

        // center_specimen
        dccSqlUtils.insertCenter_specimen(centerPk, specimenPk);

        // relatedSpecimen
        String centerId = dccSqlUtils.getCenterId(centerPk);
        for (RelatedSpecimen relatedSpecimen : specimen.getRelatedSpecimen()) {
            dccSqlUtils.insertRelatedSpecimen(centerId, specimenPk, specimen.getSpecimenID(), relatedSpecimen.getRelationship().value(), relatedSpecimen.getSpecimenID());
        }
    }
}