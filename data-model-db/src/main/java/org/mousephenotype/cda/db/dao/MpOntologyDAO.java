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

package org.mousephenotype.cda.db.dao;

import org.apache.commons.lang3.StringUtils;
import org.mousephenotype.cda.annotations.ComponentScanNonParticipant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

/**
 * This class encapsulates the code and data necessary to serve a Mammalian
 * Phenotype ontology.
 * 
 * @author mrelac
 */
@Repository
@Transactional
@ComponentScanNonParticipant
public class MpOntologyDAO extends OntologyDAO {

    private final Logger logger = LoggerFactory.getLogger(this.getClass()); 

    protected Map<String, List<String>>   mpToAnatomy = new HashMap<>(); //<mpId, <anatomyIds>>
    
    public MpOntologyDAO() {
        
    }
    
    @Override
    @PostConstruct
    public void initialize() throws RuntimeException {
        super.initialize();
        try{
        	mpToAnatomy = populateMpMappings() ;
        } catch (Exception e){
        	e.printStackTrace();
        }
    }
    @Override
    public List<String> getAnatomyMappings(String mpId){
    	return mpToAnatomy.get(mpId);
    }
    
    public Map<String, List<String>>  getMpToAnatomyMap(){
    	return mpToAnatomy;
    }
    
    private Map<String, List<String>> populateMpMappings() 
    throws SQLException {
    	    	
   	        Map<String, List<String>> beans = new HashMap<>();
            Connection ontoDbConnection = ontodbDataSource.getConnection();
            String q = "select mp.term_id, ti.term_id as ma_term_id from mp_mappings mp inner join ma_term_infos ti on mp.mapped_term_id=ti.term_id and mp.ontology='MA'";
            PreparedStatement ps = ontoDbConnection.prepareStatement(q);
            ResultSet rs = ps.executeQuery();
            int count = 0;
            while (rs.next()) {
                String mpId = rs.getString("term_id");
                String anatomyTermId = rs.getString("ma_term_id");
                if ( ! beans.containsKey(mpId)) {
                    beans.put(mpId, new ArrayList<String>());
                }
                beans.get(mpId).add(anatomyTermId);
                count ++;
            }
            logger.debug(" Added {} anatomy mapping ids.", count);

            return beans;
    
    }

    
    /**
     * Returns the set of descendent graphs for the given id.
     * @param id the mp id to query
     * @return the set of descendent graphs for the given id.
     */
    @Override
    public final List<List<String>> getDescendentGraphs(String id) {
        String nodeIds = StringUtils.join(id2nodesMap.get(id), ",");
        String query =
            "SELECT *\n"
          + "FROM mp_node_subsumption_fullpath_concat\n"
          + "WHERE node_id IN (" + nodeIds + ")\n";
        
        return getDescendentGraphsInternal(query);
    }
    
    
    // PROTECTED METHODS
    
    
    /**
     * Populate all terms, keyed by id.
     * 
     * Side Effects: this method populates a map, indexed by id, of each id's 
     *               node ids, which is later used to create the ancestor list.
     * 
     * @throws SQLException 
     */
    @Override
    protected final void populateAllTerms() throws SQLException {
        String query =
            "SELECT\n"
          + "  n2t.term_id               AS termId\n"
          + ", GROUP_CONCAT(DISTINCT n2t.node_id) AS nodes\n"
          + ", ti.name                   AS termName\n"
          + ", ti.definition             AS termDefinition\n"
          + ", GROUP_CONCAT(DISTINCT alt.alt_id) AS alt_ids\n"
          + "FROM mp_node2term n2t\n"
          + "LEFT OUTER JOIN mp_term_infos ti ON ti.term_id = n2t.term_id\n"
          + "LEFT OUTER JOIN mp_alt_ids alt ON ti.term_id = alt.term_id\n"
          + "WHERE n2t.term_id != 'MP:0000001'\n"
          + "GROUP BY n2t.term_id\n"
          + "ORDER BY n2t.term_id, n2t.node_id\n";

        populateAllTerms(query);
    }
    
    /**
     * Populates each node's ancestor hash.
     * 
     * @throws SQLException 
     */
    @Override
    protected void populateAncestorMap() throws SQLException {
        String query =
            "SELECT *\n"
          + "FROM mp_node_backtrace_fullpath\n";
        
        populateAncestorMap(query);
    }
    
    /**
     * Populates the node2term hash with the term matching each node.
     * 
     * @throws SQLException 
     */
    @Override
    protected void populateNode2TermMap() throws SQLException {
        String query =
            "SELECT *\n"
          + "FROM mp_node2term\n"
          + "ORDER BY term_id\n";
        
        populateNode2TermMap(query);
    }
    
    /**
     * Query the database, returning a map of all synonyms indexed by term id
     *
     * @throws SQLException when a database exception occurs
     */
    @Override
    protected final void populateSynonyms() throws SQLException {
        String query =
            "SELECT\n"
          + "  term_id\n"
          + ", syn_name\n"
          + "FROM mp_synonyms";
        
        populateSynonyms(query);
    }
}