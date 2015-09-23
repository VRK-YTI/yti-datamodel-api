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
public class LoginSession implements LoginInterface {

    private HttpSession session;
    
    public LoginSession(HttpSession httpSession) {
        this.session = httpSession;
    }

    @Override
    public boolean isLoggedIn() {
        return !(session.getAttribute("mail")==null);
    }

    @Override
    public boolean isInGroup(String group) {
        return session.getAttribute("group").toString().contains(group);
    }

    @Override
    public String getDisplayName() {
        return session.getAttribute("displayName").toString();
    }

    @Override
    public String getEmail() {
       return session.getAttribute("mail").toString();
    }

    @Override
    public HashMap<String,Boolean> getGroups() {
        HashMap groups = new HashMap();
        
        String[] groupString = session.getAttribute("group").toString().split(";");
        
        for (int i = 0; i<groupString.length;i++){
            
            String[] myGroup = groupString[i].split("_");
            
            if(myGroup[0].startsWith("https://tt.eduuni.fi/sites/csc-iow#")) {
                if(myGroup[1].equals("_ADMINS")) {
                    groups.put(myGroup[0], true); }
                else {
                    groups.put(myGroup[0], false); }
            }
        }
        
        return groups;
       
    }
    
}
