package application.models;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Modèle représentant un log d'activité dans le système
 */
public class LogActivity {
    private int id;
    private int userId;
    private String userCode;
    private String action;
    private String details;
    private String ipAddress;
    private LocalDateTime timestamp;
    private String statut;
    
    // Constructeurs
    public LogActivity() {
        this.timestamp = LocalDateTime.now();
        this.statut = "Succès";
    }
    
    public LogActivity(int userId, String action, String details, String ipAddress) {
        this();
        this.userId = userId;
        this.action = action;
        this.details = details;
        this.ipAddress = ipAddress;
    }
    
    public LogActivity(String timestamp, String userCode, String action, String details, String ipAddress, String statut) {
        this.timestamp = parseTimestamp(timestamp);
        this.userCode = userCode;
        this.action = action;
        this.details = details;
        this.ipAddress = ipAddress;
        this.statut = statut;
    }
    
    // Getters et Setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public int getUserId() {
        return userId;
    }
    
    public void setUserId(int userId) {
        this.userId = userId;
    }
    
    public String getUserCode() {
        return userCode;
    }
    
    public void setUserCode(String userCode) {
        this.userCode = userCode;
    }
    
    public String getAction() {
        return action;
    }
    
    public void setAction(String action) {
        this.action = action;
    }
    
    public String getDetails() {
        return details;
    }
    
    public void setDetails(String details) {
        this.details = details;
    }
    
    public String getIpAddress() {
        return ipAddress;
    }
    
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getStatut() {
        return statut;
    }
    
    public void setStatut(String statut) {
        this.statut = statut;
    }
    
    // Méthodes utilitaires
    
    /**
     * Retourne le timestamp formaté
     */
    public String getTimestampFormatted() {
        if (timestamp == null) return "";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        return timestamp.format(formatter);
    }
    
    /**
     * Retourne une version courte du timestamp
     */
    public String getTimestampShort() {
        if (timestamp == null) return "";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM HH:mm");
        return timestamp.format(formatter);
    }
    
    /**
     * Parse un timestamp au format string vers LocalDateTime
     */
    private LocalDateTime parseTimestamp(String timestampStr) {
        if (timestampStr == null || timestampStr.isEmpty()) {
            return LocalDateTime.now();
        }
        
        try {
            // Format: "dd/MM/yyyy HH:mm:ss"
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
            return LocalDateTime.parse(timestampStr, formatter);
        } catch (Exception e) {
            System.err.println("Erreur lors du parsing du timestamp: " + timestampStr);
            return LocalDateTime.now();
        }
    }
    
    /**
     * Retourne le type d'action pour le filtrage
     */
    public String getActionType() {
        if (action == null) return "Autre";
        
        String actionUpper = action.toUpperCase();
        
        if (actionUpper.contains("CONNEXION") || actionUpper.contains("LOGIN")) {
            return "Connexion";
        } else if (actionUpper.contains("DECONNEXION") || actionUpper.contains("LOGOUT")) {
            return "Déconnexion";
        } else if (actionUpper.contains("CREATION") || actionUpper.contains("CREATE")) {
            return "Création";
        } else if (actionUpper.contains("MODIFICATION") || actionUpper.contains("UPDATE") || actionUpper.contains("EDIT")) {
            return "Modification";
        } else if (actionUpper.contains("SUPPRESSION") || actionUpper.contains("DELETE")) {
            return "Suppression";
        } else if (actionUpper.contains("CONSULTATION") || actionUpper.contains("VIEW") || actionUpper.contains("READ")) {
            return "Consultation";
        } else {
            return "Autre";
        }
    }
    
    /**
     * Retourne la couleur associée au type d'action
     */
    public String getActionColor() {
        String type = getActionType();
        
        switch (type) {
            case "Connexion":
                return "#27ae60"; // Vert
            case "Déconnexion":
                return "#7f8c8d"; // Gris
            case "Création":
                return "#3498db"; // Bleu
            case "Modification":
                return "#f39c12"; // Orange
            case "Suppression":
                return "#e74c3c"; // Rouge
            case "Consultation":
                return "#9b59b6"; // Violet
            default:
                return "#95a5a6"; // Gris clair
        }
    }
    
    /**
     * Retourne l'icône associée au type d'action
     */
    public String getActionIcon() {
        String type = getActionType();
        
        switch (type) {
            case "Connexion":
                return "🔓";
            case "Déconnexion":
                return "🔒";
            case "Création":
                return "➕";
            case "Modification":
                return "✏️";
            case "Suppression":
                return "🗑️";
            case "Consultation":
                return "👁️";
            default:
                return "📋";
        }
    }
    
    /**
     * Retourne un résumé court du log
     */
    public String getSummary() {
        StringBuilder summary = new StringBuilder();
        
        if (userCode != null) {
            summary.append(userCode).append(" - ");
        }
        
        summary.append(action);
        
        if (details != null && !details.isEmpty()) {
            String shortDetails = details.length() > 50 
                ? details.substring(0, 47) + "..." 
                : details;
            summary.append(": ").append(shortDetails);
        }
        
        return summary.toString();
    }
    
    /**
     * Vérifie si le log correspond à une action sensible
     */
    public boolean isSensitiveAction() {
        if (action == null) return false;
        
        String actionUpper = action.toUpperCase();
        return actionUpper.contains("SUPPRESSION") 
            || actionUpper.contains("DELETE")
            || actionUpper.contains("MODIFICATION_ROLE")
            || actionUpper.contains("MODIFICATION_PERMISSION")
            || actionUpper.contains("ADMIN");
    }
    
    /**
     * Retourne le temps écoulé depuis l'action
     */
    public String getTimeAgo() {
        if (timestamp == null) return "";
        
        LocalDateTime now = LocalDateTime.now();
        java.time.Duration duration = java.time.Duration.between(timestamp, now);
        
        long seconds = duration.getSeconds();
        
        if (seconds < 60) {
            return "À l'instant";
        } else if (seconds < 3600) {
            long minutes = seconds / 60;
            return "Il y a " + minutes + " minute" + (minutes > 1 ? "s" : "");
        } else if (seconds < 86400) {
            long hours = seconds / 3600;
            return "Il y a " + hours + " heure" + (hours > 1 ? "s" : "");
        } else {
            long days = seconds / 86400;
            return "Il y a " + days + " jour" + (days > 1 ? "s" : "");
        }
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LogActivity that = (LogActivity) o;
        return id == that.id;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "LogActivity{" +
                "id=" + id +
                ", userCode='" + userCode + '\'' +
                ", action='" + action + '\'' +
                ", timestamp=" + getTimestampFormatted() +
                ", ipAddress='" + ipAddress + '\'' +
                ", statut='" + statut + '\'' +
                '}';
    }
}