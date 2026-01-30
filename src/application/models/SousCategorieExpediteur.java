package application.models;

/**
 * Modèle pour une sous-catégorie d'expéditeur
 */
public class SousCategorieExpediteur {
    private int id;
    private int categorieId;
    private String code;
    private String libelle;
    private int ordreAffichage;
    private boolean actif;
    
    public SousCategorieExpediteur() {
    }
    
    public SousCategorieExpediteur(int categorieId, String code, String libelle) {
        this.categorieId = categorieId;
        this.code = code;
        this.libelle = libelle;
        this.actif = true;
    }
    
    // Getters et Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public int getCategorieId() { return categorieId; }
    public void setCategorieId(int categorieId) { this.categorieId = categorieId; }
    
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    
    public String getLibelle() { return libelle; }
    public void setLibelle(String libelle) { this.libelle = libelle; }
    
    public int getOrdreAffichage() { return ordreAffichage; }
    public void setOrdreAffichage(int ordreAffichage) { this.ordreAffichage = ordreAffichage; }
    
    public boolean isActif() { return actif; }
    public void setActif(boolean actif) { this.actif = actif; }
    
    @Override
    public String toString() {
        return libelle;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        SousCategorieExpediteur that = (SousCategorieExpediteur) obj;
        return id == that.id;
    }
    
    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}