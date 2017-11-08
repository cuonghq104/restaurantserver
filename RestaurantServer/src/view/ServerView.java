/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package view;

import controller.Controller;

/**
 *
 * @author cuong
 */
public class ServerView {
    
    public Controller controller;

    public ServerView() {
    }

    public Controller getController() {
        return controller;
    }

    public void setController(Controller controller) {
        this.controller = controller;
    }
    
    
    public void showMessage(String msg) {
        System.out.println(msg);
    }
}
