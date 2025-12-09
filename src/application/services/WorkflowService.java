package application.services;

import application.models.*;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Service de gestion du workflow des courriers
 */
public class WorkflowService {
    private static WorkflowService instance;
    private Map<String, ServiceHierarchy> hierarchyCache;
    private List<ServiceHierarchy> rootServices;
    
    public WorkflowService() {
        hierarchyCache = new ConcurrentHashMap<>();
        rootServices = new ArrayList<>();
        loadHierarchyCache();
    }
    
    public static synchronized WorkflowService getInstance() {
        if (instance == null) {
            instance = new WorkflowService();
        }
        return instance;
    }
    
    // ==================== GESTION DE LA HIÉRARCHIE ====================
    
    /**
     * Charge la hiérarchie complète des services en mémoire
     */
    public void loadHierarchyCache() {
        hierarchyCache.clear();
        rootServices.clear();
        
        String sql = "SELECT service_code, service_name, parent_service_code, niveau, ordre_affichage, actif, date_creation " +
                     "FROM service_hierarchy WHERE actif = 1 ORDER BY niveau, ordre_affichage";
        
        try (Connection conn = DatabaseService.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            // Première passe : charger tous les services
            while (rs.next()) {
                ServiceHierarchy service = DatabaseService.mapResultSetToServiceHierarchy(rs);
                hierarchyCache.put(service.getServiceCode(), service);
            }
            
            // Deuxième passe : construire les relations parent-enfant
            for (ServiceHierarchy service : hierarchyCache.values()) {
                String parentCode = service.getParentServiceCode();
                
                if (parentCode != null && !parentCode.isEmpty()) {
                    ServiceHierarchy parent = hierarchyCache.get(parentCode);
                    if (parent != null) {
                        service.setParent(parent);
                        parent.getEnfants().add(service);
                    }
                } else {
                    // Service racine (niveau 0)
                    rootServices.add(service);
                }
            }
            
            System.out.println("Hiérarchie chargée : " + hierarchyCache.size() + " services");
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Récupère un service par son code
     */
    public ServiceHierarchy getServiceByCode(String serviceCode) {
        if (serviceCode == null || serviceCode.isEmpty()) {
            return null;
        }
        return hierarchyCache.get(serviceCode);
    }
    
    /**
     * Récupère tous les services actifs
     */
    public List<ServiceHierarchy> getAllServices() {
        return new ArrayList<>(hierarchyCache.values());
    }
    
    /**
     * Récupère les services racines (niveau 0)
     */
    public List<ServiceHierarchy> getRootServices() {
        return new ArrayList<>(rootServices);
    }
    
    /**
     * Récupère les services vers lesquels un utilisateur peut transférer un courrier
     */
    public List<ServiceHierarchy> getTransferableServices(User user) {
        if (user.getServiceCode() == null || user.getServiceCode().isEmpty()) {
            // Utilisateur sans service (admin/CEMAA) : peut transférer partout
            return getAllServices();
        }
        
        ServiceHierarchy currentService = getServiceByCode(user.getServiceCode());
        if (currentService == null) {
            return new ArrayList<>();
        }
        
        List<ServiceHierarchy> transferables = new ArrayList<>();
        
        // Parcourir tous les services et vérifier les règles de transfert
        for (ServiceHierarchy target : hierarchyCache.values()) {
            if (currentService.peutTransfererVers(target)) {
                transferables.add(target);
            }
        }
        
        // Trier par niveau puis par nom
        transferables.sort((s1, s2) -> {
            int niveauCompare = Integer.compare(s1.getNiveau(), s2.getNiveau());
            if (niveauCompare != 0) return niveauCompare;
            return s1.getServiceName().compareTo(s2.getServiceName());
        });
        
        return transferables;
    }
    
    // ==================== GESTION DU WORKFLOW ====================
    
    /**
     * Récupère l'historique complet du workflow d'un courrier
     */
    public List<WorkflowStep> getWorkflowHistory(int courrierId) {
        List<WorkflowStep> steps = new ArrayList<>();
        
        String sql = "SELECT w.*, s.service_name, " +
                     "CONCAT(u.prenom, ' ', u.nom) as user_name " +
                     "FROM courrier_workflow w " +
                     "LEFT JOIN service_hierarchy s ON w.service_code = s.service_code " +
                     "LEFT JOIN users u ON w.user_id = u.id " +
                     "WHERE w.courrier_id = ? " +
                     "ORDER BY w.etape_numero, w.date_action";
        
        try (Connection conn = DatabaseService.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, courrierId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    WorkflowStep step = DatabaseService.mapResultSetToWorkflowStep(rs);
                    steps.add(step);
                }
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return steps;
    }
    
    /**
     * Récupère l'étape courante d'un courrier
     */
    public WorkflowStep getCurrentStep(int courrierId) {
        String sql = "SELECT w.*, s.service_name, " +
                     "CONCAT(u.prenom, ' ', u.nom) as user_name " +
                     "FROM courrier_workflow w " +
                     "LEFT JOIN service_hierarchy s ON w.service_code = s.service_code " +
                     "LEFT JOIN users u ON w.user_id = u.id " +
                     "WHERE w.courrier_id = ? AND w.statut_etape IN ('en_attente', 'en_cours') " +
                     "ORDER BY w.etape_numero DESC LIMIT 1";
        
        try (Connection conn = DatabaseService.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, courrierId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return DatabaseService.mapResultSetToWorkflowStep(rs);
                }
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return null;
    }
    
    /**
     * Récupère les courriers visibles pour un utilisateur donné
     */
    public List<Courrier> getCourriersVisiblesPourUtilisateur(User user) {
        List<Courrier> courriers = new ArrayList<>();
        
        // Si l'utilisateur n'a pas de service (CEMAA/CSP niveau 0), il voit tous les courriers
        if (user.getServiceCode() == null || user.getServiceCode().isEmpty() || user.getNiveauAutorite() == 0) {
            return CourrierService.getInstance().getAllCourriersEnWorkflow();
        }
        
        // Sinon, récupérer uniquement les courriers où l'utilisateur a participé
        String sql = "SELECT DISTINCT c.* " +
                     "FROM courriers c " +
                     "INNER JOIN courrier_workflow w ON c.id = w.courrier_id " +
                     "WHERE c.workflow_actif = 1 " +
                     "AND (w.user_id = ? OR w.service_code = ?) " +
                     "ORDER BY c.date_reception DESC";
        
        try (Connection conn = DatabaseService.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, user.getId());
            stmt.setString(2, user.getServiceCode());
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Courrier courrier = DatabaseService.mapResultSetToCourrier(rs);
                    courriers.add(courrier);
                }
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return courriers;
    }
    
    /**
     * Transfère un courrier vers un autre service
     */
    public boolean transferCourrier(Courrier courrier, User user, String targetServiceCode, 
                                    String commentaire, LocalDateTime echeance) {
        
        // Vérifier que l'utilisateur peut transférer vers ce service
        ServiceHierarchy currentService = getServiceByCode(user.getServiceCode());
        ServiceHierarchy targetService = getServiceByCode(targetServiceCode);
        
        if (currentService == null || targetService == null) {
            System.err.println("Service source ou destination introuvable");
            return false;
        }
        
        if (!currentService.peutTransfererVers(targetService)) {
            System.err.println("Transfert non autorisé selon les règles hiérarchiques");
            return false;
        }
        
        Connection conn = null;
        try {
            conn = DatabaseService.getInstance().getConnection();
            conn.setAutoCommit(false);
            
            // 1. Terminer l'étape courante
            WorkflowStep currentStep = getCurrentStep(courrier.getId());
            if (currentStep != null) {
                String updateCurrentSql = "UPDATE courrier_workflow SET " +
                                         "statut_etape = 'transfere', " +
                                         "user_id = ?, " +
                                         "commentaire = ?, " +
                                         "date_action = NOW() " +
                                         "WHERE id = ?";
                
                try (PreparedStatement stmt = conn.prepareStatement(updateCurrentSql)) {
                    stmt.setInt(1, user.getId());
                    stmt.setString(2, commentaire != null ? commentaire : "Transfert vers " + targetService.getServiceName());
                    stmt.setInt(3, currentStep.getId());
                    stmt.executeUpdate();
                }
            }
            
            // 2. Créer une nouvelle étape
            int nextEtapeNumero = getNextEtapeNumero(courrier.getId(), conn);
            
            String insertSql = "INSERT INTO courrier_workflow " +
                              "(courrier_id, etape_numero, service_code, action, commentaire, " +
                              "date_action, statut_etape, date_echeance) " +
                              "VALUES (?, ?, ?, 'Réception', ?, NOW(), 'en_attente', ?)";
            
            try (PreparedStatement stmt = conn.prepareStatement(insertSql)) {
                stmt.setInt(1, courrier.getId());
                stmt.setInt(2, nextEtapeNumero);
                stmt.setString(3, targetServiceCode);
                stmt.setString(4, "Transféré depuis " + currentService.getServiceName());
                
                if (echeance != null) {
                    stmt.setTimestamp(5, Timestamp.valueOf(echeance));
                } else {
                    stmt.setNull(5, Types.TIMESTAMP);
                }
                
                stmt.executeUpdate();
            }
            
            // 3. Mettre à jour le courrier
            String updateCourrierSql = "UPDATE courriers SET " +
                                       "service_actuel = ?, " +
                                       "etape_actuelle = ? " +
                                       "WHERE id = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(updateCourrierSql)) {
                stmt.setString(1, targetServiceCode);
                stmt.setInt(2, nextEtapeNumero);
                stmt.setInt(3, courrier.getId());
                stmt.executeUpdate();
            }
            
            conn.commit();
            
            // 4. Notifier via le réseau
            NetworkService.getInstance().notifyWorkflowUpdate(courrier.getId(), targetServiceCode);
            
            return true;
            
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
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
     * Termine une étape de workflow
     */
    public boolean terminerEtape(int workflowStepId, User user, String commentaire) {
        String sql = "UPDATE courrier_workflow SET " +
                     "statut_etape = 'termine', " +
                     "user_id = ?, " +
                     "commentaire = ?, " +
                     "date_action = NOW() " +
                     "WHERE id = ?";
        
        try (Connection conn = DatabaseService.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, user.getId());
            stmt.setString(2, commentaire);
            stmt.setInt(3, workflowStepId);
            
            int updated = stmt.executeUpdate();
            
            if (updated > 0) {
                // Vérifier si c'est la dernière étape et marquer le workflow comme terminé
                checkAndCompleteWorkflow(workflowStepId, conn);
                return true;
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return false;
    }
    
    /**
     * Vérifie si toutes les étapes sont terminées et marque le workflow comme complet
     */
    private void checkAndCompleteWorkflow(int workflowStepId, Connection conn) throws SQLException {
        // Récupérer le courrier_id
        String getCourrierSql = "SELECT courrier_id FROM courrier_workflow WHERE id = ?";
        int courrierId;
        
        try (PreparedStatement stmt = conn.prepareStatement(getCourrierSql)) {
            stmt.setInt(1, workflowStepId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) return;
                courrierId = rs.getInt("courrier_id");
            }
        }
        
        // Vérifier si toutes les étapes sont terminées ou transférées
        String checkSql = "SELECT COUNT(*) as pending " +
                         "FROM courrier_workflow " +
                         "WHERE courrier_id = ? " +
                         "AND statut_etape IN ('en_attente', 'en_cours')";
        
        try (PreparedStatement stmt = conn.prepareStatement(checkSql)) {
            stmt.setInt(1, courrierId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next() && rs.getInt("pending") == 0) {
                    // Toutes les étapes sont terminées, marquer le workflow comme complet
                    String updateSql = "UPDATE courriers SET " +
                                      "workflow_termine = 1, " +
                                      "workflow_actif = 0, " +
                                      "statut = 'traite' " +
                                      "WHERE id = ?";
                    
                    try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                        updateStmt.setInt(1, courrierId);
                        updateStmt.executeUpdate();
                    }
                    
                    // Notifier via le réseau
                    NetworkService.getInstance().notifyWorkflowComplete(courrierId);
                }
            }
        }
    }
    
