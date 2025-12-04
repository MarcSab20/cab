package application.controllers;

import application.models.ServiceHierarchy;
import java.time.LocalDateTime;

/**
 * Classe pour stocker les informations de transfert d'un courrier
 */
public class TransfertInfo {
    private ServiceHierarchy serviceDestination;
    private String commentaire;
    private LocalDateTime dateEcheance;
    private int delaiHeures;
    private boolean urgent;
    
    public ServiceHierarchy getServiceDestination() {
        return serviceDestination;
    }
    
    public void setServiceDestination(ServiceHierarchy serviceDestination) {
        this.serviceDestination = serviceDestination;
    }
    
    public String getCommentaire() {
        return commentaire;
    }
    
    public void setCommentaire(String commentaire) {
        this.commentaire = commentaire;
    }
    
    public LocalDateTime getDateEcheance() {
        return dateEcheance;
    }
    
    public void setDateEcheance(LocalDateTime dateEcheance) {
        this.dateEcheance = dateEcheance;
    }
    
    public int getDelaiHeures() {
        return delaiHeures;
    }
    
    public void setDelaiHeures(int delaiHeures) {
        this.delaiHeures = delaiHeures;
    }
    
    public boolean isUrgent() {
        return urgent;
    }
    
    public void setUrgent(boolean urgent) {
        this.urgent = urgent;
    }
}