package com.sqlassistant.service;

import com.sqlassistant.model.dto.PromptSuggestion;
import com.sqlassistant.model.dto.SchemaSummary;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class SuggestionService {

    private final SchemaService schemaService;

    public SuggestionService(SchemaService schemaService) {
        this.schemaService = schemaService;
    }

    /**
     * Dynamically builds prompt suggestions based on active schema metadata.
     */
    public List<PromptSuggestion> getPromptSuggestions() {
        List<PromptSuggestion> suggestions = new ArrayList<>();
        SchemaSummary schema = schemaService.getDynamicSchema();

        boolean hasSensors = schema.getTables().stream().anyMatch(t -> t.getTableName().equalsIgnoreCase("SENSORS"));
        boolean hasMaintenance = schema.getTables().stream().anyMatch(t -> t.getTableName().equalsIgnoreCase("MAINTENANCE_LOGS"));
        boolean hasDepartments = schema.getTables().stream().anyMatch(t -> t.getTableName().equalsIgnoreCase("DEPARTMENTS"));
        boolean hasUsers = schema.getTables().stream().anyMatch(t -> t.getTableName().equalsIgnoreCase("USERS"));

        if (hasSensors) {
            suggestions.add(new PromptSuggestion(
                    "s1",
                    "Sensor Monitoring",
                    "Show all offline or maintenance sensors and their last readings",
                    "VIEWER",
                    "SELECT"
            ));
        }

        if (hasMaintenance && hasSensors) {
            suggestions.add(new PromptSuggestion(
                    "s2",
                    "Maintenance Analytics",
                    "List high-cost maintenance logs with sensor details and technician names",
                    "VIEWER",
                    "SELECT"
            ));
        }

        if (hasDepartments && hasUsers) {
            suggestions.add(new PromptSuggestion(
                    "s3",
                    "Department Insights",
                    "Show total department budgets and user counts per department",
                    "VIEWER",
                    "SELECT"
            ));
        }

        if (hasSensors) {
            suggestions.add(new PromptSuggestion(
                    "s4",
                    "Safety Test (Update)",
                    "Update status of sensor TEMP-101 to MAINTENANCE",
                    "ADMIN",
                    "UPDATE"
            ));
        }

        if (hasMaintenance) {
            suggestions.add(new PromptSuggestion(
                    "s5",
                    "Safety Test (Delete)",
                    "Delete maintenance logs with cost greater than 3000",
                    "ADMIN",
                    "DELETE"
            ));
        }

        if (hasUsers) {
            suggestions.add(new PromptSuggestion(
                    "s6",
                    "Access Control",
                    "List all users along with their assigned roles and departments",
                    "VIEWER",
                    "SELECT"
            ));
        }

        return suggestions;
    }
}
