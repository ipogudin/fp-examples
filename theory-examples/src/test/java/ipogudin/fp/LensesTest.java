package ipogudin.fp;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LensesTest {

    @Data
    @RequiredArgsConstructor
    static class Address {

        private final String street;
        private final String premise;

    }

    @Data
    @RequiredArgsConstructor
    static class Person {

        private final String name;
        private final Address address;

    }

    static class StreetAddressLens implements Lens<Address, String> {

        @Override
        public String get(Address address) {
            return address.premise;
        }

        @Override
        public Address set(Address address, String p) {
            return new Address(address.street, p);
        }

    }

    static class AddressPersonLens implements Lens<Person, Address> {

        @Override
        public Address get(Person person) {
            return person.address;
        }

        @Override
        public Person set(Person person, Address p) {
            return new Person(person.name, p);
        }

    }

    @Test
    public void lensComposition() {
        var person = new Person("George", new Address("W 23 St.", "10"));

        var streetLens = new StreetAddressLens();
        var addressLens = new AddressPersonLens();

        assertEquals(
                new Person("George", new Address("W 23 St.", "26")),
                addressLens.compose(streetLens).set(person, "26")
        );
    }

}
