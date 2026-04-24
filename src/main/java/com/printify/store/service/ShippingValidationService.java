package com.printify.store.service;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.printify.store.dto.order.CheckoutRequest;
import com.printify.store.exception.BadRequestException;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class ShippingValidationService {

    /*
     * Keep only countries you actually want to sell to.
     * Do not allow all Printify countries unless your frontend, tax, shipping,
     * support, and refund policy are ready for them.
     */
    private static final Set<String> ALLOWED_COUNTRIES = Set.of(
            "US", "IN", "AU", "DE", "FR"
    );

    /*
     * Printify says providers ship to most countries except some restricted countries.
     * Keep this blocklist even if someone manipulates frontend country value.
     */
    private static final Set<String> BLOCKED_COUNTRIES = Set.of(
            "KP", "RU", "BY", "UA"
    );

    private static final Set<String> FAKE_WORDS = Set.of(
            "test", "testing", "asdf", "qwerty", "dummy", "fake",
            "random", "xyz", "abc", "none", "null", "na", "n/a",
            "sample", "demo", "unknown", "address", "user", "admin"
    );

    private static final Pattern EMAIL =
            Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE);

    private static final Pattern FULL_NAME =
            Pattern.compile("^[\\p{L}][\\p{L}\\p{M} .'-]{1,69}$");

    private static final Pattern CITY_STATE =
            Pattern.compile("^[\\p{L}][\\p{L}\\p{M} .'-]{1,69}$");

    private static final Pattern ADDRESS =
            Pattern.compile("^[\\p{L}\\p{N}\\p{M}\\s,.'#\\-/()&]{6,120}$");

    private static final Map<String, Pattern> POSTAL_PATTERNS = Map.of(
            "US", Pattern.compile("^\\d{5}(-\\d{4})?$"),
            "IN", Pattern.compile("^[1-9]\\d{5}$"),
            "AU", Pattern.compile("^\\d{4}$"),
            "DE", Pattern.compile("^\\d{5}$"),
            "FR", Pattern.compile("^\\d{5}$")
    );

    private static final PhoneNumberUtil PHONE_UTIL = PhoneNumberUtil.getInstance();

    public void validate(CheckoutRequest request) {
        if (request == null) {
            throw new BadRequestException("Checkout request is required.");
        }

        String country = normalizeCountry(request.getCountry());

        if (BLOCKED_COUNTRIES.contains(country)) {
            throw new BadRequestException("Shipping is not available for this country.");
        }

        if (!ALLOWED_COUNTRIES.contains(country)) {
            throw new BadRequestException("Shipping is not available for this country.");
        }

        validateFullName(request.getFullName());
        validateEmail(request.getEmail());
        validatePhone(request.getPhone(), country);
        validateAddressLine1(request.getAddressLine1());
        validateAddressLine2(request.getAddressLine2());
        validateCity(request.getCity());
        validateState(request.getState());
        validatePostalCode(request.getPostalCode(), country);

        rejectFakeText(request.getFullName(), "Full name");
        rejectFakeText(request.getAddressLine1(), "Address line 1");
        rejectFakeText(request.getCity(), "City");
        rejectFakeText(request.getState(), "State / Region");

        rejectRepeatedPattern(request.getFullName(), "Full name");
        rejectRepeatedPattern(request.getAddressLine1(), "Address line 1");
        rejectRepeatedPattern(request.getCity(), "City");
        rejectRepeatedPattern(request.getState(), "State / Region");
    }

    private void validateFullName(String value) {
        String text = clean(value);

        if (text.isBlank()) {
            throw new BadRequestException("Full name is required.");
        }

        if (text.length() < 3 || text.length() > 70) {
            throw new BadRequestException("Full name must be between 3 and 70 characters.");
        }

        if (!FULL_NAME.matcher(text).matches()) {
            throw new BadRequestException("Full name contains invalid characters.");
        }

        if (text.trim().split("\\s+").length < 2) {
            throw new BadRequestException("Please enter first and last name.");
        }
    }

    private void validateEmail(String value) {
        String email = clean(value).toLowerCase(Locale.ROOT);

        if (email.isBlank()) {
            throw new BadRequestException("Email is required.");
        }

        if (email.length() > 100 || !EMAIL.matcher(email).matches()) {
            throw new BadRequestException("Email address is invalid.");
        }
    }

    private void validatePhone(String value, String country) {
        String phone = clean(value);

        if (phone.isBlank()) {
            throw new BadRequestException("Phone number is required.");
        }

        try {
            var parsed = PHONE_UTIL.parse(phone, country);

            if (!PHONE_UTIL.isValidNumberForRegion(parsed, country)) {
                throw new BadRequestException("Phone number is invalid for selected country.");
            }
        } catch (NumberParseException ex) {
            throw new BadRequestException("Phone number is invalid.");
        }
    }

    private void validateAddressLine1(String value) {
        String text = clean(value);

        if (text.isBlank()) {
            throw new BadRequestException("Address line 1 is required.");
        }

        if (!ADDRESS.matcher(text).matches()) {
            throw new BadRequestException("Address line 1 contains invalid characters.");
        }

        if (!containsLetterAndNumber(text)) {
            throw new BadRequestException("Address line 1 must include house/building number and street/locality.");
        }
    }

    private void validateAddressLine2(String value) {
        String text = clean(value);

        if (text.isBlank()) {
            return;
        }

        if (text.length() > 120 || !ADDRESS.matcher(text).matches()) {
            throw new BadRequestException("Address line 2 contains invalid characters.");
        }

        rejectFakeText(text, "Address line 2");
    }

    private void validateCity(String value) {
        String text = clean(value);

        if (text.isBlank()) {
            throw new BadRequestException("City is required.");
        }

        if (text.length() < 2 || text.length() > 70 || !CITY_STATE.matcher(text).matches()) {
            throw new BadRequestException("City is invalid.");
        }
    }

    private void validateState(String value) {
        String text = clean(value);

        if (text.isBlank()) {
            throw new BadRequestException("State / Region is required.");
        }

        if (text.length() < 2 || text.length() > 70 || !CITY_STATE.matcher(text).matches()) {
            throw new BadRequestException("State / Region is invalid.");
        }
    }

    private void validatePostalCode(String value, String country) {
        String code = clean(value).toUpperCase(Locale.ROOT);

        if (code.isBlank()) {
            throw new BadRequestException("Postal code is required.");
        }

        Pattern pattern = POSTAL_PATTERNS.get(country);

        if (pattern == null || !pattern.matcher(code).matches()) {
            throw new BadRequestException("Postal code is invalid for selected country.");
        }
    }

    private void rejectFakeText(String value, String field) {
        String text = clean(value).toLowerCase(Locale.ROOT);
        String padded = " " + text.replaceAll("[,.'#\\-/()&]", " ") + " ";

        for (String word : FAKE_WORDS) {
            if (padded.matches(".*\\b" + Pattern.quote(word) + "\\b.*")) {
                throw new BadRequestException(field + " looks invalid.");
            }
        }

        if (text.matches("^\\d+$")) {
            throw new BadRequestException(field + " cannot contain only numbers.");
        }
    }

    private void rejectRepeatedPattern(String value, String field) {
        String text = clean(value).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");

        if (text.length() >= 5 && text.chars().distinct().count() <= 2) {
            throw new BadRequestException(field + " looks invalid.");
        }

        if (text.matches("(.)\\1{4,}")) {
            throw new BadRequestException(field + " looks invalid.");
        }
    }

    private boolean containsLetterAndNumber(String value) {
        boolean hasLetter = value.matches(".*\\p{L}.*");
        boolean hasNumber = value.matches(".*\\d.*");
        return hasLetter && hasNumber;
    }

    private String normalizeCountry(String value) {
        String country = clean(value).toUpperCase(Locale.ROOT);

        if (country.length() != 2 || !country.matches("^[A-Z]{2}$")) {
            throw new BadRequestException("Country is invalid.");
        }

        return country;
    }

    private String clean(String value) {
        if (value == null) return "";

        String normalized = Normalizer.normalize(value, Normalizer.Form.NFKC);

        return normalized
                .trim()
                .replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "")
                .replaceAll("\\s+", " ");
    }
}