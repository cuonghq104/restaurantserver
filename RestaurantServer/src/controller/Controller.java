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
import java.util.logging.Level;
import java.util.logging.Logger;
import model.Account;
import model.Booking;
import model.Customer;
import model.Employee;
import model.Restaurant;
import model.Table;
import model.Time;
import model.TimeStatistic;
import model.User;
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
            myServer = new ServerSocket(6789);
            while (true) {
                clientSocket = myServer.accept();
                ReceiveObject();
            }
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
            pst.setString(1, endDate);
            pst.setString(2, startDate);
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
                        SendResult(GetUserByAccount(acc));
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
    
    private User GetUserByAccount(Account acc) {
        if (acc != null) {
            String sql = "SELECT * FROM tblAccount WHERE username = ?";

            try {
                PreparedStatement ps = con.prepareStatement(sql, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
                ps.setString(1, acc.getUsername());

                ResultSet rs = ps.executeQuery();

                if (!rs.last()) {
                    if(rs.getString("type").equals("Employee")) {
                        String empSql = "SELECT * FROM tblEmployee WHERE accountId=?";
                        
                        PreparedStatement psEmp = con.prepareStatement(empSql, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
                        psEmp.setInt(1, rs.getInt("id"));
                        
                        ResultSet rsEmp = psEmp.executeQuery();
                        
                        if(rsEmp.last()) {
                            Employee emp = new Employee();
                            emp.setName(rsEmp.getString(rsEmp.getString("name")));
                            emp.setRestaurant(GetRestaurantById(rs.getInt("restaurantId")));
                            
                            return emp;
                        }
                    }
                    else {
                        String cusSql = "SELECT * FROM tblCustomer WHERE accountId=?";
                        
                        PreparedStatement psCus = con.prepareStatement(cusSql, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
                        psCus.setInt(1, rs.getInt("id"));
                        
                        ResultSet rsCus = psCus.executeQuery();
                        
                        if(rsCus.last()) {
                            Customer cus = new Customer();
                            cus.setName(rsCus.getString(rsCus.getString("name")));
                            
                            return cus;
                        }
                    }
                }
            } catch (Exception ex) {
//                Logger.getLogger(ServerCtrl.class.getName()).log(Level.SEVERE, null, ex);
                ex.printStackTrace();
            }
        }

        return null;
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

    public void SendResult(Object result) {
        try {
            ObjectOutputStream oos = new ObjectOutputStream(clientSocket.getOutputStream());
            oos.writeObject(result);

//            oos.close();
//            clientSocket.close();
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

    @Override
    public ArrayList<Booking> seeBookingOnDate(String date) throws RemoteException {
        ArrayList<Booking> bookings = new ArrayList<>();

        try {

            String sql = "SELECT *\n"
                    + "FROM tblBooking\n"
                    + "WHERE dateBooking = ?\n";

            PreparedStatement pst = con.prepareStatement(sql, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            pst.setString(1, date);

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
        return bookings;
    }
}
