package com.example.offsetcalculator.rep;

import android.app.Application;
import android.location.Location;
import android.util.Log;

import androidx.room.Room;

import com.example.offsetcalculator.dao.CoordinateDAO;
import com.example.offsetcalculator.dao.RouteDAO;
import com.example.offsetcalculator.db.AppDatabase;
import com.example.offsetcalculator.model.route.Coordinate;
import com.example.offsetcalculator.model.route.Route;

import java.util.List;

public class RouteRepository {
    private RouteDAO mRouteDao;
    private CoordinateDAO mCoodinateDao;
    private List<Route> mAllRoutes;

    public RouteRepository(Application application){
        AppDatabase db = Room.databaseBuilder(application, AppDatabase.class, "thinkarbon-db")
                .allowMainThreadQueries().fallbackToDestructiveMigration()   //Allows room to do operation on main thread
                .build();
        mRouteDao = db.getRouteDAO();
        mCoodinateDao = db.getCoordinateDAO();
        mAllRoutes = mRouteDao.getAllRoutes();
    }

    public List<Route> getmAllRoutes(){
        return mAllRoutes;
    }

    public void insert(Route route, List<Coordinate> coordinates) {
        if(route != null) {
            mRouteDao.insert(route); // route must be inserted first.
            //loop through the coordinates and insert them with the route's id
            for (int i = 0; i < coordinates.size(); i++) {
                mCoodinateDao.insert(coordinates.get(i));
            }
        } else {
            Log.d("ERROR", "Route must not be null!");
            throw new RuntimeException();
        }
    }

    public void update(Route route) {
        mRouteDao.update(route);
    }

    public void delete(Route route) {
        mRouteDao.delete(route);
    }

    public void deleteAll() {
        mRouteDao.deleteAllRoutes();
    }

    public Route getLastInsertedRoute(){
        if(mAllRoutes != null) {
            if (!mAllRoutes.isEmpty()){
                return mAllRoutes.get(mAllRoutes.size() - 1);
            } else {
                return null;
            }
        }
        else {
            return null;
        }
    }

    public Route getRouteById(Integer id) {
       return mRouteDao.getRouteById(id);
    }

    public List<Coordinate> getCoordinatesFromRoute(Route route) {
        if(route.getId() != null ){
            Log.d("SUCCESS", "Coordinates have been loaded");
            return  mCoodinateDao.findCoordinatesForRoute(route.getId());
        } else {
            Log.d("FAILURE", "Route has no id");
            throw new RuntimeException();
        }
    }

    public Double calculateDistance(List<Coordinate> coordinates) {

        float total = 0.0f; //holds the total distance betweens pairs of coordinates

        for(int i=0; i < coordinates.size(); i++){

            if(i+1 != coordinates.size()){ //if we are not dealing with last the coordinate of the array
                Coordinate cord = coordinates.get(i);
                Location loc1 = new Location("Point A"); //we must translate the coordinate object to location object so that it can be used in fused location client
                loc1.setLatitude(cord.getLatitude());
                loc1.setLongitude(cord.getLongitude());

                Coordinate cord2 = coordinates.get(i+1); //could throw an index out of bounds
                Location loc2 = new Location("Point B");
                loc2.setLatitude(cord2.getLatitude());
                loc2.setLongitude(cord2.getLongitude());
                total = total + loc1.distanceTo(loc2); //use the  distance method in location to get the distance between two points.

            }
        }
        return Math.rint(total);
    }

    public Integer generateId(){
        if(getLastInsertedRoute() != null){
            return getLastInsertedRoute().getId() + 1;
        } else {
            return  1;
        }
    }


}
