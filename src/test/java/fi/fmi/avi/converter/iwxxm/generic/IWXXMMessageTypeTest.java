package fi.fmi.avi.converter.iwxxm.generic;

import org.junit.Test;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

public class IWXXMMessageTypeTest {
    @Test
    public void messageElementNamesAreNotEmpty() {
        assertThat(IWXXMMessageType.values())
                .allSatisfy(iwxxmMessageType -> assertThat(iwxxmMessageType.getMessageElementName())
                        .isNotNull()
                        .isNotEmpty());
    }

    @Test
    public void nameFollowMessageElementNames() {
        assertThat(IWXXMMessageType.values())
                .allSatisfy(iwxxmMessageType -> assertThat(iwxxmMessageType.getMessageElementName().toUpperCase(Locale.ROOT))
                        .isEqualTo(iwxxmMessageType.name().replaceAll("_", "")));
    }

    @Test
    public void nameContainsMessageTypeName() {
        assertThat(IWXXMMessageType.values())
                .allSatisfy(iwxxmMessageType -> assertThat(iwxxmMessageType.name())
                        .contains(iwxxmMessageType.getMessageType().name()));
    }
}
