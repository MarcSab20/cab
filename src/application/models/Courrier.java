package application.models;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Modèle Courrier lié obligatoirement à un Document
 */
public class Courrier {
    
    public enum TypeCourrier {
        ENTRANT("Entrant", "📨"),
        SORTANT("Sortant", "📤"),
        INTERNE("Interne", "📬");
        
        private final String libelle;
        private final String icone;
        
        TypeCourrier(String libelle, String icone) {
            this.libelle = libelle;
            this.icone = icone;
        }
        
        public String getLibelle() { return libelle; }
        public String getIcone() { return icone; }
    }
    
    public enum StatutCourrier {
        NOUVEAU("Nouveau", "🆕"),
        EN_COURS("En cours", "⏳"),
        TRAITE("Traité", "✅"),
        ARCHIVE("Archivé", "📁");
        
        private final String libelle;
        private final String icone;
        
        StatutCourrier(String libelle, String icone) {
            this.libelle = libelle;
            this.icone = icone;
        }
        
        public String getLibelle() { return libelle; }
        public String getIcone() { return icone; }
    }
    
    public enum PrioriteCourrier {
        NORMALE("Normale", "⚪"),
        URGENTE("Urgente", "🟡"),
        TRES_URGENTE("Très urgente", "🔴");
        
        private final String libelle;
        private final String icone;
        
        PrioriteCourrier(String libelle, String icone) {
            this.libelle = libelle;
            this.icone = icone;
        }
        
        public String getLibelle() { return libelle; }
        public String getIcone() { return icone; }
    }
    
    private int id;
    private String codeCourrier;
    private int documentId; // OBLIGATOIRE
    private TypeCourrier typeCourrier;
    private String objet;
    private String expediteur;
    private String destinataire;
    private String reference;
    private LocalDate dateCourrier;
    private PrioriteCourrier priorite;
    private String observations;
    private boolean confidentiel;
    private StatutCourrier statut;
    private LocalDateTime dateArchivage;
    private Integer creePar;
    private LocalDateTime dateCreation;
    private LocalDateTime dateModification;
    
    public Courrier() {
        this.statut = StatutCourrier.NOUVEAU;
        this.priorite = PrioriteCourrier.NORMALE;
        this.confidentiel = false;
        this.dateCreation = LocalDateTime.now();
    }
    
    // Getters et Setters
    
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getCodeCourrier() { return codeCourrier; }
    public void setCodeCourrier(String codeCourrier) { this.codeCourrier = codeCourrier; }
    
    public int getDocumentId() { return documentId; }
    public void setDocumentId(int documentId) { this.documentId = documentId; }
    
    public TypeCourrier getTypeCourrier() { return typeCourrier; }
    public void setTypeCourrier(TypeCourrier typeCourrier) { this.typeCourrier = typeCourrier; }
    
    public String getObjet() { return objet; }
    public void setObjet(String objet) { this.objet = objet; }
    
    public String getExpediteur() { return expediteur; }
    public void setExpediteur(String expediteur) { this.expediteur = expediteur; }
    
    public String getDestinataire() { return destinataire; }
    public void setDestinataire(String destinataire) { this.destinataire = destinataire; }
    
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
    
    public LocalDate getDateCourrier() { return dateCourrier; }
    public void setDateCourrier(LocalDate dateCourrier) { this.dateCourrier = dateCourrier; }
    
    public PrioriteCourrier getPriorite() { return priorite; }
    public void setPriorite(PrioriteCourrier priorite) { this.priorite = priorite; }
    
    public String getObservations() { return observations; }
    public void setObservations(String observations) { this.observations = observations; }
    
    public boolean isConfidentiel() { return confidentiel; }
    public void setConfidentiel(boolean confidentiel) { this.confidentiel = confidentiel; }
    
    public StatutCourrier getStatut() { return statut; }
    public void setStatut(StatutCourrier statut) { this.statut = statut; }
    
    public LocalDateTime getDateArchivage() { return dateArchivage; }
    public void setDateArchivage(LocalDateTime dateArchivage) { this.dateArchivage = dateArchivage; }
    
    public Integer getCreePar() { return creePar; }
    public void setCreePar(Integer creePar) { this.creePar = creePar; }
    
    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }
    
    public LocalDateTime getDateModification() { return dateModification; }
    public void setDateModification(LocalDateTime dateModification) { this.dateModification = dateModification; }
    
    // Méthodes utilitaires
    
    public boolean peutEtreArchive() {
        return this.statut == StatutCourrier.TRAITE;
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
                ", codeCourrier='" + codeCourrier + '\'' +
                ", documentId=" + documentId +
                ", typeCourrier=" + typeCourrier +
                ", objet='" + objet + '\'' +
                ", statut=" + statut +
                '}';
    }
}