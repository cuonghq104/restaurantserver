/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package controller;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
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
import model.Account;
import model.Booking;
import model.Customer;
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

    //Added
    private ServerSocket myServer;
    private Socket clientSocket;

    private Mapping mapping;

    public Controller() throws RemoteException {
        view = new ServerView();
        view.setController(this);
        connectDatabase();
        createRMIService();
        mapping = new Mapping();
        mapping.setView(view);

        try {
            //Added
            myServer = new ServerSocket(rmiPort);
            clientSocket = myServer.accept();
        } catch (Exception ex) {
//            Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
            ex.printStackTrace();
        }
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

    //Added
    public void ReceiveObject() {
        try {
            ObjectInputStream ois = new ObjectInputStream(clientSocket.getInputStream());
            Object o = ois.readObject();

            if (o instanceof Object[]) {
                Object[] arr = (Object[]) o;
                if (arr[0] != null && arr[0] instanceof Account && arr[1] == null) {
                    Account acc = (Account) arr[0];
                    if (CheckAccountExist(acc)) {
                        SendResult("OK");
                    } else {
                        SendResult("FAILED");
                    }
                } else if (arr[0] != null && arr[1] != null && arr[0] instanceof Account && arr[1] instanceof Customer) {
                    Account acc = (Account) arr[0];
                    Customer cus = (Customer) arr[1];

                    String result = "";

                    if (!CheckAccountExist(acc)) {
                        result = "OK";

                        if (AddAccount(acc)) {
                            if (!CheckCustomerExist(cus)) {
                                if (AddCustomer(cus)) {
                                    SendResult(result);
                                } else {
                                    result = "FAILED";
                                    SendResult(result);
                                }
                            }
                        } else {
                            result = "FAILED";
                            SendResult(result);
                        }
                    } else {
                        result = "FAILED";
                        SendResult(result);
                    }
                }
            }
        } catch (Exception ex) {
//            Logger.getLogger(ServerCtrl.class.getName()).log(Level.SEVERE, null, ex);
            ex.printStackTrace();
        }
    }

    public boolean CheckAccountExist(Account acc) {
        if (acc != null) {

            String sql = "SELECT username FROM tblAccount WHERE username = ?";

            try {
                PreparedStatement ps = con.prepareStatement(sql, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
                ps.setString(1, acc.getUsername());

                ResultSet rs = ps.executeQuery();

                if (!rs.last()) {
                    System.out.println(rs);
                    return false;
                }

                return true;
            } catch (Exception ex) {
//                Logger.getLogger(ServerCtrl.class.getName()).log(Level.SEVERE, null, ex);
                ex.printStackTrace();
            }
        }

        return false;
    }

    public void SendResult(String result) {
        try {
            ObjectOutputStream oos = new ObjectOutputStream(clientSocket.getOutputStream());
            oos.writeObject(result);
        } catch (Exception ex) {
//            Logger.getLogger(ServerCtrl.class.getName()).log(Level.SEVERE, null, ex);
            ex.printStackTrace();
        }
    }

    public boolean CheckCustomerExist(Customer cus) {
        if (cus != null) {

            String sql = "SELECT name FROM tblCustomer WHERE name = ?";

            try {
                PreparedStatement ps = con.prepareStatement(sql, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
                ps.setString(1, cus.getName());

                ResultSet rs = ps.executeQuery();

                if (!rs.last()) {
                    System.out.println(rs);
                    return false;
                }

                return true;
            } catch (Exception ex) {
//                Logger.getLogger(ServerCtrl.class.getName()).log(Level.SEVERE, null, ex);
                ex.printStackTrace();
            }
        }

        return false;
    }

    public boolean AddCustomer(Customer customer) {
        String sql = "INSERT INTO tblCustomer (name, address, tel, email, accountId) VALUES (?,?,?,?,?)";

        int id = GetMaxId();

        try {
            PreparedStatement ps = con.prepareStatement(sql, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            ps.setString(1, customer.getName());
            ps.setString(2, customer.getAddress());
            ps.setString(3, customer.getTel());
            ps.setString(4, customer.getEmail());
            ps.setInt(5, id);

            ps.executeUpdate();
            return true;
        } catch (Exception ex) {
//            Logger.getLogger(ServerCtrl.class.getName()).log(Level.SEVERE, null, ex);
            ex.printStackTrace();
        }

        return false;
    }

    public boolean AddAccount(Account acc) {
        String sql = "INSERT INTO tblAccount (username, password, type) VALUES (?,?,?)";

        try {
            PreparedStatement ps = con.prepareStatement(sql, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            ps.setString(1, acc.getUsername());
            ps.setString(2, acc.getPassword());
            ps.setString(3, acc.getType());

            ps.executeUpdate();
            return true;
        } catch (Exception ex) {
//            Logger.getLogger(ServerCtrl.class.getName()).log(Level.SEVERE, null, ex);
            ex.printStackTrace();
        }

        return false;
    }

    private int GetMaxId() {
        String sql = "SELECT MAX(id) FROM tblAccount";
        int max = 0;

        try {
            PreparedStatement ps = con.prepareStatement(sql, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);

            ResultSet rs = ps.executeQuery();

            if (rs.last()) {
                max = rs.getInt(1);
                return max;
            }
        } catch (Exception ex) {
//            Logger.getLogger(ServerCtrl.class.getName()).log(Level.SEVERE, null, ex);
            ex.printStackTrace();
        }

        return max;
    }

    private Restaurant GetRestaurantById(int id) {
        Restaurant res = null;

        String sql = "SELECT * FROM tblRestaurant WHERE id=?";

        try {
            PreparedStatement ps = con.prepareStatement(sql, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            ps.setInt(1, id);

            ResultSet rs = ps.executeQuery();
            if (rs.last()) {
                res = new Restaurant();
                res.setName(rs.getString("name"));
                res.setAddress(rs.getString("address"));
                res.setTel(rs.getString("tel"));
                res.setDescription(rs.getString("description"));

                return res;
            }
        } catch (Exception ex) {
//            Logger.getLogger(ServerCtrl.class.getName()).log(Level.SEVERE, null, ex);
            ex.printStackTrace();
        }

        return res;
    }
}
