package de.jansoh.rsistrategy.service.order;

import de.jansoh.rsistrategy.model.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;

@Component
public class OrderUpdateEventMapper {

    public Order map(Map<String, Object> eventData) {

        Map<String, Object> o = (Map<String, Object>) eventData.get("o");

        String clientOrderId = o.get("c").toString();
        String symbol = o.get("s").toString();
        OrderSide orderSide = OrderSide.valueOf(o.get("S").toString());
        PositionSide positionSide = PositionSide.valueOf(o.get("ps").toString());
        OrderType orderType = OrderType.valueOf(o.get("o").toString());
        BigDecimal originalQuantity = new BigDecimal(o.get("q").toString());
        BigDecimal lastFilledQuantity = new BigDecimal(o.get("l").toString());
        BigDecimal filledAccumulatedQuantity = new BigDecimal(o.get("z").toString());
        BigDecimal originalPrice = new BigDecimal(o.get("p").toString());
        BigDecimal averagePrice = new BigDecimal(o.get("ap").toString());
        BigDecimal lastFilledPrice = new BigDecimal(o.get("L").toString());
        BigDecimal realizedProfit = new BigDecimal(o.get("rp").toString());
        String commissionAsset = null;
        if (o.containsKey("N") && null != o.get("N")) {
            commissionAsset = o.get("N").toString();
        }
        BigDecimal commission = null;
        if (o.containsKey("n") && null != o.get("n") && !o.get("n").toString().matches("\\d+\\.\\d+")) {
            commission = new BigDecimal(o.get("n").toString());
        }
        OrderStatus orderStatus = OrderStatus.valueOf(o.get("X").toString());
        String orderId = o.get("i").toString();
        Long ott = (Long) o.get("T");
        ZonedDateTime orderTradeTime = ZonedDateTime.ofInstant(
                Instant.ofEpochMilli(ott),
                ZoneId.systemDefault()
        );

        return Order.builder()
                .symbol(symbol)
                .side(orderSide)
                .positionSide(positionSide)
                .type(orderType)
                .originalQuantity(originalQuantity)
                .lastFilledQuantity(lastFilledQuantity)
                .filledAccumulatedQuantity(filledAccumulatedQuantity)
                .originalPrice(originalPrice)
                .averagePrice(averagePrice)
                .lastFilledPrice(lastFilledPrice)
                .realizedProfit(realizedProfit)
                .commissionAsset(commissionAsset)
                .commission(commission)
                .orderStatus(orderStatus)
                .orderId(orderId)
                .orderTradeTime(orderTradeTime)
                .clientOrderId(clientOrderId)
                .build();
    }
}
