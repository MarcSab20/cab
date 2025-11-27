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
    private LocalDateTime dateReception;
    private LocalDateTime dateDocument;
    private LocalDateTime dateTraitement;
    private StatutCourrier statut;
    private String priorite;
    private String notes;
    private boolean workflowActif;
    private String serviceActuel;
    private Integer etapeActuelle;
    private boolean workflowTermine;
    private LocalDateTime dateCreation;
    
    // Constructeurs
    public Courrier() {
        this.workflowActif = false;
        this.workflowTermine = false;
        this.statut = StatutCourrier.NOUVEAU;
        this.typeCourrier = TypeCourrier.ENTRANT;
        this.priorite = "normale";
        this.dateCreation = LocalDateTime.now();
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
    
    // Méthode pour compatibilité avec String
    public void setTypeCourrierFromString(String type) {
        this.typeCourrier = TypeCourrier.fromString(type);
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
    
    public LocalDateTime getDateReception() {
        return dateReception;
    }
    
    public void setDateReception(LocalDateTime dateReception) {
        this.dateReception = dateReception;
    }
    
    public LocalDateTime getDateDocument() {
        return dateDocument;
    }
    
    public void setDateDocument(LocalDateTime dateDocument) {
        this.dateDocument = dateDocument;
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
    
    // Méthode pour compatibilité avec String
    public void setStatutFromString(String statut) {
        this.statut = StatutCourrier.fromString(statut);
    }
    
    public String getPriorite() {
        return priorite;
    }
    
    public void setPriorite(String priorite) {
        this.priorite = priorite;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
    
    public boolean isWorkflowActif() {
        return workflowActif;
    }
    
    public void setWorkflowActif(boolean workflowActif) {
        this.workflowActif = workflowActif;
    }
    
    public String getServiceActuel() {
        return serviceActuel;
    }
    
    public void setServiceActuel(String serviceActuel) {
        this.serviceActuel = serviceActuel;
    }
    
    public Integer getEtapeActuelle() {
        return etapeActuelle;
    }
    
    public void setEtapeActuelle(Integer etapeActuelle) {
        this.etapeActuelle = etapeActuelle;
    }
    
    public boolean isWorkflowTermine() {
        return workflowTermine;
    }
    
    public void setWorkflowTermine(boolean workflowTermine) {
        this.workflowTermine = workflowTermine;
    }
    
    public LocalDateTime getDateCreation() {
        return dateCreation;
    }
    
    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }
    
    // Méthodes utilitaires
    
    /**
     * Retourne la date de réception formatée
     */
    public String getDateReceptionFormatee() {
        if (dateReception == null) return "";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        return dateReception.format(formatter);
    }
    
    /**
     * Retourne la date de réception avec heure formatée
     */
    public String getDateReceptionAvecHeureFormatee() {
        if (dateReception == null) return "";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        return dateReception.format(formatter);
    }
    
    /**
     * Retourne la date du document formatée
     */
    public String getDateDocumentFormatee() {
        if (dateDocument == null) return "";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        return dateDocument.format(formatter);
    }
    
    /**
     * Retourne la date de traitement formatée
     */
    public String getDateTraitementFormatee() {
        if (dateTraitement == null) return "";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        return dateTraitement.format(formatter);
    }
    
    /**
     * Retourne le libellé du statut
     */
    public String getStatutLibelle() {
        return statut != null ? statut.getLibelle() : "";
    }
    
    /**
     * Retourne le libellé du type
     */
    public String getTypeCourrierLibelle() {
        return typeCourrier != null ? typeCourrier.getLibelle() : "";
    }
    
    /**
     * Retourne le libellé de la priorité
     */
    public String getPrioriteLibelle() {
        if (priorite == null) return "";
        
        switch (priorite.toLowerCase()) {
            case "haute":
            case "elevee":
                return "Haute";
            case "normale":
                return "Normale";
            case "basse":
                return "Basse";
            case "urgent":
                return "Urgent";
            default:
                return priorite;
        }
    }
    
    /**
     * Retourne l'icône de priorité
     */
    public String getPrioriteIcone() {
        if (priorite == null) return "⚪";
        
        switch (priorite.toLowerCase()) {
            case "haute":
            case "elevee":
                return "🔴";
            case "normale":
                return "🟡";
            case "basse":
                return "🟢";
            case "urgent":
                return "🚨";
            default:
                return "⚪";
        }
    }
    
    /**
     * Retourne la couleur associée à la priorité
     */
    public String getPrioriteCouleur() {
        if (priorite == null) return "#9e9e9e";
        
        switch (priorite.toLowerCase()) {
            case "haute":
            case "elevee":
                return "#e74c3c";
            case "normale":
                return "#f39c12";
            case "basse":
                return "#27ae60";
            case "urgent":
                return "#c0392b";
            default:
                return "#9e9e9e";
        }
    }
    
    /**
     * Retourne la couleur associée au statut
     */
    public String getStatutCouleur() {
        return statut != null ? statut.getCouleur() : "#9e9e9e";
    }
    
    /**
     * Retourne l'icône du statut
     */
    public String getStatutIcone() {
        return statut != null ? statut.getIcone() : "⚪";
    }
    
    /**
     * Retourne l'icône du type de courrier
     */
    public String getIcone() {
        return typeCourrier != null ? typeCourrier.getIcone() : "📧";
    }
    
    /**
     * Retourne le libellé du type
     */
    public String getLibelle() {
        return typeCourrier != null ? typeCourrier.getLibelle() : "";
    }
    
    /**
     * Vérifie si le courrier est en workflow
     */
    public boolean estEnWorkflow() {
        return workflowActif && !workflowTermine;
    }
    
    /**
     * Vérifie si le courrier est en retard
     */
    public boolean isEnRetard() {
        // À implémenter selon les règles métier
        // Par exemple, vérifier si une échéance est dépassée
        return false;
    }
    
    /**
     * Active le workflow pour ce courrier
     */
    public void activerWorkflow(String serviceInitial) {
        this.workflowActif = true;
        this.serviceActuel = serviceInitial;
        this.etapeActuelle = 1;
        this.statut = StatutCourrier.EN_COURS;
    }
    
    /**
     * Termine le workflow
     */
    public void terminerWorkflow() {
        this.workflowActif = false;
        this.workflowTermine = true;
        this.statut = StatutCourrier.TRAITE;
        this.dateTraitement = LocalDateTime.now();
    }
    
    /**
     * Retourne un résumé du courrier
     */
    public String getResume() {
        StringBuilder resume = new StringBuilder();
        resume.append(numeroCourrier).append(" - ");
        resume.append(objet);
        
        if (priorite != null && (priorite.equalsIgnoreCase("haute") || priorite.equalsIgnoreCase("urgent"))) {
            resume.append(" [").append(getPrioriteLibelle()).append("]");
        }
        
        return resume.toString();
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
                ", objet='" + objet + '\'' +
                ", statut='" + statut + '\'' +
                ", priorite='" + priorite + '\'' +
                ", workflowActif=" + workflowActif +
                ", serviceActuel='" + serviceActuel + '\'' +
                '}';
    }
}