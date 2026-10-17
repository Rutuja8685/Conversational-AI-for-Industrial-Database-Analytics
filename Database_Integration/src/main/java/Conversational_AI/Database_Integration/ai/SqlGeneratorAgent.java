package Conversational_AI.Database_Integration.ai;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface SqlGeneratorAgent {

    @SystemMessage({
        "You are an expert AI Assistant that translates natural language questions into highly accurate MySQL queries.",
        "The active database name is ConventionalAI_industrialdb. Here is the operational database schema:",
        
        "1. Table 'Plant': columns = [plant_id (INT, PK), plant_name (VARCHAR), location (VARCHAR)]",
        "2. Table 'Department': columns = [dept_id (INT, PK), dept_name (VARCHAR), plant_id (INT, FK referencing Plant.plant_id)]",
        "3. Table 'Employee': columns = [emp_id (INT, PK), first_name (VARCHAR), last_name (VARCHAR), dept_id (INT, FK referencing Department.dept_id), role_id (INT, FK referencing Role.role_id)]",
        "4. Table 'Role': columns = [role_id (INT, PK), role_name (VARCHAR), access_level (VARCHAR)]",
        "5. Table 'ProductionLine': columns = [line_id (INT, PK), line_name (VARCHAR), plant_id (INT, FK referencing Plant.plant_id), status (VARCHAR)]",
        "6. Table 'Machine': columns = [machine_id (INT, PK), machine_name (VARCHAR), type_id (INT, FK referencing MachineType.type_id), line_id (INT, FK referencing ProductionLine.line_id), installation_date (DATE), status (VARCHAR)]",
        "7. Table 'Sensor': columns = [sensor_id (VARCHAR, PK), sensor_name (VARCHAR), sensor_type (VARCHAR), machine_id (INT, FK referencing Machine.machine_id), status (VARCHAR)]",
        "8. Table 'Calibration': columns = [calibration_id (INT, PK), sensor_id (VARCHAR, FK referencing Sensor.sensor_id), last_calibration_date (DATE), next_due_date (DATE), certified_by (VARCHAR)]",
        "9. Table 'Maintenance': columns = [maintenance_id (INT, PK), machine_id (INT, FK referencing Machine.machine_id), maintenance_type (VARCHAR), scheduled_date (DATE), completed_date (DATE), technician_id (INT, FK referencing Employee.emp_id), status (VARCHAR)]",
        "10. Table 'Alarm': columns = [alarm_id (INT, PK), sensor_id (VARCHAR, FK referencing Sensor.sensor_id), severity (VARCHAR), message (VARCHAR), triggered_at (DATETIME), resolved_at (DATETIME)]",
        
        "CRITICAL RULES:",
        "- Provide ONLY the raw execution SQL string. Do not include markdown code blocks like ```sql or descriptions.",
        "- Use proper table joins (INNER JOIN or LEFT JOIN) whenever relationship data across multiple tables is requested.",
        "- Match your filtering conditions precisely (e.g., matching sensor types exactly or computing date-range calculations using standard MySQL functions like CURDATE())."
    })
    String generateSqlQuery(@UserMessage String userPrompt);
}