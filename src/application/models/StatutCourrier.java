package application.models;

/**
 * Énumération des statuts d'un courrier
 */
public enum StatutCourrier {
    EN_ATTENTE("En attente", "🟡", "#f39c12"),
    EN_COURS("En cours", "🟠", "#e67e22"),
    TRAITE("Traité", "✅", "#27ae60"),
    ARCHIVE("Archivé", "📁", "#95a5a6"),
    REJETE("Rejeté", "❌", "#e74c3c");
    
    private final String libelle;
    private final String icone;
    private final String couleur;
    
    StatutCourrier(String libelle, String icone, String couleur) {
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
    public static StatutCourrier fromLibelle(String libelle) {
        for (StatutCourrier statut : values()) {
            if (statut.libelle.equalsIgnoreCase(libelle)) {
                return statut;
            }
        }
        return null;
    }
    
    /**
     * Retourne le statut à partir de la valeur de la base de données
     */
    public static StatutCourrier fromDatabase(String dbValue) {
        if (dbValue == null) return null;
        
        try {
            return valueOf(dbValue.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
    
    /**
     * Vérifie si le courrier est terminé (traité ou archivé)
     */
    public boolean isTermine() {
        return this == TRAITE || this == ARCHIVE;
    }
    
    /**
     * Vérifie si le courrier nécessite une action
     */
    public boolean necessite_action() {
        return this == EN_ATTENTE || this == EN_COURS;
    }
    
    @Override
    public String toString() {
        return libelle;
    }
}