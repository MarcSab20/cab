package application.models;


/**
 * Énumération des statuts d'une étape de workflow
 */
public enum StatutEtapeWorkflow {
    EN_ATTENTE("En attente", "⏸️", "#f39c12"),
    EN_COURS("En cours", "▶️", "#3498db"),
    TERMINE("Terminé", "✅", "#27ae60"),
    REJETE("Rejeté", "❌", "#e74c3c"),
    TRANSFERE("Transféré", "➡️", "#9b59b6");
    
    private final String libelle;
    private final String icone;
    private final String couleur;
    
    StatutEtapeWorkflow(String libelle, String icone, String couleur) {
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
    
    public static StatutEtapeWorkflow fromString(String value) {
        if (value == null) return null;
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
    
    @Override
    public String toString() {
        return libelle;
    }
}

