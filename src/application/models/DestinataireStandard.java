package application.models;

import java.time.LocalDateTime;

/**
 * Modèle pour un destinataire standard
 */
public class DestinataireStandard {
    private int id;
    private String code;
    private String libelle;
    private int nombreUtilisations;
    private int ordreAffichage;
    private LocalDateTime derniereUtilisation;
    private boolean actif;
    
    public DestinataireStandard() {
    }
    
    public DestinataireStandard(String code, String libelle) {
        this.code = code;
        this.libelle = libelle;
        this.actif = true;
        this.nombreUtilisations = 0;
    }
    
    // Getters et Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    
    public String getLibelle() { return libelle; }
    public void setLibelle(String libelle) { this.libelle = libelle; }
    
    public int getNombreUtilisations() { return nombreUtilisations; }
    public void setNombreUtilisations(int nombreUtilisations) { 
        this.nombreUtilisations = nombreUtilisations; 
    }
    
    public int getOrdreAffichage() { return ordreAffichage; }
    public void setOrdreAffichage(int ordreAffichage) { this.ordreAffichage = ordreAffichage; }
    
    public LocalDateTime getDerniereUtilisation() { return derniereUtilisation; }
    public void setDerniereUtilisation(LocalDateTime derniereUtilisation) { 
        this.derniereUtilisation = derniereUtilisation; 
    }
    
    public boolean isActif() { return actif; }
    public void setActif(boolean actif) { this.actif = actif; }
    
    @Override
    public String toString() {
        return libelle;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        DestinataireStandard that = (DestinataireStandard) obj;
        return id == that.id;
    }
    
    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}