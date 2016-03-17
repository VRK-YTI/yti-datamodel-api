/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.csc.fi.ioapi.config;

import java.util.HashMap;
import javax.servlet.http.HttpSession;

/**
 *
 * @author malonen
 */
public interface LoginInterface {
    
    public boolean isLoggedIn();
    
    public boolean isSuperAdmin();
    
    public boolean isInGroup(String group);
    
    public boolean isAdminOfGroup(String group);
    
    public boolean hasRightToEditModel(String resource);
   
    public boolean hasRightToEditGroup(String resource);
    
    public String getDisplayName();
    
    public String getEmail();
    
    public HashMap<String,Boolean> getGroups();
    
}
