
import com.adobe.campaign.tests.service.JavaCalls;
import com.fasterxml.jackson.databind.ObjectMapper;

import static spark.Spark.*;

public class Main {
    public static void main(String[] args) {
        get("/hello", (req, res) -> "Hello World");
        post("/call", (req, res) -> {
            ObjectMapper mapper = new ObjectMapper();
            JavaCalls fetchedFromJSON = mapper.readValue(req.body(), JavaCalls.class);
            //return fetchedFromJSON.submitCalls().toString();
            //return req.body();
            return mapper.writeValueAsString(fetchedFromJSON.submitCalls());
        });
    }
}
