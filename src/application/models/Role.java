package application.models;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Modèle représentant un rôle utilisateur avec ses permissions
 */
public class Role {
    private int id;
    private String code;
    private String nom;
    private String description;
    private Set<Permission> permissions;
    private boolean actif;
    private LocalDateTime dateCreation;
    
    // Constructeurs
    public Role() {
        this.permissions = new HashSet<>();
        this.actif = true;
        this.dateCreation = LocalDateTime.now();
    }
    
    public Role(String code, String nom) {
        this();
        this.code = code;
        this.nom = nom;
    }
    
    public Role(String code, String nom, String description) {
        this(code, nom);
        this.description = description;
    }
    
    // Getters et Setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public String getCode() {
        return code;
    }
    
    public void setCode(String code) {
        this.code = code;
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
        return new HashSet<>(permissions);
    }
    
    public void setPermissions(Set<Permission> permissions) {
        this.permissions = permissions != null ? new HashSet<>(permissions) : new HashSet<>();
    }
    
    /**
     * Définit les permissions depuis une liste de noms (pour compatibilité)
     */
    public void setPermissionsFromNames(Set<String> permissionNames) {
        this.permissions = new HashSet<>();
        if (permissionNames != null) {
            for (String name : permissionNames) {
                Permission perm = Permission.getByName(name);
                if (perm != null) {
                    this.permissions.add(perm);
                }
            }
        }
    }
    
    /**
     * Retourne les noms des permissions (pour compatibilité)
     */
    public Set<String> getPermissionNames() {
        return permissions.stream()
                .map(Permission::name)
                .collect(Collectors.toSet());
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
    
    // Méthodes de gestion des permissions
    
    /**
     * Ajoute une permission au rôle (enum)
     */
    public void addPermission(Permission permission) {
        if (permission != null) {
            permissions.add(permission);
        }
    }
    
    /**
     * Ajoute une permission au rôle depuis un String
     */
    public void addPermission(String permissionName) {
        if (permissionName != null && !permissionName.isEmpty()) {
            try {
                Permission perm = Permission.valueOf(permissionName.toUpperCase());
                permissions.add(perm);
            } catch (IllegalArgumentException e) {
                // Essayer par nom
                Permission perm = Permission.getByName(permissionName);
                if (perm != null) {
                    permissions.add(perm);
                }
            }
        }
    }
    
    /**
     * Retire une permission du rôle (enum)
     */
    public void removePermission(Permission permission) {
        if (permission != null) {
            permissions.remove(permission);
        }
    }
    
    /**
     * Retire une permission du rôle depuis un String
     */
    public void removePermission(String permissionName) {
        if (permissionName != null) {
            try {
                Permission perm = Permission.valueOf(permissionName.toUpperCase());
                permissions.remove(perm);
            } catch (IllegalArgumentException e) {
                Permission perm = Permission.getByName(permissionName);
                if (perm != null) {
                    permissions.remove(perm);
                }
            }
        }
    }
    
    /**
     * Vérifie si le rôle a une permission spécifique (enum)
     */
    public boolean hasPermission(Permission permission) {
        return permission != null && permissions.contains(permission);
    }
    
    /**
     * Vérifie si le rôle a une permission spécifique (String)
     */
    public boolean hasPermission(String permissionName) {
        if (permissionName == null) return false;
        
        try {
            Permission perm = Permission.valueOf(permissionName.toUpperCase());
            return permissions.contains(perm);
        } catch (IllegalArgumentException e) {
            Permission perm = Permission.getByName(permissionName);
            return perm != null && permissions.contains(perm);
        }
    }
    
    /**
     * Vérifie si le rôle a toutes les permissions spécifiées (enum)
     */
    public boolean hasAllPermissions(Permission... requiredPermissions) {
        if (requiredPermissions == null || requiredPermissions.length == 0) {
            return true;
        }
        
        for (Permission permission : requiredPermissions) {
            if (!hasPermission(permission)) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Vérifie si le rôle a toutes les permissions spécifiées (String)
     */
    public boolean hasAllPermissions(String... requiredPermissions) {
        if (requiredPermissions == null || requiredPermissions.length == 0) {
            return true;
        }
        
        for (String permission : requiredPermissions) {
            if (!hasPermission(permission)) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Vérifie si le rôle a au moins une des permissions spécifiées (enum)
     */
    public boolean hasAnyPermission(Permission... requiredPermissions) {
        if (requiredPermissions == null || requiredPermissions.length == 0) {
            return false;
        }
        
        for (Permission permission : requiredPermissions) {
            if (hasPermission(permission)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Vérifie si le rôle a au moins une des permissions spécifiées (String)
     */
    public boolean hasAnyPermission(String... requiredPermissions) {
        if (requiredPermissions == null || requiredPermissions.length == 0) {
            return false;
        }
        
        for (String permission : requiredPermissions) {
            if (hasPermission(permission)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Retourne le nombre de permissions
     */
    public int getPermissionCount() {
        return permissions.size();
    }
    
    /**
     * Vérifie si c'est un rôle administrateur
     */
    public boolean isAdmin() {
        return hasPermission(Permission.ADMIN_SYSTEME) || code.equalsIgnoreCase("ADMIN");
    }
    
    /**
     * Vérifie si c'est un rôle CEMAA ou CSP (niveau 0)
     */
    public boolean isNiveauZero() {
        return code.equalsIgnoreCase("CEMAA") || 
               code.equalsIgnoreCase("CSP") ||
               code.equalsIgnoreCase("DIRECTEUR");
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Role role = (Role) o;
        return id == role.id && Objects.equals(code, role.code);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id, code);
    }
    
    @Override
    public String toString() {
        return "Role{" +
                "id=" + id +
                ", code='" + code + '\'' +
                ", nom='" + nom + '\'' +
                ", permissions=" + permissions.size() +
                '}';
    }
    
    // Rôles prédéfinis (factory methods)
    public static Role createAdminRole() {
        Role role = new Role("ADMIN", "Administrateur", "Accès complet au système");
        role.addPermission(Permission.ADMIN_SYSTEME);
        role.addPermission(Permission.ADMIN_UTILISATEURS);
        role.addPermission(Permission.ADMIN_ROLES);
        role.addPermission(Permission.ADMIN_CONFIGURATION);
        return role;
    }
    
    public static Role createCEMAARole() {
        Role role = new Role("CEMAA", "CEMAA", "Chef d'État-Major de l'Armée de l'Air");
        role.addPermission(Permission.DASHBOARD);
        role.addPermission(Permission.COURRIER_LECTURE);
        role.addPermission(Permission.WORKFLOW_VALIDATION);
        role.addPermission(Permission.RAPPORTS_LECTURE);
        role.addPermission(Permission.STATISTIQUES);
        return role;
    }
    
    public static Role createCSPRole() {
        Role role = new Role("CSP", "Chef de Service Personnel", "Chef de Service Personnel");
        role.addPermission(Permission.DASHBOARD);
        role.addPermission(Permission.COURRIER_LECTURE);
        role.addPermission(Permission.COURRIER_CREATION);
        role.addPermission(Permission.COURRIER_MODIFICATION);
        role.addPermission(Permission.WORKFLOW_VALIDATION);
        role.addPermission(Permission.WORKFLOW_GESTION);
        return role;
    }
    
    public static Role createChefServiceRole() {
        Role role = new Role("CHEF_SERVICE", "Chef de Service", "Chef de Service");
        role.addPermission(Permission.DASHBOARD);
        role.addPermission(Permission.COURRIER_LECTURE);
        role.addPermission(Permission.COURRIER_MODIFICATION);
        role.addPermission(Permission.WORKFLOW_VALIDATION);
        return role;
    }
    
    public static Role createAgentRole() {
        Role role = new Role("AGENT", "Agent", "Agent de traitement");
        role.addPermission(Permission.ACCUEIL);
        role.addPermission(Permission.COURRIER_LECTURE);
        role.addPermission(Permission.DOCUMENT_LECTURE);
        return role;
    }
}