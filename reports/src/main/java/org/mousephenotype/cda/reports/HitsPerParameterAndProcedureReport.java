/*******************************************************************************
 * Copyright © 2015 EMBL - European Bioinformatics Institute
 * <p>
 * Licensed under the Apache License, Version 2.0 (the
 * "License"); you may not use this targetFile except in compliance
 * with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the
 * License.
 ******************************************************************************/

package org.mousephenotype.cda.reports;

import org.apache.commons.lang3.ClassUtils;
import org.apache.solr.client.solrj.SolrServerException;
import org.mousephenotype.cda.reports.support.ReportException;
import org.mousephenotype.cda.solr.service.PostQcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;

import java.beans.Introspector;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Hits per parameter and procedure report.
 *
 * Created by mrelac on 24/07/2015.
 */
@SpringBootApplication
@Component
public class HitsPerParameterAndProcedureReport extends AbstractReport {

    protected Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    @Qualifier("postqcService")
    PostQcService genotypePhenotypeService;

    @Autowired


    public HitsPerParameterAndProcedureReport() {
        super();
    }

    public static void main(String args[]) {
        SpringApplication.run(HitsPerParameterAndProcedureReport.class, args);
    }

    @Override
    public String getDefaultFilename() {
        return Introspector.decapitalize(ClassUtils.getShortClassName(this.getClass().getSuperclass()));
    }

    @Override
    public void run(String[] args) throws ReportException {
        initialise(args);

        long start = System.currentTimeMillis();

        //Columns:
        //	parameter name | parameter stable id | number of significant hits

        List<List<String[]>> result = new ArrayList<>();
        try {
            List<String[]> parameters = new ArrayList<>();
            String[] headerParams  ={"Parameter Id", "Parameter Name", "# significant hits"};
            parameters.add(headerParams);
            parameters.addAll(genotypePhenotypeService.getHitsDistributionByParameter(resources));

            List<String[]> procedures = new ArrayList<>();
            String[] headerProcedures  ={"Procedure Id", "Procedure Name", "# significant hits"};
            procedures.add(headerProcedures);
            procedures.addAll(genotypePhenotypeService.getHitsDistributionByProcedure(resources));

            result.add(parameters);
            result.add(procedures);
            csvWriter.writeAllMulti(result);

        } catch (SolrServerException | InterruptedException | ExecutionException e) {
            throw new ReportException("Exception creating " + this.getClass().getCanonicalName() + ". Reason: " + e.getLocalizedMessage());
        }

        try {
            csvWriter.close();
        } catch (IOException e) {
            throw new ReportException("Exception closing csvWriter: " + e.getLocalizedMessage());
        }

        log.info(String.format("Finished. [%s]", commonUtils.msToHms(System.currentTimeMillis() - start)));
    }
}