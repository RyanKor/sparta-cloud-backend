package com.sparta.point_system.controller;

import com.sparta.point_system.entity.Membership;
import com.sparta.point_system.repository.MembershipRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class MembershipController {

    @Autowired
    private MembershipRepository membershipRepository;

    @PostMapping("/membership")
    public Membership createMembership(@RequestParam Long userId,
                                      @RequestParam Long levelId,
                                      @RequestParam(required = false) LocalDateTime expiresAt) {
        Membership membership = new Membership(userId, levelId, expiresAt);
        return membershipRepository.save(membership);
    }

    @GetMapping("/memberships")
    public List<Membership> getAllMemberships() {
        return membershipRepository.findAll();
    }

    @GetMapping("/membership/{membershipId}")
    public Optional<Membership> getMembershipById(@PathVariable Long membershipId) {
        return membershipRepository.findById(membershipId);
    }

    @GetMapping("/membership/user/{userId}")
    public Optional<Membership> getMembershipByUserId(@PathVariable Long userId) {
        return membershipRepository.findByUserId(userId);
    }

    @PutMapping("/membership/{membershipId}")
    public Membership updateMembership(@PathVariable Long membershipId,
                                      @RequestParam(required = false) Long levelId,
                                      @RequestParam(required = false) LocalDateTime expiresAt) {
        Optional<Membership> membershipOpt = membershipRepository.findById(membershipId);
        if (membershipOpt.isPresent()) {
            Membership membership = membershipOpt.get();
            if (levelId != null) membership.setLevelId(levelId);
            if (expiresAt != null) membership.setExpiresAt(expiresAt);
            return membershipRepository.save(membership);
        }
        throw new RuntimeException("Membership not found with id: " + membershipId);
    }

    @DeleteMapping("/membership/{membershipId}")
    public String deleteMembership(@PathVariable Long membershipId) {
        membershipRepository.deleteById(membershipId);
        return "Membership deleted successfully";
    }
}