    /**
     * Rejette une étape de workflow
     */
    public boolean rejeterEtape(int workflowStepId, User user, String motif, String serviceRetour) {
        Connection conn = null;
        try {
            conn = DatabaseService.getInstance().getConnection();
            conn.setAutoCommit(false);
            
            // 1. Marquer l'étape comme rejetée
            String updateSql = "UPDATE courrier_workflow SET " +
                              "statut_etape = 'rejete', " +
                              "user_id = ?, " +
                              "commentaire = ?, " +
                              "date_action = NOW() " +
                              "WHERE id = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(updateSql)) {
                stmt.setInt(1, user.getId());
                stmt.setString(2, "REJET: " + motif);
                stmt.setInt(3, workflowStepId);
                stmt.executeUpdate();
            }
            
            // 2. Récupérer le courrier_id et créer une nouvelle étape de retour
            String getCourrierSql = "SELECT courrier_id FROM courrier_workflow WHERE id = ?";
            int courrierId;
            
            try (PreparedStatement stmt = conn.prepareStatement(getCourrierSql)) {
                stmt.setInt(1, workflowStepId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next()) {
                        conn.rollback();
                        return false;
                    }
                    courrierId = rs.getInt("courrier_id");
                }
            }
            
            // 3. Créer une étape de retour
            int nextEtapeNumero = getNextEtapeNumero(courrierId, conn);
            
