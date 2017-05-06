package net.spals.appbuilder.model.protobuf;

import net.spals.appbuilder.message.protobuf.AddressBookV2;
import net.spals.appbuilder.message.protobuf.AddressBookV3;
import net.spals.appbuilder.message.protobuf.PersonV2;
import net.spals.appbuilder.message.protobuf.PersonV2.PhoneNumberV2;
import net.spals.appbuilder.message.protobuf.PersonV2.PhoneTypeV2;
import net.spals.appbuilder.message.protobuf.PersonV3;
import net.spals.appbuilder.message.protobuf.PersonV3.PhoneNumberV3;
import net.spals.appbuilder.message.protobuf.PersonV3.PhoneTypeV3;
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
public class ProtobufJavaModelSerializerTest {

    @DataProvider
    Object[][] modelEqualityProvider() {
        final PersonV2 personV2_1 = PersonV2.newBuilder().setId(1).setName("Tim").build();
        final PersonV2 personV2_2 = PersonV2.newBuilder().setId(2).setName("Jim")
                .setEmail("jim@spals.net")
                .addPhones(PhoneNumberV2.newBuilder().setNumber("123-456-7890").setType(PhoneTypeV2.MOBILE))
                .build();

        final PersonV3 personV3_1 = PersonV3.newBuilder().setId(3).setName("Tim").build();
        final PersonV3 personV3_2 = PersonV3.newBuilder().setId(4).setName("Jim")
                .setEmail("jim@spals.net")
                .addPhones(PhoneNumberV3.newBuilder().setNumber("123-456-7890").setType(PhoneTypeV3.MOBILE))
                .build();

        return new Object[][] {
                {personV2_1},
                {personV3_1},
                {AddressBookV2.newBuilder().addPeople(personV2_1).addPeople(personV2_2).build()},
                {AddressBookV3.newBuilder().addPeople(personV3_1).addPeople(personV3_2).build()},
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
