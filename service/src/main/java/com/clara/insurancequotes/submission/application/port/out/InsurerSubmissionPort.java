package com.clara.insurancequotes.submission.application.port.out;

import java.util.UUID;

/** Outbound port for submitting a quote to the insurer. */
public interface InsurerSubmissionPort {

    void submit(UUID quoteId);
}
