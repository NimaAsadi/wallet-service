package ir.ebb.base.constant;

import ir.ebb.base.dto.FixedConstantResponse;

public final class ConstantTransformer {

    private ConstantTransformer() {}

    public static FixedConstantResponse enumToFixedConstantResponse(Enum<?> enumValue) {
        return new FixedConstantResponse(enumValue.name(), enumValue.name());
    }
}
