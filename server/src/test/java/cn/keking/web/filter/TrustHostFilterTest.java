package cn.keking.web.filter;

import cn.keking.config.ConfigConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrustHostFilterTest {

    private TrustHostFilter trustHostFilter;

    @BeforeEach
    void setUp() {
        trustHostFilter = new TrustHostFilter();
    }

    /**
     * 【测试黑名单】包含主机名时返回true
     */
    @Test
    void testIsNotTrustHost_WhenBlacklistContainsHost_ReturnsTrue() {
        try (MockedStatic<ConfigConstants> mockedConfig = Mockito.mockStatic(ConfigConstants.class)) {

            Set<String> blacklist = new HashSet<>();
            blacklist.add("evil.com");
            mockedConfig.when(ConfigConstants::getNotTrustHostSet).thenReturn(blacklist);
            mockedConfig.when(ConfigConstants::getTrustHosts).thenReturn(new String[0]);

            assertTrue(trustHostFilter.isNotTrustHost("evil.com"));
        }
    }

    /**
     * 【测试黑名单】不包含主机名时返回false
     */
    @Test
    void testIsNotTrustHost_WhenBlacklistDoesNotContainHost_ReturnsFalse() {
        try (MockedStatic<ConfigConstants> mockedConfig = Mockito.mockStatic(ConfigConstants.class)) {

            Set<String> blacklist = new HashSet<>();
            blacklist.add("evil.com");
            mockedConfig.when(ConfigConstants::getNotTrustHostSet).thenReturn(blacklist);
            mockedConfig.when(ConfigConstants::getTrustHosts).thenReturn(new String[0]);

            assertFalse(trustHostFilter.isNotTrustHost("good.com"));
        }
    }

    /**
     * 【测试白名单激活】且主机不受信任时返回true
     * 模拟白名单为["trusted.com"]，验证当传入"untrusted.com"时返回true
     */
    @Test
    void testIsNotTrustHost_WhenWhitelistActiveAndHostNotTrusted_ReturnsTrue() {
        try (MockedStatic<ConfigConstants> mockedConfig = Mockito.mockStatic(ConfigConstants.class);
             ){

            mockedConfig.when(ConfigConstants::getNotTrustHostSet).thenReturn(Collections.emptySet());
            mockedConfig.when(ConfigConstants::getTrustHosts).thenReturn(new String[]{"*.trusted.com"});

            assertTrue(trustHostFilter.isNotTrustHost("test.untrusted.com"));
        }
    }

    /**
     * 【测试白名单激活】且主机受信任时返回false
     * 模拟白名单为["*.trusted.com"]，验证当传入"test.trusted.com"时返回false
     */
    @Test
    void testIsNotTrustHost_WhenWhitelistActiveAndHostTrusted_ReturnsFalse() {
        try (MockedStatic<ConfigConstants> mockedConfig = Mockito.mockStatic(ConfigConstants.class);) {

            mockedConfig.when(ConfigConstants::getNotTrustHostSet).thenReturn(Collections.emptySet());
            mockedConfig.when(ConfigConstants::getTrustHosts).thenReturn(new String[]{"*.trusted.com"});

            assertFalse(trustHostFilter.isNotTrustHost("test.trusted.com"));
        }
    }

    /**
     * 测试未配置任何名单时返回false
     */
    @Test
    void testIsNotTrustHost_WhenNoListsConfigured_ReturnsFalse() {
        try (MockedStatic<ConfigConstants> mockedConfig = Mockito.mockStatic(ConfigConstants.class)) {

            mockedConfig.when(ConfigConstants::getNotTrustHostSet).thenReturn(Collections.emptySet());
            mockedConfig.when(ConfigConstants::getTrustHosts).thenReturn(new String[0]);

            assertFalse(trustHostFilter.isNotTrustHost("anyhost.com"));
        }
    }

    /**
     * 测试传入null主机名时返回false
     */
    @Test
    void testIsNotTrustHost_WhenNullHost_ReturnsFalse() {
        try (MockedStatic<ConfigConstants> mockedConfig = Mockito.mockStatic(ConfigConstants.class)) {

            mockedConfig.when(ConfigConstants::getNotTrustHostSet).thenReturn(Collections.emptySet());
            mockedConfig.when(ConfigConstants::getTrustHosts).thenReturn(new String[0]);

            assertFalse(trustHostFilter.isNotTrustHost(null));
        }
    }
}