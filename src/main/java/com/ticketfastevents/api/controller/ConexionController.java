
package com.ticketfastevents.api.controller;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class ConexionController {

    private final MongoTemplate mongoTemplate;
    private final RedisConnectionFactory redisConnectionFactory;

    public ConexionController(
            MongoTemplate mongoTemplate,
            RedisConnectionFactory redisConnectionFactory) {
        this.mongoTemplate = mongoTemplate;
        this.redisConnectionFactory = redisConnectionFactory;
    }

    @GetMapping("/api/health/conexiones")
    public Map<String, String> comprobarConexiones() {
        Map<String, String> resultado = new LinkedHashMap<>();

        try {
            mongoTemplate.executeCommand(
                    new org.bson.Document("ping", 1)
            );
            resultado.put("mongodb", "conectado");
        } catch (Exception e) {
            resultado.put("mongodb", "error de conexión");
        }

        try (RedisConnection conexion =
                     redisConnectionFactory.getConnection()) {
            conexion.ping();
            resultado.put("redis", "conectado");
        } catch (Exception e) {
            resultado.put("redis", "error de conexión");
        }

        return resultado;
    }
}
