package net.spals.appbuilder.model.protobuf;

import net.spals.appbuilder.message.protobuf.AddressBook;
import net.spals.appbuilder.message.protobuf.Person;
import net.spals.appbuilder.message.protobuf.Person.PhoneNumber;
import net.spals.appbuilder.message.protobuf.Person.PhoneType;
import org.hamcrest.Matcher;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Unit tests for {@link ProtobufModelSerializer}
 *
 * @author tkral
 */
public class ProtobufModelSerializerTest {

    @DataProvider
    Object[][] modelEqualityProvider() {
        final Person person1 = Person.newBuilder().setId(1).setName("Tim").build();
        final Person person2 = Person.newBuilder().setId(2).setName("Jim")
                .setEmail("jim@spals.net")
                .addPhones(PhoneNumber.newBuilder().setNumber("123-456-7890").setType(PhoneType.MOBILE))
                .build();

        return new Object[][] {
                {person1},
                {AddressBook.newBuilder().addPeople(person1).addPeople(person2).build()},
        };
    }

    @Test(dataProvider = "modelEqualityProvider")
    public void testModelEquality(final Object modelObject) {
        final ProtobufModelSerializer modelSerializer = new ProtobufModelSerializer();

        final byte[] serializedModelObject = modelSerializer.serialize(modelObject);
        assertThat(serializedModelObject, is(notNullValue()));
        assertThat(serializedModelObject, not((Matcher)emptyArray()));

        final Object deserializedModelObject = modelSerializer.deserialize(serializedModelObject);
        assertThat(deserializedModelObject, is(modelObject));
    }
}
