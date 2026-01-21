package application.models;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Modèle représentant la présence/statut d'un utilisateur
 */
public class UserPresence {
    
    public enum Statut {
        ONLINE("En ligne", "🟢", "#27ae60"),
        AWAY("Absent", "🟡", "#f39c12"),
        OFFLINE("Hors ligne", "⚫", "#95a5a6");
        
        private final String libelle;
        private final String icone;
        private final String couleur;
        
        Statut(String libelle, String icone, String couleur) {
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
    }
    
    private int userId;
    private User user;
    private Statut statut;
    private LocalDateTime derniereActivite;
    private String ipAddress;
    private String hostname;
    
    public UserPresence() {
        this.statut = Statut.OFFLINE;
        this.derniereActivite = LocalDateTime.now();
    }
    
    public UserPresence(int userId) {
        this();
        this.userId = userId;
    }
    
    // Getters et Setters
    public int getUserId() {
        return userId;
    }
    
    public void setUserId(int userId) {
        this.userId = userId;
    }
    
    public User getUser() {
        return user;
    }
    
    public void setUser(User user) {
        this.user = user;
    }
    
    public Statut getStatut() {
        return statut;
    }
    
    public void setStatut(Statut statut) {
        this.statut = statut;
        this.derniereActivite = LocalDateTime.now();
    }
    
    public LocalDateTime getDerniereActivite() {
        return derniereActivite;
    }
    
    public void setDerniereActivite(LocalDateTime derniereActivite) {
        this.derniereActivite = derniereActivite;
    }
    
    public String getIpAddress() {
        return ipAddress;
    }
    
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
    
    public String getHostname() {
        return hostname;
    }
    
    public void setHostname(String hostname) {
        this.hostname = hostname;
    }
    
    // Méthodes utilitaires
    
    /**
     * Met à jour l'activité
     */
    public void updateActivite() {
        this.derniereActivite = LocalDateTime.now();
        if (this.statut == Statut.OFFLINE) {
            this.statut = Statut.ONLINE;
        }
    }
    
    /**
     * Marque l'utilisateur comme en ligne
     */
    public void setOnline(String ipAddress, String hostname) {
        this.statut = Statut.ONLINE;
        this.derniereActivite = LocalDateTime.now();
        this.ipAddress = ipAddress;
        this.hostname = hostname;
    }
    
    /**
     * Marque l'utilisateur comme absent
     */
    public void setAway() {
        this.statut = Statut.AWAY;
        this.derniereActivite = LocalDateTime.now();
    }
    
    /**
     * Marque l'utilisateur comme hors ligne
     */
    public void setOffline() {
        this.statut = Statut.OFFLINE;
        this.derniereActivite = LocalDateTime.now();
    }
    
    /**
     * Vérifie si l'utilisateur est en ligne
     */
    public boolean isOnline() {
        return statut == Statut.ONLINE;
    }
    
    /**
     * Vérifie si l'utilisateur est actif (en ligne ou absent récemment)
     */
    public boolean isActif() {
        if (statut == Statut.OFFLINE) {
            return false;
        }
        
        // Considéré actif si dernière activité < 5 minutes
        return derniereActivite != null && 
               derniereActivite.isAfter(LocalDateTime.now().minusMinutes(5));
    }
    
    /**
     * Retourne le temps écoulé depuis la dernière activité
     */
    public String getTempsInactivite() {
        if (derniereActivite == null) {
            return "Inconnu";
        }
        
        java.time.Duration duree = java.time.Duration.between(derniereActivite, LocalDateTime.now());
        long minutes = duree.toMinutes();
        
        if (minutes < 1) {
            return "À l'instant";
        } else if (minutes < 60) {
            return minutes + " minute" + (minutes > 1 ? "s" : "");
        } else {
            long heures = minutes / 60;
            return heures + " heure" + (heures > 1 ? "s" : "");
        }
    }
    
    /**
     * Retourne le statut avec icône
     */
    public String getStatutAvecIcone() {
        return statut.getIcone() + " " + statut.getLibelle();
    }
    
    /**
     * Retourne le style CSS du statut
     */
    public String getStyleStatut() {
        return "-fx-text-fill: " + statut.getCouleur() + ";";
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserPresence that = (UserPresence) o;
        return userId == that.userId;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }
    
    @Override
    public String toString() {
        return "UserPresence{" +
                "userId=" + userId +
                ", statut=" + statut +
                ", derniereActivite=" + derniereActivite +
                ", ipAddress='" + ipAddress + '\'' +
                '}';
    }
}