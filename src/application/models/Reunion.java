package application.models;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Modèle représentant une réunion dans le système
 */
public class Reunion {
    private int id;
    private String titre;
    private String description;
    private LocalDateTime dateReunion;
    private int dureeMinutes;
    private String lieu;
    private User organisateur;
    private StatutReunion statut;
    private String lienVisio;
    private List<User> participants;
    private String ordreDuJour;
    private String compteRendu;
    
    // Constructeurs
    public Reunion() {
        this.participants = new ArrayList<>();
        this.statut = StatutReunion.PROGRAMMEE;
        this.dureeMinutes = 60;
    }
    
    public Reunion(String titre, LocalDateTime dateReunion, User organisateur) {
        this();
        this.titre = titre;
        this.dateReunion = dateReunion;
        this.organisateur = organisateur;
    }
    
    // Getters et Setters
    public int getId() { 
        return id; 
    }
    
    public void setId(int id) { 
        this.id = id; 
    }
    
    public String getTitre() { 
        return titre; 
    }
    
    public void setTitre(String titre) { 
        this.titre = titre; 
    }
    
    public String getDescription() { 
        return description; 
    }
    
    public void setDescription(String description) { 
        this.description = description; 
    }
    
    public LocalDateTime getDateReunion() { 
        return dateReunion; 
    }
    
    public void setDateReunion(LocalDateTime dateReunion) { 
        this.dateReunion = dateReunion; 
    }
    
    public int getDureeMinutes() { 
        return dureeMinutes; 
    }
    
    public void setDureeMinutes(int dureeMinutes) { 
        this.dureeMinutes = dureeMinutes; 
    }
    
    public String getLieu() { 
        return lieu; 
    }
    
    public void setLieu(String lieu) { 
        this.lieu = lieu; 
    }
    
    public User getOrganisateur() { 
        return organisateur; 
    }
    
    public void setOrganisateur(User organisateur) { 
        this.organisateur = organisateur; 
    }
    
    public StatutReunion getStatut() { 
        return statut; 
    }
    
    public void setStatut(StatutReunion statut) { 
        this.statut = statut; 
    }
    
    public String getLienVisio() { 
        return lienVisio; 
    }
    
    public void setLienVisio(String lienVisio) { 
        this.lienVisio = lienVisio; 
    }
    
    public List<User> getParticipants() { 
        return participants; 
    }
    
    public void setParticipants(List<User> participants) { 
        this.participants = participants != null ? participants : new ArrayList<>();
    }
    
    public String getOrdreDuJour() { 
        return ordreDuJour; 
    }
    
    public void setOrdreDuJour(String ordreDuJour) { 
        this.ordreDuJour = ordreDuJour; 
    }
    
    public String getCompteRendu() { 
        return compteRendu; 
    }
    
    public void setCompteRendu(String compteRendu) { 
        this.compteRendu = compteRendu; 
    }
    
    // Méthodes utilitaires
    
    /**
     * Ajoute un participant à la réunion
     */
    public void ajouterParticipant(User participant) {
        if (participant != null && !participants.contains(participant)) {
            participants.add(participant);
        }
    }
    
    /**
     * Retire un participant de la réunion
     */
    public void retirerParticipant(User participant) {
        participants.remove(participant);
    }
    
    /**
     * Retourne le nombre de participants
     */
    public int getNombreParticipants() {
        return participants.size();
    }
    
    /**
     * Retourne la date formatée
     */
    public String getDateReunionFormatee() {
        if (dateReunion == null) return "";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        return dateReunion.format(formatter);
    }
    
    /**
     * Retourne l'heure de début et de fin
     */
    public String getHeureDebut() {
        if (dateReunion == null) return "";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        return dateReunion.format(formatter);
    }
    
    public String getHeureFin() {
        if (dateReunion == null) return "";
        LocalDateTime fin = dateReunion.plusMinutes(dureeMinutes);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        return fin.format(formatter);
    }
    
    /**
     * Retourne la durée formatée
     */
    public String getDureeFormatee() {
        int heures = dureeMinutes / 60;
        int minutes = dureeMinutes % 60;
        
        if (heures > 0 && minutes > 0) {
            return heures + "h" + minutes;
        } else if (heures > 0) {
            return heures + "h";
        } else {
            return minutes + "min";
        }
    }
    
    /**
     * Vérifie si la réunion est en cours
     */
    public boolean isEnCours() {
        if (dateReunion == null) return false;
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime fin = dateReunion.plusMinutes(dureeMinutes);
        
        return now.isAfter(dateReunion) && now.isBefore(fin);
    }
    
    /**
     * Vérifie si la réunion est passée
     */
    public boolean isPassee() {
        if (dateReunion == null) return false;
        LocalDateTime fin = dateReunion.plusMinutes(dureeMinutes);
        return LocalDateTime.now().isAfter(fin);
    }
    
    /**
     * Vérifie si la réunion est à venir
     */
    public boolean isAVenir() {
        if (dateReunion == null) return false;
        return LocalDateTime.now().isBefore(dateReunion);
    }
    
    /**
     * Démarre la réunion
     */
    public void demarrer() {
        if (statut != StatutReunion.PROGRAMMEE) {
            throw new IllegalStateException("Seules les réunions programmées peuvent être démarrées");
        }
        this.statut = StatutReunion.EN_COURS;
    }
    
    /**
     * Termine la réunion
     */
    public void terminer() {
        if (statut != StatutReunion.EN_COURS) {
            throw new IllegalStateException("Seules les réunions en cours peuvent être terminées");
        }
        this.statut = StatutReunion.TERMINEE;
    }
    
    /**
     * Annule la réunion
     */
    public void annuler() {
        if (statut == StatutReunion.TERMINEE) {
            throw new IllegalStateException("Une réunion terminée ne peut pas être annulée");
        }
        this.statut = StatutReunion.ANNULEE;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Reunion reunion = (Reunion) o;
        return id == reunion.id;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "Reunion{" +
                "id=" + id +
                ", titre='" + titre + '\'' +
                ", date=" + getDateReunionFormatee() +
                ", statut=" + statut +
                ", participants=" + getNombreParticipants() +
                '}';
    }
}