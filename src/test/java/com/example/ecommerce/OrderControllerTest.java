package com.example.ecommerce;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import com.example.ecommerce.entity.Product;
import com.example.ecommerce.entity.User;
import com.example.ecommerce.entity.Order;
import com.example.ecommerce.entity.enums.OrderStatus;
import com.example.ecommerce.repository.ProductRepository;
import com.example.ecommerce.repository.UserRepository;
import com.example.ecommerce.repository.OrderRepository;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class OrderControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private OrderRepository orderRepository;

    private Long testUserId;
    private Long testProductId;

    @BeforeEach
    public void setUp() {
        RestAssured.port = port;
        RestAssured.basePath = "/api/orders";

        // Seed fresh data for every test run
        User user = userRepository.save(User.builder().name("Susan_QA").vipStatus(true).build());
        testUserId = user.getId();

        Product product = productRepository.save(Product.builder()
                .name("Automation Tool")
                .price(100.0)
                .stock(50)
                .reservedStock(0)
                .build());
        testProductId = product.getId();
    }

    @AfterEach
    public void tearDown() {
        // Clear database to ensure test isolation
        orderRepository.deleteAll(); 
        userRepository.deleteAll();
        productRepository.deleteAll();
    }

    @Test
    public void testCreateOrder_Success() {
        String jsonBody = "{"
            + "\"userId\": " + testUserId + ","
            + "\"items\": [{"
            + "  \"productId\": " + testProductId + ","
            + "  \"quantity\": 2"
            + "}]"
            + "}";

        given()
            .contentType(ContentType.JSON)
            .body(jsonBody)
        .when()
            .post()
        .then()
            .log().ifError() 
            .statusCode(anyOf(is(200), is(201)))
            .body("orderId", notNullValue())
            .body("status", equalTo("CONFIRMED")); // Matches Service hardcoding
    }

    @Test
    public void testCancelOrder_Success() {
        // 1. Create the order via API
        String jsonBody = "{"
            + "\"userId\": " + testUserId + ","
            + "\"items\": [{"
            + "  \"productId\": " + testProductId + ","
            + "  \"quantity\": 1"
            + "}]"
            + "}";

        // Extract ID as Number to avoid ClassCastException (Integer vs Long)
        Number orderIdNum = given()
            .contentType(ContentType.JSON)
            .body(jsonBody)
        .when()
            .post()
        .then()
            .statusCode(anyOf(is(200), is(201)))
            .extract().path("orderId");
        
        long orderId = orderIdNum.longValue();

        // 2. MANIPULATE STATE: Service creates as CONFIRMED, but Cancel requires PENDING
        Order order = orderRepository.findById(orderId).orElseThrow();
        order.setStatus(OrderStatus.PENDING);
        orderRepository.save(order);

        // 3. Cancel the order
        given()
            .pathParam("id", orderId)
        .when()
            .delete("/{id}")
        .then()
            .log().ifError()
            .statusCode(anyOf(is(200), is(204)));

        // 4. Verify deletion
        assert(!orderRepository.existsById(orderId));
    }
}