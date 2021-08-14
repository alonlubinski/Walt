package com.walt.dao;

import com.walt.model.City;
import com.walt.model.Driver;
import com.walt.model.Delivery;
import com.walt.model.DriverDistance;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeliveryRepository extends CrudRepository<Delivery, Long> {

	List<Delivery> findAllDeliveriesByDriver(@Param("driver") Driver driver);
	
	@Query("SELECT del.driver AS driver, SUM(del.distance) AS totalDistance FROM Delivery del GROUP BY del.driver ORDER BY totalDistance DESC")
	List<DriverDistance> getDriverRankReportByDriver();
	
	@Query("SELECT del.driver AS driver, SUM(del.distance) AS totalDistance FROM Delivery del WHERE del.driver.city =:city GROUP BY del.driver ORDER BY totalDistance DESC")
	List<DriverDistance> getCityDriversRankReportByDriver(@Param("city") City city);
}


