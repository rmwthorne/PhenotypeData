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
package org.mousephenotype.cda.db.pojo;

/**
 *
 * Representation of a biological model of interest.
 *
 * @author Gautier Koscielny (EMBL-EBI) <koscieln@ebi.ac.uk>
 * @since February 2012
 * @see BiologicalSample
 * @see GenomicFeature
 */

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.*;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;


@Entity
@Table(name = "biological_model")
public class BiologicalModel extends SourcedEntry {

	@Column(name = "id", insertable=false, updatable=false)
	Long id;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Column(name = "allelic_composition")
	String allelicComposition;

	@Column(name = "genetic_background")
	String geneticBackground;

	@Column(name = "zygosity")
	private String zygosity;

	@OneToMany(cascade = CascadeType.ALL, fetch= FetchType.LAZY)
	@Fetch(FetchMode.SELECT)
    @JoinTable(
            name="biological_model_sample",
            joinColumns = @JoinColumn( name="biological_model_id"),
            inverseJoinColumns = @JoinColumn( name="biological_sample_id")
    )
	private List<BiologicalSample> biologicalSamples;

	/**
	 * Unidirectional with join table
	 * Transitive persistence with cascading
	 * We detach the association but we keep
	 * the genomic feature otherwise
	 */
	@OneToMany(cascade = CascadeType.ALL, fetch= FetchType.EAGER )
	@Fetch(FetchMode.SELECT)
	@JoinTable(
			name="biological_model_genomic_feature",
		    joinColumns = @JoinColumn( name="biological_model_id"),
            inverseJoinColumns = {@JoinColumn(name = "gf_acc"), @JoinColumn(name = "gf_db_id")}
    )
	private List<GenomicFeature> genomicFeatures;

	/**
	 * Unidirectional with join table
	 * Transitive persistence with cascading
	 * We detach the association but we keep
	 * the allele otherwise
	 */
	@OneToMany(cascade = CascadeType.ALL, fetch= FetchType.EAGER)
	@Fetch(FetchMode.SELECT)
	@JoinTable(
			name="biological_model_allele",
		    joinColumns = @JoinColumn( name="biological_model_id"),
            inverseJoinColumns = {@JoinColumn(name = "allele_acc"), @JoinColumn(name = "allele_db_id")}
    )
	private List<Allele> alleles;

	/**
	 * Unidirectional with join table
	 * Transitive persistence with cascading
	 * We detach the association but we keep
	 * the strain otherwise
	 */
	@OneToMany(cascade = CascadeType.ALL, fetch= FetchType.EAGER)
	@Fetch(FetchMode.SELECT)
	@JoinTable(
			name="biological_model_strain",
		    joinColumns = @JoinColumn( name="biological_model_id"),
            inverseJoinColumns = {@JoinColumn(name = "strain_acc"), @JoinColumn(name = "strain_db_id")}
    )
	private List<Strain> strains;

	public BiologicalModel() {
	}

	public BiologicalModel(Long id, String allelicComposition, String geneticBackground, String zygosity) {
		this.id = id;
		this.allelicComposition = allelicComposition;
		this.geneticBackground = geneticBackground;
		this.zygosity = zygosity;
	}

	/**
	 * @return the biologicalSamples
	 */
	public List<BiologicalSample> getBiologicalSamples() {
		return biologicalSamples;
	}

	/**
	 * @param biologicalSamples the biologicalSamples to set
	 */
	public void setBiologicalSamples(List<BiologicalSample> biologicalSamples) {
		this.biologicalSamples = biologicalSamples;
	}

	public void addBiologicalSample(BiologicalSample biologicalSample) {
		if (biologicalSamples == null) {
			biologicalSamples = new LinkedList<BiologicalSample>();
		}
		biologicalSamples.add(biologicalSample);
	}

	/**
	 * @return the allelicComposition
	 */
	public String getAllelicComposition() {
		return allelicComposition;
	}

	/**
	 * @param allelicComposition the allelicComposition to set
	 */
	public void setAllelicComposition(String allelicComposition) {
		this.allelicComposition = allelicComposition;
	}

	/**
	 * @return the geneticBackground
	 */
	public String getGeneticBackground() {
		return geneticBackground;
	}

	/**
	 * @param geneticBackground the geneticBackground to set
	 */
	public void setGeneticBackground(String geneticBackground) {
		this.geneticBackground = geneticBackground;
	}

	/**
	 * @return the zygosity
	 */
	public String getZygosity() {
		return zygosity;
	}

	/**
	 * @param zygosity the zygosity to set
	 */
	public void setZygosity(String zygosity) {
		this.zygosity = zygosity;
	}

	/**
	 * @return the genomicFeatures
	 */
	public List<GenomicFeature> getGenomicFeatures() {
		return genomicFeatures;
	}

	/**
	 * @param genomicFeatures the genomicFeatures to set
	 */
	public void setGenomicFeatures(List<GenomicFeature> genomicFeatures) {
		this.genomicFeatures = genomicFeatures;
	}

	/**
	 * @param genomicFeature the genomicFeature to add to the collection
	 */
	public void addGenomicFeature(GenomicFeature genomicFeature) {
		if (genomicFeatures == null) {
			this.genomicFeatures = new LinkedList<GenomicFeature>();
		}
		this.genomicFeatures.add(genomicFeature);
	}

	/**
	 * @return the alleles
	 */
	public List<Allele> getAlleles() {
		return alleles;
	}

	/**
	 * @param alleles the alleles to set
	 */
	public void setAlleles(List<Allele> alleles) {
		this.alleles = alleles;
	}

	/**
	 * @param allele the allele to add to the collection
	 */
	public void addAllele(Allele allele) {
		if (alleles == null) {
			this.alleles = new LinkedList<Allele>();
		}
		this.alleles.add(allele);
	}

	/**
	 * @return the strains
	 */
	public List<Strain> getStrains() {
		return strains;
	}

	/**
	 * @param strains the strains to set
	 */
	public void setStrains(List<Strain> strains) {
		this.strains = strains;
	}

	/**
	 * @param strain the strain to add to the collection
	 */
	public void addStrain(Strain strain) {
		if (strains == null) {
			this.strains = new LinkedList<Strain>();
		}
		this.strains.add(strain);
	}

	@Override
	public String toString() {
		return "BiologicalModel{" +
				"id=" + id +
				", allelicComposition='" + allelicComposition + '\'' +
				", geneticBackground='" + geneticBackground + '\'' +
				", zygosity='" + zygosity + '\'' +
				'}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		BiologicalModel that = (BiologicalModel) o;
		return id.equals(that.id) &&
				allelicComposition.equals(that.allelicComposition) &&
				geneticBackground.equals(that.geneticBackground) &&
				Objects.equals(zygosity, that.zygosity);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, allelicComposition, geneticBackground, zygosity);
	}
}