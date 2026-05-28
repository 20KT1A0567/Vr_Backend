package com.vrtechnologies.vrtech.service;

import com.vrtechnologies.vrtech.entity.PincodeDeliveryRule;
import com.vrtechnologies.vrtech.entity.PincodeBlacklist;
import com.vrtechnologies.vrtech.entity.PincodeZone;
import com.vrtechnologies.vrtech.repository.PincodeDeliveryRuleRepository;
import com.vrtechnologies.vrtech.repository.PincodeBlacklistRepository;
import com.vrtechnologies.vrtech.repository.PincodeZoneRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

@SpringBootTest
@ActiveProfiles("local")
class DbCheckTest {

    @Autowired
    private PincodeDeliveryRuleRepository ruleRepo;

    @Autowired
    private PincodeBlacklistRepository blacklistRepo;

    @Autowired
    private PincodeZoneRepository zoneRepo;

    @Test
    void inspectDatabaseRules() {
        System.out.println("=== INSPECTING DATABASE RULES ===");
        
        List<PincodeDeliveryRule> rules68 = ruleRepo.findByPincodeAndActiveTrueOrderByPriorityAscIdAsc("560068");
        System.out.println("Rules for 560068: " + rules68.size());
        for (PincodeDeliveryRule r : rules68) {
            System.out.println("  Rule ID=" + r.getId() + ", Serviceable=" + r.isServiceable() + ", Active=" + r.isActive());
        }

        List<PincodeDeliveryRule> rules69 = ruleRepo.findByPincodeAndActiveTrueOrderByPriorityAscIdAsc("560069");
        System.out.println("Rules for 560069: " + rules69.size());
        for (PincodeDeliveryRule r : rules69) {
            System.out.println("  Rule ID=" + r.getId() + ", Serviceable=" + r.isServiceable() + ", Active=" + r.isActive());
        }

        blacklistRepo.findByPincodeAndActiveTrue("560068").ifPresent(b -> {
            System.out.println("Blacklist entry for 560068: ID=" + b.getId() + ", Active=" + b.isActive() + ", Reason=" + b.getReason());
        });

        blacklistRepo.findByPincodeAndActiveTrue("560069").ifPresent(b -> {
            System.out.println("Blacklist entry for 560069: ID=" + b.getId() + ", Active=" + b.isActive() + ", Reason=" + b.getReason());
        });

        List<PincodeZone> zones = zoneRepo.findAllByOrderByPriorityAscIdAsc();
        System.out.println("Zones count: " + zones.size());
        for (PincodeZone z : zones) {
            System.out.println("  Zone ID=" + z.getId() + ", MatchType=" + z.getMatchType() + ", MatchValue=" + z.getMatchValue() + ", Serviceable=" + z.isServiceable());
        }
        System.out.println("=== END OF INSPECTION ===");
    }

    @Autowired
    private com.vrtechnologies.vrtech.repository.AdminPingRepository pingRepo;

    @Autowired
    private com.vrtechnologies.vrtech.controller.RealtimeAdminController realtimeController;

    @Test
    void inspectAdminPings() {
        System.out.println("=== INSPECTING ADMIN PINGS ===");
        List<com.vrtechnologies.vrtech.entity.AdminPing> pings = pingRepo.findAllByOrderByPingTimestampAsc();
        System.out.println("Pings count: " + pings.size());
        for (com.vrtechnologies.vrtech.entity.AdminPing p : pings) {
            System.out.println("  Ping ID=" + p.getId() + ", Sender=" + p.getSenderEmail() + ", Message=" + p.getMessage() + ", Timestamp=" + p.getPingTimestamp());
        }
        System.out.println("=== END OF ADMIN PINGS INSPECTION ===");
    }

    @Test
    void testBroadcastPing() {
        System.out.println("=== TESTING BROADCAST PING ===");
        try {
            java.util.Map<String, Object> payload = java.util.Map.of(
                "senderEmail", "venkat@anushatechnologies.com",
                "senderName", "Venkat Test",
                "senderRole", "SUPER_ADMIN",
                "message", "Test message from JUnit"
            );
            com.vrtechnologies.vrtech.dto.response.ApiResponse<java.util.Map<String, Object>> result = realtimeController.broadcastPing(payload);
            System.out.println("Broadcast Ping Result: " + result.getData());
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("=== END OF TESTING BROADCAST PING ===");
    }
}
