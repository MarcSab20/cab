package application.models;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Modèle Dossier pour l'arborescence hiérarchique
 */
public class Dossier {
    
    private int id;
    private String codeDossier;
    private String nomDossier;
    private Integer dossierParentId;
    private String cheminComplet;
    private String description;
    private String icone;
    private int ordreAffichage;
    private boolean actif;
    private boolean systeme;
    private Integer creePar;
    private LocalDateTime dateCreation;
    private LocalDateTime dateModification;
    
    // Informations supplémentaires (depuis la vue)
    private int nombreDocuments;
    private int nombreSousDossiers;
    private String nomDossierParent;
    private int niveauHierarchie;
    
    public Dossier() {
        this.actif = true;
        this.systeme = false;
        this.ordreAffichage = 0;
        this.icone = "📁";
        this.dateCreation = LocalDateTime.now();
    }
    
    // ============ GETTERS ET SETTERS ============
    
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public String getCodeDossier() {
        return codeDossier;
    }
    
    public void setCodeDossier(String codeDossier) {
        this.codeDossier = codeDossier;
    }
    
    public String getNomDossier() {
        return nomDossier;
    }
    
    public void setNomDossier(String nomDossier) {
        this.nomDossier = nomDossier;
    }
    
    public Integer getDossierParentId() {
        return dossierParentId;
    }
    
    public void setDossierParentId(Integer dossierParentId) {
        this.dossierParentId = dossierParentId;
    }
    
    public String getCheminComplet() {
        return cheminComplet;
    }
    
    public void setCheminComplet(String cheminComplet) {
        this.cheminComplet = cheminComplet;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getIcone() {
        return icone;
    }
    
    public void setIcone(String icone) {
        this.icone = icone;
    }
    
    public int getOrdreAffichage() {
        return ordreAffichage;
    }
    
    public void setOrdreAffichage(int ordreAffichage) {
        this.ordreAffichage = ordreAffichage;
    }
    
    public boolean isActif() {
        return actif;
    }
    
    public void setActif(boolean actif) {
        this.actif = actif;
    }
    
    public boolean isSysteme() {
        return systeme;
    }
    
    public void setSysteme(boolean systeme) {
        this.systeme = systeme;
    }
    
    public Integer getCreePar() {
        return creePar;
    }
    
    public void setCreePar(Integer creePar) {
        this.creePar = creePar;
    }
    
    public LocalDateTime getDateCreation() {
        return dateCreation;
    }
    
    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }
    
    public LocalDateTime getDateModification() {
        return dateModification;
    }
    
    public void setDateModification(LocalDateTime dateModification) {
        this.dateModification = dateModification;
    }
    
    // Informations supplémentaires
    
    public int getNombreDocuments() {
        return nombreDocuments;
    }
    
    public void setNombreDocuments(int nombreDocuments) {
        this.nombreDocuments = nombreDocuments;
    }
    
    public int getNombreSousDossiers() {
        return nombreSousDossiers;
    }
    
    public void setNombreSousDossiers(int nombreSousDossiers) {
        this.nombreSousDossiers = nombreSousDossiers;
    }
    
    public String getNomDossierParent() {
        return nomDossierParent;
    }
    
    public void setNomDossierParent(String nomDossierParent) {
        this.nomDossierParent = nomDossierParent;
    }
    
    public int getNiveauHierarchie() {
        return niveauHierarchie;
    }
    
    public void setNiveauHierarchie(int niveauHierarchie) {
        this.niveauHierarchie = niveauHierarchie;
    }
    
    // ============ MÉTHODES UTILITAIRES ============
    
    /**
     * Vérifie si le dossier est un dossier racine
     */
    public boolean estRacine() {
        return dossierParentId == null;
    }
    
    /**
     * Vérifie si le dossier contient des éléments
     */
    public boolean contientElements() {
        return nombreDocuments > 0 || nombreSousDossiers > 0;
    }
    
    /**
     * Retourne le nom complet avec l'icône
     */
    public String getNomCompletAvecIcone() {
        return icone + " " + nomDossier;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Dossier dossier = (Dossier) o;
        return id == dossier.id;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return nomDossier;
    }
}