package com.sqlassistant;

import com.sqlassistant.model.Role;
import com.sqlassistant.model.User;
import com.sqlassistant.model.dto.ConfirmRequest;
import com.sqlassistant.model.dto.QueryRequest;
import com.sqlassistant.model.dto.QueryResponse;
import com.sqlassistant.repository.UserRepository;
import com.sqlassistant.service.QueryExecutionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=none",
    "spring.jpa.defer-datasource-initialization=true",
    "spring.sql.init.mode=always"
})
public class ChatSqlControllerTest {

    @Autowired
    private QueryExecutionService queryExecutionService;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        // Ensure standard users exist in repository
        if (userRepository.findById(1L).isEmpty()) {
            userRepository.save(new User(1L, "sarah_admin", "admin123", "Sarah Jenkins", "sarah@factory.io", Role.ADMIN, 1L));
        }
        if (userRepository.findById(2L).isEmpty()) {
            userRepository.save(new User(2L, "alex_engineer", "engineer123", "Alex Rivera", "alex@factory.io", Role.ENGINEER, 1L));
        }
        if (userRepository.findById(3L).isEmpty()) {
            userRepository.save(new User(3L, "sam_viewer", "viewer123", "Sam Taylor", "sam@factory.io", Role.VIEWER, 2L));
        }
    }

    @Test
    void testChitchatIntentReturnsGreetingWithoutSql() {
        QueryRequest request = new QueryRequest("hi, who are you?", 1L);
        QueryResponse response = queryExecutionService.processQuery(request);

        assertEquals("EXECUTED", response.getStatus());
        assertEquals("CHITCHAT", response.getOperationType());
        assertNull(response.getSql());
        assertNotNull(response.getSummary());
        assertTrue(response.getSummary().contains("Conversational AI Database Assistant"));
    }

    @Test
    void testSelectQueryExecutesImmediately() {
        QueryRequest request = new QueryRequest("Show all sensors and their locations", 3L); // Viewer
        QueryResponse response = queryExecutionService.processQuery(request);

        assertEquals("EXECUTED", response.getStatus());
        assertEquals("SELECT", response.getOperationType());
        assertNotNull(response.getResult());
        assertNotNull(response.getSummary());
        assertTrue(response.getResult().getRowCount() > 0);
    }

    @Test
    void testUpdateQueryRequiresConfirmation() {
        QueryRequest request = new QueryRequest("Update status of sensor TEMP-101 to MAINTENANCE", 2L); // Engineer
        QueryResponse response = queryExecutionService.processQuery(request);

        assertEquals("NEEDS_CONFIRMATION", response.getStatus());
        assertEquals("UPDATE", response.getOperationType());
        assertNotNull(response.getQueryId());
    }

    @Test
    void testRbacBlocksEngineerFromApprovingWriteQuery() {
        // 1. Trigger write query
        QueryRequest request = new QueryRequest("Update status of sensor TEMP-101 to MAINTENANCE", 2L);
        QueryResponse response = queryExecutionService.processQuery(request);
        String queryId = response.getQueryId();

        // 2. Engineer attempts to confirm
        ConfirmRequest confirmReq = new ConfirmRequest(queryId, 2L, true); // User 2 = ENGINEER
        QueryResponse confirmResp = queryExecutionService.confirmQuery(confirmReq);

        assertEquals("BLOCKED_RBAC", confirmResp.getStatus());
        assertTrue(confirmResp.getError().contains("RBAC Violation"));
    }

    @Test
    void testAdminCanApproveWriteQuery() {
        // 1. Trigger write query
        QueryRequest request = new QueryRequest("Update status of sensor TEMP-101 to MAINTENANCE", 2L);
        QueryResponse response = queryExecutionService.processQuery(request);
        String queryId = response.getQueryId();

        // 2. Admin confirms
        ConfirmRequest confirmReq = new ConfirmRequest(queryId, 1L, true); // User 1 = ADMIN
        QueryResponse confirmResp = queryExecutionService.confirmQuery(confirmReq);

        assertEquals("EXECUTED", confirmResp.getStatus());
        assertNotNull(confirmResp.getResult());
        assertEquals(1, confirmResp.getResult().getRowsAffected());
    }
}
