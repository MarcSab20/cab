package application.models;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Modèle représentant un message dans le système de messagerie interne
 */
public class Message {
    private int id;
    private User expediteur;
    private User destinataire;
    private String objet;
    private String contenu;
    private LocalDateTime dateEnvoi;
    private LocalDateTime dateLecture;
    private boolean lu;
    private PrioriteMessage priorite;
    private TypeMessage typeMessage;
    private String pieceJointe;
    private boolean important;
    private boolean archive;
    private String reponseA; // ID du message parent si c'est une réponse
    private StatutMessage statut;
    
    // Constructeurs
    public Message() {
        this.dateEnvoi = LocalDateTime.now();
        this.lu = false;
        this.priorite = PrioriteMessage.NORMALE;
        this.typeMessage = TypeMessage.INTERNE;
        this.important = false;
        this.archive = false;
        this.statut = StatutMessage.ENVOYE;
    }
    
    public Message(User expediteur, User destinataire, String objet, String contenu) {
        this();
        this.expediteur = expediteur;
        this.destinataire = destinataire;
        this.objet = objet;
        this.contenu = contenu;
    }
    
    // Getters et Setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public User getExpediteur() {
        return expediteur;
    }
    
    public void setExpediteur(User expediteur) {
        this.expediteur = expediteur;
    }
    
    public User getDestinataire() {
        return destinataire;
    }
    
    public void setDestinataire(User destinataire) {
        this.destinataire = destinataire;
    }
    
    public String getObjet() {
        return objet;
    }
    
    public void setObjet(String objet) {
        this.objet = objet;
    }
    
    public String getContenu() {
        return contenu;
    }
    
    public void setContenu(String contenu) {
        this.contenu = contenu;
    }
    
    public LocalDateTime getDateEnvoi() {
        return dateEnvoi;
    }
    
    public void setDateEnvoi(LocalDateTime dateEnvoi) {
        this.dateEnvoi = dateEnvoi;
    }
    
    public LocalDateTime getDateLecture() {
        return dateLecture;
    }
    
    public void setDateLecture(LocalDateTime dateLecture) {
        this.dateLecture = dateLecture;
    }
    
    public boolean isLu() {
        return lu;
    }
    
    public void setLu(boolean lu) {
        this.lu = lu;
        if (lu && this.dateLecture == null) {
            this.dateLecture = LocalDateTime.now();
        }
    }
    
    public PrioriteMessage getPriorite() {
        return priorite;
    }
    
    public void setPriorite(PrioriteMessage priorite) {
        this.priorite = priorite;
    }
    
    public TypeMessage getTypeMessage() {
        return typeMessage;
    }
    
    public void setTypeMessage(TypeMessage typeMessage) {
        this.typeMessage = typeMessage;
    }
    
    public String getPieceJointe() {
        return pieceJointe;
    }
    
    public void setPieceJointe(String pieceJointe) {
        this.pieceJointe = pieceJointe;
    }
    
    public boolean isImportant() {
        return important;
    }
    
    public void setImportant(boolean important) {
        this.important = important;
    }
    
    public boolean isArchive() {
        return archive;
    }
    
    public void setArchive(boolean archive) {
        this.archive = archive;
    }
    
    public String getReponseA() {
        return reponseA;
    }
    
    public void setReponseA(String reponseA) {
        this.reponseA = reponseA;
    }
    
    public StatutMessage getStatut() {
        return statut;
    }
    
    public void setStatut(StatutMessage statut) {
        this.statut = statut;
    }
    
    // Méthodes utilitaires
    
    /**
     * Marque le message comme lu
     */
    public void marquerCommeLu() {
        this.lu = true;
        this.dateLecture = LocalDateTime.now();
    }
    
    /**
     * Marque le message comme non lu
     */
    public void marquerCommeNonLu() {
        this.lu = false;
        this.dateLecture = null;
    }
    
    /**
     * Vérifie si le message a une pièce jointe
     */
    public boolean hasPieceJointe() {
        return pieceJointe != null && !pieceJointe.trim().isEmpty();
    }
    
    /**
     * Retourne un aperçu du contenu (premiers caractères)
     */
    public String getApercu(int longueur) {
        if (contenu == null) return "";
        
        if (contenu.length() <= longueur) {
            return contenu;
        }
        
        return contenu.substring(0, longueur) + "...";
    }
    
    /**
     * Retourne un aperçu par défaut du contenu (100 caractères)
     */
    public String getApercu() {
        return getApercu(100);
    }
    
    /**
     * Vérifie si c'est une réponse à un autre message
     */
    public boolean isReponse() {
        return reponseA != null && !reponseA.trim().isEmpty();
    }
    
    /**
     * Retourne la couleur associée à la priorité
     */
    public String getCouleurPriorite() {
        switch (priorite) {
            case TRES_HAUTE:
                return "#d32f2f"; // Rouge foncé
            case HAUTE:
                return "#f57c00"; // Orange
            case NORMALE:
                return "#1976d2"; // Bleu
            case BASSE:
                return "#388e3c"; // Vert
            default:
                return "#757575"; // Gris
        }
    }
    
    /**
     * Retourne l'icône associée au type de message
     */
    public String getIconeType() {
        switch (typeMessage) {
            case INTERNE:
                return "💬";
            case SYSTEME:
                return "⚙️";
            case NOTIFICATION:
                return "🔔";
            case ALERTE:
                return "⚠️";
            case DIFFUSION:
                return "📢";
            default:
                return "📧";
        }
    }
    
    /**
     * Formate la date d'envoi pour l'affichage
     */
    public String getDateEnvoiFormatee() {
        if (dateEnvoi == null) return "";
        
        LocalDateTime maintenant = LocalDateTime.now();
        LocalDateTime debut = dateEnvoi.toLocalDate().atStartOfDay();
        
        if (debut.isEqual(maintenant.toLocalDate().atStartOfDay())) {
            // Aujourd'hui
            return dateEnvoi.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
        } else if (debut.isEqual(maintenant.minusDays(1).toLocalDate().atStartOfDay())) {
            // Hier
            return "Hier " + dateEnvoi.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
        } else {
            // Autre date
            return dateEnvoi.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        }
    }
    
    /**
     * Calcule le temps écoulé depuis l'envoi
     */
    public String getTempsEcoule() {
        if (dateEnvoi == null) return "";
        
        LocalDateTime maintenant = LocalDateTime.now();
        java.time.Duration duree = java.time.Duration.between(dateEnvoi, maintenant);
        
        long secondes = duree.getSeconds();
        
        if (secondes < 60) {
            return "À l'instant";
        } else if (secondes < 3600) {
            long minutes = secondes / 60;
            return "Il y a " + minutes + " minute" + (minutes > 1 ? "s" : "");
        } else if (secondes < 86400) {
            long heures = secondes / 3600;
            return "Il y a " + heures + " heure" + (heures > 1 ? "s" : "");
        } else {
            long jours = secondes / 86400;
            return "Il y a " + jours + " jour" + (jours > 1 ? "s" : "");
        }
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Message message = (Message) o;
        return id == message.id;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "Message{" +
                "id=" + id +
                ", expediteur=" + (expediteur != null ? expediteur.getCode() : "null") +
                ", destinataire=" + (destinataire != null ? destinataire.getCode() : "null") +
                ", objet='" + objet + '\'' +
                ", dateEnvoi=" + dateEnvoi +
                ", lu=" + lu +
                ", priorite=" + priorite +
                '}';
    }
}

