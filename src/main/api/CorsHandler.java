package main.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;

/**
 * CORS (Cross-Origin Resource Sharing) handler for preflight requests
 */
public class CorsHandler implements HttpHandler {
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // Handle OPTIONS preflight request
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");
            exchange.getResponseHeaders().add("Access-Control-Max-Age", "3600");
            exchange.sendResponseHeaders(204, -1);
        } else {
            // For non-OPTIONS requests, return 404
            ApiResponse.sendError(exchange, 404, "Endpoint not found");
        }
    }
}
