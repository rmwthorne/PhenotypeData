<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@taglib prefix="t" tagdir="/WEB-INF/tags" %>


<c:set var="count" value="0" scope="page"/>
<c:set var="maleCount" value="0" scope="page"/>
<c:set var="femaleCount" value="0" scope="page"/>
<c:forEach var="phenotype" items="${phenotypes}" varStatus="status">
    <c:forEach var="sex" items="${phenotype.sexes}">
        <c:set var="count" value="${count + 1}" scope="page"/>
        <c:if test='${sex.equalsIgnoreCase("male")}'>
            <c:set var="maleCount" value="${maleCount + 1}" scope="page"/>
        </c:if>
        <c:if test='${sex.equalsIgnoreCase("female")}'>
            <c:set var="femaleCount" value="${femaleCount + 1}" scope="page"/>
        </c:if>
    </c:forEach>
</c:forEach>
<p class="resultCount">
    <%-- Total number of significant genotype-phenotype associations: ${count} --%>
    Total number of significant genotype-phenotype associations: female(${femaleCount}) , male(${maleCount})
</p>

<script>
    var resTemp = document.getElementsByClassName("resultCount");
    if (resTemp.length > 1)
        resTemp[0].remove();
</script>


<table id="genes" class="table tableSorter">
    <thead>
    <tr>
        <th class="headerSort">Phenotype</th>
        <th class="headerSort">Allele</th>
        <th class="headerSort">Zygosity</th>
        <th class="headerSort">Sex</th>
        <th class="headerSort">Life Stage</th>
        <th class="headerSort">Procedure | Parameter</th>
        <th class="headerSort">Phenotyping Center | Source</th>
       <!-- <th class="headerSort">Source</th> -->
        <th>P Value</th>
        <th class="headerSort">Graph</th>
    </tr>
    </thead>
    <tbody>
    <c:forEach var="phenotype" items="${phenotypes}" varStatus="status">
        <c:set var="europhenome_gender" value="Both-Split"/>
        <tr>
            <td>
                <c:if test="${fn:containsIgnoreCase(phenotype.phenotypeTerm.id, 'MPATH:') }">
                    ${phenotype.phenotypeTerm.name}
                </c:if>
                <c:if test="${not fn:containsIgnoreCase(phenotype.phenotypeTerm.id, 'MPATH:') }">
                    <a href="${baseUrl}/phenotypes/${phenotype.phenotypeTerm.id}">${phenotype.phenotypeTerm.name}</a>
                </c:if>

            </td>
            <td><c:choose><c:when test="${fn:contains(phenotype.allele.accessionId, 'MGI')}"><a
                    href="http://www.informatics.jax.org/accession/${phenotype.allele.accessionId}"><t:formatAllele>${phenotype.allele.symbol}</t:formatAllele></a></c:when><c:otherwise><t:formatAllele>${phenotype.allele.symbol}</t:formatAllele></c:otherwise></c:choose>
            </td>
            <td>${phenotype.zygosity}</td>
            <td>
                <c:set var="count" value="0" scope="page"/>
                <c:forEach var="sex" items="${phenotype.sexes}"><c:set var="count" value="${count + 1}" scope="page"/>
                    <c:if test="${sex == 'female'}"><c:set var="europhenome_gender" value="Female"/>
                        <img alt="Female" src="${baseUrl}/img/female.jpg"/>
                    </c:if>
                    <c:if test="${sex == 'male'}">
                        <c:if test="${count != 2}"><img data-placement="top" src="${baseUrl}/img/empty.jpg"/></c:if>
                        <c:set var="europhenome_gender" value="Male"/><img alt="Male" src="${baseUrl}/img/male.jpg"/>
                    </c:if>
                </c:forEach>
            </td>
            <td>${phenotype.lifeStageName}</td>

            <td>${phenotype.procedure.name} | ${phenotype.parameter.name}</td>
            <td>${phenotype.phenotypingCenter} |  ${phenotype.dataSourceName}</td>
           <!-- <td>
                    ${phenotype.dataSourceName}
            </td>
    -->
            <td>${phenotype.prValueAsString}</td>

            <c:if test="${phenotype.isPreQc()}">
            <td style="text-align:center;color:#EF7B0B;">
            <c:if test="${not fn:containsIgnoreCase(phenotype.graphUrl, 'MP:0001926') && not fn:containsIgnoreCase(phenotype.graphUrl, 'MP:0001925') }">
                <a href="${phenotype.graphUrl }" class="fancyboxGraph" style="color:#EF7B0B;;text-decoration:none;">
                    <i class="fa fa-bar-chart-o" alt="Graphs"></i>
                    <i class="fa fa-exclamation" title="This is a preliminary association based on pre QC data."></i>
                </a>
                </c:if>
                <c:if test="${fn:containsIgnoreCase(phenotype.graphUrl, 'MP:0001926') || fn:containsIgnoreCase(phenotype.graphUrl, 'MP:0001925') }">
                
                    <i class="fa fa-bar-chart-o" title="No supporting data supplied."></i>
                    <i class="fa fa-exclamation" title="This is a preliminary association based on pre QC data."></i>
                
                </c:if>
            </td>
            </c:if>

            <c:if test="${not phenotype.isPreQc()}">
            	
                	<td style="text-align:center;">



                		<c:if test="${not fn:containsIgnoreCase(phenotype.graphUrl, 'IMPC_FER_') }">

                            <c:if test="${phenotype.graphUrl == ''}" >
                                <i class="fa fa-image" title="No images available."></i>
                            </c:if>
                            <c:if test="${phenotype.graphUrl != ''}" >
                                <c:if test="${fn:containsIgnoreCase(phenotype.phenotypeTerm.id, 'MPATH:') }">
                                    <a href="${phenotype.graphUrl }"><i class="fa fa-image" alt="Images"></i>
                                    </a>
                                </c:if>
                                <c:if test="${not fn:containsIgnoreCase(phenotype.phenotypeTerm.id, 'MPATH:') }">
                                    <a href="${phenotype.graphUrl }" class="fancyboxGraph"><i class="fa fa-bar-chart-o" alt="Graphs"></i>
                                    </a>
                                </c:if>

                            </c:if>


                    	</c:if>
                    	<c:if test="${fn:containsIgnoreCase(phenotype.graphUrl, 'IMPC_FER_') }">
                    		<i class="fa fa-bar-chart-o" title="No supporting data supplied."></i>
                    	</c:if>
                	</td>
                
            </c:if>


        </tr>
    </c:forEach>
    </tbody>
</table>

<!-- /row -->
