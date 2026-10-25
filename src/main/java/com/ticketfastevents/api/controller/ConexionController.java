
package com.ticketfastevents.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@Tag(name = "0. Diagnóstico y Salud", description = "Verificación pública del estado de las conexiones NoSQL")
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

    @Operation(summary = "Comprobar conexiones a MongoDB y Redis (Público)")
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
