/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.csc.fi.ioapi.config;

import com.csc.fi.ioapi.utils.ServiceDescriptionManager;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpSession;

/**
 *
 * @author malonen
 */
public class LoginSession implements LoginInterface {

    private HttpSession session;
    private static final Logger logger = Logger.getLogger(LoginSession.class.getName());
    
    public LoginSession(HttpSession httpSession) {
        this.session = httpSession;
    }

    @Override
    public boolean isLoggedIn() {
        return (session.getAttribute("mail")!=null);
    }


    @Override
    public boolean isInGroup(String group) {
        return session.getAttribute("group").toString().contains(group);
    }

    @Override
    public String getDisplayName() {
        try {
            return new String(session.getAttribute("displayName").toString().getBytes("ISO-8859-1"),"UTF-8");
        } catch (UnsupportedEncodingException ex) {
           logger.warning("Unable to convert ISO-8859-1 to UTF-8");
           return session.getAttribute("displayName").toString();
        }
    }

    @Override
    public String getEmail() {
       return session.getAttribute("mail").toString();
    }

    @Override
    public HashMap<String,Boolean> getGroups() {
        
        /* Group string format: https://example.org#GROUOP_ADMINS;https://example.org#GROUP_MEMBERS;...;*/
        
        String[] groupString;
        
        if(ApplicationProperties.getDebugMode()) {
            groupString = ApplicationProperties.getDebugGroups().split(";");
        } else if(session.getAttribute("group")==null) {
            return null;
        } else {
            groupString = session.getAttribute("group").toString().split(";");
        }
        
        HashMap groups = new HashMap();

        for (int i = 0; i<groupString.length;i++){
            
            String[] myGroup = groupString[i].split("_");
            
            if(myGroup[0].startsWith(ApplicationProperties.getGroupDomain())) {
                if(myGroup[1].equals("ADMINS")) {
                    groups.put(myGroup[0], Boolean.TRUE); }
                else {
                    groups.put(myGroup[0], Boolean.FALSE); }
            }
        }
        
        return groups;
       
    }

    @Override
    public boolean hasRightToEditModel(String model) {
        
        if(this.getGroups()==null) return false;

        return ServiceDescriptionManager.isModelInGroup(model,this.getGroups());
    }
    
    @Override
    public boolean hasRightToEditGroup(String group) {
        
        if(this.getGroups()==null) return false;
        
        if(ApplicationProperties.getDebugMode()) return true;
       
        return isInGroup(group);
    }
    
}
