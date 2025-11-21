package application.models;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.Duration;
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
    private Integer reponseA; // ID du message parent si c'est une réponse
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
            this.statut = StatutMessage.LU;
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
        if (archive) {
            this.statut = StatutMessage.ARCHIVE;
        }
    }
    
    public Integer getReponseA() {
        return reponseA;
    }
    
    public void setReponseA(Integer reponseA) {
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
        this.statut = StatutMessage.LU;
    }
    
    /**
     * Marque le message comme non lu
     */
    public void marquerCommeNonLu() {
        this.lu = false;
        this.dateLecture = null;
        if (this.statut == StatutMessage.LU) {
            this.statut = StatutMessage.DELIVRE;
        }
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
        return reponseA != null;
    }
    
    /**
     * Retourne la couleur associée à la priorité
     */
    public String getCouleurPriorite() {
        return priorite.getCouleur();
    }
    
    /**
     * Retourne l'icône associée au type de message
     */
    public String getIconeType() {
        return typeMessage.getIcone();
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
            return dateEnvoi.format(DateTimeFormatter.ofPattern("HH:mm"));
        } else if (debut.isEqual(maintenant.minusDays(1).toLocalDate().atStartOfDay())) {
            // Hier
            return "Hier " + dateEnvoi.format(DateTimeFormatter.ofPattern("HH:mm"));
        } else {
            // Autre date
            return dateEnvoi.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        }
    }
    
    /**
     * Formate la date de lecture pour l'affichage
     */
    public String getDateLectureFormatee() {
        if (dateLecture == null) return "Non lu";
        return dateLecture.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }
    
    /**
     * Calcule le temps écoulé depuis l'envoi
     */
    public String getTempsEcoule() {
        if (dateEnvoi == null) return "";
        
        LocalDateTime maintenant = LocalDateTime.now();
        Duration duree = Duration.between(dateEnvoi, maintenant);
        
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
    
    /**
     * Retourne le nom complet de l'expéditeur
     */
    public String getNomExpediteur() {
        return expediteur != null ? expediteur.getNomComplet() : "Inconnu";
    }
    
    /**
     * Retourne le nom complet du destinataire
     */
    public String getNomDestinataire() {
        return destinataire != null ? destinataire.getNomComplet() : "Inconnu";
    }
    
    /**
     * Retourne l'email de l'expéditeur
     */
    public String getEmailExpediteur() {
        return expediteur != null ? expediteur.getEmail() : "";
    }
    
    /**
     * Retourne l'email du destinataire
     */
    public String getEmailDestinataire() {
        return destinataire != null ? destinataire.getEmail() : "";
    }
    
    /**
     * Archive le message
     */
    public void archiver() {
        this.archive = true;
        this.statut = StatutMessage.ARCHIVE;
    }
    
    /**
     * Supprime le message
     */
    public void supprimer() {
        this.statut = StatutMessage.SUPPRIME;
    }
    
    /**
     * Marque comme important
     */
    public void marquerImportant() {
        this.important = true;
    }
    
    /**
     * Retire la marque important
     */
    public void retirerImportant() {
        this.important = false;
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
                ", expediteur=" + getNomExpediteur() +
                ", destinataire=" + getNomDestinataire() +
                ", objet='" + objet + '\'' +
                ", dateEnvoi=" + getDateEnvoiFormatee() +
                ", lu=" + lu +
                ", priorite=" + priorite +
                ", statut=" + statut +
                '}';
    }
}