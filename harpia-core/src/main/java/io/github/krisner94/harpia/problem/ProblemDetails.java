package io.github.krisner94.harpia.problem;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Machine-readable error description following RFC 9457 (Problem Details for
 * HTTP APIs).
 *
 * <p>It implements {@link Problem}, so it can be used directly as an error
 * type of a {@link Result}.</p>
 *
 * @param type a URI reference identifying the problem type (may be {@code null})
 * @param title short, human-readable summary
 * @param status HTTP status code
 * @param detail human-readable explanation (may be {@code null})
 * @param instance URI reference identifying the occurrence (may be {@code null})
 * @param extensions additional members, e.g. {@code errors} for validation
 */
public record ProblemDetails(
        URI type,
        String title,
        int status,
        String detail,
        URI instance,
        Map<String, Object> extensions) implements Problem {

    public ProblemDetails {
        if (status < 100 || status > 599) {
            throw new IllegalArgumentException("status must be a valid HTTP status: " + status);
        }
        extensions = extensions == null ? Map.of() : Map.copyOf(extensions);
    }

    @Override
    public ProblemDetails toProblemDetails() {
        return this;
    }

    /**
     * Creates a problem with only status and title.
     */
    public static ProblemDetails of(int status, String title) {
        return new ProblemDetails(null, title, status, null, null, Map.of());
    }

    /**
     * Creates a problem with status, title and detail.
     */
    public static ProblemDetails of(int status, String title, String detail) {
        return new ProblemDetails(null, title, status, detail, null, Map.of());
    }

    /**
     * Starts a builder anchored on status and title.
     */
    public static Builder builder(int status, String title) {
        return new Builder(status, title);
    }

    /**
     * Builder for {@link ProblemDetails}.
     */
    public static final class Builder {

        private final int status;
        private final String title;
        private URI type;
        private String detail;
        private URI instance;
        private final Map<String, Object> extensions = new LinkedHashMap<>();

        private Builder(int status, String title) {
            this.status = status;
            this.title = title;
        }

        public Builder type(URI type) {
            this.type = type;
            return this;
        }

        public Builder detail(String detail) {
            this.detail = detail;
            return this;
        }

        public Builder instance(URI instance) {
            this.instance = instance;
            return this;
        }

        public Builder extension(String key, Object value) {
            this.extensions.put(key, value);
            return this;
        }

        public ProblemDetails build() {
            return new ProblemDetails(type, title, status, detail, instance, extensions);
        }
    }
}
