package controller;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import model.Restaurant;
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
}
