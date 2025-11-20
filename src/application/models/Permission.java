package application.models;

/**
 * Énumération des permissions disponibles dans le système
 */
public enum Permission {
    // Permissions générales
    ACCUEIL("Accueil", "Accès à la page d'accueil"),
    DASHBOARD("Dashboard", "Accès au tableau de bord"),
    
    // Permissions courrier
    COURRIER_LECTURE("Courrier - Lecture", "Consulter les courriers"),
    COURRIER_CREATION("Courrier - Création", "Créer de nouveaux courriers"),
    COURRIER_MODIFICATION("Courrier - Modification", "Modifier les courriers"),
    COURRIER_SUPPRESSION("Courrier - Suppression", "Supprimer les courriers"),
    COURRIER_ARCHIVAGE("Courrier - Archivage", "Archiver les courriers"),
    COURRIER_VALIDATION("Courrier - Validation", "Valider les courriers"),
    COURRIER_EXPEDITION("Courrier - Expédition", "Expédier les courriers"),
    
    // Permissions documents
    DOCUMENT_LECTURE("Document - Lecture", "Consulter les documents"),
    DOCUMENT_CREATION("Document - Création", "Créer des documents"),
    DOCUMENT_MODIFICATION("Document - Modification", "Modifier les documents"),
    DOCUMENT_SUPPRESSION("Document - Suppression", "Supprimer les documents"),
    DOCUMENT_TELECHARGEMENT("Document - Téléchargement", "Télécharger les documents"),
    DOCUMENT_PARTAGE("Document - Partage", "Partager les documents"),
    
    // Permissions réunions
    REUNIONS_LECTURE("Réunions - Lecture", "Consulter les réunions"),
    REUNIONS_CREATION("Réunions - Création", "Créer des réunions"),
    REUNIONS_MODIFICATION("Réunions - Modification", "Modifier les réunions"),
    REUNIONS_SUPPRESSION("Réunions - Suppression", "Supprimer les réunions"),
    REUNIONS_ANIMATION("Réunions - Animation", "Animer les réunions"),
    REUNIONS_COMPTES_RENDUS("Réunions - Comptes-rendus", "Gérer les comptes-rendus"),
    
    // Permissions messages
    MESSAGES_LECTURE("Messages - Lecture", "Lire les messages"),
    MESSAGES_ENVOI("Messages - Envoi", "Envoyer des messages"),
    MESSAGES_SUPPRESSION("Messages - Suppression", "Supprimer des messages"),
    MESSAGES_DIFFUSION("Messages - Diffusion", "Envoyer des messages en diffusion"),
    MESSAGES_PRIORITAIRES("Messages - Prioritaires", "Envoyer des messages prioritaires"),
    
    // Permissions administration
    ADMIN_UTILISATEURS("Admin - Utilisateurs", "Gérer les utilisateurs"),
    ADMIN_ROLES("Admin - Rôles", "Gérer les rôles et permissions"),
    ADMIN_SYSTEME("Admin - Système", "Administration système"),
    ADMIN_LOGS("Admin - Logs", "Consulter les logs système"),
    ADMIN_SAUVEGARDE("Admin - Sauvegarde", "Effectuer des sauvegardes"),
    ADMIN_CONFIGURATION("Admin - Configuration", "Modifier la configuration système"),
    
    // Permissions recherche et archivage
    RECHERCHE("Recherche", "Utiliser la fonction recherche"),
    RECHERCHE_AVANCEE("Recherche - Avancée", "Utiliser la recherche avancée"),
    ARCHIVAGE_LECTURE("Archivage - Lecture", "Consulter les archives"),
    ARCHIVAGE_GESTION("Archivage - Gestion", "Gérer les archives"),
    ARCHIVAGE_EXPORT("Archivage - Export", "Exporter des archives"),
    ARCHIVAGE_IMPORT("Archivage - Import", "Importer des archives"),
    
    // Permissions paramètres
    PARAMETRES_PERSONNELS("Paramètres - Personnels", "Modifier ses paramètres personnels"),
    PARAMETRES_SYSTEME("Paramètres - Système", "Modifier les paramètres système"),
    PARAMETRES_RESEAU("Paramètres - Réseau", "Configurer les paramètres réseau"),
    
    // Permissions rapports et statistiques
    RAPPORTS_LECTURE("Rapports - Lecture", "Consulter les rapports"),
    RAPPORTS_CREATION("Rapports - Création", "Créer des rapports"),
    RAPPORTS_EXPORT("Rapports - Export", "Exporter des rapports"),
    STATISTIQUES("Statistiques", "Consulter les statistiques"),
    
    // Permissions workflow
    WORKFLOW_VALIDATION("Workflow - Validation", "Valider dans les workflows"),
    WORKFLOW_CREATION("Workflow - Création", "Créer des workflows"),
    WORKFLOW_GESTION("Workflow - Gestion", "Gérer les workflows");
    
    private final String nom;
    private final String description;
    
    Permission(String nom, String description) {
        this.nom = nom;
        this.description = description;
    }
    
    public String getNom() {
        return nom;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * Retourne la permission par son nom
     */
    public static Permission getByName(String nom) {
        for (Permission permission : values()) {
            if (permission.getNom().equals(nom)) {
                return permission;
            }
        }
        return null;
    }
    
    /**
     * Retourne toutes les permissions sous forme de chaîne
     */
    public static String[] getAllPermissionNames() {
        Permission[] permissions = values();
        String[] names = new String[permissions.length];
        for (int i = 0; i < permissions.length; i++) {
            names[i] = permissions[i].getNom();
        }
        return names;
    }
    
    /**
     * Vérifie si une permission existe
     */
    public static boolean exists(String nom) {
        return getByName(nom) != null;
    }
    
    @Override
    public String toString() {
        return nom;
    }
}