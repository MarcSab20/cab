package application.models;

/**
 * Énumération des priorités d'un courrier
 */
public enum PrioriteCourrier {
    BASSE("Basse", 1, "🔵", "#3498db"),
    NORMALE("Normale", 2, "🟢", "#27ae60"),
    HAUTE("Haute", 3, "🟠", "#e67e22"),
    URGENTE("Urgente", 4, "🔴", "#e74c3c");
    
    private final String libelle;
    private final int niveau;
    private final String icone;
    private final String couleur;
    
    PrioriteCourrier(String libelle, int niveau, String icone, String couleur) {
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
    public static PrioriteCourrier fromLibelle(String libelle) {
        for (PrioriteCourrier priorite : values()) {
            if (priorite.libelle.equalsIgnoreCase(libelle)) {
                return priorite;
            }
        }
        return null;
    }
    
    /**
     * Retourne la priorité à partir de la valeur de la base de données
     */
    public static PrioriteCourrier fromDatabase(String dbValue) {
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
    public static PrioriteCourrier fromNiveau(int niveau) {
        for (PrioriteCourrier priorite : values()) {
            if (priorite.niveau == niveau) {
                return priorite;
            }
        }
        return NORMALE;
    }
    
    /**
     * Vérifie si la priorité est critique (haute ou urgente)
     */
    public boolean isCritique() {
        return this == HAUTE || this == URGENTE;
    }
    
    /**
     * Compare deux priorités
     */
    public boolean isSuperieurA(PrioriteCourrier autre) {
        return this.niveau > autre.niveau;
    }
    
    @Override
    public String toString() {
        return libelle;
    }
}