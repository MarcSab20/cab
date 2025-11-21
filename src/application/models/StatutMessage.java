package application.models;

/**
 * Énumération des statuts d'un message
 */
public enum StatutMessage {
    BROUILLON("Brouillon", "📝", "#95a5a6"),
    ENVOYE("Envoyé", "📤", "#3498db"),
    DELIVRE("Délivré", "✓", "#2ecc71"),
    LU("Lu", "✓✓", "#27ae60"),
    ARCHIVE("Archivé", "📁", "#7f8c8d"),
    SUPPRIME("Supprimé", "🗑️", "#e74c3c"),
    ECHEC("Échec d'envoi", "❌", "#c0392b");
    
    private final String libelle;
    private final String icone;
    private final String couleur;
    
    StatutMessage(String libelle, String icone, String couleur) {
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
    public static StatutMessage fromLibelle(String libelle) {
        for (StatutMessage statut : values()) {
            if (statut.libelle.equalsIgnoreCase(libelle)) {
                return statut;
            }
        }
        return null;
    }
    
    /**
     * Retourne le statut à partir de la valeur de la base de données
     */
    public static StatutMessage fromDatabase(String dbValue) {
        if (dbValue == null) return null;
        
        try {
            return valueOf(dbValue.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
    
    /**
     * Vérifie si le message est en cours d'envoi ou envoyé
     */
    public boolean isEnvoye() {
        return this == ENVOYE || this == DELIVRE || this == LU;
    }
    
    /**
     * Vérifie si le message a été lu
     */
    public boolean isLu() {
        return this == LU;
    }
    
    /**
     * Vérifie si le message peut être modifié
     */
    public boolean peutEtreModifie() {
        return this == BROUILLON;
    }
    
    /**
     * Vérifie si le message peut être supprimé
     */
    public boolean peutEtreSupprime() {
        return this != SUPPRIME;
    }
    
    /**
     * Vérifie si le message est actif (ni archivé ni supprimé)
     */
    public boolean isActif() {
        return this != ARCHIVE && this != SUPPRIME;
    }
    
    /**
     * Vérifie si c'est un échec
     */
    public boolean isEchec() {
        return this == ECHEC;
    }
    
    /**
     * Retourne le style CSS pour l'affichage
     */
    public String getStyle() {
        return "-fx-text-fill: " + couleur + ";";
    }
    
    @Override
    public String toString() {
        return libelle;
    }
}