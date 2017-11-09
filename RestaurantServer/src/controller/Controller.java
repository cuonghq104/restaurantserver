/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package controller;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import model.Restaurant;
import rmi.RmiInterface;
import view.ServerView;

/**
 *
 * @author cuong
 */
public class Controller extends UnicastRemoteObject implements RmiInterface{

    private static Controller controller;

    private static ServerView view;

    private Registry registry;
    private int rmiPort = 6788;
    private String rmiService = "restaurant-booking";

    private Connection con;

    private String dbUrl = "jdbc:sqlserver://localhost;databaseName=restaurant_booking;user=sa;password=123456789";
    private String dbClass = "com.microsoft.sqlserver.jdbc.SQLServerDriver";

    private Mapping mapping;
    
    public Controller() throws RemoteException {
        view = new ServerView();
        view.setController(this);
        connectDatabase();
        createRMIService();
        mapping = new Mapping();
        mapping.setView(view);
    }

    public ServerView getView() {
        return view;
    }

    public void setView(ServerView view) {
        this.view = view;
    }

    public static Controller getController() {
        return controller;
    }

    public static void setController(Controller controller) {
        Controller.controller = controller;
    }

    public static void main(String[] args) {
        try {
            controller = new Controller();
            controller.setView(view);
        } catch (RemoteException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public ArrayList<Restaurant> getRestaurantList(String content) throws RemoteException {
        String sql = "SELECT * FROM tblRestaurant "
                + "WHERE name LIKE ? ";

        ArrayList<Restaurant> restaurants = new ArrayList<>();
        
        try {
            PreparedStatement pst = con.prepareStatement(sql, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            pst.setString(1, "%" + content + "%");
            ResultSet rs = pst.executeQuery();
            
            if (rs.last()) {
                rs.beforeFirst();
                
                while (rs.next()) {
                    Restaurant r = mapping.restaurantMapping(rs);
                    restaurants.add(r);
                }
            }
        } catch (SQLException ex) {
            view.showMessage(ex.getMessage());
            ex.printStackTrace();
        }
        System.out.println(restaurants);
        return restaurants;
    }

    private void connectDatabase() {
        try {
            view.showMessage("Connecting database: ...");
            Class.forName(dbClass);
            con = DriverManager.getConnection(dbUrl);
            view.showMessage("SUCCEED");
        } catch (ClassNotFoundException ex) {
            view.showMessage(ex.getMessage());
            ex.printStackTrace();
        } catch (SQLException ex) {
            view.showMessage(ex.getMessage());
            ex.printStackTrace();
        }

    }
//
//    private void createRMIService() {
//        try {
////            view.showMessage("Creating rmi service: ...");
//            registry = LocateRegistry.createRegistry(rmiPort);
//            registry.rebind(rmiService, this);
//            view.showMessage("SUCCEED");
//        } catch (RemoteException ex) {
//            view.showMessage(ex.getMessage());
//            ex.printStackTrace();
//        }
//
//    }

}
