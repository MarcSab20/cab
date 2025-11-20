package application.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Utilitaires pour la manipulation JSON avec Gson
 */
public class JsonUtils {
    
    private static final Gson gson;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    
    static {
        gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .setPrettyPrinting()
            .serializeNulls()
            .create();
    }
    
    /**
     * Convertit un objet en JSON
     */
    public static String toJson(Object object) {
        if (object == null) {
            return "null";
        }
        
        try {
            return gson.toJson(object);
        } catch (Exception e) {
            System.err.println("Erreur lors de la sérialisation JSON: " + e.getMessage());
            return "{}";
        }
    }
    
    /**
     * Convertit une chaîne JSON en objet
     */
    public static <T> T fromJson(String json, Class<T> classType) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        
        try {
            return gson.fromJson(json, classType);
        } catch (JsonSyntaxException e) {
            System.err.println("Erreur lors de la désérialisation JSON: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Convertit une chaîne JSON en objet avec type générique
     */
    public static <T> T fromJson(String json, Type type) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        
        try {
            return gson.fromJson(json, type);
        } catch (JsonSyntaxException e) {
            System.err.println("Erreur lors de la désérialisation JSON: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Convertit une liste d'objets en JSON
     */
    public static <T> String listToJson(List<T> list) {
        if (list == null) {
            return "[]";
        }
        
        try {
            return gson.toJson(list);
        } catch (Exception e) {
            System.err.println("Erreur lors de la sérialisation de la liste JSON: " + e.getMessage());
            return "[]";
        }
    }
    
    /**
     * Convertit une Map en JSON
     */
    public static String mapToJson(Map<String, Object> map) {
        if (map == null) {
            return "{}";
        }
        
        try {
            return gson.toJson(map);
        } catch (Exception e) {
            System.err.println("Erreur lors de la sérialisation de la map JSON: " + e.getMessage());
            return "{}";
        }
    }
    
    /**
     * Convertit un JSON en Map
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> jsonToMap(String json) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        
        try {
            return gson.fromJson(json, Map.class);
        } catch (JsonSyntaxException e) {
            System.err.println("Erreur lors de la désérialisation de la map JSON: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Vérifie si une chaîne est un JSON valide
     */
    public static boolean isValidJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return false;
        }
        
        try {
            gson.fromJson(json, Object.class);
            return true;
        } catch (JsonSyntaxException e) {
            return false;
        }
    }
    
    /**
     * Formate un JSON pour un affichage plus lisible
     */
    public static String prettify(String json) {
        if (!isValidJson(json)) {
            return json;
        }
        
        try {
            Object obj = gson.fromJson(json, Object.class);
            return gson.toJson(obj);
        } catch (Exception e) {
            return json;
        }
    }
    
    /**
     * Minifie un JSON (supprime les espaces et retours à la ligne)
     */
    public static String minify(String json) {
        if (!isValidJson(json)) {
            return json;
        }
        
        try {
            Object obj = gson.fromJson(json, Object.class);
            return new Gson().toJson(obj);
        } catch (Exception e) {
            return json;
        }
    }
    
    /**
     * Clone un objet en utilisant la sérialisation JSON
     */
    public static <T> T clone(T object, Class<T> classType) {
        if (object == null) {
            return null;
        }
        
        try {
            String json = toJson(object);
            return fromJson(json, classType);
        } catch (Exception e) {
            System.err.println("Erreur lors du clonage JSON: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Fusionne deux objets JSON
     */
    public static String merge(String json1, String json2) {
        if (!isValidJson(json1) || !isValidJson(json2)) {
            return "{}";
        }
        
        try {
            Map<String, Object> map1 = jsonToMap(json1);
            Map<String, Object> map2 = jsonToMap(json2);
            
            if (map1 != null && map2 != null) {
                map1.putAll(map2);
                return mapToJson(map1);
            }
            
            return "{}";
        } catch (Exception e) {
            System.err.println("Erreur lors de la fusion JSON: " + e.getMessage());
            return "{}";
        }
    }
    
    /**
     * Extrait une valeur d'un JSON par clé
     */
    public static Object extractValue(String json, String key) {
        Map<String, Object> map = jsonToMap(json);
        return map != null ? map.get(key) : null;
    }
    
    /**
     * Vérifie si un JSON contient une clé
     */
    public static boolean hasKey(String json, String key) {
        Map<String, Object> map = jsonToMap(json);
        return map != null && map.containsKey(key);
    }
    
    /**
     * Retourne la taille d'un objet JSON (nombre de clés)
     */
    public static int getSize(String json) {
        Map<String, Object> map = jsonToMap(json);
        return map != null ? map.size() : 0;
    }
    
    /**
     * Convertit un objet en JSON compact (une seule ligne)
     */
    public static String toCompactJson(Object object) {
        if (object == null) {
            return "null";
        }
        
        try {
            return new Gson().toJson(object);
        } catch (Exception e) {
            System.err.println("Erreur lors de la sérialisation JSON compact: " + e.getMessage());
            return "{}";
        }
    }
    
    /**
     * Adapter personnalisé pour LocalDateTime
     */
    private static class LocalDateTimeAdapter extends TypeAdapter<LocalDateTime> {
        
        @Override
        public void write(JsonWriter out, LocalDateTime value) throws IOException {
            if (value == null) {
                out.nullValue();
            } else {
                out.value(value.format(DATE_FORMATTER));
            }
        }
        
        @Override
        public LocalDateTime read(JsonReader in) throws IOException {
            if (in.peek() == com.google.gson.stream.JsonToken.NULL) {
                in.nextNull();
                return null;
            }
            
            String dateString = in.nextString();
            try {
                return LocalDateTime.parse(dateString, DATE_FORMATTER);
            } catch (Exception e) {
                System.err.println("Erreur lors du parsing de la date: " + dateString);
                return null;
            }
        }
    }
    
    /**
     * Crée un builder JSON pour construire des objets JSON de manière fluide
     */
    public static JsonBuilder builder() {
        return new JsonBuilder();
    }
    
    /**
     * Builder pour construire des objets JSON
     */
    public static class JsonBuilder {
        private final Map<String, Object> map = new java.util.HashMap<>();
        
        public JsonBuilder add(String key, Object value) {
            map.put(key, value);
            return this;
        }
        
        public JsonBuilder addIfNotNull(String key, Object value) {
            if (value != null) {
                map.put(key, value);
            }
            return this;
        }
        
        public JsonBuilder addIfNotEmpty(String key, String value) {
            if (value != null && !value.trim().isEmpty()) {
                map.put(key, value);
            }
            return this;
        }
        
        public String build() {
            return mapToJson(map);
        }
        
        public Map<String, Object> buildAsMap() {
            return new java.util.HashMap<>(map);
        }
    }
}