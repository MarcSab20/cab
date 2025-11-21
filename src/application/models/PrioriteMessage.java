package application.models;

/**
 * Énumération des priorités d'un message
 */
public enum PrioriteMessage {
    BASSE("Basse", 1, "🔵", "#3498db"),
    NORMALE("Normale", 2, "🟢", "#27ae60"),
    HAUTE("Haute", 3, "🟠", "#e67e22"),
    TRES_HAUTE("Très haute", 4, "🔴", "#e74c3c");
    
    private final String libelle;
    private final int niveau;
    private final String icone;
    private final String couleur;
    
    PrioriteMessage(String libelle, int niveau, String icone, String couleur) {
        this.libelle = libelle;
        this.niveau = niveau;
        this.icone = icone;
        this.couleur = couleur;
    }
    
    public String getLibelle() {
        return libelle;
    }
    
    public int getNiveau() {
        return niveau;
    }
    
    public String getIcone() {
        return icone;
    }
    
    public String getCouleur() {
        return couleur;
    }
    
    /**
     * Retourne la priorité à partir de son libellé
     */
    public static PrioriteMessage fromLibelle(String libelle) {
        for (PrioriteMessage priorite : values()) {
            if (priorite.libelle.equalsIgnoreCase(libelle)) {
                return priorite;
            }
        }
        return null;
    }
    
    /**
     * Retourne la priorité à partir de la valeur de la base de données
     */
    public static PrioriteMessage fromDatabase(String dbValue) {
        if (dbValue == null) return null;
        
        try {
            return valueOf(dbValue.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
    
    /**
     * Retourne la priorité à partir de son niveau
     */
    public static PrioriteMessage fromNiveau(int niveau) {
        for (PrioriteMessage priorite : values()) {
            if (priorite.niveau == niveau) {
                return priorite;
            }
        }
        return NORMALE;
    }
    
    /**
     * Vérifie si la priorité est critique (haute ou très haute)
     */
    public boolean isCritique() {
        return this == HAUTE || this == TRES_HAUTE;
    }
    
    /**
     * Vérifie si la priorité est normale ou basse
     */
    public boolean isNormale() {
        return this == NORMALE || this == BASSE;
    }
    
    /**
     * Compare deux priorités
     */
    public boolean isSuperieurA(PrioriteMessage autre) {
        return this.niveau > autre.niveau;
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