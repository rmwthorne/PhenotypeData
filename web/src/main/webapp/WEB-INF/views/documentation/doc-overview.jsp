<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>

<t:genericpage>

  <jsp:attribute name="title">IMPC Search</jsp:attribute>
  <jsp:attribute name="breadcrumb">&nbsp;&raquo;&nbsp;<a href="${baseUrl}/search/${dataType}?kw=*">${dataTypeLabel}</a> &raquo; ${searchQuery}</jsp:attribute>
  <jsp:attribute name="bodyTag"><body id="top" class="page-node searchpage one-sidebar sidebar-first small-header"></jsp:attribute>

	<jsp:attribute name="header">
		<link href="${baseUrl}/css/searchPage.cssssss" rel="stylesheet" type="text/css" />
        <style>
          table {
            margin-top: 80px;
          }
          td {
            border: none;
          }
          td {border: 1px solid white;
            width: 33%;
          }


        </style>

	</jsp:attribute>

	<jsp:attribute name="addToFooter">
		<div class="region region-pinned"></div>
	</jsp:attribute>

  <jsp:body>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
    <h1>IMPC data portal documentation</h1>
    <!--<h4>Explore how to retrieve mouse phenotype data</h4>-->

  <table style="width:100%">
    <tr>
      <td><i class="fa fa-search fa-4x"></i></td>
      <td><i class="fa fa-map-o fa-4x"></i></td>
      <td><i class="fa fa-info fa-4x"></i></td>
    </tr>
    <tr>
      <td class="descTxt">Search</td>
      <td class="descTxt">Explore</td>
      <td class="descTxt">FAQ</td>
    </tr>
    <tr>
      <td><i class="fa fa-line-chart fa-4x"></i></td>
      <td><i class="fa fa-sitemap fa-4x"></i></td>
      <td><i class="fa fa-download fa-4x"></i></td>
    </tr>
    <tr>
      <td class="descTxt">Methods</td>
      <td class="descTxt">Related Resources</td>
      <td class="descTxt">Downloads, Programmatic Access, Internal Submission
      </td>
    </tr>
  </table>


  </jsp:body>

</t:genericpage>