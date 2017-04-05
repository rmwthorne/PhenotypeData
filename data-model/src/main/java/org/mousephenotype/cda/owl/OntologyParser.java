package org.mousephenotype.cda.owl;

import org.json.JSONObject;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.search.EntitySearcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.manchester.cs.owl.owlapi.OWLObjectPropertyImpl;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class OntologyParser {

    private static final Logger logger = LoggerFactory.getLogger(OntologyParser.class);

    OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
    OWLDataFactory factory = manager.getOWLDataFactory();
    OWLOntology ontology;

    OWLAnnotationProperty LABEL_ANNOTATION;
    OWLAnnotationProperty ALT_ID;
    OWLAnnotationProperty X_REF;
    OWLAnnotationProperty REPLACEMENT;
    OWLAnnotationProperty CONSIDER;
    OWLAnnotationProperty TERM_REPLACED_BY;
    ArrayList<OWLAnnotationProperty> IS_OBSOLETE;
    ArrayList<OWLAnnotationProperty> SYNONYM_ANNOTATION;
    ArrayList<OWLAnnotationProperty> DEFINITION_ANNOTATION;
    Set<OWLPropertyExpression> PART_OF;

    private Map<String, Set<OWLClass>> ancestorsCache;
    private Map<String, OntologyTermDTO> termMap = new HashMap<>(); // OBO-style ids because that's what we index.
    private Map<String, OWLClass> classMap = new HashMap<>(); // OBO id to OWLClass object. We need this to avoid pre-loading of referrenced classes (MAs from MP)
    private Set<String> termsInSlim; // <ids of classes on slim>
    private TreeSet<String> topLevelIds;
    private List<OntologyTermDTO> toplevelterms;
    private Map<Integer, OntologyTermDTO> nodeTermMap = new HashMap<>(); // <nodeId, ontologyId>

    /**
     *
     * @param pathToOwlFile
     * @param prefix
     * @param topLevelIds ontology ids to be used as top level (selected top level); Only need to pass this if you want top levels or intermediate terms up to the top level;
     * @param wantedIds ids to be used for the slim. If null whole ontology will be used.
     * @throws OWLOntologyCreationException
     */
    public OntologyParser(String pathToOwlFile, String prefix, Collection<String> topLevelIds, Set<String> wantedIds)
    throws OWLOntologyCreationException, IOException, OWLOntologyStorageException {

        setUpParser(pathToOwlFile);

        if (wantedIds != null){
            getTermsInSlim(wantedIds, prefix);
        }

        if (topLevelIds != null) {
            this.topLevelIds = new TreeSet<>(); // sort alphabetically
            this.topLevelIds.addAll(topLevelIds);
        }

        Set<OWLClass> allClasses = ontology.getClassesInSignature();
        for (OWLClass cls : allClasses){
            if (startsWithPrefix(cls, prefix)){
                OntologyTermDTO term = getDTO(cls, prefix);
                term.setEquivalentClasses(getEquivaletNamedClasses(cls, prefix));
                termMap.put(term.getAccessionId(), term);
                classMap.put(term.getAccessionId(), cls);
            }
        }
    }

    public List<OntologyTermDTO> getTopLevelTerms(){
        if (toplevelterms == null){
            toplevelterms = new ArrayList<>();
            for (String topLevelId : topLevelIds){
                toplevelterms.add(termMap.get(topLevelId));
            }
        }
        return toplevelterms;

    }

    /**
     * Computes paths in the format needed for TreeJs, for the ontology browsers.
     * [!] This is not computed by default. If you want the trees, call this method on the parser first.
     */
    public void fillJsonTreePath(String rootId, String pathToPage,  Map<String, Integer>  countsMap, List<String> treeBrowserTopLevels, Boolean withPartOf){

        OWLClass root = classMap.get(rootId);
        // fill lists with nodes on path
        fillJsonTreePath(root, new ArrayList<>(), withPartOf, rootId);
        // use node list to generate JSON documents
        for ( String id : getTermsInSlim()) {
            OntologyTermDTO term = getOntologyTerm(id);
            List<JSONObject> searchTree = TreeJsHelper.createTreeJson(term, pathToPage, this, countsMap, treeBrowserTopLevels);
            term.setSeachJson(searchTree.toString());
            String scrollNodeId = TreeJsHelper.getScrollTo(searchTree);
            term.setScrollToNode(scrollNodeId);
            List<JSONObject> childrenTree = TreeJsHelper.getChildrenJson(term, pathToPage, this, countsMap);
            term.setChildrenJson(childrenTree.toString());
        }
    }


    /**
     *
     * @param cls - start from root.
     */
    private void fillJsonTreePath (OWLClass cls, List<Integer> pathFromRoot, Boolean withPartOf, String rootId){

        if (cls != null && (!getIdentifierShortForm(cls).equalsIgnoreCase(rootId) || nodeTermMap.size() == 0)){ // avoid starting over from root

            int nodeId = nodeTermMap.size();
            OntologyTermDTO ontDTO = getOntologyTerm(getIdentifierShortForm(cls));

            pathFromRoot.add(nodeId);
            Set<OWLClass> childrenPartOf = getChildren(cls, withPartOf);
            ontDTO.addPathsToRoot(nodeId, new ArrayList<>(pathFromRoot));
            nodeTermMap.put(nodeId, ontDTO);

            if (childrenPartOf != null){
                for (OWLClass child: childrenPartOf){
                    fillJsonTreePath(child, new ArrayList<>(pathFromRoot), withPartOf, rootId);
                }
            }
        }

    }


    private Boolean startsWithPrefix(OWLClass cls, Collection<String> prefix){
        if (prefix == null){
            return true; // when prefix is passed as null it means we don't care about it; take everything
        }
        for (String p: prefix) {
            if (!getIdentifierShortForm(cls).startsWith(p + ":")) {
                return false;
            }
        }
        return true;
    }

    private Boolean startsWithPrefix(OWLClass cls, String prefix){
        return (prefix == null || getIdentifierShortForm(cls).startsWith(prefix + ":"));
    }


    public List<OntologyTermDTO> getTerms(){
        return termMap.values().stream().collect(Collectors.toList());
    }


    /**
     * The getters by id (below) are needed until we move to an OWL API indexer. Should get rid of them and use the getters on the objects directly after that.
     */
    public OntologyTermDTO getOntologyTerm (String accessionId){

        return termMap.get(accessionId);

    }

    /**
     * Only works if you filled the nodes first
     * @param nodeId
     * @return
     */
    public OntologyTermDTO getOntologyTerm (Integer nodeId){

        return nodeTermMap.get(nodeId);

    }


    /**
     * Set up properties for parsing ontology.
     * @throws OWLOntologyCreationException
     */
    private void setUpParser(String pathToOwlFile)
            throws OWLOntologyCreationException, OWLOntologyStorageException {

        LABEL_ANNOTATION = factory.getRDFSLabel();

        ALT_ID = factory.getOWLAnnotationProperty(IRI.create("http://www.geneontology.org/formats/oboInOwl#hasAlternativeId"));

        X_REF = factory.getOWLAnnotationProperty(IRI.create("http://www.geneontology.org/formats/oboInOwl#hasDbXref"));

        SYNONYM_ANNOTATION = new ArrayList<>();
        SYNONYM_ANNOTATION.add(factory.getOWLAnnotationProperty(IRI.create("http://www.geneontology.org/formats/oboInOwl#hasExactSynonym")));
        SYNONYM_ANNOTATION.add(factory.getOWLAnnotationProperty(IRI.create("http://www.geneontology.org/formats/oboInOwl#hasNarrowSynonym")));
        SYNONYM_ANNOTATION.add(factory.getOWLAnnotationProperty(IRI.create("http://www.geneontology.org/formats/oboInOwl#hasRelatedSynonym")));
        SYNONYM_ANNOTATION.add(factory.getOWLAnnotationProperty(IRI.create("http://www.geneontology.org/formats/oboInOwl#hasBroadSynonym")));

        DEFINITION_ANNOTATION = new ArrayList<>();
        DEFINITION_ANNOTATION.add(factory.getOWLAnnotationProperty(IRI.create("http://purl.obolibrary.org/obo/IAO_0000115")));

        IS_OBSOLETE = new ArrayList<>();
        IS_OBSOLETE.add(factory.getOWLAnnotationProperty(IRI.create("http://www.geneontology.org/formats/oboInOwl#isObsolete")));
        IS_OBSOLETE.add(factory.getOWLAnnotationProperty(IRI.create("http://www.w3.org/2002/07/owl#deprecated")));

        REPLACEMENT = factory.getOWLAnnotationProperty(IRI.create("http://purl.obolibrary.org/obo/IAO_0100001"));

        CONSIDER = factory.getOWLAnnotationProperty(IRI.create("http://www.geneontology.org/formats/oboInOwl#consider"));

        PART_OF = new HashSet<>();
        PART_OF.add(new OWLObjectPropertyImpl(IRI.create("http://purl.obolibrary.org/obo/ma#part_of")));
        PART_OF.add(new OWLObjectPropertyImpl(IRI.create("http://purl.obolibrary.org/obo/emap#part_of")));
        PART_OF.add(new OWLObjectPropertyImpl(IRI.create("http://purl.obolibrary.org/obo/emapa#part_of")));
        PART_OF.add(new OWLObjectPropertyImpl(IRI.create("http://purl.obolibrary.org/obo/part_of")));
        PART_OF.add(new OWLObjectPropertyImpl(IRI.create("http://purl.obolibrary.org/obo/BFO_0000050")));

        TERM_REPLACED_BY = factory.getOWLAnnotationProperty(IRI.create("http://purl.obolibrary.org/obo/IAO_0100001"));

        ontology = manager.loadOntologyFromOntologyDocument(IRI.create(new File(pathToOwlFile)));

        ancestorsCache = new HashMap<>();

    }

    /**
     * @param maxLevels how many levels to go down for subclasses; -1 for all.
     * @param cls
     * @return Set of labels + synonyms of all subclasses {maxLevels} away from cls. !! This method can be expensive so  values are not pe-loaded. Should only be used on leaf nodes !!
     */
    public Set<String> getNarrowSynonyms(OntologyTermDTO cls, int maxLevels){

        Set<OWLClass> descendents = new HashSet<>();
        TreeSet<String> res = new TreeSet<String>();
        descendents = getDescendentsPartOf(cls.getCls(), 1, 0, descendents);

        for (OWLClass desc : descendents){
            res.addAll(getSynonyms(desc));
            res.add(getLabel(desc));
        }

        return res;
    }


    private Set<OWLClass> getDescendentsPartOf(OWLClass cls, int maxLevels, int currentLevel, Set<OWLClass> children ){

        Set<OWLClass> subclasses = getChildren(cls, true);
        currentLevel ++;
        if (!subclasses.isEmpty()){
            children.addAll(subclasses);
            if (currentLevel < maxLevels || maxLevels < 0){
                for (OWLClass subClass: subclasses ){
                    getDescendentsPartOf(subClass, maxLevels, currentLevel, children);
                }
            }
        }
        return children;

    }


    /**
     *  OWL identifiers look like "http://purl.obolibrary.org/obo/MA_0100084" and we want "MA:0100084", which is the old OBO style
     * @param cls
     * @return OBO-style id
     */
    protected String getIdentifierShortForm (OWLClass cls){

        String id = cls.getIRI().toString();
        return id.split("/|#")[id.split("/|#").length-1].replace("_", ":");

    }


    /**
     *
     * @param cls
     * @return Set of alternative ids for the given class OR empty set when none available
     */
    private Set<String> getAltIds(OWLClass cls){

        Set<String> altIds = new HashSet<>();
        Collection<OWLAnnotation> annotations = EntitySearcher.getAnnotations(cls, ontology, ALT_ID);
        if (annotations != null && !annotations.isEmpty()){
            for (OWLAnnotation ann: annotations) {
                OWLLiteral altId = ((OWLLiteral)ann.getValue());
                altIds.add(altId.getLiteral());
            }
        }
        return altIds;

    }


    /**
     *
     * @param cls
     * @return term name (class label)
     */
    private String getLabel (OWLClass cls){
        if (EntitySearcher.getAnnotations(cls,ontology, LABEL_ANNOTATION) != null && !EntitySearcher.getAnnotations(cls,ontology, LABEL_ANNOTATION).isEmpty()){
            OWLLiteral label = ((OWLLiteral)EntitySearcher.getAnnotations(cls,ontology, LABEL_ANNOTATION).iterator().next().getValue());
            if (label != null){
                return label.getLiteral();
            }
        }
        return "";

    }


    /**
     *
     * @param cls
     * @return term definition/description
     */
    private String getDefinition (OWLClass cls){

        for (OWLAnnotationProperty definition: DEFINITION_ANNOTATION){
            for (OWLAnnotation annotation : EntitySearcher.getAnnotations(cls,ontology, definition)) {
                if (annotation.getValue() instanceof OWLLiteral) {
                    OWLLiteral val = (OWLLiteral) annotation.getValue();
                    return val.getLiteral();
                }
            }
        }
        return null;
    }


    /**
     * @param cls
     * @return List of synonyms as String
     */
    private Set<String> getSynonyms (OWLClass cls){

        Set<String> synonyms = new HashSet<>();

        for (OWLAnnotationProperty synonym: SYNONYM_ANNOTATION){
            for (OWLAnnotation annotation : EntitySearcher.getAnnotations(cls,ontology, synonym)) {
                if (annotation.getValue() instanceof OWLLiteral) {
                    OWLLiteral val = (OWLLiteral) annotation.getValue();
                    synonyms.add(val.getLiteral());
                }
            }
        }

        return synonyms;
    }


    private OntologyTermDTO getDTO(OWLClass cls, String prefix){

        OntologyTermDTO term = new OntologyTermDTO();
        term.setAccessionId(getIdentifierShortForm(cls)); // i.e. MA:0100084
        term.setName(getLabel(cls));
        term.setDefinition(getDefinition(cls));
        term.setSynonyms(getSynonyms(cls));
        term.setObsolete(isObsolete(cls));
        term.setCls(cls);
        if (term.isObsolete()){
            String replacementId = getReplacementId(cls);
            if((replacementId != null)){
                term.setReplacementAccessionId(replacementId);
            }
            Set<String> considerIds = getConsiderIds(cls);
            if(considerIds != null && considerIds.size() > 0){
                term.setConsiderIds(considerIds);
            }
        }
        Set<String> altIds = getAltIds(cls);
        if ( altIds!= null && altIds.size() > 0){
            term.setAlternateIds(altIds);
        }
        addChildrenInfo(cls, term, prefix);
        addParentInfo(cls, term, prefix);
        addIntermediateInfo(cls, term, prefix);
        addTopLevelInfo(cls, term);

        return term;
    }


    /**
     *
     * @param cls
     * @return Set of equivalent *named* classes.
     */
    private Set<OntologyTermDTO> getEquivaletNamedClasses(OWLClass cls, String prefix){

        Set<OntologyTermDTO> eqClasses = new HashSet<>();
        for (OWLClassExpression classExpression : EntitySearcher.getEquivalentClasses(cls, ontology)){
            if (classExpression.isClassExpressionLiteral() && !getIdentifierShortForm(classExpression.asOWLClass()).startsWith(prefix + ":")){
                eqClasses.add(getDTO(classExpression.asOWLClass(), prefix));
            }
        }
        return  eqClasses;
    }


    public Set<String> getReferencedClasses(String clsId, Set<OWLObjectPropertyImpl> viaProperties, String prefixOfReferrencedClass){

        Set<OWLClass> res = new HashSet<>();

        if (classMap.containsKey(clsId)) {
            OWLClass cls = classMap.get(clsId);
            // get both equivalent classes and subclass of
            Collection<OWLClassExpression> expressions = EntitySearcher.getEquivalentClasses(cls, ontology);
            expressions.addAll(EntitySearcher.getSuperClasses(cls, ontology));
            Collection<OWLClassExpression> expressionsFromIntersection = new HashSet<>();

            for (OWLClassExpression classExpression : expressions) {
                // Most likely case, something like PATO_0000051 and ('inheres in' some MA_0000195) and (qualifier some PATO_0000460)
                // Treat this case first as it adds new expressions to the list processed in the nex
                if (classExpression instanceof OWLObjectIntersectionOf) {
                    OWLObjectIntersectionOf intersection = (OWLObjectIntersectionOf) classExpression;
                    // break down into simple class expressions and dd them to the set to be analysed
                    expressionsFromIntersection.addAll(intersection.asConjunctSet());
                }
            }
            expressions.addAll(expressionsFromIntersection);
            for (OWLClassExpression classExpression : expressions) {
                // Simplest case, something like ('inheres in' some MA_0000195)
                if (classExpression instanceof OWLObjectSomeValuesFrom){
                    OWLObjectSomeValuesFrom svf = (OWLObjectSomeValuesFrom) classExpression;
                    if (viaProperties.contains(svf.getProperty().asOWLObjectProperty())){
                        if (svf.getFiller() instanceof OWLNamedObject){
                            res.add(svf.getFiller().asOWLClass());
                        }
                    }
                }
            }
        }
        // Return the ids, but filter for the right prefix
        return  res.stream().filter(cls -> {return startsWithPrefix(cls, prefixOfReferrencedClass);})
            .map(cls -> {return getIdentifierShortForm(cls);}).collect(Collectors.toSet());
    }


    public Set<String> getTermsInSlim(){

        return termsInSlim;

    }

    /**
     *
     * @param wantedIDs
     * @param prefix
     * @return Set of class ids that belond in the slim
     * @throws IOException
     * @throws OWLOntologyStorageException
     */
    protected Set<String> getTermsInSlim(Set<String> wantedIDs, String prefix)
            throws IOException, OWLOntologyStorageException {

        // Cache it in termsInSlim so we don't have to re-compute this every time
        if (termsInSlim != null){
            return termsInSlim;
        }

        // Add replacement ids for deprecated classes to wanted ids
        for (OWLClass cls : ontology.getClassesInSignature()) {

            // add replacement terms for obslete terms
            if (!cls.getIRI().isNothing() && EntitySearcher.getAnnotations(cls, ontology, TERM_REPLACED_BY) != null && wantedIDs.contains(getIdentifierShortForm(cls))) {
                for (OWLAnnotation annotation : EntitySearcher.getAnnotations(cls, ontology, TERM_REPLACED_BY)) {
                    if (annotation.getValue() instanceof OWLLiteral) {
                        wantedIDs.add(((OWLLiteral) annotation.getValue()).getLiteral());
                    }
                }
            }
            // remove obseolete terms from slim
            if (isObsolete(cls)) {
                wantedIDs.remove(getIdentifierShortForm(cls));
            }
        }

        // Add the "seed" terms and their ancestors to the slim set.
        Set<String> classesInSlim = new HashSet<>();
        for (OWLClass cls : ontology.getClassesInSignature()) {
            if (wantedIDs.contains(getIdentifierShortForm(cls))) {
                if (startsWithPrefix(cls, prefix)) {
                    classesInSlim.add(getIdentifierShortForm(cls));
                    classesInSlim.addAll(getClassAncestors(cls, null).stream().map(item -> {return getIdentifierShortForm(item);}).collect(Collectors.toSet()));
                }
            }
        }

        termsInSlim = classesInSlim;
        logger.info("Set termsInSlim " + termsInSlim.size());
        return classesInSlim;

    }

    protected OWLClass getOwlClass(String shortFormId){
        return classMap.get(shortFormId);
    }

    /**
     *
     * @param cls
     * @param prefixes
     * @return
     */
    private Set<OWLClass> getClassAncestors(OWLClass cls, Set<String> prefixes){

        if( ancestorsCache.containsKey(getIdentifierShortForm(cls))){
            return ancestorsCache.get(getIdentifierShortForm(cls));
        }
        Set<OWLClass> ancestorIds = new HashSet<>();
        ancestorIds.addAll(getClassAncestors(cls, prefixes, ancestorIds));
        ancestorsCache.put(getIdentifierShortForm(cls), ancestorIds);

        return ancestorIds;
    }


    protected String getXref(OWLClass cls,  String prefixOfCrossRef) {

        if (!cls.getIRI().isNothing() && EntitySearcher.getAnnotations(cls, ontology, X_REF) != null) {
            for (OWLAnnotation annotation : EntitySearcher.getAnnotations(cls, ontology, X_REF)) {
                if (annotation.getValue() instanceof OWLLiteral) {
                    OWLLiteral val = (OWLLiteral) annotation.getValue();
                    String id = val.getLiteral().replace(":", "_");
                    if (id.startsWith(prefixOfCrossRef + "_")) {
                        return id;
                    }
                }
            }
        }

        return null;

    }


    // TODO run reasoner on ontology first
    /**
     * This is the recursive method, use the other one as it caches results too
     * @param cls
     * @param prefixes
     * @param ancestorIds
     * @return
     */
    private Set<OWLClass> getClassAncestors(OWLClass cls, Set<String> prefixes, Set<OWLClass> ancestorIds){

        Collection<OWLClassExpression> superClasses = EntitySearcher.getSuperClasses(cls, ontology);
        for(OWLClassExpression superClass : superClasses){
            if (superClass.isClassExpressionLiteral()){
                ancestorIds.add(superClass.asOWLClass());
                getClassAncestors (superClass.asOWLClass(), prefixes, ancestorIds);
            } else {
                if (superClass instanceof OWLObjectSomeValuesFrom){
                    OWLObjectSomeValuesFrom svf = (OWLObjectSomeValuesFrom) superClass;
                    if (PART_OF.contains(svf.getProperty().asOWLObjectProperty())){
                        if (svf.getFiller() instanceof OWLNamedObject){
                            ancestorIds.add(svf.getFiller().asOWLClass());
                            getClassAncestors (svf.getFiller().asOWLClass(), prefixes, ancestorIds);
                        }
                    }
                }
            }
        }

        return ancestorIds;

    }


    /**
     *
     * @param cls
     * @param term the ontology term dto where you want the child info to be added.
     * @return Return the cls dto with added information about child classes: childIds and childTerms. If terms for slim were provided to the parser the results will be restricted to slim classes.
     */
    private void addChildrenInfo(OWLClass cls, OntologyTermDTO term, String prefix){

        Set<OWLClass> children = getChildren(cls, true);
        if (termsInSlim != null){
            // Filter out child terms not in slim or with another prefix
            children = children.stream().filter(termCls -> { return termsInSlim.contains(getIdentifierShortForm(termCls)) || !startsWithPrefix(termCls, prefix);}).collect(Collectors.toSet());
        }
        for (OWLClass child : children){
            term.addChildId(getIdentifierShortForm(child.asOWLClass()));
            term.addChildName(getLabel(child.asOWLClass()));
        }
    }

    private void addTopLevelInfo (OWLClass cls, OntologyTermDTO term ){

        Set<OWLClass> classAncestors = getClassAncestors(cls, null);
        if (classAncestors != null && topLevelIds != null) {
            // Intersect list of ancestors with list of top Levels
            Set<OWLClass> localTopLevels = classAncestors.stream()
                    .filter(item -> {
                        return topLevelIds.contains(getIdentifierShortForm(item));
                    }).collect(Collectors.toSet());

            for (OWLClass topLevel : localTopLevels) {
                term.addTopLevelId(getIdentifierShortForm(topLevel));
                term.addTopLevelName(getLabel(topLevel));
                term.addTopLevelSynonym(getSynonyms(topLevel));
                term.addTopLevelTermIdsConcatenated(getLabel(topLevel), getIdentifierShortForm(topLevel));
            }
        }

    }

    /**
     * [!] At the moment this adds ancestors - topLevels . So it can adds terms on top of the higher level too.
     * @param cls
     * @param term
     * @return
     */
    private void addIntermediateInfo(OWLClass cls, OntologyTermDTO term, String prefix ){

        Set<OWLClass> classAncestors = getClassAncestors(cls, null);
        if (classAncestors != null) {
            Set<OWLClass> intermediates = classAncestors;
            if (topLevelIds != null){
                // Remove top levels from ancestors list and terms with the wrong prefix
                // Intersect list of ancestors with list of top Levels
                intermediates = classAncestors.stream()
                        .filter(item -> {
                            return !topLevelIds.contains(getIdentifierShortForm(item)) || !startsWithPrefix(item, prefix);
                        }).collect(Collectors.toSet());
            }

            for (OWLClass intermediateTerm : intermediates) {
                term.addIntermediateIds(getIdentifierShortForm(intermediateTerm));
                term.addIntermediateNames(getLabel(intermediateTerm));
                term.addIntermediateSynonyms(getSynonyms(intermediateTerm));
            }
        }

    }


    /**
     * @param term
     * @param cls
     * @return Return the cls dto with added information about child classes: childIds and childTerms.
     */
    private void addParentInfo(OWLClass cls, OntologyTermDTO term, String prefix){

        for (OWLClass parent: getParents(cls, prefix, true)){
            term.addParentId(getIdentifierShortForm(parent));
            term.addParentName(getLabel(parent));
        }
    }

    protected Set<OWLClass> getParents(OWLClass cls, String prefix, Boolean partOfToo){

        Set<OWLClass> res = new HashSet<>();
        for (OWLClassExpression classExpression : EntitySearcher.getSuperClasses(cls, ontology)){
            if (classExpression.isClassExpressionLiteral() && startsWithPrefix(classExpression.asOWLClass(), prefix)){
                res.add(classExpression.asOWLClass());
            } else {
                if (partOfToo && classExpression instanceof OWLObjectSomeValuesFrom){
                    OWLObjectSomeValuesFrom svf = (OWLObjectSomeValuesFrom) classExpression;
                    if (PART_OF.contains(svf.getProperty().asOWLObjectProperty())){
                        OWLClassExpression filler = svf.getFiller();
                        if (filler instanceof OWLNamedObject && startsWithPrefix(filler.asOWLClass(), prefix)){
                            res.add(filler.asOWLClass());
                        }
                    }
                }
            }
        }
        return res;
    }

    private boolean isObsolete(OWLClass cls){

        for (OWLAnnotationProperty synonym: IS_OBSOLETE){
            if (EntitySearcher.getAnnotations(cls,ontology, synonym).size() > 0) {
                return true;
            }
        }
        return false;
    }


    /**
     *
     * @param cls
     * @return ID of replacement class for an obsolete one. It
     */
    private String getReplacementId(OWLClass cls){

        Collection<OWLAnnotation> res = EntitySearcher.getAnnotations(cls, ontology, REPLACEMENT);

        if (res.size() > 0){
            if (res.size() > 1){
                System.out.println("WARNING: more than 1 replacement terms for deprecated class " + getIdentifierShortForm(cls));
            }
            if (res.iterator().next().getValue() instanceof OWLLiteral) {
                return ((OWLLiteral) res.iterator().next().getValue()).getLiteral();
            }
        }
        return null;

    }


    /**
     *
     * @param cls
     * @return IDs of classes to consider using instead of an obsolete term. There can be multiple ids for each obsolete term.
     */
    private Set<String> getConsiderIds(OWLClass cls){

        Collection<OWLAnnotation> res = EntitySearcher.getAnnotations(cls, ontology, CONSIDER);
        Set<String> ids = new HashSet<>();

        res.stream()
                .filter(item->(item.getValue() instanceof OWLLiteral))
                .forEach(item->ids.add(getConsiderId(item, cls)));

        return ids;

    }


    private String getConsiderId(OWLAnnotation ann, OWLClass cls){

        String consider = null ;
        if (ann.getValue() instanceof OWLLiteral){
            consider = ((OWLLiteral) ann.getValue()).getLiteral();
        }
        return consider;

    }


    private Set<OWLClass> getChildren(OWLClass cls, Boolean withPartOf){

        Set<OWLClass> children = new HashSet<>();

        Collection<OWLAxiom> referencingAxioms = EntitySearcher.getReferencingAxioms(cls, ontology);

        if (withPartOf) {
            // add part of
            for (OWLAxiom axiom : referencingAxioms) {
                if (axiom.getAxiomType().equals(AxiomType.SUBCLASS_OF)) {
                    OWLSubClassOfAxiom ax = (OWLSubClassOfAxiom) axiom;
                    if (ax.getSubClass() instanceof OWLClass &&
                            !getIdentifierShortForm((OWLClass) ax.getSubClass()).equals(getIdentifierShortForm(cls)) &&
                            ax.getSuperClass() instanceof OWLObjectSomeValuesFrom) {
                        OWLObjectSomeValuesFrom svf = (OWLObjectSomeValuesFrom) ax.getSuperClass();
                        if (PART_OF.contains(svf.getProperty().asOWLObjectProperty())) {
                            if (svf.getFiller() instanceof OWLNamedObject && getIdentifierShortForm(svf.getFiller().asOWLClass()).equals(getIdentifierShortForm(cls))) {
                                children.add((OWLClass) ax.getSubClass());
                            }
                        }
                    }
                }
            }
        }
        // add simple parents (is-a)
        for ( OWLClassExpression classExpression: EntitySearcher.getSubClasses(cls, ontology)){
            if (classExpression.isClassExpressionLiteral()){
                children.add(classExpression.asOWLClass());
            }
        }
        return children;

    }


}