package application.models;

/**
 * Énumération pour le statut d'un courrier
 */
public enum StatutCourrier {
    NOUVEAU("Nouveau", "🆕", "#3498db"),
    EN_ATTENTE("En attente", "⏸️", "#f39c12"),
    EN_COURS("En cours", "⏳", "#3498db"),
    TRAITE("Traité", "✅", "#27ae60"),
    ARCHIVE("Archivé", "📦", "#95a5a6"),
    URGENT("Urgent", "🚨", "#e74c3c"), 
    REJETE("Rejété", "", "e74c3s");
    
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
     * Convertit un String en StatutCourrier
     */
    public static StatutCourrier fromString(String value) {
        if (value == null) {
            return NOUVEAU;
        }
        
        // Essayer de matcher directement
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            // Essayer avec des variations communes
            String normalized = value.toLowerCase().replace(" ", "_");
            try {
                return valueOf(normalized.toUpperCase());
            } catch (IllegalArgumentException ex) {
                // Par défaut, retourner NOUVEAU
                return NOUVEAU;
            }
        }
    }
    
    /**
     * Retourne le nom pour la base de données (lowercase avec underscore)
     */
    public String toDbString() {
        return name().toLowerCase();
    }
    
    @Override
    public String toString() {
        return libelle;
    }
}