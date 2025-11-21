package application.models;

/**
 * Énumération des types de courrier
 */
public enum TypeCourrier {
    ENTRANT("Entrant", "📨"),
    SORTANT("Sortant", "📤");
    
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
     * Retourne le type à partir de son libellé
     */
    public static TypeCourrier fromLibelle(String libelle) {
        for (TypeCourrier type : values()) {
            if (type.libelle.equalsIgnoreCase(libelle)) {
                return type;
            }
        }
        return null;
    }
    
    /**
     * Retourne le type à partir de la valeur de la base de données
     */
    public static TypeCourrier fromDatabase(String dbValue) {
        if (dbValue == null) return null;
        
        try {
            return valueOf(dbValue.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
    
    @Override
    public String toString() {
        return libelle;
    }
}