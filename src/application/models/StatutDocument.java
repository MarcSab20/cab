package application.models;

/**
 * Énumération des statuts possibles pour un document
 */
public enum StatutDocument {
    ACTIF("Actif", "✅", "#27ae60"),
    ARCHIVE("Archivé", "📁", "#95a5a6"),
    SUPPRIME("Supprimé", "🗑️", "#e74c3c"),
    BROUILLON("Brouillon", "📝", "#f39c12"),
    EN_COURS("En cours", "⏳", "#3498db"),
    VALIDE("Validé", "✓", "#2ecc71"),
    EXPIRE("Expiré", "⌛", "#e67e22"),
    SUSPENDU("Suspendu", "⏸️", "#95a5a6"),
    EN_REVISION("En révision", "🔄", "#9b59b6"),
    ATTENTE_VALIDATION("Attente validation", "⏱️", "#f39c12");
    
    private final String libelle;
    private final String icone;
    private final String couleur;
    
    StatutDocument(String libelle, String icone, String couleur) {
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
    public static StatutDocument fromLibelle(String libelle) {
        for (StatutDocument statut : values()) {
            if (statut.libelle.equalsIgnoreCase(libelle)) {
                return statut;
            }
        }
        return null;
    }
    
    /**
     * Retourne le statut à partir de la valeur de la base de données
     */
    public static StatutDocument fromDatabase(String dbValue) {
        if (dbValue == null) return null;
        
        try {
            return valueOf(dbValue.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
    
    /**
     * Vérifie si le document est actif (ni archivé ni supprimé)
     */
    public boolean isActif() {
        return this != ARCHIVE && this != SUPPRIME;
    }
    
    /**
     * Vérifie si le document est disponible pour consultation
     */
    public boolean isDisponible() {
        return this == ACTIF || this == VALIDE || this == EN_COURS || this == EN_REVISION;
    }
    
    /**
     * Vérifie si le document peut être modifié
     */
    public boolean peutEtreModifie() {
        return this == BROUILLON || this == EN_COURS || this == EN_REVISION;
    }
    
    /**
     * Vérifie si le document peut être supprimé
     */
    public boolean peutEtreSupprime() {
        return this != SUPPRIME;
    }
    
    /**
     * Vérifie si le document nécessite une action
     */
    public boolean necessiteAction() {
        return this == ATTENTE_VALIDATION || this == EN_REVISION || this == EXPIRE;
    }
    
    /**
     * Vérifie si le document est en attente
     */
    public boolean isEnAttente() {
        return this == ATTENTE_VALIDATION || this == SUSPENDU;
    }
    
    /**
     * Vérifie si le document est terminé
     */
    public boolean isTermine() {
        return this == VALIDE || this == ARCHIVE;
    }
    
    /**
     * Vérifie si le document est en cours d'édition
     */
    public boolean isEnEdition() {
        return this == BROUILLON || this == EN_COURS || this == EN_REVISION;
    }
    
    /**
     * Retourne le style CSS pour l'affichage
     */
    public String getStyle() {
        return "-fx-background-color: " + couleur + "; -fx-text-fill: white;";
    }
    
    /**
     * Retourne le style pour le texte uniquement
     */
    public String getTextStyle() {
        return "-fx-text-fill: " + couleur + ";";
    }
    
    /**
     * Retourne une description du statut
     */
    public String getDescription() {
        return switch (this) {
            case ACTIF -> "Le document est actif et disponible";
            case ARCHIVE -> "Le document a été archivé";
            case SUPPRIME -> "Le document a été supprimé";
            case BROUILLON -> "Le document est en cours de rédaction";
            case EN_COURS -> "Le document est en cours de traitement";
            case VALIDE -> "Le document a été validé";
            case EXPIRE -> "Le document a dépassé sa date d'expiration";
            case SUSPENDU -> "Le document est temporairement suspendu";
            case EN_REVISION -> "Le document est en cours de révision";
            case ATTENTE_VALIDATION -> "Le document est en attente de validation";
        };
    }
    
    @Override
    public String toString() {
        return libelle;
    }
}