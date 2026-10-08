package com.example.LAGO.kis;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "kis")
public class KisPropertiesConfig {

    private List<Account> accounts;

    public List<Account> getAccounts() { return accounts; }
    public void setAccounts(List<Account> accounts) { this.accounts = accounts; }

    public Account byName(String name) {
        return accounts.stream()
                .filter(a -> a.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown KIS account: " + name));
    }

    public static class Account {
        private String name;
        private String appKey;
        private String appSecret;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getAppKey() { return appKey; }
        public void setAppKey(String appKey) { this.appKey = appKey; }

        public String getAppSecret() { return appSecret; }
        public void setAppSecret(String appSecret) { this.appSecret = appSecret; }
    }
}