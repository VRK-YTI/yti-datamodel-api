/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.csc.fi.ioapi.config;

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
        return session.getAttribute("uid")!=null;
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
    public String[] getGroupUris() {
        String[] groups = session.getAttribute("group").toString().split(";");
        for (int i = 0; i<groups.length;i++){
            int spacePos = groups[i].indexOf(" ");
            if (spacePos > 0) {
               groups[i] = groups[i].substring(0, spacePos);
            }
        }
        return groups;
    }
    
}
