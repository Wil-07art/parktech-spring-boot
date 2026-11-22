package com.parktech;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Clase principal de ParkTech.
 * Al ejecutarla, Spring Boot arranca el servidor Tomcat embebido en el puerto 8080,
 * se conecta a MySQL, crea las tablas y sirve el frontend que está en /static.
 */
@SpringBootApplication
public class ParktechApplication {

    public static void main(String[] args) {
        SpringApplication.run(ParktechApplication.class, args);
    }
}
