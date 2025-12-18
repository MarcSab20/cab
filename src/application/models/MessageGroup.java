package application.models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Modèle représentant un groupe de discussion
 */
public class MessageGroup {
    
    public enum TypeGroupe {
        PUBLIC("Public"),
        PRIVE("Privé"),
        DIFFUSION("Diffusion");
        
        private final String libelle;
        
        TypeGroupe(String libelle) {
            this.libelle = libelle;
        }
        
        public String getLibelle() {
            return libelle;
        }
    }
    
    private int id;
    private String nom;
    private String description;
    private User createur;
    private TypeGroupe typeGroupe;
    private LocalDateTime dateCreation;
    private boolean actif;
    private List<MessageGroupMember> membres;
    
    public MessageGroup() {
        this.typeGroupe = TypeGroupe.PRIVE;
        this.dateCreation = LocalDateTime.now();
        this.actif = true;
        this.membres = new ArrayList<>();
    }
    
    public MessageGroup(String nom, User createur) {
        this();
        this.nom = nom;
        this.createur = createur;
    }
    
    // Getters et Setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public String getNom() {
        return nom;
    }
    
    public void setNom(String nom) {
        this.nom = nom;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public User getCreateur() {
        return createur;
    }
    
    public void setCreateur(User createur) {
        this.createur = createur;
    }
    
    public TypeGroupe getTypeGroupe() {
        return typeGroupe;
    }
    
    public void setTypeGroupe(TypeGroupe typeGroupe) {
        this.typeGroupe = typeGroupe;
    }
    
    public LocalDateTime getDateCreation() {
        return dateCreation;
    }
    
    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }
    
    public boolean isActif() {
        return actif;
    }
    
    public void setActif(boolean actif) {
        this.actif = actif;
    }
    
    public List<MessageGroupMember> getMembres() {
        return membres;
    }
    
    public void setMembres(List<MessageGroupMember> membres) {
        this.membres = membres != null ? membres : new ArrayList<>();
    }
    
    // Méthodes utilitaires
    
    /**
     * Ajoute un membre au groupe
     */
    public void ajouterMembre(MessageGroupMember membre) {
        if (membre != null && !membres.contains(membre)) {
            membres.add(membre);
        }
    }
    
    /**
     * Retire un membre du groupe
     */
    public void retirerMembre(MessageGroupMember membre) {
        membres.remove(membre);
    }
    
    /**
     * Retourne le nombre de membres
     */
    public int getNombreMembres() {
        return membres.size();
    }
    
    /**
     * Vérifie si un utilisateur est membre du groupe
     */
    public boolean estMembre(User user) {
        return membres.stream()
            .anyMatch(m -> m.getUser().equals(user));
    }
    
    /**
     * Vérifie si un utilisateur est administrateur du groupe
     */
    public boolean estAdmin(User user) {
        return membres.stream()
            .anyMatch(m -> m.getUser().equals(user) && 
                         m.getRoleGroupe() == MessageGroupMember.RoleGroupe.ADMIN);
    }
    
    /**
     * Retourne l'icône du type de groupe
     */
    public String getIconeType() {
        return switch (typeGroupe) {
            case PUBLIC -> "🌐";
            case PRIVE -> "🔒";
            case DIFFUSION -> "📢";
        };
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MessageGroup that = (MessageGroup) o;
        return id == that.id;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "MessageGroup{" +
                "id=" + id +
                ", nom='" + nom + '\'' +
                ", typeGroupe=" + typeGroupe +
                ", membres=" + getNombreMembres() +
                '}';
    }
}