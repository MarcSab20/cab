package application.models;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Modèle représentant un membre d'un groupe de discussion
 */
public class MessageGroupMember {
    
    public enum RoleGroupe {
        ADMIN("Administrateur"),
        MEMBRE("Membre"),
        OBSERVATEUR("Observateur");
        
        private final String libelle;
        
        RoleGroupe(String libelle) {
            this.libelle = libelle;
        }
        
        public String getLibelle() {
            return libelle;
        }
    }
    
    private int id;
    private MessageGroup groupe;
    private User user;
    private RoleGroupe roleGroupe;
    private LocalDateTime dateAjout;
    private boolean notificationsActivees;
    
    public MessageGroupMember() {
        this.roleGroupe = RoleGroupe.MEMBRE;
        this.dateAjout = LocalDateTime.now();
        this.notificationsActivees = true;
    }
    
    public MessageGroupMember(MessageGroup groupe, User user, RoleGroupe roleGroupe) {
        this();
        this.groupe = groupe;
        this.user = user;
        this.roleGroupe = roleGroupe;
    }
    
    // Getters et Setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public MessageGroup getGroupe() {
        return groupe;
    }
    
    public void setGroupe(MessageGroup groupe) {
        this.groupe = groupe;
    }
    
    public User getUser() {
        return user;
    }
    
    public void setUser(User user) {
        this.user = user;
    }
    
    public RoleGroupe getRoleGroupe() {
        return roleGroupe;
    }
    
    public void setRoleGroupe(RoleGroupe roleGroupe) {
        this.roleGroupe = roleGroupe;
    }
    
    public LocalDateTime getDateAjout() {
        return dateAjout;
    }
    
    public void setDateAjout(LocalDateTime dateAjout) {
        this.dateAjout = dateAjout;
    }
    
    public boolean isNotificationsActivees() {
        return notificationsActivees;
    }
    
    public void setNotificationsActivees(boolean notificationsActivees) {
        this.notificationsActivees = notificationsActivees;
    }
    
    // Méthodes utilitaires
    
    /**
     * Vérifie si le membre est administrateur
     */
    public boolean isAdmin() {
        return roleGroupe == RoleGroupe.ADMIN;
    }
    
    /**
     * Vérifie si le membre peut envoyer des messages
     */
    public boolean peutEnvoyerMessages() {
        return roleGroupe == RoleGroupe.ADMIN || roleGroupe == RoleGroupe.MEMBRE;
    }
    
    /**
     * Retourne l'icône du rôle
     */
    public String getIconeRole() {
        return switch (roleGroupe) {
            case ADMIN -> "👑";
            case MEMBRE -> "👤";
            case OBSERVATEUR -> "👁️";
        };
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MessageGroupMember that = (MessageGroupMember) o;
        return id == that.id;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "MessageGroupMember{" +
                "id=" + id +
                ", user=" + (user != null ? user.getNomComplet() : "null") +
                ", roleGroupe=" + roleGroupe +
                '}';
    }
}