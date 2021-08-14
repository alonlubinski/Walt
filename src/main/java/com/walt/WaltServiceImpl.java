package com.walt;

import com.walt.dao.CustomerRepository;
import com.walt.dao.DeliveryRepository;
import com.walt.dao.DriverRepository;
import com.walt.model.*;
import com.walt.utils.Consts;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class WaltServiceImpl implements WaltService {

	@Autowired
	private DriverRepository driverRepository;
	
	@Autowired
	private DeliveryRepository deliveryRepository;
	
	@Autowired
	private CustomerRepository customerRepository;
	
    @Override
    public Delivery createOrderAndAssignDriver(Customer customer, Restaurant restaurant, Date deliveryTime) {
    	if(customerRepository.findByName(customer.getName()) == null) { // Check if customer exist in the system.
    		throw new RuntimeException(Consts.CUSTOMER_NOT_FOUND_MESSAGE);
    	}
    	
    	if(!customer.getCity().getName().equals(restaurant.getCity().getName())) { // Check if the customer and the restaurant are from the same city.
    		throw new RuntimeException(Consts.NOT_SAME_CITY_MESSAGE);
    	}
    	
    	// Find available driver - lives in the same city of the restaurant & customer, no other delivery at the same time.
    	Driver driver = findAvailableDriver(restaurant.getCity(), deliveryTime);
    	
    	// Create new delivery with all details.
    	Delivery delivery = new Delivery(driver, restaurant, customer, deliveryTime);
    	
    	// Save delivery to the database.
    	deliveryRepository.save(delivery);
    	
        return delivery;
    }

    @Override
    public List<DriverDistance> getDriverRankReport() {
        return deliveryRepository.getDriverRankReportByDriver();
    }

    @Override
    public List<DriverDistance> getDriverRankReportByCity(City city) {
        return deliveryRepository.getCityDriversRankReportByDriver(city);
    }
    
    // Function that gets a city and delivery time, and returns a driver that is available and can be assigned for the delivery.
    private Driver findAvailableDriver(City city, Date deliveryTime) {
    	Driver driver = new Driver();
    	List<Driver> allAvailableDrivers = getAllAvailableDrivers(city, deliveryTime);
    	driver = getTheLeastBusyDriverByDeliveries(allAvailableDrivers);
    	return driver;
    }
    
    // Function that gets a city and delivery time, and returns all the available drivers in this city at the requested delivery time.
    private List<Driver> getAllAvailableDrivers(City city, Date deliveryTime){
    	List<Driver> allAvailableDrivers = new ArrayList<>();
    	
    	// Find all drivers in city.
    	allAvailableDrivers = driverRepository.findAllDriversByCity(city);
    	
    	if(allAvailableDrivers.isEmpty()) {
    		throw new RuntimeException(Consts.NO_DRIVERS_MESSAGE);
    	} else {
    		// Filter the array and keep only the available drivers.
    		allAvailableDrivers = allAvailableDrivers.stream().filter(driver -> checkIfDriverAvailableAtTime(driver, deliveryTime)).collect(Collectors.toList());
    		if(allAvailableDrivers.isEmpty()) {
        		throw new RuntimeException(Consts.NO_DRIVERS_AVAILABLE_MESSAGE);
        	}
    	}
    	return allAvailableDrivers;
    }
    
    // Function that gets a driver and and delivery time, and returns if the driver is available to make this delivery.
    // Delivery is taking 1 hour, so driver can make a delivery only if he doesn't have any delivery at the hour before and at the hour after the 
    // requested delivery time.
    private boolean checkIfDriverAvailableAtTime(Driver driver, Date deliveryTime) {
    	final long hourInMilliseconds = 3600000;
    	long newDeliveryHourInMilliseconds = deliveryTime.getTime();
    	long minHour = newDeliveryHourInMilliseconds - hourInMilliseconds;
    	long maxHour = newDeliveryHourInMilliseconds + hourInMilliseconds;
    	
    	List<Delivery> allDeliveriesByDriver = deliveryRepository.findAllDeliveriesByDriver(driver);
    	for(Delivery delivery : allDeliveriesByDriver) {
    		long deliveryHourInMilliseconds = delivery.getDeliveryTime().getTime();
    		if(deliveryHourInMilliseconds > minHour && deliveryHourInMilliseconds < maxHour) {
    			return false;
    		}
    	}
    	return true;
    }
    
    // Function that gets list of all available drivers right now, and returns the least busy one according to number of deliveries that the driver have.
    private Driver getTheLeastBusyDriverByDeliveries(List<Driver> allAvailableDrivers) {
    	Driver leastBusyDriver = new Driver();
    	double temp = -1;
    	for(Driver driver : allAvailableDrivers) {
    		// Get all the deliveries for this driver.
    		List<Delivery> allDeliveries = deliveryRepository.findAllDeliveriesByDriver(driver);
    		
    		if(temp == -1) { // If this is the first driver in the list, save him.
    			temp = allDeliveries.size();
    			leastBusyDriver = driver;
    		} else {
    			if(allDeliveries.size() < temp) { 
    				// If the number of deliveries of this driver is smaller then the number of deliveries of the saved driver - switch between them.
    				temp = allDeliveries.size();
        			leastBusyDriver = driver;
    			}
    		}
    	}
    	return leastBusyDriver;
    }
}
