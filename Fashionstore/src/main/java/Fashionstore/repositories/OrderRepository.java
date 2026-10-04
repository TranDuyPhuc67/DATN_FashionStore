package Fashionstore.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import Fashionstore.entities.Order;

public interface OrderRepository extends JpaRepository<Order, String> {

	@Transactional
	@Modifying
	@Query("""
	UPDATE Order o
	SET o.status = 1
	WHERE o.orderId = :orderId
	AND o.status = 0
	""")
	int markPaid(@Param("orderId") String orderId);
}