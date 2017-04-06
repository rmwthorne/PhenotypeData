package uk.ac.ebi.phenotype.repository;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;

/**
 * Created by ckchen on 14/03/2017.
 */
@Repository
public interface DiseaseGeneRepository extends Neo4jRepository<DiseaseGene, Long> {

    DiseaseGene findByDiseaseId(String diseaseId);
    DiseaseGene findByDiseaseTerm(String diseaseTerm);

}