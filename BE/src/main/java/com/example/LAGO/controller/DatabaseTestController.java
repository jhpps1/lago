package com.example.LAGO.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
public class DatabaseTestController {

    @Autowired
    private DataSource dataSource;
    
    @GetMapping("/db-connection")
    public Map<String, Object> testDbConnection() {
        Map<String, Object> response = new HashMap<>();
        
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            
            response.put("success", true);
            response.put("message", "Database connection successful");
            response.put("database_url", metaData.getURL());
            response.put("database_user", metaData.getUserName());
            response.put("database_product", metaData.getDatabaseProductName());
            response.put("database_version", metaData.getDatabaseProductVersion());
            response.put("connection_valid", connection.isValid(5));
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Database connection failed");
            response.put("error", e.getMessage());
        }
        
        return response;
    }

    @GetMapping("/tables")
    public Map<String, Object> getTables() {
        Map<String, Object> response = new HashMap<>();
        List<String> tables = new ArrayList<>();
        
        try (Connection connection = dataSource.getConnection()) {
            response.put("database_info", connection.getMetaData().getURL());
            response.put("database_user", connection.getMetaData().getUserName());
            
            DatabaseMetaData metaData = connection.getMetaData();
            ResultSet resultSet = metaData.getTables(null, "public", "%", new String[]{"TABLE"});
            
            while (resultSet.next()) {
                tables.add(resultSet.getString("TABLE_NAME"));
            }
            
            response.put("success", true);
            response.put("tables", tables);
            response.put("table_count", tables.size());
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            response.put("tables", tables);
        }
        return response;
    }

    @PostMapping("/insert-stock-data")
    public Map<String, Object> insertStockData() {
        Map<String, Object> response = new HashMap<>();
        int insertedCount = 0;
        
        String[] stocks = {
            "INSERT INTO \"STOCK_INFO\" (code, name, market) VALUES ('005930', '삼성전자', 'KOSPI')",
            "INSERT INTO \"STOCK_INFO\" (code, name, market) VALUES ('000660', 'SK하이닉스', 'KOSPI')",
            "INSERT INTO \"STOCK_INFO\" (code, name, market) VALUES ('035420', 'NAVER', 'KOSPI')",
            "INSERT INTO \"STOCK_INFO\" (code, name, market) VALUES ('051910', 'LG화학', 'KOSPI')",
            "INSERT INTO \"STOCK_INFO\" (code, name, market) VALUES ('006400', '삼성SDI', 'KOSPI')"
        };
        
        try (Connection connection = dataSource.getConnection()) {
            for (String sql : stocks) {
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    int result = stmt.executeUpdate();
                    insertedCount += result;
                }
            }
            response.put("success", true);
            response.put("message", "Stock data inserted successfully");
            response.put("insertedCount", insertedCount);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error: " + e.getMessage());
        }
        
        return response;
    }

    @PostMapping("/insert-test-accounts")
    public Map<String, Object> insertTestAccounts() {
        Map<String, Object> response = new HashMap<>();
        int insertedCount = 0;
        
        String[] accounts = {
            "INSERT INTO \"USERS\" (user_id, username, email, fcm_token, is_active, created_at) VALUES (1, 'testuser1', 'test1@example.com', 'test_fcm_token_1', true, NOW())",
            "INSERT INTO \"USERS\" (user_id, username, email, fcm_token, is_active, created_at) VALUES (2, 'testuser2', 'test2@example.com', 'test_fcm_token_2', true, NOW())",
            "INSERT INTO \"ACCOUNTS\" (account_id, user_id, account_number, total_asset, available_cash, created_at, updated_at) VALUES (1001, 1, 'TEST_ACCOUNT_001', 10000000, 10000000, NOW(), NOW())",
            "INSERT INTO \"ACCOUNTS\" (account_id, user_id, account_number, total_asset, available_cash, created_at, updated_at) VALUES (1002, 2, 'TEST_ACCOUNT_002', 10000000, 10000000, NOW(), NOW())"
        };
        
        try (Connection connection = dataSource.getConnection()) {
            for (String sql : accounts) {
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    int result = stmt.executeUpdate();
                    insertedCount += result;
                }
            }
            response.put("success", true);
            response.put("message", "Test accounts and users inserted successfully");
            response.put("insertedCount", insertedCount);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error: " + e.getMessage());
        }
        
        return response;
    }

    @GetMapping("/mock-trade-columns")
    public Map<String, Object> getMockTradeColumns() {
        Map<String, Object> response = new HashMap<>();
        List<Map<String, String>> columns = new ArrayList<>();
        
        try (Connection connection = dataSource.getConnection()) {
            String sql = """
                SELECT column_name, data_type, is_nullable, column_default
                FROM information_schema.columns
                WHERE table_name = 'mock_trade'
                ORDER BY ordinal_position
                """;
            
            try (PreparedStatement stmt = connection.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                
                while (rs.next()) {
                    Map<String, String> column = new HashMap<>();
                    column.put("column_name", rs.getString("column_name"));
                    column.put("data_type", rs.getString("data_type"));
                    column.put("is_nullable", rs.getString("is_nullable"));
                    column.put("column_default", rs.getString("column_default"));
                    columns.add(column);
                }
                
                response.put("success", true);
                response.put("columns", columns);
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error: " + e.getMessage());
        }
        
        return response;
    }

    @GetMapping("/accounts-columns")
    public Map<String, Object> getAccountsColumns() {
        Map<String, Object> response = new HashMap<>();
        List<Map<String, String>> columns = new ArrayList<>();
        
        try (Connection connection = dataSource.getConnection()) {
            String sql = """
                SELECT column_name, data_type, is_nullable, column_default
                FROM information_schema.columns
                WHERE table_name = 'accounts'
                ORDER BY ordinal_position
                """;
            
            try (PreparedStatement stmt = connection.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                
                while (rs.next()) {
                    Map<String, String> column = new HashMap<>();
                    column.put("column_name", rs.getString("column_name"));
                    column.put("data_type", rs.getString("data_type"));
                    column.put("is_nullable", rs.getString("is_nullable"));
                    column.put("column_default", rs.getString("column_default"));
                    columns.add(column);
                }
                
                response.put("success", true);
                response.put("columns", columns);
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error: " + e.getMessage());
        }
        
        return response;
    }

    @GetMapping("/users-columns")
    public Map<String, Object> getUsersColumns() {
        Map<String, Object> response = new HashMap<>();
        List<Map<String, String>> columns = new ArrayList<>();
        
        try (Connection connection = dataSource.getConnection()) {
            String sql = """
                SELECT column_name, data_type, is_nullable, column_default
                FROM information_schema.columns
                WHERE table_name = 'users'
                ORDER BY ordinal_position
                """;
            
            try (PreparedStatement stmt = connection.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                
                while (rs.next()) {
                    Map<String, String> column = new HashMap<>();
                    column.put("column_name", rs.getString("column_name"));
                    column.put("data_type", rs.getString("data_type"));
                    column.put("is_nullable", rs.getString("is_nullable"));
                    column.put("column_default", rs.getString("column_default"));
                    columns.add(column);
                }
                
                response.put("success", true);
                response.put("columns", columns);
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error: " + e.getMessage());
        }
        
        return response;
    }
}