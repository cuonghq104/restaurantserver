package controller;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import model.Restaurant;
import model.Table;
import model.Time;
import model.TimeStatistic;
import view.ServerView;

/**
 *
 * @author cuong
 */
public class Mapping {

    public ServerView view;

    public Mapping() {
    }

    public ServerView getView() {
        return view;
    }

    public void setView(ServerView view) {
        this.view = view;
    }

    public Time timeMapping(ResultSet rs) {
        try {
            int id = rs.getInt("id");
            int minTime = rs.getInt("minTime");
            int maxTime = rs.getInt("maxTime");
            String period = rs.getString("period");
            
            Time time = new Time();
            time.setId(id);
            time.setMaxTime(maxTime);
            time.setMinTime(minTime);
            time.setPeriodOfDay(period);
            
            return time;
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public Restaurant restaurantMapping(ResultSet rs) {

        try {
            int id = rs.getInt("id");
            String name = rs.getString("name");
            String address = rs.getString("address");
            String tel = rs.getString("tel");
            String description = rs.getString("description");

            Restaurant restaurant = new Restaurant();
            restaurant.setAddress(address);
            restaurant.setDescription(description);
            restaurant.setId(id);
            restaurant.setName(name);
            restaurant.setTel(tel);

            return restaurant;
        } catch (SQLException ex) {
            view.showMessage(ex.getMessage());
            ex.printStackTrace();
        }
        return null;
    }
    
    public Table tableMapping(ResultSet rs) {
        try {
            int id = rs.getInt("id");
            String name = rs.getString("name");
            int maxSeat = rs.getInt("maxSeat");
            int minSeat = rs.getInt("minSeat");
            String description = rs.getString("description");
            
            Table table = new Table();
            table.setId(id);
            table.setName(name);
            table.setDescription(description);
            table.setMinSeats(minSeat);
            table.setMaxSeats(maxSeat);
            
            return table;
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return null;
       
    }
    
    public TimeStatistic timeStatisticMapping(ResultSet rs) {
        TimeStatistic ts = new TimeStatistic();
        
        try {
            String date = rs.getString("dateBooking");
            int counting = rs.getInt("counting");
            
            ts.setDateBooking(date);
            ts.setQuantity(counting);
            
        } catch (SQLException ex) {
            Logger.getLogger(Mapping.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return ts;
    }
}
