package com.clara.insurancequotes.quote.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.clara.insurancequotes.quote.api.model.HealthCondition;
import java.util.Set;
import org.junit.jupiter.api.Test;

class HealthProfileTest {

    @Test
    void none_hasNoHealthData() {
        var profile = HealthProfile.none();

        assertThat(profile.isPresent()).isFalse();
        assertThat(profile.conditions()).isNull();
    }

    @Test
    void selectedHealthData_exposesBusinessFlags() {
        var profile = new HealthProfile(true, Set.of(HealthCondition.DIABETES), false, true, true);

        assertThat(profile.isPresent()).isTrue();
        assertThat(profile.anyConditionSelected()).isTrue();
        assertThat(profile.tobaccoUse()).isTrue();
        assertThat(profile.spouseCoverage()).isTrue();
    }
}
