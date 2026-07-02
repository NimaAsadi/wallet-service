package ir.ebb.external.rayan.wallet.transformer;

import ir.ebb.external.rayan.wallet.dto.RayanWalletDTO;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

import static java.lang.Long.parseLong;

@Slf4j
public final class RayanWalletResponseTransformer {

    private RayanWalletResponseTransformer() {
        throw new IllegalStateException();
    }

    public static Map<Long, RayanWalletDTO> toDtoMap(String value) {
        Map<Long, RayanWalletDTO> result = new HashMap<>();
        value.lines().skip(1).forEach(line -> {
            try {
                String[] parts = line.split(",");
                RayanWalletDTO dto = new RayanWalletDTO(
                        parseLong(parts[0].trim()),
                        parts[1].trim(),
                        parseLong(parts[2].trim()),
                        parseLong(parts[3].trim()),
                        parseLong(parts[4].trim()),
                        parseLong(parts[5].trim()),
                        parseLong(parts[6].trim()),
                        parseLong(parts[7].trim()),
                        parseLong(parts[8].trim()),
                        parseLong(parts[9].trim()),
                        parseLong(parts[10].trim()),
                        parseLong(parts[11].trim()),
                        parseLong(parts[12].trim())
                );
                result.put(dto.accountNumber(), dto);
            } catch (Exception e) {
                log.atError().log("Error parsing rayan wallet record: {}", line, e);
            }
        });
        return result;
    }
}
