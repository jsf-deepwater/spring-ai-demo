package com.deepwater.longtermmemory.controller;

import com.deepwater.longtermmemory.entity.Preference;
import com.deepwater.longtermmemory.service.PreferenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class PreferenceController {
    private final PreferenceService preferenceService;

    @GetMapping("preference/add")
    public String add(String userId, String category,String preference){
        long id = preferenceService.add(userId, category, preference);
        return id + "";
    }
    @GetMapping("/preference/list")
    public List<Preference> list(@RequestParam String userId) {
        return preferenceService.findByUserId(userId);
    }
}
