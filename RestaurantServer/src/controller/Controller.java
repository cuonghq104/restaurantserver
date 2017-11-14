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
import model.Customer;
import model.Restaurant;
import model.Table;
import model.Time;
import model.TimeStatistic;
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

    public Customer getCustomerById(int id) {
        Customer customer = new Customer();

        String sql = "SELECT * FROM tblCustomer "
                + "WHERE id = ?";

        try {
            PreparedStatement pst = con.prepareStatement(sql, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            pst.setInt(1, id);

            ResultSet rs = pst.executeQuery();
            if (rs.last()) {
                rs.beforeFirst();
            }

            rs.next();

            int cid = rs.getInt(1);
            String name = rs.getString(2);
            String address = rs.getString(3);
            String tel = rs.getString(4);
            String email = rs.getString(5);

            customer.setAddress(address);
            customer.setEmail(email);
            customer.setId(cid);
            customer.setName(name);
            customer.setTel(tel);

        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        return customer;
    }

    @Override
    public ArrayList<Booking> searchBooking(String date, String customerName) throws RemoteException {

        ArrayList<Booking> bookings = new ArrayList<>();

        String sql = "SELECT * FROM tblBooking, tblCustomer "
                + "WHERE tblBooking.dateBooking = ? "
                + "AND tblBooking.idCustomer = tblCustomer.id "
                + "AND tblCustomer.name LIKE ? "
                + "AND status <> 'cancelled'";

        try {
            PreparedStatement pst = con.prepareStatement(sql, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            pst.setString(1, date);
            pst.setString(2, "%" + customerName + "%");

            ResultSet rs = pst.executeQuery();
            if (rs.last()) {
                rs.beforeFirst();
            }

            while (rs.next()) {

                Booking booking = new Booking();

                int id = rs.getInt(1);
                int idCustomer = rs.getInt(2);
                int numberOfCustomer = rs.getInt(3);
                int idRestaurant = rs.getInt(4);
                int idTable = rs.getInt(5);
                int idTime = rs.getInt(6);
                String dateBooking = rs.getString(7);
                String dateCreated = rs.getString(8);
                String status = rs.getString(9);

                Table table = getTableById(idTable);
                Restaurant restaurant = getRestaurantById(idRestaurant);
                Time time = getTimeById(idTime);
                Customer customer = getCustomerById(idCustomer);

                booking.setCustomer(customer);
                booking.setDateBooking(dateBooking);
                booking.setDateCreate(dateCreated);
                booking.setId(id);
                booking.setNumberOfCustomer(numberOfCustomer);
                booking.setRestaurant(restaurant);
                booking.setStatus(status);
                booking.setTable(table);
                booking.setTime(time);

                bookings.add(booking);

            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        System.out.println(bookings);
        for (Booking booking : bookings) {
            System.out.println(booking);
        }
        return bookings;
    }

    @Override
    public String updateBooking(Booking booking, String status) throws RemoteException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String cancelBooking(int id) throws RemoteException {
        String msg = "";

        String sql = "UPDATE tblBooking "
                + "SET status = 'cancelled' "
                + "WHERE id = ?";

        try {
            PreparedStatement pst = con.prepareStatement(sql);
            pst.setInt(1, id);

            int rs = pst.executeUpdate();
            if (rs > 0) {
                msg = "Hủy đặt bàn thành công";
            }
        } catch (SQLException ex) {
            msg = ex.getMessage();
            ex.printStackTrace();
        }

        return msg;
    }

    @Override
    public String addNewTable(Table table) throws RemoteException {
        String sql = "INSERT INTO tblTable(name, minSeat, maxSeat, description) "
                + "VALUES(?, ?, ?, ?) ";

        String msg = "";

        try {
            PreparedStatement pst = con.prepareStatement(sql);
            pst.setString(1, table.getName());
            pst.setInt(2, table.getMinSeats());
            pst.setInt(3, table.getMaxSeats());
            pst.setString(4, table.getDescription());

            int rs = pst.executeUpdate();
            if (rs > 0) {
                msg = "Thêm bàn thành công";
            }
        } catch (SQLException ex) {
            msg = ex.getMessage();
            ex.printStackTrace();
        }
        return msg;
    }

    @Override
    public ArrayList<TimeStatistic> getStatistic(String startDate, String endDate) throws RemoteException {
        ArrayList<TimeStatistic> statistics = new ArrayList<>();

        String sql = "SELECT X.dateBooking, COUNT(X.dateBooking) AS counting\n"
                + "FROM \n"
                + "(\n"
                + "SELECT * FROM tblBooking\n"
                + "WHERE dateBooking < ? AND dateBooking >= ?\n"
                + ") X\n"
                + "GROUP BY X.dateBooking\n"
                + "ORDER BY counting DESC";
        
        try {
            PreparedStatement pst = con.prepareStatement(sql, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            ResultSet rs = pst.executeQuery();
            if (rs.last()) {
                rs.beforeFirst();
            }
            
            while (rs.next()) {
                TimeStatistic ts = mapping.timeStatisticMapping(rs);
                statistics.add(ts);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        
        return statistics;
    }

}
