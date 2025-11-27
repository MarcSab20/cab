package application.utils;

import application.models.Permission;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;

/**
 * Classe utilitaire pour la gestion des permissions
 */
public class PermissionHelper {
    
    /**
     * Convertit un Set de Permission en String JSON
     * Utilisé par AdminService.permissionsToJson()
     */
    public static String permissionsToJson(Set<Permission> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return "[]";
        }
        
        String json = permissions.stream()
                .map(p -> "\"" + p.name() + "\"")
                .collect(Collectors.joining(", "));
        
        return "[" + json + "]";
    }
    
    /**
     * Convertit un String JSON en Set de Permission
     * Utilisé par AdminService.jsonToPermissions()
     */
    public static Set<Permission> jsonToPermissions(String json) {
        if (json == null || json.trim().isEmpty() || json.equals("[]")) {
            return new HashSet<>();
        }
        
        // Enlever les crochets et quotes
        String cleaned = json.replace("[", "").replace("]", "")
                             .replace("\"", "").trim();
        
        if (cleaned.isEmpty()) {
            return new HashSet<>();
        }
        
        String[] permNames = cleaned.split(",");
        return java.util.Arrays.stream(permNames)
                .map(String::trim)
                .map(Permission::fromString)
                .filter(p -> p != null)
                .collect(Collectors.toSet());
    }
    
    /**
     * Convertit un Set de String (noms de permissions) en Set de Permission
     */
    public static Set<Permission> fromNames(Set<String> permissionNames) {
        if (permissionNames == null || permissionNames.isEmpty()) {
            return new HashSet<>();
        }
        
        return permissionNames.stream()
                .map(Permission::fromString)
                .filter(p -> p != null)
                .collect(Collectors.toSet());
    }
    
    /**
     * Convertit un Set de Permission en Set de String (noms)
     */
    public static Set<String> toNames(Set<Permission> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return new HashSet<>();
        }
        
        return permissions.stream()
                .map(Permission::name)
                .collect(Collectors.toSet());
    }
    
    /**
     * Vérifie si une permission donnée existe dans un Set
     */
    public static boolean hasPermission(Set<Permission> permissions, String permissionName) {
        if (permissions == null || permissionName == null) {
            return false;
        }
        
        Permission perm = Permission.fromString(permissionName);
        return perm != null && permissions.contains(perm);
    }
    
    /**
     * Vérifie si toutes les permissions requises sont présentes
     */
    public static boolean hasAllPermissions(Set<Permission> permissions, String... requiredPermissions) {
        if (permissions == null) return false;
        if (requiredPermissions == null || requiredPermissions.length == 0) return true;
        
        for (String required : requiredPermissions) {
            if (!hasPermission(permissions, required)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Vérifie si au moins une des permissions requises est présente
     */
    public static boolean hasAnyPermission(Set<Permission> permissions, String... requiredPermissions) {
        if (permissions == null) return false;
        if (requiredPermissions == null || requiredPermissions.length == 0) return false;
        
        for (String required : requiredPermissions) {
            if (hasPermission(permissions, required)) {
                return true;
            }
        }
        return false;
    }
}