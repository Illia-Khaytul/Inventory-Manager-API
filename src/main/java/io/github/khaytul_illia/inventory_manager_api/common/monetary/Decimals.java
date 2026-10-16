package io.github.khaytul_illia.inventory_manager_api.common.monetary;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Decimals {

    public static final int SCALE = 2;
    public static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    public static BigDecimal of(BigDecimal value){
        return value.setScale(SCALE, ROUNDING_MODE);
    }

    public static BigDecimal of(String value){
        return of(new BigDecimal(value));
    }

    public static BigDecimal of(int value){
        return of(new BigDecimal(value));
    }

    public static BigDecimal of(double value){
        return of(new BigDecimal(value));
    }

}
