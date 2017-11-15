/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package controller;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import model.Account;
import model.Customer;
import model.Employee;
import model.Restaurant;
import model.User;

/**
 *
 * @author Dell
 */
public class ServerThread extends Thread {

    private Socket clientSocket;
    private Connection con;

    public ServerThread(Socket clientSocket) {
        this.clientSocket = clientSocket;
        OpenDB();
    }

    private void OpenDB() {
        String dbUrl = "jdbc:sqlserver://localhost;databaseName=restaurant_booking;user=sa;password=123456789";
        String dbClass = "com.microsoft.sqlserver.jdbc.SQLServerDriver";

        try {
            Class.forName(dbClass);
            con = DriverManager.getConnection(dbUrl);
            System.out.println("Datasource connected");
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(ServerThread.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SQLException ex) {
            Logger.getLogger(ServerThread.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

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
//            Logger.getLogger(ServerThread.class.getName()).log(Level.SEVERE, null, ex);
            ex.printStackTrace();
        }
    }

    public boolean CheckAccountExist(Account acc) {
        if (acc != null) {
            String sql = "SELECT * FROM tblAccount WHERE username = ?";

            try {
                PreparedStatement ps = con.prepareStatement(sql, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
                ps.setString(1, acc.getUsername());

                ResultSet rs = ps.executeQuery();

                if (!rs.last()) {
                    return false;
                }
                
                return true;
            } catch (Exception ex) {
//                Logger.getLogger(ServerThread.class.getName()).log(Level.SEVERE, null, ex);
                ex.printStackTrace();
            }
        }

        return true;
    }

    public void SendResult(Object result) {
        try {
            ObjectOutputStream oos = new ObjectOutputStream(clientSocket.getOutputStream());
            oos.writeObject(result);

//            oos.close();
//            clientSocket.close();
        } catch (Exception ex) {
//            Logger.getLogger(ServerThread.class.getName()).log(Level.SEVERE, null, ex);
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
//                Logger.getLogger(ServerThread.class.getName()).log(Level.SEVERE, null, ex);
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
//            Logger.getLogger(ServerThread.class.getName()).log(Level.SEVERE, null, ex);
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
//            Logger.getLogger(ServerThread.class.getName()).log(Level.SEVERE, null, ex);
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
//            Logger.getLogger(ServerThread.class.getName()).log(Level.SEVERE, null, ex);
            ex.printStackTrace();
        }

        return max;
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
//                Logger.getLogger(ServerThread.class.getName()).log(Level.SEVERE, null, ex);
                ex.printStackTrace();
            }
        }

        return null;
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
//            Logger.getLogger(ServerThread.class.getName()).log(Level.SEVERE, null, ex);
            ex.printStackTrace();
        }

        return res;
    }

    @Override
    public void run() {

        while (true) {
            ReceiveObject();
        }
    }
}
