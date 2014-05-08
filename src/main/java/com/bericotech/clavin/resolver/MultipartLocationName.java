package com.bericotech.clavin.resolver;

/**
 * Convenience object used as input for {@link LocationResolver}
 *
 * Container class representing a multipart location name, such as
 * what's often found in structured data like a spreadsheet or database
 * table, e.g., [Reston][Virginia][United States]. The names are stored
 * as three Strings.
 *
 */
public class MultipartLocationName {

    /**
     * The city, state/province/etc., and country names making up a
     * multipart location name, e.g., [Reston][Virginia][United States]
     */
    private final String city;
    private final String state;
    private final String country;

    /**
     * Sole constructor for {@link MultipartLocationName} class.
     *
     * Represents a multipart location name, such as what's often found
     * in structured data, e.g., [Reston][Virginia][United States].
     *
     * @param city      the text of the city name, e.g., "Reston"
     * @param state     the text of the city/province/etc. name, e.g., "Virginia"
     * @param country   the text of the country name, e.g., "United States"
     */
    public MultipartLocationName(String city, String state, String country) {
        this.city = city;
        this.state = state;
        this.country = country;
    }

    /**
     * Tests equivalence based on names of city, state, and country.
     * @param obj   Object to compare this against
     * @return      true if objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null) return false;

        if (this.getClass() != obj.getClass()) return false;
        MultipartLocationName other = (MultipartLocationName)obj;
        return this.getCity().equalsIgnoreCase(other.getCity())
                && this.getState().equalsIgnoreCase(other.getState())
                && this.getCountry().equalsIgnoreCase(other.getCountry());
    }

    /**
     * For pretty-printing.
     * @return  "(city, state, country)"
     */
    @Override
    public String toString() {
        return String.format("(%s, %s, %s)", getCity(), getState(), getCountry());
    }

    /**
     * Get the text of the city name.
     * @return  the text of the city name
     */
    public String getCity() {
        return city;
    }

    /**
     * Get the text of the state/province/etc. name.
     * @return  the text of the state/province/etc. name
     */
    public String getState() {
        return state;
    }

    /**
     * Get the text of the country name.
     * @return  the text of the country name
     */
    public String getCountry() {
        return country;
    }
}