/**
 * Énumération des priorités des messages
 */
enum PrioriteMessage {
    TRES_HAUTE("Très haute", 4),
    HAUTE("Haute", 3),
    NORMALE("Normale", 2),
    BASSE("Basse", 1);
    
    private final String libelle;
    private final int niveau;
    
    PrioriteMessage(String libelle, int niveau) {
        this.libelle = libelle;
        this.niveau = niveau;
    }
    
    public String getLibelle() {
        return libelle;
    }
    
    public int getNiveau() {
        return niveau;
    }
    
    @Override
    public String toString() {
        return libelle;
    }
}

/**
 * Énumération des types de messages
 */
enum TypeMessage {
    INTERNE("Message interne"),
    SYSTEME("Message système"),
    NOTIFICATION("Notification"),
    ALERTE("Alerte"),
    DIFFUSION("Diffusion");
    
    private final String libelle;
    
    TypeMessage(String libelle) {
        this.libelle = libelle;
    }
    
    public String getLibelle() {
        return libelle;
    }
    
    @Override
    public String toString() {
        return libelle;
    }
}

/**
 * Énumération des statuts des messages
 */
enum StatutMessage {
    BROUILLON("Brouillon"),
    ENVOYE("Envoyé"),
    DELIVRE("Délivré"),
    LU("Lu"),
    ARCHIVE("Archivé"),
    SUPPRIME("Supprimé"),
    ECHEC("Échec d'envoi");
    
    private final String libelle;
    
    StatutMessage(String libelle) {
        this.libelle = libelle;
    }
    
    public String getLibelle() {
        return libelle;
    }
    
    @Override
    public String toString() {
        return libelle;
    }
}