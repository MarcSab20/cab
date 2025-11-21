package application.models;

/**
 * Énumération des statuts d'une réunion
 */
public enum StatutReunion {
    PROGRAMMEE("Programmée", "🟡", "#f39c12"),
    EN_COURS("En cours", "🟢", "#27ae60"),
    TERMINEE("Terminée", "✅", "#2ecc71"),
    ANNULEE("Annulée", "❌", "#e74c3c"),
    REPORTEE("Reportée", "📅", "#3498db");
    
    private final String libelle;
    private final String icone;
    private final String couleur;
    
    StatutReunion(String libelle, String icone, String couleur) {
        this.libelle = libelle;
        this.icone = icone;
        this.couleur = couleur;
    }
    
    public String getLibelle() {
        return libelle;
    }
    
    public String getIcone() {
        return icone;
    }
    
    public String getCouleur() {
        return couleur;
    }
    
    /**
     * Retourne le statut à partir de son libellé
     */
    public static StatutReunion fromLibelle(String libelle) {
        for (StatutReunion statut : values()) {
            if (statut.libelle.equalsIgnoreCase(libelle)) {
                return statut;
            }
        }
        return null;
    }
    
    /**
     * Retourne le statut à partir de la valeur de la base de données
     */
    public static StatutReunion fromDatabase(String dbValue) {
        if (dbValue == null) return null;
        
        try {
            return valueOf(dbValue.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
    
    /**
     * Vérifie si la réunion est active (programmée ou en cours)
     */
    public boolean isActive() {
        return this == PROGRAMMEE || this == EN_COURS;
    }
    
    /**
     * Vérifie si la réunion est terminée (terminée, annulée)
     */
    public boolean isTerminee() {
        return this == TERMINEE || this == ANNULEE;
    }
    
    /**
     * Vérifie si la réunion peut être modifiée
     */
    public boolean peutEtreModifiee() {
        return this == PROGRAMMEE || this == REPORTEE;
    }
    
    /**
     * Vérifie si un compte-rendu peut être ajouté
     */
    public boolean peutAvoirCompteRendu() {
        return this == TERMINEE || this == EN_COURS;
    }
    
    @Override
    public String toString() {
        return libelle;
    }
}