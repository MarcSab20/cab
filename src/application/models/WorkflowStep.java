package application.models;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;



/**
 * Modèle représentant une étape dans le workflow d'un courrier
 */
public class WorkflowStep {
    private int id;
    private int courrierId;
    private int etapeNumero;
    private String serviceCode;
    private String serviceName;
    private Integer userId;
    private String userName;
    private String action;
    private String commentaire;
    private LocalDateTime dateAction;
    private StatutEtapeWorkflow statutEtape;
    private Integer delaiTraitement; // En heures
    private LocalDateTime dateEcheance;
    
    // Constructeurs
    public WorkflowStep() {
        this.statutEtape = StatutEtapeWorkflow.EN_ATTENTE;
        this.dateAction = LocalDateTime.now();
    }
    
    public WorkflowStep(int courrierId, int etapeNumero, String serviceCode) {
        this();
        this.courrierId = courrierId;
        this.etapeNumero = etapeNumero;
        this.serviceCode = serviceCode;
    }
    
    // Getters et Setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public int getCourrierId() {
        return courrierId;
    }
    
    public void setCourrierId(int courrierId) {
        this.courrierId = courrierId;
    }
    
    public int getEtapeNumero() {
        return etapeNumero;
    }
    
    public void setEtapeNumero(int etapeNumero) {
        this.etapeNumero = etapeNumero;
    }
    
    public String getServiceCode() {
        return serviceCode;
    }
    
    public void setServiceCode(String serviceCode) {
        this.serviceCode = serviceCode;
    }
    
    public String getServiceName() {
        return serviceName;
    }
    
    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }
    
    public Integer getUserId() {
        return userId;
    }
    
    public void setUserId(Integer userId) {
        this.userId = userId;
    }
    
    public String getUserName() {
        return userName;
    }
    
    public void setUserName(String userName) {
        this.userName = userName;
    }
    
    public String getAction() {
        return action;
    }
    
    public void setAction(String action) {
        this.action = action;
    }
    
    public String getCommentaire() {
        return commentaire;
    }
    
    public void setCommentaire(String commentaire) {
        this.commentaire = commentaire;
    }
    
    public LocalDateTime getDateAction() {
        return dateAction;
    }
    
    public void setDateAction(LocalDateTime dateAction) {
        this.dateAction = dateAction;
    }
    
    public StatutEtapeWorkflow getStatutEtape() {
        return statutEtape;
    }
    
    public void setStatutEtape(StatutEtapeWorkflow statutEtape) {
        this.statutEtape = statutEtape;
    }
    
    public Integer getDelaiTraitement() {
        return delaiTraitement;
    }
    
    public void setDelaiTraitement(Integer delaiTraitement) {
        this.delaiTraitement = delaiTraitement;
        
        // Calculer la date d'échéance si un délai est fourni
        if (delaiTraitement != null && delaiTraitement > 0) {
            this.dateEcheance = dateAction.plusHours(delaiTraitement);
        }
    }
    
    public LocalDateTime getDateEcheance() {
        return dateEcheance;
    }
    
    public void setDateEcheance(LocalDateTime dateEcheance) {
        this.dateEcheance = dateEcheance;
    }
    
    // Méthodes utilitaires
    
    /**
     * Retourne la date d'action formatée
     */
    public String getDateActionFormatee() {
        if (dateAction == null) return "";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        return dateAction.format(formatter);
    }
    
    /**
     * Retourne la date d'échéance formatée
     */
    public String getDateEcheanceFormatee() {
        if (dateEcheance == null) return "Aucune";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        return dateEcheance.format(formatter);
    }
    
    /**
     * Vérifie si l'étape est en retard
     */
    public boolean isEnRetard() {
        if (dateEcheance == null) return false;
        if (statutEtape == StatutEtapeWorkflow.TERMINE || statutEtape == StatutEtapeWorkflow.TRANSFERE) {
            return false;
        }
        return LocalDateTime.now().isAfter(dateEcheance);
    }
    
    /**
     * Calcule le temps écoulé depuis la date d'action
     */
    public String getTempsEcoule() {
        if (dateAction == null) return "";
        
        LocalDateTime now = LocalDateTime.now();
        java.time.Duration duration = java.time.Duration.between(dateAction, now);
        
        long heures = duration.toHours();
        
        if (heures < 1) {
            long minutes = duration.toMinutes();
            return "Il y a " + minutes + " minute" + (minutes > 1 ? "s" : "");
        } else if (heures < 24) {
            return "Il y a " + heures + " heure" + (heures > 1 ? "s" : "");
        } else {
            long jours = duration.toDays();
            return "Il y a " + jours + " jour" + (jours > 1 ? "s" : "");
        }
    }
    
    /**
     * Calcule le temps restant jusqu'à l'échéance
     */
    public String getTempsRestant() {
        if (dateEcheance == null) return "Aucune échéance";
        
        LocalDateTime now = LocalDateTime.now();
        
        if (now.isAfter(dateEcheance)) {
            java.time.Duration duration = java.time.Duration.between(dateEcheance, now);
            long heures = duration.toHours();
            
            if (heures < 24) {
                return "En retard de " + heures + " heure" + (heures > 1 ? "s" : "");
            } else {
                long jours = duration.toDays();
                return "En retard de " + jours + " jour" + (jours > 1 ? "s" : "");
            }
        } else {
            java.time.Duration duration = java.time.Duration.between(now, dateEcheance);
            long heures = duration.toHours();
            
            if (heures < 1) {
                long minutes = duration.toMinutes();
                return "Dans " + minutes + " minute" + (minutes > 1 ? "s" : "");
            } else if (heures < 24) {
                return "Dans " + heures + " heure" + (heures > 1 ? "s" : "");
            } else {
                long jours = duration.toDays();
                return "Dans " + jours + " jour" + (jours > 1 ? "s" : "");
            }
        }
    }
    
    /**
     * Retourne l'icône associée au statut
     */
    public String getIconeStatut() {
        return statutEtape.getIcone();
    }
    
    /**
     * Retourne la couleur associée au statut
     */
    public String getCouleurStatut() {
        return statutEtape.getCouleur();
    }
    
    /**
     * Marque l'étape comme terminée
     */
    public void terminer(User user, String commentaire) {
        this.statutEtape = StatutEtapeWorkflow.TERMINE;
        this.userId = user.getId();
        this.userName = user.getNomComplet();
        this.commentaire = commentaire;
        this.dateAction = LocalDateTime.now();
    }
    
    /**
     * Marque l'étape comme transférée
     */
    public void transferer(User user, String commentaire) {
        this.statutEtape = StatutEtapeWorkflow.TRANSFERE;
        this.userId = user.getId();
        this.userName = user.getNomComplet();
        this.commentaire = commentaire;
        this.dateAction = LocalDateTime.now();
    }
    
    /**
     * Marque l'étape comme rejetée
     */
    public void rejeter(User user, String motif) {
        this.statutEtape = StatutEtapeWorkflow.REJETE;
        this.userId = user.getId();
        this.userName = user.getNomComplet();
        this.commentaire = "REJET: " + motif;
        this.dateAction = LocalDateTime.now();
    }
    
    /**
     * Met l'étape en cours
     */
    public void mettreEnCours(User user) {
        if (this.statutEtape == StatutEtapeWorkflow.EN_ATTENTE) {
            this.statutEtape = StatutEtapeWorkflow.EN_COURS;
            this.userId = user.getId();
            this.userName = user.getNomComplet();
            this.dateAction = LocalDateTime.now();
        }
    }
    
    /**
     * Retourne un résumé de l'étape
     */
    public String getResume() {
        StringBuilder resume = new StringBuilder();
        resume.append(serviceName != null ? serviceName : serviceCode);
        
        if (userName != null && !userName.isEmpty()) {
            resume.append(" - ").append(userName);
        }
        
        resume.append(" - ").append(statutEtape.getLibelle());
        resume.append(" - ").append(getDateActionFormatee());
        
        return resume.toString();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WorkflowStep that = (WorkflowStep) o;
        return id == that.id;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "WorkflowStep{" +
                "etape=" + etapeNumero +
                ", service='" + serviceCode + '\'' +
                ", statut=" + statutEtape +
                ", date=" + getDateActionFormatee() +
                '}';
    }
}