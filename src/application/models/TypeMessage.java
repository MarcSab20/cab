package application.models;

/**
 * Énumération des types de messages
 */
public enum TypeMessage {
    INTERNE("Message interne", "💬", "#3498db"),
    SYSTEME("Message système", "⚙️", "#95a5a6"),
    NOTIFICATION("Notification", "🔔", "#f39c12"),
    ALERTE("Alerte", "⚠️", "#e67e22"),
    DIFFUSION("Diffusion", "📢", "#9b59b6");
    
    private final String libelle;
    private final String icone;
    private final String couleur;
    
    TypeMessage(String libelle, String icone, String couleur) {
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
     * Retourne le type à partir de son libellé
     */
    public static TypeMessage fromLibelle(String libelle) {
        for (TypeMessage type : values()) {
            if (type.libelle.equalsIgnoreCase(libelle)) {
                return type;
            }
        }
        return null;
    }
    
    /**
     * Retourne le type à partir de la valeur de la base de données
     */
    public static TypeMessage fromDatabase(String dbValue) {
        if (dbValue == null) return null;
        
        try {
            return valueOf(dbValue.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
    
    /**
     * Vérifie si c'est un message système
     */
    public boolean isSysteme() {
        return this == SYSTEME || this == NOTIFICATION || this == ALERTE;
    }
    
    /**
     * Vérifie si c'est un message utilisateur
     */
    public boolean isUtilisateur() {
        return this == INTERNE || this == DIFFUSION;
    }
    
    /**
     * Vérifie si le message nécessite une attention immédiate
     */
    public boolean necessiteAttention() {
        return this == ALERTE;
    }
    
    /**
     * Retourne le style CSS pour l'affichage
     */
    public String getStyle() {
        return "-fx-background-color: " + couleur + "; -fx-text-fill: white;";
    }
    
    @Override
    public String toString() {
        return libelle;
    }
}