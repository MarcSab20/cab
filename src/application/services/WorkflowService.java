package application.services;

import application.models.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Service pour gérer le workflow des courriers
 */
public class WorkflowService {
    
    private static WorkflowService instance;
    private final DatabaseService databaseService;
    
    // Cache pour la hiérarchie
    private Map<String, ServiceHierarchy> hierarchyCache;
    private LocalDateTime lastCacheUpdate;
    private static final long CACHE_DURATION_MINUTES = 30;
    
    private WorkflowService() {
        this.databaseService = DatabaseService.getInstance();
        this.hierarchyCache = new HashMap<>();
        loadHierarchyCache();
    }
    
    public static synchronized WorkflowService getInstance() {
        if (instance == null) {
            instance = new WorkflowService();
        }
        return instance;
    }
    
    // ========================================
    // GESTION DE LA HIÉRARCHIE
    // ========================================
    
    /**
     * Charge la hiérarchie complète en cache
     */
    public void loadHierarchyCache() {
        String query = "SELECT * FROM service_hierarchy WHERE actif = TRUE ORDER BY niveau, ordre_affichage";
        
        try (Connection conn = databaseService.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            hierarchyCache.clear();
            Map<String, ServiceHierarchy> tempMap = new HashMap<>();
            
            // Première passe : créer tous les services
            while (rs.next()) {
                ServiceHierarchy service = mapResultSetToService(rs);
                tempMap.put(service.getServiceCode(), service);
            }
            
            // Deuxième passe : établir les relations parent-enfant
            for (ServiceHierarchy service : tempMap.values()) {
                if (service.getParentServiceCode() != null) {
                    ServiceHierarchy parent = tempMap.get(service.getParentServiceCode());
                    if (parent != null) {
                        service.setParent(parent);
                        parent.ajouterEnfant(service);
                    }
                }
            }
            
            hierarchyCache = tempMap;
            lastCacheUpdate = LocalDateTime.now();
            
            System.out.println("Hiérarchie chargée en cache: " + hierarchyCache.size() + " services");
            
        } catch (SQLException e) {
            System.err.println("Erreur lors du chargement de la hiérarchie: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Rafraîchit le cache si nécessaire
     */
    private void refreshCacheIfNeeded() {
        if (lastCacheUpdate == null || 
            LocalDateTime.now().minusMinutes(CACHE_DURATION_MINUTES).isAfter(lastCacheUpdate)) {
            loadHierarchyCache();
        }
    }
    
    /**
     * Récupère un service par son code
     */
    public ServiceHierarchy getServiceByCode(String serviceCode) {
        refreshCacheIfNeeded();
        return hierarchyCache.get(serviceCode);
    }
    
    /**
     * Récupère tous les services
     */
    public List<ServiceHierarchy> getAllServices() {
        refreshCacheIfNeeded();
        return new ArrayList<>(hierarchyCache.values());
    }
    
    /**
     * Récupère les services racines (sans parent)
     */
    public List<ServiceHierarchy> getRootServices() {
        refreshCacheIfNeeded();
        List<ServiceHierarchy> roots = new ArrayList<>();
        
        for (ServiceHierarchy service : hierarchyCache.values()) {
            if (service.getParentServiceCode() == null || service.getNiveau() == 0) {
                roots.add(service);
            }
        }
        
        roots.sort(Comparator.comparingInt(ServiceHierarchy::getOrdreAffichage));
        return roots;
    }
    
    /**
     * Récupère les enfants directs d'un service
     */
    public List<ServiceHierarchy> getChildServices(String parentCode) {
        ServiceHierarchy parent = getServiceByCode(parentCode);
        if (parent == null) return new ArrayList<>();
        
        return new ArrayList<>(parent.getEnfants());
    }
    
    /**
     * Vérifie si un utilisateur peut transférer vers un service
     */
    public boolean canTransferTo(User user, String destinationCode) {
        if (user.getServiceCode() == null) return false;
        
        ServiceHierarchy userService = getServiceByCode(user.getServiceCode());
        ServiceHierarchy destination = getServiceByCode(destinationCode);
        
        if (userService == null || destination == null) return false;
        
        return userService.peutTransfererVers(destination);
    }
    
    /**
     * Récupère les services vers lesquels un utilisateur peut transférer
     */
    public List<ServiceHierarchy> getTransferableServices(User user) {
        if (user.getServiceCode() == null) return new ArrayList<>();
        
        ServiceHierarchy userService = getServiceByCode(user.getServiceCode());
        if (userService == null) return new ArrayList<>();
        
        List<ServiceHierarchy> transferable = new ArrayList<>();
        
        for (ServiceHierarchy service : hierarchyCache.values()) {
            if (userService.peutTransfererVers(service) && 
                !service.getServiceCode().equals(user.getServiceCode())) {
                transferable.add(service);
            }
        }
        
        transferable.sort(Comparator.comparingInt(ServiceHierarchy::getNiveau)
                                    .thenComparingInt(ServiceHierarchy::getOrdreAffichage));
        
        return transferable;
    }
    
    // ========================================
    // GESTION DU WORKFLOW DES COURRIERS
    // ========================================
    
    /**
     * Démarre le workflow pour un courrier
     */
    public boolean startWorkflow(Courrier courrier, String destinationInitiale) {
        String query = "UPDATE courriers SET workflow_actif = TRUE, service_actuel = ?, etape_actuelle = 1 WHERE id = ?";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, destinationInitiale);
            stmt.setInt(2, courrier.getId());
            
            boolean success = stmt.executeUpdate() > 0;
            
            if (success) {
                // Créer la première étape
                createWorkflowStep(courrier.getId(), 1, destinationInitiale, "Réception du courrier", null);
            }
            
            return success;
            
        } catch (SQLException e) {
            System.err.println("Erreur lors du démarrage du workflow: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Crée une étape de workflow
     */
    public boolean createWorkflowStep(int courrierId, int etapeNumero, String serviceCode, 
                                     String action, Integer delaiHeures) {
        String query = """
            INSERT INTO courrier_workflow (courrier_id, etape_numero, service_code, action, delai_traitement)
            VALUES (?, ?, ?, ?, ?)
        """;
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, courrierId);
            stmt.setInt(2, etapeNumero);
            stmt.setString(3, serviceCode);
            stmt.setString(4, action);
            
            if (delaiHeures != null) {
                stmt.setInt(5, delaiHeures);
            } else {
                stmt.setNull(5, Types.INTEGER);
            }
            
            return stmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("Erreur lors de la création de l'étape: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Transfère un courrier vers un autre service
     */
    public boolean transferCourrier(Courrier courrier, User user, String destinationCode, 
                                   String commentaire, Integer delaiHeures) {
        
        // Vérifier les permissions
        if (!canTransferTo(user, destinationCode)) {
            System.err.println("L'utilisateur n'a pas l'autorité pour transférer vers ce service");
            return false;
        }
        
        Connection conn = null;
        try {
            conn = databaseService.getConnection();
            conn.setAutoCommit(false);
            
            // Mettre à jour l'étape actuelle
            String updateCurrentQuery = """
                UPDATE courrier_workflow 
                SET statut_etape = 'transfere', user_id = ?, commentaire = ?, date_action = NOW()
                WHERE courrier_id = ? AND etape_numero = ?
            """;
            
            try (PreparedStatement stmt = conn.prepareStatement(updateCurrentQuery)) {
                stmt.setInt(1, user.getId());
                stmt.setString(2, commentaire);
                stmt.setInt(3, courrier.getId());
                stmt.setInt(4, courrier.getEtapeActuelle());
                stmt.executeUpdate();
            }
            
            // Créer la nouvelle étape
            int nouvelleEtape = courrier.getEtapeActuelle() + 1;
            
            String insertNewQuery = """
                INSERT INTO courrier_workflow 
                (courrier_id, etape_numero, service_code, action, delai_traitement, date_echeance, statut_etape)
                VALUES (?, ?, ?, 'Transfert reçu', ?, DATE_ADD(NOW(), INTERVAL ? HOUR), 'en_attente')
            """;
            
            try (PreparedStatement stmt = conn.prepareStatement(insertNewQuery)) {
                stmt.setInt(1, courrier.getId());
                stmt.setInt(2, nouvelleEtape);
                stmt.setString(3, destinationCode);
                
                if (delaiHeures != null) {
                    stmt.setInt(4, delaiHeures);
                    stmt.setInt(5, delaiHeures);
                } else {
                    stmt.setNull(4, Types.INTEGER);
                    stmt.setNull(5, Types.INTEGER);
                }
                
                stmt.executeUpdate();
            }
            
            // Mettre à jour le courrier
            String updateCourrierQuery = """
                UPDATE courriers 
                SET service_actuel = ?, etape_actuelle = ?, statut = 'en_cours'
                WHERE id = ?
            """;
            
            try (PreparedStatement stmt = conn.prepareStatement(updateCourrierQuery)) {
                stmt.setString(1, destinationCode);
                stmt.setInt(2, nouvelleEtape);
                stmt.setInt(3, courrier.getId());
                stmt.executeUpdate();
            }
            
            conn.commit();
            
            // Notifier via le réseau
            NetworkService.getInstance().notifyWorkflowUpdate(courrier.getId(), destinationCode);
            
            return true;
            
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            System.err.println("Erreur lors du transfert: " + e.getMessage());
            e.printStackTrace();
            return false;
            
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    /**
     * Marque un courrier comme traité et termine le workflow
     */
    public boolean terminerWorkflow(Courrier courrier, User user, String commentaire) {
        String query = """
            UPDATE courrier_workflow 
            SET statut_etape = 'termine', user_id = ?, commentaire = ?, date_action = NOW()
            WHERE courrier_id = ? AND etape_numero = ?
        """;
        
        String updateCourrierQuery = """
            UPDATE courriers 
            SET workflow_termine = TRUE, statut = 'archive', date_traitement = NOW()
            WHERE id = ?
        """;
        
        Connection conn = null;
        try {
            conn = databaseService.getConnection();
            conn.setAutoCommit(false);
            
            // Mettre à jour l'étape
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setInt(1, user.getId());
                stmt.setString(2, commentaire);
                stmt.setInt(3, courrier.getId());
                stmt.setInt(4, courrier.getEtapeActuelle());
                stmt.executeUpdate();
            }
            
            // Mettre à jour le courrier
            try (PreparedStatement stmt = conn.prepareStatement(updateCourrierQuery)) {
                stmt.setInt(1, courrier.getId());
                stmt.executeUpdate();
            }
            
            conn.commit();
            
            // Notifier
            NetworkService.getInstance().notifyWorkflowComplete(courrier.getId());
            
            return true;
            
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            System.err.println("Erreur lors de la terminaison du workflow: " + e.getMessage());
            return false;
            
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    /**
     * Récupère l'historique du workflow d'un courrier
     */
    public List<WorkflowStep> getWorkflowHistory(int courrierId) {
        List<WorkflowStep> steps = new ArrayList<>();
        
        String query = """
            SELECT w.*, s.service_name, u.nom, u.prenom
            FROM courrier_workflow w
            LEFT JOIN service_hierarchy s ON w.service_code = s.service_code
            LEFT JOIN users u ON w.user_id = u.id
            WHERE w.courrier_id = ?
            ORDER BY w.etape_numero
        """;
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, courrierId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    WorkflowStep step = mapResultSetToWorkflowStep(rs);
                    steps.add(step);
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération de l'historique: " + e.getMessage());
        }
        
        return steps;
    }
    
    /**
     * Récupère les courriers dans le workflow d'un service
     */
    public List<Courrier> getCourriersForService(String serviceCode) {
        List<Courrier> courriers = new ArrayList<>();
        
        String query = """
            SELECT c.*, w.etape_numero, w.statut_etape, w.date_action, w.date_echeance
            FROM courriers c
            INNER JOIN courrier_workflow w ON c.id = w.courrier_id AND c.etape_actuelle = w.etape_numero
            WHERE c.service_actuel = ? AND c.workflow_actif = TRUE AND c.workflow_termine = FALSE
            ORDER BY c.date_reception DESC
        """;
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, serviceCode);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Courrier courrier = CourrierService.getInstance().mapResultSetToCourrier(rs);
                    courriers.add(courrier);
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des courriers: " + e.getMessage());
        }
        
        return courriers;
    }
    
    /**
     * Récupère les courriers visibles pour un utilisateur
     */
    public List<Courrier> getCourriersVisiblesPourUtilisateur(User user) {
        if (user.getServiceCode() == null) {
            return new ArrayList<>();
        }
        
        ServiceHierarchy userService = getServiceByCode(user.getServiceCode());
        if (userService == null) {
            return new ArrayList<>();
        }
        
        // Services de niveau 0 voient tout
        if (userService.getNiveau() == 0) {
            return getAllCourriersEnWorkflow();
        }
        
        // Autres services voient leurs courriers + ceux des descendants
        List<String> visibleServiceCodes = new ArrayList<>();
        visibleServiceCodes.add(user.getServiceCode());
        
        for (ServiceHierarchy descendant : userService.getTousLesDescendants()) {
            visibleServiceCodes.add(descendant.getServiceCode());
        }
        
        return getCourriersForServices(visibleServiceCodes);
    }
    
    /**
     * Récupère tous les courriers en workflow
     */
    private List<Courrier> getAllCourriersEnWorkflow() {
        List<Courrier> courriers = new ArrayList<>();
        
        String query = """
            SELECT DISTINCT c.*
            FROM courriers c
            WHERE c.workflow_actif = TRUE
            ORDER BY c.date_reception DESC
        """;
        
        try (Connection conn = databaseService.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            while (rs.next()) {
                Courrier courrier = CourrierService.getInstance().mapResultSetToCourrier(rs);
                courriers.add(courrier);
            }
            
        } catch (SQLException e) {
            System.err.println("Erreur: " + e.getMessage());
        }
        
        return courriers;
    }
    
    /**
     * Récupère les courriers pour plusieurs services
     */
    private List<Courrier> getCourriersForServices(List<String> serviceCodes) {
        if (serviceCodes.isEmpty()) return new ArrayList<>();
        
        List<Courrier> courriers = new ArrayList<>();
        
        String placeholders = String.join(",", Collections.nCopies(serviceCodes.size(), "?"));
        String query = String.format("""
            SELECT DISTINCT c.*
            FROM courriers c
            INNER JOIN courrier_workflow w ON c.id = w.courrier_id
            WHERE w.service_code IN (%s) AND c.workflow_actif = TRUE
            ORDER BY c.date_reception DESC
        """, placeholders);
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            for (int i = 0; i < serviceCodes.size(); i++) {
                stmt.setString(i + 1, serviceCodes.get(i));
            }
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Courrier courrier = CourrierService.getInstance().mapResultSetToCourrier(rs);
                    courriers.add(courrier);
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Erreur: " + e.getMessage());
        }
        
        return courriers;
    }
    
    // ========================================
    // STATISTIQUES
    // ========================================
    
    /**
     * Récupère les statistiques de workflow pour un service
     */
    public Map<String, Integer> getWorkflowStats(String serviceCode) {
        Map<String, Integer> stats = new HashMap<>();
        
        String query = """
            SELECT 
                COUNT(*) as total,
                SUM(CASE WHEN w.statut_etape = 'en_attente' THEN 1 ELSE 0 END) as en_attente,
                SUM(CASE WHEN w.statut_etape = 'en_cours' THEN 1 ELSE 0 END) as en_cours,
                SUM(CASE WHEN w.date_echeance < NOW() AND w.statut_etape NOT IN ('termine', 'transfere') THEN 1 ELSE 0 END) as en_retard
            FROM courrier_workflow w
            INNER JOIN courriers c ON w.courrier_id = c.id
            WHERE w.service_code = ? AND c.workflow_termine = FALSE
        """;
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, serviceCode);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    stats.put("total", rs.getInt("total"));
                    stats.put("en_attente", rs.getInt("en_attente"));
                    stats.put("en_cours", rs.getInt("en_cours"));
                    stats.put("en_retard", rs.getInt("en_retard"));
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Erreur lors du calcul des statistiques: " + e.getMessage());
        }
        
        return stats;
    }
    
    // ========================================
    // MÉTHODES UTILITAIRES
    // ========================================
    
    private ServiceHierarchy mapResultSetToService(ResultSet rs) throws SQLException {
        ServiceHierarchy service = new ServiceHierarchy();
        service.setId(rs.getInt("id"));
        service.setServiceCode(rs.getString("service_code"));
        service.setServiceName(rs.getString("service_name"));
        service.setParentServiceCode(rs.getString("parent_service_code"));
        service.setNiveau(rs.getInt("niveau"));
        service.setOrdreAffichage(rs.getInt("ordre_affichage"));
        service.setActif(rs.getBoolean("actif"));
        
        Timestamp dateCreation = rs.getTimestamp("date_creation");
        if (dateCreation != null) {
            service.setDateCreation(dateCreation.toLocalDateTime());
        }
        
        return service;
    }
    
    private WorkflowStep mapResultSetToWorkflowStep(ResultSet rs) throws SQLException {
        WorkflowStep step = new WorkflowStep();
        step.setId(rs.getInt("id"));
        step.setCourrierId(rs.getInt("courrier_id"));
        step.setEtapeNumero(rs.getInt("etape_numero"));
        step.setServiceCode(rs.getString("service_code"));
        step.setServiceName(rs.getString("service_name"));
        
        int userId = rs.getInt("user_id");
        if (!rs.wasNull()) {
            step.setUserId(userId);
            String nom = rs.getString("nom");
            String prenom = rs.getString("prenom");
            if (nom != null && prenom != null) {
                step.setUserName(prenom + " " + nom);
            }
        }
        
        step.setAction(rs.getString("action"));
        step.setCommentaire(rs.getString("commentaire"));
        
        Timestamp dateAction = rs.getTimestamp("date_action");
        if (dateAction != null) {
            step.setDateAction(dateAction.toLocalDateTime());
        }
        
        String statutStr = rs.getString("statut_etape");
        if (statutStr != null) {
            // Conversion correcte de l'enum
            step.setStatutEtape(StatutEtapeWorkflow.fromString(statutStr));
        }
        
        int delai = rs.getInt("delai_traitement");
        if (!rs.wasNull()) {
            step.setDelaiTraitement(delai);
        }
        
        Timestamp dateEcheance = rs.getTimestamp("date_echeance");
        if (dateEcheance != null) {
            step.setDateEcheance(dateEcheance.toLocalDateTime());
        }
        
        return step;
    }
}