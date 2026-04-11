package de.jansoh.rsistrategy.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.jansoh.rsistrategy.model.Order;
import de.jansoh.rsistrategy.model.OrderUpdateEventMapper;
import de.jansoh.rsistrategy.model.Position;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

class OpenPositionRegistryTest {

    OpenPositionRegistry positionRegistry = new OpenPositionRegistry();
    ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void update() throws JsonProcessingException {

        Map<String, Object> posMap = objectMapper.readValue(
                "{\n" +
                        "  \"e\": \"ORDER_TRADE_UPDATE\",\n" +
                        "  \"T\": 1775640315975,\n" +
                        "  \"E\": 1775640316010,\n" +
                        "  \"o\": {\n" +
                        "    \"s\": \"BTCUSDT\",\n" +
                        "    \"c\": \"web_usdt_fhjsms76loeommpxkwufqzu\",\n" +
                        "    \"S\": \"BUY\",\n" +
                        "    \"o\": \"MARKET\",\n" +
                        "    \"f\": \"GTC\",\n" +
                        "    \"q\": 0.01,\n" +
                        "    \"p\": 0,\n" +
                        "    \"ap\": 71795.2,\n" +
                        "    \"sp\": 0,\n" +
                        "    \"x\": \"TRADE\",\n" +
                        "    \"X\": \"FILLED\",\n" +
                        "    \"i\": 13024645552,\n" +
                        "    \"l\": 0.0009,\n" +
                        "    \"z\": 0.0069,\n" +
                        "    \"L\": 71795.2,\n" +
                        "    \"n\": 0.02584627,\n" +
                        "    \"N\": \"USDT\",\n" +
                        "    \"T\": 1775640315975,\n" +
                        "    \"t\": 473102755,\n" +
                        "    \"b\": 0,\n" +
                        "    \"a\": 0,\n" +
                        "    \"m\": \"false\",\n" +
                        "    \"R\": \"false\",\n" +
                        "    \"wt\": \"CONTRACT_PRICE\",\n" +
                        "    \"ot\": \"MARKET\",\n" +
                        "    \"ps\": \"BOTH\",\n" +
                        "    \"cp\": \"false\",\n" +
                        "    \"rp\": 0,\n" +
                        "    \"pP\": \"false\",\n" +
                        "    \"si\": 0,\n" +
                        "    \"ss\": 0,\n" +
                        "    \"V\": \"EXPIRE_MAKER\",\n" +
                        "    \"pm\": \"NONE\",\n" +
                        "    \"gtd\": 0,\n" +
                        "    \"er\": 0\n" +
                        "  }\n" +
                        "}",
                Map.class
        );
        Map<String, Object> slMap = objectMapper.readValue(
                "{\n" +
                        "  \"e\": \"ORDER_TRADE_UPDATE\",\n" +
                        "  \"T\": 1775847770077,\n" +
                        "  \"E\": 1775847770077,\n" +
                        "  \"o\": {\n" +
                        "    \"s\": \"BTCUSDT\",\n" +
                        "    \"c\": \"SdR9gf8MlO9rPGIlVpAjNW\",\n" +
                        "    \"S\": \"SELL\",\n" +
                        "    \"o\": \"MARKET\",\n" +
                        "    \"f\": \"GTC\",\n" +
                        "    \"q\": \"0.01\",\n" +
                        "    \"p\": \"0\",\n" +
                        "    \"ap\": \"73007.9\",\n" +
                        "    \"sp\": \"0\",\n" +
                        "    \"x\": \"TRADE\",\n" +
                        "    \"X\": \"FILLED\",\n" +
                        "    \"i\": 13027274192,\n" +
                        "    \"l\": \"0.0035\",\n" +
                        "    \"z\": \"0.0035\",\n" +
                        "    \"L\": \"73007.9\",\n" +
                        "    \"n\": \"0.10221106\",\n" +
                        "    \"N\": \"USDT\",\n" +
                        "    \"T\": 1775847770077,\n" +
                        "    \"t\": 473964667,\n" +
                        "    \"b\": \"0\",\n" +
                        "    \"a\": \"0\",\n" +
                        "    \"m\": false,\n" +
                        "    \"R\": true,\n" +
                        "    \"wt\": \"MARK_PRICE\",\n" +
                        "    \"ot\": \"MARKET\",\n" +
                        "    \"ps\": \"BOTH\",\n" +
                        "    \"cp\": true,\n" +
                        "    \"rp\": \"0.180075\",\n" +
                        "    \"pP\": false,\n" +
                        "    \"si\": 1000000046042621,\n" +
                        "    \"ss\": -1,\n" +
                        "    \"st\": \"ALGO_CONDITION\",\n" +
                        "    \"V\": \"EXPIRE_MAKER\",\n" +
                        "    \"pm\": \"NONE\",\n" +
                        "    \"gtd\": 0,\n" +
                        "    \"er\": \"0\"\n" +
                        "  }\n" +
                        "}",
                Map.class
        );
        Order posOrder = new OrderUpdateEventMapper().map(posMap);
        Order slOrder = new OrderUpdateEventMapper().map(slMap);
        positionRegistry.update(posOrder);
        Optional<Position> pos = positionRegistry.update(slOrder);

        Assertions.assertTrue(pos.isPresent());
    }
}