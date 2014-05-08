package com.bericotech.clavin.resolver;

/**
 * Convenience object used as output for {@link LocationResolver}
 *
 * Container class representing a resolved multipart location name,
 * such as what's often found in structured data like a spreadsheet
 * or database table. The resolved names for the three components
 * (e.g., city, state, country) are stored as three
 * {@link ResolvedLocation} objects.
 */
public class ResolvedMultipartLocation {
    private final ResolvedLocation city;
    private final ResolvedLocation state;
    private final ResolvedLocation country;

    /**
     * Sole constructor for {@link ResolvedMultipartLocation} class.
     *
     * Represents a resolved multipart location name, where the three
     * components (e.g., city, state, country) have been resolved to
     * three {@link ResolvedLocation} objects.
     *
     * @param city      ResolvedLocation for city name
     * @param state     ResolvedLocation for city/province/etc. name
     * @param country   ResolvedLocation for country name
     */
    public ResolvedMultipartLocation(ResolvedLocation city, ResolvedLocation state, ResolvedLocation country) {
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
        ResolvedMultipartLocation other = (ResolvedMultipartLocation)obj;
        return this.getCity().equals(other.getCity())
                && this.getState().equals(other.getState())
                && this.getCountry().equals(other.getCountry());
    }

    /**
     * For pretty-printing.
     * @return  "(<city>, <state>, <country>)"
     */
    @Override
    public String toString() {
        return String.format("(%s, %s, %s)", getCity(), getState(), getCountry());
    }

    /**
     * Get the text of the city name.
     * @return  the text of the city name
     */
    public ResolvedLocation getCity() {
        return city;
    }

    /**
     * Get the text of the state/province/etc. name.
     * @return  the text of the state/province/etc. name
     */
    public ResolvedLocation getState() {
        return state;
    }

    /**
     * Get the text of the country name.
     * @return  the text of the country name
     */
    public ResolvedLocation getCountry() {
        return country;
    }
}
