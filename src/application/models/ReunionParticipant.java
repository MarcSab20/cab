package application.models;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Modèle représentant un participant à une réunion
 */
public class ReunionParticipant {
    
    public enum StatutParticipation {
        INVITE("Invité", "📨"),
        CONFIRME("Confirmé", "✅"),
        DECLINE("Décliné", "❌"),
        ABSENT("Absent", "⭕");
        
        private final String libelle;
        private final String icone;
        
        StatutParticipation(String libelle, String icone) {
            this.libelle = libelle;
            this.icone = icone;
        }
        
        public String getLibelle() {
            return libelle;
        }
        
        public String getIcone() {
            return icone;
        }
    }
    
    private int id;
    private Reunion reunion;
    private User user;
    private StatutParticipation statutParticipation;
    private LocalDateTime dateReponse;
    private String commentaire;
    
    public ReunionParticipant() {
        this.statutParticipation = StatutParticipation.INVITE;
    }
    
    public ReunionParticipant(Reunion reunion, User user) {
        this();
        this.reunion = reunion;
        this.user = user;
    }
    
    // Getters et Setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public Reunion getReunion() {
        return reunion;
    }
    
    public void setReunion(Reunion reunion) {
        this.reunion = reunion;
    }
    
    public User getUser() {
        return user;
    }
    
    public void setUser(User user) {
        this.user = user;
    }
    
    public StatutParticipation getStatutParticipation() {
        return statutParticipation;
    }
    
    public void setStatutParticipation(StatutParticipation statutParticipation) {
        this.statutParticipation = statutParticipation;
    }
    
    public LocalDateTime getDateReponse() {
        return dateReponse;
    }
    
    public void setDateReponse(LocalDateTime dateReponse) {
        this.dateReponse = dateReponse;
    }
    
    public String getCommentaire() {
        return commentaire;
    }
    
    public void setCommentaire(String commentaire) {
        this.commentaire = commentaire;
    }
    
    // Méthodes utilitaires
    
    /**
     * Confirme la participation
     */
    public void confirmer() {
        this.statutParticipation = StatutParticipation.CONFIRME;
        this.dateReponse = LocalDateTime.now();
    }
    
    /**
     * Décline la participation
     */
    public void decliner(String raison) {
        this.statutParticipation = StatutParticipation.DECLINE;
        this.dateReponse = LocalDateTime.now();
        this.commentaire = raison;
    }
    
    /**
     * Marque comme absent
     */
    public void marquerAbsent() {
        this.statutParticipation = StatutParticipation.ABSENT;
    }
    
    /**
     * Vérifie si le participant a répondu
     */
    public boolean aRepondu() {
        return dateReponse != null;
    }
    
    /**
     * Retourne le statut avec icône
     */
    public String getStatutAvecIcone() {
        return statutParticipation.getIcone() + " " + statutParticipation.getLibelle();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReunionParticipant that = (ReunionParticipant) o;
        return id == that.id;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "ReunionParticipant{" +
                "id=" + id +
                ", user=" + (user != null ? user.getNomComplet() : "null") +
                ", statut=" + statutParticipation +
                '}';
    }
}