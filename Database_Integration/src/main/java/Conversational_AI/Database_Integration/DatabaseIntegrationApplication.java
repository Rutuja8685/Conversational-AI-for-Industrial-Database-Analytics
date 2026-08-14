package Conversational_AI.Database_Integration;

import Conversational_AI.Database_Integration.ai.SqlGeneratorAgent;
import Conversational_AI.Database_Integration.service.DatabaseExecutorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.List;
import java.util.Map;
import java.util.Scanner;

@SpringBootApplication
public class DatabaseIntegrationApplication implements CommandLineRunner {

    @Autowired
    private SqlGeneratorAgent aiAgent;

    @Autowired
    private DatabaseExecutorService executorService;

    public static void main(String[] args) {
        SpringApplication.run(DatabaseIntegrationApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        Scanner scanner = new Scanner(System.in);
       System.out.println("==================================================");
System.out.println("   CONVERSATIONAL AI INDUSTRIAL CONSOLE IS LIVE!   ");
System.out.println("==================================================");
        // Simulating Login / Role Selection for RBAC compliance
        System.out.print("Enter your system role (OPERATOR / MANAGER): ");
        String currentUserRole = scanner.nextLine().trim().toUpperCase();
        System.out.println("Logged in successfully as role: " + currentUserRole);
        System.out.println("--------------------------------------------------");
        System.out.println("Type 'exit' to quit.\n");

        String lastGeneratedSql = "";
        boolean awaitingConfirmation = false;

        while (true) {
            if (awaitingConfirmation) {
                System.out.print("Do you want to execute this query? (yes/no): ");
            } else {
                System.out.print("Ask your database: ");
            }
            
            String userInput = scanner.nextLine();

            if ("exit".equalsIgnoreCase(userInput.trim())) {
                System.out.println("Shutting down AI Playground...");
                break;
            }

            try {
                if (awaitingConfirmation) {
                    if (userInput.equalsIgnoreCase("yes") || userInput.equalsIgnoreCase("y")) {
                        // Pass user role down to execute secure modification checks
                        int rowsAffected = executorService.executeModifyQuery(lastGeneratedSql, currentUserRole);
                        System.out.println("[Database Status]: Success! " + rowsAffected + " rows altered.");
                    } else {
                        System.out.println("[Database Status]: Operation cancelled by user safely.");
                    }
                    System.out.println("==================================================\n");
                    awaitingConfirmation = false;
                    lastGeneratedSql = "";
                    continue;
                }

                String generatedSql = aiAgent.generateSqlQuery(userInput);
                System.out.println("\n[AI Translated SQL]: " + generatedSql);

                String cleanLookup = generatedSql.replaceAll("(?i)```sql", "").replaceAll("```", "").trim().toUpperCase();
                cleanLookup = cleanLookup.replaceAll("^[^A-Z]+", "");

                if (cleanLookup.startsWith("UPDATE") || cleanLookup.startsWith("DELETE") || cleanLookup.startsWith("INSERT")) {
                    
                    // Fail-fast logic if an OPERATOR attempts a modification query string
                    if (!"MANAGER".equalsIgnoreCase(currentUserRole) && !"ADMIN".equalsIgnoreCase(currentUserRole)) {
                        System.err.println("[SECURITY BLOCK]: Access Denied. Operators cannot execute modification commands.");
                        System.out.println("==================================================\n");
                        continue;
                    }

                    lastGeneratedSql = generatedSql;
                    awaitingConfirmation = true;
                    System.out.println("⚠️ WARNING: This query will modify data details.");
                } else {
                    List<Map<String, Object>> dataResult = executorService.executeSelectQuery(generatedSql);
                    System.out.println("[Database Results]:");
                    if (dataResult.isEmpty()) {
                        System.out.println(" -> No matching entries found.");
                    } else {
                        dataResult.forEach(row -> System.out.println("    " + row));
                    }
                    System.out.println("==================================================\n");
                }

            } catch (Exception e) {
                System.err.println("\n[ERROR/SECURITY BLOCK]: " + e.getMessage());
                System.out.println("Please try rephrasing your request.\n");
                awaitingConfirmation = false;
                lastGeneratedSql = "";
            }
        }
        scanner.close();
    }
}