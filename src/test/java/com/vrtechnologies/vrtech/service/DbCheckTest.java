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
}
