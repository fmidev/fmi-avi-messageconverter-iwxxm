package fi.fmi.avi.converter.iwxxm.generic;

import fi.fmi.avi.model.MessageType;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum IWXXMMessageType {
    TAF("TAF", MessageType.TAF),
    METAR("METAR", MessageType.METAR),
    SPECI("SPECI", MessageType.SPECI),
    SIGMET("SIGMET", MessageType.SIGMET),
    TROPICAL_CYCLONE_SIGMET("TropicalCycloneSIGMET", MessageType.SIGMET),
    VOLCANIC_ASH_SIGMET("VolcanicAshSIGMET", MessageType.SIGMET),
    AIRMET("AIRMET", MessageType.AIRMET),
    SPACE_WEATHER_ADVISORY("SpaceWeatherAdvisory", MessageType.SPACE_WEATHER_ADVISORY),
    VOLCANIC_ASH_ADVISORY("VolcanicAshAdvisory", MessageType.VOLCANIC_ASH_ADVISORY),
    TROPICAL_CYCLONE_ADVISORY("TropicalCycloneAdvisory", MessageType.TROPICAL_CYCLONE_ADVISORY),
    ;

    private static final Map<String, IWXXMMessageType> VALUES_BY_MESSGE_ELEMENT_NAME = Collections.unmodifiableMap(Arrays.stream(values())
            .collect(Collectors.toMap(IWXXMMessageType::getMessageElementName, Function.identity())));

    private final String messageElementName;
    private final MessageType messageType;

    IWXXMMessageType(final String messageElementName, final MessageType messageType) {
        this.messageElementName = messageElementName;
        this.messageType = messageType;
    }

    public static Optional<IWXXMMessageType> fromMessageElementName(@Nullable final String messageElementName) {
        return Optional.ofNullable(VALUES_BY_MESSGE_ELEMENT_NAME.get(messageElementName));
    }

    public String getMessageElementName() {
        return messageElementName;
    }

    public MessageType getMessageType() {
        return messageType;
    }
}
