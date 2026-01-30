package application.models;

/**
 * Modèle pour une catégorie d'expéditeur
 */
public class CategorieExpediteur {
    private int id;
    private String code;
    private String libelle;
    private int ordreAffichage;
    private boolean aSousCategories;
    private boolean actif;
    
    public CategorieExpediteur() {
    }
    
    public CategorieExpediteur(String code, String libelle) {
        this.code = code;
        this.libelle = libelle;
        this.actif = true;
    }
    
    // Getters et Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    
    public String getLibelle() { return libelle; }
    public void setLibelle(String libelle) { this.libelle = libelle; }
    
    public int getOrdreAffichage() { return ordreAffichage; }
    public void setOrdreAffichage(int ordreAffichage) { this.ordreAffichage = ordreAffichage; }
    
    public boolean isASousCategories() { return aSousCategories; }
    public void setASousCategories(boolean aSousCategories) { this.aSousCategories = aSousCategories; }
    
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
        CategorieExpediteur that = (CategorieExpediteur) obj;
        return id == that.id;
    }
    
    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}