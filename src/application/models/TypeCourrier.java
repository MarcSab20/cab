package application.models;

/**
 * Énumération pour le type de courrier
 */
public enum TypeCourrier {
    ENTRANT("Entrant", "📥"),
    SORTANT("Sortant", "📤"),
    INTERNE("Interne", "🔄"),
    URGENT("Urgent", "🚨");
    
    private final String libelle;
    private final String icone;
    
    TypeCourrier(String libelle, String icone) {
        this.libelle = libelle;
        this.icone = icone;
    }
    
    public String getLibelle() {
        return libelle;
    }
    
    public String getIcone() {
        return icone;
    }
    
    /**
     * Convertit un String en TypeCourrier
     */
    public static TypeCourrier fromString(String value) {
        if (value == null) {
            return ENTRANT;
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
                // Par défaut, retourner ENTRANT
                return ENTRANT;
            }
        }
    }
    
    /**
     * Retourne le nom pour la base de données (lowercase)
     */
    public String toDbString() {
        return name().toLowerCase();
    }
    
    @Override
    public String toString() {
        return libelle;
    }
}