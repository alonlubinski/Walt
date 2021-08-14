package com.walt;

import com.walt.dao.*;
import com.walt.model.City;
import com.walt.model.Customer;
import com.walt.model.Delivery;
import com.walt.model.Driver;
import com.walt.model.DriverDistance;
import com.walt.model.Restaurant;
import com.walt.utils.Consts;

import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.annotation.DirtiesContext;

import javax.annotation.Resource;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@SpringBootTest()
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class WaltTest {

    @TestConfiguration
    static class WaltServiceImplTestContextConfiguration {

        @Bean
        public WaltService waltService() {
            return new WaltServiceImpl();
        }
    }

    @Autowired
    WaltService waltService;

    @Resource
    CityRepository cityRepository;

    @Resource
    CustomerRepository customerRepository;

    @Resource
    DriverRepository driverRepository;

    @Resource
    DeliveryRepository deliveryRepository;

    @Resource
    RestaurantRepository restaurantRepository;

    @BeforeEach()
    public void prepareData(){

        City jerusalem = new City("Jerusalem");
        City tlv = new City("Tel-Aviv");
        City bash = new City("Beer-Sheva");
        City haifa = new City("Haifa");

        cityRepository.save(jerusalem);
        cityRepository.save(tlv);
        cityRepository.save(bash);
        cityRepository.save(haifa);

        createDrivers(jerusalem, tlv, bash, haifa);

        createCustomers(jerusalem, tlv, haifa);

        createRestaurant(jerusalem, tlv);
    }

    private void createRestaurant(City jerusalem, City tlv) {
        Restaurant meat = new Restaurant("meat", jerusalem, "All meat restaurant");
        Restaurant vegan = new Restaurant("vegan", tlv, "Only vegan");
        Restaurant cafe = new Restaurant("cafe", tlv, "Coffee shop");
        Restaurant chinese = new Restaurant("chinese", tlv, "chinese restaurant");
        Restaurant mexican = new Restaurant("restaurant", tlv, "mexican restaurant ");

        restaurantRepository.saveAll(Lists.newArrayList(meat, vegan, cafe, chinese, mexican));
    }

    private void createCustomers(City jerusalem, City tlv, City haifa) {
        Customer beethoven = new Customer("Beethoven", tlv, "Ludwig van Beethoven");
        Customer mozart = new Customer("Mozart", jerusalem, "Wolfgang Amadeus Mozart");
        Customer chopin = new Customer("Chopin", haifa, "Frédéric François Chopin");
        Customer rachmaninoff = new Customer("Rachmaninoff", tlv, "Sergei Rachmaninoff");
        Customer bach = new Customer("Bach", tlv, "Sebastian Bach. Johann");

        customerRepository.saveAll(Lists.newArrayList(beethoven, mozart, chopin, rachmaninoff, bach));
    }

    private void createDrivers(City jerusalem, City tlv, City bash, City haifa) {
        Driver mary = new Driver("Mary", tlv);
        Driver patricia = new Driver("Patricia", tlv);
        Driver jennifer = new Driver("Jennifer", haifa);
        Driver james = new Driver("James", bash);
        Driver john = new Driver("John", bash);
        Driver robert = new Driver("Robert", jerusalem);
        Driver david = new Driver("David", jerusalem);
        Driver daniel = new Driver("Daniel", tlv);
        Driver noa = new Driver("Noa", haifa);
        Driver ofri = new Driver("Ofri", haifa);
        Driver nata = new Driver("Neta", jerusalem);

        driverRepository.saveAll(Lists.newArrayList(mary, patricia, jennifer, james, john, robert, david, daniel, noa, ofri, nata));
    }

    @Test
    public void testBasics(){

        assertEquals(((List<City>) cityRepository.findAll()).size(),4);
        assertEquals((driverRepository.findAllDriversByCity(cityRepository.findByName("Beer-Sheva")).size()), 2);
    }
    
    @Test
    public void testCreateOneOrderAndAssignDriver() {
    	// Valid customer, restaurant and delivery time.
    	Customer customer = customerRepository.findByName("Beethoven");
    	Restaurant restaurant = restaurantRepository.findByName("vegan");
    	Date deliveryTime = new Date();
    	// Expecting to get delivery object that saved to the database.
    	Delivery delivery = waltService.createOrderAndAssignDriver(customer, restaurant, deliveryTime);
    	assertNotNull(delivery);
    	assertEquals(customer.getCity().getName(), delivery.getDriver().getCity().getName());
    }
    
    @Test
    public void testCreateOneOrderAndAssignDriverCustomerAndRestaurantDifferentCity() {
    	// Valid customer, restaurant and delivery time, but from different cities.
    	Customer customer = customerRepository.findByName("Mozart");
    	Restaurant restaurant = restaurantRepository.findByName("vegan");
    	Date deliveryTime = new Date();
    	// Expecting to get exception with proper message.
    	Throwable throwable = Assertions.assertThrows(Exception.class, () -> {
    		Delivery delivery = waltService.createOrderAndAssignDriver(customer, restaurant, deliveryTime);
    	});
    	assertEquals(Consts.NOT_SAME_CITY_MESSAGE, throwable.getMessage());
    }
    
    @Test
    public void testCreateOneOrderAndAssignDriverCustomerNotFound() {
    	// Invalid customer, valid restaurant and delivery time.
    	Customer customer = new Customer("Alon", new City("tlv"), "address");
    	Restaurant restaurant = restaurantRepository.findByName("vegan");
    	Date deliveryTime = new Date();
    	// Expecting to get exception with proper message. 
    	Throwable throwable = Assertions.assertThrows(Exception.class, () -> {
    		Delivery delivery = waltService.createOrderAndAssignDriver(customer, restaurant, deliveryTime);
    	});
    	assertEquals(Consts.CUSTOMER_NOT_FOUND_MESSAGE, throwable.getMessage());
    }
    
    @Test
    public void testCreateOneOrderAndAssignDriverCityWithoutDrivers() {
    	// Create new valid city, customer and restaurant, with valid delivery time.
    	City city = new City("rg");
    	cityRepository.save(city);
    	Customer customer = new Customer("Alon", city, "address");
    	customerRepository.save(customer);
    	Restaurant restaurant = new Restaurant("pizza", city, "other address");
    	restaurantRepository.save(restaurant);
    	Date deliveryTime = new Date();
    	// Expecting to get exception with proper message.
    	Throwable throwable = Assertions.assertThrows(Exception.class, () -> {
    		Delivery delivery = waltService.createOrderAndAssignDriver(customer, restaurant, deliveryTime);
    	});
    	assertEquals(Consts.NO_DRIVERS_MESSAGE, throwable.getMessage());
    }
    
    @Test
    public void testCreateFourOrdersAndAssignDriversNoDriverAvailable() {
    	// Create 3 new deliveries with different drivers and save them to the database.
    	Restaurant restaurant = restaurantRepository.findByName("meat");
    	Customer customer = customerRepository.findByName("Mozart");
    	Date deliveryTime = new Date();
    	
    	Driver driver1 = driverRepository.findByName("Robert");
    	Delivery delivery1 = new Delivery(driver1, restaurant, customer, deliveryTime);
    	
    	Driver driver2 = driverRepository.findByName("David");
    	Delivery delivery2 = new Delivery(driver2, restaurant, customer, deliveryTime);
    	
    	Driver driver3 = driverRepository.findByName("Neta");
    	Delivery delivery3 = new Delivery(driver3, restaurant, customer, deliveryTime);
    	
    	deliveryRepository.saveAll(Lists.newArrayList(delivery1, delivery2, delivery3));
    	
    	// Create new delivery at the same delivery time.
    	// Expecting to get exception with proper message.
    	Throwable throwable = Assertions.assertThrows(Exception.class, () -> {
    		Delivery delivery = waltService.createOrderAndAssignDriver(customer, restaurant, deliveryTime);
    	});
    	assertEquals(Consts.NO_DRIVERS_AVAILABLE_MESSAGE, throwable.getMessage());
    }
    
    @Test
    public void testCreateFourOrdersAndAssignDriversDifferentValidDeliveryTime() {
    	// Create 3 new deliveries with different drivers and save them to the database.
    	Restaurant restaurant = restaurantRepository.findByName("meat");
    	Customer customer = customerRepository.findByName("Mozart");
    	Date deliveryTime = new Date();
    	
    	Driver driver1 = driverRepository.findByName("Robert");
    	Delivery delivery1 = new Delivery(driver1, restaurant, customer, deliveryTime);
    	
    	Driver driver2 = driverRepository.findByName("David");
    	Delivery delivery2 = new Delivery(driver2, restaurant, customer, deliveryTime);
    	
    	Driver driver3 = driverRepository.findByName("Neta");
    	Delivery delivery3 = new Delivery(driver3, restaurant, customer, deliveryTime);
    	
    	deliveryRepository.saveAll(Lists.newArrayList(delivery1, delivery2, delivery3));
    	
    	Date newDeliveryTime = new Date(deliveryTime.getTime() + 3600000);
    	
    	// Create new delivery with valid delivery time.
    	// Expecting to get delivery object that saved to the database.
    	Delivery delivery = waltService.createOrderAndAssignDriver(customer, restaurant, newDeliveryTime);
    	assertNotNull(delivery);
    	assertEquals(customer.getCity().getName(), delivery.getDriver().getCity().getName());
    }
    
    @Test
    public void testCreateFourOrdersAndAssignDriversDifferentInvalidDeliveryTime() {
    	// Create 3 new deliveries with different drivers and save them to the database.
    	Restaurant restaurant = restaurantRepository.findByName("meat");
    	Customer customer = customerRepository.findByName("Mozart");
    	Date deliveryTime = new Date();
    	
    	Driver driver1 = driverRepository.findByName("Robert");
    	Delivery delivery1 = new Delivery(driver1, restaurant, customer, deliveryTime);
    	
    	Driver driver2 = driverRepository.findByName("David");
    	Delivery delivery2 = new Delivery(driver2, restaurant, customer, deliveryTime);
    	
    	Driver driver3 = driverRepository.findByName("Neta");
    	Delivery delivery3 = new Delivery(driver3, restaurant, customer, deliveryTime);
    	
    	deliveryRepository.saveAll(Lists.newArrayList(delivery1, delivery2, delivery3));
    	
    	// Create new delivery with invalid delivery time.
    	// Expecting to get exception with proper message.
    	Date newDeliveryTime = new Date(deliveryTime.getTime() + 3000000);
    	Throwable throwable = Assertions.assertThrows(Exception.class, () -> {
    		Delivery delivery = waltService.createOrderAndAssignDriver(customer, restaurant, newDeliveryTime);
    	});
    	assertEquals(Consts.NO_DRIVERS_AVAILABLE_MESSAGE, throwable.getMessage());
    }
    
    @Test
    public void testCreateSevenOrdersAndAssignDriverTheLeastBusyDriver() {
    	// Create 6 new deliveries with 3 drivers and 3 delivery times and save them to the database.
    	Restaurant restaurant = restaurantRepository.findByName("meat");
    	Customer customer = customerRepository.findByName("Mozart");
    	Date deliveryTime1 = new Date();
    	Date deliveryTime2 = new Date(deliveryTime1.getTime() + 3600000);
    	Date deliveryTime3 = new Date(deliveryTime2.getTime() + 3600000);
    	
    	Driver driver1 = driverRepository.findByName("Robert");
    	Driver driver2 = driverRepository.findByName("David");
    	Driver driver3 = driverRepository.findByName("Neta");
    	
    	Delivery delivery1 = new Delivery(driver1, restaurant, customer, deliveryTime1);
    	Delivery delivery2 = new Delivery(driver1, restaurant, customer, deliveryTime2);
    	Delivery delivery3 = new Delivery(driver1, restaurant, customer, deliveryTime3);
    	Delivery delivery4 = new Delivery(driver2, restaurant, customer, deliveryTime1);
    	Delivery delivery5 = new Delivery(driver2, restaurant, customer, deliveryTime2);
    	Delivery delivery6 = new Delivery(driver3, restaurant, customer, deliveryTime1);
    	
    	deliveryRepository.saveAll(Lists.newArrayList(delivery1, delivery2, delivery3, delivery4, delivery5, delivery6));
    	
    	// Create new delivery with valid delivery time.
    	// Expecting to get delivery object that saved to the database.
    	Date deliveryTime4 = new Date(deliveryTime3.getTime() + 3600000);
    	Delivery delivery = waltService.createOrderAndAssignDriver(customer, restaurant, deliveryTime4);
    	assertNotNull(delivery);
    	assertEquals(driver3.getId(), delivery.getDriver().getId());
    }
    
    @Test
    public void testGetDriverRankReport() {
    	// Create 6 deliveries with 3 drivers and 3 delivery times and save them to the database.
    	setDeliveriesForRankReportTest();
    	
    	// Get rank report.
    	List<DriverDistance> rankReport = deliveryRepository.getDriverRankReportByDriver();
    	
    	// Print the report to the console.
    	for(DriverDistance driverDistance : rankReport) {
    		Driver driver = driverDistance.getDriver();
    		Long totalDistance = driverDistance.getTotalDistance();
    		System.out.println("The driver: " + driver.getName() + " drove " + totalDistance + "KM.");
    	}
    	
    	// Expecting that every object value be bigger then the object value after him.
    	for(int i = 1; i < rankReport.size(); i++) {
    		Long totalDistance1 = rankReport.get(i - 1).getTotalDistance();
    		Long totalDistance2 = rankReport.get(i).getTotalDistance();
    		assertTrue(totalDistance1 >= totalDistance2);
    	}
    }
    
    @Test
    public void testGetDriverRankReportByCity() {
    	// Create 6 deliveries with 3 drivers and 3 delivery times and save them to the database.
    	setDeliveriesForRankReportTest();
    	
    	City city = cityRepository.findByName("Tel-Aviv");
    	
    	// Get rank report of Tel-Aviv.
    	List<DriverDistance> rankReportByCity = deliveryRepository.getCityDriversRankReportByDriver(city);
    	
    	// Print the report to the console.
    	// Expecting that every object city will be Tel-Aviv.
    	for(DriverDistance driverDistance : rankReportByCity) {
    		Driver driver = driverDistance.getDriver();
    		Long totalDistance = driverDistance.getTotalDistance();
    		System.out.println("The driver: " + driver.getName() + " drove " + totalDistance + "KM.");
    		assertEquals("Tel-Aviv", driver.getCity().getName());
    	}
    	
    	// Expecting that every object value be bigger then the object value after him.
    	for(int i = 1; i < rankReportByCity.size(); i++) {
    		Long totalDistance1 = rankReportByCity.get(i - 1).getTotalDistance();
    		Long totalDistance2 = rankReportByCity.get(i).getTotalDistance();
    		assertTrue(totalDistance1 >= totalDistance2);
    	}
    }
    
    private void setDeliveriesForRankReportTest() {
    	Driver driver1 = driverRepository.findByName("Mary"); //tlv
    	Driver driver2 = driverRepository.findByName("Patricia"); //tlv
    	Driver driver3 = driverRepository.findByName("Neta"); //jeru
    	
    	Restaurant restaurant1 = restaurantRepository.findByName("cafe"); //tlv
    	Restaurant restaurant2 = restaurantRepository.findByName("vegan"); //tlv
    	Restaurant restaurant3 = restaurantRepository.findByName("meat"); //jeru
    	
    	Customer customer1 = customerRepository.findByName("Beethoven"); //tlv
    	Customer customer2 = customerRepository.findByName("Bach"); //tlv
    	Customer customer3 = customerRepository.findByName("Mozart"); //jeru
    	
    	Date deliveryTime1 = new Date();
    	Date deliveryTime2 = new Date(deliveryTime1.getTime() + 3600000);
    	Date deliveryTime3 = new Date(deliveryTime2.getTime() + 3600000);
    	
    	Delivery delivery1 = new Delivery(driver1, restaurant1, customer1, deliveryTime1);
    	Delivery delivery2 = new Delivery(driver1, restaurant1, customer2, deliveryTime2);
    	Delivery delivery3 = new Delivery(driver1, restaurant2, customer1, deliveryTime3);
    	Delivery delivery4 = new Delivery(driver2, restaurant1, customer2, deliveryTime1);
    	Delivery delivery5 = new Delivery(driver2, restaurant2, customer1, deliveryTime2);
    	Delivery delivery6 = new Delivery(driver3, restaurant3, customer3, deliveryTime1);
    	
    	deliveryRepository.saveAll(Lists.newArrayList(delivery1, delivery2, delivery3, delivery4, delivery5, delivery6));
    }
    
    
}