            String insertSql = "INSERT INTO courrier_workflow " +
                              "(courrier_id, etape_numero, service_code, action, commentaire, " +
                              "date_action, statut_etape) " +
                              "VALUES (?, ?, ?, 'Retour après rejet', ?, NOW(), 'en_attente')";
            
            try (PreparedStatement stmt = conn.prepareStatement(insertSql)) {
                stmt.setInt(1, courrierId);
                stmt.setInt(2, nextEtapeNumero);
                stmt.setString(3, serviceRetour);
                stmt.setString(4, "Rejeté - " + motif);
                stmt.executeUpdate();
            }
            
            // 4. Mettre à jour le courrier
            String updateCourrierSql = "UPDATE courriers SET " +
                                       "service_actuel = ?, " +
                                       "etape_actuelle = ? " +
                                       "WHERE id = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(updateCourrierSql)) {
                stmt.setString(1, serviceRetour);
                stmt.setInt(2, nextEtapeNumero);
                stmt.setInt(3, courrierId);
                stmt.executeUpdate();
            }
            
            conn.commit();
            
            // Notifier via le réseau
            NetworkService.getInstance().notifyWorkflowUpdate(courrierId, serviceRetour);
            
            return true;
            
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
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
     * Récupère le prochain numéro d'étape pour un courrier
     */
    private int getNextEtapeNumero(int courrierId, Connection conn) throws SQLException {
        String sql = "SELECT MAX(etape_numero) as max_etape FROM courrier_workflow WHERE courrier_id = ?";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, courrierId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("max_etape") + 1;
                }
            }
        }
        
        return 1;
    }
    
    /**
     * Démarre le workflow pour un courrier
     */
    public boolean startWorkflow(Courrier courrier, String serviceInitial) {
        Connection conn = null;
        try {
            conn = DatabaseService.getInstance().getConnection();
            conn.setAutoCommit(false);
            
            // 1. Créer la première étape du workflow
            String insertSql = "INSERT INTO courrier_workflow " +
                              "(courrier_id, etape_numero, service_code, action, commentaire, " +
                              "date_action, statut_etape) " +
                              "VALUES (?, 1, ?, 'Initialisation', 'Workflow démarré', NOW(), 'en_attente')";
            
            try (PreparedStatement stmt = conn.prepareStatement(insertSql)) {
                stmt.setInt(1, courrier.getId());
                stmt.setString(2, serviceInitial);
                stmt.executeUpdate();
            }
            
            // 2. Mettre à jour le courrier
            String updateSql = "UPDATE courriers SET " +
                              "workflow_actif = 1, " +
                              "service_actuel = ?, " +
                              "etape_actuelle = 1, " +
                              "statut = 'en_cours' " +
                              "WHERE id = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(updateSql)) {
                stmt.setString(1, serviceInitial);
                stmt.setInt(2, courrier.getId());
                stmt.executeUpdate();
            }
            
            // 3. Mettre à jour l'objet courrier
            courrier.activerWorkflow(serviceInitial);
            
            conn.commit();
            
            // 4. Notifier via le réseau
            NetworkService.getInstance().notifyWorkflowUpdate(courrier.getId(), serviceInitial);
            
            return true;
            
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
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
     * Termine le workflow d'un courrier
     */
    public boolean terminerWorkflow(Courrier courrier, User user, String commentaire) {
        WorkflowStep currentStep = getCurrentStep(courrier.getId());
        if (currentStep == null) {
            return false;
        }
        
        return terminerEtape(currentStep.getId(), user, commentaire);
    }
    
    /**
     * Transfère un courrier vers un autre service (surcharge avec délai en heures)
     */
    public boolean transferCourrier(Courrier courrier, User user, String targetServiceCode, 
                                    String commentaire, Integer delaiHeures) {
        LocalDateTime echeance = null;
        if (delaiHeures != null && delaiHeures > 0) {
            echeance = LocalDateTime.now().plusHours(delaiHeures);
        }
        return transferCourrier(courrier, user, targetServiceCode, commentaire, echeance);
    }
}