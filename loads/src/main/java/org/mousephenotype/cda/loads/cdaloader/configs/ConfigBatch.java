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

import org.mousephenotype.cda.loads.cdaloader.exceptions.CdaLoaderException;
import org.mousephenotype.cda.loads.cdaloader.steps.DatabaseInitialiser;
import org.mousephenotype.cda.loads.cdaloader.steps.Downloader;
import org.mousephenotype.cda.loads.cdaloader.steps.OntologyLoader;
import org.springframework.batch.core.*;
import org.springframework.batch.core.configuration.annotation.*;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

import javax.annotation.Resource;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;

/**
 * Created by mrelac on 12/04/2016.
 */
@Configuration
@EnableBatchProcessing
public class ConfigBatch {

    @Autowired
    public JobBuilderFactory jobBuilderFactory;

    @Autowired
    @JobScope
    public StepBuilderFactory stepBuilderFactory;

    @Autowired
    public JobRepository jobRepository;

    @Autowired
    public DatabaseInitialiser databaseInitialiser;

    @Resource(name = "downloader")
    public List<Downloader> downloader;

    @Resource(name = "ontologyLoaderList")
    public List<OntologyLoader> ontologyLoaderList;



    @Bean
    public Job[] runJobs() throws CdaLoaderException {
        Job[] jobs = new Job[] {
                  databaseInitialiserJob()
//                , downloaderJob()
                , dbLoaderJob()
        };
        DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        String now = dateFormat.format(new Date());

        for (int i = 0; i < jobs.length; i++) {
            Job job = jobs[i];
            try {
                JobInstance instance = jobRepository.createJobInstance("flow_" + now + "_" + i, new JobParameters());
                JobExecution execution = jobRepository.createJobExecution(instance, new JobParameters(), "xxx_" + now + "_" + i);
                job.execute(execution);
            } catch (Exception e) {

                throw new CdaLoaderException(e);
            }
        }

        return jobs;
    }

    public Job databaseInitialiserJob() throws CdaLoaderException {

        return jobBuilderFactory.get("databaseInitialiserJob")
                .incrementer(new RunIdIncrementer())
                .flow(databaseInitialiser.getStep(stepBuilderFactory))
                .end()
                .build();
    }

    public Job downloaderJob() throws CdaLoaderException {

        List<Flow> flows = new ArrayList<>();
        for (int i = 0; i < downloader.size(); i++) {
            Downloader downloader = this.downloader.get(i);
            flows.add(new FlowBuilder<Flow>("subflow_" + i).from(downloader.getStep(stepBuilderFactory)).end());
        }

        FlowBuilder<Flow> flowBuilder = new FlowBuilder<Flow>("splitflow").start(flows.get(0));

        for (int i = 1; i < downloader.size(); i++) {
            SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor(Executors.defaultThreadFactory());
            flowBuilder.split(executor).add(flows.get(i));
        }

        return jobBuilderFactory.get("downloaderJob")
                .incrementer(new RunIdIncrementer())
                .start(flowBuilder.build())
                .end()
                .build();
    }

    public Job dbLoaderJob() throws CdaLoaderException {
        System.out.println("dbLoaderJob");

        List<Flow> flows = new ArrayList<>();
        for (int i = 0; i < ontologyLoaderList.size(); i++) {
            OntologyLoader ontologyLoader = ontologyLoaderList.get(i);
            flows.add(new FlowBuilder<Flow>("subflow_" + i).from(ontologyLoader).end());
        }

        FlowBuilder<Flow> flowBuilder = new FlowBuilder<Flow>("splitflow").start(flows.get(0));
        for (int i = 1; i < ontologyLoaderList.size(); i++) {
            SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor(Executors.defaultThreadFactory());
            flowBuilder.split(executor).add(flows.get(i));
        }

        return jobBuilderFactory.get("dbLoaderJob")
                .incrementer(new RunIdIncrementer())
                .start(flowBuilder.build())
                .end()
                .build();
    }
}