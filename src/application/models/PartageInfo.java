package application.models;

import application.models.User;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Classe pour stocker les informations de partage d'un document
 */
public class PartageInfo {
    
    private List<User> utilisateurs;
    private String niveauAcces;
    private LocalDate dateExpiration;
    private boolean notifierEmail;
    private String message;
    
    public PartageInfo() {
        this.utilisateurs = new ArrayList<>();
    }
    
    // Getters et Setters
    
    public List<User> getUtilisateurs() {
        return utilisateurs;
    }
    
    public void setUtilisateurs(List<User> utilisateurs) {
        this.utilisateurs = utilisateurs;
    }
    
    public String getNiveauAcces() {
        return niveauAcces;
    }
    
    public void setNiveauAcces(String niveauAcces) {
        this.niveauAcces = niveauAcces;
    }
    
    public LocalDate getDateExpiration() {
        return dateExpiration;
    }
    
    public void setDateExpiration(LocalDate dateExpiration) {
        this.dateExpiration = dateExpiration;
    }
    
    public boolean isNotifierEmail() {
        return notifierEmail;
    }
    
    public void setNotifierEmail(boolean notifierEmail) {
        this.notifierEmail = notifierEmail;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    @Override
    public String toString() {
        return "PartageInfo{" +
                "utilisateurs=" + utilisateurs.size() +
                ", niveauAcces='" + niveauAcces + '\'' +
                ", dateExpiration=" + dateExpiration +
                ", notifierEmail=" + notifierEmail +
                '}';
    }
}