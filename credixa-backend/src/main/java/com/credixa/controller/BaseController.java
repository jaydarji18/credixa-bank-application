package com.credixa.controller;

import com.credixa.security.UserPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;

public abstract class BaseController {
    
    protected String getCurrentUserCode() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserPrincipal) {
            return ((UserPrincipal) principal).getUserCode();
        }
        return null;
    }
}
