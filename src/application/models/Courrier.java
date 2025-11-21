package application.models;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Modèle représentant un courrier dans le système
 */
public class Courrier {
    private int id;
    private String numeroCourrier;
    private TypeCourrier typeCourrier;
    private String objet;
    private String expediteur;
    private String destinataire;
    private LocalDateTime dateCourrier;
    private LocalDateTime dateReception;
    private LocalDateTime dateTraitement;
    private StatutCourrier statut;
    private PrioriteCourrier priorite;
    private Document document;
    private User traitePar;
    private String notes;
    
    // Constructeurs
    public Courrier() {
        this.statut = StatutCourrier.EN_ATTENTE;
        this.priorite = PrioriteCourrier.NORMALE;
        this.dateReception = LocalDateTime.now();
    }
    
    public Courrier(String numeroCourrier, TypeCourrier typeCourrier, String objet) {
        this();
        this.numeroCourrier = numeroCourrier;
        this.typeCourrier = typeCourrier;
        this.objet = objet;
    }
    
    // Getters et Setters
    public int getId() { 
        return id; 
    }
    
    public void setId(int id) { 
        this.id = id; 
    }
    
    public String getNumeroCourrier() { 
        return numeroCourrier; 
    }
    
    public void setNumeroCourrier(String numeroCourrier) { 
        this.numeroCourrier = numeroCourrier; 
    }
    
    public TypeCourrier getTypeCourrier() { 
        return typeCourrier; 
    }
    
    public void setTypeCourrier(TypeCourrier typeCourrier) { 
        this.typeCourrier = typeCourrier; 
    }
    
    public String getObjet() { 
        return objet; 
    }
    
    public void setObjet(String objet) { 
        this.objet = objet; 
    }
    
    public String getExpediteur() { 
        return expediteur; 
    }
    
    public void setExpediteur(String expediteur) { 
        this.expediteur = expediteur; 
    }
    
    public String getDestinataire() { 
        return destinataire; 
    }
    
    public void setDestinataire(String destinataire) { 
        this.destinataire = destinataire; 
    }
    
    public LocalDateTime getDateCourrier() { 
        return dateCourrier; 
    }
    
    public void setDateCourrier(LocalDateTime dateCourrier) { 
        this.dateCourrier = dateCourrier; 
    }
    
    public LocalDateTime getDateReception() { 
        return dateReception; 
    }
    
    public void setDateReception(LocalDateTime dateReception) { 
        this.dateReception = dateReception; 
    }
    
    public LocalDateTime getDateTraitement() { 
        return dateTraitement; 
    }
    
    public void setDateTraitement(LocalDateTime dateTraitement) { 
        this.dateTraitement = dateTraitement; 
    }
    
    public StatutCourrier getStatut() { 
        return statut; 
    }
    
    public void setStatut(StatutCourrier statut) { 
        this.statut = statut; 
    }
    
    public PrioriteCourrier getPriorite() { 
        return priorite; 
    }
    
    public void setPriorite(PrioriteCourrier priorite) { 
        this.priorite = priorite; 
    }
    
    public Document getDocument() { 
        return document; 
    }
    
    public void setDocument(Document document) { 
        this.document = document; 
    }
    
    public User getTraitePar() { 
        return traitePar; 
    }
    
    public void setTraitePar(User traitePar) { 
        this.traitePar = traitePar; 
    }
    
    public String getNotes() { 
        return notes; 
    }
    
    public void setNotes(String notes) { 
        this.notes = notes; 
    }
    
    // Méthodes utilitaires
    
    /**
     * Génère un numéro de courrier unique
     */
    public static String genererNumeroCourrier(TypeCourrier type) {
        String prefix = type == TypeCourrier.ENTRANT ? "CE" : "CS";
        int year = LocalDateTime.now().getYear();
        long timestamp = System.currentTimeMillis() % 100000;
        return String.format("%s-%d-%05d", prefix, year, timestamp);
    }
    
    /**
     * Retourne la date de réception formatée
     */
    public String getDateReceptionFormatee() {
        if (dateReception == null) return "";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        return dateReception.format(formatter);
    }
    
    /**
     * Retourne la date de traitement formatée
     */
    public String getDateTraitementFormatee() {
        if (dateTraitement == null) return "Non traité";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        return dateTraitement.format(formatter);
    }
    
    /**
     * Calcule le délai de traitement en jours
     */
    public long getDelaiTraitementJours() {
        if (dateReception == null || dateTraitement == null) return 0;
        return java.time.Duration.between(dateReception, dateTraitement).toDays();
    }
    
    /**
     * Vérifie si le courrier est en retard
     */
    public boolean isEnRetard() {
        if (statut.isTermine()) return false;
        if (dateReception == null) return false;
        
        // Définir un délai selon la priorité
        int delaiMaxJours = switch (priorite) {
            case URGENTE -> 1;
            case HAUTE -> 3;
            case NORMALE -> 7;
            case BASSE -> 14;
        };
        
        LocalDateTime dateEcheance = dateReception.plusDays(delaiMaxJours);
        return LocalDateTime.now().isAfter(dateEcheance);
    }
    
    /**
     * Marque le courrier comme traité
     */
    public void marquerTraite(User utilisateur) {
        this.statut = StatutCourrier.TRAITE;
        this.dateTraitement = LocalDateTime.now();
        this.traitePar = utilisateur;
    }
    
    /**
     * Archive le courrier
     */
    public void archiver() {
        if (this.statut != StatutCourrier.TRAITE) {
            throw new IllegalStateException("Seuls les courriers traités peuvent être archivés");
        }
        this.statut = StatutCourrier.ARCHIVE;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Courrier courrier = (Courrier) o;
        return id == courrier.id;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "Courrier{" +
                "id=" + id +
                ", numero='" + numeroCourrier + '\'' +
                ", type=" + typeCourrier +
                ", objet='" + objet + '\'' +
                ", statut=" + statut +
                ", priorite=" + priorite +
                '}';
    }
}