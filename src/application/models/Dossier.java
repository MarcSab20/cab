package application.models;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Modèle représentant un dossier dans le système de gestion documentaire
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
    
    // Champs calculés (non en base)
    private int nombreDocuments;
    private int nombreSousDossiers;
    
    // Constructeurs
    public Dossier() {
        this.actif = true;
        this.systeme = false;
        this.icone = "📁";
        this.ordreAffichage = 0;
        this.dateCreation = LocalDateTime.now();
    }
    
    public Dossier(String codeDossier, String nomDossier) {
        this();
        this.codeDossier = codeDossier;
        this.nomDossier = nomDossier;
    }
    
    // Getters et Setters
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
    
    // Méthodes utilitaires
    
    /**
     * Vérifie si c'est un dossier racine
     */
    public boolean isRacine() {
        return dossierParentId == null || dossierParentId == 0;
    }
    
    /**
     * Vérifie si le dossier peut être supprimé
     */
    public boolean peutEtreSupprime() {
        return !systeme && actif;
    }
    
    /**
     * Vérifie si le dossier peut être modifié
     */
    public boolean peutEtreModifie() {
        return !systeme && actif;
    }
    
    /**
     * Retourne une représentation textuelle du chemin
     */
    public String getCheminFormate() {
        if (cheminComplet == null) return nomDossier;
        return cheminComplet.replace("/ROOT/", "").replace("/", " > ");
    }
    
    /**
     * Retourne le nom complet avec icône
     */
    public String getNomCompletAvecIcone() {
        return icone + " " + nomDossier;
    }
    
    /**
     * Retourne le niveau du dossier dans l'arborescence
     */
    public int getNiveau() {
        if (cheminComplet == null) return 0;
        return cheminComplet.split("/").length - 1;
    }
    
    /**
     * Vérifie si le dossier est vide
     */
    public boolean estVide() {
        return nombreDocuments == 0 && nombreSousDossiers == 0;
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
        return "Dossier{" +
                "id=" + id +
                ", codeDossier='" + codeDossier + '\'' +
                ", nomDossier='" + nomDossier + '\'' +
                ", cheminComplet='" + cheminComplet + '\'' +
                ", nombreDocuments=" + nombreDocuments +
                ", nombreSousDossiers=" + nombreSousDossiers +
                ", systeme=" + systeme +
                '}';
    }
}