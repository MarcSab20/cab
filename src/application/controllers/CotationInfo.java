package application.controllers;

import application.models.User;
import java.time.LocalDateTime;

/**
 * Classe pour stocker les informations de cotation d'un courrier
 */
public class CotationInfo {
    private User utilisateur;
    private String commentaire;
    private LocalDateTime dateEcheance;
    private int delaiJours;
    private boolean notifierUtilisateur;
    
    public User getUtilisateur() {
        return utilisateur;
    }
    
    public void setUtilisateur(User utilisateur) {
        this.utilisateur = utilisateur;
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
    
    public int getDelaiJours() {
        return delaiJours;
    }
    
    public void setDelaiJours(int delaiJours) {
        this.delaiJours = delaiJours;
    }
    
    public boolean isNotifierUtilisateur() {
        return notifierUtilisateur;
    }
    
    public void setNotifierUtilisateur(boolean notifierUtilisateur) {
        this.notifierUtilisateur = notifierUtilisateur;
    }
}