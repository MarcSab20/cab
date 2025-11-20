package application.models;

import java.util.Set;
import java.util.HashSet;
import java.util.Objects;

/**
 * Modèle représentant un rôle utilisateur avec ses permissions
 */
public class Role {
    private int id;
    private String nom;
    private String description;
    private Set<Permission> permissions;
    private boolean actif;
    
    // Constructeurs
    public Role() {
        this.permissions = new HashSet<>();
        this.actif = true;
    }
    
    public Role(String nom, String description) {
        this();
        this.nom = nom;
        this.description = description;
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
    
    public Set<Permission> getPermissions() {
        return permissions;
    }
    
    public void setPermissions(Set<Permission> permissions) {
        this.permissions = permissions;
    }
    
    public boolean isActif() {
        return actif;
    }
    
    public void setActif(boolean actif) {
        this.actif = actif;
    }
    
    // Méthodes utilitaires pour les permissions
    public void ajouterPermission(Permission permission) {
        this.permissions.add(permission);
    }
    
    public void supprimerPermission(Permission permission) {
        this.permissions.remove(permission);
    }
    
    public boolean possedePermission(Permission permission) {
        return this.permissions.contains(permission);
    }
    
    public boolean possedePermission(String nomPermission) {
        return this.permissions.stream()
                .anyMatch(p -> p.getNom().equals(nomPermission));
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Role role = (Role) o;
        return id == role.id && Objects.equals(nom, role.nom);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id, nom);
    }
    
    @Override
    public String toString() {
        return nom;
    }
}