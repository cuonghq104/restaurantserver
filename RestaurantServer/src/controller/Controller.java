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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import model.Booking;
import model.Restaurant;
import model.Table;
import model.Time;
import rmi.RMIService;
import view.ServerView;

/**
 *
 * @author cuong
 */
public class Controller extends UnicastRemoteObject implements RMIService {

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

    private void createRMIService() {
        try {
            view.showMessage("Creating rmi service: ...");
            registry = LocateRegistry.createRegistry(rmiPort);
            registry.rebind(rmiService, this);
            view.showMessage("SUCCEED");
        } catch (RemoteException ex) {
            view.showMessage(ex.getMessage());
            ex.printStackTrace();
        }

    }

    @Override
    public ArrayList<Time> getTimesList() throws RemoteException {
        String sql = "SELECT * FROM tblTime ";
        ArrayList<Time> times = new ArrayList<>();
        try {
            PreparedStatement pst = con.prepareStatement(sql, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            ResultSet rs = pst.executeQuery();

            if (rs.last()) {
                rs.beforeFirst();
            }

            while (rs.next()) {
                Time time = mapping.timeMapping(rs);
                times.add(time);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return times;
    }

    @Override
    public Table findTable(int seatNumber, int time, String date, int restaurant) throws RemoteException {
        String sql = "SELECT * "
                + "FROM tblTable "
                + "WHERE id NOT IN "
                + "( "
                + "SELECT idTable "
                + "FROM tblBooking "
                + "WHERE idTime=? AND idRestaurant=? AND dateBooking=? AND status='booking'"
                + ") "
                + "AND ? BETWEEN tblTable.minSeat AND tblTable.maxSeat ";

        Table table = null;

        System.out.println(seatNumber);
        System.out.println(time);
        System.out.println(date);
        System.out.println(restaurant);

        try {
            PreparedStatement pst = con.prepareStatement(sql, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            pst.setInt(1, time);
            pst.setInt(2, restaurant);
            pst.setString(3, date);
            pst.setInt(4, seatNumber);
            System.out.println(pst.toString());

            ResultSet rs = pst.executeQuery();

            if (rs.last()) {
                rs.beforeFirst();
            }

            if (rs.next()) {
                table = mapping.tableMapping(rs);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        return table;
    }

    @Override
    public Table getTableById(int id) throws RemoteException {
        String sql = "SELECT * FROM tblTable "
                + "WHERE id = ? ";

        Table table = null;
        try {
            PreparedStatement pst = con.prepareStatement(sql, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            pst.setInt(1, id);
            ResultSet rs = pst.executeQuery();

            if (rs.last()) {
                rs.beforeFirst();
            }

            if (rs.next()) {
                table = mapping.tableMapping(rs);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return table;
    }

    @Override
    public Restaurant getRestaurantById(int id) throws RemoteException {
        String sql = "SELECT * FROM tblRestaurant "
                + "WHERE id = ? ";

        Restaurant restaurant = null;
        try {
            PreparedStatement pst = con.prepareStatement(sql, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            pst.setInt(1, id);
            ResultSet rs = pst.executeQuery();

            if (rs.last()) {
                rs.beforeFirst();
            }

            if (rs.next()) {
                restaurant = mapping.restaurantMapping(rs);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return restaurant;
    }

    @Override
    public Time getTimeById(int id) throws RemoteException {
        String sql = "SELECT * FROM tblTime "
                + "WHERE id = ? ";

        Time time = null;
        
        try {
            PreparedStatement pst = con.prepareStatement(sql, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            pst.setInt(1, id);
            ResultSet rs = pst.executeQuery();

            if (rs.last()) {
                rs.beforeFirst();
            }

            if (rs.next()) {
                time = mapping.timeMapping(rs);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return time;
    }

    private String getCurrentDate() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, 1);
        Date date = cal.getTime();
        SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd");
        String date1 = format1.format(date);
        return date1;
    }
    
    @Override
    public String booking(Booking booking) throws RemoteException {
        
        String msg = "";
        
        Table table = null;
        String sql = "INSERT INTO tblBooking(idRestaurant, numberOfCustomer, idTable, idTime, dateBooking, dateCreated, status) "
                + "VALUES(?, ?, ?, ?, ?, ?, ?) ";
        
        try {
            PreparedStatement pst = con.prepareStatement(sql);
            pst.setInt(1, booking.getRestaurant().getId());
            pst.setInt(2, booking.getNumberOfCustomer());
            pst.setInt(3, booking.getTable().getId());
            pst.setInt(4, booking.getTime().getId());
            pst.setString(5, booking.getDateBooking());
            pst.setString(6, getCurrentDate());
            pst.setString(7, "booking");
            
            int rs = pst.executeUpdate();
            
            if (rs > 0) {
                msg = "Đặt bàn thành công";
            }
        } catch (SQLException ex) {
            msg = ex.getMessage();
            ex.printStackTrace();
        }
        return msg;
    }
}

